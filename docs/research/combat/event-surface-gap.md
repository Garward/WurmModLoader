# Combat Event Surface — Gap Analysis & Roadmap

**Strategic principle:** A mod writing a bytecode patch or reimplementing a Wurm formula is a symptom of a missing framework event. The framework's job is to make the vanilla source invisible. No mod author should ever need to open `CombatEngine.java` again.

This document enumerates, concretely, what Armoury and DUSKombat had to build for themselves that should exist as first-class framework events. See [`mod-crossref.md`](mod-crossref.md) for how they currently do each one.

---

## What a mod author should be able to write

Target experience — a DUSKombat-equivalent written purely against the event API:

```java
@SubscribeEvent(priority = HIGH)
public void onWeaponBaseDamage(WeaponBaseDamageEvent e) {
    if (e.getWeapon().getMaterial() == MaterialIds.STEEL) {
        e.modifyBaseDamage(1.15);
    }
}

@SubscribeEvent
public void onParryCheck(ParryCheckEvent e) {
    if (e.getAttacker().hasBuff("daze")) {
        e.setParryChance(e.getParryChance() * 0.5f);
    }
}

@SubscribeEvent
public void onDamageFinalized(CombatDamageBreakdownEvent e) {
    // Full breakdown: weapon base, str mod, skill, parry, armour, final
    logger.info(e.describe());
}
```

Zero bytecode. Zero vanilla-source-reading. Zero reimplementation.

---

## Event surface the framework currently exposes

From `wurmmodloader-api/.../api/events/` (run `codeindex pattern event_handlers` for current list):

- `CombatAttackEvent` — attack sequence start. **Cancellable, mods can replace entirely.** (This is DUSKombat's only entry point.)
- `CombatDamageEvent` — fired from `ProxyServerHook.fireCombatDamage()` after damage is finalized. Single `double damage` value.
- `ShieldCheckEvent`, `ShieldDamageEvent` — shield block + shield wear
- `WeaponStatQueryEvent` — weapon stat queries
- `MaterialBonusEvent`, `MaterialDamageModifierEvent` — material multipliers
- *(…others — audit needed)*

This is sufficient to **replace** vanilla combat (DUSKombat proves it). It is not sufficient to **modify** individual stages without reimplementing.

---

## Events that need to exist

Grouped by combat stage. For each, the "currently handled by" column shows how Armoury or DUSKombat works around the gap today.

### Attack-sequence stages

| Proposed event | Fires when | Payload | Modifiable | Currently handled by |
|---|---|---|---|---|
| `WeaponBaseDamageEvent` | Weapon damage roll resolves | `weapon`, `attacker`, `baseDamage`, `fullDamage` flag | `baseDamage` | DUSKombat reimplements `Weapon.getModifiedDamageForWeapon` call chain |
| `StrengthScalingEvent` | Strength → damage multiplier applied | `attacker`, `strengthSkill`, `multiplier` | `multiplier` | DUSKombat computes inline |
| `SkillRollEvent` | Weapon skill check for hit | `attacker`, `skill`, `rollBefore`, `rollAfter`, `bonuses` | `rollAfter` | DUSKombat `getHitCheck()` |
| `DodgeCheckEvent` | Defender dodge roll | `defender`, `bodyControl`, `mindSpeed`, `movement`, `finalDodgeChance` | `finalDodgeChance` | DUSKombat `getDodgeCheck()` |
| `CriticalCheckEvent` | Crit roll | `attacker`, `weapon`, `baseCritChance`, `styleBonus`, `result` | `result` | DUSKombat `getCriticalChance()` |
| `ParryCheckEvent` | Parry roll | `attacker`, `defender`, `weaponSkill`, `staminaPenalty`, `result` | `result` | DUSKombat `getParryCheck()` |
| `ShieldBlockCheckEvent` | Shield block roll (separate from `ShieldCheckEvent` if that one is stat-only) | `defender`, `shieldSkill`, `staminaPenalty`, `result` | `result` | DUSKombat `getShieldCheck()` |
| `FightingStyleModifierEvent` | Style modifiers (def/norm/agg) applied | `attacker`, `style`, `damageMod`, `defenseMod`, `speedMod` | all three | DUSKombat inline in `getDamageMultiplier` |
| `DamageMultiplierEvent` | All situational multipliers applied | `attacker`, `defender`, breakdown map (enemy-presence, polearm-vs-mounted, war-god, village-war, fight-level-focus) | each entry | DUSKombat `getDamageMultiplier(131-230)` |
| `WoundApplicationEvent` | Inside `addWound()` before wound object created | `attacker`, `defender`, `rawDamage`, `armourMod`, `effectiveDamage`, `woundType`, `bodyPart` | `rawDamage`, `armourMod` | *missing* |
| `CombatDamageBreakdownEvent` | After full formula, before damage applied | Full named breakdown from all stages above | read-only | *missing* (DUSKombat's `duskombat:calculate_damage` approximates) |

### Armour & material queries (replace Armoury's ModQueryEvent keys)

| Proposed event | Purpose | Currently handled by |
|---|---|---|
| `ArmourDamageReductionEvent` | "What's the DR for this armour at this quality, against this wound type?" | Armoury `ModQueryEvent("armoury:armor_damage_reduction")` |
| `ArmourGlanceRateEvent` | Glance chance | Armoury `ModQueryEvent("armoury:armor_glance_rate")` |
| `MaterialWeaponBonusEvent` | Material → damage/speed/parry/armorDamage | Armoury `ModQueryEvent("armoury:material_weapon_bonus")` |
| `MaterialToolBonusEvent` | Material → action/durability/difficulty | Armoury `ModQueryEvent("armoury:material_tool_bonus")` |
| `ArmourSetBonusEvent` | Set-bonus eligibility & payload | Armoury placeholder, not implemented |

### Shield system

`ShieldCheckEvent` + `ShieldDamageEvent` exist. Audit whether they expose:
- Shield skill
- Stamina penalty
- Block-vs-parry-vs-dodge disambiguation
- Durability loss
…and if not, extend them.

### Special moves / combat styles

DUSKombat handles `SpecialMoveSendEvent` and `SpecialMoveHandleEvent`. Confirm these are general enough that a future combat mod can add its own special moves without subclassing DUSKombat's.

---

## Bytecode patches to add (where events can't exist without them)

Current state: [`wurmmodloader-core/.../core/bytecode/patches/`](../../../wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/bytecode/patches) has 57 patches. To cover the events above, the framework needs patches at:

- `com.wurmonline.server.combat.CombatEngine.attack(...)` — entry already covered by `CombatAttackEvent`
- `com.wurmonline.server.combat.CombatEngine.addWound(...)` — `WoundApplicationEvent` needs to inject here
- Weapon damage roll (probably `Weapon.getModifiedDamageForWeapon` or similar) — `WeaponBaseDamageEvent`
- Each skill check site in `CombatEngine` for hit/dodge/crit/parry/shield — the *CheckEvents
- Armour application site in `addWound` — `ArmourDamageReductionEvent`

**Important:** these patches should fire events that return modified values and let the formula continue, **not** wholesale replace the formula. DUSKombat's replacement pattern should become unnecessary, not canonized.

Companion patch target: **if `DamageEngine.addWound()` style wrappers exist in the wild** (DUSKombat's does), decide whether the framework should:
- (a) require all custom damage handlers to call `ProxyServerHook.fireCombatDamage()` (DUSKombat already does this)
- (b) add a patch that targets any `addWound`-shaped method via signature matching
- (c) ignore — accept that replacement mods bypass wound-level events

Recommend (a) — document the contract — because once the per-stage events above exist, replacement mods have no reason to exist.

---

## Migration path

The goal is for DUSKombat to be rewritable as ~200 lines of event handlers instead of a 1,100-line formula replacement. That's the acceptance test.

Suggested order (each item makes the next one testable):

1. **Audit current events** — generate an up-to-date list of combat-related events with `codeindex pattern event_handlers` and `wurmquery search addWound`. Any event already on the proposed list can be skipped.
2. **`WoundApplicationEvent`** — small, self-contained, covers vanilla and Armoury path. (Tier 1 of old plan — still a quick win.)
3. **`CombatDamageBreakdownEvent`** — framework fires it with whatever values it has post-formula. Mods that already have richer data (DUSKombat) can fire it themselves with the full breakdown.
4. **`ParryCheckEvent` + `ShieldBlockCheckEvent` + `DodgeCheckEvent`** — the three cheapest check events. Prove the pattern.
5. **`CriticalCheckEvent`, `SkillRollEvent`** — broader reach, probably requires patching into each skill-check call site.
6. **`WeaponBaseDamageEvent`, `StrengthScalingEvent`, `DamageMultiplierEvent`** — the heart of the damage formula. When these work, DUSKombat can be retired as "how combat mods get written."
7. **Armoury query events → proper events** — convert `ModQueryEvent` keys into typed events. Keep the old keys as a legacy alias for one release.
8. **Port one of the bundled mods** (powerscaling? materialsystem?) to prove the new API handles real gameplay logic, not just theory.

---

## Acceptance criteria

The framework's combat event surface is "done" when:

- [ ] DUSKombat can be rewritten using only `@SubscribeEvent` handlers — no `CombatAttackEvent` cancel + replace, no `DamageEngine` wrapper
- [ ] Armoury can be rewritten using typed events — no `ModQueryEvent` query keys
- [ ] No mod in the bundled set (`mods/`) reads a `com.wurmonline.*` class directly
- [ ] A new combat mod author can write their mod without opening any file under `wurm_server_index.json` (sanity check: `wurmquery` usage by the author approaches zero)
