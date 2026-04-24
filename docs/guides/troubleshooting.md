# Troubleshooting — Server Mods

When something goes wrong, it's almost always one of these. Symptoms →
root cause → fix. Read top-to-bottom the first time; bookmark it for
next time.

---

## Where to look first

Every server mod problem eventually surfaces in one of these logs (relative
to your server install dir, `<wurm-server-dir>`):

- `<wurm-server-dir>/logs/wurmmodloader.0.log`
  — framework + mod loading + startup stack traces
- `<wurm-server-dir>/<WORLDNAME>/Logs/*.log`
  — per-world gameplay logs

Default Steam locations for `<wurm-server-dir>`:

- **Windows:** `C:\Program Files (x86)\Steam\steamapps\common\Wurm Unlimited Dedicated Server\`
- **Linux:**   `~/.local/share/Steam/steamapps/common/Wurm Unlimited Dedicated Server/`

Useful one-liners (set `LOG=<wurm-server-dir>/logs/wurmmodloader.0.log` first):

```bash
grep -E "SEVERE|WARNING" "$LOG"                # all errors/warnings
grep -A 20 "SEVERE" "$LOG"                     # errors with following stacktrace lines
grep -i "yourmod" "$LOG"                       # anything mentioning your mod
tail -f "$LOG"                                 # live tail
tail -f "$LOG" | grep --line-buffered YourMod  # live tail filtered to your mod
```

JUL log records are two lines (`<timestamp> <class> <method>` then `LEVEL: message`),
so use `grep -A N` / `grep -B N` to keep stacktrace context glued to its `SEVERE`.

---

## Mod doesn't load at all

### Symptom: no startup message, no error

```bash
grep -i "yourmod" "$LOG"
# (no output)
```

The loader never saw the mod. Check:

1. **Descriptor lives in the right place.**
   `~/.../mods/yourmod.properties` — directly in `mods/`, not in a
   subfolder.
2. **JAR filename matches the descriptor.** `classpath=yourmod/yourmod.jar`
   in the `.properties` means the jar must be at
   `~/.../mods/yourmod/yourmod.jar` — *exactly*. A `-1.0.0` version
   suffix on the jar breaks this.
3. **Descriptor has `classname=`.** Missing or misspelled is a silent
   skip.

### Symptom: `ClassNotFoundException: com.yourorg.YourMod`

The descriptor's `classname=` points at a class the loader can't find.
Causes:

- `classpath=` in the `.properties` doesn't match the jar location.
- The jar is empty or corrupt. Check `jar tf yourmod.jar` — your class
  should be listed at `com/yourorg/YourMod.class`.
- You accidentally shipped `server.jar` / `common.jar` inside your mod
  jar (you used `implementation` instead of `compileOnly` in gradle),
  and the duplicate classloader breakage eats your own class.

### Symptom: `ModLoadException: dependency not satisfied`

You have `depend.requires=foo` but `foo` isn't installed or failed to
load. Either install it or drop the dependency. Load-order-only
dependencies go in `depend.after`, not `depend.requires`.

---

## Mod loads but does nothing

### Event handler never fires

Checklist, in order:

1. **`@SubscribeEvent` annotation present?** The event bus only
   dispatches to annotated methods. Forgetting the annotation is the
   #1 silent failure.
2. **Method is `public`?** The bus ignores package-private methods.
3. **Exactly one parameter, and it extends `Event`?** `void
   handler(String foo)` won't be picked up.
4. **The class registered with the bus?** For modern mods this happens
   automatically when `classname=` in the descriptor points at the
   class. If your handler is on a *different* class, that class needs
   to be registered (framework handles the descriptor class; anything
   else is on you).
5. **The event actually fires during your test.** Check with:
   `grep "YourEvent" "$LOG"`. If the framework
   never fired it, your handler has nothing to react to.

### Legacy-bridge listener not called

If you ported a mod with `implements ServerStartedListener` and
`onServerStarted()` never runs:

- Confirm you implemented `WurmServerMod` (or `WurmMod`) too — the
  bridge only looks at mods that declare themselves.
- The legacy bridge ignores listeners that aren't in the Ago interface
  set. Custom listener interfaces won't work — use events instead.

---

## Bytecode patch failed

### Symptom: `NotFoundException: com.wurmonline.server.Foo`

Your patch target class doesn't exist, was renamed, or isn't in the
Wurm version you're running.

1. Confirm the class exists in the current `server.jar`:
   `unzip -l server.jar | grep -i Foo`.
2. If it does but the full path differs, fix the `classPool.getCtClass(...)`
   argument.
3. If it doesn't, you're targeting a class that was renamed between WU
   patches — decompile `server.jar` (CFR / Vineflower / Procyon) and
   search there.

### Symptom: `NotFoundException` on a method (but the class exists)

Method signature mismatch. Javassist needs the exact descriptor:

```java
ctClass.getMethod("breed", "(Lcom/wurmonline/server/creatures/Creature;...)Z")
```

Use `javap -s -p -classpath server.jar com.wurmonline.server.Foo | grep -A1 methodname`
to read the exact descriptor from the current jar, and copy it verbatim.

### Symptom: `cannot modify frozen class`

Another mod already loaded and modified the class before yours. Two
causes:

- **Load order.** Add `depend.after=theothermod` to your `.properties`
  so you run first. Or `depend.before` on the other mod if you own
  both.
- **You're using `init()` instead of `preInit()`.** Bytecode patches
  must run in `preInit()`. By `init()`, Wurm classes are loaded and
  frozen.

### Symptom: patch compiles, mod loads, but runtime behavior didn't change

Almost always: the patched class was loaded *before* your patch ran.
Verify with:

```bash
grep -E "Applied .* to .*Foo" "$LOG"
```

If the log line is there but the behavior didn't change, your patch
code is wrong (wrong method, `$proceed` args mismatched, etc.). If
it's *not* there, the patch silently didn't apply — check for a
surrounding error and confirm `preInit()` was called.

---

## Mod conflicts & load order

### Two mods patch the same method

With legacy Javassist, the second mod to patch wins — but only if the
first mod hasn't frozen the class. In practice you get either "second
mod's patch silently replaces first mod's" or "second mod's patch
throws `cannot modify frozen class`".

**Fix:** convert at least one of them to an event. If both patches
need to run, events via the bus are ordered by `EventPriority` and
both handlers run in sequence. See [`event-bus.md`](event-bus.md) for
priority semantics.

### Mod A needs Mod B's data at startup

Use `depend.requires=modb` (hard) or `depend.after=modb` (soft, for
load order only). Without it, nothing guarantees Mod B has finished
`init()` when Mod A runs.

---

## Build problems

### `compileOnly` vs `implementation`

Wurm jars, Javassist, and the framework modules must be **`compileOnly`**.
If they're `implementation`, gradle packs copies of Wurm classes into
your mod jar → the mod classloader sees two copies of
`com.wurmonline.server.Server` and fails with
`LinkageError: loader constraint violated`.

Correct:
```kotlin
compileOnly(files("$wurm/server.jar", "$wurm/common.jar"))
compileOnly("org.javassist:javassist:3.23.1-GA")
```

### Mod jar has a version suffix

`archiveVersion.set("")` in the gradle build is non-negotiable. The
loader matches jars by exact name against the `classpath=` in the
`.properties`. `yourmod-1.0.0.jar` won't match `classpath=yourmod/yourmod.jar`.

### Java version mismatch

Wurm runs Java 8 bytecode. Your build uses Java 17 toolchain but must
*emit* Java 8:

```kotlin
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}
tasks.withType<JavaCompile> { options.release.set(8) }
```

Symptoms of wrong target: mod loads, but the first call into Wurm
throws `UnsupportedClassVersionError`.

---

## Runtime crashes

### `NoSuchMethodError` at runtime

The method existed when you compiled but not when the server ran.
Causes:

- Wurm updated between your build and the current server jar. Rebuild
  against the new jars.
- You compiled against framework v0.X but the deployed framework is
  v0.Y with a different API. Redeploy the framework or rebuild.

### `ClassCastException` after a classloader boundary

A mod with `sharedClassLoader=false` passed an object to another
classloader that re-loads the same class — two different `Class<?>`
instances, one can't be cast to the other. Either:

- Use `sharedClassLoader=true` (simpler, what most mods want), or
- Pass only primitives / `String` / framework-owned types across the
  boundary.

### Server hangs / never finishes loading

Usually a mod's `preInit()` or `init()` is in an infinite loop, or
waiting on something that never arrives. Check:

```bash
tail -f "$LOG"
```

The last mod to log before the hang is your suspect. Thread dump with
`jstack <pid>` if you can pin down the process.

### Server crashes during shutdown (lost world data)

**Don't** `Ctrl-C` or `kill` the server — that skips the DB flush.
Use the in-game GM console:

```
#shutdown 5 "restart for mod update"
```

Or from server console with five minutes of warning. See
[`../reference/console-commands.md`](../reference/console-commands.md).

---

## Quick log patterns

| Symptom in log | Likely cause |
|---|---|
| `ClassNotFoundException` on your own class | jar not deployed, `classpath=` wrong, or `implementation` vs `compileOnly` leak |
| `NotFoundException` on `com.wurmonline.*` | wrong class/method name in Javassist call |
| `cannot modify frozen class` | ran patch in `init()` or load-order issue, another mod got there first |
| `LinkageError: loader constraint violated` | duplicate Wurm classes shipped in your jar |
| `UnsupportedClassVersionError` | jar compiled for wrong Java target |
| `ModLoadException: dependency not satisfied` | `depend.requires=` references a mod that isn't loaded |
| Silent handler no-op | missing `@SubscribeEvent` / wrong method signature |

---

## When you really can't figure it out

1. `grep -E "SEVERE|WARNING" -A 20 "$LOG"` — every error/warning with
   following stacktrace lines.
2. `grep -B 1 -A 5 YourMod "$LOG"` — your mod's records with surrounding
   context.
3. Reproduce against [`examples/hellomod/`](../../examples/hellomod/) —
   the smallest possible mod. If hellomod works and yours doesn't,
   the problem is in your mod. If hellomod also fails, the framework
   or deployment is broken.
4. Compare your `.properties` and `build.gradle.kts` against a known-good
   community mod — e.g.
   [`mods/timerfix/`](../../../WurmModLoader-CommunityMods/mods/timerfix/) for a
   legacy-bridge mod,
   [`mods/powerscaling/`](../../../WurmModLoader-CommunityMods/mods/powerscaling/) for
   a fully modernized one.

---

## See also

- [`../migration/porting-existing-mods.md`](../migration/porting-existing-mods.md) — how to bring an Ago-era mod over
- [`../migration/from-legacy.md`](../migration/from-legacy.md) — full interface-by-interface migration
- [`event-bus.md`](event-bus.md) — event system (priority, cancellation, catalog)
- [`../reference/console-commands.md`](../reference/console-commands.md) — GM commands for safe shutdown / diagnostics
