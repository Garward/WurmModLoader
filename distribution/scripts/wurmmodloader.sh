#!/usr/bin/env bash
# WurmModLoader launcher script

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Find Java (use bundled JRE if available, otherwise system Java)
if test -x "runtime/jre1.8.0_172/bin/java"; then
	JAVA="runtime/jre1.8.0_172/bin/java"
elif test -x "../runtime/jre1.8.0_172/bin/java"; then
	JAVA="../runtime/jre1.8.0_172/bin/java"
else
	JAVA="java"
fi

# Set up library path for Steam JNI and other natives
LD_LIBRARY_PATH="$PWD/nativelibs"
if test $(uname -m) = "x86_64"; then
	LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$PWD/linux64"
else
	LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$PWD"
fi
export LD_LIBRARY_PATH

# Optional logging configuration
if test -e "logging.properties"; then
	LOGGING=-Djava.util.logging.config.file=logging.properties
fi

# Resolve jar helper
resolve_jar() {
	local pattern="$1"
	local jar
	jar=$(compgen -G "$pattern" | head -n 1)
	if [ -z "$jar" ]; then
		echo "ERROR: Unable to find jar matching pattern '$pattern'" >&2
		exit 1
	fi
	printf '%s' "$jar"
}

# Uber jar bundles api/core/modsupport/legacy/cli + javassist/gson/snakeyaml.
UBER_JAR=$(resolve_jar "wurmmodloader-*.jar")

# =============================
# Interactive menu (no args passed)
# =============================
# Without start=<World> vanilla Wurm silently pops the Steam GUI, which offers
# no hint that modded boots require start=<folder>. This menu is the friendly
# default; power users pass start=<World> and skip straight to launch.
if [ $# -eq 0 ] && [ -t 0 ]; then
	find_worlds() {
		local d
		for d in */; do
			[ -f "$d/wurm.ini" ] && printf '%s\n' "${d%/}"
		done
	}

	while :; do
		echo
		echo "═══════════════════════════════════════════════════════════════"
		echo "  WurmModLoader — $(basename "$UBER_JAR" .jar | sed 's/^wurmmodloader-//')"
		echo "═══════════════════════════════════════════════════════════════"
		echo
		echo "  What would you like to do?"
		echo
		echo "    1) Start an existing world"
		echo "    2) Create a new world          (wurmmodloader-create-world)"
		echo "    3) Rebuild world databases     (wurmmodloader-rebuild-dbs)"
		echo "    4) Launch vanilla Wurm GUI     (no modloader — advanced)"
		echo "    5) Exit"
		echo
		echo "  Tip: skip this menu with:  ./wurmmodloader.sh start=<WorldName>"
		echo
		printf "Choice [1-5]: "
		read -r choice
		case "$choice" in
			1)
				mapfile -t WORLDS < <(find_worlds)
				if [ "${#WORLDS[@]}" -eq 0 ]; then
					echo
					echo "No worlds found in $PWD."
					echo "Run option 2 first to create a new world."
					continue
				fi
				echo
				echo "Available worlds:"
				for i in "${!WORLDS[@]}"; do
					printf "   %d) %s\n" "$((i+1))" "${WORLDS[$i]}"
				done
				echo
				printf "Pick a world [1-%d] (or blank to go back): " "${#WORLDS[@]}"
				read -r wpick
				[ -z "$wpick" ] && continue
				if ! [[ "$wpick" =~ ^[0-9]+$ ]] || [ "$wpick" -lt 1 ] || [ "$wpick" -gt "${#WORLDS[@]}" ]; then
					echo "Invalid selection."
					continue
				fi
				set -- "start=${WORLDS[$((wpick-1))]}"
				break
				;;
			2)
				exec ./wurmmodloader-create-world.sh
				;;
			3)
				exec ./wurmmodloader-rebuild-dbs.sh
				;;
			4)
				echo
				echo "Launching vanilla Steam GUI (modloader disabled)..."
				break
				;;
			5|q|Q|exit|quit)
				exit 0
				;;
			*)
				echo "Invalid choice."
				;;
		esac
	done
fi

CP="$UBER_JAR"
CP="$CP:server.jar"
CP="$CP:common.jar"

# Launch modloader
"$JAVA" "-Dworkdir=$PWD" "-Djava.library.path=$PWD/nativelibs" $LOGGING \
    -Xmn512M -Xms1g -Xmx4g -XX:+OptimizeStringConcat \
    -cp "$CP" com.garward.wurmmodloader.serverlauncher.ServerLauncher "$@"
