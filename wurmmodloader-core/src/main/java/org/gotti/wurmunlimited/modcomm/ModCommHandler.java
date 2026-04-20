package org.gotti.wurmunlimited.modcomm;

import com.wurmonline.server.players.Player;

import java.nio.ByteBuffer;

/**
 * Legacy compat shim. Never invoked by bytecode anymore — the canonical
 * handler is {@link com.garward.wurmmodloader.modcomm.ModCommHandler}. Kept
 * only as a symbol so legacy references still resolve; delegates on the off
 * chance anything reflects in.
 */
public class ModCommHandler {
    public static void handlePacket(Player player, ByteBuffer msg) {
        com.garward.wurmmodloader.modcomm.ModCommHandler.handlePacket(player, msg);
    }
}
