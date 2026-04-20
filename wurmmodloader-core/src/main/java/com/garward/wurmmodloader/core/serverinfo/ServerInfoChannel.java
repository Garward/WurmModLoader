package com.garward.wurmmodloader.core.serverinfo;

import com.garward.wurmmodloader.api.httpserver.ModHttpServer;
import com.wurmonline.server.players.Player;
import com.garward.wurmmodloader.modcomm.Channel;
import com.garward.wurmmodloader.modcomm.IChannelListener;
import com.garward.wurmmodloader.modcomm.ModComm;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Framework-level ModComm channel that advertises server connection details
 * (HTTP URI, modloader version) to connecting clients.
 *
 * <p>Client mods obtain the server's HTTP base URI through this channel instead
 * of hardcoding a host/port — the livemap mod is the first consumer. Any future
 * mod that needs the server's HTTP endpoint should pull it from the matching
 * client-side {@code ServerInfoRegistry}.
 */
public final class ServerInfoChannel {

    private static final Logger logger = Logger.getLogger(ServerInfoChannel.class.getName());
    private static final String CHANNEL_NAME = "wml.serverinfo";
    private static final byte PACKET_SERVER_INFO = 1;
    private static final String MODLOADER_VERSION = "0.9.1";

    private static Channel channel;

    private ServerInfoChannel() {}

    public static synchronized void initialize() {
        if (channel != null) {
            return;
        }
        channel = ModComm.registerChannel(CHANNEL_NAME, new IChannelListener() {
            @Override
            public void onPlayerConnected(Player player) {
                // Fired once the client has completed the handshake and this
                // channel is actually active — only now can we push the server
                // info packet.
                sendToPlayer(player);
            }
        });
        logger.info("[ServerInfo] Registered " + CHANNEL_NAME + " channel");
    }

    public static void sendToPlayer(Player player) {
        if (channel == null) {
            return;
        }
        if (!channel.isActiveForPlayer(player)) {
            logger.fine("[ServerInfo] Channel not active for " + player.getName() + " (no client modloader)");
            return;
        }

        String httpUri = resolveHttpUri();
        try {
            ByteBuffer buf = buildPacket(httpUri, MODLOADER_VERSION);
            channel.sendMessage(player, buf);
            logger.info("[ServerInfo] Sent server info to " + player.getName() + " (httpUri=" + httpUri + ")");
        } catch (Exception e) {
            logger.warning("[ServerInfo] Failed to send server info to " + player.getName() + ": " + e.getMessage());
        }
    }

    private static String resolveHttpUri() {
        try {
            ModHttpServer http = ModHttpServer.getInstance();
            if (http != null && http.isRunning()) {
                URI uri = http.getUri();
                return uri != null ? uri.toString() : "";
            }
        } catch (Exception e) {
            logger.fine("[ServerInfo] HTTP server not available: " + e.getMessage());
        }
        return "";
    }

    private static ByteBuffer buildPacket(String httpUri, String version) {
        byte[] uriBytes = httpUri.getBytes(StandardCharsets.UTF_8);
        byte[] verBytes = version.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(1 + 2 + uriBytes.length + 2 + verBytes.length);
        buf.put(PACKET_SERVER_INFO);
        buf.putShort((short) uriBytes.length);
        buf.put(uriBytes);
        buf.putShort((short) verBytes.length);
        buf.put(verBytes);
        buf.flip();
        return buf;
    }
}
