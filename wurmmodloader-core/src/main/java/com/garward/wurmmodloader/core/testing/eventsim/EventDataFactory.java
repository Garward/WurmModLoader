package com.garward.wurmmodloader.core.testing.eventsim;

import com.garward.wurmmodloader.core.eventlogic.PlayerUtil;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.Items;
import com.wurmonline.server.players.Player;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for generating valid test data from live server objects.
 *
 * <p>This factory provides access to real game entities (players, creatures, items)
 * for event simulation, ensuring that all test events use valid, existing objects
 * rather than null or mock data.
 *
 * <p><strong>Thread Safety:</strong> Thread-safe. Uses synchronized access to
 * Wurm's concurrent-safe registries.
 *
 * @since 1.0.0
 */
public class EventDataFactory {

    private static final Logger LOGGER = Logger.getLogger(EventDataFactory.class.getName());
    private final Random random = new Random();

    /**
     * Gets a random online player.
     *
     * @return a random player, or null if no players online
     */
    public Player getRandomPlayer() {
        try {
            Player[] players = PlayerUtil.getAllPlayers();
            if (players == null || players.length == 0) {
                LOGGER.fine("No players online for event testing");
                return null;
            }
            return players[random.nextInt(players.length)];
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get random player", e);
            return null;
        }
    }

    /**
     * Gets a random creature from the world.
     *
     * @return a random creature, or null if none available
     */
    public Creature getRandomCreature() {
        try {
            Creature[] creatures = Creatures.getInstance().getCreatures();
            if (creatures == null || creatures.length == 0) {
                LOGGER.fine("No creatures available for event testing");
                return null;
            }
            return creatures[random.nextInt(creatures.length)];
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get random creature", e);
            return null;
        }
    }

    /**
     * Gets a random non-player creature (NPC).
     *
     * @return a random NPC creature, or null if none available
     */
    public Creature getRandomNPC() {
        try {
            Creature[] creatures = Creatures.getInstance().getCreatures();
            if (creatures == null || creatures.length == 0) {
                return null;
            }

            // Try up to 10 times to find an NPC
            for (int i = 0; i < 10; i++) {
                Creature creature = creatures[random.nextInt(creatures.length)];
                if (creature != null && !creature.isPlayer()) {
                    return creature;
                }
            }

            LOGGER.fine("No NPC creatures found after 10 attempts");
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get random NPC", e);
            return null;
        }
    }

    /**
     * Gets a random champion creature, if any exist.
     *
     * @return a random champion creature, or null if none available
     */
    public Creature getRandomChampion() {
        try {
            Creature[] creatures = Creatures.getInstance().getCreatures();
            if (creatures == null || creatures.length == 0) {
                return null;
            }

            // Try up to 20 times to find a champion
            for (int i = 0; i < 20; i++) {
                Creature creature = creatures[random.nextInt(creatures.length)];
                if (creature != null && creature.isChampion()) {
                    return creature;
                }
            }

            LOGGER.fine("No champion creatures found");
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get random champion", e);
            return null;
        }
    }

    /**
     * Gets a random unique creature, if any exist.
     *
     * @return a random unique creature, or null if none available
     */
    public Creature getRandomUnique() {
        try {
            Creature[] creatures = Creatures.getInstance().getCreatures();
            if (creatures == null || creatures.length == 0) {
                return null;
            }

            // Try up to 20 times to find a unique
            for (int i = 0; i < 20; i++) {
                Creature creature = creatures[random.nextInt(creatures.length)];
                if (creature != null && creature.isUnique()) {
                    return creature;
                }
            }

            LOGGER.fine("No unique creatures found");
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get random unique", e);
            return null;
        }
    }

    /**
     * Gets a random item from the world.
     *
     * @return a random item, or null if none available
     */
    public Item getRandomItem() {
        try {
            Item[] items = Items.getAllItems();
            if (items == null || items.length == 0) {
                LOGGER.fine("No items available for event testing");
                return null;
            }
            return items[random.nextInt(items.length)];
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get random item", e);
            return null;
        }
    }

    /**
     * Gets a random player's inventory item.
     *
     * @param player the player whose inventory to search
     * @return a random item from player's inventory, or null if inventory is empty
     */
    public Item getRandomPlayerItem(Player player) {
        if (player == null) {
            return null;
        }

        try {
            Item inventory = player.getInventory();
            if (inventory == null) {
                return null;
            }

            Item[] items = inventory.getAllItems(false);
            if (items == null || items.length == 0) {
                return null;
            }

            return items[random.nextInt(items.length)];
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get random player item", e);
            return null;
        }
    }

    /**
     * Gets a random skill ID for testing.
     *
     * @return a skill ID (1-110 range for Wurm skills)
     */
    public int getRandomSkillId() {
        return 1 + random.nextInt(110);
    }

    /**
     * Gets a random damage amount for combat testing.
     *
     * @return damage amount (0.1 to 50.0)
     */
    public float getRandomDamage() {
        return 0.1f + random.nextFloat() * 49.9f;
    }

    /**
     * Gets a random skill check value.
     *
     * @return skill value (1.0 to 100.0)
     */
    public double getRandomSkillValue() {
        return 1.0 + random.nextDouble() * 99.0;
    }

    /**
     * Gets a test string for message events.
     *
     * @return a test message string
     */
    public String getTestMessage() {
        String[] messages = {
            "Test message",
            "Hello world",
            "/framework testEvents",
            "Event simulation test",
            "System check"
        };
        return messages[random.nextInt(messages.length)];
    }

    /**
     * Checks if there are any players online.
     *
     * @return true if at least one player is online
     */
    public boolean hasPlayersOnline() {
        try {
            Player[] players = PlayerUtil.getAllPlayers();
            return players != null && players.length > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if there are any creatures in the world.
     *
     * @return true if at least one creature exists
     */
    public boolean hasCreatures() {
        try {
            Creature[] creatures = Creatures.getInstance().getCreatures();
            return creatures != null && creatures.length > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if there are any items in the world.
     *
     * @return true if at least one item exists
     */
    public boolean hasItems() {
        try {
            Item[] items = Items.getAllItems();
            return items != null && items.length > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
