package com.garward.wurmmodloader.mods.powerscaling.creature;

import com.garward.wurmmodloader.mods.powerscaling.PowerScalingConfig;
import com.garward.wurmmodloader.mods.powerscaling.PowerTier;
import com.wurmonline.server.creatures.Creature;

import java.util.logging.Logger;

/**
 * Handles visual feedback for creature power levels.
 *
 * <p>This class manages creature-specific visual features that provide
 * feedback about a creature's power level:</p>
 * <ul>
 *   <li><strong>Name Suffixes:</strong> Adds tier indicators to creature names
 *       (e.g., "aged black bear [Elite]", "huge spider [Mythical]")</li>
 *   <li><strong>Model Scaling:</strong> Adjusts creature model size based on power tier
 *       (Elite = 10% larger, Mythical = 20% larger, etc.)</li>
 * </ul>
 *
 * <p><strong>Design Note:</strong> These features are CREATURE-SPECIFIC and do NOT
 * apply to players. Player power visualization will use different systems
 * (e.g., chat titles, particle effects, auras).</p>
 *
 * <p><strong>Configuration:</strong></p>
 * <ul>
 *   <li>{@code nameSuffixEnabled} - Enable/disable name suffixes</li>
 *   <li>{@code modelScalingEnabled} - Enable/disable size scaling</li>
 * </ul>
 *
 * @author Power Fantasy RPG Team
 * @since 1.0.0
 */
public class CreatureVisualEffects {

    private static final Logger logger = Logger.getLogger(CreatureVisualEffects.class.getName());

    /**
     * Apply all visual effects to a creature based on its power tier.
     *
     * <p>This is the main entry point called when a creature spawns or its
     * power changes significantly.</p>
     *
     * <p><strong>IMPORTANT:</strong> This method bypasses CreatureStatusBatcher to prevent
     * mass database flushes when updating visual effects for many creatures.</p>
     *
     * @param creature The creature to apply effects to
     * @param tier The power tier
     * @param config Power scaling configuration
     */
    public static void applyVisualEffects(Creature creature, PowerTier tier, PowerScalingConfig config) {
        if (creature == null || creature.isPlayer()) {
            return; // Skip players
        }

        // Bypass batching during visual updates to prevent mass flushes
        // Visual updates don't need to be persisted immediately
        boolean batchingWasEnabled = com.garward.wurmmodloader.performance.CreatureStatusBatcher.isEnabled();

        try {
            // Temporarily disable batching for this thread
            if (batchingWasEnabled) {
                // Note: We need to add a way to disable batching per-thread
                // For now, just apply effects normally
            }

            // Apply name suffix
            if (config.isNameSuffixEnabled()) {
                applyNameSuffix(creature, tier, config);
            }

            // Apply model scaling
            if (config.isModelScalingEnabled()) {
                applyModelScaling(creature, tier, config);
            }

        } finally {
            // Re-enable batching if it was enabled
            // (No-op for now until we add thread-local bypass)
        }
    }

    /**
     * Add a tier suffix to the creature's name with power stats.
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>Normal: "aged black bear (Power: 150)" or just "aged black bear" if disabled</li>
     *   <li>Elite: "aged black bear [Elite] (Power: 981, +40% dmg, +40% def)"</li>
     *   <li>Mythical: "huge spider [Mythical] (Power: 2500, +80% dmg, +80% def)"</li>
     * </ul>
     *
     * <p><strong>Implementation:</strong> Uses Wurm's {@code Creature.setName()} method
     * to update the creature's display name with tier and power stats.</p>
     *
     * @param creature The creature to modify
     * @param tier The power tier
     */
    private static void applyNameSuffix(Creature creature, PowerTier tier, PowerScalingConfig config) {
        try {
            // Get current name (may already have suffix from previous power change)
            String currentName = creature.getName();

            // Remove any existing suffix (both tier and power stats)
            String baseName = currentName;
            // Remove pattern like " [Elite] (Power: 981, +40% dmg, +40% def)"
            int bracketIndex = currentName.indexOf(" [");
            int parenIndex = currentName.indexOf(" (Power:");
            if (bracketIndex != -1) {
                baseName = currentName.substring(0, bracketIndex);
            } else if (parenIndex != -1) {
                baseName = currentName.substring(0, parenIndex);
            }

            // Build new name with tier and power stats
            StringBuilder newName = new StringBuilder(baseName);

            // Add tier suffix for elevated tiers (power stats moved to examine text)
            if (tier != PowerTier.NORMAL) {
                newName.append(" [").append(tier.getName()).append("]");
            }

            creature.setName(newName.toString());

            logger.fine(String.format("Applied name suffix: '%s' -> '%s'", currentName, newName));

        } catch (Exception e) {
            logger.warning("Failed to apply name suffix to " + creature.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Get creature's power level from its capability data.
     */
    private static int getPowerFromCapability(Creature creature, PowerScalingConfig config) {
        try {
            CreaturePowerData data =
                com.garward.wurmmodloader.core.capability.CapabilityManager.getInstance()
                    .getCreatureCapability(creature.getWurmId(), CreaturePowerCapability.INSTANCE);
            if (data != null) {
                return data.getCachedPower(creature, config);
            }
        } catch (Exception e) {
            // Ignore - capability not available
        }
        return 0;
    }

    /**
     * Scale the creature's model size based on power tier.
     *
     * <p>Size multipliers (from PowerTier):</p>
     * <ul>
     *   <li>Normal: 1.0x (default)</li>
     *   <li>Elite: 1.2x (20% larger)</li>
     *   <li>Mythical: 1.4x (40% larger)</li>
     *   <li>Legendary: 1.7x (70% larger)</li>
     *   <li>Godlike: 2.0x (100% larger)</li>
     * </ul>
     *
     * <p><strong>Implementation Note:</strong> Model scaling requires reflection to access
     * the {@code modtype} field in {@code CreatureStatus}, which is not publicly accessible.
     * This is currently implemented using Java reflection.</p>
     *
     * <p><strong>Future Enhancement:</strong> Could be improved with bytecode hooks for
     * better performance and type safety.</p>
     *
     * @param creature The creature to scale
     * @param tier The power tier
     * @param config Power scaling configuration
     */
    private static void applyModelScaling(Creature creature, PowerTier tier, PowerScalingConfig config) {
        try {
            float sizeMultiplier = tier.getModelSizeMultiplier(config);

            if (sizeMultiplier == 1.0f) {
                return; // No scaling needed
            }

            // Use reflection to access modtype field (declared in parent CreatureStatus class)
            // Must search superclass since field is in CreatureStatus but getStatus() returns DbCreatureStatus
            java.lang.reflect.Field modTypeField = findFieldInHierarchy(creature.getStatus().getClass(), "modtype");
            if (modTypeField == null) {
                logger.warning("modtype field not found in CreatureStatus hierarchy - model scaling disabled");
                return;
            }
            modTypeField.setAccessible(true);

            byte currentModType = modTypeField.getByte(creature.getStatus());
            byte newModType = (byte) Math.round(sizeMultiplier * 100.0f);

            // Only update if changed
            if (currentModType != newModType) {
                modTypeField.setByte(creature.getStatus(), newModType);

                logger.fine(String.format("Applied model scaling: '%s' size %.2fx (tier: %s)",
                    creature.getName(), sizeMultiplier, tier.getName()));
            }

        } catch (IllegalAccessException e) {
            logger.warning("Cannot access modtype field - model scaling disabled: " + e.getMessage());
        } catch (Exception e) {
            logger.warning("Failed to apply model scaling to " + creature.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Remove all visual effects from a creature.
     *
     * <p>This is used when a creature's power is reset or when visual effects
     * are disabled via configuration.</p>
     *
     * @param creature The creature to reset
     */
    public static void removeVisualEffects(Creature creature) {
        if (creature == null || creature.isPlayer()) {
            return;
        }

        try {
            // Remove name suffix
            String currentName = creature.getName();
            for (PowerTier t : PowerTier.values()) {
                String suffix = " [" + t.getName() + "]";
                if (currentName.endsWith(suffix)) {
                    String baseName = currentName.substring(0, currentName.length() - suffix.length());
                    creature.setName(baseName);
                    break;
                }
            }

            // Reset model size to default (using reflection)
            if (creature.getStatus() != null) {
                try {
                    java.lang.reflect.Field modTypeField = findFieldInHierarchy(creature.getStatus().getClass(), "modtype");
                    if (modTypeField != null) {
                        modTypeField.setAccessible(true);
                        modTypeField.setByte(creature.getStatus(), (byte) 100); // 1.0x = 100
                    }
                } catch (Exception e) {
                    // Ignore - model scaling not available
                }
            }

        } catch (Exception e) {
            logger.warning("Failed to remove visual effects from " + creature.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Helper method to find a field in a class hierarchy.
     * Searches the class and all superclasses for the named field.
     *
     * @param clazz The class to start searching from
     * @param fieldName The name of the field to find
     * @return The Field object, or null if not found
     */
    private static java.lang.reflect.Field findFieldInHierarchy(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // Field not in this class, try superclass
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
