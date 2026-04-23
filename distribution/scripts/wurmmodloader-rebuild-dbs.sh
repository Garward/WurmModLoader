#!/usr/bin/env sh
#
# wurmmodloader-rebuild-dbs.sh
#
# Drop-in replacement for vanilla's Wurm Unlimited ./rebuild-dbs that produces
# a *clean* starter world instead of re-seeding the Adventure map's full
# populated game state. Vanilla's insertion scripts (shipped identically in
# every world's sqlite/ dir) bake in:
#
#   * 49 guard towers + pre-spawned kingdom guards at Adventure coordinates
#     (NPE-spam when Gartopolis's smaller terrain can't host them)
#   * 14,000+ pre-spawned creatures (mobs, animals, offspring)
#   * 3 demo player accounts (Ceyer, Brightberry, Zampooklidin) with their
#     positions, skills, and inventories — this is why "fresh" worlds let
#     old characters log in with full gear
#   * Starter villages Hearth/Winkshir/Litocania + HISTORY rows about them
#   * Pre-placed traders, banks, supply-demand prices, coin reserves
#   * HotA zones, focus zones, recruitment boards — all Adventure-specific
#   * SERVERS row with SPAWNPOINT* coords hardcoded to Adventure tiles
#     (why new custom-map worlds spawn players in the NW corner)
#
# Strategy: we run the schema-creation .sql for every DB but only run the
# insert<db>.sql scripts for DBs that hold genuine definition/bootstrap data
# (item templates, pantheon, SERVERS row, empty log tables). The five
# game-state DBs — items, creatures, zones, players, economy — get their
# schema but NOT their Adventure data, so they start empty and the new world
# begins truly fresh. WurmModLoader's WorldSeedBootstrap then seeds one
# starter village at the map's landmass centroid on first boot.
#
# Unlike vanilla rebuild-dbs this does NOT require a local ./sqlite3 binary
# in the world's sqlite/ dir — we pick one up from the directory, the server
# root, or $PATH, in that order.
#
# Usage:
#   cd <ServerRoot>/<WorldName>/sqlite
#   <ServerRoot>/wurmmodloader-rebuild-dbs.sh
#
#   # or from anywhere, pointing at the sqlite dir:
#   <ServerRoot>/wurmmodloader-rebuild-dbs.sh <ServerRoot>/<WorldName>/sqlite
#
#   # or from the server root with just the world name:
#   cd <ServerRoot>
#   ./wurmmodloader-rebuild-dbs.sh <WorldName>
#
# Flags:
#   -noconfirm   skip the "are you sure" prompt
#   -nopause     skip the trailing "press enter" pause
#   --help, -h   show this help
#
set -eu

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

usage() {
    grep -E '^# ?' "$0" | sed -E 's/^# ?//' | sed -n '2,45p'
    exit 0
}

CONFIRM=1
PAUSE=1
TARGET=""
for arg in "$@"; do
    case "$arg" in
        -noconfirm) CONFIRM=0 ;;
        -nopause)   PAUSE=0 ;;
        --help|-h)  usage ;;
        -*)
            printf "${RED}Unknown flag:${NC} %s\n" "$arg" >&2
            exit 2
            ;;
        *)
            if [ -z "$TARGET" ]; then TARGET="$arg"
            else
                printf "${RED}Too many positional args (got extra: %s)${NC}\n" "$arg" >&2
                exit 2
            fi
            ;;
    esac
done

resolve_sqlite_dir() {
    # 1) explicit arg is an existing sqlite dir
    if [ -n "$TARGET" ] && [ -d "$TARGET" ] && [ -f "$TARGET/wurmzones.sql" ]; then
        (cd "$TARGET" && pwd); return
    fi
    # 2) explicit arg is a world dir; look for <arg>/sqlite
    if [ -n "$TARGET" ] && [ -d "$TARGET/sqlite" ] && [ -f "$TARGET/sqlite/wurmzones.sql" ]; then
        (cd "$TARGET/sqlite" && pwd); return
    fi
    # 3) explicit arg is a world-name relative to CWD (server root convention)
    if [ -n "$TARGET" ] && [ -d "./$TARGET/sqlite" ] && [ -f "./$TARGET/sqlite/wurmzones.sql" ]; then
        (cd "./$TARGET/sqlite" && pwd); return
    fi
    # 4) no arg, and we're *in* a sqlite dir
    if [ -z "$TARGET" ] && [ -f "./wurmzones.sql" ]; then
        pwd; return
    fi
    printf "${RED}Could not find a Wurm sqlite directory.${NC}\n" >&2
    printf "${YELLOW}Expected a directory containing wurmzones.sql (and siblings).${NC}\n" >&2
    printf "${YELLOW}Run from inside <WorldName>/sqlite/ or pass the world name as an argument.${NC}\n" >&2
    exit 1
}

# Pick a usable sqlite3: prefer the world-local binary, then server-root,
# then system PATH. Vanilla ships Windows sqlite3.exe + DLLs but no Linux
# binary by default, so most real installs need the PATH fallback.
pick_sqlite_cmd() {
    dir="$1"
    if [ -x "$dir/sqlite3" ]; then echo "$dir/sqlite3"; return; fi
    # Server root (parent of the world dir).
    server_root="$(cd "$dir/../.." && pwd)"
    if [ -x "$server_root/sqlite3" ]; then echo "$server_root/sqlite3"; return; fi
    # System.
    if command -v sqlite3 >/dev/null 2>&1; then command -v sqlite3; return; fi
    return 1
}

SQLITE_DIR=$(resolve_sqlite_dir)
SQLITE_CMD=$(pick_sqlite_cmd "$SQLITE_DIR") || {
    printf "${RED}ERROR:${NC} no sqlite3 binary found (checked %s/sqlite3, server-root, PATH).\n" "$SQLITE_DIR" >&2
    printf "${YELLOW}Install sqlite3 (e.g. 'pacman -S sqlite' / 'apt install sqlite3') or drop a binary in the server root.${NC}\n" >&2
    exit 1
}

printf "${BLUE}======================================================================${NC}\n"
printf "${BLUE}🧹 WurmModLoader Clean Rebuild${NC}\n"
printf "${BLUE}======================================================================${NC}\n"
printf "${CYAN}Target:${NC} %s\n" "$SQLITE_DIR"
printf "${CYAN}sqlite3:${NC} %s\n" "$SQLITE_CMD"
printf "\n"

if [ $CONFIRM -eq 1 ]; then
    printf "${YELLOW}This will DELETE and rebuild all databases in:${NC}\n  %s\n" "$SQLITE_DIR"
    printf "${YELLOW}Adventure-specific hardcoded villages / spawns / zones will be scrubbed.${NC}\n"
    printf "Proceed? (y/[n]) "
    read ANSWER
    if [ "$ANSWER" != "y" ] && [ "$ANSWER" != "Y" ]; then
        printf "${YELLOW}Aborted.${NC}\n"
        exit 0
    fi
fi

DB_NAMES="wurmcreatures wurmdeities wurmeconomy wurmitems wurmlogin wurmlogs wurmplayers wurmtemplates wurmzones"

# DBs whose *data* we scrub. The Adventure-shipped .db files carry the
# fully-migrated schema that Wurm expects (ITEMDATA.EXTRA1/EXTRA2, RECIPES*
# tables, PLAYERS nutrition cols, many more). The shipped wurm*.sql scaffolds
# are stale and don't match that schema, so dropping and re-running the .sql
# produces a DB Wurm treats as broken at runtime (setAllData, loadBannedSteamIds,
# loadPlayerRecipes all fail with "column/relation does not exist").
#
# Strategy: keep the template's .db files intact (preserves schema) and DELETE
# FROM every table to empty them. Bootstrap DBs retain their rows so Wurm has
# item templates, pantheon, SERVERS row, log table structure, etc.
SCRUB_DATA="wurmcreatures wurmeconomy wurmitems wurmplayers wurmzones"

in_list() {
    needle="$1"; shift
    for item in "$@"; do [ "$item" = "$needle" ] && return 0; done
    return 1
}

# --- Phase 1: verify the template .db files are present ---------------------
missing=0
for db in $DB_NAMES; do
    if [ ! -f "$SQLITE_DIR/$db.db" ]; then
        printf "${RED}Missing:${NC} %s (template .db not copied in?)\n" "$SQLITE_DIR/$db.db" >&2
        missing=1
    fi
done
[ $missing -eq 1 ] && exit 1

# --- Phase 2: scrub game-state rows, preserve schema ------------------------
printf "${BLUE}[1/2]${NC} Scrubbing Adventure row data from template databases...\n"
for db in $DB_NAMES; do
    target="$SQLITE_DIR/$db.db"
    # Drop transient WAL/SHM companions so the next open starts clean.
    rm -f "$target-wal" "$target-shm"
    if in_list "$db" $SCRUB_DATA; then
        # Enumerate user tables (exclude sqlite_* internals + Flyway's
        # SCHEMA_VERSION — Wurm manages its own migration state) and empty
        # each. PRAGMA foreign_keys=OFF avoids ordering issues; SQLite
        # doesn't enforce FK by default but be explicit.
        tables=$("$SQLITE_CMD" "$target" "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name <> 'SCHEMA_VERSION';")
        {
            echo "PRAGMA foreign_keys=OFF;"
            echo "BEGIN;"
            for t in $tables; do
                echo "DELETE FROM \"$t\";"
            done
            echo "COMMIT;"
            echo "VACUUM;"
        } | "$SQLITE_CMD" "$target" >/dev/null
        count=$(echo "$tables" | wc -w | tr -d ' ')
        printf "  ${GREEN}✓${NC} %s (emptied %s user table(s); schema preserved)\n" "$db.db" "$count"
    else
        printf "  ${GREEN}✓${NC} %s (bootstrap data retained)\n" "$db.db"
    fi
done

# --- Phase 3: scrub Adventure-specific bootstrap values ---------------------
printf "\n${BLUE}[2/2]${NC} Normalizing bootstrap values for a fresh world...\n"

# wurmlogin.db — SERVERS row is essential (insertwurmlogin ran). Zero out the
# Adventure-map spawn coordinates and force a Freedom home-server default so
# WorldSeedBootstrap can pick a landmass-centroid tile on first boot.
# World folder name = parent directory of sqlite/. The inserted SERVERS row
# still carries Adventure's NAME/MAPNAME ('Heavenord'); rename both to match
# this world so the server and map-name dependent logic don't trip.
WORLD_NAME=$(basename "$(dirname "$SQLITE_DIR")")
# SQL-escape single quotes.
WORLD_NAME_SQL=$(printf '%s' "$WORLD_NAME" | sed "s/'/''/g")

HISTORY_BEFORE=$("$SQLITE_CMD" "$SQLITE_DIR/wurmlogin.db" "SELECT COUNT(*) FROM HISTORY;")
"$SQLITE_CMD" "$SQLITE_DIR/wurmlogin.db" <<SQL
DELETE FROM HISTORY;
UPDATE SERVERS SET
    NAME            = '${WORLD_NAME_SQL}',
    MAPNAME         = '${WORLD_NAME_SQL}',
    SPAWNPOINTJENNX = 0, SPAWNPOINTJENNY = 0,
    SPAWNPOINTMOLX  = 0, SPAWNPOINTMOLY  = 0,
    SPAWNPOINTLIBX  = 0, SPAWNPOINTLIBY  = 0,
    HOMESERVER = 1, KINGDOM = 4, PVP = 0, LOCAL = 1;

-- SERVERPROPERTIES holds the SERVERS-adjacent tunables Wurm reads at boot.
-- HOMESERVER_KINGDOM defaults to 1 (JK) in Adventure's scaffold; override to
-- Freedom (4) so ServerConfigGenerator emits homeServerKingdom: 4 in the
-- generated yaml and Wurm's home-kingdom logic matches KINGDOM=4 above.
-- AUTO_NETWORKING=false lets the user's externalIp/externalPort yaml values
-- actually land in SERVERS instead of being overwritten by Wurm's detector.
DELETE FROM SERVERPROPERTIES WHERE PROPKEY IN ('HOMESERVER_KINGDOM','AUTO_NETWORKING');
INSERT INTO SERVERPROPERTIES (PROPKEY, PROPVAL) VALUES ('HOMESERVER_KINGDOM', '4');
INSERT INTO SERVERPROPERTIES (PROPKEY, PROPVAL) VALUES ('AUTO_NETWORKING', 'false');
SQL
printf "  ${GREEN}✓${NC} wurmlogin: wiped HISTORY (%s), set NAME/MAPNAME=%s, zeroed spawn points, forced Freedom home (HOMESERVER=1, KINGDOM=4, PVP=0, LOCAL=1)\n" \
    "$HISTORY_BEFORE" "$WORLD_NAME"
printf "  ${GREEN}✓${NC} SERVERPROPERTIES: HOMESERVER_KINGDOM=4 (Freedom), AUTO_NETWORKING=false (yaml externalIp wins)\n"

# Mark this sqlite scaffold as freshly rebuilt. The PostgresBackend mod honors
# this marker on the next boot by dropping its per-world database (if one
# exists from a prior run) before re-importing — otherwise the fresh SQLite
# wouldn't take effect, since Postgres already holds the old world's state.
touch "$SQLITE_DIR/.wurmmodloader-fresh-world"
printf "  ${GREEN}✓${NC} wrote .wurmmodloader-fresh-world marker (Postgres will DROP+reimport on next boot)\n"

printf "\n${GREEN}✅ Clean rebuild complete.${NC}\n"
printf "${CYAN}Next:${NC} start the server with ${YELLOW}start=<WorldName>${NC}; WorldSeedBootstrap will\n"
printf "       pick a landmass-centroid tile and seed one starter village at first boot.\n"

if [ $PAUSE -eq 1 ]; then
    printf "\nPress [Enter] to continue..."
    read _
fi
