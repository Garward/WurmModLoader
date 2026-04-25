package com.garward.wurmmodloader.mods.livemap.modcomm;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.garward.wurmmodloader.mods.livemap.data.MapData;
import com.garward.wurmmodloader.mods.livemap.data.MapData.PlayerData;
import com.garward.wurmmodloader.mods.livemap.data.MapData.VillageData;
import com.garward.wurmmodloader.modcomm.Channel;
import com.garward.wurmmodloader.modcomm.IChannelListener;
import com.garward.wurmmodloader.modcomm.ModComm;
import com.wurmonline.server.Players;
import com.wurmonline.server.players.Player;

/**
 * ModComm channel that pushes per-player marker snapshots to the in-game
 * client. Channel name {@code livemap.markers}.
 *
 * <p>The browser path uses HTTP polling against {@code /livemap/data/players.json};
 * the in-game client receives the same data via push so it doesn't need the
 * server's HTTP port and gets per-viewer village-gated player visibility.
 *
 * <p>Packet format ({@link #PACKET_SNAPSHOT}):
 * <pre>
 *   byte   packet type
 *   short  JSON byte length
 *   bytes  UTF-8 JSON payload
 * </pre>
 *
 * <p>The JSON shape mirrors the browser feed:
 * <pre>{"players":[...], "villages":[...]}</pre>
 */
public final class MarkersChannel {

    private static final Logger LOGGER = Logger.getLogger(MarkersChannel.class.getName());
    public static final String CHANNEL_NAME = "livemap.markers";
    private static final byte PACKET_SNAPSHOT = 1;

    private final long pushIntervalMs;
    private final boolean enablePlayerTracking;
    private final boolean enableVillageDisplay;

    private Channel channel;
    private ScheduledExecutorService scheduler;

    public MarkersChannel(long pushIntervalMs, boolean enablePlayerTracking, boolean enableVillageDisplay) {
        this.pushIntervalMs = pushIntervalMs;
        this.enablePlayerTracking = enablePlayerTracking;
        this.enableVillageDisplay = enableVillageDisplay;
    }

    public synchronized void start() {
        if (channel != null) return;
        // Reload-safe: drop any stale registration from a prior boot.
        ModComm.unregisterChannel(CHANNEL_NAME);
        channel = ModComm.registerChannel(CHANNEL_NAME, new IChannelListener() {
            @Override
            public void onPlayerConnected(Player player) {
                pushOne(player);
            }
        });
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LiveMap-MarkerPush");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::pushAll, pushIntervalMs, pushIntervalMs, TimeUnit.MILLISECONDS);
        LOGGER.info("[LiveMap] ModComm channel " + CHANNEL_NAME + " registered, push every " + pushIntervalMs + "ms");
    }

    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void pushAll() {
        if (channel == null) return;
        try {
            for (Player p : Players.getInstance().getPlayers()) {
                if (p == null || p.isOffline()) continue;
                if (!channel.isActiveForPlayer(p)) continue;
                pushOne(p);
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "[LiveMap] marker push tick failed", t);
        }
    }

    private void pushOne(Player viewer) {
        if (channel == null || !channel.isActiveForPlayer(viewer)) return;
        try {
            String json = buildSnapshot(viewer.getName());
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            ByteBuffer buf = ByteBuffer.allocate(1 + 2 + body.length);
            buf.put(PACKET_SNAPSHOT);
            buf.putShort((short) body.length);
            buf.put(body);
            buf.flip();
            channel.sendMessage(viewer, buf);
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "[LiveMap] failed to push markers to " + viewer.getName(), t);
        }
    }

    private String buildSnapshot(String viewerName) {
        StringBuilder s = new StringBuilder(512);
        s.append("{\"players\":[");
        if (enablePlayerTracking) {
            List<PlayerData> players = MapData.getPlayersForViewer(viewerName);
            for (int i = 0; i < players.size(); i++) {
                if (i > 0) s.append(',');
                PlayerData pd = players.get(i);
                s.append("{\"name\":\"").append(esc(pd.name)).append("\",")
                 .append("\"x\":").append(pd.x).append(',')
                 .append("\"y\":").append(pd.y).append(',')
                 .append("\"surface\":").append(pd.surface).append(',')
                 .append("\"kingdom\":").append(pd.kingdom).append('}');
            }
        }
        s.append("],\"villages\":[");
        if (enableVillageDisplay) {
            List<VillageData> villages = MapData.getVillages();
            for (int i = 0; i < villages.size(); i++) {
                if (i > 0) s.append(',');
                VillageData v = villages.get(i);
                s.append("{\"name\":\"").append(esc(v.name)).append("\",")
                 .append("\"x\":").append(v.tokenX).append(',')
                 .append("\"y\":").append(v.tokenY).append(',')
                 .append("\"borders\":[").append(v.startX).append(',').append(v.startY)
                 .append(',').append(v.endX).append(',').append(v.endY).append("]}");
            }
        }
        s.append("]}");
        return s.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
            }
        }
        return out.toString();
    }
}
