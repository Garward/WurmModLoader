package com.garward.wurmmodloader.mods.oversizedclub.capability;

/**
 * ═════════════════════════════════════════════════════════════════════════
 * TUTORIAL: Capability Data Class - Your Custom Data Structure
 * ═════════════════════════════════════════════════════════════════════════
 *
 * <p><b>What is This?</b></p>
 * This is a simple POJO (Plain Old Java Object) that holds your custom data.
 * Think of it like a database row, but managed automatically by the framework.
 *
 * <p><b>Best Practices:</b></p>
 * <ul>
 *   <li>Keep it simple - just fields, getters, setters, and helper methods</li>
 *   <li>Make fields private with public getters/setters</li>
 *   <li>Add helper methods for common operations (like addExperience here)</li>
 *   <li>Don't store references to Wurm objects (can't serialize)</li>
 *   <li>Use primitives or String/wrappers that can be easily serialized</li>
 * </ul>
 *
 * <p><b>Example Data Structures You Could Create:</b></p>
 * <pre>
 * // Player RPG stats
 * public class PlayerStats {
 *     private int strength, dexterity, intelligence;
 *     private int skillPoints;
 * }
 *
 * // Soulbound item data
 * public class SoulboundData {
 *     private long ownerPlayerId;
 *     private boolean canTrade, canDrop;
 * }
 *
 * // Quest progress
 * public class QuestProgress {
 *     private Map<String, Integer> completedQuests;
 *     private String currentQuest;
 * }
 * </pre>
 *
 * @author WurmModLoader Team
 * @since 1.0.0 (Phase 5.5)
 * @see ItemLevelCapability The capability that manages this data
 */
public class ItemLevel {
    // ═════════════════════════════════════════════════════════════════════
    // Fields - Your custom data
    // ═════════════════════════════════════════════════════════════════════
    private int level;       // Current level (1-100)
    private int experience;  // XP towards next level

    // ═════════════════════════════════════════════════════════════════════
    // Constructors
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Default constructor - used by createDefaultInstance()
     * New items start at level 1 with 0 XP
     */
    public ItemLevel() {
        this.level = 1;
        this.experience = 0;
    }

    /**
     * Constructor with values - used by deserialize()
     * When loading from database, restore saved level and XP
     */
    public ItemLevel(int level, int experience) {
        this.level = level;
        this.experience = experience;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Getters and Setters - Standard Java bean pattern
    // ═════════════════════════════════════════════════════════════════════

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Helper Methods - Business Logic
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Add experience and automatically level up if threshold reached.
     *
     * <p><b>Leveling Formula:</b></p>
     * - Level 1 → 2: 100 XP
     * - Level 2 → 3: 200 XP
     * - Level 3 → 4: 300 XP
     * - etc. (100 * current_level)
     *
     * <p>When leveling up, excess XP carries over to next level.</p>
     *
     * @param exp Experience points to add (can be negative for penalties)
     * @return true if leveled up, false otherwise
     */
    public boolean addExperience(int exp) {
        this.experience += exp;

        // Calculate XP needed for next level (100 * current level)
        int expNeeded = 100 * level;

        // Check if we have enough XP to level up
        if (this.experience >= expNeeded) {
            // Subtract XP cost and increase level
            this.experience -= expNeeded;
            this.level++;
            return true; // Leveled up!
        }

        return false; // Not enough XP yet
    }

    /**
     * Returns a formatted string for display (used in examine text).
     *
     * <p>Example output: "Level 5 (XP: 250/500)"</p>
     *
     * @return Human-readable representation
     */
    @Override
    public String toString() {
        int expNeeded = level * 100;
        return "Level " + level + " (XP: " + experience + "/" + expNeeded + ")";
    }
}
