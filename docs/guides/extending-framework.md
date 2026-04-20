## Extending the framework — adding events and patches

This is the repeatable recipe for surfacing a vanilla Wurm hook as a first-class event. If you find yourself wanting to open `com.wurmonline.*` from inside a mod, the correct fix is to add a framework event here instead.

**Guiding rule:** mods should never need bytecode patches. If they do, the framework has a gap — fill it using this guide.

---

### The five-file touch

Adding one event means editing (or creating) exactly these files:

| # | File | Role |
|---|---|---|
| 1 | `wurmmodloader-api/.../api/events/<area>/XxxEvent.java` | Event class — payload + mutable fields |
| 2 | `wurmmodloader-api/.../api/bytecode/BytecodeConflictKeys.java` | Add a conflict-key constant (if new) |
| 3 | `wurmmodloader-core/.../core/bytecode/patches/XxxPatch.java` | Javassist patch — injects the fire call |
| 4 | `wurmmodloader-core/.../core/bytecode/CoreBytecodePatches.java` | Register the new patch |
| 5 | `wurmmodloader-core/.../modloader/server/ProxyServerHook.java` | Static entry point the bytecode calls |
| 6 | `wurmmodloader-core/.../modloader/server/ServerHook.java` | Builds the event, posts to the bus, returns the (possibly modified) value |

The two "small hook" files that get touched every time are **ProxyServerHook** (static entry called from the Javassist snippet) and **ServerHook** (posts to the bus).

Optional: `wurmmodloader-core/.../core/eventlogic/<area>/` — for non-trivial dispatch / resolution / util logic that doesn't belong inside the event class itself (see `CreatureDeathEventLogic.java`, `WoundUtil.java`).

---

### Step 1 — Find the injection point

Use `wurmquery`, not grep, on the decompiled server/client source:

```bash
wurmquery search addWound            # find the target method
wurmquery search getModifiedDamageForWeapon
wurmquery file com/wurmonline/server/combat/CombatEngine.java
```

You need three things from the vanilla method:
- **Fully qualified class name** — `com.wurmonline.server.combat.CombatEngine`
- **Method name** — `addWound`
- **JVM method descriptor** — the `(Lfoo/Bar;I...)Z` string. See "Descriptor cheat sheet" below.

---

### Step 2 — Write the event class

Minimal read-only event (observer only):

```java
package com.garward.wurmmodloader.api.events.combat;

import com.garward.wurmmodloader.api.events.Event;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

public class XxxEvent extends Event {
    private final Creature attacker;
    private final Item weapon;

    public XxxEvent(Creature attacker, Item weapon) {
        this.attacker = attacker;
        this.weapon = weapon;
    }

    public Creature getAttacker() { return attacker; }
    public Item getWeapon() { return weapon; }
}
```

Mutable event (mods can change a value before it's used):

```java
public class CombatSwingSpeedEvent extends Event {
    private final Creature attacker;
    private final Item weapon;
    private final float baseSpeed;
    private float swingSpeed;

    public CombatSwingSpeedEvent(Creature attacker, Item weapon, float baseSpeed) {
        this.attacker = attacker;
        this.weapon = weapon;
        this.baseSpeed = baseSpeed;
        this.swingSpeed = baseSpeed;
    }

    public float getSwingSpeed() { return swingSpeed; }
    public void setSwingSpeed(float v) { this.swingSpeed = v; }
    // + getters for attacker/weapon/baseSpeed
}
```

Cancellable event: pass `true` to `super(...)` — see `Event.java` for the constructor. Subscribers check `event.isCancelled()`.

Organize under `api/events/<area>/` (combat, items, creatures, world, ui, …) — match existing layout.

---

### Step 3 — Write the bytecode patch

Template (canonical: `CombatDamagePatch.java`):

```java
package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;

import javassist.*;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

public final class XxxPatch implements BytecodePatch {
    private static final Logger LOGGER = Logger.getLogger(XxxPatch.class.getName());

    @Override public String targetClassName() { return "com.wurmonline.server.foo.BarClass"; }
    @Override public String methodName()      { return "someMethod"; }
    @Override public String methodDescriptor() { return "(Lcom/wurmonline/server/creatures/Creature;F)F"; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hm = (HookManager) hookManagerObj;
        try {
            ClassPool pool = hm.getClassPool();
            CtClass target = pool.get(targetClassName());
            CtMethod m = target.getMethod(methodName(), methodDescriptor());

            // Option A: observe / modify a param BEFORE the method body runs.
            // $1, $2, ... = params. $0 = this. $_ = return value (insertAfter only).
            StringBuilder code = new StringBuilder();
            code.append("{\n  try {\n");
            code.append("    $2 = ").append(ProxyServerHook.class.getName())
                .append(".fireXxxEvent($1, $2);\n");
            code.append("  } catch (Exception e) {\n");
            code.append("    java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n");
            code.append("      .log(java.util.logging.Level.WARNING, \"fireXxxEvent failed\", e);\n");
            code.append("  }\n}\n");
            m.insertBefore(code.toString());

            // Option B: rewrite the return value after the method runs
            // m.insertAfter("$_ = " + ProxyServerHook.class.getName() + ".fireXxxEvent($1, $_);");

            LOGGER.info("Registered XxxPatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install XxxPatch", e);
        }
    }

    @Override public int priority() { return 55; }
    @Override public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.XXX);  // add a constant if new
    }
}
```

**Javassist cheatsheet:**
- `$0` → `this` · `$1..$n` → params · `$_` → return value (only inside `insertAfter`)
- `$args` → `Object[]` of all args · `$r` → return type · `$class` → declaring class
- `insertBefore` runs before the method body. `insertAfter` runs before the real return.
- Always wrap the call in `try/catch(Exception)` — an exception in an injected snippet will crash vanilla code paths.
- Mutating `$N = …` actually rewrites the local; vanilla will see the new value.

**Priority:** lower numbers run earlier. Most combat patches use `55`. Use a unique conflict key if two patches target the same method signature.

---

### Step 4 — Register the patch

`wurmmodloader-core/.../core/bytecode/CoreBytecodePatches.java` — append to the list:

```java
new XxxPatch(),
```

That's it. `PatchRegistry.add()` is called for you at startup.

---

### Step 5 — ProxyServerHook static entry point

`wurmmodloader-core/.../modloader/server/ProxyServerHook.java` — one-liner that the Javassist code calls:

```java
public static float fireXxxEvent(Creature attacker, float value) {
    return getInstance().fireXxx(attacker, value);
}
```

Return type must match what the patch assigns back (`$N = …` or `$_ = …`). For void / observer events, return `void` and skip the assignment in the patch.

---

### Step 6 — ServerHook fire method

`wurmmodloader-core/.../modloader/server/ServerHook.java` — build the event, post, return:

```java
public float fireXxx(Creature attacker, float value) {
    XxxEvent event = new XxxEvent(attacker, value);
    eventBus.post(event);
    return event.getValue();   // modified value, or `value` if nothing subscribed
}
```

Cancellable variant:
```java
public boolean fireXxx(Creature attacker) {
    XxxEvent event = new XxxEvent(attacker);
    eventBus.post(event);
    return !event.isCancelled();
}
```

---

### Step 7 — Build, deploy, regen the index

```bash
./build-and-deploy.sh
codeindex regen
```

Watch the server log for `Registered XxxPatch` at boot. If the patch silently no-ops, the descriptor is wrong (see cheat sheet).

---

### Step 8 — Mod-side subscription

Once the event exists, a mod subscribes with zero Wurm imports beyond the event's payload types:

```java
@SubscribeEvent(priority = EventPriority.HIGH)
public void onXxx(XxxEvent e) {
    if (someCondition(e.getAttacker())) {
        e.setValue(e.getValue() * 1.15f);
    }
}
```

---

### Descriptor cheat sheet

JVM method descriptors are position-sensitive. Get them wrong and `getMethod()` throws `NotFoundException`.

| Java type | Descriptor |
|---|---|
| `boolean` | `Z` |
| `byte` `short` `int` `long` | `B` `S` `I` `J` |
| `float` `double` | `F` `D` |
| `char` | `C` |
| `void` (return only) | `V` |
| `SomeClass` | `Lcom/pkg/SomeClass;` |
| `int[]` | `[I` |
| `String[]` | `[Ljava/lang/String;` |

Format: `(ParamDescriptors)ReturnDescriptor`. Example: `addWound(Creature, Creature, byte, int, double, float, String, Battle, float, float, boolean, boolean, boolean, boolean) → boolean` becomes:

```
(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/creatures/Creature;BIDFLjava/lang/String;Lcom/wurmonline/server/combat/Battle;FFZZZZ)Z
```

Quick way to get the real descriptor: `javap -s -p -classpath <path-to-server.jar> com.wurmonline.server.combat.CombatEngine | grep -A1 addWound`.

---

### Common pitfalls

- **Silent no-op** — `methodDescriptor()` doesn't match an overload. Log at `CannotCompileException` is your friend; also try `javap -s`.
- **Wrong `$N`** — Javassist counts only method params, not `this`. `$1` is the first param.
- **`insertAfter` can't see params as locals in some control paths** — if you need post-hook access to a param, capture it into a `$_` expression or stash in a local with `m.addLocalVariable` before `insertAfter`.
- **Event fires but nothing mutates** — the patch is probably not reassigning `$N = …`. Observer-style events are fine; mutators must write back.
- **Conflict with another patch** — same class+method+conflict-key collides. Use a distinct `BytecodeConflictKeys` constant.
- **Static method injection** — no `$0`; `$1` is the first real param.
- **Classloader** — patches target vanilla classes only. Don't target framework/mod classes with Javassist; use events.

---

### Reference implementations to copy from

- `core/bytecode/patches/CombatDamagePatch.java` — `insertBefore`, mutate `$5`, try/catch wrapper
- `core/bytecode/patches/CombatSwingSpeedPatch.java` paired with `api/events/combat/CombatSwingSpeedEvent.java` — mutable float event
- `core/bytecode/patches/CropHarvestPatch.java` — `ExprEditor` pattern when `insertBefore`/`insertAfter` is too coarse
- `core/bytecode/patches/ActionArrayBoundsCheckPatch.java` — defensive guard (no event)

---

### Checklist

- [ ] Event class under `wurmmodloader-api/.../api/events/<area>/`
- [ ] Conflict key added in `wurmmodloader-api/.../api/bytecode/BytecodeConflictKeys.java` (if new)
- [ ] Patch class under `wurmmodloader-core/.../core/bytecode/patches/`
- [ ] Entry added to `wurmmodloader-core/.../core/bytecode/CoreBytecodePatches.java`
- [ ] `fireXxxEvent` static entry in `wurmmodloader-core/.../modloader/server/ProxyServerHook.java`
- [ ] `fireXxx` implementation in `wurmmodloader-core/.../modloader/server/ServerHook.java`
- [ ] (If complex) dispatch/util helpers in `wurmmodloader-core/.../core/eventlogic/<area>/`
- [ ] `./build-and-deploy.sh` runs clean
- [ ] `codeindex regen`
- [ ] Server log shows `Registered XxxPatch`
- [ ] A subscriber handler (even a logging one) verifies it fires
