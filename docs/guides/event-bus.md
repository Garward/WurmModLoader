# Event Bus

The event bus is how mods react to things that happen on the server — players
logging in, items taking damage, weapons swinging, creatures dying, the
server starting. You annotate a method, the bus calls it. No interface
implementation, no manual registration.

> **When to use this:** You want your mod to *react* to existing game
> behavior. If you instead need to *introduce* a new event the framework
> doesn't expose yet (because it requires a new bytecode patch), see
> [`extending-framework.md`](extending-framework.md).

---

## The shape of a handler

```java
import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;

public class MyMod implements WurmServerMod {
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Server is up. Do your thing.
    }

    @Override public String getVersion() { return "1.0.0"; }
}
```

That's it. Drop the file in your mod, set `classname=` in your
`.properties`, deploy. The framework discovers `@SubscribeEvent` methods on
your mod class automatically.

### Rules a handler must follow

- The method is `public` (or `protected`)
- Returns `void`
- Takes **exactly one** parameter, which extends `Event`
- The parameter type matches the event class exactly (no superclass / wildcard
  matching)

If a handler isn't firing, 9 times out of 10 it's one of those four. Check
the server log (`<wurm-server-dir>/logs/wurmmodloader.0.log`) for the
word `subscriber` — registration failures are logged.

---

## Priorities

Five levels, default `NORMAL`. Handlers run in this order:

`HIGHEST → HIGH → NORMAL → LOW → LOWEST`

```java
@SubscribeEvent(priority = EventPriority.HIGH)
public void onPlayerLogin(PlayerLoginEvent event) { … }
```

Use this when ordering matters across mods — e.g. a ban check should run
`HIGHEST` so it cancels before logging handlers see the event; cleanup or
audit-log handlers belong at `LOWEST`. If you don't need ordering, leave it
at `NORMAL` and don't think about it.

---

## Cancellation

Some events are cancellable — calling `event.setCancelled(true)` (or
`event.cancel()`) prevents the underlying game action. Cancelling a
`PlayerLoginEvent` denies the login. Cancelling an `ItemDamageEvent` skips
the damage application. Each event's Javadoc says whether it's cancellable
and what cancellation means for that event specifically.

By default, **cancelled events skip remaining handlers**. To audit/log even
cancelled events, opt in:

```java
@SubscribeEvent(receiveCancelled = true)
public void auditLogins(PlayerLoginEvent event) {
    if (event.isCancelled()) {
        logger.info("Blocked login: " + event.getKickMessage());
    }
}
```

Trying to cancel a non-cancellable event throws
`UnsupportedOperationException`.

---

## Event catalog

All events live under
[`wurmmodloader-api/.../api/events/`](../../wurmmodloader-api/src/main/java/com/garward/wurmmodloader/api/events/).
Read the source for full Javadoc and field list — this table is a discovery
index, not a reference.

### Server lifecycle — `events/server/`

| Event | When it fires | Notes |
|---|---|---|
| `ServerStartedEvent` | `ServerLauncher.runServer` returns — core init done | Register-only work (content, handlers). **NOT** "fully operational" — Steam, DB pool warmup, and the console reader are still async. |
| `ServerFullyReadyEvent` | `CommandReader.run` begins — truly settled | DB sync, migrations, pool-dependent work. Replaces the old "sleep N seconds after `ServerStartedEvent`" pattern. |
| `ServerStoppingEvent` | Server is shutting down cleanly | Save state, close connections |
| `ServerPollEvent` | Every server tick | **Hot path — keep handlers tiny.** Throttle internally. |
| `CapabilityRegistrationEvent` | Capability registration phase | Register custom item/player/creature data here |
| `CapabilityLoadedEvent` | A specific capability finished loading | Reactive followups |

### Player — `events/player/`

| Event | Cancellable | Notes |
|---|---|---|
| `PlayerLoginEvent` | ✅ | `setKickMessage(...)` to tell the player why |
| `PlayerLogoutEvent` | ❌ | Save per-player state |
| `PlayerDeathEvent` | ❌ | |
| `PlayerMessageEvent` | ✅ | Chat / commands; cancel to suppress |
| `ChannelMessageEvent` | ✅ | Kingdom / village / alliance channels |
| `PlayerSkillLossEvent` | ✅ | E.g. on death |
| `PrayerFaithEvent` | — | |
| `PriestRestrictionCheckEvent` | — | Override priest-can-do-X rules |
| `BodyMenuPopulateEvent` | — | Right-click-self menu — see also `ui-api-submenus.md` |

### Item — `events/item/` (and `events/item/material/`)

| Event | Notes |
|---|---|
| `ItemTemplatesCreatedEvent` | Earliest safe point to call `ItemTemplateBuilder` |
| `ItemExamineEvent` | Add lines to the examine text via `event.addDescription(...)` |
| `ItemDamageEvent` | Modify or cancel item damage |
| `ItemDropEvent` | Player drops an item |
| `ItemTradeEvent` | Trade window completion |
| `ItemEnchantmentStringsEvent` | Custom enchantment names / descriptions |
| `ContainerVolumeEvent` | Adjust how much a container can hold |
| `MaterialBonusEvent`, `MaterialDamageModifierEvent`, `MaterialDecayModifierEvent`, `MaterialImpBonusEvent`, `MaterialRepairTimeEvent` | Per-material gameplay tweaks |

### Combat — `events/combat/` (and `events/combat/weapon/`, `events/combat/shield/`)

| Event | Notes |
|---|---|
| `CombatAttackEvent` | High-level swing dispatch |
| `CombatCriticalHitEvent` | Override crit chance per-swing |
| `CombatDualWieldEvent` | Dual-wield decisions |
| `CombatRatingEvent` | Combat rating computation |
| `CombatSwingSpeedEvent` | Per-swing speed override |
| `OpportunityAttackEvent` | Free swings on missed attacks |
| `SpecialMoveSendEvent`, `SpecialMoveHandleEvent` | Special move requests / dispatches |
| `WeaponUseEvent` | Generic weapon-was-used hook |
| `WeaponStatQueryEvent` | Per-item stat tweaks (damage / speed / parry / armour-dmg) — used by OversizedClub |
| `ShieldCheckEvent`, `ShieldDamageEvent` | Shield blocking + durability |

### Creature — `events/creature/`

| Event | Notes |
|---|---|
| `CreatureSpawnEvent` | New creature spawned |
| `CreatureDeathEvent` | Creature died |
| `CreatureExamineEvent` | Add lines to examine text |
| `CreatureBreedEvent` | Breeding outcome |
| `CombatDamageEvent` | Damage about to be applied to a creature (note: lives under `creature/`, not `combat/`) |
| `CreaturePositionUpdatedEvent` | Position changed |
| `CreatureDbLoadEvent`, `CreatureDbSaveEvent` | Persistence hook points |
| `MountEquipmentCheckEvent` | "Can this creature be ridden / saddled?" |
| `PetReleasedEvent` | Pet release |
| `StaminaCostEvent` | Adjust stamina drain |
| `TameAttemptEvent`, `TameCompleteEvent` | Taming workflow |

### Skill — `events/skill/`

| Event | Notes |
|---|---|
| `SkillAdvanceEvent` | Skill went up — modify gain or cancel |
| `SkillCheckEvent` | Skill roll about to happen |
| `SkillDifficultyEvent` | Difficulty adjustment |

### Action — `events/action/`

| Event | Notes |
|---|---|
| `ActionTimeCalculationEvent` | How long the action takes |
| `ActionSpeedModifierEvent` | Speed multiplier |
| `ActionFatigueEvent` | Fatigue cost |

### Movement / vehicles / sync — `events/movement/`, `events/vehicle/`, `events/sync/`

| Event | Notes |
|---|---|
| `MovementBroadcastEvent`, `PlayerMovementBroadcastEvent` | Movement packets |
| `VehicleMountEvent`, `VehicleSpeedCalculationEvent` | Mount / wagon / cart logic |
| `MovementIntentReceivedEvent`, `PredictionStateReceivedEvent` | Server-side hooks for client prediction |

### Other

| Path | Events |
|---|---|
| `events/farming/` | `CropGrowthEvent`, `CropHarvestEvent` |
| `events/structure/` | `StructureDbLoadEvent`, `StructureDbSaveEvent` |
| `events/deity/` | `DeityDbLoadEvent`, `DeityDbSaveEvent` |
| `events/spell/` | `SpellFavorCostEvent` |
| `events/` (top-level) | `ModActionEvent`, `ModQueryEvent` — generic mod-to-mod dispatch |

If you need to find an event by what it *does* rather than where it lives,
grep the api module:

```bash
grep -rln "extends Event" wurmmodloader-api/src
grep -rln "@SubscribeEvent" wurmmodloader-core/src
grep -rln "CreatureDeath" wurmmodloader-api/src
```

---

## Real-world examples

The simplest possible handler:

- **[`examples/hellomod/`](../../examples/hellomod/)** — one `@SubscribeEvent`
  on `ServerStartedEvent`.

Multiple events working together:

- **[`examples/oversizedclub/`](../../examples/oversizedclub/)** —
  subscribes to `ItemTemplatesCreatedEvent`, `ServerStartedEvent`,
  `CapabilityRegistrationEvent`, `ItemExamineEvent`,
  `WeaponStatQueryEvent`, `CombatCriticalHitEvent`,
  `OpportunityAttackEvent`. Read the file top-to-bottom — every handler is
  documented inline.

UI-driven:

- **`examples/templatemod/`** — `ServerStartedEvent` + the UI API to
  register a context-menu entry that opens a multi-page questionnaire.

---

## Common pitfalls

- **Wrong import path.** Events live under
  `com.garward.wurmmodloader.api.events.<area>.<EventName>`. The old path
  `com.garward.wurmmodloader.api.event.*` (singular `event`) is gone.
- **`org.gotti.*` mod interface.** That's the legacy bridge. New mods
  implement `com.garward.wurmmodloader.modloader.interfaces.WurmServerMod`.
- **Throwing from a handler.** Exceptions are logged and other handlers
  continue, but the user-visible game action may proceed in an inconsistent
  state. Wrap risky work in try/catch and log; never let an
  `ItemTemplatesCreatedEvent` handler crash mid-registration.
- **`ServerPollEvent` doing real work.** It fires every tick. Throttle with
  a counter, or keep a `nextRunAt` timestamp.
- **Handler not firing on cancelled events.** That's the default — opt in
  with `receiveCancelled = true` if you actually want the audit.
- **Cancelling a non-cancellable event.** Throws — check the event's
  Javadoc / source first. The `Event` superclass exposes `isCancellable()`.

---

## See also

- **[`extending-framework.md`](extending-framework.md)** — when no existing
  event covers what you need: the five-file recipe to add a new bytecode
  patch + event class
- **[`bml-ui.md`](bml-ui.md)** / **[`ui-api.md`](ui-api.md)** — what to
  *do* in your handler if the answer is "show the player a window"
- **[`legacy-mod-compatibility.md`](legacy-mod-compatibility.md)** — how
  Ago-era listener interfaces (`PlayerLoginListener`, etc.) get bridged to
  the modern events
- **Source:** [`wurmmodloader-api/.../api/events/base/`](../../wurmmodloader-api/src/main/java/com/garward/wurmmodloader/api/events/base/)
  — `Event`, `SubscribeEvent`, `EventPriority`, `CancellableEvent`
