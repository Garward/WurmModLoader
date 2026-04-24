# WurmModLoader

**Modern Modding Framework for Wurm Unlimited**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://adoptium.net/)
[![Wurm Version](https://img.shields.io/badge/Wurm-4596061+-green.svg)](https://store.steampowered.com/app/366220/Wurm_Unlimited/)
[![GitHub release (latest)](https://img.shields.io/github/v/release/Garward/WurmModLoader?label=release)
[![Downloads](https://img.shields.io/github/downloads/Garward/WurmModLoader/total?color=brightgreen)](https://github.com/Garward/WurmModLoader/releases)
WurmModLoader is a modern, extensible modding framework for Wurm Unlimited servers. It's a fork and complete modernization of the excellent [WurmServerModLauncher](https://github.com/ago1024/WurmServerModLauncher) by ago1024.

## 🎯 Project Status

**Version**: 0.10.1

The framework is in active use. See [`Architecture.MD`](Architecture.MD) for the module map and the boot sequence.

- ✅ **Core Infrastructure** — Legacy compatibility + modern runtime subsystems
- ✅ **Native Launcher** — GUI/headless launchers, patched bootstrap, diagnostic flags
- ✅ **Event Bus / Registries** — 130+ annotation-driven events with automatic legacy bridge
- ✅ **Bytecode patch pipeline** — 110+ patches with conflict detection and `--continue-on-patch-error`
- ✅ **Dual Interface Support** — Modern `@SubscribeEvent` and legacy listeners both work
- ✅ **Database backend SPI** — pluggable SQLite/MySQL/Postgres backends

## ✨ Features

### Current Highlights
- ✅ Backward compatibility with Ago-style mods (properties/JAR layout intact)
- ✅ Event Bus + Runtime Registries (bytecode, eventlogic, capability systems)
- ✅ EventLogic bundles (materials, swing speed, weapon timers, dual wield) with JSON registries
- ✅ Bytecode patch pipeline (`PatchManager`, conflict keys, `--force-bytecode-conflicts`, `--continue-on-patch-error`)
- ✅ Mod loading diagnostics (structured launcher logs, patch error archive, registry dumps)
- ✅ Native patched launcher (GUI/headless) + patcher scripts
- ✅ Gradle build, Java 17 runtime, Java 8 bytecode output

### Planned / Upcoming
- 🚧 Additional eventlogic modules (shield enchant resolvers, diagnostics command, capability-integrated combat DSL)
- 🚧 `mod.json` descriptors + dependency graph tooling
- 🚧 CLI & UI utilities (mod list, registry dumps, profile hot-reload, event bus inspector)
- 🚧 DatabaseOptimizer configuration + telemetry, optional Vector API accelerators
- 🚧 Expanded docs, sample data packs (Armoury-style profiles), migration guides

## 🚀 Quick Start

### Prerequisites

- **Java 17 or later** (GraalVM recommended, Adoptium works great)
- **Wurm Unlimited Dedicated Server** (version 4596061+)
- **Wurm server location** — defaults:
  - **Windows:** `C:\Program Files (x86)\Steam\steamapps\common\Wurm Unlimited Dedicated Server\`
  - **Linux:**   `~/.local/share/Steam/steamapps/common/Wurm Unlimited Dedicated Server/`

> **Windows users:** every shell command shown in this README has a Windows
> equivalent. Replace `./build.sh`, `./deploy.sh`, `./build-and-deploy.sh`,
> and `./gradlew` with `build.bat`, `deploy.bat`, `build-and-deploy.bat`,
> and `gradlew.bat` respectively. Run them from a regular `cmd.exe` /
> PowerShell window in the repo root. Set `WURM_SERVER_DIR` if your server
> lives somewhere other than the Steam default.

### Installation

1. **Download the latest release**
   ```bash
   wget https://github.com/garward/WurmModLoader/releases/latest/download/WurmModloader-Runtime-0.10.1.zip
   # Or build from source: ./build.sh   (runs ./gradlew clean build dist)
   ```

2. **Extract to your Wurm server directory**
   ```bash
   cd ~/.local/share/Steam/steamapps/common/Wurm\ Unlimited\ Dedicated\ Server/
   unzip /path/to/WurmModloader-Runtime-0.10.1.zip
   ```

3. **Launch your server**

   Instead of using `WurmServerLauncher`, use the patched launcher:

   **GUI Mode** (development/testing):
   ```bash
   ./wurmmodloader.sh
   ```
   Opens JavaFX GUI to select which server to start (Adventure, Creative, etc.)

   **Headless Mode** (production - FAST startup):
   ```bash
   ./wurmmodloader.sh start=Adventure
   ```
   Directly starts Adventure server, bypassing GUI. Perfect for:
   - Production servers
   - Automated restarts
   - Quick testing cycles
   _Diagnostics:_ append `--continue-on-patch-error` (or `--force-bytecode-conflicts`) to gather full patch reports without aborting startup.

   **Windows equivalent:**
   ```cmd
   wurmmodloader.bat start=Adventure
   ```

4. **Verify modloader is running**

   Check the logs for:
   ```
   INFO ModLoader initialization COMPLETE
   INFO Loaded N mods
   ```

### Adding Mods

**Important:** WurmModLoader is a framework only. Individual mods are distributed separately. Mods built against this framework ship as a self-contained folder you drop into `mods/`.

#### Required Directory Structure

Each mod lives in its own subfolder under `mods/`. The descriptor (`mod.properties`),
optional config (`mod.config`), and the JAR all sit *inside* that subfolder:

```
mods/
├── announcer/
│   ├── mod.properties        ← REQUIRED (descriptor: classname, classpath)
│   ├── mod.config            ← Optional mod-specific settings
│   └── announcer.jar         ← Mod code
├── bagofholding/
│   ├── mod.properties
│   ├── mod.config
│   └── bagofholding.jar
├── livemap/
│   ├── mod.properties
│   └── livemap.jar
...
```

⚠️ **Critical:** the JAR filename **must not contain a version suffix**. The loader matches by folder name — `announcer.jar`, not `announcer-0.47.jar`.

> **Legacy layout still works.** Older Ago-style mods that use top-level
> `mods/<modname>.properties` + `mods/<modname>/<modname>.jar` continue to
> load via the legacy bridge. New mods should use the self-contained subfolder
> layout above.

#### Installing a mod

1. Download the mod's release archive.
2. Extract it so its `<modname>/` folder ends up under `mods/`.
3. Confirm structure: `ls mods/<modname>/` should show `mod.properties` and `<modname>.jar`.

#### Quick Test

After adding mods, verify they load:
```bash
./wurmmodloader.sh start=Adventure
```

You should see: `INFO com.garward.wurmmodloader.serverlauncher.DelegatedLauncher main Loaded N mods`

### Configuration & EventLogic Profiles

Each mod's folder under `mods/` carries its own descriptor and config:
- `mod.properties` – Required loader metadata (`classname`, `classpath`)
- `mod.config` – Optional mod-specific settings

**EventLogic data** can be loaded from any JSON file (no code required):

```java
// Materials: tweak damage/decay/imp/skill modifiers
MaterialProfileRegistry.getInstance().loadFromJson(Paths.get("configs/materials.json"));

// Combat timing: swing floors, rarity bonuses
SwingSpeedRegistry.getInstance().load(Paths.get("configs/swing_profiles.json"));

// Weapon timers & dual wield policies
WeaponTimerRegistry.getInstance().load(Paths.get("configs/weapon_timers.json"));
DualWieldRegistry.getInstance().load(Paths.get("configs/dual_wield.json"));
```

Profiles are hot-loadable during bootstrap, so packs like Armoury can ship purely data-driven tweaks.

#### Debug Flags

WurmModLoader provides system property flags to enable detailed logging for debugging:

**Event Debug Logging** (`-DeventDebug=true`):
- Logs all framework events (CreatureDeath, CombatDamage, ItemTrade, etc.)
- High-frequency events (CombatDamage, SkillAdvance, ItemExamine) show rate-limited summaries: `[Event] CombatDamageEvent: fired 247 times in last 30 seconds`
- Low-frequency events (CreatureDeath, PlayerDeath, ItemTrade) show full details: `[Event] CreatureDeathEvent: victim=aged black bear (player=false), killer=Playername (player=true)`
- **Critical for verifying @SubscribeEvent handlers** - many game mechanics are invisible without logging

**Loot System Debug Logging** (`-DlootDebug=true`):
- Creature deaths and attacker tracking
- Damage dealt/taken statistics
- Top damage dealer and damage taker per fight
- Loot rule matching and execution

**Usage:**
```bash
# Start server with debug logging
./wurmmodloader.sh start=Adventure -DeventDebug=true -DlootDebug=true
```

## 📋 Requirements

### Runtime
- **Java 8 for server**
- **Wurm Unlimited Dedicated Server** (version 4596061+)

### Build (Development Only)
- **Gradle 8.x** (wrapper included, no separate installation needed)
- **JDK 17+** for building

## 🏗️ Architecture

WurmModLoader is designed as a modular framework:

```
wurmmodloader/
├── wurmmodloader-api/         # Public API (stable, semantic versioned)
├── wurmmodloader-core/        # Core loader, bytecode patches, eventlogic
├── wurmmodloader-legacy/      # Backward compatibility layer (Ago bridge)
├── wurmmodloader-modsupport/  # Mod development utilities
└── wurmmodloader-cli/         # Command-line tools
```

## 🧠 EventLogic Modules

WurmModLoader now ships opinionated eventlogic handlers so data packs can change gameplay without bytecode:

- **Materials** (`core/eventlogic/materials`) – `MaterialProfile`, registry + handler cover damage/decay/imp/repair bonuses, weapon stats, action & skill modifiers, spell power, etc. Load JSON profiles and they apply automatically.
- **Combat timing** (`core/eventlogic/combat/timing`) – `SwingSpeedAdjuster`, `WeaponTimerReset`, and `DualWieldScheduler` expose Armory-grade logic (swing floors, rarity bonuses, timer resets, off-hand scheduling) via registries.
- **Registries & JSON** – Each module exposes `load(Path json)` helpers so mods like Armoury/DuskCombat can ship pure data packs or override defaults at runtime.

Upcoming modules (shield enchant resolvers, diagnostics command, capability-backed combat DSL) will reuse the same pattern.

### Distribution Contents

When you extract the distribution ZIP, you get:

```
WurmServerLauncher/
├── wurmmodloader-api-0.10.1.jar
├── wurmmodloader-core-0.10.1.jar
├── wurmmodloader-modsupport-0.10.1.jar
├── wurmmodloader-legacy-0.10.1.jar
├── modlauncher.jar                  # Main launcher JAR (fat JAR)
├── javassist.jar                    # Bytecode manipulation
├── modloader-shared-0.18.jar        # HookManager infrastructure
├── wurmmodloader.sh                 # Linux/Mac launcher
├── wurmmodloader.bat                # Windows launcher
├── logging.properties               # Logger configuration
└── mods/                            # Drop mod folders here
    └── .gitkeep
```

**Note:** The distribution does NOT include any mods.

## 🛠 Diagnostics & Flags

- `--continue-on-patch-error` – keeps applying bytecode patches even if one fails, logging every error to `logs/wurmmodloader.*` and `debug/patch_errors.txt`.
- `--force-bytecode-conflicts` – overrides conflict-key checks (useful when intentionally stacking compatible instrumentation).
- Launcher logs now emit structured summaries (`Loaded X mod(s)`, eventlogic registration counts, registry snapshots) and the patcher captures fail-fast output for quick triage.

## 🔧 Building from Source

```bash
# Clone the repository
git clone https://github.com/garward/WurmModLoader.git
cd WurmModLoader

# Run tests
./gradlew testAll

# ⚠️ IMPORTANT: Always use distribution build for deployable artifacts
# Regular ./gradlew build creates stub JARs (not usable)
./gradlew dist

# Output: build/distributions/WurmModloader-Runtime-0.10.1.zip
```

**Build Notes:**
- ✅ `./gradlew dist` - Creates deployable ZIP with fat JARs (**REQUIRED**)



## 📖 Documentation

### Getting Started
- **[docs/getting-started/index.md](docs/getting-started/index.md)** — full walkthrough: write your first mod, deploy, debug.
- **[docs/guides/custom-map-setup.md](docs/guides/custom-map-setup.md)** — **Running a custom map?** Read this first. Fixes the "all NPC towns in the NW corner" papercut; covers the world-seed bootstrap and its `config/wurmmodloader-world-seed.yaml`.

### For Mod Developers
- **[docs/guides/extending-framework.md](docs/guides/extending-framework.md)** — adding a new event + bytecode patch, end to end.
- **[docs/guides/event-bus.md](docs/guides/event-bus.md)** — `@SubscribeEvent` mechanics, priority, cancellation.
- **[docs/guides/legacy-mod-compatibility.md](docs/guides/legacy-mod-compatibility.md)** — what the Ago bridge supports.
- **[docs/guides/ui-api-overview.md](docs/guides/ui-api-overview.md)** — building in-game UI with `UIWindow`.
- **[docs/guides/questions-api.md](docs/guides/questions-api.md)** — BML / question-menu prompts.
- **[docs/guides/database-backend-spi.md](docs/guides/database-backend-spi.md)** — pluggable Postgres/MySQL/SQLite backends.

### Reference
- **[Architecture.MD](Architecture.MD)** — module map + boot sequence.
- **[docs/reference/console-commands.md](docs/reference/console-commands.md)** — `#`-prefixed server console GM commands (safe shutdown, player/item/creature ops).
- **[docs/guides/troubleshooting.md](docs/guides/troubleshooting.md)** — log triage and common failure modes.
- **[NOTICE.md](NOTICE.md)** — attribution and licenses.

## 🆚 Differences from WurmServerModLauncher

| Feature | WurmServerModLauncher | WurmModLoader |
|---------|----------------------|---------------|
| Java Version | Java 8 | Java 17+ (compiles to Java 8 bytecode) |
| Build System | Maven | Gradle |
| Package | `org.gotti.wurmunlimited` | `com.garward.wurmmodloader` (modern), `org.gotti.*` (legacy compat) |
| Interface Support | Legacy only | Both modern (`com.garward.*`) and legacy (`org.gotti.*`) |
| Legacy Support | N/A | Full compatibility via legacy bridge |
| Registry System | String-based IDs | Namespaced ResourceLocations + runtime registries |
| Event System | Listener interfaces | Annotations + interfaces + eventlogic modules |
| Mod Descriptor | .properties | .properties + mod.json (planned) |

**All existing mods work without modification** - the legacy bridge ensures 100% backward compatibility. New mods can use the modern interface (`com.garward.wurmmodloader.modloader.interfaces.WurmServerMod`) as the preferred method.

## 🔄 Updating

### From Previous WurmModLoader Version

1. Backup your `mods/` directory
2. Download latest release
3. Extract to Wurm server directory (overwrite existing files)
4. run ./wurmmodloader.sh start=Worldnamehere

### From WurmServerModLauncher

WurmModLoader is a drop-in replacement:

1. Backup your `mods/` directory.
2. Remove old WurmServerModLauncher files.
3. Follow installation instructions above.
4. Copy your mods back to `mods/`. Old-style `mods/<modname>.properties` + `mods/<modname>/<modname>.jar` continues to load via the legacy bridge — no need to re-pack them into the new self-contained subfolder layout.

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

**Areas Where We Need Help:**
- Testing legacy mod compatibility
- Documentation and examples
- Code review and feedback on architecture
- Performance testing and optimization
- New Bytecode Patches

## 🙏 Acknowledgments

This project is a fork and modernization of **WurmServerModLauncher** created by [ago1024](https://github.com/ago1024).

**Original Contributors:**
- ago1024 (Alexander Gottwald)
- bdew
- Darius Tumas
- Dynamic Revolutions
- Tyoda

See [NOTICE.md](NOTICE.md) for complete attribution.

Without ago1024's pioneering work, the Wurm Unlimited modding community would not exist as it does today. This project aims to honor that legacy by bringing modern tooling and best practices to the ecosystem.

## 📜 License

MIT License - See [LICENSE](LICENSE) for details.

This project includes:
- Original work Copyright (c) 2016-2024 ago1024 and contributors
- Modified work Copyright (c) 2025 Garward

## 🔗 Links

- **Original Project**: [WurmServerModLauncher](https://github.com/ago1024/WurmServerModLauncher)
- **Wurm Unlimited**: [Steam Page](https://store.steampowered.com/app/366220/Wurm_Unlimited/)
- **Issues**: [GitHub Issues](https://github.com/garward/WurmModLoader/issues)
- **Discussions**: [GitHub Discussions](https://github.com/garward/WurmModLoader/discussions)

## ❓ FAQ

**Q: Should I use WurmModLoader or WurmServerModLauncher?**
A: WurmModLoader if you want the modern event API, the bytecode patch pipeline, the database backend SPI, and the legacy bridge that runs Ago-style mods unmodified. Stick with the original WurmServerModLauncher if your existing setup works and you don't need any of that.

**Q: Will my existing mods work with WurmModLoader?**
A: Yes — the legacy bridge runs Ago-style mods (`org.gotti.wurmunlimited.modloader.interfaces.*`) without modification.

**Q: Why does it say "Loaded 0 mods"?**
A: Either no mod folders exist under `mods/`, or the descriptors are missing. Each mod must have a `mod.properties` (new layout, inside `mods/<modname>/`) **or** a `mods/<modname>.properties` (legacy layout). See the [Troubleshooting](#-troubleshooting) section.

**Q: Where do I get mods?**
A: Download individual mod releases from their authors. The framework doesn't bundle mods.

**Q: Do I need to update my mods?**
A: No, your mods work as-is. However, you can optionally modernize them to use new features:
- **Modern interface:** `import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;` (preferred)
- **Event annotations:** `@SubscribeEvent` for cleaner event handling
- **Registries:** Namespaced ResourceLocations for conflict-free IDs

**Q: What Java version do I need?**
A: Java 17+ to run. Mods can still target Java 8 bytecode for compatibility.

**Q: Can I use this with existing server saves?**
A: Yes! WurmModLoader doesn't change world data or save formats.

**Q: Why fork instead of contributing to the original?**
A: The modernization involves fundamental architectural changes (Java 17, Gradle, event system) that would break existing setups. A fork allows us to modernize while the original remains stable for current users.

**Q: How can I help?**
A: Test with your mods, report issues, contribute documentation, or sponsor development!

**Q: Where do logs go?**
A: Server console output shows mod loading. Look for lines like `INFO Loaded X mods`.

**Q: Can I run multiple worlds with different mods?**
A: Yes! Each world folder (Adventure, Creative, etc.) can have its own mods configuration via wurm.ini.

## 🔧 Troubleshooting

### Common Issues

This section covers the most common problems encountered when migrating mods to WurmModLoader.

#### Mod Not Detected During Discovery

**Symptoms:**
- Mod folder exists but mod never appears in discovery logs
- Server starts but a specific mod is completely skipped

**Causes & Solutions:**

1. **Case sensitivity mismatch** (Linux only — most common)
   ```
   ❌ WRONG: mods/MyMod/mod.properties      ← capitalized folder
   ✅ RIGHT: mods/mymod/mod.properties      ← lowercase folder
   ```
   **Fix:** lowercase the folder name. The `classname=` value inside the descriptor is unaffected.

2. **Versioned JAR name**
   ```
   ❌ WRONG: mods/armoury/armoury-4.1.0.jar   ← version in filename
   ✅ RIGHT: mods/armoury/armoury.jar         ← matches folder name
   ```
   The loader matches `<folder>/<folder>.jar`. **Fix:** rename the JAR, or set `classpath=armoury-4.1.0.jar` in `mod.properties`.

3. **Descriptor missing or in the wrong place**
   - **New layout:** `mods/<modname>/mod.properties`
   - **Legacy layout:** `mods/<modname>.properties`
   - Either works. **Both** is fine. **Neither** = no load.

4. **Obsolete dependencies**
   ```properties
   ❌ depend.import=SinduskLibrary   # no longer exists
   ```
   Many old library deps (SinduskLibrary, etc.) are folded into `wurmmodloader-core` — remove them.

#### ClassCastException on Mod Load

**Symptoms:**
```
SEVERE: class mod.sin.armoury.ArmouryModMain
java.lang.ClassCastException: class mod.sin.armoury.ArmouryModMain
```

**Cause:** Mod implements one interface but a stale loader cast expects the other.

**Solution:**
Both interfaces work — pick either:
- **Modern (preferred for new mods):** `import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;`
- **Legacy (for Ago-style mods):** `import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;`

If you still see this on a current build, file an issue.

#### Complete Mod Migration Checklist

When migrating a mod from legacy modloader or fixing detection issues:

- [ ] **Descriptor exists** — either `mods/<modname>/mod.properties` (new layout) or `mods/<modname>.properties` (legacy layout)
- [ ] **Directory name** is lowercase and matches the JAR's basename
- [ ] **JAR name** has no version suffix (`armoury.jar`, not `armoury-4.1.0.jar`)
- [ ] **`classpath=` in the descriptor** points to the JAR (relative to the mod folder for new layout, relative to `mods/` for legacy)
- [ ] **Remove obsolete dependencies** (SinduskLibrary, etc.)
- [ ] **Test load:** verify the `Loaded N mods` line shows your mod count

**Example — new layout:**
```
mods/
└── armoury/
    ├── mod.properties           # classname=..., classpath=armoury.jar
    └── armoury.jar
```

**`mod.properties`:**
```properties
classname=mod.sin.armoury.ArmouryModMain
classpath=armoury.jar
```

### "Loaded 0 mods" or Mods Not Loading

**Symptom:** Server starts but no mods load. Log shows `Loaded 0 mods`.

**Cause:** Each mod's descriptor is missing or in the wrong place.

**Solution:**
1. Each mod folder under `mods/` must contain a `mod.properties`:
   ```bash
   ls mods/<modname>/
   # Should show: mod.properties  <modname>.jar  (and optionally mod.config)
   ```
2. Legacy mods may instead use the older layout (`mods/<modname>.properties` + `mods/<modname>/<modname>.jar`) — both are supported.
3. A minimal `mod.properties` looks like:
   ```properties
   classname=com.example.mymod.MyMod
   classpath=mymod.jar
   ```

**Technical Details:** ModLoaderShared marks mods as "ondemand" when no descriptor is found. The DependencyResolver then prunes them from the load list — `.properties`/`mod.properties` is the explicit "install me" signal.

### Class Not Found Errors

**Symptom:** Errors like `ClassNotFoundException: org.gotti.wurmunlimited.mods.announcer.AnnounceMod`

**Cause:** Incorrect `classname` in `.properties` file.

**Solution:**
1. Verify class name with:
   ```bash
   javap -cp mods/announcer/announcer.jar org.gotti.wurmunlimited.mods.announcer.AnnounceMod
   ```
2. Update `.properties` file with correct class name

### Server Fails to Start After Patching

**Symptom:** Server won't start after running patcher.

**Solution:**
1. Verify `server.jar` wasn't corrupted
2. Restore from Steam: Validate game files
3. Re-run patcher after restoration
4. Check Java version: `java -version` (need 17+)

### "Class is Frozen" Errors

**Symptom:** Errors like `com.wurmonline.server.creatures.Creature class is frozen` or `com.wurmonline.server.items.Item class is frozen`

**Cause:** Mod is importing or loading game classes during `preInit()`, which freezes them before the framework can set up callbacks.

**Solution:** Move game class usage to event handlers. See the preInit() Q&A below.

## 🎓 Mod Development Q&A

### When should I use preInit() vs event handlers?

**TL;DR:** Most mods should use events. Only use `preInit()` for config loading and reflection hook registration.

#### ✅ Valid preInit() Uses

**1. Config file loading (no game class imports):**
```java
@Override
public void preInit() {
    config = MyConfig.load("mymod.config");  // ← Safe
    logger.info("Mod initializing...");
}
```

**2. Reflection hook registration (for advanced mods):**
```java
@Override
public void preInit() {
    // Register reflection hooks BEFORE classes freeze
    HookManager.getInstance().registerHook(
        "com.wurmonline.server.items.Item",
        "getContainerVolume",
        "()I",
        invocationHandlerFactory
    );
}
```

**3. Data structure initialization:**
```java
@Override
public void preInit() {
    myCache = new HashMap<>();
    customRegistry = new ArrayList<>();
}
```

#### ❌ What NOT to do in preInit()

**1. Don't import or use game classes:**
```java
@Override
public void preInit() {
    Creature creature = ...;  // ❌ BAD! Loads and freezes Creature class
    Item item = ...;          // ❌ BAD! Loads and freezes Item class
}
```

**2. Don't call methods that import game classes:**
```java
@Override
public void preInit() {
    // ❌ BAD! If MyIntegration imports Creature, this freezes it
    MyIntegration.registerStuff();
}
```

**3. Don't use Javassist for bytecode patches:**
```java
@Override
public void preInit() {
    // ❌ BAD! Framework's job, not yours
    CtClass ctCreature = classPool.get("com.wurmonline.server.creatures.Creature");
    // ...
}
```

**4. Don't register items/creatures/content:**
```java
@Override
public void preInit() {
    // ❌ BAD! Use ItemTemplatesCreatedEvent instead
    ItemTemplateBuilder builder = new ItemTemplateBuilder("myitem");
}
```

#### ✅ The Modern Pattern

Most mods should look like this:

```java
public class MyMod implements WurmServerMod, PreInitable, Configurable {

    private MyConfig config;

    @Override
    public void configure(Properties properties) {
        // Load config from .properties file
    }

    @Override
    public void preInit() {
        // ONLY if needed:
        // - Load additional config files
        // - Register reflection hooks (HookManager.registerHook)
        // - Initialize data structures
        // NO GAME CLASS IMPORTS!
        config = MyConfig.load("mymod.config");
    }

    @SubscribeEvent
    public void onItemTemplatesCreated(ItemTemplatesCreatedEvent event) {
        // ✅ Register custom items here
        ItemTemplate sword = new ItemTemplateBuilder(
            new ResourceLocation("mymod", "magic_sword")
        ).name("Magic Sword", "Magic Swords", "A powerful blade")
         .build();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // ✅ Do initialization that needs game classes
        // ✅ Register integrations with other systems
        MyIntegration.registerStuff();  // Now safe!
        logger.info("Mod fully initialized!");
    }

    @SubscribeEvent
    public void onCreatureDeath(CreatureDeathEvent event) {
        // ✅ Handle game events
        Creature victim = event.getVictim();  // Safe here!
        // Do stuff...
    }
}
```

#### ♻️ Hot-Reloading Config (`#reloadmods`)

GMs can run `#reloadmods` from the server console or in-game (`#reloadmods`
for all mods, `#reloadmods <modname>` for one). On reload the framework
re-reads each mod's `.properties` / `.config` from disk and re-fires
`Configurable.configure(Properties)`. Mods that only consume Properties get
this for free.

For mods that load extra files (YAML, JSON, CSV, …) outside the Properties
pipeline — the `MyConfig.load("mymod.config")` line in the example above is a
typical case — implement the optional `Reloadable` interface so the custom
load runs again on `#reloadmods`:

```java
import com.garward.wurmmodloader.modloader.interfaces.Reloadable;

public class MyMod implements WurmServerMod, PreInitable, Configurable, Reloadable {

    private MyConfig config;

    @Override
    public void preInit() {
        config = MyConfig.load("mymod.config");   // boot-time load
    }

    @Override
    public void onReload() {
        // Fired AFTER configure(properties) has been re-applied with fresh
        // values from mod.properties + mod.config.
        config = MyConfig.load("mymod.config");   // re-load on #reloadmods
    }
}
```

**Idempotency caveat.** Reload does not tear the mod down — event
subscriptions, registered items, and bytecode patches all stay in place.
If your `configure()` or `onReload()` mutates collections, reset them
before repopulating (e.g. `routes.clear()` then re-parse), and avoid
re-registering listeners you registered at boot.

### Why this restriction?

**Technical explanation:**

When Java loads a class, it processes ALL imports at the top of the file. If your `preInit()` calls a method in a class that imports `Creature`, Java loads the `Creature` class. Once loaded, Javassist "freezes" it, preventing the framework from adding callbacks.

**The initialization sequence:**
1. Your mod's `preInit()` runs
2. Framework's `ModSupport.init()` adds callbacks to game classes (Item, Creature, Action)
3. Framework's `SystemBootstrap` applies bytecode patches (freezes classes)
4. Event handlers run (safe to use game classes now)

If you load game classes in step 1, they freeze before step 2, causing "class is frozen" errors.

### What if I need game classes during initialization?

**Use the ServerStartedEvent:**

```java
@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) {
    // Now it's safe to import/use any game classes
    Creature creature = ...;
    Item item = ...;
    MetalLootIntegration.registerLootRules();  // Even if this imports Creature
}
```

Event handlers run AFTER the framework sets everything up, so all game classes are safe to use.

### Quick Reference

| Need to... | Use... |
|------------|--------|
| Load config files | `preInit()` |
| Register reflection hooks | `preInit()` |
| Initialize data structures | `preInit()` |
| Register custom items | `@SubscribeEvent` on ItemTemplatesCreatedEvent |
| Use game classes (Item, Creature, etc.) | `@SubscribeEvent` on ServerStartedEvent |
| Handle game events | `@SubscribeEvent` on specific events |
| Integrate with other systems | `@SubscribeEvent` on ServerStartedEvent |
| Re-read custom config files on `#reloadmods` | Implement `Reloadable.onReload()` |
| Write bytecode patches | **Don't!** Framework handles this |

## 💬 Support

- **Issues**: [GitHub Issues](https://github.com/garward/WurmModLoader/issues)
- **Discussions**: [GitHub Discussions](https://github.com/garward/WurmModLoader/discussions)
- **Original Project**: For general Wurm modding questions, see [WurmServerModLauncher](https://github.com/ago1024/WurmServerModLauncher)

## 🧪 Testing Status

**Tested configurations:**
- ✅ Linux (Arch, kernel 6.x)
- ✅ Wurm Unlimited version 4596061+
- ✅ Java 8 (bundled JRE) and Java 17+ (OpenJDK)
- ✅ Native launcher (WurmServerLauncher-patched) and pure-Java launcher (`wurmmodloader.sh`)
- ✅ Headless server operation
- ✅ Modern (`com.garward.wurmmodloader.*`) and legacy (`org.gotti.wurmunlimited.*`) mod interfaces

**Known gaps:**
- Windows testing is light — reports welcome.
- macOS untested.

---

**Built with ❤️ for the Wurm Unlimited community**
