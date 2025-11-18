package com.garward.wurmmodloader.api.events.skill;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired when a skill check difficulty is calculated.
 * Allows modification of action difficulty before the skill check.
 *
 * Use cases:
 * - Efficiency spell: Reduce difficulty based on tool enchantment
 * - Industry spell: Reduce difficulty based on jewelry enchantment
 * - Difficulty modifiers for various systems
 */
public class SkillDifficultyEvent extends Event {
    private final long performerId;
    private final String performerName;
    private final int skillId;
    private final String skillName;
    private final long toolId;
    private final String toolName;
    private final double originalDifficulty;
    private double modifiedDifficulty;

    public SkillDifficultyEvent(long performerId, String performerName,
                                int skillId, String skillName,
                                long toolId, String toolName,
                                double difficulty) {
        this.performerId = performerId;
        this.performerName = performerName;
        this.skillId = skillId;
        this.skillName = skillName;
        this.toolId = toolId;
        this.toolName = toolName;
        this.originalDifficulty = difficulty;
        this.modifiedDifficulty = difficulty;
    }

    /**
     * Get the performer's unique ID.
     */
    public long getPerformerId() {
        return performerId;
    }

    /**
     * Get the performer's name.
     */
    public String getPerformerName() {
        return performerName;
    }

    /**
     * Get the skill ID being checked.
     */
    public int getSkillId() {
        return skillId;
    }

    /**
     * Get the skill name.
     */
    public String getSkillName() {
        return skillName;
    }

    /**
     * Get the tool item ID (or -1 if no tool).
     */
    public long getToolId() {
        return toolId;
    }

    /**
     * Get the tool name (or null if no tool).
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * Get the original difficulty before modifications.
     */
    public double getOriginalDifficulty() {
        return originalDifficulty;
    }

    /**
     * Get the current modified difficulty.
     */
    public double getModifiedDifficulty() {
        return modifiedDifficulty;
    }

    /**
     * Set the modified difficulty.
     */
    public void setModifiedDifficulty(double difficulty) {
        this.modifiedDifficulty = Math.max(0, difficulty);
    }

    /**
     * Multiply difficulty by a factor (e.g., 0.8 for 20% reduction).
     */
    public void multiplyDifficulty(double multiplier) {
        this.modifiedDifficulty = Math.max(0, this.modifiedDifficulty * multiplier);
    }

    /**
     * Reduce difficulty by a flat amount.
     */
    public void reduceDifficulty(double reduction) {
        this.modifiedDifficulty = Math.max(0, this.modifiedDifficulty - reduction);
    }
}
