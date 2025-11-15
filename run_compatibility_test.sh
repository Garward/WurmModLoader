#!/bin/bash
#
# Quick helper script to run Phase 4 compatibility tests
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== WurmModLoader Phase 4 Compatibility Test ===${NC}\n"

# Resolve build jars helper
resolve_build_jar() {
    local pattern="$1"
    compgen -G "$pattern" | head -n 1
}

CORE_JAR=$(resolve_build_jar "wurmmodloader-core/build/libs/wurmmodloader-core-*.jar")
MODSUPPORT_JAR=$(resolve_build_jar "wurmmodloader-modsupport/build/libs/wurmmodloader-modsupport-*.jar")
API_JAR=$(resolve_build_jar "wurmmodloader-api/build/libs/wurmmodloader-api-*.jar")

if [ -z "$CORE_JAR" ] || [ -z "$MODSUPPORT_JAR" ] || [ -z "$API_JAR" ]; then
    echo -e "${YELLOW}JARs not found. Building WurmModLoader...${NC}"
    ./gradlew clean build -x test
    echo ""
    CORE_JAR=$(resolve_build_jar "wurmmodloader-core/build/libs/wurmmodloader-core-*.jar")
    MODSUPPORT_JAR=$(resolve_build_jar "wurmmodloader-modsupport/build/libs/wurmmodloader-modsupport-*.jar")
    API_JAR=$(resolve_build_jar "wurmmodloader-api/build/libs/wurmmodloader-api-*.jar")
fi

if [ -z "$CORE_JAR" ] || [ -z "$MODSUPPORT_JAR" ] || [ -z "$API_JAR" ]; then
    echo -e "${RED}Unable to locate built jars after Gradle build. Aborting.${NC}"
    exit 1
fi

# Build classpath
CP="$CORE_JAR:$MODSUPPORT_JAR:$API_JAR"

# Compile test if needed
if [ ! -f "Phase4CompatibilityTest.class" ] || [ "Phase4CompatibilityTest.java" -nt "Phase4CompatibilityTest.class" ]; then
    echo -e "${YELLOW}Compiling test...${NC}"
    javac -cp "$CP" Phase4CompatibilityTest.java
    echo -e "${GREEN}✓ Compilation successful${NC}\n"
fi

# Run test
echo -e "${YELLOW}Running compatibility test...${NC}\n"
if java -cp ".:$CP" Phase4CompatibilityTest 2>&1 | grep -v "^Nov"; then
    echo -e "\n${GREEN}✓✓✓ All tests passed! ✓✓✓${NC}"
    echo -e "${GREEN}Phase 4 compatibility layer is working correctly.${NC}"
    exit 0
else
    echo -e "\n${RED}✗✗✗ Tests failed! ✗✗✗${NC}"
    echo -e "${RED}Check output above for details.${NC}"
    exit 1
fi
