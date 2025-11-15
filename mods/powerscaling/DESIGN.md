# PowerScaling Mod - Design Document

## Overview

PowerScaling is the core combat progression system for Power Fantasy RPG. It provides exponential scaling for both players and creatures, enabling progression from weak starter to godlike endgame power.

**Key Philosophy:**
- **Players AND creatures** both have power levels
- Creatures scale to provide appropriate challenge
- Players must use soulbound gear + materials + upgrades to keep pace
- Forces innovation and progression through multiple systems

---

## Power Level System

### Player Power Levels (1-100+)

**Sources of Power:**
1. **Base Power** - Character level * multiplier (e.g., level 50 = 50 base power)
2. **Kill Power** - Accumulated from creature kills (diminishing returns)
3. **Achievement Power** - Bonus from completing achievements
4. **Quest Power** - Bonus from completing custom quests (future)

**Formula:**
```
PlayerPowerLevel = (CharacterLevel * basePowerMultiplier) +
                   (TotalKills * killPowerRate * diminishingFactor) +
                   AchievementBonuses +
                   QuestBonuses
```

**Diminishing Returns on Kills:**
```
diminishingFactor = 1.0 / (1.0 + (totalKills / killDiminishingThreshold))
```

### Creature Power Levels

**Base Assignment:**
- Spawned creatures get power level based on:
  - Creature CR (Combat Rating)
  - Creature type (champion/unique/titan multipliers)
  - Spawn location (future: zones have min/max power)

**Formula:**
```
CreaturePowerLevel = (CreatureCR * crPowerMultiplier) * typeMultiplier

typeMultiplier:
- Normal: 1.0x
- Champion: 2.0x
- Unique: 5.0x
- Titan: 10.0x
```

**Examples:**
- Troll (CR 10): Power Level 10
- Champion Troll (CR 10): Power Level 20
- Dragon (CR 50): Power Level 50
- Unique Dragon (CR 50): Power Level 250
- Titan Dragon (CR 50): Power Level 500

---

## Combat Scaling

### Damage Calculation

**Final Damage Formula:**
```java
// Base damage from weapon
float baseDamage = weapon.getDamage();

// Power level scaling
float attackerPowerMult = 1.0 + (attackerPower * damagePerPowerLevel);
float defenderPowerMult = 1.0 + (defenderPower * defensePerPowerLevel);

// Soulbound item bonuses (from SoulboundGear)
float soulboundDamageMult = soulboundBonus.getTotalDamageMultiplier();
float soulboundBaseDamage = soulboundBonus.getMaterialBaseDamage();

// Calculate final damage
float scaledDamage = (baseDamage + soulboundBaseDamage) *
                     attackerPowerMult *
                     soulboundDamageMult;

// Apply defender mitigation
float finalDamage = scaledDamage / defenderPowerMult;

// Add elemental damage (from material infusions)
finalDamage += applyElementalDamage(soulboundBonus);
```

### HP Scaling

**Formula:**
```
ScaledHP = BaseHP * (1.0 + (powerLevel * hpPerPowerLevel))
```

**Examples with hpPerPowerLevel = 0.05:**
- Troll (10 HP base, Power 10): 10 * 1.5 = 15 HP
- Champion Troll (10 HP base, Power 20): 10 * 2.0 = 20 HP
- Dragon (50 HP base, Power 50): 50 * 3.5 = 175 HP
- Titan Dragon (50 HP base, Power 500): 50 * 26.0 = 1,300 HP

### Critical Hits

**Formula:**
```
baseCritChance = 0.05  // 5% base
totalCritChance = baseCritChance + soulboundBonus.getTotalCritChance()

if (random() < totalCritChance) {
    damage *= critMultiplier;  // e.g., 2.0x
}
```

### Attack Speed

**Formula:**
```
baseAttackSpeed = weapon.getAttackSpeed();
attackSpeedMult = soulboundBonus.getTotalAttackSpeedMultiplier();
finalAttackSpeed = baseAttackSpeed * attackSpeedMult;
```

---

## Elemental Damage System

Integrates with MaterialSystem's damage types (fire, cold, shadow, poison).

**Implementation:**
```java
Map<String, Float> elementalDamage = soulboundBonus.getElementalDamage();

for (Map.Entry<String, Float> entry : elementalDamage.entrySet()) {
    String damageType = entry.getKey();  // "fire", "cold", "shadow", "poison"
    float damage = entry.getValue();

    switch (damageType) {
        case "fire":
            // Apply Flaming Aura enchant damage (see DAMAGE_TYPES.md)
            applyEnchantDamage(defender, damage, ENCHANT_FLAMING_AURA);
            break;

        case "cold":
            // Apply Frostbrand enchant damage
            applyEnchantDamage(defender, damage, ENCHANT_FROSTBRAND);
            break;

        case "shadow":
            // Apply Rotting Touch enchant damage (hard to heal!)
            applyEnchantDamage(defender, damage, ENCHANT_ROTTING_TOUCH);
            break;

        case "poison":
            // Apply Venom enchant damage
            applyEnchantDamage(defender, damage, ENCHANT_VENOM);
            break;
    }
}
```

**Benefits:**
- Leverages existing Wurm enchantment damage systems
- Healing difficulty already implemented (Rotting Touch wounds degrade 2-3x faster)
- Visual effects already in client (flames, frost, shadow particles)
- Stacks with actual enchantments if item also has Flaming Aura, etc.

---

## Data Storage

### Player Power Levels (Database)

**Table: player_power_levels**
```sql
CREATE TABLE player_power_levels (
    player_wurm_id INTEGER PRIMARY KEY,
    base_power INTEGER NOT NULL DEFAULT 0,
    kill_power_accumulated REAL NOT NULL DEFAULT 0.0,
    total_kills INTEGER NOT NULL DEFAULT 0,
    achievement_power INTEGER NOT NULL DEFAULT 0,
    quest_power INTEGER NOT NULL DEFAULT 0,
    last_updated INTEGER NOT NULL  -- Unix timestamp
);
```

### Creature Power Levels (In-Memory)

Creatures get power level assigned on spawn, stored in creature data:
```java
// On creature spawn
int powerLevel = calculateCreaturePowerLevel(creature);
creature.setData(POWER_LEVEL_KEY, powerLevel);

// In combat
int creaturePower = creature.getData(POWER_LEVEL_KEY);
```

---

## Configuration

**powerscaling.config:**
```properties
# PLAYER POWER SCALING
basePowerMultiplier=1.0          # CharacterLevel * this = base power
killPowerRate=0.01               # XP per kill toward power level
killDiminishingThreshold=1000    # Kills before diminishing returns kick in
maxPlayerPowerLevel=100          # Cap on player power level

# CREATURE POWER SCALING
crPowerMultiplier=1.0            # CreatureCR * this = base power
championPowerMultiplier=2.0      # Champions get 2x power
uniquePowerMultiplier=5.0        # Uniques get 5x power
titanPowerMultiplier=10.0        # Titans get 10x power

# COMBAT SCALING
damagePerPowerLevel=0.02         # +2% damage per power level
defensePerPowerLevel=0.01        # +1% defense per power level
hpPerPowerLevel=0.05             # +5% HP per power level

# CRITICAL HITS
baseCritChance=0.05              # 5% base crit chance
critMultiplier=2.0               # 2x damage on crit

# STATUS EFFECT
showPowerLevelStatusEffect=true  # Show "Power Level: X" status
statusEffectUpdateInterval=60000 # Update status every 60 seconds

# PERFORMANCE
cachePlayerPowerLevels=true      # Cache player power in memory
powerLevelCacheSize=1000         # Max cached entries
```

---

## Integration with Other Mods

### SoulboundGear Integration

**Read bonuses from equipped soulbound items:**
```java
Item weapon = player.getPrimWeapon();
if (weapon != null) {
    Optional<SoulboundItem> soulbound = SoulboundGearManager.getInstance().getItem(weapon.getWurmId());
    if (soulbound.isPresent()) {
        SoulboundBonuses bonuses = soulbound.get().getBonuses();

        // Apply damage multipliers
        damage *= bonuses.getTotalDamageMultiplier();

        // Apply base damage bonus
        damage += bonuses.getMaterialBaseDamage();

        // Apply elemental damage
        applyElementalDamage(defender, bonuses.getElementalDamage());

        // Apply crit chance
        critChance += bonuses.getTotalCritChance();

        // Apply attack speed
        attackSpeed *= bonuses.getTotalAttackSpeedMultiplier();
    }
}
```

### MaterialSystem Integration

**Elemental damage applied via SoulboundBonuses:**
- SoulboundBonuses already queries MaterialRegistry
- PowerScaling reads elemental damage from SoulboundBonuses
- No direct MaterialSystem dependency needed

### UpgradeTree Integration (Future)

**Read node bonuses:**
```java
// Future: When UpgradeTree is implemented
float nodeDamageBonus = bonuses.getNodeDamageMultiplier();
damage *= nodeDamageBonus;
```

---

## UI/UX

### Player Visibility

**Status Effect:**
- Single status effect: "Power Level: 42"
- Updates every 60 seconds (configurable)
- Color-coded by tier:
  - 1-25: White (Novice)
  - 26-50: Green (Adept)
  - 51-75: Blue (Expert)
  - 76-100: Purple (Master)
  - 100+: Gold (Legendary)

**Context Menu:**
- Right-click self → "Check Power Level"
- Shows detailed breakdown:
  ```
  ═══════════════════════════════════
  POWER LEVEL: 42
  ═══════════════════════════════════
  Base Power: 35 (from level 35)
  Kill Power: 5 (from 500 kills)
  Achievement Power: 2

  Combat Stats:
  Damage: +84% (42 * 2%)
  Defense: +42% (42 * 1%)
  HP: +210% (42 * 5%)

  Next Power Level: 850/1000 kills
  ═══════════════════════════════════
  ```

### Creature Visibility

**Examine Text:**
- Shows creature power level on examine
- "This champion troll radiates immense power. [Power Level: 20]"
- Color-coded by danger level relative to player

---

## Implementation Phases

### Phase 1: Core Infrastructure
- [x] Design document
- [ ] PowerScalingConfig class
- [ ] PowerLevel data model
- [ ] Database schema and DAO
- [ ] PowerScalingManager

### Phase 2: Player Power System
- [ ] Player power level calculation
- [ ] Kill tracking and diminishing returns
- [ ] Achievement power bonuses
- [ ] Status effect display

### Phase 3: Creature Power System
- [ ] Creature power level assignment on spawn
- [ ] Champion/unique/titan multipliers
- [ ] Store power level in creature data

### Phase 4: Combat Integration
- [ ] Hook damage calculation
- [ ] Apply power level scaling
- [ ] Apply soulbound bonuses
- [ ] Apply elemental damage
- [ ] Apply crit/attack speed

### Phase 5: Testing and Balance
- [ ] Test player progression curve
- [ ] Test creature difficulty scaling
- [ ] Balance configuration values
- [ ] Integration testing with SoulboundGear

---

## Balancing Considerations

**Early Game (Level 1-10):**
- Player power: 1-10
- Creature power: 1-10
- Progression feels normal, vanilla-like

**Mid Game (Level 25-50):**
- Player power: 25-50
- Creature power: 20-100 (champions/uniques appear)
- Power fantasy begins, soulbound items make big difference

**Late Game (Level 75-100):**
- Player power: 75-100+
- Creature power: 100-500 (titans appear)
- True power fantasy, require full optimization

**Exponential Feel:**
- Each 25 power levels doubles effective combat power
- Power 25 vs Power 50 creature = 2x harder
- Power 50 vs Power 100 creature = 2x harder again
- Forces continuous progression through all systems

---

## References

- SoulboundGear: `/mods/soulboundgear/`
- MaterialSystem: `/mods/materialsystem/`
- Damage Types: `/mods/materialsystem/DAMAGE_TYPES.md`
- CreatureStatusBatcher pattern: For batched database writes
