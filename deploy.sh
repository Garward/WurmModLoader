#!/usr/bin/env bash
set -e

# === CONFIG ===
PROJECT_DIR="$HOME/Scripts/Games/WurmUnlimited/WurmModLoader"
SERVER_DIR="$HOME/.local/share/Steam/steamapps/common/Wurm Unlimited Dedicated Server"
DEPLOY_DIR="$SERVER_DIR/deploy"

# === BUILD ===
cd "$PROJECT_DIR"
echo "🛠️  Building full WurmModLoader distribution..."
./gradlew clean build dist

# === WAIT A BIT ===
echo "⌛ Waiting for build artifacts..."
sleep 5

# === DEPLOY ===
echo "🚚 Copying distribution to $DEPLOY_DIR ..."
mkdir -p "$DEPLOY_DIR"
cp -r "$PROJECT_DIR/distribution/"* "$DEPLOY_DIR/"

# Optional: confirm what was copied
echo "✅ Deployed files:"
ls -lh "$DEPLOY_DIR" | grep -E 'jar|zip'

# === OPTIONAL AUTO-LAUNCH ===
# echo "🧩 Launching patched server..."
# cd "$SERVER_DIR"
# ./WurmServerLauncher-patched start=Adventure

echo "🎉 Build + deploy complete!"
