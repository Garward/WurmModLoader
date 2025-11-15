# Healing System Design - Power Fantasy Sustain

## The Problem

Wurm's healing is BRUTALLY slow:
- First aid requires high skill and time
- Priest healing needs alt/guild support
- Healing cover enchants are rare/expensive
- No potions, no instant heals, no regen

**Result:** Players kill 1-2 mobs → wait 10 minutes → repeat. This **KILLS** any motivation for combat grinding.

Our Power Fantasy mods **require** players to kill hundreds/thousands of mobs. We need sustainable combat.

---

## The Solution - Multi-Layered Healing

We'll implement **THREE** healing mechanics that stack together:

### 1. Out of Combat Regeneration (Phase 1 - IMMEDIATE)

**Concept:** When you're not in combat, you heal FAST.

**Mechanics:**
- Combat ends when no damage taken/dealt for **10 seconds**
- Out of combat: Regenerate **5% max HP per second**
- Full heal from 1 HP: **20 seconds** (compare to vanilla: 5+ minutes)
- Re-entering combat: Regen stops immediately

**Why This Works:**
- Eliminates downtime between fights
- Doesn't affect combat balance (only works OUT of combat)
- Standard in every modern RPG
- Easy to implement (tick check + heal)

**Configuration:**
```properties
# Out of Combat Healing
enableOutOfCombatRegen=true
outOfCombatDelay=10000           # 10 seconds no combat = regen starts
outOfCombatRegenRate=0.05        # 5% max HP per second
outOfCombatRegenTick=1000        # Check every 1 second
```

---

### 2. Lifesteal on Kill (Phase 2 - Soulbound Integration)

**Concept:** Killing enemies with soulbound weapons heals you.

**Mechanics:**
- Kill mob with soulbound weapon = heal **10% max HP**
- Scales with weapon level:
  - Level 1-5: 10% heal
  - Level 6-10: 15% heal
  - Level 11-15: 20% heal
  - Level 16-20: 25% heal
- Only works with soulbound weapons (encourages using the system)
- Instant heal on kill (feels GOOD)

**Why This Works:**
- Rewards aggressive play
- Fits power fantasy theme ("I drink your life force")
- Makes soulbound weapons even MORE valuable
- Creates sustain DURING combat (not just after)

**Configuration:**
```properties
# Lifesteal on Kill
enableLifestealOnKill=true
baseLifestealPercent=0.10        # 10% max HP
lifestealPerLevel=0.01           # +1% per weapon level (max 25% at level 15)
lifestealMaxPercent=0.25         # Cap at 25%
```

**Example:**
- Level 10 soulbound sword
- Kill champion goblin
- Instant heal: 10% + (10 × 1%) = **20% max HP**
- You can chain kills without stopping!

---

### 3. Power Level Passive Regen (Phase 2 - Power Scaling Integration)

**Concept:** Higher power level = better natural regeneration.

**Mechanics:**
- Base HP regen: **0.1% max HP per second** (vanilla Wurm level)
- Power scaling bonus: **+0.01% per power level**
- Power 50: 0.6% HP/sec (6x vanilla)
- Power 100: 1.1% HP/sec (11x vanilla)
- Works ALL the time (in combat + out of combat)
- Stacks with out-of-combat regen

**Why This Works:**
- Passive benefit to leveling up
- High-level players can tank more (endgame feel)
- Doesn't trivialize early game (low power = low regen)
- Natural progression curve

**Configuration:**
```properties
# Power Level Regen
enablePowerLevelRegen=true
baseRegenRate=0.001              # 0.1% max HP per second (baseline)
regenPerPowerLevel=0.0001        # +0.01% per power level
powerRegenTick=1000              # Check every 1 second
```

**Example Math:**
- Player has 1000 max HP
- Power Level 50
- Regen: 0.1% + (50 × 0.01%) = 0.6% per second
- Heals: 6 HP per second
- Full heal from 1 HP: **~3 minutes** (compare to vanilla: 10+ minutes)

---

## Combined System Example

**Scenario: Level 50 player, Power 50, Soulbound Sword Level 10**

**During Combat:**
- Power regen: 6 HP/sec (0.6% max HP/sec)
- Kill enemy: **Instant +20% max HP** (lifesteal)
- Can chain 3-4 kills before needing to back off

**After Combat (10 seconds of no damage):**
- Power regen: 6 HP/sec (still active)
- Out of combat regen: **50 HP/sec** (5% max HP/sec)
- Combined: **56 HP/sec**
- Full heal: **~18 seconds**

**Result:** Sustainable combat grind, minimal downtime, POWER FANTASY ENGAGED.

---

## Implementation Priority

### Phase 1 (Now) - Out of Combat Regen
**Files to modify:**
- `PowerScalingMod.java` - Add regen tick hook
- `PowerScalingConfig.java` - Add regen config values
- `PowerScalingManager.java` - Track combat state per player

**Hook Points:**
- `Creature.addAttacker()` - Track combat start
- `Creature.getCurrentCombatState()` - Check if in combat
- Timer task every 1 second - Apply regen if out of combat

**Estimated Work:** 1-2 hours

---

### Phase 2 (After Soulbound Integration) - Lifesteal + Power Regen
**Files to modify:**
- `SoulboundGearMod.java` - Add lifesteal on kill
- `XPAwardHook.java` - Heal player after XP award
- `PowerScalingManager.java` - Add passive power regen

**Hook Points:**
- `Creature.die()` - Heal killer if using soulbound weapon
- Existing power level tick - Add HP regen based on power

**Estimated Work:** 2-3 hours

---

## Configuration File Addition

Add to `powerscaling.config`:

```properties
# ============================================================================
# HEALING SYSTEM
# ============================================================================
# Solves Wurm's brutal healing problem to make combat grinding viable

# Out of Combat Regeneration
enableOutOfCombatRegen=true
outOfCombatDelay=10000           # 10 seconds no combat = regen starts
outOfCombatRegenRate=0.05        # 5% max HP per second (20 sec full heal)
outOfCombatRegenTick=1000        # Check every 1 second

# Lifesteal on Kill (requires soulbound weapon)
enableLifestealOnKill=true
baseLifestealPercent=0.10        # 10% max HP base
lifestealPerLevel=0.01           # +1% per weapon level
lifestealMaxPercent=0.25         # Cap at 25% (level 15+ weapons)

# Power Level Passive Regen
enablePowerLevelRegen=true
baseRegenRate=0.001              # 0.1% max HP per second (baseline)
regenPerPowerLevel=0.0001        # +0.01% per power level
powerRegenTick=1000              # Check every 1 second

# Combat State Tracking
combatTimeout=10000              # 10 seconds no damage = out of combat
```

---

## Player Experience Impact

### Before Healing System:
```
Kill goblin → 80% HP remaining
Kill troll → 30% HP remaining
Bandage for 5 minutes → 60% HP
Wait 5 more minutes → 90% HP
Continue grinding → ???
Give up, log out, play different game
```

### After Healing System:
```
Kill goblin → 80% HP remaining
Kill troll → 30% HP + 20% lifesteal = 50% HP remaining
Kill another goblin → 50% + 20% = 70% HP remaining
Back off for 10 seconds → Full HP (out of combat regen)
Continue chain → DOPAMINE ENGAGED
```

---

## Balance Considerations

**Q: Won't this make combat too easy?**

**A:** No, because:
1. Out of combat regen only works when SAFE (10 sec no damage)
2. Lifesteal requires WINNING fights (you need kills)
3. Power regen is weak early game (scales with progression)
4. High-tier mobs still hit HARD (Titans will humble you)

**Q: What about PvP?**

**A:** Power scaling is already disabled for PvP. We can also:
- Disable out of combat regen in PvP zones
- Reduce lifesteal effectiveness vs players
- Keep it PvE focused

**Q: Does this trivialize priest healing?**

**A:** No:
- Priests still have utility (buffs, ress, debuff removal)
- In-combat emergency healing still valuable
- Group content benefits from dedicated healers
- This just makes SOLO grinding viable

---

## Implementation Notes

### Combat State Tracking

Need to track per-player:
```java
class PlayerCombatState {
    long lastCombatTime;      // Timestamp of last damage taken/dealt
    boolean inCombat;          // Current combat state
    long lastRegenTick;        // Last regen application time

    boolean isInCombat() {
        long timeSinceCombat = System.currentTimeMillis() - lastCombatTime;
        return timeSinceCombat < COMBAT_TIMEOUT;
    }
}
```

Store in `PowerScalingManager` as `Map<Long, PlayerCombatState>`.

### Hook Points

**Track combat engagement:**
```java
// In Creature.addAttacker()
playerCombatState.lastCombatTime = System.currentTimeMillis();
playerCombatState.inCombat = true;
```

**Apply regen:**
```java
// In PowerScalingManager timer (every 1 second)
for (Player player : onlinePlayers) {
    if (!player.isInCombat()) {
        // Apply out of combat regen
        float healAmount = player.maxHP * outOfCombatRegenRate;
        player.heal(healAmount);
    }

    // Apply power level passive regen (always active)
    float powerRegen = player.maxHP * (baseRegenRate + powerLevel * regenPerPowerLevel);
    player.heal(powerRegen);
}
```

**Lifesteal on kill:**
```java
// In XPAwardHook.onCreatureDeath()
if (killerWeapon.isSoulbound()) {
    int weaponLevel = soulboundItem.getLevel();
    float lifestealPercent = baseLifestealPercent + (weaponLevel * lifestealPerLevel);
    lifestealPercent = Math.min(lifestealPercent, lifestealMaxPercent);

    float healAmount = killer.getMaxHP() * lifestealPercent;
    killer.heal(healAmount);

    killer.getCommunicator().sendCombatNormalMessage(
        "Your weapon drains life force! (+" + (int)healAmount + " HP)"
    );
}
```

---

## Visual Feedback (Future Enhancement)

When healing triggers, send player messages:

**Out of Combat Regen:**
- "You are no longer in combat. (Regenerating...)"
- Green HP bar glow effect

**Lifesteal:**
- "Your weapon drains life force! (+200 HP)"
- Red particle effect on kill

**Power Regen:**
- Silent (passive buff, no spam)
- Can see in character sheet: "Power Regen: +6 HP/sec"

---

## Testing Checklist

- [ ] Out of combat regen starts after 10 seconds
- [ ] Combat re-engagement stops out of combat regen
- [ ] Lifesteal heals correct % based on weapon level
- [ ] Power regen scales with power level
- [ ] Full heal from 1 HP takes ~20 seconds (out of combat)
- [ ] Can chain kill 3-4 mobs without stopping (with lifesteal)
- [ ] Titans still dangerous (high damage > regen)
- [ ] No server lag from regen ticks
- [ ] Messages display correctly
- [ ] Config values can be tweaked

---

## Player Guide Update

Add to PLAYER_GUIDE.md:

### 🩹 Healing System - "Why Am I Not Dead Yet?"

**The Problem (Vanilla Wurm):**
Kill 1 mob → bandage for 5 minutes → kill another → repeat → give up on life

**The Solution (Power Fantasy Mod):**
HEALING THAT DOESN'T SUCK

**Three Ways to Heal:**

1. **Out of Combat Regen** (The Basic One)
   - Don't take damage for 10 seconds
   - Heal **5% max HP per second**
   - Full heal: **20 seconds** (compare to vanilla: literal eternity)
   - "Just don't get hit for 10 seconds" - Sun Tzu, Art of War

2. **Lifesteal** (The Based One)
   - Kill stuff with soulbound weapon
   - Instant heal **10-25% max HP** (scales with weapon level)
   - Can chain kills like an absolute UNIT
   - "I drink your life force" energy

3. **Power Regen** (The Passive One)
   - Higher power level = better passive regen
   - Power 50: **6 HP/sec** (works in combat!)
   - Power 100: **11 HP/sec** (literally unkillable)
   - Scales forever (number go up simulator)

**Combined Power:**
- At Power 50 with Level 10 weapon:
- Kill mob → **+20% HP** (lifesteal)
- Take damage → **+6 HP/sec** (power regen)
- Back off 10 seconds → **+56 HP/sec** (everything stacked)
- Result: **INFINITE SUSTAIN**

**Pro Tip:** At high levels, you can face-tank packs of mobs and heal faster than they damage you. Congratulations, you're now the raid boss. 😎

---

## Conclusion

This healing system:
✅ Solves the sustain problem
✅ Enables combat grinding gameplay
✅ Scales with progression (power fantasy!)
✅ Doesn't trivialize combat (you can still die)
✅ Fits thematically (growing stronger = healing faster)
✅ Easy to implement (Phase 1 in 1-2 hours)

**Priority:** HIGH - Without this, the entire mod system has terrible UX.

Let's implement Phase 1 (out of combat regen) IMMEDIATELY, then add lifesteal/power regen in Phase 2.
