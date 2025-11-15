# OversizedClubMod Tutorial Summary

This document provides a quick reference for the comprehensive tutorials found in `OversizedClubMod.java`.

## What This Example Teaches

The OversizedClubMod serves as the **definitive tutorial** for creating custom weapons with WurmModLoader. It demonstrates every critical pattern needed for fully functional weapon creation.

## Quick Links to Inline Tutorials

Open `OversizedClubMod.java` and search for these section headers:

### 1. 🎨 Icon Copying Strategy
**Lines: ~125-140**
- How to dynamically copy icons from existing items
- Why hardcoding icon numbers is bad
- Examples of icon sources for different weapon types

### 2. 🔴 ITEM TYPES - Weapon Damage Types
**Lines: ~177-221**
- **CRITICAL SECTION**: How to change weapon damage types
- Explains CRUSH vs SLASH vs PIERCE
- Shows which skills each type uses
- Common pitfall: Missing `ITEM_TYPE_WEAPON` base type

**Quick Examples:**
```java
// Current (CRUSH):
ItemTypes.ITEM_TYPE_WEAPON_CRUSH

// Change to SLASH:
ItemTypes.ITEM_TYPE_WEAPON_SLASH

// Change to PIERCE:
ItemTypes.ITEM_TYPE_WEAPON_PIERCE
```

### 3. 🌳 Material Types Tutorial
**Lines: ~243-279**
- How to change from WOOD to METAL, STONE, etc.
- Lists all available materials (iron, steel, silver, gold, etc.)
- **Critical**: Both `.material()` and `itemTypes` must match!

**Quick Example - Wood to Steel:**
1. Change line 212: `ITEM_TYPE_WOOD` → `ITEM_TYPE_METAL`
2. Change line 278: `MATERIAL_WOOD_BIRCH` → `MATERIAL_STEEL`
3. Update recipe (see section 4)

### 4. ⚔️ Weapon Combat Registration
**Lines: ~294-380**
- **CRITICAL FOR ARMOURY/DUSKCOMBAT**: Weapon() constructor explained
- All 8 parameters documented with examples
- Comparison table of vanilla weapon stats
- Explains why weapons do 0 damage without this

### 4b. 📚 Armoury/DUSKombat Advanced Systems (OPTIONAL)
**Lines: ~382-505**
- **Skip if vanilla server** - This section is for combat mods only!
- Material damage modifiers (steel +3%, etc.)
- DUSKombat combat flow (Accuracy → Dodge → Crit → Parry → Damage)
- Complete damage calculations with examples
- Armor damage mechanics (4 different sources explained)
- PvP damage multipliers (0.7x)
- Critical strike system (1.5x damage, bypass defenses)

**Parameter Guide:**
```java
new Weapon(
    templateId,      // From .getTemplateId()
    damage,          // 15.0 = high damage
    speed,           // 6.0 = seconds between swings
    critChance,      // 0.002 = 0.2% crit
    reach,           // 3 = standard melee
    weightGroup,     // 4 = heavy stamina drain
    parryPercent,    // 0.4 = 40% parry modifier
    armourDamage     // 0.5 = 50% bonus vs armor
);
```

### 5. 📖 Crafting Recipe Tutorial
**Lines: ~371-434**
- Recipe format: `[SOURCE] + [TOOL] = [RESULT]`
- How to change tools (carving knife → hammer, etc.)
- How to change source materials
- **Critical**: Source weight must exceed result weight!
- Examples of common tools and materials

**Quick Recipe Changes:**
```java
// Current: LOG + CARVING KNIFE (carpentry)
ItemList.log + ItemList.knifeCarving

// To smithing: STEEL LUMP + HAMMER
ItemList.steelLump + ItemList.hammerMetal
(also change skill to SkillList.SMITHING_WEAPON_HEADS)

// Multiple recipes: Call createSimpleEntry multiple times!
```

### 6. 🔒 Material Restrictions ("Steel Only")
**Lines: ~436-509**
- Problem: Default recipes accept any variant of material
- Two solutions:
  - **Simple** (recommended): Create specific recipes per material
  - **Advanced**: Bytecode hooks for validation

**Recommended "Steel Only" Pattern:**
```java
// Only accept steel lump
CreationEntryCreator.createSimpleEntry(
    SkillList.SMITHING_WEAPON_HEADS,
    ItemList.hammerMetal,
    ItemList.steelLump,              // ONLY this material!
    oversizedClubTemplateId,
    false, true, 0.0f, false, false,
    CreationCategories.WEAPONS
);

// Want to also allow steel bar? Add second recipe:
CreationEntryCreator.createSimpleEntry(
    SkillList.SMITHING_WEAPON_HEADS,
    ItemList.hammerMetal,
    ItemList.steelBar,               // Also accept bars
    oversizedClubTemplateId,
    false, true, 0.0f, false, false,
    CreationCategories.WEAPONS
);
```

## Complete Example Walkthrough

Want to create a **Steel Slashing Sword** instead of a wood crushing club?

### Changes Required:

1. **Line 133**: Copy icon from sword instead
   ```java
   ItemTemplate sourceTpl = ItemTemplateFactory.getInstance()
       .getTemplate(ItemList.swordLong);
   ```

2. **Line 173**: Use sword model
   ```java
   .modelName("model.weapon.sword.long.")
   ```

3. **Lines 212-214**: Change material type
   ```java
   ItemTypes.ITEM_TYPE_METAL,      // Was: ITEM_TYPE_WOOD
   ItemTypes.ITEM_TYPE_WEAPON,
   ItemTypes.ITEM_TYPE_WEAPON_SLASH // Was: WEAPON_CRUSH
   ```

4. **Line 227**: Change skill
   ```java
   .primarySkill(SkillList.SWORDS_LONG) // Was: CLUB_HUGE
   ```

5. **Line 278**: Change material
   ```java
   .material(Materials.MATERIAL_STEEL) // Was: MATERIAL_WOOD_BIRCH
   ```

6. **Lines 360-369**: Adjust Weapon stats
   ```java
   new Weapon(
       oversizedClubTemplateId,
       12.0f,    // Sword damage (was 15.0)
       4.0f,     // Faster swing (was 6.0)
       0.005f,   // Higher crit (was 0.002)
       3, 3,     // Lighter weight group (was 4)
       0.3f,     // Lower parry (was 0.4)
       0.3d      // Lower armor damage (was 0.5)
   );
   ```

7. **Lines 423-434**: Change recipe to smithing
   ```java
   CreationEntryCreator.createSimpleEntry(
       SkillList.SMITHING_WEAPON_HEADS, // Was: CARPENTRY
       ItemList.hammerMetal,            // Was: knifeCarving
       ItemList.steelLump,              // Was: log
       oversizedClubTemplateId,
       false, true, 0.0f, false, false,
       CreationCategories.WEAPONS
   );
   ```

**Done!** You now have a steel slashing sword instead of a wood crushing club.

## Testing Checklist

After making changes, verify:

1. ✅ **Compiles**: `./gradlew build`
2. ✅ **Server starts**: Check logs for creation confirmation
3. ✅ **Recipe visible**: Right-click source item with tool activated
4. ✅ **Craftable**: Can successfully create the item
5. ✅ **Equippable**: Item goes in weapon slots, not inventory
6. ✅ **Examine text**: Shows proper description and stats
7. ✅ **Deals damage**: Attack a mob to verify combat works
8. ✅ **Icon correct**: Inventory shows proper icon, not black box

## Common Mistakes

See `WEAPON_CREATION_PITFALLS.md` for detailed explanations of:

1. **Material weight validation failure** - Source too light for result
2. **Missing ITEM_TYPE_WEAPON** - Item acts as container
3. **No Weapon registration** - 0 damage on Armoury/DUSKombat servers
4. **Hardcoded icons** - Black boxes or wrong icons

## Performance Results

Our oversized club at Quality 9:
- **Damage**: 8341 (vs regular club's 4290)
- **Speed**: 6.0s (vs regular club's 4.5s)
- **DPS**: ~1390 (vs regular club's ~953)
- **Result**: ~46% DPS increase, balanced by slower swing

## Additional Resources

- **Main tutorial**: `OversizedClubMod.java` (heavily commented)
- **Pitfalls guide**: `WEAPON_CREATION_PITFALLS.md`
- **API reference**: JavaDoc in WurmModLoader modules
- **Example patterns**: Other example mods in `examples/` directory

## Armoury/DUSKombat Quick Reference

### When Do I Need This?

Check your server logs for:
```
WARNING mod.sin.armoury.WeaponsTweaks ... Weapon map does not contain entry for ...
```

If you see this, your server uses Armoury/DUSKombat and you **MUST** register weapons with `new Weapon()`.

### Material Bonuses (Armoury)

| Material | Damage | Speed | Parry | Armor Dmg | Notes |
|----------|--------|-------|-------|-----------|-------|
| Wood | 1.0x | 1.0x | 1.0x | - | No bonuses |
| Iron | 1.0x | 1.0x | 1.0x | - | Baseline metal |
| Steel | **1.03x** | 1.0x | 1.0x | - | 3% more damage |
| Bronze | 1.0x | **0.98x** | 1.0x | - | 2% faster |
| Glimmersteel | 1.0x | 1.0x | **0.95x** | - | 5% more parries |
| Adamantine | 1.0x | 1.0x | 1.0x | **1.05x** | 5% armor dmg |

**Note**: Lower speed = faster swings, Lower parry = more parries

### DUSKombat Damage Multipliers

- **PvE (vs mobs)**: 1.0x (no change)
- **PvP (player vs player)**: **0.7x** (30% reduction)
- **Critical hit**: **1.5x** damage, bypasses parry/block
- **Armor damage**: **2.0x** compared to vanilla

### Quick Damage Formula (Armoury + DUSKombat)

```
Final Damage = base_damage × material_mod × quality_mod × DUSK_multiplier × skill/stance/focus
```

Example (our club @ Q9 vs mob):
```
8341 = 15.0 × 1.0 (wood) × 0.556 (Q9) × 1.0 (PvE) × [other multipliers]
```

Example (Q100 steel vs mob):
```
~27000 = 15.0 × 1.03 (steel) × 1.0 (Q100) × 1.0 (PvE) × [other multipliers]
```

## Questions?

This example was created through iterative debugging and testing. Every pattern shown here was discovered by hitting real errors and finding solutions. The inline comments reference specific line numbers and show exactly what to change for different weapon types.

**For vanilla servers**: Skip the Armoury/DUSKombat section (lines 382-505).
**For modded servers**: Study the advanced combat calculations to understand damage scaling.

**Start with OversizedClubMod.java and follow along with the inline tutorials!**
