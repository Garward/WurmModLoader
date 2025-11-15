# 🧩 Wurm Modloader Modernization – System Summary

**Last Updated:** November 11, 2025

---

## 🎯 **MAJOR ARCHITECTURAL SHIFT (Nov 11, 2025): Complete Independence**

### **We Now Own the Entire Modloader**

**What Changed:**
- ✅ **Removed modloader-shared-0.18.jar dependency entirely** – vendored all source code into `wurmmodloader-core`
- ✅ **Complete package rename** – `org.gotti.wurmunlimited.modloader.*` → `com.garward.wurmmodloader.modloader.internal.*`
- ✅ **Full control over boot sequence, mod discovery, and class loading**
- ✅ **Comprehensive legacy compatibility layer** – created 40+ wrapper classes/interfaces in `wurmmodloader-legacy`

**Why This Matters:**
- **No external dependencies** – We can modify any part of the modloader without forking
- **Better debugging** – Full visibility into mod discovery, properties loading, JAR resolution
- **Bug fixes** – Fixed critical flat JAR loading bug that prevented modern mods from loading
- **Future-proof** – Can evolve the architecture without breaking existing mods

### **Module Structure**

```
wurmmodloader-api/          - Public API (events, registries, capabilities, bytecode contracts)
wurmmodloader-core/         - Internal implementation (boot logic, mod loading, event bus, registries)
  └─ modloader/internal/    - Vendored & renamed modloader-shared code
  └─ eventlogic/            - Default event logic handlers
  └─ bytecode/patches/      - Core bytecode patches
wurmmodloader-modsupport/   - Builder APIs (ItemTemplateBuilder, CreatureTemplateBuilder, etc.)
wurmmodloader-legacy/       - Backward compatibility wrappers (org.gotti.* → com.garward.*)
```

### **Legacy Compatibility Layer**

To maintain backward compatibility with existing mods, we created comprehensive wrappers:

**Classes & Interfaces** (`wurmmodloader-legacy/src/main/java/org/gotti/`):
```
modloader/
  ├─ classhooks/
  │  ├─ HookException               - Extends internal implementation
  │  ├─ HookManager                 - Delegates to internal singleton
  │  └─ InvocationHandlerFactory    - Extends internal implementation
  ├─ interfaces/
  │  ├─ Configurable                - Extends API interface
  │  ├─ Initable                    - Extends API interface
  │  ├─ PreInitable                 - Extends API interface
  │  ├─ ModEntry                    - Extends internal interface
  │  └─ WurmServerMod               - Re-exports API interface
  └─ ReflectionUtil                 - Static delegation to internal

modsupport/
  ├─ actions/
  │  ├─ ModActions                  - Static delegation
  │  ├─ ModAction                   - Interface extension
  │  ├─ ActionPerformer             - Interface extension
  │  ├─ BehaviourProvider           - Interface extension
  │  └─ ActionEntryBuilder          - Constructor delegation
  ├─ items/
  │  ├─ ItemTemplateBuilder         - Constructor delegation
  │  ├─ ModItems                    - Static delegation
  │  └─ ModelNameProvider           - Interface extension
  ├─ creatures/
  │  ├─ CreatureTemplateBuilder     - Constructor delegation
  │  ├─ ModCreatures                - Static delegation
  │  └─ ModCreature                 - Interface extension
  ├─ vehicles/
  │  ├─ ModVehicleBehaviours        - Static delegation
  │  └─ ModVehicleBehaviour         - Abstract class extension
  ├─ questions/
  │  ├─ ModQuestions                - Static delegation
  │  └─ ModQuestion                 - Interface extension
  ├─ bml/
  │  ├─ BmlBuilder                  - Instance delegation
  │  ├─ BmlNodeBuilder              - Instance delegation
  │  └─ TextStyle                   - Enum re-export
  ├─ IdFactory                      - Static delegation with type conversion
  ├─ IdType                         - Enum re-implementation with conversion
  └─ IIdType                        - Interface extension
```

**Pattern Types:**
1. **Static Delegation** – Utility classes with private constructors that delegate all static methods
2. **Interface Extension** – Legacy interfaces extend new implementations for type compatibility
3. **Constructor Delegation** – Builder classes that call super() with appropriate constructors
4. **Instance Delegation** – Classes that wrap internal instances and delegate method calls
5. **Singleton Delegation** – Wrappers that delegate to internal singleton instances

### **Critical Bug Fixes**

**1. Flat JAR Loading Bug (Nov 11, 2025)**

**Problem:** `ModInstanceBuilder.getClassLoaderEntries()` always searched in `mods/modname/` subfolder, even for flat JAR layouts.

**Old Code (line 83):**
```java
Path modPath = Paths.get("mods", modname);  // ALWAYS mods/Armoury/
```

**Fix:** Intelligently detect layout based on classpath entry:
```java
if (entry.contains("/") || entry.contains("\\")) {
    // Subfolder layout: "modname/mod.jar"
    modPath = Paths.get("mods");
    searchPattern = entry;
} else {
    // Flat layout: "mod.jar" - check subfolder first (legacy priority)
    Path subfolderPath = Paths.get("mods", modname);
    if (Files.exists(subfolderPath) && Files.isDirectory(subfolderPath)) {
        modPath = subfolderPath;  // Subfolder exists
    } else {
        modPath = Paths.get("mods");  // No subfolder, search root
    }
    searchPattern = entry;
}
```

**Result:**
- ✅ Flat JAR mods now work: `mods/Armoury.jar` with `classpath=Armoury.jar`
- ✅ Subfolder mods still work: `mods/bagofholding/bagofholding.jar`
- ✅ Mixed layouts supported with legacy priority

**2. EventLogicClasspathScanner Package Fix**

**Problem:** Scanner was looking in wrong package: `com.garward.wurmmodloader.api.events.eventlogic`

**Fix:** Updated to correct location: `com.garward.wurmmodloader.core.eventlogic`

**3. Missing Gson Dependency**

**Problem:** Event logic handlers need Gson, but it wasn't in runtime classpath

**Fix:**
- ✅ Added `gson-2.10.1.jar` to distribution
- ✅ Updated `wurmmodloader.sh` classpath to include `gson.jar`
- ✅ MaterialEventHandler and combat timing handlers now load successfully

### **Boot Sequence**

**1. DelegatedLauncher (Entry Point)**
```
1. Configure file logging
2. Install capability hooks (before any Wurm classes load)
3. Create ServerHook
4. Normalize legacy mod properties (.properties file rewriting)
5. Load mods via ModDiscovery
6. Add mods to ServerHook
7. Bootstrap registries via SystemBootstrap
8. Initialize callbacks
9. Start Wurm server
```

**2. Mod Discovery (4 Phases)**

**Phase 1: Legacy Subfolder Mods**
- Scans for `*.properties` files in `mods/`
- Looks for matching subfolder: `mods/modname/modname.jar`

**Phase 2: Flat Layout with Properties**
- Remaining `*.properties` files
- Looks for JAR in same directory: `mods/modname.jar`

**Phase 3: Modern Flat (Embedded Metadata)**
- Scans for `*.jar` files not yet handled
- Checks for `META-INF/org.gotti.wurmunlimited.modloader/modname.properties`
- Extracts mod name from JAR filename

**Phase 4: Subfolder Without Properties**
- Scans subdirectories not yet handled
- Looks for JAR inside: `mods/modname/modname.jar`

**3. Properties Loading Priority**

Properties are merged in this order (later overrides earlier):
1. Embedded properties from JAR (`META-INF/...`)
2. External properties file (`mods/modname.properties`)
3. Config file (`mods/modname.config`)

**4. Structured Boot Logging**

Added 7-phase boot sequence with success/warning/error tracking:

```
[BOOT] === Phase 1/7: Mod Discovery ===
[BOOT] ✅ Phase 1/7 complete (234ms)
[BOOT] === Phase 2/7: Dependency Resolution ===
[BOOT] ✅ Phase 2/7 complete (12ms)
...
[BOOT] 🎉 Boot Complete! 15 mods loaded (0 errors, 2 warnings, 1842ms total)
```

Warnings include:
- Mods without `PreInitable` (no bytecode hooks)
- Mods without `Initable` (no init phase)
- Dependency resolution issues

---

## 📂 Core Directory Overview

### `/core/modloader/internal`

**NEW - Vendored Modloader Code**

**Key Files:**
* `ModLoaderShared` (lines 1-400+) - Core mod loading logic
* `ModDiscovery` (lines 1-300+) - 4-phase mod discovery
* `ModInstanceBuilder` (lines 1-150) - JAR classloader & class instantiation
* `ReflectionUtil` (lines 1-200) - Reflection utilities with Java 9+ final field handling

**Purpose:**
This is the vendored and renamed code from Ago's modloader-shared. We now have complete control over:
- Mod discovery and properties loading
- JAR resolution and classloader creation
- Dependency resolution
- Boot sequence coordination

**Key Changes:**
- Fixed flat JAR loading bug
- Added structured boot logging
- Updated to use com.garward packages

---

### `/core/capability`

**Subdirectories:** none
**Key Files:**

* `CapabilityManager` (lines 13–80)
* `CapabilityDatabase` (lines 1–80)
* `CapabilityHooks` (lines 13–112)

**Purpose:**
Registers capabilities, persists them via SQLite, and injects `ICapabilityProvider` into Player, Creature, and Item objects before mods load.

---

### `/core/event`

**Subdirectories:** none
**Key Files:**

* `EventBus` (1–78)
* `EventSubscriber` (1–85)
* `EventLogicBootstrap`
* `EventLogicClasspathScanner`

**Purpose:**
Thread-safe publish/subscribe dispatcher plus runtime helpers that scan `core.eventlogic.*` for handler classes and auto-register them through `RuntimeRegistries.EVENT_LOGIC` before the server finishes booting.

**Recent Fix:** Scanner now correctly searches `com.garward.wurmmodloader.core.eventlogic` package.

---

### `/core/bytecode`

**Subdirectories:** `patches`
**Key Files:**

* `CoreBytecodePatches`
* `PatchRegistry`
* `PatchManager`
* `PatchSettings`
* Representative `patches/*` implementations (`ServerStartPatch`, `ActionTimingPatch`, `CommunicatorChannelPatch`, `VehicleSpeedPatch`, etc.)

**Purpose:**
Defines the modular bytecode pipeline. `CoreBytecodePatches` registers 29 `BytecodePatch` implementations that cover lifecycle, combat, action timing, communicator, vehicle, and item hooks. `PatchRegistry` collects them, `PatchManager` applies them with conflict-key enforcement (and optional `--force-bytecode-conflicts` or `--continue-on-patch-error` diagnostics via `PatchSettings`), and every patch ultimately calls back into `ProxyServerHook` fire helpers. Both launchers strip these internal flags from the Wurm args and log when diagnostic mode is active.

---

### `/core/icon`

**Subdirectories:** none
**Key Files:**

* `IconRegistry` (1–120, 280–305)
* `IconPackGenerator` (1–118)
* `IconPackServerHook` (1–86)
* `IconPackServerPacksBridge` (1–110)

**Purpose:**
Manages icon IDs, sprite-sheet generation, and optional ServerPacks distribution.

---

### `/core/legacy`

**Subdirectories:** none
**Key File:**

* `LegacyListenerBridge` (1–166)

**Purpose:**
Bridges legacy listener interfaces to the modern event bus.
**TODO:** player/message bridging.

---

### `/core/migration`

**Subdirectories:** none
**Key File:**

* `IdFactoryMigration` (1–118)

**Purpose:**
Migrates IDs from legacy SQL tables into the JSON-based allocator without overwriting existing allocations.

---

### `/core/registry`

**Subdirectories:** none
**Key Files:**

* `Registries` (10–147)
* `SimpleRegistry` (1–120)
* `IdAllocator` (1–80)
* `RuntimeRegistry` (6–35)
* `RuntimeRegistries` (7–16)
* `SystemBootstrap` (3–28)

**Purpose:**
Coordinates both static and runtime registries. `SystemBootstrap.initializeAll()` (now invoked from `ServerHook.fireOnServerStarted`) resets runtime registries, registers `CoreBytecodePatches`, scans event-logic handlers, runs `PatchManager`, and freezes every registry automatically.

---

## 🧠 API Layer

### `/api/capability`

**Subdirectories:** none
**Key Files:**

* `Capability` (1–70)
* `ICapabilityProvider` (1–50)

**Purpose:**
Defines the capability contract and provider interface used by injected hooks.

---

### `/api/events`

**Subdirectories:**
`action`, `base`, `combat`, `creature`, `eventlogic`, `item`, `player`, `server`, `skill`, `vehicle`

**Key Files:**

* `Event` (base, 1–80)
* `SubscribeEvent` / `EventPriority`
* Representative: `PlayerLoginEvent` (1–78)

**Purpose:**
Declares strongly typed gameplay events.

> ✅ Update (Nov 10, 2025): Core now ships default eventlogic handlers—see "Event Logic Modules" below.

---

### `/api/icon`

**Subdirectories:** none
**Key Files:**

* `Icon` (1–118)
* `IconType`

**Purpose:**
Immutable metadata for registered icons and their sprite data.

---

### `/api/registry`

**Subdirectories:** none
**Key Files:**

* `Registry` (1–120)
* `ResourceLocation`
* `RegistryEvent`

**Purpose:**
Defines the shared registry and lifecycle contracts.

---

### `/api/bytecode`

**Subdirectories:** none
**Key Files:**

* `BytecodePatch`
* `BytecodeConflictKeys`

**Purpose:**
Defines the contract for modular instrumentation: each `BytecodePatch` declares the target class/method descriptor, an invocation handler factory (or custom `apply`), optional priority + conflict keys, and the runtime uses `BytecodeConflictKeys` to coordinate overlapping patches across core and mods.

---

## 🧱 Module Status

| Module                        | Status          | Notes                                                           |
| ----------------------------- | --------------- | --------------------------------------------------------------- |
| `wurmmodloader-api`           | ✅ Complete     | Public API contracts, stable                                    |
| `wurmmodloader-core`          | ✅ Complete     | Internal implementation, includes vendored modloader code       |
| `wurmmodloader-modsupport`    | ✅ Complete     | Builder APIs, fully functional                                  |
| `wurmmodloader-legacy`        | ✅ Complete     | 40+ compatibility wrappers, full backward compatibility         |
| `wurmmodloader-patcher`       | ✅ Complete     | Server JAR patching, standalone                                 |
| `/systems/`                   | 🚫 Missing      | Only placeholder `RuntimeRegistries.SYSTEMS` exists             |
| `/runtime/`                   | 🚫 Missing      | No `com.garward.wurmmodloader.runtime` package exists           |
| `/bootstrap/`                 | ⚠️ Virtual      | Boot logic in `core/registry/SystemBootstrap`                   |

---

## ⚙️ Core Components Summary

### **Registries & IDs**

Static registries (`Registries`, `SimpleRegistry`) and `IdAllocator` manage namespace-safe storage.
`SystemBootstrap.initializeAll()` now runs via `ServerHook.fireOnServerStarted`, which resets runtime registries, registers `CoreBytecodePatches`, scans event-logic handlers, applies patches, and freezes both runtime and content registries automatically.

> `RuntimeRegistries.BYTECODE_PATCHES` and `.EVENT_LOGIC` are active; `.SYSTEMS` still has no contributors.

---

### **Bytecode Diagnostics & Hook Coverage (Nov 10, 2025)**

- Added a debug flag (`--continue-on-patch-error` or system property `wurmmodloader.bytecode.continueOnError`) so PatchManager logs every failing patch without aborting the bootstrap. Use this during instrumentation sweeps; remove it for normal fail-fast launches.
- `ProxyServerHook` gained a lightweight thread-local `OpportunityContext`, letting combat patches share data without inflating the vanilla method's local-variable table (fixes the VerifyError seen in `Creature.opportunityAttack`).
- The opportunity-attack instrumentation now matches the real `CombatHandler.attack(Creature,int,boolean,float,Action)` signature and feeds modded combat counters/action timers into those parameters. Server boots cleanly with all patches applied.

### **Event Logic Modules (Nov 10, 2025)**

- `/core/eventlogic/materials` now provides `MaterialProfile`, `MaterialProfileRegistry`, and `MaterialEventHandler`. Mods can register JSON-backed profiles that define material modifiers for every hook (damage/decay/imp/repair/bonuses/weapon stats/action+skill scaling) without rewriting handlers.
- `/core/eventlogic/combat/timing` adds reusable combat helpers:
  - `SwingSpeedAdjuster` + `SwingSpeedRegistry` for minimum swing floors and rarity reductions.
  - `WeaponTimerReset` + `WeaponTimerRegistry` for timer normalization via `WeaponUseEvent`.
  - `DualWieldScheduler` + `DualWieldRegistry` for data-driven off-hand scheduling (mirrors Armoury's smarter dual wield logic).
- Registries expose JSON loaders so complex mods (Armoury, DuskCombat, etc.) can ship data files instead of bytecode patches. Default handlers are auto-registered through `RuntimeRegistries.EVENT_LOGIC` during `SystemBootstrap.initializeAll()`.

### **Event System**

`EventBus` and `EventSubscriber` handle dispatching, while `EventLogicBootstrap` + `EventLogicClasspathScanner` crawl `core.eventlogic.*` and register discovered handler classes through `RuntimeRegistries.EVENT_LOGIC` before server events post.
`LegacyListenerBridge` auto-registers legacy interfaces.
`ServerHook` and `ProxyServerHook` bridge Wurm lifecycle hooks.

---

### **Capability System**

`CapabilityManager` registers and caches providers;
`CapabilityDatabase` handles persistence;
`CapabilityHooks` injects accessors before vanilla load.

> Integrated with `ServerHook.fireOnServerStarted`, which fires `CapabilityRegistrationEvent`.
> **Tile hooks** are still pending.

---

### **Icon Pipeline**

`IconRegistry` tracks icons,
`IconPackGenerator` builds sprite sheets,
`IconPackServerHook` and `IconPackServerPacksBridge` handle ServerPacks registration.

> Fully functional but requires mods to register icons.

---

### **Mod Loader & Hooks**

`DelegatedLauncher` installs capability hooks before any Wurm classes load, normalizes legacy mod properties, flips `PatchSettings` when `--force-bytecode-conflicts` is supplied, creates `ServerHook`, then launches Wurm through the HookManager loader.

`ModLoaderShared` (vendored) handles mod discovery, properties loading, dependency resolution, and class instantiation. Integrates with both legacy/new ModComm stacks, ModIntraServer, and performance helpers.

> `ProxyServerHook` now concentrates on communicator/message routing and exposes the `fire*` helpers that bytecode patches invoke; the instrumentation itself lives in `core/bytecode`.

---

### **Performance & Bytecode**

`CreatureStatusBatcher` batches DB writes;
`DatabaseOptimizer` currently just verifies WAL settings (still needs configuration).
Bytecode patching is now centralized: `CoreBytecodePatches` registers 29 `BytecodePatch` implementations via `RuntimeRegistries.BYTECODE_PATCHES`, `PatchRegistry` collects them, and `PatchManager` applies them with conflict-key enforcement (opt-in force mode).

> Next step: expose config/telemetry for DatabaseOptimizer and determine whether `RuntimeRegistries.SYSTEMS` should host additional non-bytecode services.

---

## 🔁 Overlap or Duplication

| Area            | Description                                                                  | Status                |
| --------------- | ---------------------------------------------------------------------------- | --------------------- |
| **ModComm**     | Both legacy (`org.gotti`) and modern (`com.garward`) stacks run in parallel | ✅ Working, needs consolidation |
| **ModSupport**  | `ModActions` wrappers duplicate functionality across modules                 | ✅ Resolved with legacy wrappers |
| **Event Logic** | Scanner is wired and auto-registers handlers from `core.eventlogic.*`       | ✅ Working             |

---

## 🧩 TODOs & Stubbed Classes

* `LegacyListenerBridge`: Player/message bridging unfinished.
* `CapabilityHooks`: Tile hooks pending.
* `DatabaseOptimizer`: Hardcoded enable flag; needs config.
* `BehaviourProvider`, `ModAction`, `ModCreature`: Have legacy wrappers but minimal functionality.
* `SimpleMod`: Empty placeholder implementing `WurmServerMod`.

---

## 🔗 Integration Points

* **`DelegatedLauncher.main`** – installs capability hooks before vanilla classes load, normalizes mod properties, processes `--force-bytecode-conflicts`, creates `ServerHook`, then launches Wurm through HookManager.
* **`ModLoaderShared`** (vendored) – handles mod discovery (4 phases), properties loading (3 sources), dependency resolution, and JAR classloading.
* **`SystemBootstrap` + `CoreBytecodePatches`/`PatchManager`** – reset runtime registries, register all core `BytecodePatch` instances, scan `core.eventlogic.*`, enforce conflict keys, and freeze registries before gameplay events fire.
* **`ProxyServerHook`** – centralizes communicator/message hooks and exposes `fire*` helpers that bytecode patches call into.
* **`ServerHook`** – bridges legacy listeners, fires initialization events, and triggers `SystemBootstrap`.
* **`IconPackServerHook` / `IconPackServerPacksBridge`** – generate & register icon packs.
* **`ModComm` (new + legacy)** – both hook player communicators.

---

## 📊 Summary Table

| System                     | Main Classes                                                                                | Status        | Notes                                                    |
| -------------------------- | ------------------------------------------------------------------------------------------- | ------------- | -------------------------------------------------------- |
| **Registries & IDs**       | `Registries`, `SimpleRegistry`, `IdAllocator`, `IdFactoryMigration`, `RuntimeRegistry`, `SystemBootstrap` | ✅ Implemented | `SystemBootstrap` now resets/applies/freezes registries; `RuntimeRegistries.SYSTEMS` still empty. |
| **Event System**           | `EventBus`, `EventSubscriber`, `EventLogicBootstrap`, `EventLogicClasspathScanner`, `LegacyListenerBridge`, `ServerHook`, `ProxyServerHook` | ✅ Implemented | Event bus + scanner wired; eventlogic handlers are now auto-registered via `RuntimeRegistries.EVENT_LOGIC`. |
| **Event Logic Modules**    | `MaterialEventHandler`, `SwingSpeedAdjuster`, `WeaponTimerReset`, `DualWieldScheduler`, associated registries | ✅ Implemented | Data-driven material/combat timing helpers with JSON-backed registries; more modules (diagnostics, shields) in progress. |
| **Capabilities**           | `CapabilityManager`, `CapabilityDatabase`, `CapabilityHooks`, `CapabilityRegistrationEvent` | ✅ Implemented | Entity injection works; tile hooks pending.              |
| **Icon Pipeline**          | `IconRegistry`, `IconPackGenerator`, `IconPackServerHook`, `IconPackServerPacksBridge`      | ✅ Implemented | Fully functional once mods register icons.               |
| **Bytecode & Performance** | `CoreBytecodePatches`, `PatchRegistry`, `PatchManager`, `PatchSettings`, `ProxyServerHook`, `CreatureStatusBatcher`, `DatabaseOptimizer` | ✅ Implemented | Unified BytecodePatch API with 29 patches; DatabaseOptimizer still logging-only/config TBD. |
| **Mod Loading**            | `ModLoaderShared`, `ModDiscovery`, `ModInstanceBuilder`, `DelegatedLauncher`                | ✅ Implemented | Vendored & enhanced; supports 4 discovery phases, 3 property sources, fixed flat JAR bug. |
| **Legacy Compatibility**   | 40+ wrapper classes in `wurmmodloader-legacy`                                               | ✅ Implemented | Full backward compatibility with org.gotti packages.     |
| **Mod Support / Builders** | `ModActions`, `ItemTemplateBuilder`, `CreatureTemplateBuilder`, etc.                        | ✅ Implemented | Fully functional with legacy wrappers for compatibility. |
| **Runtime Bootstrap**      | `RuntimeRegistry`, `RuntimeRegistries`, `SystemBootstrap`                                   | ⚙️ Partial    | Executes at server start, but `/runtime` + `/systems` packages lack concrete systems. |

---

## 🚀 Next Steps

1. **Consolidate ModComm** – Merge legacy and modern ModComm stacks into single implementation
2. **Add DatabaseOptimizer config** – Expose configuration for WAL settings, batch sizes, etc.
3. **RuntimeRegistries.SYSTEMS** – Decide purpose or remove placeholder
4. **Tile Capabilities** – Complete capability injection for Tile objects
5. **Player/Message Bridging** – Finish `LegacyListenerBridge` implementation
6. **Documentation** – Update public docs for new architecture and flat JAR support

---

## 🎓 Loading This Context into LLMs

This summary is designed to give LLMs comprehensive understanding of:
- **Architecture** – Module structure, boot sequence, integration points
- **Recent Changes** – Nov 11 independence milestone and bug fixes
- **Key Classes** – Where to find critical functionality
- **Compatibility** – How legacy mods are supported
- **TODOs** – What needs work

**Recommended Context Loading:**
1. Start with "MAJOR ARCHITECTURAL SHIFT" section
2. Reference "Boot Sequence" and "Mod Discovery" for loading issues
3. Check "Integration Points" for hook locations
4. Use "Summary Table" for quick status reference
5. Consult specific directory sections for detailed implementation

**For Debugging:**
- Mod loading issues → See "Mod Discovery (4 Phases)" and ModInstanceBuilder fix
- Event system → See "Event System" and EventLogicClasspathScanner
- Backward compatibility → See "Legacy Compatibility Layer"
- Bytecode patches → See "Bytecode & Performance"
