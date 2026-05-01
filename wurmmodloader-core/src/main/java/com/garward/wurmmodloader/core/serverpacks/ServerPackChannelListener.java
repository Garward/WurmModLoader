package com.garward.wurmmodloader.core.serverpacks;

import com.wurmonline.server.Server;
import com.wurmonline.server.players.Player;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helpers for ModComm command handling on the serverpacks channel. Renamed
 * from the community-mod {@code CommandHandler} during framework promotion.
 */
final class ServerPackChannelListener {

    private static final Logger logger = Logger.getLogger(ServerPackChannelListener.class.getName());

    private ServerPackChannelListener() {}

    /**
     * Refreshes the player's vision area + creature port. Triggered by the
     * {@code CMD_REFRESH} (0x01) command on either pack channel.
     */
    static void sendModelRefresh(Player player) {
        try {
            player.createVisionArea();
            Server.getInstance().addCreatureToPort(player);
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }
}
