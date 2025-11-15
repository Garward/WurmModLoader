# Material Damage Types - Implementation Notes

## Using Existing Wurm Damage Types

Instead of inventing custom damage types, we leverage existing Wurm enchantment damage systems.

### Elemental Damage Mapping

**Fire Damage (Ifrit Core)**
- Use existing **Flaming Aura** enchantment damage type
- Already implemented in Wurm
- Fire-based damage over time
- Visual: flames particle effect

**Cold Damage (Frozen Heart)**
- Use existing **Frostbrand** enchantment damage type
- Already implemented in Wurm
- Cold-based damage, slowing effect
- Visual: ice/frost particle effect

**Shadow Damage (Void Shard)**
- Use existing **Rotting Touch** enchantment damage type
- Already implemented in Wurm
- Key properties:
  - Extremely difficult to heal by conventional first aid
  - Healing covers often necessary
  - Wounds degrade much faster (2-3x normal rate per server tick)
  - Perfect thematic fit for "shadow" damage (rotting, decay, corruption)
- Visual: shadow/dark particles

**Poison Damage (Spider Venom Sac)**
- Use existing poison damage type (from Venom enchant)
- Already implemented in Wurm
- Standard poison DoT
- Visual: green/sickly particles

### Implementation Approach

When PowerScaling calculates damage, it will:

```java
// Get elemental damage from material infusions
Map<String, Float> elementalDamage = materialBonus.getElementalDamage();

for (Map.Entry<String, Float> entry : elementalDamage.entrySet()) {
    String damageType = entry.getKey();
    float damage = entry.getValue();

    switch (damageType) {
        case "fire":
            // Apply Flaming Aura enchant damage type
            applyEnchantDamage(defender, damage, ENCHANT_FLAMING_AURA);
            break;

        case "cold":
            // Apply Frostbrand enchant damage type
            applyEnchantDamage(defender, damage, ENCHANT_FROSTBRAND);
            break;

        case "shadow":
            // Apply Rotting Touch enchant damage type
            applyEnchantDamage(defender, damage, ENCHANT_ROTTING_TOUCH);
            break;

        case "poison":
            // Apply Venom enchant damage type
            applyEnchantDamage(defender, damage, ENCHANT_VENOM);
            break;
    }
}
```

### Benefits

✅ **No custom code needed** - Use existing Wurm systems
✅ **Already balanced** - Enchantment damage is tested/balanced
✅ **Healing mechanics work** - First aid, healing covers already handle these
✅ **Visual effects exist** - Particle effects already in client
✅ **Thematic fit** - Rotting Touch is perfect for "shadow/void" damage

### Material Bonuses (Updated)

**Ifrit Core:**
- +50 base damage
- +50 fire damage (Flaming Aura type)
- +10% attack speed
- Visual: flames

**Frozen Heart:**
- +50 base damage
- +50 cold damage (Frostbrand type)
- +10% crit chance
- Visual: ice

**Void Shard:**
- +45 base damage
- +40 shadow damage (Rotting Touch type - hard to heal!)
- +5% damage
- Visual: shadow
- **Special:** Wounds degrade 2-3x faster (Rotting Touch mechanic)

**Verdant Seed:**
- +40 base damage
- Lifesteal effect
- Visual: nature
- **Balances Void Shard:** Heals vs hard-to-heal damage

### Integration with Spellcraft

Since you already have Spellcraft mod (enchant stacking enabled):
- Materials grant enchant-type damage
- Can stack with actual enchants (3-4 damage enchants per weapon)
- Ifrit Core + Flaming Aura enchant = DOUBLE fire damage
- Void Shard + Rotting Touch enchant = EXTREME rot damage

### PowerScaling TODO

When implementing PowerScaling damage calculation:

1. Check if weapon has material infusions
2. Get elemental damage map from MaterialRegistry
3. Apply each damage type using Wurm's enchant damage system
4. Let Wurm handle healing difficulty, particle effects, etc.

**No custom damage systems needed - just hook into existing enchant code!**

---

**References:**
- Wurm enchantments: Flaming Aura, Frostbrand, Rotting Touch, Venom
- Spellcraft mod: Already configured for enchant stacking
- Material damage defined in MaterialSystemMod.java (elementalDamage map)
