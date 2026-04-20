# Pet Class — Architecture & Implementation Plan

**Goal:** A "pet class" archetype for power-fantasy RPG mods where skill-tree choices trade player stats for pet capabilities. Concrete examples:

- "Reduce player damage by 50% → gain 4 pet slots"
- "Give up 30% move speed → pets gain +40% HP"
- Unlock pet abilities (roar/charge/heal/etc.) as skill-tree leaves

This doc is the persistent TODO for the pet-class roadmap. It belongs alongside `hook-surface.md` — that doc established *where* hooks are needed; this one establishes *what we build on top*.

---

## Vanilla reality check

The vanilla pet system is a **single-slot** primitive:

- `Creature.getPet() / setPet(long)` — one `long` field, one pet at a time.
- `PlayerInfo.setPet(long)` — DB-backed, persisted.
- `CharmAnimal` (spells/CharmAnimal.java:43–50) and `Dominate` (spells/Dominate.java:62–70) both kick the previous pet when a new one is assigned: `oldPet.setDominator(-10L)`, `performer.setPet(-10L)`, then reassign.
- Loyalty is the decay mechanism. `performer.getPet().getLeader()` is the follow-link.

**Do not replace the vanilla slot.** Follow/command/loyalty-poll/combat targeting all read it. Layer on top.

---

## Architecture: single slot + capability list

```
PlayerPetsCapability
├── List<Long>  wurmIds        ← full pack roster (persistent)
├── int         maxSlots        ← skill tree writes this (default 1)
└── long        activeLeader    ← currently the vanilla pet (redundant, for sanity)

Vanilla Creature.pet slot       ← "who's leading right now"
                                 (swapped via /selectpet or body-menu action)
```

**Invariants:**
- `maxSlots == 1` → identical to vanilla behaviour.
- A new tame is appended to `wurmIds` instead of kicking the current pet, iff `wurmIds.size() < maxSlots`.
- The vanilla pet slot always holds one of the creatures in `wurmIds` (or -10L if none).
- On player login: reconcile — drop any wurmId that no longer exists, re-attach still-dominated survivors.

**Keyed by** player wurmId, persisted via `CapabilityManager` (same mechanism as `CreaturePowerCapability`).

---

## Skill tree / cost-benefit mechanics

This is the "pet class" design space. The framework only needs to expose the knobs; the skill-tree mod decides how to spend them.

| Knob | Where it lives | How skill tree mutates it |
|---|---|---|
| `maxSlots` | `PlayerPetsCapability.maxSlots` | Direct write on node unlock |
| Pet damage mult | `PetBuffsCapability` on each pet | Listener on `CombatDamageEvent` multiplies when `attacker ∈ PlayerPetsCapability.wurmIds` |
| Pet HP mult | `PetBuffsCapability` | Applied via `CombatDamageEvent` when the pet is defender (inverse multiplier on incoming damage) |
| Pet move speed | `CreaturePollEvent` (not yet built) | Set a movement modifier each tick |
| Player damage penalty | Existing `CombatDamageEvent` | Listener halves when attacker is the skill-tree owner |
| Tamability gate | `TameAttemptEvent` handler | Veto by tier / creature type unless skill is learned |

**Key design rule:** trade-offs are enforced *in the skill-tree mod's event handlers*. The framework just carries the data.

### Example build: "Beastmaster"
- Tier 1 node (free): `maxSlots = 2`
- Tier 2 (cost: -15% player weapon damage): `maxSlots = 3`, unlock "Tame Alpha" permission
- Tier 3 (cost: -30% player weapon damage, -20% stamina regen): `maxSlots = 5`, pets gain +25% HP
- Capstone (cost: -50% player weapon damage): `maxSlots = 7`, unlock pet-ability: Roar (AoE fear)

Each tier is just (a) a write to `maxSlots`, (b) a set of flags in a mod-owned player capability the skill-tree mod already manages, (c) listeners that read those flags inside `CombatDamageEvent` / `TameAttemptEvent`.

---

## Pet abilities — three viable paths

Currently **no clean hook** for pet-only buffs. Options, ranked by ambition:

1. **`PetBuffsCapability` + read in `CombatDamageEvent`** (ships today, no new framework work needed).
   List of `{abilityId, expiry, magnitude}`. Mod reads it in its damage handler and applies.
2. **New `SpellEffect` IDs** — piggyback on vanilla's enchant-effect framework. Cleanest for buffs that should appear in the vanilla examine/UI. Needs small bytecode to register custom effect IDs.
3. **`CreaturePollEvent` + `CreatureAbilityUseEvent`** — future framework events. Required for things like "pet charges on cooldown", "pet casts heal every 20s". Add when we start shipping proactive pet abilities rather than passive stat buffs.

---

## Required framework changes

### Missing events (the three this plan lands first)

| Event | Fire site | Cancellable | Payload |
|---|---|---|---|
| `TameAttemptEvent` | `CharmAnimal.precondition`, `Dominate.mayDominate`, future whip-tame | **yes** | `performer, target, Source{CHARM,DOMINATE,TAME}` |
| `TameCompleteEvent` | `CharmAnimal.doEffect` (post-`setPet`), `Dominate.dominate` (post-`setPet`) | no | `performer, target, Source` |
| `PetReleasedEvent` | `Creature.die()` when dominator != -10L, and any explicit release path | no | `pet, formerOwner, Reason{DIED,UNTAMED,MANUAL}` |

**Bytecode patch surface:** 3 files — `spells/CharmAnimal`, `spells/Dominate`, `creatures/Creature` (die method already patched; add a second insertBefore that checks dominator).

### Later (in order)

1. **Active-pet-swap action** — new `ModAction` (`/selectpet <id>` or body-menu entry). Sets vanilla pet slot from capability list. Validates: target wurmId is in `wurmIds`, still alive, still loyal.
2. **Reconciliation on login** — `PlayerLoginEvent` handler drops dead/gone pets, reattaches survivors.
3. **`CreaturePollEvent`** — framework event, fires per-creature each AI tick. Enables speed/aggro buffs and proactive abilities.
4. **`CreatureAbilityUseEvent`** — abstraction for mod-defined pet abilities with cooldowns.

---

## Build order

1. **Land the 3 taming events** — this PR. Enables everything downstream.
2. `PlayerPetsCapability` (mod side, uses existing `CapabilityManager`).
3. Active-pet-swap action + login reconciliation.
4. Skill-tree nodes that write `maxSlots` and flags.
5. `PetBuffsCapability` routed through existing `CombatDamageEvent`.
6. Tier-gated taming once `CreaturePowerCapability` tiers exist.
7. `CreaturePollEvent` + ability cooldowns (separate effort; see `hook-surface.md` roadmap).

Steps 2–6 are **pure mod-side code**. Framework work stops after step 1 (and step 7 eventually).

---

## Open questions

- **Loyalty across the roster**: does every pet in the list bleed loyalty continuously, or only the active one? Vanilla poll only ticks `getPet()`. Leaning "only active decays" to avoid a stampede of detames when a player logs in after a week.
- **Follow behaviour for inactive pets**: stay put where released, or despawn into a "stabled" state? Despawning breaks vanilla creature persistence assumptions; probably simpler to leave them where they are and let them wander.
- **PvP**: do stabled pets count toward kingdom population? Probably yes (they're real `Creature` instances) — will matter for server admins.

These are mod-design problems, not framework problems — resolve when we build step 2.
