package com.garward.wurmmodloader.mods.livemap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import com.garward.wurmmodloader.api.events.ModActionEvent;
import com.garward.wurmmodloader.core.event.EventBus;
import com.garward.wurmmodloader.mods.livemap.data.MapData;
import com.garward.wurmmodloader.mods.livemap.data.MapData.GuardTowerData;
import com.garward.wurmmodloader.mods.livemap.data.MapData.PlayerData;
import com.garward.wurmmodloader.mods.livemap.data.MapData.VillageData;
import com.garward.wurmmodloader.mods.livemap.modcomm.MarkersChannel;
import com.garward.wurmmodloader.mods.livemap.renderer.LiveMapRenderer;
import com.garward.wurmmodloader.mods.livemap.renderer.TileGridBuilder;
import com.garward.wurmmodloader.mods.livemap.web.Mustache;

import com.wurmonline.server.Server;

/**
 * Live interactive map mod — WurmMapGen-derived web UI + our renderer.
 *
 * <p>Architecture (see DESIGN.md): static tile grid generated to disk on
 * first boot, live marker data served as JSON. Web UI lifted from
 * tyoda-wurm/WurmMapGen, repointed at our endpoints.
 */
public class LiveMapMod implements WurmServerMod, Initable, Configurable, ServerStartedListener {

    private static final Logger LOGGER = Logger.getLogger(LiveMapMod.class.getName());
    private static final String MOD_NAME = "livemap";

    // Config
    private int tileSize = 256;
    private int maxRenderThreads = 4;
    private boolean regenOnBoot = false;
    private boolean enablePlayerTracking = true;
    private boolean enableVillageDisplay = true;
    private long markerPushIntervalMs = 2000;
    private String serverName = "Wurm Unlimited Server";

    // Runtime
    private LiveMapRenderer renderer;
    private Path tileDir;
    private String cachedIndexHtml;
    private MarkersChannel markersChannel;
    private final java.util.concurrent.atomic.AtomicBoolean regenInFlight =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    @Override
    public void init() {
        LOGGER.info("[LiveMap] init");
    }

    @Override
    public void configure(Properties p) {
        tileSize = parseInt(p.getProperty("tileSize"), tileSize);
        maxRenderThreads = parseInt(p.getProperty("maxRenderThreads"), maxRenderThreads);
        regenOnBoot = Boolean.parseBoolean(p.getProperty("regenOnBoot", String.valueOf(regenOnBoot)));
        enablePlayerTracking = Boolean.parseBoolean(p.getProperty("enablePlayerTracking", "true"));
        enableVillageDisplay = Boolean.parseBoolean(p.getProperty("enableVillageDisplay", "true"));
        markerPushIntervalMs = parseLong(p.getProperty("markerPushIntervalMs"), markerPushIntervalMs);
        serverName = p.getProperty("serverName", serverName);

        LOGGER.info("[LiveMap] config: tileSize=" + tileSize + " threads=" + maxRenderThreads
                + " regenOnBoot=" + regenOnBoot + " players=" + enablePlayerTracking
                + " villages=" + enableVillageDisplay + " pushMs=" + markerPushIntervalMs);
    }

    @Override
    public void onServerStarted() {
        renderer = new LiveMapRenderer(Server.surfaceMesh);
        tileDir = resolveTileDir();
        registerEndpoints();
        maybeBuildTiles();
        startMarkerPush();
        LOGGER.info("[LiveMap] ready — UI at /" + MOD_NAME + "/, tiles at " + tileDir);
    }

    private void startMarkerPush() {
        if (markerPushIntervalMs <= 0) {
            LOGGER.info("[LiveMap] marker push disabled (markerPushIntervalMs=" + markerPushIntervalMs + ")");
            return;
        }
        markersChannel = new MarkersChannel(markerPushIntervalMs, enablePlayerTracking, enableVillageDisplay);
        markersChannel.start();
    }

    private Path resolveTileDir() {
        String serverRoot = System.getProperty("wurmmodloader.serverdir", ".");
        return Paths.get(serverRoot, "mods", MOD_NAME, "cache", "images");
    }

    private void maybeBuildTiles() {
        TileGridBuilder builder = new TileGridBuilder(
                Server.surfaceMesh, renderer, tileDir, tileSize, maxRenderThreads);

        if (!regenOnBoot && builder.isAlreadyBuilt()) {
            LOGGER.info("[LiveMap] tiles already present, skipping regen (set regenOnBoot=true to force)");
            return;
        }

        Thread t = new Thread(() -> {
            try {
                builder.generate();
            } catch (Throwable th) {
                LOGGER.log(Level.SEVERE, "[LiveMap] tile generation failed", th);
            }
        }, "LiveMap-Bootstrap");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    // === HTTP routing ===

    private void registerEndpoints() {
        ModActionEvent ev = new ModActionEvent("httpserver:register_endpoint");
        ev.set("modName", MOD_NAME);
        // Match every subpath; we route internally below.
        ev.set("pattern", Pattern.compile("^(?<path>.*)$"));
        ev.set("handler", (java.util.function.Function<String, InputStream>) this::route);
        EventBus.getInstance().post(ev);
        if (!ev.isHandled()) {
            LOGGER.warning("[LiveMap] HTTP endpoint registration failed — httpserver subsystem not loaded?");
        } else {
            LOGGER.info("[LiveMap] HTTP endpoints registered at: " + ev.getString("prefix"));
        }
    }

    /**
     * Route a subpath (the part after {@code /livemap}). Empty / "/" → index.
     */
    private InputStream route(String subpath) {
        try {
            // Strip leading slash for easier matching
            String p = subpath == null ? "" : subpath;
            if (p.startsWith("/")) p = p.substring(1);
            // Strip query string (cachebusters etc.)
            int q = p.indexOf('?');
            if (q >= 0) p = p.substring(0, q);

            if (p.isEmpty() || "index.html".equals(p)) {
                return serveIndex();
            }
            if (p.startsWith("images/")) {
                return serveTile(p.substring("images/".length()));
            }
            if (p.startsWith("data/")) {
                return serveData(p.substring("data/".length()));
            }
            if (p.startsWith("admin/")) {
                return serveAdmin(p.substring("admin/".length()));
            }
            // Static UI: app/, css/, dist/, markers/
            if (p.startsWith("app/") || p.startsWith("css/")
                    || p.startsWith("dist/") || p.startsWith("markers/")) {
                return getClass().getResourceAsStream("/livemap-web/" + p);
            }
            return null;
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "[LiveMap] route error for " + subpath, t);
            return null;
        }
    }

    private InputStream serveIndex() throws IOException {
        if (cachedIndexHtml == null) {
            try (InputStream raw = getClass().getResourceAsStream("/livemap-web/index.html")) {
                if (raw == null) return null;
                String tpl = new String(readAll(raw), StandardCharsets.UTF_8);
                Map<String, String> ctx = new HashMap<>();
                ctx.put("serverName", serverName);
                ctx.put("enableRealtimeMarkers", String.valueOf(enablePlayerTracking));
                ctx.put("showPlayers", String.valueOf(enablePlayerTracking));
                ctx.put("showDeeds", String.valueOf(enableVillageDisplay));
                ctx.put("showGuardTowers", "true");
                ctx.put("showStructures", "false");
                ctx.put("showPortals", "false");
                ctx.put("showNoJs", "false");
                cachedIndexHtml = Mustache.render(tpl, ctx);
            }
        }
        return new ByteArrayInputStream(cachedIndexHtml.getBytes(StandardCharsets.UTF_8));
    }

    private InputStream serveTile(String name) throws IOException {
        // Defensive: only allow simple "{x}-{y}.png" names — no path traversal
        if (!name.matches("\\d+-\\d+\\.png")) return null;
        Path file = tileDir.resolve(name);
        if (!Files.exists(file)) return null;
        return Files.newInputStream(file);
    }

    private InputStream serveData(String name) {
        switch (name) {
            case "config.json":      return jsonStream(buildConfigJson());
            case "villages.json":    return jsonStream(buildVillagesJson());
            case "guardtowers.json": return jsonStream(buildGuardTowersJson());
            case "players.json":     return jsonStream(buildPlayersJson());
            case "structures.json":  return jsonStream("{\"structures\":[]}");
            case "portals.json":     return jsonStream("{\"portals\":[]}");
            default: return null;
        }
    }

    private InputStream serveAdmin(String name) {
        if ("regen".equals(name)) {
            if (!regenInFlight.compareAndSet(false, true)) {
                return jsonStream("{\"status\":\"in_progress\"}");
            }
            Thread t = new Thread(() -> {
                try {
                    LOGGER.info("[LiveMap] admin regen requested");
                    new TileGridBuilder(Server.surfaceMesh, renderer, tileDir, tileSize, maxRenderThreads).generate();
                } catch (Throwable th) {
                    LOGGER.log(Level.SEVERE, "[LiveMap] admin regen failed", th);
                } finally {
                    regenInFlight.set(false);
                }
            }, "LiveMap-AdminRegen");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
            return jsonStream("{\"status\":\"started\"}");
        }
        return null;
    }

    // === JSON builders ===

    private String buildConfigJson() {
        int mapSize = Server.surfaceMesh.getSize();
        // Match WurmMapGen's ConfigFileGen exactly — Leaflet's maxBounds in
        // map.js is computed from (maxMapSize, mapMaxZoom), and that pair must
        // map back to the world's true latlng range or most tiles fall outside
        // the bounds and never render. Upstream uses maxMapSize = mapSize * 8.
        int maxMapSize = mapSize * 8;
        int nativeZoom = upstreamZoom(mapSize);
        int mapMaxZoom = upstreamZoom(maxMapSize);
        // At zoom = nativeZoom the world spans `baseTilesPerSide × tileSize`
        // pixels (e.g. 8192 for an 8192-tile world at 256px tiles). Each step
        // down halves that. nativeZoom-3 lands the full world at ~1024px,
        // which fits comfortably in a typical browser viewport — that becomes
        // the floor so wheel-out tops out with the whole map in view.
        int mapMinZoom = Math.max(0, nativeZoom - 3);

        StringBuilder s = new StringBuilder(256);
        s.append("{\"config\":{");
        s.append("\"actualMapSize\":").append(mapSize).append(',');
        s.append("\"maxMapSize\":").append(maxMapSize).append(',');
        s.append("\"mapTileSize\":").append(tileSize).append(',');
        s.append("\"nativeZoom\":").append(nativeZoom).append(',');
        s.append("\"mapMinZoom\":").append(mapMinZoom).append(',');
        s.append("\"mapMaxZoom\":").append(mapMaxZoom).append(',');
        s.append("\"markerType\":2");
        s.append("}}");
        return s.toString();
    }

    /**
     * Mirrors WurmMapGen ConfigFileGen's zoom loop (deliberately quirky:
     * it does i = i/2 then i++ each iteration). For mapSize 8192 + tileSize 256
     * this yields 5; for 65536 + 256 it yields 8 — matching upstream output.
     */
    private int upstreamZoom(int size) {
        int zoom = 0;
        int count = 0;
        for (int i = size; i > tileSize; i++) {
            i = i / 2;
            zoom = count;
            count++;
        }
        return zoom;
    }

    private String buildVillagesJson() {
        StringBuilder s = new StringBuilder(1024);
        s.append("{\"villages\":[");
        if (enableVillageDisplay) {
            List<VillageData> villages = MapData.getVillages();
            for (int i = 0; i < villages.size(); i++) {
                if (i > 0) s.append(',');
                VillageData v = villages.get(i);
                s.append("{")
                 .append("\"id\":").append(i).append(',')
                 .append("\"name\":\"").append(esc(v.name)).append("\",")
                 .append("\"x\":").append(v.tokenX).append(',')
                 .append("\"y\":").append(v.tokenY).append(',')
                 .append("\"borders\":[").append(v.startX).append(',').append(v.startY)
                 .append(',').append(v.endX).append(',').append(v.endY).append("],")
                 .append("\"mayor\":\"").append(esc(v.mayor)).append("\",")
                 .append("\"motto\":\"").append(esc(v.motto)).append("\",")
                 .append("\"citizenCount\":").append(v.citizens).append(',')
                 .append("\"citizenNames\":[],")
                 .append("\"permanent\":").append(v.permanent)
                 .append("}");
            }
        }
        s.append("]}");
        return s.toString();
    }

    private String buildGuardTowersJson() {
        StringBuilder s = new StringBuilder(512);
        s.append("{\"guardtowers\":[");
        List<GuardTowerData> towers = MapData.getGuardTowers();
        for (int i = 0; i < towers.size(); i++) {
            if (i > 0) s.append(',');
            GuardTowerData t = towers.get(i);
            s.append("{")
             .append("\"x\":").append(t.x).append(',')
             .append("\"y\":").append(t.y).append(',')
             .append("\"borders\":[").append(t.x - 25).append(',').append(t.y - 25)
             .append(',').append(t.x + 25).append(',').append(t.y + 25).append("],")
             .append("\"creator\":\"\",")
             .append("\"ql\":0,")
             .append("\"dmg\":").append(t.damage)
             .append("}");
        }
        s.append("]}");
        return s.toString();
    }

    private String buildPlayersJson() {
        StringBuilder s = new StringBuilder(512);
        s.append("{\"players\":[");
        if (enablePlayerTracking) {
            // Browser path uses public view (no village gating) — show everyone.
            // Future: gate by viewer for in-game ModComm push, not here.
            List<PlayerData> players = allOnlinePlayers();
            for (int i = 0; i < players.size(); i++) {
                if (i > 0) s.append(',');
                PlayerData pd = players.get(i);
                s.append("{")
                 .append("\"id\":\"").append(esc(pd.name)).append("\",")
                 .append("\"name\":\"").append(esc(pd.name)).append("\",")
                 .append("\"x\":").append(pd.x).append(',')
                 .append("\"y\":").append(pd.y)
                 .append("}");
            }
        }
        s.append("]}");
        return s.toString();
    }

    private List<PlayerData> allOnlinePlayers() {
        List<PlayerData> all = new java.util.ArrayList<>();
        try {
            for (com.wurmonline.server.players.Player p : com.wurmonline.server.Players.getInstance().getPlayers()) {
                if (p == null || p.isOffline()) continue;
                all.add(new PlayerData(p.getName(), p.getTileX(), p.getTileY(),
                        p.isOnSurface(), p.getKingdomId()));
            }
        } catch (Throwable ignored) {}
        return all;
    }

    // === Utilities ===

    private static InputStream jsonStream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
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

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) baos.write(buf, 0, n);
        return baos.toByteArray();
    }

    private static int parseInt(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException nfe) { return def; }
    }

    private static long parseLong(String s, long def) {
        if (s == null) return def;
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException nfe) { return def; }
    }
}
