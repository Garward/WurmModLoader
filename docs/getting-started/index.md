# Getting Started — Server Mods

A modder's onramp to **WurmModLoader (server)**. If you've written an Ago-era
`org.gotti.*` mod before, you'll recognize the shape; the changes since then
are explained below. If this is your first Wurm mod, start at the top.

> Looking for **client** mods (UI, prediction, in-game overlays)? See the
> [client getting-started hub](../../../WurmModLoader-Client/docs/getting-started/index.md)
> *(when written)*. The two repos are separate — server mods don't ship UI;
> client mods don't run game logic.

---

## What WurmModLoader is

A drop-in replacement for Ago's WurmServerLauncher. It boots Wurm Unlimited's
dedicated server, applies bytecode patches, isolates each mod in its own
classloader, and exposes a typed event bus mods subscribe to instead of
patching the game themselves.

> **Windows users:** every `./foo.sh` / `./gradlew` command shown below has
> a `foo.bat` / `gradlew.bat` equivalent. Run them from `cmd.exe` or
> PowerShell in the repo root.

**What you get over the Ago-era loader:**

- Annotation-driven event bus (`@SubscribeEvent`) instead of marker interfaces
- Bytecode patches centralized in the framework — most mods never write one
- Capability system for attaching custom data to items / players / creatures
  without writing schema or save code
- Reflection-safe mod-side helpers (`ItemTemplateBuilder`, `ModQuestion`, …)
  so mods don't reach into `com.wurmonline.*` directly
- A single classloader rule: framework owns Wurm imports, mods don't
- Build via Gradle, ship via `./build-and-deploy.sh` — no Maven, no manual jar
  copying

Legacy `org.gotti.*` interfaces still work for compatibility — but **new mods
should use the `com.garward.*` namespace and `@SubscribeEvent`**.

---

## What can I actually build with this?

Server mods are server-authoritative — game logic, content, balance,
systems. If your idea fits one of these, the framework has a lane for
it:

- **Content** — new items, creatures, recipes, skills, spells, deities
- **Balance / mechanics** — damage formulas, skill-gain curves, combat
  rules, timers (Spellcraft, DUSKombat, Armoury territory)
- **Systems** — village perks, dungeon spawners, bounty, scaling,
  power-trees, seasonal events
- **UI flows** — server-authored popups, questionnaires, context-menu
  commands (no client mod required; BML goes over the wire to vanilla
  clients)
- **Admin tooling** — GM commands, console extensions, live diagnostics,
  auto-moderation
- **Cross-mod protocols** — HTTP endpoints for external tools, ModComm
  channels for typed packets to a client-side counterpart (see the
  [client repo](../../../WurmModLoader-Client/))

The framework runs purely server-side. If you need to paint pixels or
read keyboard input, that's a *client* mod — separate repo.

---

## What you touch, what you don't

Three layers, clear ownership:

**You own**
- Event handlers, your business logic
- Your items, creatures, recipes, spells, templates
- Your config files and wire formats

**The framework owns**
- Bytecode patches and classloaders
- The event bus and the widened API surface
- Capability storage and `wurmmodloader-modsupport` helpers

**Wurm owns**
- The game loop, rendering, persistence, network stack
- Everything under `com.wurmonline.*`

**The practical rule.** If you're about to type `import com.wurmonline.…`
in mod code, stop and check whether a `wurmmodloader-modsupport` helper
or an event already covers it. If nothing covers it, that's a signal
to extend the framework — *not* to reach into Wurm directly from the
mod. Every `com.wurmonline.*` import in a mod is a future liability.

---

## The normal workflow — idea to working mod

1. **Find your hook point.** What moment in the game does your mod
   react to? Item used, creature spawned, action performed, tick fires,
   player logs in. That's your event.
2. **Check the event catalog.** [`../guides/event-bus.md`](../guides/event-bus.md)
   lists everything the framework already emits. ~80% of the time
   it's there.
3. **Scaffold from `examples/hellomod/`.** Copy the directory, rename
   the class + package, change the subscribed event type. Register it
   in `settings.gradle.kts`.
4. **Build and deploy.** `./build-and-deploy.sh` — then start the
   server and run `wurmlog --since-last-restart --grep <YourMod>`.
   Verify the event fires before writing behavior.
5. **Write the behavior.** If you need a helper, look in
   `wurmmodloader-modsupport` first. If it's missing, add it to
   modsupport — not inside your mod.
6. **If the event you need doesn't exist** → extend the framework per
   [`../guides/extending-framework.md`](../guides/extending-framework.md).
   Five-file recipe, reusable by every future mod.

**The whole trip from empty folder to "my event handler ran" is
under an hour** if the event already exists — which it usually does.
The only part that takes real time is the behavior itself.

---

## Project layout — what lives where

| Path | What it is |
|---|---|
| `wurmmodloader-api/` | Public API — events, annotations, mod interfaces. Mods compile against this. |
| `wurmmodloader-core/` | Engine internals — bytecode patches, hook installation, event bus. Mods don't import from here. |
| `wurmmodloader-modsupport/` | Helper APIs (`ItemTemplateBuilder`, `ModQuestion`, capabilities). Reflection-safe wrappers around Wurm internals. |
| `wurmmodloader-legacy/` | Bridges Ago `org.gotti.*` mods to the new event system. Don't depend on it from new mods. |
| `examples/hellomod/` | **Smallest possible mod.** One class, one event handler. Start here. |
| `examples/oversizedclub/` | **Canonical tutorial mod.** Heavily commented; covers items, capabilities, combat hooks, recipes. |
| `mods/<name>/` | First-party mods built on the framework. |

The full module map (and the load-bearing boot sequence) is in
[`../../Architecture.MD`](../../Architecture.MD).

---

## Hello-mod in 10 minutes

A complete, working mod that prints a banner when the server starts.

### 1. Source code

`examples/hellomod/src/main/java/com/garward/wurmmodloader/examples/hellomod/HelloMod.java`:

```java
package com.garward.wurmmodloader.examples.hellomod;

import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;

import java.util.logging.Logger;

public class HelloMod implements WurmServerMod {
    private static final Logger logger = Logger.getLogger(HelloMod.class.getName());

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        logger.info("HelloMod loaded — WurmModLoader is alive.");
    }

    @Override public String getVersion() { return "1.0.0"; }
}
```

That's the whole mod. No `com.wurmonline.*` imports. The `@SubscribeEvent`
annotation is what wires the method to the event bus — you don't register
listeners manually.

### 2. Mod descriptor

`examples/hellomod/src/dist/hellomod.properties`:

```properties
classname=com.garward.wurmmodloader.examples.hellomod.HelloMod
```

The framework finds your mod class via this file. Optional fields (`depends=`,
custom config keys) live here too — see
[`guides/mod-config.md`](../guides/mod-config.md) *(when written)*.

### 3. Build script

`examples/hellomod/build.gradle.kts`:

```kotlin
plugins { java }

dependencies {
    implementation(project(":wurmmodloader-api"))
    compileOnly(files("../../distribution/server.jar", "../../distribution/common.jar"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
```

Wurm's server jar is `compileOnly` — it's provided at runtime by the loader.
Java 8 source level matches Wurm's runtime.

### 4. Register with the build

In `settings.gradle.kts`, add:

```kotlin
include("examples:hellomod")
project(":examples:hellomod").projectDir = file("examples/hellomod")
```

(HelloMod is already registered in this repo — only needed if you fork.)

### 5. Build + deploy

```bash
./build-and-deploy.sh
```

Builds every module, packages the distribution zip, rsyncs only changed jars
to your server install directory (`<wurm-server-dir>`). Default Steam locations:

- **Windows:** `C:\Program Files (x86)\Steam\steamapps\common\Wurm Unlimited Dedicated Server\`
- **Linux:**   `~/.local/share/Steam/steamapps/common/Wurm Unlimited Dedicated Server/`

Set `WURM_SERVER_DIR` (or `wurmServerDir` in `~/.gradle/gradle.properties`) so
the build scripts know where yours lives — see `gradle.properties.example`.

### 6. Start the server + verify

From `<wurm-server-dir>`:

```bash
./wurmmodloader.sh start=Riverweave    # Linux/macOS — your world name
```
```bat
wurmmodloader.bat start=Riverweave     :: Windows
```

Then in another terminal:

```bash
wurmlog --since-last-restart --grep HelloMod
```

You should see:

```
INFO: HelloMod loaded — WurmModLoader is alive.
```

If you don't, see [troubleshooting](../guides/) *(when written)* — usually
either the `.properties` file didn't get deployed or the mod folder name
doesn't match the jar name.

---

## Questions you probably have right now

If you're coming from an Ago-era mod — Spellcraft, Bounty, Bestiary,
Armoury, one of the Sindusk libraries — these are the "wait, why?"
moments that hit on the first port. Answers are short on purpose; each
points at the doc with the full story.

### "My old mod implements `Configurable`, `PreInitable`, `Initable`, `ServerPollListener`… do I need all of those?"

No. The modern framework replaces every one of those marker
interfaces with `@SubscribeEvent`. `Configurable.configure()` →
subscribe to the configuration event (or just parse the Properties
yourself); `ServerStartedListener` → `ServerStartedEvent`;
`ServerPollListener` → `ServerPollEvent`. One annotation, one method,
one event type. The legacy bridge keeps old mods compiling — see
[`../migration/from-legacy.md`](../migration/from-legacy.md) for the
per-interface mapping — but new code uses the bus.

### "My mod has classes in `com.wurmonline.server.spells.*` — is that still OK?"

It compiles, but it's a smell. Ago-era mods routinely put their own
classes inside Wurm packages to reach package-private fields and
methods. The modern framework widens the access it needs *centrally*
(see `wurmmodloader-core/.../bytecode/patches/`) so mods can live in
their own namespace. If you're porting, leave the classes where they
are for the first pass — get it compiling, then migrate package by
package. If you hit a field the framework hasn't widened yet, extend
the patcher (recipe in
[`../guides/extending-framework.md`](../guides/extending-framework.md))
rather than burying the access inside a Wurm-package class.

### "My `build.gradle` pulls `org.gotti.wurmunlimited:server-modlauncher:0.43` from gotti's maven repo. That repo is dead."

Right. The modern layout is **repo-local**: your mod becomes a
subproject of WurmModLoader (or a sibling with a `compileOnly(files(...))`
pointing at a built `wurmmodloader-api` jar). No external maven, no
JitPack. `examples/hellomod/build.gradle.kts` is the canonical
template — copy it. And while you're there: `compile` → `implementation`,
Groovy DSL → Kotlin DSL (optional but standard here),
`archiveName "..."` → `archiveFileName.set("...")`.

### "What about `sindusklibrary` / other mod-as-library dependencies?"

Same answer: no external maven. Either vendor the library into your
mod jar (if it's small and self-contained — a `Prop` helper is ~30
lines), or port the library into the framework itself if other mods
also need it. Many SinduskLibrary utilities are already covered by
`wurmmodloader-modsupport` — check there first. `depend.import=Foo`
in `.properties` still works for genuine cross-mod load ordering, but
it's rarely needed once the shared helpers live in the framework.

### "Do I still write bytecode patches myself with Javassist?"

Usually no. The framework ships patches for the hook surface you'd
have hand-written against — creature update, combat, actions, items,
recipes, login, polling, shutdown. You subscribe to events instead.
See [`../guides/event-bus.md`](../guides/event-bus.md) for the
catalog. If the event you need isn't there, the centralized-patch
recipe is in
[`../guides/extending-framework.md`](../guides/extending-framework.md)
— patches live in the framework, events live in the API, your mod
just subscribes. The Spellcraft/DUSKombat-style pattern of shipping a
`ClassPool` manipulator inside the mod jar is obsolete.

### "My mod reads 80 config fields with `Properties.getProperty(...)`. Is there something better?"

Nothing forces you to change it — the manual-parse pattern still
works. But most modern mods use JSON or YAML config via the helpers
in `wurmmodloader-modsupport`, which bind a file to a POJO in one
line. For a port, keep the `Properties` logic verbatim in the first
pass and migrate later if you care; it's not load-bearing.

### "Java version — do I target Java 8 like always?"

Yes. Wurm's server runs on Java 8. Set
`sourceCompatibility / targetCompatibility = JavaVersion.VERSION_1_8`.
The *build* toolchain can be 17 (and should be — modern Gradle wants
it), but bytecode must be Java 8 or the classloader rejects it.

### "Why no version suffix in the jar filename?"

The loader matches `.jar` names against folder names. `spellcraft-4.3.jar`
in `mods/spellcraft/` doesn't load — it has to be `mods/spellcraft/spellcraft.jar`.
`archiveFileName.set("spellcraft.jar")` (or the equivalent for the old
Groovy DSL) strips it.

### "`import org.gotti.wurmunlimited.modloader.ReflectionUtil` — still available?"

Yes, via the legacy bridge. But the same operations exist in
`wurmmodloader-modsupport` with cleaner names and without the legacy
namespace. Both work; new code should prefer the modsupport
equivalents so the `org.gotti.*` package stays scoped to the
compatibility bridge.

### "Do I still need `sharedClassLoader=true` in the `.properties`?"

Yes for anything that touches Wurm classes — which for a combat,
spell, or item mod is "yes, always." Omitting it gives you an
isolated `URLClassLoader` that can't see `com.wurmonline.*`, which is
almost never what you want on the server side.

---

## API, modsupport, or extend the framework?

The three places your mod code interacts with the framework:

- **`wurmmodloader-api`** — events, the `@SubscribeEvent` annotation,
  mod interfaces. Import freely; this is the contract.
- **`wurmmodloader-modsupport`** — typed helpers that wrap Wurm
  internals safely (`ItemTemplateBuilder`, `ModQuestion`, capability
  storage, `ModActions`, etc.). Import when a helper exists for what
  you need.
- **Extending the framework** — adding a new event + its bytecode
  patch so every mod can subscribe. Lives in `wurmmodloader-core`.

Decision flow:

```
I need to react to something in-game.
  ├── Does the event exist?
  │     ├── Yes → subscribe via @SubscribeEvent (API)
  │     └── No → is it a real game hook I need?
  │           ├── Yes → extend the framework (new event + patch)
  │           └── No, it's "how do I do X to a Wurm object"
  │                 ├── modsupport already has a helper → use it
  │                 └── modsupport doesn't → add a helper there, not in your mod
  └── I just need data/config/state → that's your own code, no framework touch
```

**Hard rule:** mod code never directly invokes Javassist or
`ClassPool`. If you're reaching for bytecode tools, you're in
"extend the framework" territory — move the patch to
`wurmmodloader-core/.../bytecode/patches/` and expose it as an event.

---

## I want to build…

Decision tree by shape of mod. Each row points at the single doc that
will unblock you first.

| If you're building… | Start here |
|---|---|
| **A simple event-driven mod** (chat tweak, pray-rate change, timer tweak) | [`../guides/event-bus.md`](../guides/event-bus.md) — subscribe, log, test. ~50-100 LOC total. |
| **New items, recipes, creatures** | [`examples/oversizedclub/`](../../examples/oversizedclub/) end-to-end. Uses `ItemTemplateBuilder`, `ModCreatures`, recipe API — no bytecode. |
| **A balance / mechanics mod** (damage formula, skill gain, combat rule) | [`../guides/event-bus.md`](../guides/event-bus.md) for the event surface, then [`../research/combat/`](../research/combat/) if you're touching combat math specifically. |
| **A player-facing popup, form, or questionnaire** | [`../guides/ui-api.md`](../guides/ui-api.md) — works with vanilla clients, no client mod needed. Drop into [`../guides/bml-ui.md`](../guides/bml-ui.md) only if the high-level API can't express what you want. |
| **A context-menu entry or submenu** | [`../guides/ui-api-submenus.md`](../guides/ui-api-submenus.md) |
| **A GM / admin console command** | [`../reference/console-commands.md`](../reference/console-commands.md) — `#shutdown <minutes> <reason>` for safe shutdown (SIGTERM skips the DB flush) |
| **Something the framework doesn't expose yet** (new event, new widened class) | [`../guides/extending-framework.md`](../guides/extending-framework.md) — five-file recipe |
| **A client-server bridge** (HTTP, ModComm) | Server side: framework-owned helpers in modsupport. Client side: [`../../../WurmModLoader-Client/docs/guides/client-server-bridge.md`](../../../WurmModLoader-Client/docs/guides/client-server-bridge.md) |
| **A port of an Ago-era mod** | [`../migration/porting-existing-mods.md`](../migration/porting-existing-mods.md) — "it mostly just works" + the repackage-only path |

---

## Top 5 mistakes that will waste your time

In rough order of "how often I've seen this":

1. **JAR filename with a version suffix.** `mymod-1.0.0.jar` silently
   doesn't load — the loader matches `classpath=` against the exact
   filename. Set `archiveFileName.set("mymod.jar")` in
   `build.gradle.kts` and `classpath=mymod.jar` in `.properties`.
2. **Running Javassist from inside the mod jar** (as Ago-era mods
   often did). It works until the framework's own patches hit the
   same class, and then you get `cannot modify frozen class` with no
   hint of which side ran second. Move patches into the framework.
3. **Importing `com.wurmonline.*` in mod code.** Each one is a future
   break. Check `wurmmodloader-modsupport` first; if nothing fits,
   extend the framework rather than reaching into Wurm directly.
4. **Not reading `wurmlog --since-last-restart --errors` before
   asking "why doesn't it work?"** Bytecode patch failures, class-not-found
   errors, mod-load failures all show up there with the exact line
   that broke. The log tells you the answer 90% of the time.
5. **Forgetting to regenerate `codeindex` after adding classes.**
   `codeindex regen` — the index is how you (and I) find code fast;
   stale index → ghost hunts. Run it after any structural change.

---

## Where to go from here — deeper docs

Each link below is one focused topic. Skip straight to the one matching
what you're actually building.

### Core systems

| Topic | Read |
|---|---|
| Event bus internals (priorities, cancellation, custom events) | [`../guides/event-bus.md`](../guides/event-bus.md) |
| Adding events + bytecode patches | [`../guides/extending-framework.md`](../guides/extending-framework.md) |
| Player UI — high-level | [`../guides/ui-api.md`](../guides/ui-api.md) |
| Player UI — raw BML | [`../guides/bml-ui.md`](../guides/bml-ui.md) |
| Context-menu submenus | [`../guides/ui-api-submenus.md`](../guides/ui-api-submenus.md) |
| GM console commands | [`../reference/console-commands.md`](../reference/console-commands.md) |

### Reference / lookup

| Need | Doc |
|---|---|
| What's in the public API | [`reference/api-surface.md`](../reference/api-surface.md) |

### Migration

| From | To |
|---|---|
| Old Ago listener interfaces (`*Listener`) | [`migration/from-legacy.md`](../migration/from-legacy.md) |

### Reading mod source

When you want to see "how is this actually done in a real mod":

- **[`examples/hellomod/`](../../examples/hellomod/)** — minimum viable mod
- **[`examples/oversizedclub/`](../../examples/oversizedclub/)** — fully
  worked tutorial: item template, custom capability, combat stat hooks,
  crafting recipe, examine text. Read top-to-bottom.
- `examples/templatemod/` — UI-focused starter (context menu + multi-page
  questionnaire)
- `examples/basic-item-mod/`, `examples/custom-creature/`,
  `examples/action-system/` — single-topic focused examples

---

## Conventions

A few things to know before you start writing:

- **Namespace.** New mods go under `com.garward.wurmmodloader.mods.<name>` (or
  your own `com.<you>.*`). Don't put new code under `org.gotti.*` — that
  namespace is reserved for the legacy compatibility bridge.
- **No vanilla imports in mods.** If you find yourself typing
  `import com.wurmonline.…` in mod code, stop and check whether there's a
  helper in `wurmmodloader-modsupport` or an event you can subscribe to. If
  there genuinely isn't, that's a signal to extend the framework — see
  [`guides/extending-framework.md`](../guides/extending-framework.md).
- **JAR filenames must not include version numbers.** The loader matches by
  folder/file name. `hellomod.jar`, not `hellomod-1.0.0.jar`. The
  `build.gradle.kts` snippet above sets `archiveBaseName` accordingly.
- **Configs load before `init()`.** Properties and JSON files in `mods/` are
  parsed before your mod runs, so reading them in `init()` is safe.
- **Always verify in logs, not in theory.** `wurmlog --since-last-restart
  --errors` is your first stop after any change. Patches that silently fail to
  apply will show up there.

---

## Adjacent docs

- [`../../Architecture.MD`](../../Architecture.MD) — module map and the
  load-bearing boot sequence (read before touching `core/` or patches)
- [`../../CONTRIBUTING.md`](../../CONTRIBUTING.md) — contribution rules
- [`../README.md`](../README.md) — full documentation index
