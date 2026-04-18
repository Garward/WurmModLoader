package com.garward.wurmmodloader.capabilities;

import com.garward.wurmmodloader.modcomm.Channel;
import com.garward.wurmmodloader.modcomm.IChannelListener;
import com.garward.wurmmodloader.modcomm.ModComm;
import com.wurmonline.server.players.Player;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * ModComm channel for advertising server mod capabilities to clients.
 *
 * <p>This channel enables clients to detect which server-side mods are active
 * before enabling client-side features. For example, a sprint client mod should
 * only enable if the server has the sprint server mod.</p>
 *
 * <h2>Server-Side Usage:</h2>
 * <pre>{@code
 * // Register your mod's capability during initialization
 * WMLCapabilitiesChannel.registerServerMod("sprint_system", "1.0.0");
 * WMLCapabilitiesChannel.registerServerMod("power_scaling", "2.1.3");
 * }</pre>
 *
 * <h2>Client-Side Usage:</h2>
 * <pre>{@code
 * // In your client mod, subscribe to the event
 * @SubscribeEvent
 * public void onServerCapabilities(ServerCapabilitiesReceivedEvent event) {
 *     if (ServerCapabilities.hasServerMod("sprint_system")) {
 *         enableSprintFeatures();
 *     } else {
 *         logger.info("Server does not have sprint_system, client features disabled");
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public class WMLCapabilitiesChannel implements IChannelListener {

    private static final Logger logger = Logger.getLogger(WMLCapabilitiesChannel.class.getName());
    private static final String CHANNEL_NAME = "WML_CAPABILITIES";

    // Packet types
    private static final byte PACKET_SERVER_CAPABILITIES = 1;

    private static Channel channel;
    private static WMLCapabilitiesChannel instance;

    // Registry of server mods (populated during server startup)
    private static final List<ServerModCapability> serverMods = new ArrayList<>();

    /**
     * Initialize the WML_CAPABILITIES channel. Called during server startup.
     */
    public static void initialize() {
        if (instance == null) {
            instance = new WMLCapabilitiesChannel();
            channel = ModComm.registerChannel(CHANNEL_NAME, instance);
            logger.info("[WMLCapabilities] Registered WML_CAPABILITIES ModComm channel");
        }
    }

    /**
     * Register a server mod capability.
     *
     * <p>Call this during your mod's initialization (e.g., in {@code configure()}
     * or on {@code ServerStartedEvent}) to advertise your mod to clients.</p>
     *
     * @param modId Unique mod identifier (e.g., "sprint_system")
     * @param version Mod version (e.g., "1.0.0")
     */
    public static void registerServerMod(String modId, String version) {
        registerServerMod(modId, version, "");
    }

    /**
     * Register a server mod capability with description.
     *
     * @param modId Unique mod identifier
     * @param version Mod version
     * @param description Human-readable description
     */
    public static synchronized void registerServerMod(String modId, String version, String description) {
        ServerModCapability capability = new ServerModCapability(modId, version, description);

        // Check for duplicates
        for (ServerModCapability existing : serverMods) {
            if (existing.getModId().equals(modId)) {
                logger.warning("[WMLCapabilities] Mod '" + modId + "' already registered (version: " +
                              existing.getVersion() + "), skipping duplicate registration");
                return;
            }
        }

        serverMods.add(capability);
        logger.info("[WMLCapabilities] Registered server mod: " + capability);
    }

    /**
     * Get all registered server mod capabilities.
     *
     * @return Unmodifiable list of server mods
     */
    public static synchronized List<ServerModCapability> getServerMods() {
        return new ArrayList<>(serverMods);
    }

    /**
     * Send capability information to a player when they connect.
     *
     * <p>This is called automatically on player login via {@link PlayerLoginListener}.</p>
     *
     * @param player Player to send capabilities to
     */
    public static void sendCapabilitiesToPlayer(Player player) {
        if (channel == null) {
            logger.warning("[WMLCapabilities] Cannot send capabilities - channel not initialized");
            return;
        }

        if (!channel.isActiveForPlayer(player)) {
            logger.fine("[WMLCapabilities] Cannot send capabilities to " + player.getName() +
                       " - channel not active (client doesn't have WurmModLoader)");
            return;
        }

        synchronized (serverMods) {
            if (serverMods.isEmpty()) {
                logger.fine("[WMLCapabilities] No server mods registered, sending empty capability list to " +
                           player.getName());
            }

            try {
                ByteBuffer buffer = createCapabilitiesPacket();
                channel.sendMessage(player, buffer);

                logger.info("[WMLCapabilities] Sent " + serverMods.size() + " mod capabilities to " +
                           player.getName());
            } catch (Exception e) {
                logger.severe("[WMLCapabilities] Failed to send capabilities to " + player.getName() +
                             ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Create a ByteBuffer containing all server mod capabilities.
     *
     * <p>Packet format:</p>
     * <pre>
     * byte   - Packet type (PACKET_SERVER_CAPABILITIES = 1)
     * short  - Number of mods (N)
     * For each mod:
     *   string - Mod ID (UTF-8, length-prefixed)
     *   string - Version (UTF-8, length-prefixed)
     *   string - Description (UTF-8, length-prefixed)
     * </pre>
     *
     * @return ByteBuffer ready to send
     */
    private static ByteBuffer createCapabilitiesPacket() {
        // Calculate buffer size
        int size = 1 + 2; // packet type + mod count
        for (ServerModCapability mod : serverMods) {
            size += 2 + mod.getModId().getBytes().length;        // modId
            size += 2 + mod.getVersion().getBytes().length;      // version
            size += 2 + mod.getDescription().getBytes().length;  // description
        }

        ByteBuffer buffer = ByteBuffer.allocate(size);

        // Write packet type
        buffer.put(PACKET_SERVER_CAPABILITIES);

        // Write number of mods
        buffer.putShort((short) serverMods.size());

        // Write each mod
        for (ServerModCapability mod : serverMods) {
            writeString(buffer, mod.getModId());
            writeString(buffer, mod.getVersion());
            writeString(buffer, mod.getDescription());
        }

        buffer.flip();
        return buffer;
    }

    /**
     * Write a UTF-8 string to buffer with length prefix.
     *
     * @param buffer Target buffer
     * @param str String to write
     */
    private static void writeString(ByteBuffer buffer, String str) {
        byte[] bytes = str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        buffer.putShort((short) bytes.length);
        buffer.put(bytes);
    }

    // IChannelListener implementation - server doesn't receive capability packets
    @Override
    public void handleMessage(Player player, ByteBuffer message) {
        logger.warning("[WMLCapabilities] Unexpected message from client " + player.getName() +
                      " - server doesn't accept capability packets");
    }
}
