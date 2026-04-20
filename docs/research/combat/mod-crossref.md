# Combat Mod Cross-Reference — Armoury & DUSKombat

How two existing combat mods hook into (or around) Wurm. Feeds the [event-surface-gap](event-surface-gap.md) roadmap.

Sources:
- `~/Scripts/Games/WurmUnlimited/WurmModLoader-CommunityMods/mods/armoury/`
- `~/Scripts/Games/WurmUnlimited/WurmModLoader-CommunityMods/mods/DUSKombat/`

---

## Armoury — the "query events + multiplier tables" pattern

### Hook strategy
- **Zero bytecode patches.** `CombatsTweaks.java:15` explicitly: "no custom bytecode is required."
- Everything routes through framework events that Armoury subscribes to.

### Events it listens to
- `ShieldCheckEvent`, `ShieldDamageEvent` — shield block & shield durability
- `WeaponStatQueryEvent` — weapon stat lookups
- `MaterialBonusEvent`, `MaterialDamageModifierEvent` — material-driven multipliers

### Events it fires (downstream API)
Uses `ModQueryEvent` as a generic request/response channel. Other mods fire these with a key and read the response map:

| Query key | Returns |
|---|---|
| `armoury:armor_damage_reduction` | `baseDR`, `effectiveness`, `finalDR` |
| `armoury:armor_glance_rate` | glance percentage |
| `armoury:material_weapon_bonus` | `damageMultiplier`, `speedMultiplier`, `parryBonus`, `armorDamage` |
| `armoury:material_tool_bonus` | action / durability / difficulty modifiers |
| `armoury:armor_set_check` | placeholder (not implemented) |

### Intermediate damage values accessed
**None.** Armoury never sees weapon base damage, skill mods, strength mod, parry state, or crit flag as separate numbers. It only registers multipliers that somebody else applies at wound-application time.

### Does it reimplement the formula?
No. It's pure configuration + query-response.

---

## DUSKombat — the "full formula replacement" pattern

### Hook strategy
- **Zero bytecode patches.** `DUSKombatMod.java:136`: "no bytecode hooks needed!"
- Cancels vanilla `CombatAttackEvent` and runs its own attack sequence end-to-end.

### Where it intercepts
- **`CombatHandler#attack(Creature, int, boolean, float, Action)`** — via framework `CombatAttackEvent`. Handler (`DUSKombatMod.java:157-173`) cancels the event and calls `DUSKombat.attackHandled()` directly.
  - This is *earlier* than `CombatEngine.addWound()` (~line 918 vanilla). DUSKombat never reaches `addWound()` on vanilla's code path.
- **Provides its own `DamageEngine.addWound(...)`** (`DamageEngine.java:44`) with an expanded signature (adds archery, alreadyCalculatedResist, noMinimumDamage, spell, critical, glance flags).
- **Calls `ProxyServerHook.fireCombatDamage()` via reflection** (`DamageEngine.java:87-89`) — so the framework's damage event *does* still fire for DUSKombat hits, just through a non-standard path.

### Full reimplementation footprint
`DUSKombat.java` attack sequence (lines 275+):
1. `getHitCheck()` — weapon skill + fighting skill check with style bonuses
2. `getDodgeCheck()` — body control + mind speed + movement + style mods
3. `getCriticalChance()` — weapon base crit + style bonus
4. Crit → 1.5× damage multiplier
5. `getShieldCheck()` — shield skill with stamina penalties
6. `getParryCheck()` — weapon skill with style/stamina penalties
7. `dealDamage()` — style mods, venom, sparring checks
8. `DamageEngine.addWound()` (DUSKombat's own) — wound application

### Intermediate damage values DUSKombat has
`DamageMethods.java:99-286` — every stage is a named variable:
- Weapon base (`Weapon.getModifiedDamageForWeapon(weapon, strengthSkill, fullDamage) * 1000`)
- Quality effect bonus (`Server.getBuffedQualityEffect(...) * base * 2400`)
- Material damage bonus (`Weapon.getMaterialDamageBonus(...)`)
- Hunter damage bonus (conditional)
- Item bonus (`ItemBonus.getWeaponDamageIncreaseBonus(...)`)
- Bloodthirst modifier (additive vs multiplicative, configurable)
- Rotting touch (spell bonus)
- Damage multiplier (`getDamageMultiplier`, lines 131-230): enemy-presence +15%, polearm-vs-mounted +70%, war-god +15%, strength scaling, fighting-style skill checks, village-war +0-30%, fight-level focus +10% per level 4+

`CombatMethods.java:22-262` — checks it computes: hit, dodge, crit, parry, shield.

### Events it exposes
- `ModActionEvent("duskombat:calculate_damage")` — fired inside `DamageMethods.getDamage()` at line 260. Payload: `{attackerId, defenderId, baseDamage, damageType, weaponId, isBackstab}`. Subscribers can modify `damageMultiplier` (default 1.0) and `bonusDamage` (default 0). Damage is then finalized as `damage * damageMultiplier + bonusDamage`.
- `SpecialMoveSendEvent`, `SpecialMoveHandleEvent` — special-move UI
- `ItemEnchantmentStringsEvent` — surfaces combat info on item examine

### Does it reimplement the formula?
**Entirely.** DUSKombat's attack sequence never calls `CombatEngine.attack()` and never lets wounds flow through vanilla `CombatEngine.addWound()`. It is a replacement, not a modification.

---

## Side-by-side

| Aspect | Armoury | DUSKombat |
|---|---|---|
| Bytecode patches | None | None |
| Hook point | Framework query events | `CombatAttackEvent` (cancels vanilla) |
| Intermediate damage visibility | None (multipliers only) | Full (reimplemented formula) |
| Reaches vanilla `addWound()`? | Via the rest of the game's code path | No — uses own `DamageEngine.addWound()` |
| Fires framework `fireCombatDamage`? | Indirectly | Yes, via reflection from `DamageEngine` |
| Events exposed downstream | `ModQueryEvent` query keys | `ModActionEvent("duskombat:calculate_damage")` |
| Formula reimplemented? | No | Completely |

---

## Why this matters for the framework

Both mods work *around* the framework's event surface, not *through* it:
- **Armoury** couldn't subscribe to "what's the DR for this armour piece?" so it invented `ModQueryEvent` keys to serve that query itself.
- **DUSKombat** couldn't hook individual stages of the attack formula — there's no `WeaponBaseDamageEvent`, `SkillRollEvent`, `ParryCheckEvent`, `ShieldBlockEvent`, etc. — so it rewrote the whole attack path and exposed its *own* generic mid-damage hook (`duskombat:calculate_damage`) for other mods to modify.

Each of those workarounds is a missing framework event. The roadmap lives in [`event-surface-gap.md`](event-surface-gap.md).
