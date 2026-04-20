# Creature / NPC Hook Surface

What affects every creature in Wurm, what the framework currently exposes, what's still missing, and how `powerscaling` specifically plugs in. Feeds the creature-side counterpart to [`../combat/event-surface-gap.md`](../combat/event-surface-gap.md).

**Strategic principle (same as combat):** if a mod has to open `com.wurmonline.server.creatures.*` to do its job, the framework is missing an event — that is the bug, not the mod's problem.

---

## 1. Current hook surface

### Events that exist

| Event | Patch / fire site | Payload | Cancellable |
|---|---|---|---|
| `CreatureSpawnEvent` | `CreatureSpawnPatch` → `Creature.setWurmId()` | creature | No |
| `CreatureDbLoadEvent` | `DbCreatureStatusPatch` → `DbCreatureStatus` ctor | creature, resultSet | No |
| `CreatureDbSaveEvent` | `DbCreatureStatusPatch` → `DbCreatureStatus.save()` | creature, custom columns | No |
| `CreatureDeathEvent` | `CreatureDeathPatch` → `Creature.die()` | victim, killer, attackers map | Yes |
| `CreatureExamineEvent` | `CreatureExaminePatch` | creature, examine text | No |
| `CreaturePositionUpdatedEvent` | `CreaturePositionPatch` | creature, old/new tile | No |
| `CreatureBreedEvent` | `CreatureBreedPatch` | parent1, parent2, offspring | No |
| `MountEquipmentCheckEvent` | `VehicleMountCreaturePatch` | creature, item | Yes |
| `StaminaCostEvent` | (stamina cost patch) | creature, cost | Yes |
| `CombatDamageEvent` | `CombatDamagePatch` → `CombatEngine.addWound()` | attacker, defender, damage | Yes |

Event classes under `wurmmodloader-api/src/main/java/com/garward/wurmmodloader/api/events/creature/`.
Patches under `wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/bytecode/patches/Creature*.java`.

### Events that don't exist (mods hit vanilla source to get at them)

| Gap | Vanilla target | Why it's a gap |
|---|---|---|
| AI tick lifecycle | `CreatureAI.pollCreature()` — decompiled/…/creatures/ai/CreatureAI.java:248–310 | Subclasses (`WildAnimalAI`, `MonsterAI`, etc.) drive all per-creature behavior. No per-tick event. |
| Attack initiation (creature-side) | `CreatureAI.pollAttack()` (abstract, subclass-specific) | Mods can't react to "creature decided to attack target X" before the combat path runs. |
| Target selection | `CreatureAI` subclasses | No event fires when a creature picks a new target. |
| Pre-application damage | Upstream of `CombatEngine.addWound()` | `CombatDamageEvent` fires *inside* `addWound`, after the formula. The stages are in [`../combat/event-surface-gap.md`](../combat/event-surface-gap.md). |
| Creature skill gain | `Creature.addExperiencePoints()` / `Skill.skillCheck()` | Power/level mods have to patch or reimplement to tier skills. |
| Stat mutation | `Creature.setBodyPart()` and friends | HP / body reset isn't observable without patching. |
| Movement decision | `CreatureAI.pollMovement()` (abstract) | "Where is this creature going next" isn't exposed; `CreaturePositionUpdatedEvent` fires *after* movement resolves. |
| Capability-load notification | `CapabilityManager.load()` (see §2) | `CreatureDbLoadEvent` fires before capabilities are restored, so mods have to subscribe to load and then poll later. |
| Breeding eligibility | Before `Creature.breed()` runs | `CreatureBreedEvent` fires after birth; no veto hook. |
| Wound-application specifics | Inside `CombatEngine.addWound()` | Raw vs armour-reduced isn't separable. Covered by `WoundApplicationEvent` in the combat gap doc. |

`CreatureAI.pollCreature()` tick order is `pollAttack → pollBreeding → pollMovement`. Any mod wanting to react to AI decisions today has to read that file.

---

## 2. Creature ID persistence — already solved

**The problem (original):** Vanilla Wurm assigns creature IDs from a runtime sequence. They are **not persisted as stable identities across reboots**. Capabilities or per-creature data keyed by ID vanished when the server restarted — which is exactly what broke the early `powerscaling` iterations.

**The framework's fix:** the `CapabilityManager` / `CapabilityDatabase` layer uses the creature's `wurmId` as the persistent key, backed by SQLite. Because creatures are rehydrated with the same wurmId from `DbCreatureStatus`, capabilities keyed by that ID survive the reboot.

| Piece | Location |
|---|---|
| `CapabilityManager.getCreatureCapability(wurmId, cap)` | `wurmmodloader-core/.../core/capability/CapabilityManager.java:120–159` |
| `CapabilityDatabase` schema + load | `wurmmodloader-core/.../core/capability/CapabilityDatabase.java:21–30, 155–174` |
| On-disk store | `mods/wurmmodloader/capabilities.db` (SQLite, auto-created) |
| Unload / flush | `CapabilityManager.unloadCreature(wurmId)` |
| Registration | `CapabilityRegistrationEvent` (fires during bootstrap) |

**Schema:** `capability_data(entity_type, entity_id, capability_id, data, updated_at)` — `entity_id` is the creature's wurmId; `capability_id` is the mod-defined name; `data` is the serialized blob.

**Flow:**
1. Spawn → `CreatureSpawnEvent` → mod attaches capability via `CapabilityManager.getCreatureCapability(wurmId, CAP)`
2. DB load → `CreatureDbLoadEvent` → framework *silently* loads any persisted capability rows for that wurmId into memory
3. Death / unload → `CapabilityManager.unloadCreature(wurmId)` flushes dirty capabilities

### Known gap: no `CapabilityLoadedEvent`

`CapabilityManager.load()` doesn't post an event when a capability is deserialized for a creature. Mods that want to take action *after* their persisted state is back have to subscribe to `CreatureDbLoadEvent` and poll, which is awkward and order-sensitive. This should be a first-class event.

---

## 3. Creature combat shares the replaceable path

**Short answer:** creatures and players both flow through `CombatHandler.attack(Creature attacker, int defenderId, …)` → `CombatEngine.attack()` → `CombatEngine.addWound()`. There is **no branch** for creature vs. player combat at the patch site.

- `CombatAttackPatch` (`wurmmodloader-core/.../bytecode/patches/CombatAttackPatch.java:28–42`) patches `CombatHandler.attack()` indiscriminately.
- `CreatureAI.pollAttack()` subclass implementations call `CombatHandler.attack(this.creature, targetId, …)`, so they hit the same fire site.
- **Therefore: DUSKombat's `CombatAttackEvent` cancel-and-replace pattern also replaces creature combat.** If DUSKombat is loaded, a goblin swinging at a player runs through DUSKombat's formula, not vanilla's.
- `CombatDamageEvent` still fires for creature damage, because `DamageMethods.java:87–89` calls `ProxyServerHook.fireCombatDamage()` via reflection.

This means the event-surface-gap roadmap for combat ([`../combat/event-surface-gap.md`](../combat/event-surface-gap.md)) automatically covers creature AI combat as well — once the per-stage events exist, both humanoid and monster attacks flow through them.

---

## 4. How `powerscaling` plugs in

Located at `mods/powerscaling/`. Touches creatures via:

| Subscription | File:line | What it does |
|---|---|---|
| `CombatDamageEvent` | `PowerScalingMod.java:191–282` | Multiplies outgoing creature damage by `1.0 + (powerLevel * 0.05)`; reduces incoming by `1.0 + (powerLevel * 0.04)` |
| `CreatureSpawnEvent` | `PowerScalingMod.java:287–311` | Assigns `CreaturePowerCapability` with randomized base power, age growth, settlement suppression |
| `CreatureDeathEvent` | `PowerScalingMod.java:131–146` | Awards power to the killer |
| `CreatureExamineEvent` | `PowerScalingMod.java:151–186` | Appends power stats to the examine text |
| `CapabilityRegistrationEvent` | `PowerScalingMod.java:382–389` | Registers `PowerLevelCapability` (players) + `CreaturePowerCapability` (creatures) |
| `ServerStartedEvent` | `PowerScalingMod.java:399–479` | Initializes manager, settlement registry |
| `ServerPollEvent` | `PowerScalingMod.java:485–490` | Ticks age-based growth |
| `ModQueryEvent("powerscaling:power_level")` | `PowerScalingMod.java:659–674` | Public read API |
| `ModActionEvent("powerscaling:spend_power")` | `PowerScalingMod.java:686–732` | Public mutate API |
| `ModActionEvent("duskombat:calculate_damage")` | `PowerScalingMod.java:743–769` | Stacks scaling on DUSKombat's formula when DUSKombat is loaded |

**Persistence:** `CreaturePowerCapability` serializes to `basePower,spawnTimestamp,spawnTileX,spawnTileY`. Keyed by wurmId. Survives reboot because of the layer in §2.

**Where the original breakage was:** earlier versions stored per-creature data in in-memory maps keyed by `Creature.getWurmId()` with no persistence. Reboot → map cleared → every creature reverted to baseline. The capability system fixed this, but the migration points to a broader pattern: **anywhere a mod wants per-creature state, it should use `CapabilityManager`, never a raw `Map<Long, …>` on the mod instance.**

### Latent footguns in `powerscaling`

- No `CapabilityLoadedEvent` means age-based growth tick (§`ServerPollEvent`) can fire before a freshly-loaded creature's capability has been deserialized if timing is unlucky. Current code probably tolerates this via lazy-init; worth verifying.
- `CombatDamageEvent` fires inside `addWound`, i.e. post-formula. DUSKombat-compatible scaling happens via its `ModActionEvent` subscription — which is fragile because it depends on a specific mod's public API, not on a framework event. Once the combat gap doc's `WeaponBaseDamageEvent` / `DamageMultiplierEvent` exist, `powerscaling` should migrate off the DUSKombat hook.

---

## 5. Logging for per-creature bug testing

### What already exists

| Tool | Location | Scope |
|---|---|---|
| `DiagnosticServer` | `wurmmodloader-core/.../debug/DiagnosticServer.java` | HTTP JSON dump of patches, events, mods — static snapshot |
| `EventSimulator` | `wurmmodloader-core/.../core/testing/eventsim/EventSimulator.java` | Fires synthetic events for handler isolation tests |
| `CreatureDeathEventLogic` debug flag | `wurmmodloader-core/.../core/eventlogic/CreatureDeathEventLogic.java:36` | `-DdeathEventDebug=true` logs killer determination |
| `eventlister` mod | `mods/eventlister/` | Dumps fired events to log — global, not per-creature |
| Per-class `java.util.logging.Logger` | everywhere | Enable via `logging.properties` |

### The gap

Nothing today answers "log every event involving creature ID `12345`." Tracing a single problematic NPC means either enabling global event logging (spammy) or sprinkling temporary `LOGGER.info` calls across every handler that might touch it.

### Cheapest fix — `-DtraceCreatureId=<id>` filter in `ServerHook`

All creature events are built inside `ServerHook.fireXxx(…)` methods before `eventBus.post(event)`. Adding one central helper there gives per-creature tracing at one point instead of 10+ handlers:

```java
// in ServerHook
private static final long TRACE_ID = Long.getLong("traceCreatureId", -1L);

private void maybeTrace(Event e, Creature... participants) {
    if (TRACE_ID < 0) return;
    for (Creature c : participants) {
        if (c != null && c.getWurmId() == TRACE_ID) {
            logger.info("[Trace " + TRACE_ID + "] " + e.getClass().getSimpleName());
            return;
        }
    }
}
```

Called from each `fireXxx` method right before `eventBus.post(event)`. Zero cost when the flag is off (`-1L` short-circuits), rich trace when on. No new patches needed.

Optionally extend with a comma-separated list (`-DtraceCreatureIds=12345,67890`) and log the event's own `toString()` for payload detail.

### Bonus: `CapabilityLoadedEvent`

Separate from the tracer, this is a first-class gap. Firing it from `CapabilityManager.load()` after each capability is deserialized for a creature lets `powerscaling` (and any future mod) react to "my persisted state is back in memory" without polling.

---

## Roadmap

1. **Implement `-DtraceCreatureId`** in `ServerHook` — biggest-bang-for-buck, unblocks bug testing today.
2. **Add `CapabilityLoadedEvent`** — small, closes the known gap in §2.
3. **AI-tick event family** — `CreaturePollEvent`, `CreatureTargetSelectedEvent`, `CreatureAttackDecidedEvent`. These require patches into `CreatureAI` subclasses or the abstract polling method. See [`../combat/event-surface-gap.md`](../combat/event-surface-gap.md) for the analogous combat roadmap.
4. **Creature skill / stat events** — `CreatureSkillGainEvent`, `CreatureStatChangeEvent`.
5. **Breeding-eligibility event** — cancellable pre-`breed()` hook.

Acceptance criterion: the next `powerscaling`-style mod is writeable end-to-end without opening a file under `com.wurmonline.server.creatures.*`.
