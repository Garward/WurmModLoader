package com.garward.wurmmodloader.core.eventlogic;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.players.Player;
import com.wurmonline.shared.util.MulticolorLineSegment;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Framework utility for player communication and registry access.
 *
 * <p>This utility provides safe, framework-level access to send messages to players
 * and access the player registry without requiring mods to import Wurm classes directly.</p>
 *
 * <p><strong>Thread Safety:</strong> Thread-safe. All methods are stateless.</p>
 *
 * @since 1.0.0
 */
public final class PlayerUtil {

    private static final Logger LOGGER = Logger.getLogger(PlayerUtil.class.getName());

    private PlayerUtil() {
        // Utility class
    }

    /**
     * Send a combat message to a player.
     *
     * <p>Combat messages appear in the combat tab and are typically orange/red colored.</p>
     *
     * @param player The player (must be a player, not NPC)
     * @param message The message text
     */
    public static void sendCombatMessage(Creature player, String message) {
        if (player == null || !player.isPlayer()) {
            return;
        }
        try {
            Communicator comm = player.getCommunicator();
            if (comm != null) {
                comm.sendCombatNormalMessage(message);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send combat message to player " + player.getWurmId(), e);
        }
    }

    /**
     * Send a server message to a player.
     *
     * <p>Server messages appear in the event tab with configurable color.</p>
     *
     * @param player The player (must be a player, not NPC)
     * @param message The message text
     * @param colorCode Wurm color code (0=white, 1=green, 2=blue, 3=red, etc.)
     */
    public static void sendServerMessage(Creature player, String message, byte colorCode) {
        if (player == null || !player.isPlayer()) {
            return;
        }
        try {
            Communicator comm = player.getCommunicator();
            if (comm != null) {
                comm.sendNormalServerMessage(message, colorCode);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send server message to player " + player.getWurmId(), e);
        }
    }

    /**
     * Send a colored message event to a player.
     *
     * <p>Colored messages allow multiple color segments in a single message.</p>
     *
     * @param player The player (must be a player, not NPC)
     * @param segments List of colored message segments
     */
    public static void sendColoredMessage(Creature player, List<MulticolorLineSegment> segments) {
        if (player == null || !player.isPlayer()) {
            return;
        }
        try {
            Communicator comm = player.getCommunicator();
            if (comm != null) {
                comm.sendColoredMessageEvent(segments);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send colored message to player " + player.getWurmId(), e);
        }
    }

    /**
     * Get all online players.
     *
     * @return Array of online players (never null, but may be empty)
     */
    public static Player[] getAllPlayers() {
        try {
            return com.wurmonline.server.Players.getInstance().getPlayers();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get all players", e);
            return new Player[0];
        }
    }

    /**
     * Get a player by wurm ID.
     *
     * <p>This uses the Players registry. For a combined player/creature lookup,
     * use {@link CreatureResolver#getCreatureOrNull(long)} instead.</p>
     *
     * @param playerWurmId Player wurm ID
     * @return Player instance, or null if not found/offline
     */
    public static Player getPlayer(long playerWurmId) {
        try {
            return com.wurmonline.server.Players.getInstance().getPlayerOrNull(playerWurmId);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get player " + playerWurmId, e);
            return null;
        }
    }
}
