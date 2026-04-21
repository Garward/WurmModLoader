#!/usr/bin/env bash
#
# WurmModLoader Smart Deploy Script
# Deploys framework and mods to Wurm server
# Only copies files that have changed
#
set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# === CONFIG ===
PROJECT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
SERVER_DIR="${WURM_SERVER_DIR:-$HOME/.local/share/Steam/steamapps/common/Wurm Unlimited Dedicated Server}"
TEMP_EXTRACT="/tmp/wurmmodloader-deploy-$$"

# Find latest distribution ZIP (auto-detect version)
DIST_ZIP=$(ls -t "$PROJECT_DIR"/build/distributions/WurmModloader-Runtime-*.zip 2>/dev/null | head -1)

# Track changes
COPIED_FILES=0
SKIPPED_FILES=0
ERRORS=0

echo -e "${BLUE}======================================================================${NC}"
echo -e "${BLUE}🚀 WurmModLoader Smart Deploy${NC}"
echo -e "${BLUE}======================================================================${NC}"
echo ""

# === CHECKS ===
if [ -z "$DIST_ZIP" ] || [ ! -f "$DIST_ZIP" ]; then
    echo -e "${RED}❌ Distribution ZIP not found!${NC}"
    echo -e "${YELLOW}Expected location:${NC} $PROJECT_DIR/build/distributions/"
    echo -e "${YELLOW}Run build first:${NC} ./build.sh"
    exit 1
fi

if [ ! -d "$SERVER_DIR" ]; then
    echo -e "${RED}❌ Server directory not found!${NC}"
    echo -e "${YELLOW}Expected:${NC} $SERVER_DIR"
    exit 1
fi

echo -e "${CYAN}📦 Distribution:${NC} $(basename "$DIST_ZIP")"
echo -e "${CYAN}🎯 Server:${NC} $SERVER_DIR"
echo ""

# === EXTRACT DISTRIBUTION ===
echo -e "${YELLOW}📂 Extracting distribution to temp...${NC}"
mkdir -p "$TEMP_EXTRACT"
unzip -q "$DIST_ZIP" -d "$TEMP_EXTRACT"
echo -e "${GREEN}✓${NC} Extracted to $TEMP_EXTRACT"
echo ""

# === FUNCTION: Copy if changed ===
copy_if_changed() {
    local src="$1"
    local dest="$2"
    local desc="$3"

    if [ ! -f "$src" ]; then
        echo -e "  ${RED}✗${NC} ${desc} - source not found"
        ERRORS=$((ERRORS + 1))
        return 1
    fi

    # Create destination directory if needed
    mkdir -p "$(dirname "$dest")"

    # Check if file exists and is identical
    if [ -f "$dest" ]; then
        if cmp -s "$src" "$dest"; then
            echo -e "  ${BLUE}⊙${NC} ${desc} - unchanged"
            SKIPPED_FILES=$((SKIPPED_FILES + 1))
            return 0
        fi
    fi

    # Copy the file
    if cp "$src" "$dest"; then
        echo -e "  ${GREEN}✓${NC} ${desc}"
        COPIED_FILES=$((COPIED_FILES + 1))
        return 0
    else
        echo -e "  ${RED}✗${NC} ${desc} - copy failed"
        ERRORS=$((ERRORS + 1))
        return 1
    fi
}

# === DEPLOY FRAMEWORK JARS ===
echo -e "${BLUE}======================================================================${NC}"
echo -e "${YELLOW}📚 Deploying Framework JARs${NC}"
echo -e "${BLUE}======================================================================${NC}"

# Framework JARs go to server root
for jar in "$TEMP_EXTRACT"/*.jar; do
    if [ -f "$jar" ]; then
        basename_jar=$(basename "$jar")
        copy_if_changed "$jar" "$SERVER_DIR/$basename_jar" "Framework: $basename_jar"
    fi
done

echo ""

# === SYNC FRAMEWORK JARS TO COMMUNITYMODS/libs ===
# Keeps the sibling CommunityMods repo building against the freshly-built
# framework. Silently skipped if the repo isn't present.
COMMUNITY_LIBS="${WURMMODLOADER_COMMUNITY_DIR:-$PROJECT_DIR/../WurmModLoader-CommunityMods}/libs"
if [ -d "$COMMUNITY_LIBS" ]; then
    echo -e "${BLUE}======================================================================${NC}"
    echo -e "${YELLOW}🔗 Syncing Framework JARs to CommunityMods/libs${NC}"
    echo -e "${BLUE}======================================================================${NC}"
    for module in wurmmodloader-api wurmmodloader-core wurmmodloader-legacy wurmmodloader-modsupport; do
        # Pick the main (non-sources, non-javadoc) jar
        src_jar=$(ls "$PROJECT_DIR/$module/build/libs/"${module}-*.jar 2>/dev/null \
            | grep -v -- '-sources\.jar$' | grep -v -- '-javadoc\.jar$' | head -1)
        if [ -n "$src_jar" ] && [ -f "$src_jar" ]; then
            dest_jar="$COMMUNITY_LIBS/$(basename "$src_jar")"
            copy_if_changed "$src_jar" "$dest_jar" "CommunityLib: $(basename "$src_jar")"
        else
            echo -e "  ${RED}✗${NC} CommunityLib: $module - source jar not found"
            ERRORS=$((ERRORS + 1))
        fi
    done
    echo ""
fi

# === SEED FRAMEWORK CONFIG (non-destructive) ===
# Copy the HTTP subsystem config template if user hasn't created one yet.
# Never overwrite — preserves user edits across deploys.
HTTP_CFG_SRC="$PROJECT_DIR/docs/reference/wurmmodloader-http.properties.example"
HTTP_CFG_DEST="$SERVER_DIR/config/wurmmodloader-http.properties"
if [ -f "$HTTP_CFG_SRC" ] && [ ! -f "$HTTP_CFG_DEST" ]; then
    mkdir -p "$(dirname "$HTTP_CFG_DEST")"
    cp "$HTTP_CFG_SRC" "$HTTP_CFG_DEST"
    echo -e "  ${GREEN}✓${NC} Seeded config/wurmmodloader-http.properties (edit to customize port/address)"
    COPIED_FILES=$((COPIED_FILES + 1))
fi

VFIX_CFG_SRC="$PROJECT_DIR/docs/reference/wurmmodloader-vanilla-fixes.properties.example"
VFIX_CFG_DEST="$SERVER_DIR/config/wurmmodloader-vanilla-fixes.properties"
if [ -f "$VFIX_CFG_SRC" ] && [ ! -f "$VFIX_CFG_DEST" ]; then
    mkdir -p "$(dirname "$VFIX_CFG_DEST")"
    cp "$VFIX_CFG_SRC" "$VFIX_CFG_DEST"
    echo -e "  ${GREEN}✓${NC} Seeded config/wurmmodloader-vanilla-fixes.properties (edit to toggle fixes / tune timer caps)"
    COPIED_FILES=$((COPIED_FILES + 1))
fi

# === DEPLOY MODS ===
echo -e "${BLUE}======================================================================${NC}"
echo -e "${YELLOW}🎮 Deploying Mods${NC}"
echo -e "${BLUE}======================================================================${NC}"

# Find all mod JARs in the project
# Mods are in mods/*/build/libs/*.jar (excluding sources/javadoc)
for mod_dir in "$PROJECT_DIR"/mods/*/; do
    if [ -d "$mod_dir" ]; then
        mod_name=$(basename "$mod_dir")
        mod_jar="$mod_dir/build/libs/${mod_name}.jar"

        if [ -f "$mod_jar" ]; then
            # Deploy to server/mods/modname/
            server_mod_dir="$SERVER_DIR/mods/$mod_name"
            mkdir -p "$server_mod_dir"

            copy_if_changed "$mod_jar" "$server_mod_dir/$mod_name.jar" "Mod: $mod_name"

            # Also copy .properties if exists
            mod_props="$mod_dir/src/dist/${mod_name}.properties"
            if [ -f "$mod_props" ]; then
                copy_if_changed "$mod_props" "$SERVER_DIR/mods/${mod_name}.properties" "Config: ${mod_name}.properties"
            fi
        else
            echo -e "  ${YELLOW}⚠${NC}  Mod: $mod_name - JAR not built (run ./build.sh)"
        fi
    fi
done

echo ""

# === CLEANUP ===
echo -e "${YELLOW}🧹 Cleaning up...${NC}"
rm -rf "$TEMP_EXTRACT"
echo -e "${GREEN}✓${NC} Temp files removed"
echo ""

# === SUMMARY ===
echo -e "${BLUE}======================================================================${NC}"
echo -e "${GREEN}📊 Deployment Summary${NC}"
echo -e "${BLUE}======================================================================${NC}"
echo -e "${GREEN}✓ Copied:${NC}    $COPIED_FILES files"
echo -e "${BLUE}⊙ Unchanged:${NC} $SKIPPED_FILES files"
if [ $ERRORS -gt 0 ]; then
    echo -e "${RED}✗ Errors:${NC}    $ERRORS"
    echo ""
    echo -e "${RED}⚠️  Deployment completed with errors${NC}"
    exit 1
else
    echo ""
    echo -e "${GREEN}✅ Deployment Complete!${NC}"
    echo ""
    echo -e "${CYAN}Next steps:${NC}"
    echo -e "  1. Start your Wurm server"
    echo -e "  2. Check logs for mod loading"
    echo -e "  3. Test your changes"
    echo ""
    exit 0
fi
