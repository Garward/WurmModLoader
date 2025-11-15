package com.garward.wurmmodloader.api.events.skill;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.skills.Skill;

/**
 * Fired whenever a skill check is performed so mods can override difficulty or provide context modifiers.
 */
public class SkillCheckEvent extends Event {

    private final Skill skill;
    private final Creature performer;
    private final Item activeItem;
    private double difficulty;

    public SkillCheckEvent(Skill skill, Creature performer, Item activeItem, double difficulty) {
        this.skill = skill;
        this.performer = performer;
        this.activeItem = activeItem;
        this.difficulty = difficulty;
    }

    public Skill getSkill() {
        return skill;
    }

    public Creature getPerformer() {
        return performer;
    }

    public Item getActiveItem() {
        return activeItem;
    }

    public double getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(double difficulty) {
        this.difficulty = difficulty;
    }
}
