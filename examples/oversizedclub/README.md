# Oversized Club — Reference Tutorial Mod

The canonical example for WurmModLoader. Demonstrates every moving part a
typical gameplay mod touches: custom item template, `Weapon` combat
registration, crafting recipe, a persistent per-item capability (`ItemLevel`),
and event subscriptions for examine / stat queries / crits / opportunity
attacks.

If you're writing your first mod, this is the file to read.
See also [`WEAPON_CREATION_PITFALLS.md`](WEAPON_CREATION_PITFALLS.md) for the
short list of gotchas this mod was built to demonstrate.

## Weapon

| Stat | Value | Notes |
| --- | --- | --- |
| Damage | 19.0 | intentionally high so the level-scaling hooks are visible |
| Swing speed | 8.0 s | very slow; DPS ≈ 2.38, close to a huge axe's 2.41 |
| Reach | 2 | short — you close the gap |
| Weight group | 5 | heavy |
| Primary skill | `CLUB_HUGE` | shares progression with the huge club |
| Damage type | Crush | `ITEM_TYPE_WEAPON_CRUSH` |
| Material | Birch wood | `MATERIAL_WOOD_BIRCH` |

## Crafting

Log + carving knife, Carpentry (40 for 100% success).

## What the mod demonstrates

| Event | Purpose |
| --- | --- |
| `ItemTemplatesCreatedEvent` | register the template + Weapon + recipe |
| `ServerStartedEvent` | log a short admin-visible ready message |
| `CapabilityRegistrationEvent` | register `ItemLevelCapability` |
| `ItemExamineEvent` | show level / damage bonus on examine |
| `WeaponStatQueryEvent` | per-item tweaks to damage / speed / parry |
| `CombatCriticalHitEvent` | scale crit chance with level |
| `OpportunityAttackEvent` | block novice whiffs / speed up veteran counter-swings |

## Capability — `ItemLevel`

`ItemLevelCapability` is registered against `Item`, so every item in the game
can lazily gain an `ItemLevel` the first time something asks for it. The
framework handles persistence; the mod code just does
`provider.getCapability(ItemLevelCapability.INSTANCE)` and reads / writes
fields on the returned object.

In this mod the capability only affects oversized clubs (we early-return on
template id mismatch in every hook), but the capability itself is global — any
other mod that wants to add "items can have levels" can use the same
registration.

## Installing (for testing)

Drop-in layout after build:

```
mods/oversizedclub/oversizedclub.jar
mods/oversizedclub.properties
```

## Adapting this as a starting template

Most common modifications, and where to change them in
`OversizedClubMod.java`:

- **Damage type** — swap the `ITEM_TYPE_WEAPON_*` entry in `.itemTypes(...)`.
- **Material** — change `.material(...)` *and* the matching `ITEM_TYPE_*` in
  `.itemTypes(...)` together. Mismatches cause subtle breakage
  (see pitfall #2).
- **Recipe** — change the `CreationEntryCreator.createSimpleEntry(...)` call.
  Call it multiple times to allow multiple source materials.
- **Combat numbers** — change the `new Weapon(...)` constructor arguments.
  Remember `critParam` is divided by 5.0 internally (pitfall #4).
- **Namespace** — change `"garward.oversizedclub"` to your own unique
  namespace. Treat this as permanent once a server has run with it.
