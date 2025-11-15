package com.garward.wurmmodloader.core.testing.eventsim;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.Items;
import com.wurmonline.server.players.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Validates event parameter integrity and object validity.
 *
 * <p>This validator inspects event instances to ensure:
 * <ul>
 *   <li>All required parameters are non-null (where applicable)</li>
 *   <li>Game objects (Creature, Player, Item) exist in their registries</li>
 *   <li>Object types match expected parameter types</li>
 *   <li>Primitive values are within valid ranges</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class EventIntegrityValidator {

    private static final Logger LOGGER = Logger.getLogger(EventIntegrityValidator.class.getName());

    /**
     * Validation result for an event.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;
        private final int nullCount;
        private final int validObjectCount;

        public ValidationResult(boolean valid, List<String> errors, List<String> warnings,
                              int nullCount, int validObjectCount) {
            this.valid = valid;
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
            this.nullCount = nullCount;
            this.validObjectCount = validObjectCount;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public int getNullCount() {
            return nullCount;
        }

        public int getValidObjectCount() {
            return validObjectCount;
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }

    /**
     * Validates an event's parameters and object integrity.
     *
     * @param event the event to validate
     * @return validation result with errors and warnings
     */
    public ValidationResult validate(Event event) {
        if (event == null) {
            List<String> errors = new ArrayList<>();
            errors.add("Event is null");
            return new ValidationResult(false, errors, new ArrayList<>(), 1, 0);
        }

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int nullCount = 0;
        int validObjectCount = 0;

        try {
            // Get all fields from the event class
            List<Field> fields = getAllFields(event.getClass());

            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(event);
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();

                // Skip synthetic and special fields
                if (field.isSynthetic() || fieldName.equals("cancellable") ||
                    fieldName.equals("cancelled")) {
                    continue;
                }

                // Check for null values
                if (value == null) {
                    nullCount++;
                    // Some fields are allowed to be null (like "killer" in CreatureDeathEvent)
                    if (fieldName.contains("killer") || fieldName.contains("optional")) {
                        warnings.add("Optional field '" + fieldName + "' is null");
                    } else {
                        warnings.add("Field '" + fieldName + "' is null");
                    }
                    continue;
                }

                // Validate Creature objects
                if (value instanceof Creature) {
                    Creature creature = (Creature) value;
                    if (!isValidCreature(creature)) {
                        errors.add("Creature '" + fieldName + "' (ID: " + creature.getWurmId() +
                                 ") is not in registry");
                    } else {
                        validObjectCount++;
                    }
                }

                // Validate Player objects
                else if (value instanceof Player) {
                    Player player = (Player) value;
                    if (!isValidPlayer(player)) {
                        errors.add("Player '" + fieldName + "' (ID: " + player.getWurmId() +
                                 ") is not in registry");
                    } else {
                        validObjectCount++;
                    }
                }

                // Validate Item objects
                else if (value instanceof Item) {
                    Item item = (Item) value;
                    if (!isValidItem(item)) {
                        errors.add("Item '" + fieldName + "' (ID: " + item.getWurmId() +
                                 ") is not in registry");
                    } else {
                        validObjectCount++;
                    }
                }

                // Validate primitive ranges
                else if (fieldType == float.class || fieldType == Float.class) {
                    float floatValue = (Float) value;
                    if (Float.isNaN(floatValue) || Float.isInfinite(floatValue)) {
                        errors.add("Float field '" + fieldName + "' has invalid value: " + floatValue);
                    }
                }
                else if (fieldType == double.class || fieldType == Double.class) {
                    double doubleValue = (Double) value;
                    if (Double.isNaN(doubleValue) || Double.isInfinite(doubleValue)) {
                        errors.add("Double field '" + fieldName + "' has invalid value: " + doubleValue);
                    }
                }
            }

        } catch (Exception e) {
            errors.add("Validation exception: " + e.getMessage());
            LOGGER.warning("Failed to validate event: " + e.getMessage());
        }

        boolean valid = errors.isEmpty();
        return new ValidationResult(valid, errors, warnings, nullCount, validObjectCount);
    }

    /**
     * Checks if a creature exists in the world registries.
     *
     * @param creature the creature to check
     * @return true if the creature is valid and in registry
     */
    private boolean isValidCreature(Creature creature) {
        if (creature == null) {
            return false;
        }

        try {
            long wurmId = creature.getWurmId();

            // Check Players registry
            if (creature.isPlayer()) {
                Player player = com.wurmonline.server.Players.getInstance().getPlayerOrNull(wurmId);
                return player != null;
            }

            // Check Creatures registry
            Creature found = Creatures.getInstance().getCreatureOrNull(wurmId);
            return found != null && found == creature;
        } catch (Exception e) {
            LOGGER.fine("Failed to validate creature: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a player exists in the Players registry.
     *
     * @param player the player to check
     * @return true if the player is valid and in registry
     */
    private boolean isValidPlayer(Player player) {
        if (player == null) {
            return false;
        }

        try {
            long wurmId = player.getWurmId();
            Player found = com.wurmonline.server.Players.getInstance().getPlayerOrNull(wurmId);
            return found != null && found == player;
        } catch (Exception e) {
            LOGGER.fine("Failed to validate player: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if an item exists in the Items registry.
     *
     * @param item the item to check
     * @return true if the item is valid and in registry
     */
    private boolean isValidItem(Item item) {
        if (item == null) {
            return false;
        }

        try {
            long wurmId = item.getWurmId();
            Item found = Items.getItem(wurmId);
            return found != null && found == item;
        } catch (Exception e) {
            // Item might be deleted or in transition
            LOGGER.fine("Failed to validate item: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets all fields from a class hierarchy.
     *
     * @param clazz the class to inspect
     * @return list of all fields including inherited ones
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                fields.add(field);
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }
}
