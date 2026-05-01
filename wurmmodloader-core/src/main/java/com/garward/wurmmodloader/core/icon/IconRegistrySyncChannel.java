package com.garward.wurmmodloader.core.icon;

import com.garward.wurmmodloader.api.icon.Icon;
import com.garward.wurmmodloader.modcomm.Channel;
import com.garward.wurmmodloader.modcomm.IChannelListener;
import com.garward.wurmmodloader.modcomm.ModComm;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ModComm channel that pushes the dynamic-icon registry to clients.
 *
 * <p>Channel name {@code com.garward.icons}; one packet type
 * {@link #PACKET_REGISTRY_SYNC}, sent on player connect once the channel is
 * active for that player. Vanilla clients (no client modloader) won't have
 * the channel active — {@link Channel#isActiveForPlayer(Player)} short-circuits
 * the send, and the imageNumber rewrite (DynIcons 6) handles the fallback.
 *
 * <p>Wire format for {@code PACKET_REGISTRY_SYNC}:
 * <pre>
 * byte    type = 1
 * int     count
 * count × {
 *   short id
 *   short uriLen
 *   bytes uri (UTF-8, length = uriLen)
 * }
 * </pre>
 *
 * <p>Sent once per login, after the {@code wml.serverinfo} packet has set up
 * the HTTP base URI on the client. The server-pack manifest is announced by
 * vanilla resource sync earlier in the connect sequence, so by the time this
 * lands the client has already begun (or finished) downloading the
 * {@code framework-icons} pack.
 *
 * @since 0.4.1
 */
public final class IconRegistrySyncChannel {

    private static final Logger logger = Logger.getLogger(IconRegistrySyncChannel.class.getName());

    private static final String CHANNEL_NAME = "com.garward.icons";

    /** Packet type: full registry sync. */
    public static final byte PACKET_REGISTRY_SYNC = 1;

    private static Channel channel;

    private IconRegistrySyncChannel() {}

    /**
     * Test whether the icon-sync channel is active (i.e., client has a
     * matching modloader channel registered) for the given player.
     *
     * <p>Used by the outbound imageNumber gate to decide whether dynamic
     * icon ids (>= {@link com.garward.wurmmodloader.api.icon.Icon#FIRST_CUSTOM_ICON})
     * can safely be sent raw or must be rewritten to a vanilla fallback for
     * non-modloader clients.
     */
    public static boolean isActiveFor(Player player) {
        return channel != null && channel.isActiveForPlayer(player);
    }

    public static synchronized void initialize() {
        if (channel != null) {
            return;
        }
        channel = ModComm.registerChannel(CHANNEL_NAME, new IChannelListener() {
            @Override
            public void onPlayerConnected(Player player) {
                sendToPlayer(player);
            }
        });
        logger.info("[IconRegistrySync] Registered " + CHANNEL_NAME + " channel");
    }

    /**
     * Push the full registry contents to a single player. No-ops if the player
     * doesn't have the matching client channel (vanilla client) or if there
     * are no entries to ship.
     */
    public static void sendToPlayer(Player player) {
        if (channel == null) {
            return;
        }
        if (!channel.isActiveForPlayer(player)) {
            logger.fine("[IconRegistrySync] channel not active for " + player.getName()
                + " (vanilla client; falling back to imageNumber rewrite)");
            return;
        }

        Map<Short, String> entries = IconRegistry.entries();
        if (entries.isEmpty()) {
            logger.fine("[IconRegistrySync] no entries; not sending to " + player.getName());
            return;
        }

        try {
            ByteBuffer buf = buildPacket(entries);
            channel.sendMessage(player, buf);
            logger.info("[IconRegistrySync] Sent " + entries.size() + " icons to "
                + player.getName());
        } catch (Exception e) {
            logger.warning("[IconRegistrySync] Failed to send to " + player.getName() + ": "
                + e.getMessage());
        }

        // Items already in the player's inventory at login were sent before the
        // ModComm channel activated, so OutboundIconGate rewrote any custom
        // imageNumber to 0 — the client cached the paper-doll icon for those
        // items. Now that the channel is up, walk the inventory and re-emit
        // updates so the client picks up the proper imageNumbers.
        resendInventoryIconUpdates(player);
    }

    /** Hard cap on traversal depth — protects against pathological container nesting. */
    private static final int MAX_INVENTORY_DEPTH = 32;

    /** Hard cap on items visited — protects against parent-graph cycles. */
    private static final int MAX_INVENTORY_VISITS = 4096;

    private static void resendInventoryIconUpdates(Player player) {
        try {
            Communicator comm = player.getCommunicator();
            if (comm == null) {
                return;
            }
            Item inv = player.getInventory();
            if (inv == null) {
                return;
            }
            java.util.Set<Long> visited = new java.util.HashSet<>();
            int[] counters = new int[] { 0, 0 }; // updates, visits
            walkAndResend(inv, comm, 0, visited, counters, player);
            if (counters[0] > 0) {
                logger.info("[IconRegistrySync] Re-sent " + counters[0]
                    + " inventory item update(s) with custom icons to " + player.getName());
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING,
                "[IconRegistrySync] inventory icon refresh failed for " + player.getName(), t);
        }
    }

    private static void walkAndResend(Item container, Communicator comm, int depth,
                                      java.util.Set<Long> visited, int[] counters, Player player) {
        if (container == null) return;
        if (depth > MAX_INVENTORY_DEPTH) {
            logger.warning("[IconRegistrySync] inventory walk hit depth cap " + MAX_INVENTORY_DEPTH
                + " for " + player.getName() + " — stopping recursion");
            return;
        }
        if (counters[1] > MAX_INVENTORY_VISITS) {
            return; // single warn per call below
        }
        if (!visited.add(container.getWurmId())) {
            return; // already walked through this container — cycle
        }
        for (Item child : container.getItems()) {
            if (child == null) continue;
            if (++counters[1] > MAX_INVENTORY_VISITS) {
                logger.warning("[IconRegistrySync] inventory walk hit visit cap "
                    + MAX_INVENTORY_VISITS + " for " + player.getName() + " — stopping");
                return;
            }
            if ((child.getImageNumber() & 0xFFFF) >= Icon.FIRST_CUSTOM_ICON) {
                try {
                    comm.sendUpdateInventoryItem(child);
                    counters[0]++;
                } catch (Throwable t) {
                    // Skip the offender; keep walking the rest of the tree.
                }
            }
            walkAndResend(child, comm, depth + 1, visited, counters, player);
        }
    }

    private static ByteBuffer buildPacket(Map<Short, String> entries) {
        // Pre-encode URIs to size the buffer exactly.
        int payloadSize = 1 /* type */ + 4 /* count */;
        byte[][] uriBytes = new byte[entries.size()][];
        short[] ids = new short[entries.size()];
        int i = 0;
        for (Map.Entry<Short, String> e : entries.entrySet()) {
            ids[i] = e.getKey();
            uriBytes[i] = e.getValue().getBytes(StandardCharsets.UTF_8);
            payloadSize += 2 /* id */ + 2 /* uriLen */ + uriBytes[i].length;
            i++;
        }

        ByteBuffer buf = ByteBuffer.allocate(payloadSize);
        buf.put(PACKET_REGISTRY_SYNC);
        buf.putInt(entries.size());
        for (int k = 0; k < ids.length; k++) {
            buf.putShort(ids[k]);
            buf.putShort((short) uriBytes[k].length);
            buf.put(uriBytes[k]);
        }
        buf.flip();
        return buf;
    }
}
