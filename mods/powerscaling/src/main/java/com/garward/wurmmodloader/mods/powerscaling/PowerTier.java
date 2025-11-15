package com.garward.wurmmodloader.mods.powerscaling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Power tier system for categorizing creature and player power levels.
 *
 * <p>Tiers provide visual feedback and gameplay impact based on power thresholds:
 * <ul>
 *   <li><strong>Normal</strong> (0-499): Standard creatures, no special effects</li>
 *   <li><strong>Elite</strong> (500-999): Slightly stronger, yellow name suffix</li>
 *   <li><strong>Mythical</strong> (1000-1999): Significantly stronger, orange name suffix</li>
 *   <li><strong>Legendary</strong> (2000-4999): Very powerful, red name suffix</li>
 *   <li><strong>Godlike</strong> (5000+): Ultimate power, purple name suffix</li>
 * </ul>
 *
 * <p>Tiers are configurable via powerscaling.config and can be dynamically loaded
 * at runtime.</p>
 *
 * <p><strong>Design Note:</strong> This enum is used for BOTH creatures AND players.
 * Visual effects (size scaling, name suffixes) are creature-specific and handled
 * separately. Players can use this for tooltip display and progression tracking.</p>
 *
 * @author Power Fantasy RPG Team
 * @since 1.0.0
 */
public enum PowerTier {
    NORMAL(0, "Normal", "default"),
    ELITE(500, "Elite", "yellow"),
    MYTHICAL(1000, "Mythical", "orange"),
    LEGENDARY(2000, "Legendary", "red"),
    GODLIKE(5000, "Godlike", "purple");

    private final int threshold;
    private final String name;
    private final String colorCode;

    /**
     * Create a power tier.
     *
     * @param threshold The minimum power level for this tier
     * @param name The display name
     * @param colorCode The color code for visual feedback
     */
    PowerTier(int threshold, String name, String colorCode) {
        this.threshold = threshold;
        this.name = name;
        this.colorCode = colorCode;
    }

    /**
     * Get the minimum power level for this tier.
     *
     * @return The threshold
     */
    public int getThreshold() {
        return threshold;
    }

    /**
     * Get the display name.
     *
     * @return The tier name (e.g., "Elite", "Legendary")
     */
    public String getName() {
        return name;
    }

    /**
     * Get the color code for visual feedback.
     *
     * @return The color code (e.g., "yellow", "red", "purple")
     */
    public String getColorCode() {
        return colorCode;
    }

    /**
     * Get the tier for a given power level.
     *
     * <p>Returns the highest tier where power >= threshold.</p>
     *
     * @param power The power level
     * @return The corresponding tier
     */
    public static PowerTier fromPower(int power) {
        PowerTier result = NORMAL;
        for (PowerTier tier : values()) {
            if (power >= tier.threshold) {
                result = tier;
            }
        }
        return result;
    }

    /**
     * Check if this tier is higher than another.
     *
     * @param other The other tier
     * @return true if this tier has a higher threshold
     */
    public boolean isHigherThan(PowerTier other) {
        return this.threshold > other.threshold;
    }

    /**
     * Check if this tier is lower than another.
     *
     * @param other The other tier
     * @return true if this tier has a lower threshold
     */
    public boolean isLowerThan(PowerTier other) {
        return this.threshold < other.threshold;
    }

    /**
     * Get a descriptive power level message.
     *
     * <p>Used for tooltips and examine text.</p>
     *
     * @param power The actual power value
     * @param isPlayer true if this is for a player
     * @return A formatted message
     */
    public String getDescriptiveMessage(int power, boolean isPlayer) {
        if (isPlayer) {
            // Player-specific message
            switch (this) {
                case NORMAL:
                    return String.format("Power Level: %d (Just getting started)", power);
                case ELITE:
                    return String.format("Power Level: %d (%s Warrior)", power, name);
                case MYTHICAL:
                    return String.format("Power Level: %d (%s Hero)", power, name);
                case LEGENDARY:
                    return String.format("Power Level: %d (%s Champion)", power, name);
                case GODLIKE:
                    return String.format("Power Level: %d (%s Entity)", power, name);
                default:
                    return String.format("Power Level: %d", power);
            }
        } else {
            // Creature-specific message
            switch (this) {
                case NORMAL:
                    return String.format("A standard creature (Power: %d)", power);
                case ELITE:
                    return String.format("This creature radiates %s power! (Level: %d)", name, power);
                case MYTHICAL:
                    return String.format("This creature exudes %s power! (Level: %d)", name, power);
                case LEGENDARY:
                    return String.format("This creature emanates %s power! (Level: %d)", name, power);
                case GODLIKE:
                    return String.format("This creature transcends mortal power! (%s - Level: %d)", name, power);
                default:
                    return String.format("Power Level: %d", power);
            }
        }
    }

    /**
     * Get the model size multiplier for this tier (creature-specific).
     *
     * <p>Only applies to creatures. Players do not scale in size.</p>
     *
     * @param config The power scaling configuration
     * @return The size multiplier (1.0 = normal size)
     */
    public float getModelSizeMultiplier(PowerScalingConfig config) {
        if (!config.isModelScalingEnabled()) {
            return 1.0f;
        }

        // Progressive scaling: Normal=1.0, Elite=1.2, Mythical=1.4, Legendary=1.7, Godlike=2.0
        switch (this) {
            case NORMAL:
                return 1.0f;
            case ELITE:
                return 1.2f;
            case MYTHICAL:
                return 1.4f;
            case LEGENDARY:
                return 1.7f;
            case GODLIKE:
                return 2.0f;
            default:
                return 1.0f;
        }
    }

    @Override
    public String toString() {
        return String.format("%s (≥%d)", name, threshold);
    }
}
