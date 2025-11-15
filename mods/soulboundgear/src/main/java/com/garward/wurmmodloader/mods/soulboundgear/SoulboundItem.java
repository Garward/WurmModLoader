package com.garward.wurmmodloader.mods.soulboundgear;

import com.garward.wurmmodloader.api.registry.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a soulbound item with progression data.
 *
 * <p>Mutable data model for soulbound gear. Each item tracks its bound player,
 * level, XP, upgrade tree nodes, material infusions, and statistics. Changes
 * are persisted to database via SoulboundGearManager.</p>
 *
 * <p><strong>Thread Safety:</strong> NOT thread-safe. Callers must synchronize
 * access or use SoulboundGearManager's locking.</p>
 *
 * @author Power Fantasy RPG Team
 * @version 1.0.0
 */
public class SoulboundItem {

    // ========================================================================
    // Identity
    // ========================================================================

    /** Wurm ID of the item (Item.wurmId) */
    private final long itemWurmId;

    /** Wurm ID of the player who bound this item (Creature.wurmId) */
    private long ownerWurmId;

    // ========================================================================
    // Progression
    // ========================================================================

    /** Current level (1 to maxItemLevel) */
    private int level;

    /** Current XP toward next level */
    private int currentXP;

    /** Total XP earned across all levels */
    private long totalXP;

    /** Unspent upgrade points from leveling */
    private int upgradePoints;

    // ========================================================================
    // Customization
    // ========================================================================

    /** Player-assigned custom name (nullable) */
    private String customName;

    /** Player-assigned custom description (nullable) */
    private String customDescription;

    // ========================================================================
    // Upgrades and Infusions
    // ========================================================================

    /**
     * List of upgrade tree node IDs (ResourceLocation strings).
     * Example: ["powerfantasy:crit_chance_1", "powerfantasy:lifesteal_path"]
     */
    private final List<String> upgradeTreeNodeIds;

    /**
     * Map of infused materials to stack counts.
     * Key: Material ResourceLocation string (e.g., "powerfantasy:ifrit_core")
     * Value: Stack count (1-N based on MaterialRegistry.getMaxStacks)
     */
    private final Map<String, Integer> materialInfusions;

    // ========================================================================
    // Statistics
    // ========================================================================

    /** Total creature kills with this item */
    private int kills;

    /** Times this item saved the player from death (future feature) */
    private int deathsSaved;

    /** Unix timestamp when item was first bound */
    private final long createdAt;

    /** Unix timestamp of last use (updated on kill, level, etc.) */
    private long lastUsedAt;

    // ========================================================================
    // Cached Bonuses
    // ========================================================================

    /**
     * Cached bonus calculations.
     * Recalculated when level, nodes, or infusions change.
     * Lazily computed on first access.
     */
    private transient SoulboundBonuses cachedBonuses;

    /**
     * Flag indicating cached bonuses are stale and need recalculation.
     */
    private transient boolean bonusesDirty = true;

    // ========================================================================
    // Constructor
    // ========================================================================

    /**
     * Create a new soulbound item.
     *
     * @param itemWurmId Wurm ID of the item
     * @param ownerWurmId Wurm ID of the owner
     * @param createdAt Unix timestamp of creation
     */
    public SoulboundItem(long itemWurmId, long ownerWurmId, long createdAt) {
        this.itemWurmId = itemWurmId;
        this.ownerWurmId = ownerWurmId;
        this.level = 1;
        this.currentXP = 0;
        this.totalXP = 0;
        this.upgradePoints = 0;
        this.customName = null;
        this.customDescription = null;
        this.upgradeTreeNodeIds = new ArrayList<>();
        this.materialInfusions = new HashMap<>();
        this.kills = 0;
        this.deathsSaved = 0;
        this.createdAt = createdAt;
        this.lastUsedAt = createdAt;
    }

    /**
     * Constructor for loading from database (includes all fields).
     *
     * @param itemWurmId Wurm ID of the item
     * @param ownerWurmId Wurm ID of the owner
     * @param level Current level
     * @param currentXP Current XP toward next level
     * @param totalXP Total XP earned
     * @param upgradePoints Unspent upgrade points
     * @param customName Custom name (nullable)
     * @param customDescription Custom description (nullable)
     * @param upgradeTreeNodeIds List of upgrade node IDs
     * @param materialInfusions Map of material infusions
     * @param kills Total kills
     * @param deathsSaved Deaths saved count
     * @param createdAt Creation timestamp
     * @param lastUsedAt Last use timestamp
     */
    public SoulboundItem(long itemWurmId, long ownerWurmId, int level, int currentXP,
                         long totalXP, int upgradePoints, String customName,
                         String customDescription, List<String> upgradeTreeNodeIds,
                         Map<String, Integer> materialInfusions, int kills,
                         int deathsSaved, long createdAt, long lastUsedAt) {
        this.itemWurmId = itemWurmId;
        this.ownerWurmId = ownerWurmId;
        this.level = level;
        this.currentXP = currentXP;
        this.totalXP = totalXP;
        this.upgradePoints = upgradePoints;
        this.customName = customName;
        this.customDescription = customDescription;
        this.upgradeTreeNodeIds = new ArrayList<>(upgradeTreeNodeIds);
        this.materialInfusions = new HashMap<>(materialInfusions);
        this.kills = kills;
        this.deathsSaved = deathsSaved;
        this.createdAt = createdAt;
        this.lastUsedAt = lastUsedAt;
    }

    // ========================================================================
    // Getters
    // ========================================================================

    public long getItemWurmId() { return itemWurmId; }
    public long getOwnerWurmId() { return ownerWurmId; }
    public int getLevel() { return level; }
    public int getCurrentXP() { return currentXP; }
    public long getTotalXP() { return totalXP; }
    public int getUpgradePoints() { return upgradePoints; }
    public String getCustomName() { return customName; }
    public String getCustomDescription() { return customDescription; }
    public List<String> getUpgradeTreeNodeIds() { return new ArrayList<>(upgradeTreeNodeIds); }
    public Map<String, Integer> getMaterialInfusions() { return new HashMap<>(materialInfusions); }
    public int getKills() { return kills; }
    public int getDeathsSaved() { return deathsSaved; }
    public long getCreatedAt() { return createdAt; }
    public long getLastUsedAt() { return lastUsedAt; }

    // ========================================================================
    // Setters (mark bonuses dirty when relevant fields change)
    // ========================================================================

    public void setOwnerWurmId(long ownerWurmId) {
        this.ownerWurmId = ownerWurmId;
    }

    public void setLevel(int level) {
        this.level = level;
        this.bonusesDirty = true;
    }

    public void setCurrentXP(int currentXP) {
        this.currentXP = currentXP;
    }

    public void setTotalXP(long totalXP) {
        this.totalXP = totalXP;
    }

    public void setUpgradePoints(int upgradePoints) {
        this.upgradePoints = upgradePoints;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public void setCustomDescription(String customDescription) {
        this.customDescription = customDescription;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public void setDeathsSaved(int deathsSaved) {
        this.deathsSaved = deathsSaved;
    }

    public void setLastUsedAt(long lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    // ========================================================================
    // Upgrade Tree Operations
    // ========================================================================

    /**
     * Add an upgrade tree node.
     *
     * @param nodeId Node ResourceLocation as string
     */
    public void addUpgradeNode(String nodeId) {
        if (!upgradeTreeNodeIds.contains(nodeId)) {
            upgradeTreeNodeIds.add(nodeId);
            bonusesDirty = true;
        }
    }

    /**
     * Remove an upgrade tree node (if respecs are enabled).
     *
     * @param nodeId Node ResourceLocation as string
     */
    public void removeUpgradeNode(String nodeId) {
        if (upgradeTreeNodeIds.remove(nodeId)) {
            bonusesDirty = true;
        }
    }

    /**
     * Check if item has a specific upgrade node.
     *
     * @param nodeId Node ResourceLocation as string
     * @return true if node is unlocked
     */
    public boolean hasUpgradeNode(String nodeId) {
        return upgradeTreeNodeIds.contains(nodeId);
    }

    // ========================================================================
    // Material Infusion Operations
    // ========================================================================

    /**
     * Add or increase material infusion stacks.
     *
     * @param materialId Material ResourceLocation as string
     * @param stacks Number of stacks to add
     */
    public void addMaterialInfusion(String materialId, int stacks) {
        int currentStacks = materialInfusions.getOrDefault(materialId, 0);
        materialInfusions.put(materialId, currentStacks + stacks);
        bonusesDirty = true;
    }

    /**
     * Set material infusion stacks (replaces existing).
     *
     * @param materialId Material ResourceLocation as string
     * @param stacks Stack count
     */
    public void setMaterialInfusion(String materialId, int stacks) {
        if (stacks <= 0) {
            materialInfusions.remove(materialId);
        } else {
            materialInfusions.put(materialId, stacks);
        }
        bonusesDirty = true;
    }

    /**
     * Remove material infusion entirely.
     *
     * @param materialId Material ResourceLocation as string
     */
    public void removeMaterialInfusion(String materialId) {
        if (materialInfusions.remove(materialId) != null) {
            bonusesDirty = true;
        }
    }

    /**
     * Get stack count for a material.
     *
     * @param materialId Material ResourceLocation as string
     * @return Stack count, or 0 if not infused
     */
    public int getMaterialInfusionStacks(String materialId) {
        return materialInfusions.getOrDefault(materialId, 0);
    }

    /**
     * Get total number of material infusion slots used.
     *
     * @return Number of distinct materials infused
     */
    public int getInfusionSlotCount() {
        return materialInfusions.size();
    }

    // ========================================================================
    // XP and Leveling
    // ========================================================================

    /**
     * Add XP and check for level up.
     *
     * @param xp XP amount to add
     * @return true if leveled up
     */
    public boolean addXP(int xp) {
        if (xp <= 0) {
            return false;
        }

        currentXP += xp;
        totalXP += xp;
        lastUsedAt = System.currentTimeMillis();

        // Check for level up
        SoulboundConfig config = SoulboundConfig.getInstance();
        int xpNeeded = config.getXPRequiredForLevel(level + 1);

        if (level >= config.getMaxItemLevel()) {
            // Max level reached
            return false;
        }

        if (currentXP >= xpNeeded) {
            // Level up!
            currentXP -= xpNeeded;
            level++;
            upgradePoints += config.getUpgradePointsPerLevel();
            bonusesDirty = true;
            return true;
        }

        return false;
    }

    /**
     * Increment kill counter.
     */
    public void incrementKills() {
        kills++;
        lastUsedAt = System.currentTimeMillis();
    }

    // ========================================================================
    // Bonus Calculation
    // ========================================================================

    /**
     * Get calculated bonuses for this item.
     * Bonuses are cached and only recalculated when dirty.
     *
     * @return Calculated bonuses
     */
    public SoulboundBonuses getBonuses() {
        if (bonusesDirty || cachedBonuses == null) {
            cachedBonuses = SoulboundBonuses.calculate(this);
            bonusesDirty = false;
        }
        return cachedBonuses;
    }

    /**
     * Force recalculation of bonuses on next access.
     */
    public void invalidateBonusCache() {
        bonusesDirty = true;
    }

    // ========================================================================
    // Utility
    // ========================================================================

    @Override
    public String toString() {
        return String.format("SoulboundItem{itemId=%d, owner=%d, level=%d, xp=%d/%d, kills=%d}",
                itemWurmId, ownerWurmId, level, currentXP,
                SoulboundConfig.getInstance().getXPRequiredForLevel(level + 1), kills);
    }
}
