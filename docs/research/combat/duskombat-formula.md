# DUSKombat — End-to-End Damage Formula

Engineering spec for DUSKombat's combat formula. **Goal:** given attacker stats + equipped gear + target stats + equipped gear + environment, reproduce DUSKombat's exact damage number from first principles.

This is dense. Every stage cites source lines. Every branch is named. If a value is clamped, the clamp is reported.

Source tree: `~/Scripts/Games/WurmUnlimited/WurmModLoader-CommunityMods/mods/DUSKombat/`
Key files:
- `src/main/java/mod/piddagoras/duskombat/DUSKombatMod.java` — framework hook
- `src/main/java/mod/piddagoras/duskombat/DUSKombat.java` — attack sequence
- `src/main/java/mod/piddagoras/duskombat/DamageMethods.java` — damage formula
- `src/main/java/mod/piddagoras/duskombat/CombatMethods.java` — hit/dodge/crit/parry/shield
- `src/main/java/mod/piddagoras/duskombat/DamageEngine.java` — wound application

Cross-ref: [`mod-crossref.md`](mod-crossref.md), [`event-surface-gap.md`](event-surface-gap.md).

---

## 0. Entry point

DUSKombat cancels the framework's `CombatAttackEvent` (`DUSKombatMod.java:157–173`) and takes over:

```
DUSKombatMod.onCombatAttack(CombatAttackEvent)
  → event.setCancelled(true)
  → DUSKombat.attackHandled(attacker, defender, combatCounter, opportunity, actionCounter, action)  [DUSKombat.java:108–118]
      → DUSKombat instance per attacker
      → attackLoop()  [DUSKombat.java:275–450]
```

**This affects creature combat too** — creatures and players both route through `CombatHandler.attack()` which fires `CombatAttackEvent`. See [`../creatures/hook-surface.md`](../creatures/hook-surface.md) §3.

Entry signature: `public static boolean attackHandled(Creature, Creature, int, boolean, float, Action)` → returns `dead`.

---

## 1. Attack sequence skeleton

Ordered stages inside `attackLoop()`:

1. **Prerequisites** — `CombatHandler.prerequisitesFail()` (`DUSKombat.java:293`). Kills loop if false.
2. **Opportunity attack** (`:337–362`) — if `opportunity==true`, attacker uses secondary weapon if available, calls `swingWeapon()` once, exits.
3. **Secondary weapon loop** (`:366–407`) — per secondary weapon: check timer `getSpeed(attacker, sec) * timeMod`, call `swingWeapon()`.
4. **Primary swing** (`:409–424`) — check timer `getSpeed(attacker, weapon)`, call `swingWeapon()` if threshold exceeded.
5. **Stance / special-move checks** (`:425–447`) — if no attack was performed.

Early exits during a swing: **miss** (`:657–674`), **dodge** (`:652–655`), **parry** (`:642–650`), **shield block** (`:579–602`), **defender death**. Critical hit (`:560–571`) does not exit; it amplifies damage.

---

## 2. Hit check — `CombatMethods.getHitCheck` (`:40–127`)

Attacker's weapon-skill `skillCheck()` with a cumulative additive bonus.

**Bonus components (all additive):**

| Source | Formula | Range |
|---|---|---|
| Fighting skill vs. BC + height diff (`:43`) | `fightingSkill.skillCheck(...)` | [-100, 100] |
| Height diff (`:45–46`) | `clamp(heightDiff * 5, -10, 10)` | [-10, 10] |
| Fight level (`:48–49`) | `min(5, fightLevel) * 3` | [0, 15] |
| Angle (attack-from-behind) (`:51–55`) | `max(0, 25 * (1 - sqrt(|attAngle-180|/60)))` | [0, 25] |
| Piloting vehicle (`:57–60`) | `-20` | {0, -20} |
| Mounted (`:62–68`) | `+20`, `+30` if dominator | {0, +20, +30} |
| CR counter potion (`:71–73`) | `CRCounterBonus * 10` | varies |
| **Deity** (faith > 70, `:81–111`) | Fo on natural terrain: `-2.5`; Mag/Libila always: `-5`; Vynora on sea/road: `-2.5` | {-5, -2.5, 0} |
| True Hit enchant (`:115`) | `+ enchantEffect * 0.05` | varies |
| Nimbleness on weapon (`:116`) | `+ weapon.enchantEffect * 0.07` | varies |
| Weapon skill penalty (`:122–124`) | `- getSkillPenaltyForWeapon(weapon) * 10` | ≤ 0 |

**Base difficulty** (`:75–78`): `5`, plus `opponent.getBodyControl() * 0.1` if `opponent.getBaseCombatRating() > 50`.

**Final:** `primWeaponSkill.skillCheck(check, weapon, bonus, noSkillGain, 10.0f)` → `hitCheck`.
**Hit succeeds when `hitCheck >= 0`.** The value of `hitCheck` (call it `attackCheck`) becomes an input to every subsequent defensive check.

---

## 3. Dodge check — `CombatMethods.getDodgeCheck` (`:128–179`)

Defender's fighting-skill check.

| Bonus source | Formula |
|---|---|
| Body control (`:136`) | `BC.skillCheck(attackCheck*0.5, 0, true, 10) * 0.5` (±50) |
| Mind speed (`:138`) | `MS.skillCheck(attackCheck*0.5, 0, true, 10) * 0.5` (±50) |
| Movement scheme (`:140`) | `moveMod * 100` (~±30) |
| Willowspine (`:143–144`) | `attackCheck *= 1 - willowEffect * 0.002` |
| Excellence (`:146–147`) | `attackCheck -= excelEffect * 0.05` |
| Attacker style (`:151–155`) | aggressive `+10`, defensive `-5` |
| Defender style (`:158–162`) | defensive `-10`, aggressive `+5` |

**Adjustments:** `attackCheck *= 1.5` if attacker is a player (`:166`, harder to dodge players). Creatures with `CR < 50` lose up to 50 based on stamina: `0.5 * (100 - stamPct)` (`:171–176`). Creatures with `CR >= 50` gain `(CR / battleRatingModifier) * 0.1`.

**Final:** `fightingSkill.skillCheck(attackCheck, bonus, true, 10)` → `dodgeCheck`.
**Dodge succeeds when `dodgeCheck >= 0`** (attack ends). Fails → attack continues.

---

## 4. Critical check — `CombatMethods.getCriticalChance` (`:180–194`)

**Base:** `Weapon.getCritChanceForWeapon(weapon)` (vanilla; e.g. longsword ≈ 0.002).

**Modifiers:**
- `+0.03` if `CombatEngine.getEnchantBonus(weapon, opponent) > 0` (`:185–187`)
- Aggressive style: `*= 1.2` (`:190–191`)

**Roll:** crit succeeds if `Server.rand.nextDouble() <= critChance` **OR** attacker has True Strike (`DUSKombat.java:559–564`).

**Damage effect (applied later, in `swingWeapon`, `:565–569`):** `damage *= 1.4` if defender defensive style, else `damage *= 1.5`.

---

## 5. Shield block check — `CombatMethods.getShieldCheck` (`:234–261`)

Only tested if defender has a shield **and** crit did not fire (`DUSKombat.java:573`).

| Bonus | Source |
|---|---|
| Fighting skill seed (`:244`) | `fightingSkill.skillCheck(attackCheck*0.4, weapon, 0, true, 10)` |
| Attacker aggressive (`:246–247`) | `-5` |
| Defender defensive (`:251–252`) | `+10` |
| Excellence (`:217–218`) | `+ excelEffect * 0.05` |
| Shield Nimbleness (`:220–221`) | `+ shield.nimbEffect * 0.05` |
| Stamina penalty (`:257–258`) | `- 0.7 * (100 - stamPct)` (≤ 0, floor -70) |

**Base check:** `attackCheck * 0.25` (`:255`). **Block resistance** reduction (`DUSKombat.java:576–578`): `- 80 * (1 - blockRes)`. Block-resistance recovery: `0.2/sec` (`:147`).

**Final:** `shieldSkill.skillCheck(attackCheck*0.25, shield, bonus, true, 10)` → `shieldCheck`.
**Block succeeds when `shieldCheck >= 0`.** (Attack ends; shield takes damage.)

---

## 6. Parry check — `CombatMethods.getParryCheck` (`:195–233`)

Only tested if shield block failed and defender holds a parry-capable weapon.

| Bonus | Source |
|---|---|
| Fighting skill seed (`:204`) | `fightingSkill.skillCheck(attackCheck*0.5, weapon, 0, true, 10)` |
| Attacker aggressive (`:206–207`) | `-5` |
| Defender defensive (`:211–212`) | `+10` |
| Excellence (`:217–218`) | `+ excelEffect * 0.05` |
| Weapon Nimbleness (`:220–221`) | `+ defWeapon.nimbEffect * 0.05` |
| Parry % (`:225–227`) | `- 30 * (1 - getWeaponParryPercent(defWeapon))` |
| Stamina (`:229–230`) | `- 0.2 * (100 - stamPct)` (floor -20) |

**Parry-resistance reduction** (`DUSKombat.java:607–609`): `- 50 * (1 - parryRes)`. Recovery: `0.04/sec` (swords `0.5×`, `:146, 165–167`).

**Final:** `weaponSkill.skillCheck(attackCheck*1.1, defWeapon, bonus, true, 10)` → `parryCheck`. Secondary weapons test at `attackCheck*2` (`:617`).
**Parry succeeds when `parryCheck >= 0`.**

---

## 7. Damage formula — `DamageMethods.getDamage` (`:232–286`)

### 7a. Base weapon damage

**Unarmed path** — `getBaseUnarmedDamage` (`:74–97`):
- `creature.getCombatDamage(weapon) * 1000 * damageTypeModifier`
- Players: `*= 1.0 + 2.0 * weaponlessFighting.getKnowledge() / 100.0` (1×→3×)
- Bearpaw if dmg < 10000: `+= getBuffedQualityEffect(bearpaw/100.0) * 5000`
- Randomizer: `*= (50 + rand(0..50)) / 100` (0.5×–1.0×)

**Weapon path** — `getBaseWeaponDamage` (`:99–129`):
```
base  = Weapon.getModifiedDamageForWeapon(weapon, bodyStrength, false) * 1000
base += Server.getBuffedQualityEffect(QL/100) * Weapon.getBaseDamageForWeapon(weapon) * 2400   (:111)
base *= Weapon.getMaterialDamageBonus(weapon.getMaterial())                                     (:112)
base *= Weapon.getMaterialHunterDamageBonus(mat)  [if opponent is a non-player hunter target]   (:113–115)
base *= ItemBonus.getWeaponDamageIncreaseBonus(attacker, weapon)                                (:117)

// Bloodthirst — one of:
// useEpicBloodthirst=false (additive):   base += (QL/100) * spellExtraDamage                   (:106–108)
// useEpicBloodthirst=true  (multiplicative): base *= 1 + (QL/100) * spellExtraDamage / 30000   (:119–121)

base += base * rottingTouchPower * 0.002   [Rotting Touch enchant, 0.2% per power]              (:124–127)
```

### 7b. Damage multiplier — `getDamageMultiplier` (`:131–230`)

Start `mult = 1.0`. Applied as product (skip if condition not met):

| Condition | Multiplier | Line |
|---|---|---|
| `getEnemyPresense() > 1200s` AND opponent is player AND weapon not artifact | `*= 1.15` | :134–135 |
| Polearm vs mounted creature/rider | `*= 1.70` | :145–146 |
| Hate-war (Path of Power `doubleWarDamage()`) | `*= 1.50` | :149–151 |
| War-deity AND faith ≥ 40 AND favor ≥ 20 | `*= 1.15` | :152–155 |
| Player, strength scaling active | `*= 1 + (bodyStrength.realKnowledge - 20) / 200` (floor 1.0 at skill 20, 1.4 at skill 100) | :158–162 |
| **Fighting style — defensive (0)** | skillCheck vs `opp.baseCR * 3`: success `*= 0.8`, failure `*= 0.5` | :164–201 |
| **Fighting style — aggressive (1)** with stam > 2000 | on success `*= 1 + Server.getModifiedFloatEffect(fstyle.realKnowledge/100) / 4` | :164–201 |
| **Fighting style — warrior (2+)** | no gain from this stage | :164–201 |
| Weapon skill < 50 | `mult = 0.8 * mult + 0.2 * (skill/50) * mult`  (linear 0.8×→1.0×) | :211–213 |
| NPC attacker | `*= 0.85 + currentStyle * 0.15` (range 0.85–1.0) | :214–215 |
| Village/faith war bonus | `*= 1 + villageFaithWarBonus/100` (up to +30%) | :219–221 |
| Attacker focus ≥ 4 | `*= 1.10` | :224–227 |

Then in `getDamage` (`:244`): `damage *= getDamageMultiplier(...)`.

### 7c. Backstab (`:248–254`)

If attacker in stealth, opponent unaware and visible: `damage = min(50000, damage * 4.0)`.

### 7d. Mod hook — `duskombat:calculate_damage` (`:260–283`)

`ModActionEvent` fired with payload:
```
attackerId, defenderId, baseDamage, damageType, weaponId, isBackstab
```
Subscribers can write:
- `damageMultiplier` (float, default 1.0)
- `bonusDamage` (int, default 0)

Applied: `damage = damage * multiplier + bonus`. **Fires before armour reduction.** This is the hook `powerscaling` uses to stack scaling on top of DUSKombat.

`getDamage` returns this value.

---

## 8. Post-base: critical & style

`swingWeapon` applies crit after `getDamage` returns (`DUSKombat.java:565–569`):
- Defender defensive: `damage *= 1.4`
- Else: `damage *= 1.5`

No additional style damage multiplier here — style impact already lives in `getDamageMultiplier` (§7b).

---

## 9. Armour reduction — `DUSKombat.getArmourMod` (`:732–762`)

`armourMod` is a `float` in [0, 1]. **Lower = more protection.** Wound severity is scaled by this factor (§10).

**If defender wears armour at the hit location:**
- Template DR: `ArmourTemplate.calculateDR(armour, woundType)` (vanilla)
- Creature with natural armour + worn: `armourMod = min(natural, calculateDR(worn))` (worst-of)
- Otherwise: `armourMod *= calculateDR(worn)`

**If no worn armour:**
- **Oakshell** (`:754–759`): `armourMod = 0.2 + (1 - getBuffedQualityEffect(oakshell / divisor)) * scale` → range 0.2–0.8 depending on player/creature.

**Glance** (`:797–810`):
```
chance = 0.05 + armour_glance_mod * qualityEffect
if rand() < chance:
    damage *= (150 - QL) / 150        // higher QL glances less off
    set glance flag (message)
```

---

## 10. Wound application — `DamageEngine.addWound` (`:44–236`)

**Signature:** extended from vanilla — adds `archery, alreadyCalculatedResist, noMinimumDamage, spell, critical, glance` flags.

**Multiplicative resistances applied first** (`:62–136`):

| Resist | Multiplier | Line |
|---|---|---|
| Player → NPC | `* playerToEnvironmentDamageMultiplier` (config, default 1.0) | :62–70 |
| NPC → Player | `* environmentToPlayerDamageMultiplier` (1.0) | :71–80 |
| Player → Player | `* playerToPlayerDamageMultiplier` (0.7) | :81–90 |
| Path of Power elemental immunity | up to ×0, halved in PvP | :111–127 |
| Continuum sorcery | `* 0.8` | :130–131 |
| `Wound.getResistModifier(perf, def, type)` (vanilla) | varies | :135 |

**Wound stacking** (`:160–178`): 80% chance to stack on an existing wound of the same type at the same body part.

**Severity:** `(int)(damage * armourMod)` → `wound.modifySeverity(...)`.

**Side effects:**
- Infection stack (max 99%, `:166–167`)
- Poison stack (max 99%, `:169–170`)
- Loyalty loss for dominated creatures: `- damage * armourMod * CR / 200000` per wound (`:195–200`)

**Returns:** `true` if defender is now dead.

---

## 11. Framework event fired by DUSKombat

Before the wound is written, DUSKombat calls (via reflection, `DamageEngine.java:77–98`):
```
ProxyServerHook.getInstance().fireCombatDamage(performer, defender, damage, type, pos)
```
→ framework's `CombatDamageEvent` fires. `powerscaling` and other mods receive it with armour already applied. Modified damage from the event is written back (`DamageEngine.java:91–92`).

Also: `causedWound()` / `receivedWound()` AI hooks (`:101–106`) for creature-behavior mods.

---

## 12. Config knobs (`DUSKombatMod.java:62–129` + `.properties`)

| Key | Default | Effect |
|---|---|---|
| `enableDUSKombat` | true | Kill switch |
| `minimumSwingTimer` | 2.0 s | Lower bound on swing interval |
| `useEpicBloodthirst` | true | Switches §7a bloodthirst between additive / multiplicative |
| `showItemCombatInformation` | true | Examine shows combat stats |
| `disablePlayerSkillLoss` | false | No skill loss on death |
| `playerToEnvironmentDamageMultiplier` | 1.0 | §10 |
| `environmentToPlayerDamageMultiplier` | 1.0 | §10 |
| `playerToPlayerDamageMultiplier` | 0.7 | §10 |
| `combatEnchantCap` | 0 | Hard cap on combat enchant stacks (0 = unlimited) |

---

## 13. Worked example

**Attacker:** Player, 60 Fighting, 70 Longsword, 80 Body Strength, QL 90 steel longsword, normal stance, no spells.
**Defender:** Troll, 40 CR, no worn armour, no focus.
**Environment:** ground level, face-to-face.

1. **Hit:** base 5, bonus ≈ 60 (fighting) + 0 (height/level/angle/mount/deity) ⇒ `skillCheck(5, +60)` → `attackCheck ≈ +20`. Hit.
2. **Dodge:** troll checks against `attackCheck * 1.5 = 30`. BC 40 → +4. Roll → -10. No dodge.
3. **Crit:** base 0.002. Roll 0.001 → crit (applied later).
4. **Base damage (§7a):**
   - `getModifiedDamageForWeapon` ≈ `5.5 * (1 + (80-40)/200) = 6.65` → `* 1000 = 6650`
   - Quality: `getBuffedQualityEffect(0.9) * 5.5 * 2400 ≈ 14256`
   - `base = 6650 + 14256 = 20906`
   - Material steel ×1.0, item bonus ×1.0, bloodthirst ≈ ×1.0, no rotting touch → **20906**
5. **Multiplier (§7b):**
   - Strength scaling: `1 + (80-20)/200 = 1.30`
   - Defensive-style check fails → `× 0.5` → `1.30 × 0.5 = 0.65`
   - Weapon skill 70 (≥ 50) → no ramp penalty
   - No village war / focus → **0.65**
6. After multiplier: `20906 × 0.65 = 13589`.
7. Backstab: none.
8. Mod hook: none.
9. Crit (§8): `× 1.5` → **20384**.
10. Armour (§9): troll natural `armourMod ≈ 0.4`. No glance (roll 0.08 > 0.05). `armourMod = 0.4`.
11. Wound (§10): slash, player→NPC ×1.0, no resists. Severity = `(int)(20384 × 0.4) = 8154`. Apply to torso.

**Final wound severity: 8154.**

---

## 14. Gaps — you still need vanilla source for these

DUSKombat delegates to these vanilla calls; to reproduce the numbers exactly, read:

| Method | Decompiled at |
|---|---|
| `Weapon.getModifiedDamageForWeapon` | `.../combat/Weapon.java` |
| `Weapon.getBaseDamageForWeapon` | `.../combat/Weapon.java` |
| `Weapon.getMaterialDamageBonus` | `.../combat/Weapon.java` |
| `Weapon.getMaterialHunterDamageBonus` | `.../combat/Weapon.java` |
| `Weapon.getCritChanceForWeapon` | `.../combat/Weapon.java` |
| `Weapon.getWeaponParryPercent` | `.../combat/Weapon.java` |
| `Weapon.getSkillPenaltyForWeapon` | `.../combat/Weapon.java` |
| `Server.getBuffedQualityEffect` | `.../Server.java` |
| `Server.getModifiedFloatEffect` | `.../Server.java` |
| `ItemBonus.getWeaponDamageIncreaseBonus` | `.../combat/ItemBonus.java` |
| `ArmourTemplate.calculateDR` | `.../combat/ArmourTemplate.java` |
| `Wound.getResistModifier` | `.../combat/Wound.java` |
| `CombatEngine.getEnchantBonus` | `.../combat/CombatEngine.java` |

Decompile root: `~/Scripts/Games/WurmUnlimited/PowerFantasy/Wurmguide/decompiled/server_decompiled/`

Use `wurmquery search <methodName>` to locate any of these.

Also opaque: the reflection-based `getDamageTypeModifier` used in unarmed damage (`DamageMethods.java:78`) — depends on creature template age modifier (champion, venerable).

---

## 15. Strategic note

This formula is 1100+ lines of imperative code across three files. **Writing a new combat mod today means reading all of it.** The per-stage events proposed in [`event-surface-gap.md`](event-surface-gap.md) (`WeaponBaseDamageEvent`, `DamageMultiplierEvent`, `ParryCheckEvent`, etc.) exist to make this readable: a future mod subscribes to the stage it cares about and doesn't need to know the rest of the formula.

Acceptance test for those events: **DUSKombat can be rewritten as ≤200 lines of `@SubscribeEvent` handlers, no `CombatAttackEvent` cancel, no `DamageEngine` rewrite.** When that's true, specs like this one stop needing to exist.
