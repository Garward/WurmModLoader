# Metal Loot System - Integration with bdew LootManager

## The Problem

Vanilla Wurm crafting grind is BRUTAL:
- Need to mine for 100+ hours to get decent quality materials
- Kills combat-focused gameplay loop
- Players want to FIGHT, not MINE

## The Solution - Power-Scaled Metal Drops

**Hook into bdew's existing LootManager** to add metal lump drops based on creature power level.

---

## Integration Strategy

### Use bdew_server_mod_tools LootManager

**Why:**
- ✅ Already deployed on server
- ✅ Clean fluent API for loot rules
- ✅ Already hooked into creature death
- ✅ No duplicated logic
- ✅ Proven, tested system

**Approach:**
1. Add bdew_server_mod_tools as dependency
2. Create LootRules in PowerScalingMod.preInit()
3. Register rules with LootManager.add()
4. Let bdew handle the rest!

---

## Metal Lump Scaling Formula

### Quality Based on Creature Power

```
Creature Power → Metal Lump Quality

Power 1-10:    30-40 QL  (early game, better than terrible mining)
Power 11-25:   40-50 QL  (mid-early, serviceable gear)
Power 26-50:   50-70 QL  (mid game, good quality)
Power 51-75:   70-85 QL  (late game, great quality)
Power 76-100:  85-95 QL  (endgame, excellent quality)
Power 100+:    95-99 QL  (legendary mobs, near-perfect)
```

**Formula:**
```java
baseQL = 30 + (creaturePower * 0.65)  // Linear scaling
variance = ±5                          // Random variance
finalQL = Math.min(99, baseQL + rand(-5, +5))
```

### Metal Type Based on Creature Type

**Regular Mobs:**
- Iron lumps (50% chance)
- Copper lumps (30% chance)
- Tin lumps (15% chance)
- Zinc lumps (5% chance)

**Champions (2x power):**
- Steel lumps (40% chance) - RARE, instant value
- Iron lumps (40% chance)
- Silver lumps (15% chance) - NICE
- Gold lumps (5% chance) - JACKPOT

**Uniques (5x power):**
- Adamantine lumps (30% chance) - EXOTIC
- Steel lumps (30% chance)
- Gold lumps (20% chance)
- Silver lumps (20% chance)

**Titans (10x power - custom):**
- Glimmersteel lumps (40% chance) - LEGENDARY
- Adamantine lumps (30% chance)
- Seryll lumps (20% chance) - PRIEST METAL
- Gold lumps (10% chance)

### Drop Amount

```
Base amount = 1 lump
Champion = 2-3 lumps
Unique = 3-5 lumps
Titan = 5-10 lumps
```

---

## Implementation

### 1. Add bdew Dependency

**File:** `mods/powerscaling/build.gradle.kts`

```kotlin
dependencies {
    // ... existing dependencies

    // bdew loot system integration
    compileOnly(files("../../distribution/bdew_server_mod_tools.jar"))
}
```

### 2. Create Metal Loot Rules

**File:** `mods/powerscaling/src/main/java/com/garward/wurmmodloader/mods/powerscaling/MetalLootIntegration.java`

```java
package com.garward.wurmmodloader.mods.powerscaling;

import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.items.ItemList;
import net.bdew.wurm.tools.server.loot.LootManager;
import net.bdew.wurm.tools.server.loot.LootRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

public class MetalLootIntegration {
    private static final Logger logger = Logger.getLogger(MetalLootIntegration.class.getName());

    public static void registerLootRules() {
        // Metal drops for all non-player creatures
        LootRule.create()
            .requireCreature(c -> !c.isPlayer())  // Any non-player creature
            .addDrop((creature, killer) -> generateMetalLoot(creature))
            .register();

        logger.info("Registered power-scaled metal loot rules");
    }

    private static Collection<Item> generateMetalLoot(Creature creature, Player killer) {
        Collection<Item> loot = new ArrayList<>();

        try {
            PowerScalingManager manager = PowerScalingManager.getInstance();
            int creaturePower = manager.getCreaturePowerLevel(creature);

            // Calculate quality based on power
            float baseQL = 30 + (creaturePower * 0.65f);
            float variance = (Server.rand.nextFloat() * 10) - 5; // ±5
            float finalQL = Math.min(99, Math.max(1, baseQL + variance));

            // Determine metal type and amount
            int metalTemplate = selectMetalType(creature);
            int amount = calculateAmount(creature);

            // Create lumps
            for (int i = 0; i < amount; i++) {
                Item lump = ItemFactory.createItem(
                    metalTemplate,
                    finalQL,
                    null // material (unused for lumps)
                );
                loot.add(lump);
            }

            // Debug log
            if (manager.getConfig().isDebugLogging()) {
                logger.info(String.format(
                    "Generated %d metal lump(s) (QL %.1f) from %s (power %d)",
                    amount, finalQL, creature.getName(), creaturePower
                ));
            }

        } catch (Exception e) {
            logger.warning("Failed to generate metal loot: " + e.getMessage());
        }

        return loot;
    }

    private static int selectMetalType(Creature creature) {
        float roll = Server.rand.nextFloat();

        if (creature.isUnique()) {
            // Uniques drop exotic metals
            if (roll < 0.30f) return ItemList.adamantineLump;
            if (roll < 0.60f) return ItemList.steelLump;
            if (roll < 0.80f) return ItemList.goldLump;
            return ItemList.silverLump;

        } else if (creature.isChampion()) {
            // Champions drop rare metals
            if (roll < 0.40f) return ItemList.steelLump;
            if (roll < 0.80f) return ItemList.ironLump;
            if (roll < 0.95f) return ItemList.silverLump;
            return ItemList.goldLump;

        } else {
            // Regular mobs drop common metals
            if (roll < 0.50f) return ItemList.ironLump;
            if (roll < 0.80f) return ItemList.copperLump;
            if (roll < 0.95f) return ItemList.tinLump;
            return ItemList.zincLump;
        }
    }

    private static int calculateAmount(Creature creature) {
        if (creature.isUnique()) {
            return 3 + Server.rand.nextInt(3); // 3-5 lumps
        } else if (creature.isChampion()) {
            return 2 + Server.rand.nextInt(2); // 2-3 lumps
        } else {
            return 1; // 1 lump
        }
    }
}
```

### 3. Register in PowerScalingMod

**File:** `PowerScalingMod.java`

```java
@Override
public void preInit() {
    // ... existing initialization

    // Register metal loot rules with bdew LootManager
    try {
        MetalLootIntegration.registerLootRules();
        logger.info("Metal loot integration enabled");
    } catch (Exception e) {
        logger.log(Level.WARNING, "Failed to register metal loot (bdew not loaded?)", e);
    }

    // ... rest of preInit
}
```

---

## Configuration

Add to `powerscaling.config`:

```properties
# ============================================================================
# METAL LOOT SYSTEM
# ============================================================================

# Enable metal lump drops from creatures
enableMetalLoot=true

# Quality scaling (base + power × multiplier)
metalLootBaseQL=30              # Minimum quality
metalLootPowerMultiplier=0.65   # Quality increase per power level
metalLootQLVariance=5           # ±variance

# Drop rates
regularMobDropChance=1.0        # 100% chance (always drop)
championBonusAmount=2           # Champions drop 2-3 lumps
uniqueBonusAmount=4             # Uniques drop 3-5 lumps
titanBonusAmount=9              # Titans drop 5-10 lumps

# Metal type chances (can be tuned per type)
# Future: Add fine-grained control if needed
```

---

## Player Experience

### Early Game (Power 1-25, Killing Regular Mobs)

```
Kill goblin (Power 5)
  ↓
Drop: 1× Iron Lump (35 QL)
  ↓
Craft basic sword → 35 QL weapon (better than newbie 10 QL!)
  ↓
Result: Crafting is ACCESSIBLE without mining grind
```

### Mid Game (Power 25-50, Killing Champions)

```
Kill Champion Troll (Power 50)
  ↓
Drop: 2× Steel Lump (62 QL) + 1× Silver Lump (60 QL)
  ↓
Craft armor set → 60+ QL gear (SOLID quality)
  ↓
Result: Combat = crafting progression
```

### Late Game (Power 75+, Killing Uniques)

```
Kill Forest Giant (Power 250)
  ↓
Drop: 4× Adamantine Lump (95 QL) + 1× Gold Lump (96 QL)
  ↓
Craft legendary gear → 95+ QL equipment (INSANE)
  ↓
Result: Endgame boss kills = endgame gear WITHOUT MINING
```

---

## Balance Notes

### Why This is Balanced

**Not Too Easy:**
- Still need smithing/crafting skills to USE the lumps
- Quality scales with creature difficulty (hard fights = better loot)
- Champions/Uniques are rare
- Lower power mobs drop lower QL (early game isn't trivial)

**Not Too Hard:**
- 100% drop rate means consistent progression
- No RNG frustration (you WILL get loot)
- Scales with power (natural difficulty curve)

**Encourages Combat:**
- Kill stuff → get materials → craft gear → kill harder stuff
- Perfect gameplay loop for combat-focused pack
- Mining becomes OPTIONAL (for massive quantities or specific projects)

### Comparison to Vanilla

**Vanilla Wurm:**
- Mine for 10 hours → Get 50 QL iron ore
- Smelt → Get 45 QL iron lump (skill loss)
- Improve to 60 QL → Another 5 hours
- **Total: 15+ hours for ONE 60 QL lump**

**Power Fantasy Mod:**
- Kill champion → Get 2-3× 60+ QL lumps instantly
- **Total: 5 minutes**

**But:**
- You still need to BEAT the champion (not easy)
- You still need smithing skill (crafting matters)
- You still need to survive (combat is risky)

---

## Testing Checklist

- [ ] bdew dependency compiles
- [ ] MetalLootIntegration registers without errors
- [ ] Killing regular mob drops 1 common metal lump
- [ ] Quality scales with creature power
- [ ] Champion drops 2-3 lumps of better metals
- [ ] Unique drops 3-5 lumps of exotic metals
- [ ] Lumps go into player inventory
- [ ] Quality formula works correctly (30-99 QL range)
- [ ] Config values can be tuned
- [ ] Debug logging shows loot generation

---

## Future Enhancements

### Phase 2 - Special Metal Drops

- **Titans drop Glimmersteel/Seryll** (legendary metals)
- **Boss mechanics** (special bosses drop guaranteed high-QL exotics)
- **Loot tables per creature type** (trolls drop iron, goblins drop copper, etc.)

### Phase 3 - Rare Drops

- **Enchanted lumps** (pre-enchanted materials)
- **Material infusion drops** (for MaterialSystem integration)
- **Crafting recipes** (rare scrolls that unlock recipes)

### Phase 4 - Loot Scaling

- **Group bonuses** (more players = more loot split)
- **Power level requirements** (high-level players get less from low mobs)
- **Diminishing returns** (farming same mob type reduces drops)

---

## Conclusion

This integration:
✅ Reuses existing bdew loot infrastructure
✅ No duplicated logic
✅ Clean, maintainable code
✅ Power-scaled drops (fits our progression)
✅ Makes crafting accessible (combat → materials)
✅ Eliminates mining grind (optional, not mandatory)
✅ Balanced (hard fights = better loot)
✅ Configurable (can tune all values)

**Priority:** HIGH - This is critical for combat-focused gameplay loop.

**Implementation Time:** 1-2 hours

Let's make combat → crafting progression ACTUALLY WORK! ⚔️🔨
