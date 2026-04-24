# Porting Existing Mods — "It Mostly Just Works"

A short, practical guide for bringing an Ago-era server mod into this
modloader. If you're wondering *"do I have to rewrite my mod around
`@SubscribeEvent`?"* — the answer is **no**. The legacy compatibility
layer keeps `org.gotti.*` imports working. Most ports are a gradle
reshuffle, not a source rewrite.

> Already sold on modernizing to the new API? See
> [`from-legacy.md`](./from-legacy.md) — the thorough interface-by-interface
> migration guide. This doc is about the *minimum* to get a legacy mod
> running here.

---

## The headline: two paths

| Path | When to pick it | Effort |
|---|---|---|
| **Repackage only** — keep `org.gotti.*` imports, wrap in new gradle build | Your mod works, you just want it on the new loader | ~30 min |
| **Modernize** — swap to `com.garward.wurmmodloader.*` + `@SubscribeEvent` | You're actively developing the mod and want priority/cancellation/type safety | Hours–days, depending on mod size |

Almost every mod in
[`WurmModLoader-CommunityMods/mods/`](../../../WurmModLoader-CommunityMods/mods/)
(timerfix, bagofholding, announcer, creatureagemod, cropmod, servermap,
serverpacks, scriptrunner, inbreedwarning, harvesthelper, WyvernMods,
spellmod) took the **repackage-only** path. Sources are byte-identical to
the Ago originals; only the gradle build and deploy layout changed.

When a mod genuinely benefited from events (powerscaling,
soulboundgear, materialsystem), it was modernized. The rest didn't
need to be.

---

## Path 1: Repackage-only port

### 1. Confirm it'll work as-is

The legacy compatibility layer supports every interface in
`org.gotti.wurmunlimited.modloader.interfaces.*`:
`WurmMod`, `WurmServerMod`, `Initable`, `PreInitable`, `Configurable`,
`ServerStartedListener`, `ServerPollListener`, `ItemTemplatesCreatedListener`,
`PlayerLoginListener`, `PlayerMessageListener`, `ChannelMessageListener`,
and more. `HookManager.getInstance().getClassPool()` still returns the
live Javassist `ClassPool`.

**If your mod only uses those imports** (plus `com.wurmonline.*` and
standard Java), you're clear to proceed without touching the source.

### 2. Create the gradle project

Drop the mod into `mods/<yourmod>/`. Use this
`build.gradle.kts` template (mirrors
[`MOD_BUILD_STANDARD.md`](../../../WurmModLoader-CommunityMods/MOD_BUILD_STANDARD.md)):

```kotlin
plugins { java }

group = "com.yourorg.wurmmods"
version = "1.0.0"

repositories { mavenCentral() }

dependencies {
    val wurmServerDir = System.getenv("WURM_SERVER_DIR")
        ?: error("Set WURM_SERVER_DIR to your Wurm Unlimited Dedicated Server install dir.")
    compileOnly(files("$wurmServerDir/server.jar", "$wurmServerDir/common.jar"))
    compileOnly("org.javassist:javassist:3.23.1-GA")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

tasks.withType<JavaCompile> { options.release.set(8) }

tasks.register<Jar>("modJar") {
    from(sourceSets.main.get().output)
    archiveBaseName.set("yourmod")
    archiveVersion.set("")                                      // no version in jar filename
    destinationDirectory.set(file("${projectDir}/build/mods/yourmod"))
}

tasks.register<Copy>("modDistribution") {
    dependsOn("modJar")
    from("mods/yourmod.properties")                              // or src/dist/
    into("${projectDir}/build/mods")
}

tasks.named("build") { dependsOn("modDistribution") }
```

Key rules from the standard:

- **`archiveVersion.set("")`** — the deployed jar name must be
  `yourmod.jar` (no `-1.0.0` suffix). The loader matches by name.
- **JAR lives in `build/mods/yourmod/`**, properties lives in
  `build/mods/` — so `build/mods/*` is drag-and-drop into the server.
- **Use `compileOnly` for server.jar / common.jar / javassist.** None
  of them ship in the jar.

### 3. Write the descriptor

`.properties` format is unchanged. Example from
[`mods/timerfix/mods/timerfix.properties`](../../../WurmModLoader-CommunityMods/mods/timerfix/mods/timerfix.properties):

```properties
classname=net.bdew.wurm.timerfix.TimerFix
classpath=timerfix/timerfix.jar
sharedClassLoader=true
```

`sharedClassLoader=true` is the common setting — it lets the mod see
Wurm's classes. Set it `false` only if you want strict isolation (and
know why).

Optional dependency graph keys (new, not in Ago):

```properties
depend.requires=soulboundgear,materialsystem
depend.after=soulboundgear,materialsystem
depend.before=someothermod
depend.conflicts=oldversion
```

### 4. Build, deploy, tail the log

```bash
./gradlew :mods:yourmod:build
cp -r mods/yourmod/build/mods/* "$WURM_SERVER/mods/"
```

Tail the log: `tail -f "$WURM_SERVER/logs/wurmmodloader.0.log" | grep YourMod`.
If you see your mod's startup message, you're done.

---

## Path 2: Modernize (selectively)

The legacy-bridge path above gets you running. You only need to
modernize when you actually benefit from it:

- Want to control handler priority → `@EventPriority(HIGHEST)`
- Want to cancel an event → `CancellableEvent.setCancelled(true)`
- Multiple mods interacting on the same hook → events give you an
  ordered bus instead of competing bytecode patches
- You want to *stop* touching bytecode and let the framework fire a
  typed event

Per-interface swap table (what replaces what):

| Old interface | New equivalent |
|---|---|
| `implements ServerStartedListener` + `onServerStarted()` | `@SubscribeEvent public void x(ServerStartedEvent e)` |
| `implements ServerPollListener` + `onServerPoll()` | `@SubscribeEvent public void x(ServerPollEvent e)` |
| `implements ItemTemplatesCreatedListener` + `onItemTemplatesCreated()` | `@SubscribeEvent public void x(ItemTemplatesCreatedEvent e)` |
| `implements PlayerLoginListener` + `onPlayerLogin(Player)` | `@SubscribeEvent public void x(PlayerLoginEvent e)` |
| `implements PlayerMessageListener` + `onPlayerMessage(Creature, String)` | `@SubscribeEvent public void x(PlayerMessageEvent e)` (cancellable) |
| Manual Javassist patch in `preInit()` for a common hook | Check [`guides/event-bus.md`](../guides/event-bus.md) — there may already be an event |

Imports change wholesale:

```
org.gotti.wurmunlimited.modloader.interfaces.*
    → com.garward.wurmmodloader.modloader.interfaces.*
(new) com.garward.wurmmodloader.api.events.base.SubscribeEvent
(new) com.garward.wurmmodloader.api.events.<area>.<Event>
```

For the full before/after walkthrough (including cancellation, priority,
mixed-mode mods that still do custom Javassist), see
[`from-legacy.md`](./from-legacy.md).

### When to *keep* bytecode patches

Modernization isn't always the right answer. TimerFix is the canonical
example — it fixes vanilla timer calculations in a dozen specific
method calls. Extracting that into events would:

1. Require firing an event for every timer calculation (hot path → perf
   hit)
2. Add framework surface area that only one mod would ever use
3. Not fit the event model — these are *calculation bugs*, not
   *behavioral hooks*

**Rule of thumb:** events are for adding or altering *behavior*;
bytecode patches are for fixing *calculation or logic*. If your mod is
correcting vanilla math, keep the patches.

See the ["When to Use Events vs Bytecode" table](../../../WurmModLoader-CommunityMods/mods/timerfix/MODERNIZATION_NOTES.md)
in timerfix for the full write-up.

---

## What actually changes, in source

From the community-mods repo, here's what each modernization category
typically touches:

- **Imports:** `org.gotti.*` → `com.garward.wurmmodloader.*` (same
  interface names mostly).
- **Mod class:** same `WurmServerMod` interface, but listener
  interfaces (`ServerStartedListener` etc.) are removed if you're
  moving those to events.
- **Hook registration:** `HookManager.getInstance().registerHook(...)` →
  gone; replaced by either `@SubscribeEvent` or (for bytecode-only
  mods) just `HookManager.getInstance().getClassPool()` still works.
- **Configuration:** `configure(Properties)` unchanged. Most mods keep
  the `.properties`-driven config; new mods with complex config load
  JSON in `preInit()` (see `powerscaling.config`).
- **Logging:** `Logger.getLogger("SomeName")` →
  `Logger.getLogger(ThisClass.class.getName())` is the preferred
  idiom, but not required.

---

## Checklist

Before deploying your ported mod, verify:

- [ ] Jar filename has **no version suffix** (`yourmod.jar`, not
  `yourmod-1.0.0.jar`)
- [ ] `.properties` classpath points at
  `yourmod/yourmod.jar` (subfolder layout)
- [ ] `sharedClassLoader=true` unless you have a reason for
  isolation
- [ ] `server.jar` / `common.jar` / `javassist` are `compileOnly`, not
  `implementation` — otherwise they'd ship in your jar and blow up
  classloading
- [ ] You ran the server once and the log shows your mod's startup
  message — not a `ModLoadException` stack trace

---

## See also

- [`from-legacy.md`](./from-legacy.md) — thorough
  interface-by-interface migration (for the modernize path)
- [`../guides/legacy-mod-compatibility.md`](../guides/legacy-mod-compatibility.md) — how the
  compat bridge actually works under the hood
- [`../guides/event-bus.md`](../guides/event-bus.md) — full event catalog, to check whether
  what you want to patch is already an event
- [`troubleshooting.md`](../guides/troubleshooting.md) — when the port fails (classloader
  errors, patch conflicts, mod doesn't load)
- Real ports to read:
  [`mods/timerfix/`](../../../WurmModLoader-CommunityMods/mods/timerfix/) (legacy bridge, kept patches),
  [`mods/powerscaling/`](../../../WurmModLoader-CommunityMods/mods/powerscaling/) (fully modernized with events)
