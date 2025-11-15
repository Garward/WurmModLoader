package com.garward.wurmmodloader.api.events.item.material;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.items.Item;

/**
 * Fired when a generic material bonus (creation, anchor, pendulum, etc.) is requested.
 * Use {@link #getBonusType()} to identify the context and adjust the bonus accordingly.
 */
public class MaterialBonusEvent extends Event {

    public enum BonusType {
        CREATION,
        MOVEMENT,
        ANCHOR,
        PENDULUM,
        LOCKPICK,
        SPELL_POWER,
        ARMOUR,
        SHATTER,
        OTHER
    }

    private final Object context;
    private final byte material;
    private final BonusType bonusType;
    private final double baseBonus;
    private double bonus;

    public MaterialBonusEvent(Object context, byte material, BonusType bonusType, double baseBonus) {
        this.context = context;
        this.material = material;
        this.bonusType = bonusType;
        this.baseBonus = baseBonus;
        this.bonus = baseBonus;
    }

    /**
     * @return Context object for this bonus (may be an Item, Spell, Skill, etc.).
     */
    public Object getContext() {
        return context;
    }

    public byte getMaterial() {
        return material;
    }

    public BonusType getBonusType() {
        return bonusType;
    }

    public double getBaseBonus() {
        return baseBonus;
    }

    public double getBonus() {
        return bonus;
    }

    public void setBonus(double bonus) {
        this.bonus = bonus;
    }

    /**
     * Helper context for spell-power related material bonuses.
     */
    public static final class SpellContext {

        private final Item item;
        private final byte enchantment;

        public SpellContext(Item item, byte enchantment) {
            this.item = item;
            this.enchantment = enchantment;
        }

        public Item getItem() {
            return item;
        }

        public byte getEnchantment() {
            return enchantment;
        }
    }
}
