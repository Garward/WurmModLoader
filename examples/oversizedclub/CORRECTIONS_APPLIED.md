# Documentation Corrections Based on Weapon Class Bytecode Analysis

## Summary

After verifying against the actual `com.wurmonline.server.combat.Weapon` bytecode and WyvernMods examples, two critical errors were found and corrected in the tutorial documentation.

## Verification Method

1. **Decompiled Weapon class** using `javap -cp server.jar -c com.wurmonline.server.combat.Weapon`
2. **Analyzed bytecode** to determine exact parameter handling
3. **Cross-referenced** with working WyvernMods weapon registrations
4. **Verified** field names using `javap -private`

## Critical Corrections

### 1. Crit Parameter Division (CRITICAL BUG FIX)

**Original Documentation (WRONG):**
```java
0.002f,  // Crit chance: 0.2% (0.002 = 0.2%)
```

**Bytecode Evidence:**
```
Line 25: fload         4              // Load parameter 4 (crit)
Line 27: ldc           #7             // Load constant 5.0f
Line 29: fdiv                         // Divide: crit / 5.0
Line 30: putfield      #8             // Store in critchance field
```

**Corrected Documentation:**
```java
0.002f,  // Crit param: 0.04% actual (0.002/5.0 = 0.0004 = 0.04%)
```

**Impact:**
- Parameter values are **divided by 5.0** before being stored internally
- To get 1% actual crit, you need to pass 0.05f (not 0.01f)
- To get 0.4% actual crit, you need to pass 0.02f (not 0.004f)

**Conversion Table:**
| Parameter Value | Actual Crit Chance |
|-----------------|-------------------|
| 0.002f          | 0.04% (clubs)     |
| 0.012f          | 0.24%             |
| 0.015f          | 0.3%              |
| 0.02f           | 0.4% (titan)      |
| 0.05f           | 1.0%              |

### 2. Last Parameter is skillPenalty, Not armourDamageBonus

**Original Documentation (WRONG):**
```java
0.5d  // Armour damage bonus: 50% bonus (good vs armor)
```

**Method Signature Evidence:**
```java
public static final double getSkillPenaltyForWeapon(com.wurmonline.server.items.Item);
```

**Bytecode Evidence:**
```
Line 52: dload         8              // Load parameter 8 (double)
Line 54: putfield      #12            // Field skillPenalty:D
```

**Corrected Documentation:**
```java
0.5d  // Skill penalty: 0.5 (moderate difficulty)
```

**Impact:**
- This parameter controls skill effectiveness, NOT armor damage
- Armor damage is handled by `getMaterialArmourDamageBonus` (separate, material-based)
- Higher values = harder weapon to use (skill penalty)
- Common values: 0d (no penalty), 0.5d (moderate penalty)

**Value Meanings:**
| Value | Meaning                           | Examples        |
|-------|-----------------------------------|-----------------|
| 0d    | No skill penalty                  | Titan weapons   |
| 0.5d  | Moderate penalty                  | Clubs, knuckles |
| Higher| More difficult to use effectively | N/A in examples |

## Files Updated

1. **OversizedClubMod.java** - Lines 330-356, 370-379
   - Corrected crit parameter documentation
   - Corrected last parameter name and explanation
   - Added reference values from actual WyvernMods weapons

2. **WEAPON_CREATION_PITFALLS.md** - Lines 128-143, 239-248
   - Fixed Weapon constructor parameter comments
   - Added crit division warning
   - Updated complete working pattern

3. **CORRECTIONS_APPLIED.md** (this file)
   - Documents what was wrong and why
   - Provides bytecode evidence
   - Shows conversion tables

## Verified Against Working Examples

All corrections verified against WyvernMods ItemMod.java line 273-284:

```java
new Weapon(BattleYoyo.templateId, 6.85f, 3.75f, 0.012f, 2, 2, 0.0f, 0d);
new Weapon(Club.templateId, 8.3f, 4.5f, 0.002f, 3, 3, 0.4f, 0.5d);
new Weapon(Knuckles.templateId, 3.8f, 2.2f, 0.002f, 1, 1, 0.2f, 0.5d);
new Weapon(Warhammer.templateId, 9.50f, 5.6f, 0.015f, 4, 3, 1f, 0d);
new Weapon(MaartensMight.templateId, 11, 5, 0.02f, 4, 4, 1.0f, 0d);
new Weapon(Eviscerator.templateId, 100, 3f, 0.02f, 5, 5, 0.4f, 0.5d);
```

These are **production weapons** that have been used on live servers, confirming our corrected documentation is accurate.

## Complete Corrected Constructor

```java
/**
 * Weapon constructor - registers weapon with combat system
 *
 * @param templateId     Item template ID
 * @param damage         Base damage multiplier
 * @param speed          Seconds between attacks
 * @param critChance     Crit parameter (divided by 5.0 internally!)
 * @param reach          Attack range
 * @param weightGroup    Stamina drain category
 * @param parryPercent   Parry effectiveness (0.0-1.0)
 * @param skillPenalty   Combat difficulty modifier
 */
new Weapon(
    oversizedClubTemplateId,  // templateId
    15.0f,                    // damage
    6.0f,                     // speed
    0.002f,                   // critChance (becomes 0.04% actual)
    3,                        // reach
    4,                        // weightGroup
    0.4f,                     // parryPercent
    0.5d                      // skillPenalty
);
```

## Why This Matters

These corrections are **critical** for accurate weapon balancing:

1. **Crit rates** - Without knowing about the /5.0 division, modders would create weapons with 5x lower crit than intended
2. **Skill penalty** - Misunderstanding this as "armor damage" would lead to confusion about weapon effectiveness

The original documentation would have led future mod authors to create incorrectly balanced weapons.

## Additional Documentation: Armoury/DUSKombat Systems

After correcting the Weapon constructor parameters, comprehensive documentation was added for servers running Armoury/DUSKombat combat mods.

### Armoury Material System

**Material Damage Modifiers** (from Armoury.properties analysis):
- Steel: 1.03x damage bonus
- Bronze: 0.98x swing speed (2% faster)
- Glimmersteel: 0.95x parry modifier (5% more parries)
- Adamantine: 1.05x armor damage
- Wood: 1.0x (no bonuses)

**Armor Damage Sources**:
1. Vanilla Wurm: `getMaterialArmourDamageBonus(material)` - varies by material
2. Armoury: `materialWeaponArmourDamage` multiplier - configurable per material
3. Damage type effectiveness: Crush > Slash > Pierce vs armor
4. DUSKombat: Armor takes 2x damage compared to vanilla

### DUSKombat Combat Flow

**New Combat Sequence:**
```
Swing → Accuracy → Dodge → Critical → Shield Block → Parry → Glance → Damage
```

**Key Changes**:
- Accuracy check (weapon skill) replaces CR hit/miss
- Dodge is core mechanic (not RNG)
- Critical strikes: 1.5x damage, bypass parry/block
- Stamina affects dodge/parry chance significantly
- PvP damage: 0.7x multiplier (30% reduction)
- PvE damage: 1.0x multiplier

**Critical Strike System**:
- Available in both PvE and PvP
- Bypasses all defensive checks except dodge
- Our club: 0.04% base crit (0.002 param ÷ 5.0)

### Documentation Location

All Armoury/DUSKombat information added to **OversizedClubMod.java lines 382-505**:
- Material damage modifiers
- DUSKombat combat flow
- Complete damage calculation examples
- Armor damage mechanics (all 4 sources)

**Kept vanilla documentation** for servers without these mods - the advanced section is clearly marked as optional.

## Verification Status

✅ **Verified against bytecode** - Direct analysis of Weapon class constructor
✅ **Verified against production code** - WyvernMods examples that work on live servers
✅ **Verified method signatures** - Confirmed field names via javap
✅ **All corrections applied** - OversizedClubMod.java and WEAPON_CREATION_PITFALLS.md updated
✅ **Armoury config analyzed** - Extracted and documented material modifiers
✅ **DUSKombat flow documented** - Combat mechanics from official documentation
✅ **Armor damage clarified** - Corrected misconception about skillPenalty parameter

## Credits

Corrections discovered through user feedback requesting verification against actual Armoury examples, followed by bytecode analysis to determine ground truth. Additional Armoury/DUSKombat documentation added per user request to include combat mod calculations.
