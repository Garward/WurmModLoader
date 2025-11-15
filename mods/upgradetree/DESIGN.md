# Upgrade Tree Mod - Design Document

**Depends on:** PowerScaling mod

## Core Concept

Players can **spend their power levels** from PowerScaling to unlock permanent upgrades, creating a risk/reward mechanic where investing in the future temporarily weakens you.

---

## Game Loop

1. **Gain Power** - Kill creatures to earn power levels (PowerScaling)
2. **Get Temporary Bonuses** - Power levels provide scaling multipliers
3. **Strategic Decision** - Keep power for combat OR spend on permanent upgrades
4. **Investment Phase** - Spend power, become temporarily weaker
5. **Rebuilding Phase** - Regain power levels through more kills
6. **Compound Growth** - Now you have power bonuses PLUS permanent upgrades

---

## Integration with PowerScaling

### PowerScaling Provides:
- `getPlayerPowerLevel(long wurmId)` - Current power level
- `getDamageMultiplier(int power)` - 1.0x at 0 power, scales up
- `getDefenseMultiplier(int power)` - Same scaling
- `getHpMultiplier(int power)` - Same scaling

### UpgradeTree Adds:
- `spendPowerForUpgrade(long wurmId, int cost)` - Deduct power
- `hasUpgrade(long wurmId, String upgradeId)` - Check if unlocked
- Permanent effect application (crit chance, skill bonuses, etc.)

### Example Integration:
```java
// Player wants to buy "Critical Strike" (costs 20 power)
int currentPower = PowerScalingManager.getInstance().getPlayerPowerLevel(playerId);

if (currentPower >= 20) {
    // Spend the power
    PowerScalingManager.getInstance().modifyPowerLevel(playerId, -20);

    // Unlock the upgrade
    UpgradeTreeManager.getInstance().unlockUpgrade(playerId, "crit_strike");

    // Player now has:
    // - 20 less power (weaker multipliers temporarily)
    // - Permanent +15% crit chance

    player.getCommunicator().sendNormalServerMessage(
        "You spent 20 power to unlock Critical Strike! " +
        "Your power bonuses decreased, but you gained permanent +15% crit chance."
    );
}
```

---

## Upgrade Categories

### Combat Offensive
**Costs 15-30 power each**

- **Critical Strike** (20 power)
  - +15% critical hit chance
  - Requires: 50 total power earned (lifetime)

- **Armor Penetration** (25 power)
  - +10% armor penetration
  - Requires: Critical Strike

- **Execute** (30 power)
  - Deal +50% damage to targets below 20% HP
  - Requires: Critical Strike

### Combat Defensive
**Costs 15-30 power each**

- **Block Mastery** (20 power)
  - +15% block chance with shields

- **Damage Reduction** (25 power)
  - Take 10% less damage from all sources

- **Last Stand** (30 power)
  - When dropping below 20% HP, gain 50% damage reduction for 10 seconds (cooldown: 60s)

### Utility
**Costs 10-25 power each**

- **Efficient Learner** (15 power)
  - +10% skill gain rate

- **Harvester** (20 power)
  - +20% chance for extra resources when gathering

- **Fast Healer** (25 power)
  - Heal 2x faster when bandaged

### Ultimate Upgrades
**Costs 50+ power each**

- **Ascension** (100 power)
  - Permanently +25% to all stats
  - Requires: At least 5 other upgrades unlocked

---

## Upgrade Tree Structure

```
                    [Foundation]
                   (Free - Always)
                         |
        +----------------+----------------+
        |                |                |
   [OFFENSE]        [DEFENSE]        [UTILITY]
        |                |                |
    Tier 2:          Tier 2:          Tier 2:
  - Crit Strike    - Block Master   - Learner
  - Armor Pen      - Dmg Reduce     - Harvester
        |                |                |
    Tier 3:          Tier 3:          Tier 3:
  - Execute        - Last Stand     - Fast Heal
  - Berserk        - Fortress        - Veteran
        |                |                |
        +----------------+----------------+
                         |
                   [ASCENSION]
                  (Tier 5 Ultimate)
```

---

## UI Design (Radio + Pagination)

```
╔════════════════════════════════════════════════════╗
║        UPGRADE TREE - PAGE 1/4                     ║
╠════════════════════════════════════════════════════╣
║                                                    ║
║  Current Power: 45 (Available to spend)            ║
║  Total Power Earned: 127 (Lifetime)                ║
║  Upgrades Unlocked: 3                              ║
║                                                    ║
║  WARNING: Spending power reduces your combat       ║
║  bonuses temporarily. Regain power to restore!     ║
║                                                    ║
║  Select an upgrade to unlock:                      ║
║  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  ║
║                                                    ║
║  ○ Foundation (FREE - UNLOCKED ✓)                  ║
║    Base upgrade - Required for all paths          ║
║                                                    ║
║  ○ Critical Strike (Cost: 20 power)                ║
║    +15% critical hit chance (permanent)            ║
║    Requires: Foundation ✓                          ║
║    After purchase: Power drops to 25               ║
║    Your bonuses: 1.5x → 1.25x damage               ║
║                                                    ║
║  ○ Block Mastery (Cost: 20 power)                  ║
║    +15% block chance with shields                  ║
║    Requires: Foundation ✓                          ║
║                                                    ║
║  ○ Efficient Learner (Cost: 15 power)              ║
║    +10% skill gain rate (permanent)                ║
║    Requires: Foundation ✓                          ║
║                                                    ║
║  ○ Armor Penetration (Cost: 25 power - LOCKED)     ║
║    +10% armor penetration                          ║
║    Requires: Critical Strike ✗                     ║
║                                                    ║
║  [UNLOCK SELECTED]  [NEXT PAGE]  [PREV PAGE]       ║
║                                                    ║
╚════════════════════════════════════════════════════╝
```

---

## Database Schema

```sql
-- Existing PowerScaling table (already exists)
CREATE TABLE IF NOT EXISTS power_levels (
    player_id INTEGER PRIMARY KEY,
    total_power INTEGER DEFAULT 0,
    lifetime_power INTEGER DEFAULT 0  -- Never decreases, for unlock requirements
);

-- New UpgradeTree tables
CREATE TABLE IF NOT EXISTS player_upgrades (
    player_id INTEGER NOT NULL,
    upgrade_id TEXT NOT NULL,
    unlocked_at INTEGER,  -- Timestamp
    PRIMARY KEY (player_id, upgrade_id)
);

-- Track power expenditure for analytics
CREATE TABLE IF NOT EXISTS power_spending_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_id INTEGER NOT NULL,
    upgrade_id TEXT NOT NULL,
    power_spent INTEGER,
    power_before INTEGER,
    power_after INTEGER,
    timestamp INTEGER
);
```

---

## PowerScaling Modifications Needed

Add to `PowerScalingManager.java`:

```java
/**
 * Modify power level (can be negative for spending).
 * Updates both current power and lifetime total.
 */
public void modifyPowerLevel(long wurmId, int delta) {
    try (Connection conn = getConnection()) {
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE power_levels SET total_power = total_power + ?, " +
            "lifetime_power = lifetime_power + CASE WHEN ? > 0 THEN ? ELSE 0 END " +
            "WHERE player_id = ?"
        );
        ps.setInt(1, delta);
        ps.setInt(2, delta);
        ps.setInt(3, delta);
        ps.setLong(4, wurmId);
        ps.executeUpdate();
    } catch (SQLException e) {
        logger.log(Level.WARNING, "Failed to modify power level", e);
    }
}

/**
 * Get lifetime total power earned (never decreases, even when spending).
 */
public int getLifetimePowerEarned(long wurmId) {
    try (Connection conn = getConnection()) {
        PreparedStatement ps = conn.prepareStatement(
            "SELECT lifetime_power FROM power_levels WHERE player_id = ?"
        );
        ps.setLong(1, wurmId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("lifetime_power");
        }
    } catch (SQLException e) {
        logger.log(Level.WARNING, "Failed to get lifetime power", e);
    }
    return 0;
}
```

---

## Upgrade Effects Implementation

### Passive Effects (Always Active)
```java
// CriticalStrike upgrade
@SubscribeEvent
public void onCombatDamage(CombatDamageEvent event) {
    if (!event.isPlayerAttack()) return;

    if (UpgradeTreeManager.getInstance().hasUpgrade(event.getAttacker().getWurmId(), "crit_strike")) {
        // 15% chance to crit
        if (Server.rand.nextFloat() < 0.15f) {
            event.setDamage(event.getDamage() * 2.0);
            event.getAttacker().getCommunicator().sendCombatNormalMessage("Critical hit!");
        }
    }
}
```

### Skill Gain Modification
```java
// EfficientLearner upgrade
@SubscribeEvent
public void onSkillAdvance(SkillAdvanceEvent event) {
    if (UpgradeTreeManager.getInstance().hasUpgrade(event.getPlayer().getWurmId(), "efficient_learner")) {
        double bonus = event.getGain() * 0.10;  // +10%
        event.setGain(event.getGain() + bonus);
    }
}
```

---

## Balancing Considerations

### Power Costs
- **Low tier (15-20)**: Easy decisions, minor investments
- **Mid tier (25-35)**: Meaningful sacrifice, significant choices
- **High tier (50+)**: Major investment, requires planning
- **Ultimate (100+)**: End-game goal, requires dedication

### Upgrade Power
- Tier 2 upgrades: ~10-15% bonuses
- Tier 3 upgrades: ~20-25% bonuses
- Tier 4 upgrades: ~30-40% bonuses
- Ultimate: ~50%+ bonuses

### Power Rebuild Rate
At current rates:
- Kill low-tier creature: +1 power
- Kill mid-tier creature: +3-5 power
- Kill high-tier creature: +10-20 power

**Rebuild time after 20 power investment:**
- Casual player: 1-2 hours of hunting
- Active player: 30-60 minutes
- Power gamer: 15-30 minutes

---

## Strategic Depth Examples

### Example 1: Early Investment
**Player at 30 power:**
- Option A: Keep power, stay strong
  - Current: 1.3x damage
  - Safe but no growth
- Option B: Buy "Efficient Learner" (15 power)
  - Drop to 15 power (1.15x damage)
  - Gain +10% skill forever
  - Better long-term

### Example 2: Build Planning
**Player at 75 power:**
- Goal: Unlock "Ascension" (100 power, requires 5 upgrades)
- Strategy:
  1. Unlock 5 cheap upgrades (15-20 each) = 90 power spent
  2. Rebuild to 100 power
  3. Unlock Ascension
  4. Now has 6 permanent upgrades + weak power bonuses
  5. Rebuild to 100+ power again
  6. Final state: All upgrades + full power bonuses = GODMODE

### Example 3: Risk Management
**Player at 40 power, entering dangerous dungeon:**
- Smart: Don't spend, keep combat power
- Risky: Spend 30 on "Last Stand" before dungeon
  - Drop to 10 power (very weak!)
  - But Last Stand might save your life
  - High risk, high reward

---

## UI Flow

1. **Player opens body menu** → Clicks "Power Fantasy"
2. **Stats window shows:**
   - Current power: 45
   - Current bonuses: 1.45x damage, 1.3x defense, 1.2x HP
   - Unlocked upgrades: Critical Strike (✓), Efficient Learner (✓)
3. **Player clicks "Upgrade Tree"**
4. **Upgrade window opens** (page 1/4)
5. **Player selects "Block Mastery" (20 power)**
6. **Clicks "UNLOCK SELECTED"**
7. **Confirmation message:**
   - "Unlock Block Mastery for 20 power?"
   - "Your power will drop from 45 → 25"
   - "Bonuses will decrease temporarily"
   - "Upgrade is PERMANENT"
   - [YES] [NO]
8. **Player confirms:**
   - Power: 45 → 25
   - Bonuses: 1.45x → 1.25x damage, etc.
   - Message: "Unlocked Block Mastery! You now have +15% block chance permanently."
9. **Window refreshes** showing new state

---

## Future Enhancements

### Respec System
- Allow resetting upgrades for a cost
- Refund 50% of power spent
- Cooldown: Once per week

### Synergies
- Unlock both "Crit Strike" + "Execute" = Bonus synergy
- "Execute now also applies on critical hits"

### Prestige System
- After unlocking Ascension, option to "Prestige"
- Reset all upgrades, keep 10% of power
- Gain prestige points for exclusive upgrades

---

## Implementation Checklist

- [ ] Create UpgradeTreeManager singleton
- [ ] Create Upgrade class
- [ ] Create database schema
- [ ] Add modifyPowerLevel() to PowerScaling
- [ ] Add getLifetimePowerEarned() to PowerScaling
- [ ] Create UpgradeTreeQuestion UI (radio + pagination)
- [ ] Create ViewUpgradeTreeAction
- [ ] Register action in body menu
- [ ] Implement upgrade effects:
  - [ ] CriticalStrike (CombatDamageEvent)
  - [ ] EfficientLearner (SkillAdvanceEvent)
  - [ ] BlockMastery (CombatEvent)
  - [ ] DamageReduction (CombatDamageEvent)
- [ ] Add confirmation dialog before purchase
- [ ] Add power spending analytics
- [ ] Testing and balance

---

## Estimated Timeline

**Day 1: Backend**
- UpgradeTreeManager class
- Database schema
- PowerScaling integration
- Upgrade definitions

**Day 2: UI**
- UpgradeTreeQuestion implementation
- Radio button layout
- Pagination logic
- Confirmation dialog

**Day 3: Effects**
- Implement upgrade effects
- Event handlers
- Testing

**Day 4: Polish**
- Balance tuning
- UI refinements
- Bug fixes

**Total: 4 days** for complete implementation

---

## Risk/Reward Analysis

### Pros of This Design
✅ Creates meaningful strategic decisions
✅ Prevents infinite power creep (spending limits growth)
✅ Encourages diverse playstyles (different upgrade paths)
✅ Adds long-term progression goals
✅ Makes every kill matter (rebuilding power)

### Cons to Consider
⚠️ Players might hoard power (fear of spending)
⚠️ Temporary weakness might feel bad
⚠️ Complex for new players

### Mitigations
- Tutorial explaining the system
- Early cheap upgrades (low risk to try)
- Clear UI showing impact before purchase
- "Recommended" flag on good first upgrades

---

**Author:** Power Fantasy RPG Team
**Status:** Design Phase
**Dependencies:** PowerScaling mod (Phase 5+)
