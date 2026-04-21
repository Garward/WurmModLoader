# WurmModLoader — Architecture

A map of **where everything is** and **how the pieces connect**. For hands-on guides see [`docs/`](docs/README.md). For current project status see [`PROJECT_STATE.md`](PROJECT_STATE.md). For the canonical mod example see [`examples/oversizedclub/`](examples/oversizedclub/).

---

## What this project is

A from-scratch modloader for **Wurm Unlimited Dedicated Server**, built on top of a fork of Ago's `WurmServerModLauncher`. Not a patch set — a full replacement with a clean event-driven API, a capability system, a bytecode-patch pipeline, and a legacy compatibility bridge so older Ago-era mods still load.

Current scale (regenerate with `codeindex regen` + `wurmquery regen`):
- **591** Java files, **1,222** classes, **4,495** methods across the framework
- **57** bytecode patches
- **98** API event types
- **5** bundled showcase mods

---

## Module map

Multi-module Gradle build (`settings.gradle.kts`). One JAR per module.

| Module | Role | Depends on Wurm? |
|---|---|---|
| **`wurmmodloader-api`** | Pure interfaces & event classes that mods compile against. No implementation, no Wurm imports. | No |
| **`wurmmodloader-core`** | **The engine.** Classloader control, Javassist bytecode pipeline, event bus, mod discovery, lifecycle orchestration, `ServerHook`/`ProxyServerHook`, capability system, ModComm subsystem, HTTP-server subsystem, all direct `com.wurmonline.*` references live here. | Yes |
| **`wurmmodloader-modsupport`** | Higher-level helper APIs — `CreatureTemplateBuilder`, `ItemTemplateBuilder`, BML UI helpers, loot/vehicle utilities. Optional for mods. | Yes (via core) |
| **`wurmmodloader-legacy`** | Compatibility shim that bridges the old `org.gotti.wurmunlimited.*` package namespace to the new framework, so Ago-era mods run unchanged. | Yes |
| **`wurmmodloader-cli`** | picocli-based CLI tooling (mod inspection, diagnostics). | No |
| `wurmmodloader-patcher` | *Disabled* — client-only JAR patcher. Commented out in `settings.gradle.kts`; source retained for when client modloader work resumes. | — |

**Golden rule:** `com.wurmonline.*` imports are allowed only in `wurmmodloader-core`. Everything else (mods, modsupport helpers) talks to the game through events, the capability system, or reflection-safe wrappers.

---

## Bundled mods (`mods/`)

Each mod is its own Gradle sub-project. Build output lands in `mods/<name>/build/libs/<name>.jar`.

| Mod | Purpose |
|---|---|
| `powerscaling` | Power-fantasy scaling: stats, healing v2, metal loot |
| `soulboundgear` | Gear that binds to the player who attunes it |
| `upgradetree` | In-world UI for picking upgrade paths |
| `materialsystem` | Damage types & material behavior overhaul |
| `eventlister` | Diagnostic mod that prints fired events — useful when exploring the event catalog |

---

## Examples (`examples/`)

| Path | What it shows |
|---|---|
| **`oversizedclub/`** | **Canonical tutorial mod.** Heavily commented, intentionally simple mechanic (bigger club = harder hits). Read this first when you're new to the framework. |
| `templatemod/` | Bare-bones skeleton to copy when starting a new mod |
| `basic-item-mod/` | Registering a custom item |
| `custom-creature/` | Defining a custom creature template |
| `action-system/` | Hooking into the action/interaction system |

---

## Boot sequence

```
┌─────────────────────────────────────────────────────────────┐
│ 1. User runs server start script                           │
│    →  java -cp wurmmodloader-core.jar:...                  │
│          com.garward.wurmmodloader.serverlauncher.ServerLauncher
└────────────────────────────────┬────────────────────────────┘
                                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. ServerLauncher (wurmmodloader-core)                     │
│    Installs Javassist Loader, delegates javafx/javassist   │
│    packages, runs DelegatedLauncher through it so class    │
│    loading is intercepted BEFORE Wurm classes resolve.     │
└────────────────────────────────┬────────────────────────────┘
                                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. DelegatedLauncher                                       │
│    • CapabilityHooks.installHooks()                        │
│    • ServerHook.createServerHook()   ← single Wurm hook    │
│    • ModLoader.loadModsFromModDir(Paths.get("mods"))       │
│    • SystemBootstrap.initializeAll()                       │
│    • Hands off to com.wurmonline.server.ServerLauncher     │
└────────────────────────────────┬────────────────────────────┘
                                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Wurm boots. Patched classes load. Mods receive events   │
│    through the EventBus as gameplay unfolds.               │
└─────────────────────────────────────────────────────────────┘
```

### Startup event ordering (the actual sequence)

This trips people up, so the ordering is spelled out here. The names are
historical — `ServerStartedEvent` is NOT the "server is fully operational"
moment; it fires before several async subsystems finish. Use
`ServerFullyReadyEvent` for anything that depends on a hot DB pool, Steam
being connected, or the console reader being live.

```
  mod preInit / init
     ↓
  World load + DB pool initialization begins
     ↓
  ServerLauncher.runServer returns
     ↓
  Legacy ServerStartedListener.onServerStarted() fires
  ServerStartedEvent fires                         ← core init complete
     ↓
  Steam connect, DB pool warmup, misc async subsystems settle
     ↓
  CommandReader.run begins (console accepts input)
  ServerFullyReadyEvent fires                      ← truly ready
     └── framework DB config sync runs here
```

| Event | When | Use for |
|---|---|---|
| `ServerStartedEvent` | `ServerLauncher.runServer` returns | World + mods initialized. Safe for register-only work that doesn't touch DB pool / Steam / console. |
| `ServerFullyReadyEvent` | `CommandReader.run` begins | DB-dependent sync, migrations, pool warmup checks, any post-boot work that historically needed a "sleep N seconds after ServerStarted" workaround. |


**Mod discovery** (in `ModDiscovery`) tries four strategies in order:
1. Legacy subfolder (`mods/<name>/<name>.jar` + sibling `.properties`)
2. Flat properties file
3. **Modern flat** — `mods/<name>.jar` with embedded properties
4. Subfolder without `.properties`

**Mod lifecycle:** dependencies resolved via topological sort, then `configure()` → `preInit()` → `init()` — each phase runs across all mods before the next starts.

---

## Key subsystems (where to look)

| Subsystem | Where |
|---|---|
| **Event definitions** (98 events) | `wurmmodloader-api/src/main/java/com/garward/wurmmodloader/api/events/` |
| **Event bus** | `wurmmodloader-core/.../core/event/` |
| **Bytecode patches** (57 patches) | `wurmmodloader-core/.../core/bytecode/patches/` |
| **`ServerHook` / `ProxyServerHook`** | `wurmmodloader-core/.../core/hooks/` — the single point of contact between the framework and Wurm's server |
| **Capability system** | `wurmmodloader-core/.../core/capability/` + `CAPABILITY_SYSTEM.md` inside |
| **ModComm subsystem** (inter-mod packet channels) | `wurmmodloader-core/.../modcomm/` + `wurmmodloader-api/.../api/modcomm/` |
| **HTTP server subsystem** (graphics-pack delivery, mod HTTP endpoints) | `wurmmodloader-core/.../httpserver/` + `wurmmodloader-api/.../api/httpserver/ModHttpServer.java` — config at `<server>/config/wurmmodloader-http.properties` (alongside vanilla's `logging.properties`; seeded by `deploy.sh`; template at `docs/reference/wurmmodloader-http.properties.example`) |
| **Console GM commands** (`#`-prefixed stdin commands, safe shutdown) | `wurmmodloader-core/.../core/console/` — `ConsoleGMCommandRouter`, `GMCommandDiscovery`, `GMCommandAutoInvoker`. Full reference: [`docs/reference/console-commands.md`](docs/reference/console-commands.md) |
| **Mod discovery & loader** | `wurmmodloader-core/.../modloader/` |
| **Javassist / HookManager** | `wurmmodloader-core/.../modloader/internal/classhooks/` |
| **Legacy `org.gotti.*` bridge** | `wurmmodloader-legacy/` |
| **BML UI helpers** | `wurmmodloader-modsupport/.../bml/` (see [`docs/guides/bml-ui.md`](docs/guides/bml-ui.md)) |

---

## How a mod plugs in

A mod is a JAR containing a class implementing `WurmServerMod` (plus optionally `Configurable`, `PreInitable`, `Initable`, `Versioned`).

Modern (recommended) event handling — from `docs/guides/event-bus.md`:
```java
public class MyMod implements WurmServerMod {
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) { ... }
}
```

Legacy listener-style (still supported via the legacy bridge):
```java
public class MyMod implements WurmServerMod, ServerStartedListener {
    @Override public void onServerStarted() { ... }
}
```

For the full walkthrough, read `examples/oversizedclub/` top to bottom — it's deliberately over-commented for exactly this purpose.

---

## Adding a new event type

1. If a bytecode patch is needed, add it under `wurmmodloader-core/.../core/bytecode/patches/`
2. Add the event class under `wurmmodloader-api/.../api/events/`
3. Install the hook in `ProxyServerHook` (framework-side)
4. Add a fire method in `ServerHook`
5. (Optional) Put complex dispatch / resolver / util logic in `wurmmodloader-core/.../core/eventlogic/<area>/`
6. Build & deploy: `./build-and-deploy.sh`
7. Regenerate the code index: `codeindex regen`
8. Convert any mods that should use it

---

## Development workflow

| Task | Command |
|---|---|
| Build all modules + distribution zip | `./build.sh` |
| Deploy (changed JARs only) to live server | `./deploy.sh` |
| Build + deploy in one shot | `./build-and-deploy.sh` |
| Search framework code | `codeindex search <term>` / `codeindex pattern event_handlers` |
| Search vanilla Wurm (decompiled) | `wurmquery search <term>` (server) / `wurmquery client search <term>` |
| Regen framework index | `codeindex regen` |
| Regen vanilla indexes | `wurmquery regen server` / `wurmquery regen client` |
| Check index ages | `codeindex status` / `wurmquery status` |

Server location: `~/.local/share/Steam/steamapps/common/Wurm Unlimited Dedicated Server/`
Server logs: `logs/` and `<WORLDNAME>/Logs/` under the server root.

---

## Cross-repo references

| Resource | Path |
|---|---|
| Community mods fork (ported mods) | `~/Scripts/Games/WurmUnlimited/WurmModLoader-CommunityMods/` |
| Decompiled vanilla Wurm source (for bytecode target reference) | `~/Scripts/Games/WurmUnlimited/PowerFantasy/Wurmguide/decompiled/` |
| Live server runtime | `~/.local/share/Steam/steamapps/common/Wurm Unlimited Dedicated Server/` |
