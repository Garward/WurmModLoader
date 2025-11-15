# MaterialSystem Mod - Design Document

**Version:** 1.0
**Status:** Design Phase
**Dependencies:** Phase 4 Registry (✅ Complete), Icon System (✅ Complete)

---

## Table of Contents

1. [Overview](#overview)
2. [Material Tiers](#material-tiers)
3. [Material Definitions](#material-definitions)
4. [Item Templates](#item-templates)
5. [Creature Loot Integration](#creature-loot-integration)
6. [Bonus System](#bonus-system)
7. [Icon System Integration](#icon-system-integration)
8. [Public API](#public-api)
9. [Implementation Tasks](#implementation-tasks)

---

## Overview

### Purpose

MaterialSystem registers custom material items that drop from creatures and can be infused into soulbound gear for permanent bonuses.

### Key Features

- ✅ 15+ unique material items (Common, Rare, Legendary)
- ✅ Phase 4 Registry integration with ResourceLocation
- ✅ Creature loot table system (JSON-based)
- ✅ Custom icons for all materials (64×64 PNG)
- ✅ Material bonus definitions for SoulboundGear integration
- ✅ Public API for other mods to query material data

### Design Goals

1. **Simplicity:** Materials are just items with custom metadata
2. **Modularity:** No database needed - all data in item templates
3. **Extensibility:** Other mods can add materials via registry
4. **Performance:** Lightweight - no hooks or complex logic

---

## Material Tiers

### Tier 1: Common Materials

**Sources:** Basic creatures (wolves, bears, spiders, trolls)
**Drop Rate:** 5-10%
**Max Stacks:** 50 per item
**Power Level:** Small incremental bonuses

| Material | Source | Bonus |
|----------|--------|-------|
| Wolf Fang | Wolf | +1-3 damage per stack |
| Bear Claw | Bear | +2-4 damage per stack |
| Spider Venom Sac | Large Spider | +1% poison chance |
| Troll Hide | Troll | +5% durability |
| Scorpion Chitin | Scorpion | +2% parry chance |

### Tier 2: Rare Materials

**Sources:** Champion creatures, unique spawns
**Drop Rate:** 25-50% (champions), 100% (uniques)
**Max Stacks:** 5 per item
**Power Level:** Significant bonuses

| Material | Source | Bonus |
|----------|--------|-------|
| Champion Heart | Any Champion | +20 damage, +5% all stats |
| Dragon Scale | Dragon Hatchling | +15 damage, +10% armor |
| Unicorn Horn | Unicorn | +10% healing effectiveness |
| Forest Giant Essence | Forest Giant | +25% stamina regen |

### Tier 3: Legendary Materials

**Sources:** Titans only (Fire, Ice, Nature, Shadow)
**Drop Rate:** 100%
**Max Stacks:** 1 per item (unique)
**Power Level:** Build-defining bonuses

| Material | Source | Bonus |
|----------|--------|-------|
| Ifrit Core | Fire Titan | +50 fire damage, +10% attack speed, flames visual |
| Frozen Heart | Ice Titan | +50 cold damage, +10% crit chance, ice visual |
| Verdant Seed | Nature Titan | Life leech, +HP regen, nature visual |
| Void Shard | Shadow Titan | Shadow damage, fear effect, shadow visual |

---

## Material Definitions

### Data Model

Each material has:

```java
public class Material {
    // Identity (from ResourceLocation)
    private final String namespace;     // "powerfantasy"
    private final String id;            // "ifrit_core"

    // Display
    private final String name;          // "Ifrit Core"
    private final String namePlural;    // "Ifrit Cores"
    private final String description;   // "A blazing core..."

    // Tier
    private final MaterialTier tier;    // COMMON, RARE, LEGENDARY

    // Infusion properties
    private final int maxStacks;        // How many can be infused per item
    private final MaterialBonus bonus;  // What bonuses it grants

    // Visual
    private final short iconId;         // Custom icon ID
    private final String visualEffect;  // "flames", "ice", "shadow", null

    // Item template properties
    private final int weightGrams;      // Physical weight
    private final int value;            // Copper value
}

public enum MaterialTier {
    COMMON,      // Wolf Fang, Bear Claw
    RARE,        // Champion Heart, Dragon Scale
    LEGENDARY    // Titan materials
}

public class MaterialBonus {
    // Flat bonuses
    private final float baseDamage;           // +50 damage

    // Percentage bonuses
    private final float damagePercent;        // +10% damage
    private final float attackSpeedPercent;   // +10% attack speed
    private final float critChancePercent;    // +10% crit chance
    private final float durabilityPercent;    // +5% durability
    private final float parryPercent;         // +2% parry

    // Elemental damage
    private final Map<String, Float> elementalDamage;  // {"fire": 50.0}

    // Special effects
    private final SpecialEffect specialEffect;  // LIFESTEAL, FEAR, etc.
}
```

### Material Registry

Materials stored in Phase 4 Registry:

```java
// Registration (in MaterialSystemMod.onItemTemplatesCreated)
ResourceLocation id = new ResourceLocation("powerfantasy", "ifrit_core");
Material material = new Material(id, materialBonus, ...);

// Registry storage (automatic via ItemTemplateBuilder)
Registries.ITEMS.register(id, itemTemplate);

// Additional metadata storage
MaterialRegistry.register(id, material);  // Custom registry for bonus data
```

---

## Item Templates

### Template Structure

All materials follow this pattern:

```java
@SubscribeEvent
public void onItemTemplatesCreated(ItemTemplatesCreatedEvent event) {
    // Register icon
    Icon icon = IconRegistry.registerCustom(
        new ResourceLocation("powerfantasy", "ifrit_core"),
        "ifrit_core.png"
    );

    // Create item template
    ItemTemplate template = new ItemTemplateBuilder(
            new ResourceLocation("powerfantasy", "ifrit_core"))
        .name("Ifrit Core", "Ifrit Cores",
              "A blazing core pulsing with the essence of a Fire Titan. " +
              "Infusing this into a weapon grants devastating fire damage.")
        .imageNumber((short) icon.getIconId())

        // Physical properties
        .weightGrams(500)       // 0.5kg (materials are light)
        .dimensions(5, 5, 5)    // Small cube
        .material(Materials.MATERIAL_STONE)  // Generic material

        // Item types
        .itemTypes(new short[] {
            ItemTypes.ITEM_TYPE_DECORATION,  // Can be placed
            ItemTypes.ITEM_TYPE_NO_IMPROVE,  // Cannot be improved
            ItemTypes.ITEM_TYPE_TURNABLE     // Can rotate
        })

        // Value and decay
        .value(100000)          // 1 silver (legendary material!)
        .decayTime(Long.MAX_VALUE)  // Never decays

        // Behavior
        .behaviourType((short) 1)
        .difficulty(0.0f)

        .build();

    // Store bonus metadata for later retrieval
    MaterialBonus bonus = new MaterialBonus.Builder()
        .baseDamage(50.0f)
        .attackSpeedPercent(0.10f)
        .elementalDamage("fire", 50.0f)
        .visualEffect("flames")
        .build();

    MaterialRegistry.register(
        new ResourceLocation("powerfantasy", "ifrit_core"),
        bonus,
        MaterialTier.LEGENDARY,
        1  // maxStacks
    );
}
```

### Common Template Properties

**All materials share:**
- No crafting recipes (drop-only)
- No improvement allowed
- Never decay
- Small size (5×5×5 mm)
- Light weight (100-500g)
- High value (tier-based)

**Tier-specific values:**
```java
// Common materials
.value(1000)           // 10 copper
.weightGrams(100)

// Rare materials
.value(10000)          // 1 silver
.weightGrams(300)

// Legendary materials
.value(100000)         // 1 gold
.weightGrams(500)
```

---

## Creature Loot Integration

### Approach: JSON Loot Tables

**Why JSON?**
- ✅ No database needed
- ✅ Easy to edit/balance
- ✅ Can be loaded from mods directory
- ✅ Supports hot-reload (future)

### Loot Table Format

**File:** `mods/powerfantasy/loot_tables.json`

```json
{
  "version": "1.0",
  "loot_tables": [
    {
      "creature_template": "wolf",
      "drops": [
        {
          "material_id": "powerfantasy:wolf_fang",
          "chance": 0.05,
          "min_quantity": 1,
          "max_quantity": 1
        }
      ]
    },
    {
      "creature_template": "bear",
      "drops": [
        {
          "material_id": "powerfantasy:bear_claw",
          "chance": 0.10,
          "min_quantity": 1,
          "max_quantity": 2
        }
      ]
    },
    {
      "creature_template": "fire_titan",
      "drops": [
        {
          "material_id": "powerfantasy:ifrit_core",
          "chance": 1.0,
          "min_quantity": 1,
          "max_quantity": 1
        }
      ]
    }
  ]
}
```

### Integration Options

#### Option 1: CreatureMod Integration (Recommended)

If using existing creature mod system:

```json
// creatures/fire_titan.json
{
  "template_id": "powerfantasy.fire_titan",
  "combat_rating": 50,
  "loot_table": {
    "powerfantasy:ifrit_core": {
      "chance": 1.0,
      "min_quantity": 1,
      "max_quantity": 1
    }
  }
}
```

#### Option 2: Hook Creature.die() (Fallback)

If no creature mod integration:

```java
@Override
public void preInit() {
    ClassPool classPool = HookManager.getInstance().getClassPool();
    CtClass ctCreature = classPool.get("com.wurmonline.server.creatures.Creature");

    CtMethod dieMethod = ctCreature.getDeclaredMethod("die");
    dieMethod.insertAfter(
        "com.garward.wurmmodloader.mods.materialsystem.LootDropHandler.onCreatureDeath(this);"
    );
}

// LootDropHandler.java
public static void onCreatureDeath(Creature creature) {
    String templateName = creature.getTemplate().getTemplateName();
    List<LootDrop> drops = LootTableManager.getDrops(templateName);

    for (LootDrop drop : drops) {
        if (Math.random() < drop.getChance()) {
            int quantity = drop.getRandomQuantity();
            ItemTemplate material = MaterialRegistry.getItemTemplate(drop.getMaterialId());

            // Create item at corpse location
            Item materialItem = ItemFactory.createItem(
                material.getTemplateId(),
                (float) (10 + Math.random() * 90),  // QL 10-100
                null
            );
            materialItem.setPosXYZ(creature.getPosX(), creature.getPosY(), creature.getPosZ());
        }
    }
}
```

---

## Bonus System

### MaterialBonus Class

```java
public class MaterialBonus {
    // Flat bonuses (added directly)
    private final float baseDamage;           // +50 damage

    // Percentage bonuses (multiplicative)
    private final float damagePercent;        // +10% damage (0.10)
    private final float attackSpeedPercent;   // +10% attack speed (0.10)
    private final float critChancePercent;    // +10% crit (0.10)
    private final float durabilityPercent;    // +5% durability (0.05)
    private final float parryPercent;         // +2% parry (0.02)

    // Elemental damage (separate damage types)
    private final Map<String, Float> elementalDamage;
    // {"fire": 50.0, "cold": 25.0}

    // Special effects (unique per material)
    private final SpecialEffect specialEffect;

    // Visual effects (client-side)
    private final String visualEffect;  // "flames", "ice", "shadow", null

    // Builder pattern for construction
    public static class Builder {
        private float baseDamage = 0.0f;
        private float damagePercent = 0.0f;
        private float attackSpeedPercent = 0.0f;
        private float critChancePercent = 0.0f;
        private float durabilityPercent = 0.0f;
        private float parryPercent = 0.0f;
        private Map<String, Float> elementalDamage = new HashMap<>();
        private SpecialEffect specialEffect = null;
        private String visualEffect = null;

        public Builder baseDamage(float value) {
            this.baseDamage = value;
            return this;
        }

        public Builder damagePercent(float value) {
            this.damagePercent = value;
            return this;
        }

        public Builder elementalDamage(String type, float value) {
            this.elementalDamage.put(type, value);
            return this;
        }

        public Builder visualEffect(String effect) {
            this.visualEffect = effect;
            return this;
        }

        public MaterialBonus build() {
            return new MaterialBonus(this);
        }
    }
}
```

### Bonus Calculation (for SoulboundGear)

```java
public class MaterialBonusCalculator {
    /**
     * Calculate total bonuses from all infused materials.
     *
     * @param infusions Map of material_id -> stack_count
     * @return Aggregated bonuses
     */
    public static MaterialBonus calculateTotalBonuses(Map<String, Integer> infusions) {
        MaterialBonus.Builder total = new MaterialBonus.Builder();

        for (Map.Entry<String, Integer> entry : infusions.entrySet()) {
            ResourceLocation materialId = ResourceLocation.parse(entry.getKey());
            int stacks = entry.getValue();

            MaterialBonus singleBonus = MaterialRegistry.getBonus(materialId);
            if (singleBonus == null) continue;

            // Multiply bonuses by stack count
            total.baseDamage(total.baseDamage + singleBonus.getBaseDamage() * stacks);
            total.damagePercent(total.damagePercent + singleBonus.getDamagePercent() * stacks);
            total.attackSpeedPercent(total.attackSpeedPercent + singleBonus.getAttackSpeedPercent() * stacks);

            // Elemental damage (additive)
            for (Map.Entry<String, Float> elem : singleBonus.getElementalDamage().entrySet()) {
                float current = total.elementalDamage.getOrDefault(elem.getKey(), 0.0f);
                total.elementalDamage(elem.getKey(), current + elem.getValue() * stacks);
            }
        }

        return total.build();
    }
}
```

### Special Effects

```java
public enum SpecialEffect {
    LIFESTEAL,      // Verdant Seed - heal on hit
    FEAR,           // Void Shard - chance to fear enemy
    BURN,           // Ifrit Core - DoT fire damage
    FREEZE,         // Frozen Heart - chance to slow
    POISON,         // Spider Venom - DoT poison
    NONE
}
```

---

## Icon System Integration

### Icon Requirements

**Total Icons Needed:** 15+ materials

**Icon Specifications:**
- Format: PNG with alpha transparency
- Size: 64×64 pixels
- Style: Match Wurm's aesthetic (hand-painted, medieval)
- Naming: `{material_id}.png`

### Icon File Structure

```
mods/powerfantasy/icons/
├── wolf_fang.png
├── bear_claw.png
├── spider_venom_sac.png
├── troll_hide.png
├── scorpion_chitin.png
├── champion_heart.png
├── dragon_scale.png
├── unicorn_horn.png
├── forest_giant_essence.png
├── ifrit_core.png
├── frozen_heart.png
├── verdant_seed.png
└── void_shard.png
```

### Icon Registration Pattern

```java
// In MaterialSystemMod.onItemTemplatesCreated()
private static short registerMaterialIcon(String name) {
    Icon icon = IconRegistry.registerCustom(
        new ResourceLocation("powerfantasy", name),
        name + ".png"
    );
    return (short) icon.getIconId();
}

// Usage
short ifritIcon = registerMaterialIcon("ifrit_core");
builder.imageNumber(ifritIcon);
```

### Icon Generation Notes

**Automatic pack generation will:**
1. Load all `powerfantasy:*` icons from `mods/powerfantasy/icons/`
2. Composite onto vanilla sheets at allocated positions
3. Save to `httpserver/iconpacks/` for client distribution
4. Generate manifest for debugging

**No manual work needed!** Just drop 64×64 PNGs in `icons/` folder.

---

## Public API

### MaterialRegistry

```java
package com.garward.wurmmodloader.mods.materialsystem;

import com.garward.wurmmodloader.api.registry.ResourceLocation;
import java.util.Optional;

/**
 * Public API for other mods to query material data.
 *
 * Thread-safe singleton.
 */
public final class MaterialRegistry {

    /**
     * Register a material with bonus data.
     *
     * @param id Material ResourceLocation
     * @param bonus Bonuses granted by this material
     * @param tier Material tier
     * @param maxStacks Maximum stacks per item
     */
    public static void register(ResourceLocation id, MaterialBonus bonus,
                                MaterialTier tier, int maxStacks);

    /**
     * Get material bonus data.
     *
     * @param id Material ResourceLocation
     * @return Material bonus, or empty if not found
     */
    public static Optional<MaterialBonus> getBonus(ResourceLocation id);

    /**
     * Get material tier.
     *
     * @param id Material ResourceLocation
     * @return Material tier, or empty if not found
     */
    public static Optional<MaterialTier> getTier(ResourceLocation id);

    /**
     * Get maximum stacks for material.
     *
     * @param id Material ResourceLocation
     * @return Max stacks, or 0 if not found
     */
    public static int getMaxStacks(ResourceLocation id);

    /**
     * Check if ResourceLocation is a registered material.
     *
     * @param id ResourceLocation to check
     * @return true if material exists
     */
    public static boolean isMaterial(ResourceLocation id);

    /**
     * Get all registered materials.
     *
     * @return Unmodifiable collection of material IDs
     */
    public static Collection<ResourceLocation> getAllMaterials();

    /**
     * Get all materials of a specific tier.
     *
     * @param tier Material tier to filter
     * @return Unmodifiable collection of material IDs
     */
    public static Collection<ResourceLocation> getMaterialsByTier(MaterialTier tier);
}
```

### Usage by Other Mods

```java
// SoulboundGear mod checking if item is a material
public boolean canInfuse(Item item) {
    ItemTemplate template = item.getTemplate();
    String templateName = template.getName();  // e.g., "powerfantasy.ifrit_core"
    ResourceLocation id = ResourceLocation.parse(templateName);

    return MaterialRegistry.isMaterial(id);
}

// Getting material bonuses
Optional<MaterialBonus> bonus = MaterialRegistry.getBonus(
    new ResourceLocation("powerfantasy", "ifrit_core")
);

if (bonus.isPresent()) {
    float damage = bonus.get().getBaseDamage();  // 50.0
    // Apply to soulbound item...
}
```

---

## Implementation Tasks

### Phase 1: Core Infrastructure (Week 1, Day 1-2)

- [x] Create MaterialSystemMod skeleton
- [ ] Implement MaterialBonus class with Builder
- [ ] Implement MaterialRegistry singleton
- [ ] Add material tier enum
- [ ] Create loot table JSON schema
- [ ] Write unit tests for MaterialBonus

### Phase 2: Item Templates (Week 1, Day 3-4)

- [ ] Register Common materials (Wolf Fang, Bear Claw, etc.)
- [ ] Register Rare materials (Champion Heart, Dragon Scale)
- [ ] Register Legendary materials (Ifrit Core, Frozen Heart, etc.)
- [ ] Implement icon registration for all materials
- [ ] Test item creation in-game

### Phase 3: Icon Integration (Week 1, Day 4)

- [ ] Create/source 64×64 PNG icons for all materials
- [ ] Place icons in `mods/powerfantasy/icons/`
- [ ] Test icon pack generation
- [ ] Verify icons display in client

### Phase 4: Loot System (Week 1, Day 5)

**Option A: CreatureMod Integration**
- [ ] Create JSON loot tables for all creatures
- [ ] Integrate with existing creature mod system
- [ ] Test drops in-game

**Option B: Hook-based System**
- [ ] Hook Creature.die() in preInit()
- [ ] Implement LootDropHandler
- [ ] Load JSON loot tables
- [ ] Test drops in-game

### Phase 5: Testing & Polish (Week 1, Day 6-7)

- [ ] Kill test creatures, verify drops
- [ ] Check icon display
- [ ] Verify MaterialRegistry API works
- [ ] Document API for SoulboundGear integration
- [ ] Write integration guide

---

## Success Criteria

✅ **All materials registered** - 15+ unique materials in registry
✅ **Icons working** - All materials have custom 64×64 icons displaying in client
✅ **Loot drops working** - Materials drop from correct creatures at correct rates
✅ **API functional** - SoulboundGear mod can query material data
✅ **No database needed** - All data in item templates + JSON loot tables
✅ **No performance issues** - Loot system adds <1ms per creature death

---

## Integration Notes for Other Mods

### For SoulboundGear

```java
// Check if item is infuseable material
if (MaterialRegistry.isMaterial(itemId)) {
    MaterialBonus bonus = MaterialRegistry.getBonus(itemId).orElseThrow();
    int maxStacks = MaterialRegistry.getMaxStacks(itemId);

    // Apply infusion logic...
}
```

### For PowerScaling

```java
// Get total material bonuses from soulbound item
Map<String, Integer> infusions = soulboundItem.getInfusions();
MaterialBonus totalBonus = MaterialBonusCalculator.calculateTotalBonuses(infusions);

float finalDamage = baseDamage;
finalDamage += totalBonus.getBaseDamage();
finalDamage *= (1.0f + totalBonus.getDamagePercent());

// Apply elemental damage...
Map<String, Float> elemDmg = totalBonus.getElementalDamage();
for (Map.Entry<String, Float> entry : elemDmg.entrySet()) {
    applyElementalDamage(defender, entry.getKey(), entry.getValue());
}
```

---

**Next Steps:** Implement Phase 1 (Core Infrastructure)

**Estimated Time:** 1 week for complete MaterialSystem implementation
