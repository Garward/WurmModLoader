package com.garward.wurmmodloader.api.events.creature;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired when a creature is examined.
 *
 * <p>Allows mods to append custom text to creature examine descriptions.</p>
 *
 * @since 1.0.0
 */
public class CreatureExamineEvent extends Event {

    private final com.wurmonline.server.creatures.Creature creature;
    private String examineText;

    /**
     * Create a new CreatureExamineEvent.
     *
     * @param creature The creature being examined
     * @param examineText The original examine text
     */
    public CreatureExamineEvent(com.wurmonline.server.creatures.Creature creature, String examineText) {
        this.creature = creature;
        this.examineText = examineText;
    }

    /**
     * Get the creature being examined.
     *
     * @return The creature
     */
    public com.wurmonline.server.creatures.Creature getCreature() {
        return creature;
    }

    /**
     * Get the current examine text.
     *
     * @return The examine text (may have been modified by previous handlers)
     */
    public String getExamineText() {
        return examineText;
    }

    /**
     * Set the examine text.
     *
     * <p>Use this to append or modify the description shown to players.</p>
     *
     * @param examineText The new examine text
     */
    public void setExamineText(String examineText) {
        this.examineText = examineText;
    }

    /**
     * Append text to the examine description.
     *
     * @param text Text to append
     */
    public void appendText(String text) {
        this.examineText = this.examineText + text;
    }
}
