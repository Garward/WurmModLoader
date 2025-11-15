# Changelog

All notable changes to WurmModLoader will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Phase 5 - Legacy Bridge Layer: ModComm Compatibility (COMPLETED - November 2025)

**Note:** This phase took approximately 20x longer than originally estimated due to complex architectural challenges with class loading timing and module dependencies. The core issue was ensuring backward compatibility with Ago's Client Mod Launcher and proper integration with the legacy ModComm protocol.

### Phase 5.5 - Database Performance Optimization: CreatureStatusBatcher (COMPLETED - November 2025)

**PERFORMANCE BREAKTHROUGH:** Implemented batching system for creature database updates to eliminate 1000ms+ lag spikes.

**Problem Identified:**
- Wurm's `DbCreatureStatus` class executes individual UPDATE statements for every property change (17 setter methods)
- Every creature poll cycle triggers thousands of individual database writes with no batching
- Despite WAL mode being enabled, the unbatched writes caused severe lag spikes in `Zones.pollnextzones`
- Example: 1000 creatures with 5 property changes = 5,000 individual transactions per poll cycle

**Solution Implemented:**
- Created `CreatureStatusBatcher` class in `wurmmodloader-core` package
- Hooks 6 high-frequency DbCreatureStatus methods to defer database writes:
  - `save()` - returns boolean (creature persistence)
  - `updateAge()` - void (aging every poll cycle)
  - `updateFat()` - void (eating/starving)
  - `setLoyalty(float)` - void (tamed creature loyalty changes)
  - `setDead(boolean)` - void (creature death)
  - `setKingdom(byte)` - void (kingdom changes)
- Marks creatures as "dirty" and queues them for batch flush every 100ms
- Uses ThreadLocal bypass flag to prevent infinite recursion during flush operations
- Smart logging: only reports large batches (>100 creatures) or slow flushes (>50ms)
- Can be disabled via system property: `-Dwurmmodloader.performance.creature_batching=false`

**Performance Gain Achieved:**
- Eliminated database write spam during startup and gameplay
- Server startup significantly faster
- Batch sizes vary based on activity (1-100+ creatures per 100ms window)
- Individual UPDATEs consolidated into efficient batched operations

**Architectural Decision:** Placed in `wurmmodloader-core` (not separate mod) following ModComm precedent from Phase 5, with configuration toggle for opt-out.

#### Added
- Full compatibility with Ago's Client Mod Launcher for server pack transmission
- Extensive debug logging throughout ModComm initialization and lifecycle
- Direct import-based calls to legacy ModComm methods (matching Ago's original architecture)

#### Changed
- **CRITICAL ARCHITECTURAL CHANGE:** Moved `org.gotti.wurmunlimited.modcomm.ModComm` and related classes from `wurmmodloader-legacy` module to `wurmmodloader-core` module
  - Reason: Eliminated circular dependency that forced use of reflection
  - Matches Ago's monolithic architecture where ModLoader and ModComm are in the same module
  - Enables direct method calls instead of reflection-based calls
- Replaced all reflection-based ModComm initialization with direct imports and calls in:
  - `ModLoader.modcommInit()` - Now directly calls `org.gotti.wurmunlimited.modcomm.ModComm.init()`
  - `ServerHook.fireOnServerStarted()` - Now directly calls `org.gotti.wurmunlimited.modcomm.ModComm.serverStarted()`
  - `ServerHook.fireOnPlayerLogin()` - Now directly calls `org.gotti.wurmunlimited.modcomm.ModComm.playerConnected()`
- Modified `ModLoader.modcommInit()` to only call legacy ModComm.init() once (removed duplicate call to new package version)
- Ensured ModComm.init() runs during modcommInit phase (Phase 3/4 in bdew's loading phases) BEFORE vanilla Player class is loaded by JVM

#### Fixed
- **CRITICAL FIX:** `NoSuchFieldException: modConnection` error that prevented server pack transmission
  - Root cause: Player class was being loaded before Javassist could add the `modConnection` field
  - Solution: Proper timing of ModComm.init() in modcommInit phase, before vanilla classes load
- Fixed incorrect import in `ServerHook.java`: Changed `com.oracle.truffle.api.library.Message` to `com.wurmonline.server.Message`
- Eliminated "split-brain" issue where legacy and new ModComm maintained separate channel HashMaps
- Fixed duplicate field addition attempts by removing redundant ModComm.init() call

#### Technical Details
- **Class Loading Timing:** Javassist modifications (adding `modConnection` field to Player class) must occur in CtClass form BEFORE the JVM loads the actual Player.class
- **bdew's Loading Phases:** ModComm.init() must run in Phase 3/4 (preInit/modcommInit) when Javassist can still modify vanilla classes
- **Module Architecture:** ModComm location in core module (not legacy) is critical for proper initialization timing
- **Verification:** Successful server pack handshake confirmed - all server packs now transmit correctly to clients using Ago's Client Mod Launcher

#### Lessons Learned
- Reflection-based calls introduce timing and reliability issues for bytecode manipulation
- Module structure significantly impacts class loading order and Javassist modification timing
- Direct imports (matching Ago's original design) provide more reliable initialization
- The monolithic approach for core modloader components (ModLoader + ModComm in same module) is architecturally sound for bytecode modification scenarios

---

## Previous Phases

### Phase 4 - Registry System (COMPLETED)
- Core registry infrastructure
- ID allocation with persistence
- Builder migration (ItemTemplateBuilder, CreatureTemplateBuilder)
- Comprehensive testing (101 tests, 100% passing)
- Documentation and examples

### Phase 3 - Event Bus Implementation (Status: PENDING)

### Phase 2 - Package Rename & Core Refactoring (COMPLETED)
- Renamed base package from `org.gotti.wurmunlimited` to `com.garward.wurmmodloader`
- Split modules into API, Core, and Legacy packages
- Maintained backward compatibility with legacy `org.gotti` package

### Phase 1 - Maven → Gradle Migration (COMPLETED)
- Migrated from Maven to Gradle build system
- Multi-module Gradle project structure
- Distribution build system for deployable artifacts

### Phase 0 - Pre-Fork Setup (COMPLETED)
- Forked from Ago's WurmServerModLauncher
- Initial repository setup and documentation
