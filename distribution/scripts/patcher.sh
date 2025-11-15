#!/bin/bash
# WurmModLoader patcher script

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Patcher JAR is self-contained (shadow JAR with Javassist)
java -jar wurmmodloader-patcher-1.0.0-SNAPSHOT.jar "$@"
