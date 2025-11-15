# SoulboundGear Mod - Design Document

**Version:** 1.0
**Status:** Design Phase
**Dependencies:** MaterialSystem (✅ Complete), Phase 4 Registry (✅ Complete)

---

## Table of Contents

1. [Overview](#overview)
2. [Configuration System](#configuration-system)
3. [Database Schema](#database-schema)
4. [Data Model](#data-model)
5. [Core Components](#core-components)
6. [XP and Leveling System](#xp-and-leveling-system)
7. [Binding System](#binding-system)
8. [Integration Points](#integration-points)
9. [Implementation Tasks](#implementation-tasks)

---

## Overview

### Purpose

SoulboundGear allows players to bind items to their soul, making them grow in power through XP and leveling.

### Key Features

- ✅ Bind items at deity altars
- ✅ Items gain XP from kills
- ✅ Level up (1-20) for upgrade points
- ✅ Cannot trade/drop soulbound items
- ✅ Batched database writes (CreatureStatusBatcher pattern)
- ✅ **Highly configurable** for easy balance tuning

### Design Goals

1. **Configurability:** Every number is a config value
2. **Performance:** Batched writes, in-memory cache
3. **Integration:** Clean API for UpgradeTree and PowerScaling
4. **Persistence:** No XP loss, survive server restarts
5. **Balance:** Easy to tune progression curves

---

## Configuration System

### Configuration File: `soulboundgear.config`

**All balance knobs in one place for easy tuning:**

```properties
# ═══════════════════════════════════════════════════════════════════════════
# SOULBOUND GEAR CONFIGURATION
# ═══════════════════════════════════════════════════════════════════════════

# ───────────────────────────────────────────────────────────────────────────
# BINDING SYSTEM
# ───────────────────────────────────────────────────────────────────────────

# Allow binding at altars only? (true = altar required, false = anywhere)
requireAltarForBinding=true

# Require deity alignment? (true = must match altar deity, false = any deity)
requireDeityAlignment=false

# Destroy item on player death? (true = perma-loss, false = keep on death)
destroyOnDeath=false

# Allow unbinding items? (true = can unbind, false = permanent)
allowUnbinding=false

# Cost to unbind (if allowed) in copper
unbindCost=100000

# Maximum soulbound items per player
maxSoulboundWeapons=1
maxSoulboundArmor=1

# ───────────────────────────────────────────────────────────────────────────
# LEVELING SYSTEM
# ───────────────────────────────────────────────────────────────────────────

# Maximum item level
maxItemLevel=20

# Base XP required for level 2 (scales exponentially)
baseXPRequirement=1000

# XP curve exponent (higher = steeper curve)
# Level N requires: baseXP * (N ^ xpCurveExponent)
xpCurveExponent=1.5

# Upgrade points granted per level
upgradePointsPerLevel=1

# ───────────────────────────────────────────────────────────────────────────
# XP GAIN FROM KILLS
# ───────────────────────────────────────────────────────────────────────────

# Base XP per kill (scaled by creature difficulty)
baseXPPerKill=10

# XP multiplier based on creature combat rating (CR)
# Formula: baseXP * (1 + creatureCR * crXPMultiplier)
crXPMultiplier=0.1

# Bonus XP for champion creatures
championXPMultiplier=5.0

# Bonus XP for unique creatures
uniqueXPMultiplier=10.0

# Bonus XP for titans
titanXPMultiplier=50.0

# XP gain radius (only kills within this radius count)
xpGainRadius=50.0

# Require wielding weapon to gain XP? (true = must be equipped, false = in inventory)
requireEquippedForXP=true

# ───────────────────────────────────────────────────────────────────────────
# LEVEL BONUSES
# ───────────────────────────────────────────────────────────────────────────

# Damage bonus per level (percentage)
damagePerLevel=0.02

# Attack speed bonus per level (percentage)
attackSpeedPerLevel=0.0

# Critical chance bonus per level (percentage)
critChancePerLevel=0.0

# Durability bonus per level (percentage)
durabilityPerLevel=0.01

# ───────────────────────────────────────────────────────────────────────────
# PERFORMANCE TUNING
# ───────────────────────────────────────────────────────────────────────────

# Batch write interval (milliseconds)
batchWriteInterval=100

# Force save interval (seconds) - periodic full save regardless of dirty state
forceSaveInterval=300

# Enable verbose logging? (true = debug logs, false = info only)
verboseLogging=false

# Log batching performance? (true = log batch sizes and timing, false = quiet)
logBatchingPerformance=false

# ───────────────────────────────────────────────────────────────────────────
# NOTIFICATIONS
# ───────────────────────────────────────────────────────────────────────────

# Show XP gain messages? (true = "Your sword gains 150 XP", false = silent)
showXPGainMessages=true

# Show level up messages? (true = "Your sword reached level 5!", false = silent)
showLevelUpMessages=true

# Show upgrade point messages? (true = "You gained 1 upgrade point", false = silent)
showUpgradePointMessages=true

# ───────────────────────────────────────────────────────────────────────────
# DATABASE SETTINGS
# ───────────────────────────────────────────────────────────────────────────

# Database file path (relative to server root)
databasePath=sqlite/soulboundgear.db

# Enable Write-Ahead Logging (WAL) mode? (true = better concurrency, false = default)
enableWAL=true

# Database busy timeout (milliseconds)
busyTimeout=5000
```

### Configuration Loading

```java
public class SoulboundConfig {
    // Binding
    public static boolean requireAltarForBinding;
    public static boolean requireDeityAlignment;
    public static boolean destroyOnDeath;
    public static boolean allowUnbinding;
    public static int unbindCost;
    public static int maxSoulboundWeapons;
    public static int maxSoulboundArmor;

    // Leveling
    public static int maxItemLevel;
    public static long baseXPRequirement;
    public static double xpCurveExponent;
    public static int upgradePointsPerLevel;

    // XP Gain
    public static int baseXPPerKill;
    public static double crXPMultiplier;
    public static double championXPMultiplier;
    public static double uniqueXPMultiplier;
    public static double titanXPMultiplier;
    public static double xpGainRadius;
    public static boolean requireEquippedForXP;

    // Bonuses
    public static double damagePerLevel;
    public static double attackSpeedPerLevel;
    public static double critChancePerLevel;
    public static double durabilityPerLevel;

    // Performance
    public static int batchWriteInterval;
    public static int forceSaveInterval;
    public static boolean verboseLogging;
    public static boolean logBatchingPerformance;

    // Notifications
    public static boolean showXPGainMessages;
    public static boolean showLevelUpMessages;
    public static boolean showUpgradePointMessages;

    // Database
    public static String databasePath;
    public static boolean enableWAL;
    public static int busyTimeout;

    public static void load(Properties properties) {
        // Load all config values with defaults
        requireAltarForBinding = Boolean.parseBoolean(
            properties.getProperty("requireAltarForBinding", "true"));

        requireDeityAlignment = Boolean.parseBoolean(
            properties.getProperty("requireDeityAlignment", "false"));

        // ... load all other values
    }
}
```

---

## Database Schema

### Table: `soulbound_items`

**Primary storage for soulbound item data:**

```sql
CREATE TABLE IF NOT EXISTS soulbound_items (
    -- Identity
    item_wurm_id BIGINT PRIMARY KEY,
    owner_wurm_id BIGINT NOT NULL,
    deity VARCHAR(16),

    -- Progression
    level INT DEFAULT 1,
    xp BIGINT DEFAULT 0,
    upgrade_points INT DEFAULT 0,

    -- Customization
    custom_name VARCHAR(255),
    lore TEXT,

    -- Complex data (JSON)
    allocated_nodes TEXT,      -- ["foundation", "sharpness", "blademaster"]
    infusions TEXT,            -- {"powerfantasy:ifrit_core": 1, "powerfantasy:wolf_fang": 15}

    -- Statistics
    kill_count INT DEFAULT 0,
    titan_kills INT DEFAULT 0,
    champion_kills INT DEFAULT 0,

    -- Timestamps
    created_timestamp BIGINT NOT NULL,
    last_modified BIGINT NOT NULL,

    -- Indices for queries
    INDEX idx_owner (owner_wurm_id),
    INDEX idx_deity (deity),
    INDEX idx_level (level)
);
```

### Table: `soulbound_xp_batch`

**Batching table for dirty tracking (CreatureStatusBatcher pattern):**

```sql
CREATE TABLE IF NOT EXISTS soulbound_xp_batch (
    item_wurm_id BIGINT PRIMARY KEY,

    -- Pending changes
    pending_xp BIGINT DEFAULT 0,
    pending_kill_count INT DEFAULT 0,
    pending_titan_kills INT DEFAULT 0,
    pending_champion_kills INT DEFAULT 0,

    -- Dirty flags (CSV of field names)
    dirty_fields TEXT,

    -- Timestamp
    last_updated BIGINT NOT NULL
);
```

### Batching Flow

```
1. Item gains XP → Mark dirty in memory
2. Every 100ms → Flush dirty items to soulbound_xp_batch
3. Every 1 second → Merge soulbound_xp_batch into soulbound_items
4. Clear batch table
```

**Benefits:**
- 10x fewer writes during combat
- No XP loss (batched writes persist pending changes)
- Proven pattern (CreatureStatusBatcher)

---

## Data Model

### SoulboundItem Class

```java
public class SoulboundItem {
    // ═══════════════════════════════════════════════════════════════════════
    // IDENTITY
    // ═══════════════════════════════════════════════════════════════════════

    private final long itemWurmId;
    private final long ownerWurmId;
    private String deity;  // "fo", "magranon", "vynora", "libila", null

    // ═══════════════════════════════════════════════════════════════════════
    // PROGRESSION
    // ═══════════════════════════════════════════════════════════════════════

    private int level;              // 1-20 (configurable via maxItemLevel)
    private long xp;                // Current XP
    private int upgradePoints;      // Unspent points

    // ═══════════════════════════════════════════════════════════════════════
    // CUSTOMIZATION
    // ═══════════════════════════════════════════════════════════════════════

    private String customName;      // "Flamereaver"
    private String lore;            // Player-written story

    // ═══════════════════════════════════════════════════════════════════════
    // UPGRADES & INFUSIONS
    // ═══════════════════════════════════════════════════════════════════════

    private Set<String> allocatedNodes;           // Upgrade tree nodes
    private Map<String, Integer> infusions;       // Material infusions
    // Key: "powerfantasy:ifrit_core", Value: stack count

    // ═══════════════════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════════════════

    private int killCount;
    private int titanKills;
    private int championKills;

    // ═══════════════════════════════════════════════════════════════════════
    // TIMESTAMPS
    // ═══════════════════════════════════════════════════════════════════════

    private long createdTimestamp;
    private long lastModified;

    // ═══════════════════════════════════════════════════════════════════════
    // CACHED BONUSES (recalculated on load/change)
    // ═══════════════════════════════════════════════════════════════════════

    private transient SoulboundBonuses cachedBonuses;

    // ═══════════════════════════════════════════════════════════════════════
    // METHODS
    // ═══════════════════════════════════════════════════════════════════════

    public void addXP(long amount) {
        this.xp += amount;
        checkLevelUp();
    }

    public void checkLevelUp() {
        while (level < SoulboundConfig.maxItemLevel &&
               xp >= getXPForNextLevel()) {
            level++;
            upgradePoints += SoulboundConfig.upgradePointsPerLevel;
            xp -= getXPForNextLevel();
            recalculateBonuses();
        }
    }

    public long getXPForNextLevel() {
        // Exponential curve: baseXP * (level ^ exponent)
        return (long) (SoulboundConfig.baseXPRequirement *
                      Math.pow(level, SoulboundConfig.xpCurveExponent));
    }

    public void recalculateBonuses() {
        this.cachedBonuses = new SoulboundBonuses(this);
    }

    // Getters/setters...
}
```

### SoulboundBonuses Class

```java
public class SoulboundBonuses {
    // From levels
    private float levelDamageBonus;
    private float levelSpeedBonus;
    private float levelCritBonus;
    private float levelDurabilityBonus;

    // From upgrade nodes (calculated by UpgradeTree mod)
    private float nodeDamageBonus;
    private float nodeSpeedBonus;
    private float nodeCritBonus;

    // From infusions (calculated via MaterialRegistry)
    private float infusionDamageBonus;
    private Map<String, Float> elementalDamage;

    // Total multipliers
    private float totalDamageMultiplier;
    private float totalSpeedMultiplier;
    private float totalCritChance;

    public SoulboundBonuses(SoulboundItem item) {
        calculateLevelBonuses(item);
        calculateNodeBonuses(item);
        calculateInfusionBonuses(item);
        calculateTotals();
    }

    private void calculateLevelBonuses(SoulboundItem item) {
        int level = item.getLevel();

        levelDamageBonus = level * (float) SoulboundConfig.damagePerLevel;
        levelSpeedBonus = level * (float) SoulboundConfig.attackSpeedPerLevel;
        levelCritBonus = level * (float) SoulboundConfig.critChancePerLevel;
        levelDurabilityBonus = level * (float) SoulboundConfig.durabilityPerLevel;
    }

    private void calculateNodeBonuses(SoulboundItem item) {
        // UpgradeTree mod will populate this
        // For now, default to 0
        nodeDamageBonus = 0.0f;
        nodeSpeedBonus = 0.0f;
        nodeCritBonus = 0.0f;
    }

    private void calculateInfusionBonuses(SoulboundItem item) {
        // Query MaterialRegistry for each infusion
        Map<String, Integer> infusions = item.getInfusions();
        float totalDamage = 0.0f;
        Map<String, Float> elemDmg = new HashMap<>();

        for (Map.Entry<String, Integer> entry : infusions.entrySet()) {
            ResourceLocation materialId = ResourceLocation.parse(entry.getKey());
            int stacks = entry.getValue();

            Optional<MaterialBonus> bonusOpt = MaterialRegistry.getBonus(materialId);
            if (bonusOpt.isPresent()) {
                MaterialBonus bonus = bonusOpt.get();

                // Add flat damage per stack
                totalDamage += bonus.getBaseDamage() * stacks;

                // Add elemental damage per stack
                for (Map.Entry<String, Float> elem : bonus.getElementalDamage().entrySet()) {
                    String type = elem.getKey();
                    float dmg = elem.getValue() * stacks;
                    elemDmg.put(type, elemDmg.getOrDefault(type, 0.0f) + dmg);
                }
            }
        }

        this.infusionDamageBonus = totalDamage;
        this.elementalDamage = elemDmg;
    }

    private void calculateTotals() {
        totalDamageMultiplier = 1.0f + levelDamageBonus + nodeDamageBonus;
        totalSpeedMultiplier = 1.0f + levelSpeedBonus + nodeSpeedBonus;
        totalCritChance = levelCritBonus + nodeCritBonus;
    }

    // Getters...
}
```

---

## Core Components

### SoulboundGearManager

**Thread-safe singleton managing all soulbound items:**

```java
public class SoulboundGearManager {
    private static final SoulboundGearManager INSTANCE = new SoulboundGearManager();

    // ═══════════════════════════════════════════════════════════════════════
    // IN-MEMORY CACHE
    // ═══════════════════════════════════════════════════════════════════════

    private final Map<Long, SoulboundItem> cache = new ConcurrentHashMap<>();

    // ═══════════════════════════════════════════════════════════════════════
    // DIRTY TRACKING (for batching)
    // ═══════════════════════════════════════════════════════════════════════

    private final Set<Long> dirtyItems = ConcurrentHashMap.newKeySet();

    // ═══════════════════════════════════════════════════════════════════════
    // BATCHING (CreatureStatusBatcher pattern)
    // ═══════════════════════════════════════════════════════════════════════

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor();

    private final ThreadLocal<Boolean> bypassFlag =
        ThreadLocal.withInitial(() -> false);

    // ═══════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════

    private SoulboundGearManager() {
        // Start batching scheduler
        scheduler.scheduleAtFixedRate(
            this::flushDirty,
            SoulboundConfig.batchWriteInterval,
            SoulboundConfig.batchWriteInterval,
            TimeUnit.MILLISECONDS
        );

        // Periodic force save
        scheduler.scheduleAtFixedRate(
            this::forceSaveAll,
            SoulboundConfig.forceSaveInterval,
            SoulboundConfig.forceSaveInterval,
            TimeUnit.SECONDS
        );
    }

    public static SoulboundGearManager getInstance() {
        return INSTANCE;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CORE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════

    public boolean bindItemToPlayer(Item item, Player player, String deity) {
        // Check if player can bind another item
        if (!canBindItem(player, item)) {
            return false;
        }

        // Create soulbound data
        SoulboundItem sb = new SoulboundItem(
            item.getWurmId(),
            player.getWurmId(),
            deity,
            System.currentTimeMillis()
        );

        // Store in cache
        cache.put(item.getWurmId(), sb);

        // Mark item as soulbound
        item.setData("soulbound", "true");
        item.setData("soulbound_owner", String.valueOf(player.getWurmId()));

        // Save to database
        SoulboundDAO.insert(sb);

        return true;
    }

    public void awardXP(Item item, long xp) {
        SoulboundItem sb = cache.get(item.getWurmId());
        if (sb == null) return;

        // Bypass check to prevent recursion
        if (bypassFlag.get()) return;

        sb.addXP(xp);
        markDirty(sb);

        if (SoulboundConfig.showXPGainMessages) {
            Player owner = Players.getInstance().getPlayer(sb.getOwnerWurmId());
            if (owner != null) {
                owner.getCommunicator().sendNormalServerMessage(
                    String.format("Your %s gains %d XP!", item.getName(), xp));
            }
        }
    }

    private void markDirty(SoulboundItem item) {
        dirtyItems.add(item.getItemWurmId());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BATCHING
    // ═══════════════════════════════════════════════════════════════════════

    private void flushDirty() {
        if (dirtyItems.isEmpty()) return;

        long startTime = System.currentTimeMillis();
        int count = dirtyItems.size();

        try {
            bypassFlag.set(true);

            List<SoulboundItem> toBatch = new ArrayList<>();
            for (Long itemId : dirtyItems) {
                SoulboundItem item = cache.get(itemId);
                if (item != null) {
                    toBatch.add(item);
                }
            }

            if (!toBatch.isEmpty()) {
                SoulboundDAO.batchUpdate(toBatch);
            }

            dirtyItems.clear();

        } finally {
            bypassFlag.set(false);
        }

        long elapsed = System.currentTimeMillis() - startTime;

        if (SoulboundConfig.logBatchingPerformance &&
            (count > 100 || elapsed > 50)) {
            logger.info(String.format(
                "Batched %d soulbound items in %dms", count, elapsed));
        }
    }

    private void forceSaveAll() {
        if (SoulboundConfig.verboseLogging) {
            logger.fine("Force saving all soulbound items");
        }

        for (SoulboundItem item : cache.values()) {
            markDirty(item);
        }

        flushDirty();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════════════════════════════════

    public SoulboundItem getSoulboundData(long itemWurmId) {
        return cache.get(itemWurmId);
    }

    public boolean isSoulbound(Item item) {
        return cache.containsKey(item.getWurmId());
    }

    public boolean isOwner(Item item, Player player) {
        SoulboundItem sb = cache.get(item.getWurmId());
        return sb != null && sb.getOwnerWurmId() == player.getWurmId();
    }

    public List<SoulboundItem> getPlayerSoulboundItems(long playerWurmId) {
        return cache.values().stream()
            .filter(sb -> sb.getOwnerWurmId() == playerWurmId)
            .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // VALIDATION
    // ═══════════════════════════════════════════════════════════════════════

    private boolean canBindItem(Player player, Item item) {
        // Check if item is a weapon (simplified check)
        if (!item.isWeapon()) {
            return false;
        }

        // Check max soulbound items
        long existingWeapons = getPlayerSoulboundItems(player.getWurmId())
            .stream()
            .filter(sb -> {
                try {
                    Item i = Items.getItem(sb.getItemWurmId());
                    return i.isWeapon();
                } catch (NoSuchItemException e) {
                    return false;
                }
            })
            .count();

        if (existingWeapons >= SoulboundConfig.maxSoulboundWeapons) {
            return false;
        }

        return true;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════

    public void loadAll() {
        logger.info("Loading all soulbound items from database...");

        List<SoulboundItem> items = SoulboundDAO.loadAll();

        for (SoulboundItem item : items) {
            cache.put(item.getItemWurmId(), item);
            item.recalculateBonuses();
        }

        logger.info("Loaded " + items.size() + " soulbound items");
    }

    public void shutdown() {
        logger.info("Shutting down SoulboundGearManager...");

        // Final save
        forceSaveAll();

        // Stop scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }

        logger.info("SoulboundGearManager shutdown complete");
    }
}
```

---

## XP and Leveling System

### XP Calculation

**Formula:** `baseXP * (1 + creatureCR * crMultiplier) * typeMultiplier`

```java
public static long calculateXPReward(Creature creature) {
    long baseXP = SoulboundConfig.baseXPPerKill;

    // Combat rating scaling
    float cr = creature.getCombatRating();
    double crBonus = 1.0 + (cr * SoulboundConfig.crXPMultiplier);

    // Type multipliers
    double typeMultiplier = 1.0;
    if (creature.isChampion()) {
        typeMultiplier = SoulboundConfig.championXPMultiplier;
    } else if (creature.isUnique()) {
        typeMultiplier = SoulboundConfig.uniqueXPMultiplier;
    } else if (isTitan(creature)) {
        typeMultiplier = SoulboundConfig.titanXPMultiplier;
    }

    return (long) (baseXP * crBonus * typeMultiplier);
}
```

### Level Curve (Default Config)

```
baseXP = 1000
exponent = 1.5

Level 1→2:   1,000 XP
Level 2→3:   2,828 XP
Level 3→4:   5,196 XP
Level 4→5:   8,000 XP
Level 5→6:  11,180 XP
...
Level 19→20: 84,525 XP

Total XP to max (level 20): ~500,000 XP
```

**Tuneable via config** - Change `baseXPRequirement` or `xpCurveExponent` to adjust!

---

## Binding System

### Bind Soul Action

```java
public class BindSoulAction implements ActionPerformer {
    @Override
    public boolean action(Action action, Creature performer, Item source,
                         Item target, short num, float counter) {

        if (!(performer instanceof Player)) {
            return true;
        }

        Player player = (Player) performer;

        // Check if at altar (if required)
        if (SoulboundConfig.requireAltarForBinding) {
            if (!isNearAltar(player)) {
                player.getCommunicator().sendNormalServerMessage(
                    "You must be at a deity altar to bind items to your soul.");
                return true;
            }
        }

        // Check deity alignment (if required)
        String deity = getAltarDeity(player);
        if (SoulboundConfig.requireDeityAlignment) {
            if (!player.getDeity().getName().equalsIgnoreCase(deity)) {
                player.getCommunicator().sendNormalServerMessage(
                    "You must worship " + deity + " to bind items at this altar.");
                return true;
            }
        }

        // Attempt binding
        boolean success = SoulboundGearManager.getInstance()
            .bindItemToPlayer(target, player, deity);

        if (success) {
            player.getCommunicator().sendNormalServerMessage(
                String.format("You bind the %s to your soul! It will grow with you.",
                             target.getName()));
        } else {
            player.getCommunicator().sendNormalServerMessage(
                "You cannot bind this item to your soul.");
        }

        return true;
    }
}
```

---

## Integration Points

### For UpgradeTree Mod

```java
// Query soulbound item
SoulboundItem item = SoulboundGearManager.getInstance()
    .getSoulboundData(itemWurmId);

// Check upgrade points
int points = item.getUpgradePoints();

// Allocate node
item.getAllocatedNodes().add("sharpness");
item.setUpgradePoints(points - 1);

// Recalculate bonuses
item.recalculateBonuses();

// Mark dirty
SoulboundGearManager.getInstance().markDirty(item);
```

### For PowerScaling Mod

```java
// Get bonuses
SoulboundItem item = SoulboundGearManager.getInstance()
    .getSoulboundData(weaponWurmId);

SoulboundBonuses bonuses = item.getCachedBonuses();

// Apply to damage
float damageMultiplier = bonuses.getTotalDamageMultiplier();
float finalDamage = baseDamage * damageMultiplier;

// Apply elemental damage
Map<String, Float> elemental = bonuses.getElementalDamage();
for (Map.Entry<String, Float> entry : elemental.entrySet()) {
    applyElementalDamage(defender, entry.getKey(), entry.getValue());
}
```

---

## Implementation Tasks

### Phase 1: Core Infrastructure (Days 1-2)

- [ ] Create SoulboundConfig class
- [ ] Create soulboundgear.config with all balance knobs
- [ ] Create database schema (soulbound_items, soulbound_xp_batch)
- [ ] Implement SoulboundItem class
- [ ] Implement SoulboundBonuses class
- [ ] Unit tests for XP curve calculations

### Phase 2: Manager & DAO (Days 3-4)

- [ ] Implement SoulboundGearManager with batching
- [ ] Implement SoulboundDAO (insert, update, batchUpdate, loadAll)
- [ ] Implement database initialization (WAL mode, indices)
- [ ] Test batching performance

### Phase 3: Binding System (Day 5)

- [ ] Implement BindSoulAction
- [ ] Hook item trade/drop to prevent transfer
- [ ] Implement altar detection
- [ ] Test binding in-game

### Phase 4: XP System (Day 6)

- [ ] Hook Creature.die() to award XP
- [ ] Implement XP calculation
- [ ] Implement level-up logic
- [ ] Test XP gain and leveling

### Phase 5: Testing & Polish (Day 7)

- [ ] Integration test with MaterialSystem
- [ ] Performance test (1000 items, batch timing)
- [ ] Balance test (XP curve feels good?)
- [ ] Documentation

---

## Success Criteria

✅ **Binding works** - Can bind items at altars
✅ **XP gain works** - Killing creatures awards XP
✅ **Leveling works** - Items level up at correct thresholds
✅ **Batching works** - <1ms average flush time for 100 items
✅ **Config works** - All balance knobs functional
✅ **No XP loss** - Survive server restarts
✅ **Integration ready** - Clean API for UpgradeTree/PowerScaling

---

**Estimated Time:** 1 week for complete SoulboundGear implementation

**Next Steps:** Implement Phase 1 (core infrastructure)
