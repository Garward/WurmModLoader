# Legacy Mod Compatibility

## Overview

WurmModLoader supports drag-and-drop compatibility with legacy mods from Ago's modloader (org.gotti namespace). However, this comes with **important trade-offs** regarding the event system.

## The Frozen Class Problem

### What Happens

Legacy mods use Javassist to directly patch game classes in their `preInit()` method. When Javassist patches a class, it **freezes** that class, making it immutable. Once frozen, no further bytecode modifications can be applied.

**Timeline:**
1. Mods' `preInit()` runs (legacy mods patch classes here)
2. SystemBootstrap applies bytecode patches (modern event system)
3. **If a class is already frozen, bytecode patches are skipped**

### Impact on Event System

The modern event system relies on bytecode patches to inject event firing code. If a legacy mod freezes a class before our patches apply:

- ❌ **Events won't fire** for methods in that class
- ❌ **Mods subscribing to those events won't be notified**
- ✅ Legacy mod's functionality still works
- ✅ Java access to the class remains normal

### Example: Bag of Holding

The bag of holding mod from Ago's modloader patches these classes:
- `com.wurmonline.server.items.Item`
- `com.wurmonline.server.behaviours.Action`
- `com.wurmonline.server.creatures.Creature`

**Affected events when bag of holding is enabled:**
- `CreatureDeathEvent` - Won't fire (Creature class frozen)
- `ItemTradeEvent` - May not fire (Item class frozen)
- `ItemDropEvent` - May not fire (Item class frozen)

**Unaffected events:**
- `ServerStartedEvent` - Still works (server startup classes not touched)
- `CombatDamageEvent` - Still works (combat code separate from Creature.die)
- `ItemTemplatesCreatedEvent` - Still works (template system separate)

## Detection and Warnings

When a bytecode patch is skipped due to a frozen class, you'll see warnings:

```
WARNING: [BytecodePatch] Skipping CreatureDeathPatch - class already frozen by legacy mod
WARNING: [BytecodePatch] Events for com.wurmonline.server.creatures.Creature may not fire
WARNING: [BytecodePatch] Consider porting legacy mod to event system
```

These warnings indicate:
- Which patch was skipped
- Which class is frozen
- What events might not work

## Decision Matrix

### When to Use Legacy Mods

✅ **Use legacy mods if:**
- You need the mod's functionality immediately
- The mod doesn't conflict with events you need
- You plan to port it to the event system later
- You're testing mixed legacy/modern mod compatibility

❌ **Don't use legacy mods if:**
- You need full event system coverage
- Other mods depend on the affected events
- The legacy mod is small enough to port quickly
- You want predictable event behavior

### Checking Compatibility

To determine what classes a legacy mod touches:

1. **Check the mod's source** in `preInit()`:
   ```java
   ClassPool classPool = HookManager.getInstance().getClassPool();
   CtClass ctClass = classPool.get("com.wurmonline.server.SomeClass"); // ← This class will be frozen
   ```

2. **Run the server** and check logs for frozen class warnings

3. **Cross-reference** with the event system:
   - CreatureDeathPatch → `com.wurmonline.server.creatures.Creature`
   - CombatCriticalHitPatch → `com.wurmonline.server.combat.CombatEngine`
   - ItemTemplatesCreatedPatch → (no freezing - uses listener pattern)

## Migration Path

To convert a legacy mod to the event system:

1. **Identify what the mod does** in its bytecode patches
2. **Check if an event exists** for that hook point
3. **If event exists**: Replace bytecode patch with `@SubscribeEvent` handler
4. **If event doesn't exist**: Request new event in framework

### Example: Converting Creature Death Hook

**Legacy approach (bag of holding):**
```java
@Override
public void preInit() {
    ClassPool classPool = HookManager.getInstance().getClassPool();
    CtClass ctCreature = classPool.get("com.wurmonline.server.creatures.Creature");
    CtMethod dieMethod = ctCreature.getMethod("die", "(ZLjava/lang/String;)V");
    dieMethod.insertAfter("{ /* custom logic */ }");
}
```

**Modern approach (event system):**
```java
@SubscribeEvent
public void onCreatureDeath(CreatureDeathEvent event) {
    // Custom logic here
    if (event.isPlayerKill()) {
        // Handle player kill
    }
}
```

**Benefits of modern approach:**
- No class freezing
- No conflicts with other mods
- Cleaner code
- Can be cancelled/modified by other mods

## Current Status (Phase 4)

**Framework stance:**
- ✅ Legacy compatibility layer exists (wurmmodloader-legacy module)
- ✅ Frozen class handling prevents crashes
- ⚠️ Event coverage gaps documented
- 📋 Phase 5 (Legacy Bridge Layer) will address systematic migration

**Known legacy mods:**
- Bag of Holding (Ago) - Freezes Item, Action, Creature
- (Add others as discovered)

**Recommended action:**
- Document which legacy mods you're using
- Note which events they affect
- Plan migration in Phase 5 or 6
- For critical mods, consider porting immediately

## Technical Details

### Initialization Order

The current initialization sequence prioritizes legacy compatibility:

```
1. CapabilityHooks.installHooks()
2. ModLoader.loadModsFromModDir()
   ├── Mods' configure()
   └── Mods' preInit() ← LEGACY MODS PATCH HERE
3. SystemBootstrap.initializeAll() ← BYTECODE PATCHES (may be blocked)
4. ModActions/ModItems/ModCreatures.init()
5. ServerHook.createServerHook()
6. ModComm.init()
```

This order ensures:
- Legacy mods can patch classes before they're loaded
- Bytecode patches layer on top (if class not frozen)
- Framework hooks apply last

### Why Not Reverse the Order?

**Why can't bytecode patches apply first?**
- Classes must be unloaded when patches apply
- Once a patch loads a class, it's frozen
- Legacy mods in preInit() would fail if classes already loaded

**The fundamental conflict:**
- Legacy mods need unloaded classes to patch
- Bytecode patches need unloaded classes to patch
- Both can't patch the same class

## Workarounds

### Option 1: Selective Legacy Support
Enable/disable legacy mods based on event needs:
- Testing: Enable bag of holding, accept event gaps
- Production: Disable bag of holding, use ported version

### Option 2: Event Emulation
Manually fire events from legacy mod code:
```java
// In legacy mod's preInit() hook
dieMethod.insertAfter("{" +
    "  com.garward.wurmmodloader.modloader.server.ProxyServerHook" +
    "    .fireCreatureDeathEvent(this, killer);" +
    "}");
```

### Option 3: Port to Events
Convert legacy mod to use `@SubscribeEvent` (recommended long-term)

## Future Plans

**Phase 5: Legacy Bridge Layer**
- Systematic analysis of legacy mod patterns
- Automated detection of frozen classes
- Migration tooling and documentation
- Compatibility matrix for common mods

**Phase 6+: Mod Ecosystem**
- Official ports of popular legacy mods
- Community migration guidelines
- Event coverage expansion for common hook points

## Questions?

See:
- `WURMMODLOADER_MODERNIZATION_PLAN.md` - Overall project roadmap
- `CLAUDE.md` - Event system architecture (Phase 3)
- `PHASE4_USAGE_EXAMPLES.md` - Modern API examples
