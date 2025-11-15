# WurmModLoader Installation Guide

Quick and simple installation for server hosts.

## Requirements

- Wurm Unlimited Dedicated Server (from Steam)
- Java 8 (bundled with Wurm) or Java 17+

## Installation Steps

### 1. Extract WurmModLoader

Extract `wurmmodloader-X.X.X.zip` to your Wurm Unlimited Dedicated Server directory.

**Example:**
```bash
cd "/path/to/Wurm Unlimited Dedicated Server"
unzip wurmmodloader-1.0.0-SNAPSHOT.zip
```

### 2. Create Database Symlink

Create a symlink so the server can find your world's databases:

```bash
ln -sfn Adventure/sqlite sqlite
```

*Note: If you use a different world name (Creative, etc.), adjust accordingly.*

### 3. Patch the Server

Run the patcher to inject the mod loader:

```bash
./patcher.sh
```

**What this does:**
- Backs up `server.jar` to `server.jar.bak` (automatically, safely)
- Injects WurmModLoader into the server
- Creates `WurmServerLauncher-patched`

### 4. Add Mods

Place your mods in the `mods/` directory. Each mod needs:
- A `.properties` file (in `mods/` root)
- A mod directory with the JAR file (in `mods/modname/`)

**Example structure:**
```
mods/
├── oversizedclub.properties
├── oversizedclub/
│   └── oversizedclub-1.0.0.jar
├── duskombat.properties
└── duskombat/
    └── DUSKombat.jar
```

### 5. Run the Server

**GUI Mode** (development/testing - allows server selection):

```bash
./WurmServerLauncher-patched
```

Opens JavaFX GUI where you can select which server to start (Adventure, Creative, etc.)

**Headless Mode** (production/automated - fast startup):

```bash
./WurmServerLauncher-patched start=Adventure
```

Starts the Adventure server directly, bypassing GUI. Super fast startup, perfect for:
- Production servers
- Automated restarts
- Quick testing cycles

**Advanced: Java launcher for more control:**

```bash
./wurmmodloader.sh
```

## Verification

When the server starts, you should see:

```
ModLoader initialization COMPLETE!
Loaded XX mods
All mod hooks are installed and ready
```

## Troubleshooting

### "Loaded 0 mods"

**Cause:** Missing `.properties` files in `mods/` root.

**Fix:** Ensure each mod has its `.properties` file in `mods/` (not inside the mod directory).

### Database Errors

**Cause:** Missing or incorrect database symlink.

**Fix:** Verify symlink exists: `ls -la sqlite`

Should show: `sqlite -> Adventure/sqlite`

### "ClassNotFoundException"

**Cause:** server.jar or common.jar missing/corrupted.

**Fix:** Verify files through Steam (Right-click → Properties → Installed Files → Verify)

## Rollback

To remove WurmModLoader:

1. Delete `WurmServerLauncher-patched`
2. Restore backup: `mv server.jar.bak server.jar`
3. Use vanilla launcher: `./WurmServerLauncher`

## Support

- GitHub Issues: https://github.com/garward/WurmModLoader/issues
- Documentation: See `docs/` directory
- Original project: https://github.com/ago1024/WurmServerModLauncher

## Phase 6: Modern Event System

WurmModLoader includes a modern annotation-driven event system! See:
- `docs/EVENT_BUS_GUIDE.md` - Complete API reference
- `docs/MIGRATION_GUIDE.md` - Convert old mods
- `examples/oversizedclub/` - Working example mod

**Quick example:**
```java
public class MyMod implements WurmServerMod {
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        logger.info("Server started with modern events!");
    }
}
```

100% backward compatible with old mods - they work without any changes!
