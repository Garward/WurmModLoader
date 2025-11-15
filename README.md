# WurmModLoader

**Modern Modding Framework for Wurm Unlimited**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://adoptium.net/)
[![Wurm Version](https://img.shields.io/badge/Wurm-4596061+-green.svg)](https://store.steampowered.com/app/366220/Wurm_Unlimited/)
[![GitHub release (latest)](https://img.shields.io/github/v/release/Garward/WurmModLoader?label=release)
[![Downloads](https://img.shields.io/github/downloads/Garward/WurmModLoader/total?color=brightgreen)](https://github.com/Garward/WurmModLoader/releases)
WurmModLoader is a modern, extensible modding framework for Wurm Unlimited servers. It's a fork and complete modernization of the excellent [WurmServerModLauncher](https://github.com/ago1024/WurmServerModLauncher) by ago1024.

## 🎯 Project Status

**Current Phase**: Phase 7 – Event Logic & Diagnostics ✅ COMPLETE
**Version**: 0.9.0
**Stability**: ✅ **STABLE** - Release Candidate (Nov 11, 2025)

✅ **Core Infrastructure Complete** – Legacy compatibility + new runtime subsystems
✅ **Native Launcher & Patcher** – GUI/headless launchers, patched bootstrap, diagnostic flags
✅ **Event Bus / Registries** – Annotation-driven events with automatic legacy bridge + runtime registries
✅ **EventLogic Modules** – Data-driven materials + combat timing helpers ready for Armory/DuskCombat workloads
✅ **Diagnostic Tooling** – Structured logs, patch error dumps, `--continue-on-patch-error` for sweep runs
✅ **Dual Interface Support** – Both modern and legacy interfaces work seamlessly (v0.9.0+)

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
- **Wurm server location**: Usually at `~/.local/share/Steam/steamapps/common/Wurm Unlimited/WurmServerLauncher/`

### Installation

1. **Download the latest release**
   ```bash
   wget https://github.com/garward/WurmModLoader/releases/latest/download/wurmmodloader-1.0.0-SNAPSHOT.zip
   # Or build from source: ./gradlew dist
   ```

2. **Extract to your Wurm server directory**
   ```bash
   cd ~/.local/share/Steam/steamapps/common/Wurm\ Unlimited/WurmServerLauncher/
   unzip /path/to/WurmModloader-Server-0.9.0
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

   **Alternative: Java launcher:**
   ```bash
   # Linux/Mac
   ./wurmmodloader.sh start=Adventure

   # Windows
   wurmmodloader.bat start=Adventure
   ```

5. **Verify modloader is running**

   Check the logs for:
   ```
   INFO ModLoader version 1.0.0-SNAPSHOT
   ```

### Adding Mods

**Important:** WurmModLoader is a framework only. Individual mods must be downloaded separately from their respective releases (e.g., from the [original WurmServerModLauncher releases](https://github.com/ago1024/WurmServerModLauncher/releases)).

#### Installation Steps

1. **Download mod releases** - Each mod is a separate ZIP file (e.g., `announcer-0.47.zip`)
2. **Extract to mods directory** - Extract the mod ZIP into your `mods/` folder
3. **Verify structure** - Each mod needs both a `.properties` file and its JAR

#### Required Directory Structure

```
mods/
├── announcer.properties          ← REQUIRED (in root)
├── announcer.config              ← Optional mod configuration
├── announcer/
│   └── announcer.jar             ← Mod code
├── bagofholding.properties       ← REQUIRED (in root)
├── bagofholding.config           ← Optional
├── bagofholding/
│   └── bagofholding.jar
...
```

⚠️ **Critical:** The `.properties` file in `mods/` root is MANDATORY. Without it, the mod will be marked as "ondemand" and automatically pruned from the load list, resulting in "Loaded 0 mods".

#### Extracting from Mod Releases

When you download a mod release ZIP (e.g., `announcer-0.47.zip`), it contains:
```
announcer-0.47/
└── mods/
    ├── announcer.properties      ← Extract this
    ├── announcer.config          ← Extract this
    └── announcer/
        └── announcer.jar         ← Extract this
```

Extract the contents of the `mods/` folder from the ZIP into your server's `mods/` directory.

#### Quick Test

After adding mods, verify they load:
```bash
./wurmmodloder.sh start=Adventure
```

You should see: `INFO com.garward.wurmmodloader.serverlauncher.DelegatedLauncher main Loaded X mods`

### Configuration & EventLogic Profiles

Traditional mods still live in `mods/`:
- `modname.properties` – Required loader metadata (mod class + classpath)
- `modname.config` – Optional mod-specific settings

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
├── wurmmodloader-core/        # Core loader implementation
├── wurmmodloader-legacy/      # Backward compatibility layer
├── wurmmodloader-modsupport/  # Mod development utilities
├── wurmmodloader-patcher/     # Bytecode patcher
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
├── wurmmodloader-api-1.0.0-SNAPSHOT.jar
├── wurmmodloader-core-1.0.0-SNAPSHOT.jar
├── wurmmodloader-modsupport-1.0.0-SNAPSHOT.jar
├── wurmmodloader-legacy-1.0.0-SNAPSHOT.jar
├── wurmmodloader-patcher-1.0.0-SNAPSHOT.jar
├── modlauncher.jar                  # Main launcher JAR (fat JAR)
├── javassist.jar                    # Bytecode manipulation
├── modloader-shared-0.18.jar        # HookManager infrastructure
├── wurmmodloader.sh                 # Linux/Mac launcher
├── wurmmodloader.bat                # Windows launcher
├── patcher.sh / patcher.bat         # One-time patcher
├── logging.properties               # Logger configuration
└── mods/                            # Download mods separately!
    └── .gitkeep
```

**Note:** The distribution does NOT include any mods. Download individual mods from their releases (e.g., [WurmServerModLauncher releases](https://github.com/ago1024/WurmServerModLauncher/releases)).

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

# Output: build/distributions/wurmmodloader-1.0.0-SNAPSHOT.zip
```

**Build Notes:**
- ✅ `./gradlew dist` - Creates deployable ZIP with fat JARs (**REQUIRED**)



## 📖 Documentation

### Getting Started
- **[README.md](README.md)** - This file (installation and quick start)
- **[NOTICE.md](NOTICE.md)** - Attribution and licenses

### Technical Documentation
- **[MOD_LOADING_DISCOVERY_REPORT.md](MOD_LOADING_DISCOVERY_REPORT.md)** - Deep dive into mod loading architecture
- **[MODERNIZATION_PLAN_AUDIT.md](MODERNIZATION_PLAN_AUDIT.md)** - Critical analysis of design gaps
- **[WURMMODLOADER_MODERNIZATION_PLAN.md](WURMMODLOADER_MODERNIZATION_PLAN.md)** - Complete technical roadmap

### Troubleshooting
- **[FAQ](#-faq)** - Common questions and solutions
- **[Troubleshooting](#-troubleshooting)** - Common issues and fixes

### For Mod Developers

Documentation is under active development. See the modernization plan for upcoming features.

**Current Resources:**
- Original [WurmServerModLauncher Wiki](https://github.com/ago1024/WurmServerModLauncher/wiki)
- Legacy mod examples in the original repository

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

1. Backup your `mods/` directory
2. Remove old WurmServerModLauncher files
3. Follow installation instructions above
4. Copy your mods back to `mods/` directory
5. Mods will work without modification

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

## 📊 Development Roadmap

### Phase 0: Foundation ✅ COMPLETE
- ✅ Repository setup and legal foundation
- ✅ Gradle build system migration
- ✅ Distribution infrastructure
- ✅ Documentation framework

### Phase 1: Build System (Weeks 3-4)
- ✅ Maven → Gradle migration
- ✅ Java 17 toolchain setup
- ✅ Module configuration
- 🚧 CI/CD pipeline

### Phase 2: Package Rename (Weeks 5-8)
- Rename from `org.gotti.wurmunlimited` to `com.garward.wurmmodloader`
- API/implementation split
- Legacy bridge implementation

### Phase 3: Legacy Compatibility Layer (Weeks 9-12)
- Transparent interception of old API calls
- Property-based mod loading support
- Backward compatibility testing

### Phase 4: Registry System (Weeks 13-16)
- ResourceLocation (namespaced identifiers)
- Generic Registry<T> implementation
- IdAllocator for database persistence
- ItemTemplate/CreatureTemplate integration

### Phase 5-10
See [WURMMODLOADER_MODERNIZATION_PLAN.md](WURMMODLOADER_MODERNIZATION_PLAN.md) for complete timeline.

## ❓ FAQ

**Q: Should I use WurmModLoader or WurmServerModLauncher?**
A: ✅ **WurmModLoader v0.9.0 is now a stable release candidate!** (Nov 11, 2025). Successfully running 17+ mods with full backward compatibility and modern interface support. Ready for testing - recommended for new servers and brave existing servers. 1.0.0 release coming after wider community testing.

**Q: Will my existing mods work with WurmModLoader?**
A: Yes! Full backward compatibility is a core design goal. All existing mods work without modification through the legacy bridge.

**Q: Why does it say "Loaded 0 mods"?**
A: This usually means the `.properties` files are missing from `mods/` root. See the [Troubleshooting](#-troubleshooting) section below.

**Q: Where do I get mods?**
A: Download individual mod releases from [WurmServerModLauncher releases](https://github.com/ago1024/WurmServerModLauncher/releases). The framework doesn't include mods.

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
- Server starts but specific mod is completely skipped
- No "Found legacy subfolder mod: modname" message in logs

**Causes & Solutions:**

1. **Case Sensitivity Mismatch** ⭐ Most Common
   ```
   ❌ WRONG:
   mods/
   ├── mymod.properties          ← lowercase
   └── MyMod/                    ← Capital M
       └── mymod.jar

   ✅ CORRECT:
   mods/
   ├── mymod.properties          ← lowercase
   └── mymod/                    ← lowercase
       └── mymod.jar
   ```
   **Fix:** Rename directory to match properties file name exactly (case-sensitive).

2. **Versioned JAR Name**
   ```
   ❌ WRONG:
   mods/
   ├── armoury.properties
   └── armoury/
       └── armoury-4.1.0.jar     ← versioned name

   ✅ CORRECT:
   mods/
   ├── armoury.properties
   └── armoury/
       └── armoury.jar           ← exact match
   ```
   Legacy discovery expects exact name match: `modname/modname.jar`

   **Fix:** Rename JAR to match mod name without version suffix, or update classpath in .properties:
   ```properties
   classpath=armoury/armoury.jar
   ```

3. **Obsolete Dependencies**
   ```properties
   ❌ WRONG:
   depend.import=SinduskLibrary   # No longer exists

   ✅ CORRECT:
   # depend.import=SinduskLibrary  # Removed - now in wurmmodloader-core
   ```
   **Fix:** Remove dependencies on legacy libraries (SinduskLibrary, etc.) - functionality is now in core.

#### ClassCastException on Mod Load

**Symptoms:**
```
SEVERE: class mod.sin.armoury.ArmouryModMain
java.lang.ClassCastException: class mod.sin.armoury.ArmouryModMain
```

**Cause:** Mod implements modern interface but modloader expects legacy interface (fixed in v0.9.0).

**Solution (v0.9.0+):**
✅ Both interfaces now work! Use either:
- **Modern (preferred):** `import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;`
- **Legacy (backward compat):** `import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;`

**For older versions:**
Change mod to use legacy interface temporarily, then upgrade WurmModLoader.

#### Complete Mod Migration Checklist

When migrating a mod from legacy modloader or fixing detection issues:

- [ ] **Properties file exists** in `mods/` root (not just in subfolder)
- [ ] **Directory name** matches properties file name (case-sensitive)
- [ ] **JAR name** matches directory name without version suffix
- [ ] **classpath** in properties points to correct JAR location
- [ ] **Remove obsolete dependencies** (SinduskLibrary, etc.)
- [ ] **Interface import** uses either modern or legacy (both work in v0.9.0+)
- [ ] **Test load:** Check for "Found legacy subfolder mod: modname" in logs

**Example - Correct Structure:**
```
mods/
├── armoury.properties           # lowercase, in root
└── armoury/                     # lowercase directory
    └── armoury.jar              # unversioned JAR
```

**Properties file:**
```properties
classname=mod.sin.armoury.ArmouryModMain
classpath=armoury/armoury.jar
# depend.import=SinduskLibrary  # REMOVED - now in core
```

### "Loaded 0 mods" or Mods Not Loading

**Symptom:** Server starts but no mods load. Log shows `Loaded 0 mods`.

**Cause:** Missing `.properties` files in `mods/` directory root.

**Solution:**
1. Verify you have `.properties` files in `mods/` root (not just in subdirectories)
2. Check your directory structure:
   ```bash
   ls -la mods/
   # Should show: modname.properties files
   ```
3. If missing, extract from mod release ZIPs or create manually:
   ```properties
   # mods/announcer.properties
   classname=org.gotti.wurmunlimited.mods.announcer.AnnounceMod
   classpath=announcer.jar
   ```

**Technical Details:** ModLoaderShared marks mods as "ondemand" when external `.properties` files are absent. The DependencyResolver then prunes these mods from the load list. This is intentional - `.properties` files signal explicit mod installation. See [MOD_LOADING_DISCOVERY_REPORT.md](MOD_LOADING_DISCOVERY_REPORT.md) for full details.

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
| Write bytecode patches | **Don't!** Framework handles this |

## 💬 Support

- **Issues**: [GitHub Issues](https://github.com/garward/WurmModLoader/issues)
- **Discussions**: [GitHub Discussions](https://github.com/garward/WurmModLoader/discussions)
- **Original Project**: For general Wurm modding questions, see [WurmServerModLauncher](https://github.com/ago1024/WurmServerModLauncher)

## 🧪 Testing Status

**✅ RELEASE CANDIDATE - v0.9.0 (November 11, 2025)**

This is the first stable release candidate with complete backward compatibility and dual interface support. Awaiting wider community testing with complex mod setups before 1.0.0 release.

**Tested Configurations:**
- ✅ Linux (Arch Linux, kernel 6.17.5)
- ✅ Wurm Unlimited version 4596061
- ✅ Java 8 (bundled JRE) and Java 17 (OpenJDK)
- ✅ Native launcher (WurmServerLauncher-patched)
- ✅ Pure Java launcher (wurmmodloader.sh)
- ✅ Legacy mod property loading
- ✅ Headless server operation
- ✅ Modern interface implementation (com.garward.wurmmodloader.*)
- ✅ Legacy interface implementation (org.gotti.wurmunlimited.*)

**Verified Working Mods (17+ total):**
- **Legacy mods:** actiondemo, announcer, bagofholding, christmasmod, creatureagemod, cropmod, digtoground, harvesthelper, hitchingpost, httpserver, inbreedwarning, scriptrunner, serverfixes, serverpacks, servermap, spellmod
- **Modern mods:** armoury (Sindusk), oversizedclub (modern interface)

All legacy mods tested at version v0.47-73f7152 from original repository.

**Known Issues:**
- Windows testing needed
- macOS testing needed
- Some CI/CD features still in development

**Milestone Achievements (November 11, 2025):**
- ✅ **Unified Interface Hierarchy** - Legacy and modern interfaces work seamlessly
- ✅ **Complete Backward Compatibility** - All legacy mods work without modification
- ✅ **Modern Interface Support** - New mods can use com.garward.* as preferred method
- ✅ **Production Testing** - Successfully migrated Armoury mod (complex combat system)
- ✅ **Comprehensive Troubleshooting** - Common Issues section covers migration pitfalls

**Recent Fixes (November 11, 2025):**
- ✅ Unified interface hierarchy (legacy extends modern)
- ✅ Fixed ClassCastException for modern interface implementations
- ✅ Updated ModLoader/DelegatedLauncher/ServerHook to use modern interface
- ✅ Comprehensive Common Issues documentation with migration checklist

**Previous Fixes (November 4, 2025):**
- ✅ Fixed PatchedLauncher package references
- ✅ Fixed modlauncher.jar MANIFEST (Main-Class)
- ✅ Documented .properties file requirements
- ✅ Verified full compatibility layer functionality

---

**Built with ❤️ for the Wurm Unlimited community**
