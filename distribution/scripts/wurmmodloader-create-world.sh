#!/usr/bin/env bash
# WurmModLoader — one-button new-world setup.
#
# Creates a clean world folder (sqlite DBs scrubbed of Adventure's hardcoded
# villages/spawn), optionally drops in a custom map, and configures the world
# seeder to paint a starter town at the landmass center on first boot.
#
# Usage (interactive):
#     ./wurmmodloader-create-world.sh
#
# Usage (non-interactive — pass args, any omitted arg gets prompted):
#     ./wurmmodloader-create-world.sh --name Riverweave --maps ~/maps/riverweave --starter-town yes
#
# Flags:
#     --name <WorldName>         Folder name under the server root
#     --maps <dir|default>       Directory holding top_layer.map etc., or "default"
#                                to clone Adventure's shipped terrain
#     --starter-town yes|no      Whether to seed Winkshir at the map center
#     --mode pve|pvp             pve = peaceful home (HOMESERVER=1, PVP=0) — default.
#                                pvp = Chaos-style (HOMESERVER=0, PVP=1). Kingdom stays
#                                Freedom (4) in both modes; PvP/PvE is a server-flag
#                                choice, not a kingdom choice.
#     --template <WorldName>     World folder to clone sqlite/wurm.ini from
#                                (default: Adventure)
#     --server <dir>             Server root (default: resolved relative to this script)
#     --force                    Overwrite an existing world folder of the same name
#     --yes                      Accept all defaults, no prompts
#     --help                     Show this help text

set -euo pipefail

die() { echo "ERROR: $*" >&2; exit 1; }
note() { printf '>>> %s\n' "$*"; }

# ---------------------------------------------------------------- arg parse --
WORLD_NAME=""
MAPS_SOURCE=""
STARTER_TOWN=""
MODE=""
TEMPLATE="Adventure"
SERVER_DIR=""
FORCE=0
ACCEPT_DEFAULTS=0

while [ $# -gt 0 ]; do
    case "$1" in
        --name)          WORLD_NAME="$2"; shift 2 ;;
        --maps)          MAPS_SOURCE="$2"; shift 2 ;;
        --starter-town)  STARTER_TOWN="$2"; shift 2 ;;
        --mode)          MODE="$2"; shift 2 ;;
        --template)      TEMPLATE="$2"; shift 2 ;;
        --server)        SERVER_DIR="$2"; shift 2 ;;
        --force)         FORCE=1; shift ;;
        --yes)           ACCEPT_DEFAULTS=1; shift ;;
        --help|-h)       sed -n '2,25p' "$0"; exit 0 ;;
        *) die "unknown argument: $1" ;;
    esac
done

# -------------------------------------------------------------- server root --
if [ -z "$SERVER_DIR" ]; then
    # Scripts ship to the server root; resolve there.
    SERVER_DIR="$(cd "$(dirname "$0")" && pwd)"
fi
[ -d "$SERVER_DIR" ] || die "server directory not found: $SERVER_DIR"
[ -f "$SERVER_DIR/wurmmodloader.sh" ] || die "doesn't look like a server root (no wurmmodloader.sh): $SERVER_DIR"

REBUILD_SCRIPT="$SERVER_DIR/wurmmodloader-rebuild-dbs.sh"
[ -x "$REBUILD_SCRIPT" ] || die "rebuild-dbs script missing or not executable: $REBUILD_SCRIPT"

# ---------------------------------------------------------------- prompts ---
ask() {
    # ask <varname> <prompt> <default>
    local var="$1" prompt="$2" default="$3" reply
    if [ -n "${!var:-}" ]; then return; fi
    if [ "$ACCEPT_DEFAULTS" = 1 ]; then
        printf -v "$var" '%s' "$default"
        return
    fi
    if [ -n "$default" ]; then
        read -r -p "$prompt [$default]: " reply
        reply="${reply:-$default}"
    else
        read -r -p "$prompt: " reply
    fi
    printf -v "$var" '%s' "$reply"
}

ask WORLD_NAME "New world folder name" "Riverweave"
[ -n "$WORLD_NAME" ] || die "world name is required"
case "$WORLD_NAME" in
    */*|*..*|'') die "invalid world name: $WORLD_NAME" ;;
esac

TEMPLATE_DIR="$SERVER_DIR/$TEMPLATE"
[ -d "$TEMPLATE_DIR" ] || die "template world not found: $TEMPLATE_DIR"

# If a matching map pack lives under Maps/<WorldName>/ (canonical WU layout),
# offer it as the default. Users almost always want the terrain that matches
# the world name they just picked, not Adventure's clone.
MAPS_DEFAULT="default"
if [ -d "$SERVER_DIR/Maps/$WORLD_NAME" ] && \
   ( [ -f "$SERVER_DIR/Maps/$WORLD_NAME/top_layer.map" ] || \
     [ -f "$SERVER_DIR/Maps/$WORLD_NAME/rock_layer.map" ] ); then
    MAPS_DEFAULT="$SERVER_DIR/Maps/$WORLD_NAME"
    note "Found map pack at Maps/$WORLD_NAME — offering as default."
fi
ask MAPS_SOURCE "Maps: 'default' to clone $TEMPLATE terrain, a directory path, or a pack name under Maps/ (e.g. Riverweave)" "$MAPS_DEFAULT"

ask STARTER_TOWN "Seed Winkshir starter town at the landmass center? (yes/no)" "yes"
case "$STARTER_TOWN" in
    yes|y|true|1)  STARTER_TOWN="yes" ;;
    no|n|false|0)  STARTER_TOWN="no" ;;
    *) die "invalid --starter-town value: $STARTER_TOWN (expected yes/no)" ;;
esac

ask MODE "Server mode: pve (peaceful home) or pvp (Chaos-style)" "pve"
case "$MODE" in
    pve|PvE|PVE) MODE="pve" ;;
    pvp|PvP|PVP) MODE="pvp" ;;
    *) die "invalid --mode value: $MODE (expected pve/pvp)" ;;
esac

# ----------------------------------------------------- target preparation ---
WORLD_DIR="$SERVER_DIR/$WORLD_NAME"
if [ -d "$WORLD_DIR" ]; then
    if [ "$FORCE" = 1 ]; then
        note "Removing existing $WORLD_DIR (--force)."
        rm -rf "$WORLD_DIR"
    else
        die "world already exists: $WORLD_DIR (pass --force to overwrite)"
    fi
fi

mkdir -p "$WORLD_DIR/sqlite"
mkdir -p "$WORLD_DIR/Logs"

# WU uses an empty 'gamedir' marker file to identify world folders in the
# launcher (see com.wurmonline.server.gui.folders.GameEntity). Without it,
# Folders.setCurrent() NPEs on start=<WorldName>.
touch "$WORLD_DIR/gamedir"

# ------------------------------------------------------------- sqlite copy --
# Pull the full sqlite scaffold — .db files carry the *migrated* schema that
# Wurm expects (ITEMDATA.EXTRA1/EXTRA2, RECIPES* tables, PLAYERS nutrition
# cols, etc.). The shipped wurm*.sql scaffolds are stale and lack most of it,
# so rebuild-dbs scrubs rows out of the template .db files in place instead
# of dropping+recreating tables. Skip .db-wal/.db-shm (transient log files).
note "Copying sqlite scaffold from $TEMPLATE/sqlite/."
for f in "$TEMPLATE_DIR/sqlite/"*; do
    [ -e "$f" ] || continue
    name="$(basename "$f")"
    case "$name" in
        *.db-wal|*.db-shm) continue ;;
    esac
    cp "$f" "$WORLD_DIR/sqlite/$name"
done

# ------------------------------------------------------------- wurm.ini -----
if [ -f "$TEMPLATE_DIR/wurm.ini" ]; then
    cp "$TEMPLATE_DIR/wurm.ini" "$WORLD_DIR/wurm.ini"
fi

# --------------------------------------------------------------- map files --
# Terrain game data (required) + client-side visuals (optional, used by livemap
# and the WU launcher map preview). PNGs aren't consumed by the server runtime
# itself, but worlds sourced from Maps/<name>/ ship them and players expect
# them to travel with the map.
MAP_FILES=(top_layer.map rock_layer.map map_cave.map flags.map resources.map protectedTiles.bmap map_actions.act)
MAP_EXTRAS=(heightmap.png biomes.png cave.png map.png topography.png)
if [ "$MAPS_SOURCE" = "default" ] || [ -z "$MAPS_SOURCE" ]; then
    note "Cloning map files from $TEMPLATE/."
    for m in "${MAP_FILES[@]}" "${MAP_EXTRAS[@]}"; do
        [ -f "$TEMPLATE_DIR/$m" ] && cp "$TEMPLATE_DIR/$m" "$WORLD_DIR/$m"
    done
else
    # Expand ~ and resolve bare names (e.g. "Riverweave") against Maps/<name>/.
    MAPS_SOURCE="${MAPS_SOURCE/#\~/$HOME}"
    if [ ! -d "$MAPS_SOURCE" ] && [ -d "$SERVER_DIR/Maps/$MAPS_SOURCE" ]; then
        note "Resolving '$MAPS_SOURCE' → $SERVER_DIR/Maps/$MAPS_SOURCE."
        MAPS_SOURCE="$SERVER_DIR/Maps/$MAPS_SOURCE"
    fi
    [ -d "$MAPS_SOURCE" ] || die "custom map source directory not found: $MAPS_SOURCE"
    note "Copying custom map files from $MAPS_SOURCE."
    local_found=0
    for m in "${MAP_FILES[@]}" "${MAP_EXTRAS[@]}"; do
        if [ -f "$MAPS_SOURCE/$m" ]; then
            cp "$MAPS_SOURCE/$m" "$WORLD_DIR/$m"
            case "$m" in *.map|*.bmap) local_found=$((local_found + 1)) ;; esac
        fi
    done
    [ "$local_found" -gt 0 ] || die "no map files found in $MAPS_SOURCE (looking for ${MAP_FILES[*]})"
    # Carry over the template's protectedTiles if the custom source didn't supply one.
    if [ ! -f "$WORLD_DIR/protectedTiles.bmap" ] && [ -f "$TEMPLATE_DIR/protectedTiles.bmap" ]; then
        cp "$TEMPLATE_DIR/protectedTiles.bmap" "$WORLD_DIR/protectedTiles.bmap"
    fi
fi

# ---------------------------------------------------------- rebuild DBs ----
note "Rebuilding & scrubbing databases via $REBUILD_SCRIPT."
if ! "$REBUILD_SCRIPT" -noconfirm -nopause "$WORLD_DIR/sqlite" >/dev/null; then
    die "rebuild-dbs failed — inspect $WORLD_DIR/sqlite/ and rerun manually."
fi

# -------------------------------------------------- server mode override ---
# rebuild-dbs forces a peaceful Freedom home (HOMESERVER=1, KINGDOM=4, PVP=0).
# Kingdom=4 is just the server's base kingdom — PvP is a SERVERS-flag choice,
# not a kingdom choice, so --mode pvp only flips HOMESERVER + PVP.
if [ "$MODE" = "pvp" ]; then
    note "Setting SERVERS to Chaos-style (HOMESERVER=0, PVP=1)."
    SQLITE_BIN="$(command -v sqlite3 || true)"
    if [ -z "$SQLITE_BIN" ]; then
        SQLITE_BIN="$SERVER_DIR/nativelibs/sqlite/sqlite3"
    fi
    [ -x "$SQLITE_BIN" ] || die "sqlite3 binary not found (needed for --mode pvp override)"
    "$SQLITE_BIN" "$WORLD_DIR/sqlite/wurmlogin.db" \
        "UPDATE SERVERS SET HOMESERVER=0, PVP=1;"
fi

# --------------------------------------------- seeder / starter-town config --
SEED_CONFIG="$SERVER_DIR/config/wurmmodloader-world-seed.yaml"
mkdir -p "$SERVER_DIR/config"
note "Writing $SEED_CONFIG."

if [ "$STARTER_TOWN" = "yes" ]; then
    cat > "$SEED_CONFIG" <<EOF
# Auto-generated by wurmmodloader-create-world.sh
# World: $WORLD_NAME
enabled: true
force: false
strategy: center
centerWaterBuffer: 20
importStarterTown: true
starterTownSnapshot: winkshir
createVillage: true
centerTownName: Freedom Landing
despawnFounderNpc: true
EOF
else
    cat > "$SEED_CONFIG" <<EOF
# Auto-generated by wurmmodloader-create-world.sh
# World: $WORLD_NAME
enabled: true
force: false
strategy: center
centerWaterBuffer: 20
importStarterTown: false
createVillage: true
centerTownName: Freedom Landing
despawnFounderNpc: true
EOF
fi

# ---------------------------------------------------------------- summary --
cat <<EOF

World '$WORLD_NAME' ready.
  Folder:       $WORLD_DIR
  Map source:   $([ "$MAPS_SOURCE" = "default" ] && echo "$TEMPLATE (default)" || echo "$MAPS_SOURCE")
  Starter town: $STARTER_TOWN ($([ "$STARTER_TOWN" = "yes" ] && echo "Winkshir, painted at landmass center on first boot" || echo "bare token only"))
  Mode:         $MODE ($([ "$MODE" = "pve" ] && echo "peaceful home, Freedom starter" || echo "Chaos-style PvP, Freedom starter"))
  Seed config:  $SEED_CONFIG

Launch the server with:
    ./wurmmodloader.sh start=$WORLD_NAME

EOF
