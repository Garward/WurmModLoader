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

API_JAR=$(resolve_jar "wurmmodloader-api-*.jar")
CORE_JAR=$(resolve_jar "wurmmodloader-core-*.jar")
MODSUPPORT_JAR=$(resolve_jar "wurmmodloader-modsupport-*.jar")
LEGACY_JAR=$(resolve_jar "wurmmodloader-legacy-*.jar")

# Build classpath (order matters: API -> Core -> ModSupport -> Legacy)
CP="$API_JAR"
CP="$CP:$CORE_JAR"
CP="$CP:$MODSUPPORT_JAR"
CP="$CP:$LEGACY_JAR"
CP="$CP:javassist.jar"
CP="$CP:gson.jar"
CP="$CP:snakeyaml-2.2.jar"
CP="$CP:server.jar"
CP="$CP:common.jar"

# Launch modloader
"$JAVA" "-Dworkdir=$PWD" "-Djava.library.path=$PWD/nativelibs" $LOGGING \
    -Xmn256M -Xms512m -Xmx2048m -XX:+OptimizeStringConcat -XX:+AggressiveOpts \
    -cp "$CP" com.garward.wurmmodloader.serverlauncher.ServerLauncher "$@"
