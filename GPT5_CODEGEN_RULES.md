# GPT-5 Code Generation Rules - WurmModLoader Framework

## Event Creation Pattern

### 1. Event Class Location & Structure
```java
// Location: wurmmodloader-api/src/main/java/com/garward/wurmmodloader/api/events/<category>/
package com.garward.wurmmodloader.api.events.<category>;

import com.garward.wurmmodloader.api.events.base.Event;  // CRITICAL: .base.Event NOT .Event
import com.wurmonline.server.creatures.Creature;

public class MyEvent extends Event {
    // Fields, constructor, getters only - no setters
}
```

### 2. BytecodePatch Requirements
```java
// Location: wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/bytecode/patches/
package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import javassist.*;

public final class MyPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(MyPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.full.ClassName";  // FULL package path
    }

    @Override
    public String methodName() {
        return "methodName";  // REQUIRED - don't skip
    }

    @Override
    public String methodDescriptor() {
        return "(JZ)V";  // REQUIRED - JVM descriptor format
    }

    @Override
    public String displayName() {
        return "MyPatch (ClassName.methodName)";
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singletonList(BytecodeConflictKeys.MY_KEY);
    }

    @Override
    public void apply(Object hookManagerObj) {  // CRITICAL: Object type, NOT HookManager
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctClass = classPool.get(targetClassName());

            if (ctClass.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping MyPatch - class frozen");
                return;
            }

            CtMethod method = ctClass.getDeclaredMethod("methodName");

            String proxyClass = com.garward.wurmmodloader.modloader.server.ProxyServerHook.class.getName();

            method.insertAfter(
                "{ " +
                "  " + proxyClass + ".fireMyEvent(param1, param2);" +
                "}"
            );

            LOGGER.info("[BytecodePatch] Registered MyPatch successfully");
        } catch (Exception e) {
            throw new IllegalStateException("Unable to install MyPatch", e);
        }
    }
}
```

### 3. Framework Hook Chain (MANDATORY PATTERN)

**NEVER use EventBus.publish() directly in patches!**

```
BytecodePatch
  → ProxyServerHook.fireMyEvent()      // Add static method here
    → ServerHook.fireMyEvent()          // Add instance method here
      → eventBus.post(MyEvent)
```

#### ProxyServerHook method:
```java
// Location: wurmmodloader-core/.../modloader/server/ProxyServerHook.java
/**
 * Fire MyEvent (called from bytecode hook).
 */
public static void fireMyEvent(Creature creature, int value) {
    getInstance().fireMyEvent(creature, value);
}
```

#### ServerHook method:
```java
// Location: wurmmodloader-core/.../modloader/server/ServerHook.java
public void fireMyEvent(Creature creature, int value) {
    if (DEBUG) {
        logger.info(String.format("[Event] MyEvent: creature=%s, value=%d",
            creature.getName(), value));
    }

    eventBus.post(new MyEvent(creature, value));

    if (DEBUG) {
        logger.info("[Event] MyEvent: completed");
    }
}
```

### 4. Registration Checklist

- [ ] Add conflict key to `BytecodeConflictKeys.java`:
  ```java
  public static final String MY_KEY = "category.myfeature";
  ```

- [ ] Add import to `CoreBytecodePatches.java`:
  ```java
  import com.garward.wurmmodloader.core.bytecode.patches.MyPatch;
  ```

- [ ] Add to patches list in `CoreBytecodePatches.java`:
  ```java
  new MyPatch(),
  ```

### 5. Common Mistakes to AVOID

❌ `extends Event` → ✅ `extends com.garward.wurmmodloader.api.events.base.Event`
❌ `EventBus.publish()` in patch → ✅ `ProxyServerHook.fireMyEvent()`
❌ `extends BytecodePatch` → ✅ `implements BytecodePatch`
❌ Missing `methodName()` → ✅ All interface methods required
❌ `apply(HookManager h)` → ✅ `apply(Object hookManagerObj)`
❌ Forgetting import in `CoreBytecodePatches.java` → ✅ Always add import

### 6. Technology Stack

- Use **Javassist** (CtClass, CtMethod, insertBefore/insertAfter)
- Do NOT use ASM (low-level bytecode)
- Method descriptors use JVM format: `(FFFFJ)V` = (float, float, float, float, long) void

### 7. File Checklist for New Event

1. Event class: `wurmmodloader-api/.../events/<category>/MyEvent.java`
2. Patch class: `wurmmodloader-core/.../bytecode/patches/MyPatch.java`
3. Add conflict key: `wurmmodloader-api/.../bytecode/BytecodeConflictKeys.java`
4. Add ProxyServerHook method: `wurmmodloader-core/.../server/ProxyServerHook.java`
5. Add ServerHook method: `wurmmodloader-core/.../server/ServerHook.java`
6. Register patch: `wurmmodloader-core/.../bytecode/CoreBytecodePatches.java` (import + list)
