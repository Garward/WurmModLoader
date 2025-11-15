# Healing System Design V2 - Respecting Wurm's Wound System

## Key Discovery: Wurm's Limb-Based Wounds

Wurm doesn't use simple HP bars. Each creature has:
- **Individual wounds per body part** (head, chest, arms, legs, stomach)
- **Wound severity** (damage amount per wound)
- **Wound types** (cut, bruise, burn, internal, bite, etc.)

You can't just heal "HP" - you must heal **individual wounds**.

**Example:**
```
Player HP: 500/1000 (50% health)
├─ Head: Light cut (2000 severity)
├─ Chest: Medium bruise (5000 severity)
├─ Left Arm: Serious cut (8000 severity)
└─ Right Leg: Light bite (1500 severity)

Total wound severity: 16,500
```

Healing works by:
1. Get all wounds: `creature.getBody().getWounds()`
2. Create "healing pool" (amount to distribute)
3. Heal wounds fully: `wound.heal()` or partially: `wound.modifySeverity(-amount)`

---

## Revised Healing System

Based on Spellcraft's implementation of Light of Fo (lines 173-243), here's the CORRECT approach:

### 1. Out of Combat Regeneration - "Sanctuary Aura" (Location-Based)

**Concept:** When out of combat, you're in a "sanctuary" state that periodically heals wounds. Healing speed depends on WHERE you are - villages are safe, wilderness is dangerous.

**Mechanics:**
- Combat ends: 10 seconds no damage taken/dealt
- Every **5 seconds** out of combat: Apply healing
- Base healing pool: **5000 + (PowerLevel × 50)** per tick
- **Location multiplier** (THE KEY MECHANIC):
  - **Inside your village:** **2.0x** healing (SAFE, encouraged to build home base)
  - **Near village (guard tower range):** **1.5x** healing (safer frontier)
  - **Wilderness (no village):** **1.0x** healing (standard, still playable)
  - **Enemy village territory:** **0.5x** healing (HOSTILE, high risk)
  - **Enemy deed:** **0.0x** NO REGEN (you're INVADING, git gud)

**Examples:**
```
Power 50 player in wilderness:
├─ Base: 7,500 healing per tick
├─ Multiplier: 1.0x (wilderness)
└─ Result: 7,500 healing per tick (~4 ticks to full heal = 20 seconds)

Power 50 player in own village:
├─ Base: 7,500 healing per tick
├─ Multiplier: 2.0x (village safety)
└─ Result: 15,000 healing per tick (~2 ticks to full heal = 10 seconds)

Power 50 player in enemy territory:
├─ Base: 7,500 healing per tick
├─ Multiplier: 0.5x (hostile)
└─ Result: 3,750 healing per tick (~8 ticks to full heal = 40 seconds)

Power 50 player on enemy deed:
├─ Base: 7,500 healing per tick
├─ Multiplier: 0.0x (NO REGEN)
└─ Result: 0 healing (need to escape to heal!)
```

**Why This is BRILLIANT:**
- **Encourages village building** (home base = power spike)
- **Creates strategic depth** (do I push deeper or retreat?)
- **Natural pacing mechanic** (dungeon crawl → retreat → heal → repeat)
- **Makes wilderness FEEL dangerous** (far from home = risky)
- **PvP implications** (raiding enemy territory is HIGH RISK)
- **Uses existing village system** (no new infrastructure needed)

**Future Enhancement - Healing Shrines/Altars:**
Could add buildable shrines that provide healing aura (like altar faith bonus):
- Healing Shrine (crafted item, placeable)
- Provides 1.5x multiplier within 20 tiles
- Stacks with village bonus (max 3.0x in village with shrine)
- Creates "forward camps" for deep wilderness exploration
- Requires resources to build (encourages economy)

**Implementation (based on Spellcraft code):**
```java
public static void applyOutOfCombatRegen(Creature player) {
    Wounds wounds = player.getBody().getWounds();
    if (wounds == null || wounds.getWounds().length == 0) {
        return; // No wounds to heal
    }

    int powerLevel = PowerScalingManager.getInstance().getPlayerPowerLevel(player.getWurmId());
    double baseHealingPool = 5000 + (powerLevel * 50); // Scales with power

    // LOCATION-BASED MULTIPLIER (THE KEY MECHANIC)
    float locationMultiplier = getLocationHealingMultiplier(player);
    double healingPool = baseHealingPool * locationMultiplier;

    if (healingPool <= 0) {
        return; // No healing in enemy territory
    }

    // Heal smallest wounds first (feels better)
    Wound[] woundArray = wounds.getWounds();
    Arrays.sort(woundArray, Comparator.comparingInt(Wound::getSeverity));

    for (Wound wound : woundArray) {
        if (healingPool <= 0) break;

        if (wound.getSeverity() <= healingPool) {
            // Fully heal this wound
            healingPool -= wound.getSeverity();
            wound.heal();
            player.getCommunicator().sendNormalServerMessage(
                "Your " + wound.getName() + " wound fully heals.", (byte) 2
            );
        } else {
            // Partially heal this wound
            wound.modifySeverity((int) -healingPool);
            player.getCommunicator().sendNormalServerMessage(
                "Your " + wound.getName() + " wound feels better.", (byte) 2
            );
            healingPool = 0;
        }
    }

    // Visual effect (same as FO heal)
    VolaTile tile = Zones.getTileOrNull(player.getTileX(), player.getTileY(), player.isOnSurface());
    if (tile != null) {
        tile.sendAttachCreatureEffect(player, (byte) 11, (byte) 0, (byte) 0, (byte) 0, (byte) 0);
    }
}

private static float getLocationHealingMultiplier(Creature player) {
    PowerScalingConfig config = PowerScalingConfig.getInstance();

    // Check if player is in a village
    Village playerVillage = player.getCitizenVillage();
    VolaTile currentTile = Zones.getTileOrNull(player.getTileX(), player.getTileY(), player.isOnSurface());

    if (currentTile == null) {
        return config.getWildernessHealingMultiplier(); // Default to wilderness
    }

    Village currentVillage = currentTile.getVillage();

    // No village at current location
    if (currentVillage == null) {
        // Check if near guard tower (within range of friendly village)
        if (playerVillage != null && playerVillage.isWithinGuardTowerRange(player.getTileX(), player.getTileY())) {
            return config.getGuardTowerHealingMultiplier(); // Near friendly guard tower
        }
        return config.getWildernessHealingMultiplier(); // Pure wilderness
    }

    // Inside a village - check relationship
    if (playerVillage != null && currentVillage.getId() == playerVillage.getId()) {
        // Inside YOUR village
        return config.getVillageHealingMultiplier();
    } else if (currentVillage.isEnemy(player)) {
        // Inside enemy village
        if (currentVillage.isOnDeed(player.getTileX(), player.getTileY())) {
            // On enemy deed - NO REGEN
            return 0.0f;
        } else {
            // Enemy village territory (not on deed)
            return config.getEnemyTerritoryHealingMultiplier();
        }
    } else {
        // Neutral/allied village
        return config.getWildernessHealingMultiplier(); // Treat as wilderness
    }
}
```

**Why 5000 base?**
- Light of Fo uses: `15000 + (power × 500)` for AoE healing
- Our single-target out-of-combat should be weaker: `5000 + (power × 50)`
- Still effective but not instant-full-heal (keeps tension)

---

### 2. Lifesteal on Kill - "Soul Drain"

**Concept:** Killing with soulbound weapon drains enemy life force to heal YOUR wounds.

**Mechanics:**
- Kill creature with soulbound weapon
- Healing pool: **5000 + (WeaponLevel × 500)**
- Instant heal on kill (mid-combat sustain)
- Heals largest wounds first (maximize combat effectiveness)
- Level 1 weapon: 5,500 healing
- Level 10 weapon: 10,000 healing
- Level 20 weapon: 15,000 healing (same as Light of Fo!)

**Implementation:**
```java
// In XPAwardHook.onCreatureDeath()
if (soulboundWeapon != null) {
    Wounds wounds = killer.getBody().getWounds();
    if (wounds != null && wounds.getWounds().length > 0) {
        int weaponLevel = soulboundItem.getLevel();
        double healingPool = 5000 + (weaponLevel * 500);

        // Heal LARGEST wounds first (maximize combat effectiveness)
        Wound[] woundArray = wounds.getWounds();
        Arrays.sort(woundArray, (a, b) -> Integer.compare(b.getSeverity(), a.getSeverity()));

        for (Wound wound : woundArray) {
            if (healingPool <= 0) break;

            if (wound.getSeverity() <= healingPool) {
                healingPool -= wound.getSeverity();
                wound.heal();
            } else {
                wound.modifySeverity((int) -healingPool);
                healingPool = 0;
            }
        }

        killer.getCommunicator().sendCombatNormalMessage(
            "Your weapon drains life force! (Wounds heal)"
        );
    }
}
```

**Why heal largest wounds first?**
- In combat, you want to reduce total damage taken
- Largest wounds = most dangerous
- Better survival during combat chains

---

### 3. Power Level Passive Regen - REMOVED

**Why removed:**
- User wants combat to stay "hectic and scary"
- Constant in-combat healing trivializes danger
- FO priest spells should remain valuable
- Out-of-combat regen + lifesteal is enough

**Alternative:** Passive regen only at VERY high power (100+) as a reward
- Power 100+: Small healing pool (1000) every 10 seconds
- Not enough to save you in combat
- Just enough to slowly recover if kiting

---

## Healing Resistance Integration

Wurm has **healing resistance** to prevent spam healing (Spellcraft lines 27-70).

**Do we need this?**

**For out-of-combat regen:** NO
- Only triggers when safe (no combat)
- Not spammable (5 second intervals)
- Players earned safety by disengaging

**For lifesteal:** MAYBE
- Could allow spam killing weak mobs for infinite healing
- Solution: Don't add resistance, but limit lifesteal to once per 2 seconds
- Prevents cheese, maintains balance

---

## Configuration Values

```properties
# ============================================================================
# HEALING SYSTEM - Wurm Wound-Based Healing
# ============================================================================

# Out of Combat Regeneration (Sanctuary Aura) - Location-Based
enableOutOfCombatRegen=true
outOfCombatDelay=10000                # 10 seconds no combat = sanctuary
outOfCombatRegenInterval=5000         # Heal every 5 seconds
outOfCombatBaseHealing=5000           # Base healing pool per tick
outOfCombatPowerScaling=50            # +50 healing per power level

# Location-Based Healing Multipliers (THE STRATEGIC LAYER)
villageHealingMultiplier=2.0          # 2x healing in YOUR village (SAFE)
guardTowerHealingMultiplier=1.5       # 1.5x healing near guard towers (safer)
wildernessHealingMultiplier=1.0       # 1x healing in wilderness (standard)
enemyTerritoryHealingMultiplier=0.5   # 0.5x healing in enemy village (RISKY)
enemyDeedHealingMultiplier=0.0        # 0x (NO REGEN) on enemy deeds (GIT GUD)

# Lifesteal on Kill (Soul Drain)
enableLifestealOnKill=true
lifestealBaseHealing=5000             # Base healing pool
lifestealPerLevel=500                 # +500 per weapon level (max 15000 at level 20)
lifestealCooldown=2000                # 2 second cooldown (prevent cheese)

# High Power Passive Regen (Level 100+ only)
enableHighPowerRegen=false            # Disabled by default (keeps combat scary)
highPowerRegenThreshold=100           # Only active at power 100+
highPowerRegenInterval=10000          # Every 10 seconds
highPowerRegenAmount=1000             # Tiny healing pool (1-2 small wounds)

# Visual Feedback
showHealingMessages=true              # Send wound healing messages
showHealingEffects=true               # Visual particle effects (byte 11 = green glow)
```

---

## Player Experience - Combat Flow

### Scenario 1: Fighting Near Home (Village Territory)

```
[At village border, exploring nearby forest]
Player fights troll → Takes 13,000 wound damage
Player kills troll → Lifesteal heals 10,000
Player backs off 10 seconds → Sanctuary activates
Location: Near guard tower → 1.5x healing multiplier
Tick 1 (5 sec): Heal 11,250 → Fully healed
[Ready for next fight - 15 seconds total]
```

**Result:** Safe, sustainable grinding near home.

---

### Scenario 2: Deep Wilderness Exploration

```
[Far from village, exploring dangerous territory]
Player fights champion → Takes 20,000 wound damage
Player kills champion → Lifesteal heals 10,000 (10k remaining)
Player backs off 10 seconds → Sanctuary activates
Location: Wilderness → 1.0x healing multiplier (no bonus)
Tick 1 (5 sec): Heal 7,500 (2.5k remaining)
Tick 2 (10 sec): Heal 7,500 → Fully healed
[Ready for next fight - 20 seconds total]
```

**Result:** Slower recovery, need to be more careful. Strategic decision: push deeper or retreat to village?

---

### Scenario 3: Raiding Enemy Territory (HIGH RISK)

```
[Raiding enemy village, trying to steal resources]
Player fights guard → Takes 15,000 wound damage
Player kills guard → Lifesteal heals 10,000 (5k remaining)
Player backs off 10 seconds → Sanctuary activates
Location: Enemy territory → 0.5x healing multiplier (HOSTILE)
Tick 1 (5 sec): Heal 3,750 (1.25k remaining)
Tick 2 (10 sec): Heal 3,750 → Fully healed (barely)
[Took 20 seconds - MORE GUARDS INCOMING]
```

**Result:** High risk raiding - healing is SLOW, need to extract quickly or die trying.

---

### Scenario 4: Invading Enemy Deed (HARDCORE MODE)

```
[On enemy deed, attempting to destroy something]
Player fights defender → Takes 10,000 wound damage
Player kills defender → Lifesteal heals 10,000 (saved by lifesteal)
Player backs off 10 seconds → Sanctuary attempts to activate
Location: Enemy deed → 0.0x healing multiplier (NO REGEN)
Tick 1 (5 sec): Heal 0 (NO HEALING AT ALL)
Tick 2 (10 sec): Heal 0 (STILL NO HEALING)
[Must escape enemy deed to heal OR die trying]
```

**Result:** HARDCORE raiding - you MUST win fast or get out. No sustain. GIT GUD.

---

**Gameplay Implications:**
- ✅ Combat is SCARY everywhere (full damage always)
- ✅ Village = Power fantasy (fast recovery, chain kills)
- ✅ Wilderness = Risky (slower recovery, need strategy)
- ✅ Enemy territory = HIGH RISK (minimal recovery, extract quickly)
- ✅ Enemy deed = HARDCORE (no recovery, win or die)
- ✅ **Strategic depth through location-based risk/reward**

---

## Special Cases

### 1. Death Wounds (CRITICAL/Fatal)
If wound severity exceeds certain thresholds, they might be "fatal" and need immediate attention.

**Solution:** Lifesteal prioritizes largest wounds (will catch fatal wounds first)

---

### 2. Poison/Disease Wounds
Some wounds have DoT (damage over time) effects.

**Solution:** Our healing REMOVES wounds (calls `wound.heal()`), which removes DoT effects too.

---

### 3. Multiple Small Wounds vs Few Large Wounds
Out-of-combat heals smallest first, lifesteal heals largest first.

**Why different strategies?**
- **Small first (out of combat):** Feels good to see wounds disappearing
- **Large first (in combat):** Keeps you alive (reduces most dangerous wounds)

---

## Implementation Priority

### Phase 1 - Out of Combat Regen (2-3 hours)

**Files to modify:**
1. `PowerScalingManager.java`
   - Add combat state tracking (lastCombatTime per player)
   - Add timer task (every 5 seconds)
   - Check if player out of combat → apply healing

2. `PowerScalingConfig.java`
   - Add healing config values

3. `PowerScalingMod.java`
   - Hook damage events to track combat state
   - Hook creature spawn to track combat state

**Code structure:**
```java
// In PowerScalingManager
private Map<Long, Long> lastCombatTime = new ConcurrentHashMap<>();
private Timer healingTimer;

private void startHealingTimer() {
    healingTimer = new Timer("PowerScaling-Healing", true);
    healingTimer.scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
            for (Player player : Players.getInstance().getPlayers()) {
                if (isOutOfCombat(player.getWurmId())) {
                    applyOutOfCombatHealing(player);
                }
            }
        }
    }, 5000, 5000); // Every 5 seconds
}

private boolean isOutOfCombat(long playerWurmId) {
    Long lastCombat = lastCombatTime.get(playerWurmId);
    if (lastCombat == null) return true;
    return System.currentTimeMillis() - lastCombat > config.getOutOfCombatDelay();
}

private void applyOutOfCombatHealing(Creature player) {
    // Implementation from above
}
```

---

### Phase 2 - Lifesteal on Kill (1 hour)

**Files to modify:**
1. `XPAwardHook.java`
   - After awarding XP, apply lifesteal healing
   - Use same wound healing logic

---

## Testing Checklist

- [ ] Out of combat regen starts after 10 seconds no damage
- [ ] Heals smallest wounds first (visible progression)
- [ ] Power scaling increases healing (test at power 1, 50, 100)
- [ ] Combat re-engagement stops healing immediately
- [ ] Lifesteal heals on kill (largest wounds first)
- [ ] Can chain 3-4 kills with lifesteal sustain
- [ ] High-damage enemies still SCARY (no god mode)
- [ ] Full recovery from near-death: 30-60 seconds
- [ ] Visual effects work (green glow on heal)
- [ ] Messages display correctly
- [ ] No server lag from heal ticks
- [ ] Titans/Uniques still dangerous (sustain < incoming damage)

---

## Balance Notes

**Early Game (Power 1-25):**
- Out-of-combat: 5000-6250 healing per 5 sec tick
- Lifesteal: 5500-10000 healing per kill
- **Feel:** Still fragile, need to be careful

**Mid Game (Power 25-50):**
- Out-of-combat: 6250-7500 healing per 5 sec tick
- Lifesteal: 10000-15000 healing per kill
- **Feel:** Can chain kills, sustainable grinding

**Late Game (Power 50-100):**
- Out-of-combat: 7500-10000 healing per 5 sec tick
- Lifesteal: 15000+ healing per kill (Light of Fo equivalent)
- **Feel:** Powerful but not invincible, Titans still wreck you

**Comparison to Vanilla:**
- FO priest: ~15000 healing every 30 seconds (cast time + favor regen)
- Our system: Similar potency but requires KILLING (offensive healing)
- Priests still valuable for: In-combat emergency heals, debuff removal, group utility

---

## Player Guide Update

### 🩹 Healing System - "Git Gud at Not Dying"

**The Problem:**
Wurm has limb-based wounds. You can have 5+ different injuries and bandaging takes FOREVER.

**The Solution:**
Out-of-combat healing that respects Wurm's wound system (not a cheap HP bar).

#### How It Works:

**1. Sanctuary Aura (Out of Combat) - LOCATION MATTERS!**
- Don't take damage for **10 seconds** → you're safe
- Every **5 seconds**: Heal wounds automatically
- Heals **smallest wounds first** (feels GOOD watching them disappear)
- **Healing speed depends on WHERE you are:**

**Inside Your Village:** 🏠
- 2.0x healing multiplier (FAST)
- Power 50: Full heal in ~10 seconds
- Safe, sustainable grinding
- **Encourages building a home base!**

**Near Guard Tower:** 🗼
- 1.5x healing multiplier (safer frontier)
- Power 50: Full heal in ~15 seconds
- Good for exploring near home

**Wilderness:** 🌲
- 1.0x healing multiplier (standard)
- Power 50: Full heal in ~20 seconds
- Slower, need to be more careful
- **Risk vs reward: push deeper or retreat?**

**Enemy Village Territory:** ⚔️
- 0.5x healing multiplier (RISKY)
- Power 50: Full heal in ~40 seconds
- High risk raiding
- **Need to extract quickly!**

**Enemy Deed:** 💀
- 0.0x healing multiplier (NO REGEN)
- Power 50: **ZERO HEALING**
- Hardcore mode - win fast or die
- **Lifesteal is your ONLY sustain**

**Visual:** Green glow, messages like "Your left arm wound fully heals."

**Strategic Implications:**
- Build villages for power spikes 📈
- Plan exploration routes (how far from home?) 🗺️
- Raiding is HIGH RISK (slow/no healing in enemy territory) ⚠️
- Creates natural "push → retreat → heal → push" rhythm 🔄

**2. Soul Drain (Lifesteal on Kill)**
- Kill enemy with soulbound weapon → instant healing
- Heals **largest wounds first** (keeps you alive in combat)
- Level 1 weapon: Heals 5,500 damage worth of wounds
- Level 10 weapon: Heals 10,000 damage
- Level 20 weapon: Heals 15,000 damage (same as FO priest spell!)

**Cooldown:** 2 seconds (can't cheese by killing chickens)

**Visual:** "Your weapon drains life force! (Wounds heal)"

#### Combat Flow Example:

```
Kill Troll #1 → Take 15,000 damage in wounds
Lifesteal heals 10,000 → 5,000 wounds remaining
Kill Troll #2 → Take 12,000 more damage
Lifesteal heals 10,000 → 7,000 wounds remaining
Kill Troll #3 → Take 10,000 more damage
Lifesteal heals 10,000 → 7,000 wounds remaining
Back off for 10 seconds → Sanctuary activates
2 healing ticks (10 seconds) → Fully healed
Ready for next pack → DOPAMINE ENGAGED
```

#### Key Rules:

✅ **Out of combat = FAST healing** (10-30 seconds full recovery)
✅ **In combat = Only lifesteal** (must KILL to sustain)
✅ **Combat stays scary** (you're not immortal)
✅ **High-level content still dangerous** (Titans hit HARD)
✅ **Grinding is sustainable** (no 10 minute bandage breaks)

**Pro Tip:** At max level with maxed weapon, you can chain-kill packs of 5-10 mobs before needing to back off. THIS is the power fantasy. 🔥

---

## Conclusion

This revised system:
✅ Respects Wurm's limb-based wound complexity
✅ Keeps combat SCARY (no in-combat god mode)
✅ Makes grinding sustainable (no 10 minute breaks)
✅ Scales with progression (power fantasy!)
✅ Based on proven Spellcraft healing code
✅ Doesn't trivialize priest healing (still valuable)
✅ **Location-based healing creates strategic depth** (THE GAME CHANGER)
✅ Encourages village building (home base = power spike)
✅ Natural exploration pacing (push → retreat → heal → repeat)
✅ High-risk PvP raiding (no healing in enemy territory)
✅ Uses existing Wurm systems (villages, guard towers, deeds)

**Priority:** CRITICAL - Implement Phase 1 immediately, Phase 2 when integrating with Soulbound.

**Why This is PERFECT for Power Fantasy:**
- Villages become meaningful (not just cosmetic)
- Exploration has real risk/reward (distance from home matters)
- PvP raiding is intense (no sustain in enemy territory)
- Creates strategic decisions every play session
- Fits thematically (your village is your sanctuary)

Let's make combat actually playable AND strategically interesting! 💪🏰
