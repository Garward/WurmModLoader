package com.garward.wurmmodloader.mods.livemap;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import com.garward.wurmmodloader.mods.livemap.data.MapData;
import com.garward.wurmmodloader.mods.livemap.data.MapData.PlayerData;
import com.garward.wurmmodloader.mods.livemap.data.MapData.VillageData;
import com.garward.wurmmodloader.mods.livemap.renderer.LiveMapRenderer;

import com.garward.wurmmodloader.api.events.ModActionEvent;
import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.structure.SurfaceTileChangedEvent;
import com.garward.wurmmodloader.core.event.EventBus;
import com.wurmonline.server.Server;

/**
 * Live interactive map mod.
 * Provides web-based map viewer with real-time player positions, villages, and terrain.
 */
public class LiveMapMod implements WurmServerMod, Initable, Configurable, ServerStartedListener {

    private static final Logger logger = Logger.getLogger(LiveMapMod.class.getName());

    // Configuration
    private int tileSize = 256;
    private int cacheTimeMinutes = 30;

    // Zoom range. Negative zooms downsample more world per tile so the
    // whole 8k map can fit on screen; each step out halves the tile count
    // per side. MIN_ZOOM=-2 adds two overview levels beyond the base grid.
    private static final int MIN_ZOOM = -3;
    private static final int MAX_ZOOM = 5;

    private static int tilesPerSideAt(int baseTilesPerSide, int zoom) {
        if (zoom >= 0) return baseTilesPerSide << zoom;
        int shifted = baseTilesPerSide >> (-zoom);
        return Math.max(1, shifted);
    }
    private boolean enablePlayerTracking = true;
    private boolean enableVillageDisplay = true;

    // Tile cache
    private final Map<String, CachedTile> tileCache = new HashMap<>();

    // Renderer
    private LiveMapRenderer renderer;

    @Override
    public void init() {
        logger.info("LiveMap mod initializing...");
    }

    @Override
    public void configure(Properties properties) {
        tileSize = Integer.parseInt(properties.getProperty("tileSize", "256"));
        cacheTimeMinutes = Integer.parseInt(properties.getProperty("cacheTimeMinutes", "30"));
        enablePlayerTracking = Boolean.parseBoolean(properties.getProperty("enablePlayerTracking", "true"));
        enableVillageDisplay = Boolean.parseBoolean(properties.getProperty("enableVillageDisplay", "true"));

        logger.info("tileSize: " + tileSize);
        logger.info("cacheTimeMinutes: " + cacheTimeMinutes);
        logger.info("enablePlayerTracking: " + enablePlayerTracking);
        logger.info("enableVillageDisplay: " + enableVillageDisplay);
    }

    @Override
    public void onServerStarted() {
        // Initialize renderer with server mesh
        renderer = new LiveMapRenderer(Server.surfaceMesh);

        // Register HTTP endpoints using event API
        registerEndpoints();

        // Subscribe to terrain change events so cached PNG tiles drop when
        // the world mutates (admin terraform, future terraforming hooks, etc.).
        EventBus.getInstance().register(this);

        logger.info("LiveMap started successfully!");
    }

    /**
     * Drop any cached PNG tiles whose world region covers the changed mesh tile.
     * Keys are "z/x/y"; region is [x*tileWorldSize, (x+1)*tileWorldSize) x same Y.
     */
    @SubscribeEvent
    public void onSurfaceTileChanged(SurfaceTileChangedEvent e) {
        if (!e.isOnSurface()) return;
        int tx = e.getTileX();
        int ty = e.getTileY();
        int mapSize = Server.surfaceMesh.getSize();
        int baseTilesPerSide = (mapSize + tileSize - 1) / tileSize;
        synchronized (tileCache) {
            tileCache.keySet().removeIf(key -> {
                int s1 = key.indexOf('/');
                int s2 = key.indexOf('/', s1 + 1);
                if (s1 < 0 || s2 < 0) return false;
                try {
                    int z = Integer.parseInt(key.substring(0, s1));
                    int x = Integer.parseInt(key.substring(s1 + 1, s2));
                    int y = Integer.parseInt(key.substring(s2 + 1));
                    int tps = tilesPerSideAt(baseTilesPerSide, z);
                    int tileWorldSize = Math.max(1, mapSize / tps);
                    int wx0 = x * tileWorldSize, wx1 = wx0 + tileWorldSize;
                    int wy0 = y * tileWorldSize, wy1 = wy0 + tileWorldSize;
                    return tx >= wx0 && tx < wx1 && ty >= wy0 && ty < wy1;
                } catch (NumberFormatException nfe) {
                    return false;
                }
            });
        }
    }

    /**
     * Register HTTP endpoints via event-based API
     */
    private void registerEndpoints() {
        // Register tile endpoint: /livemap/tile/{z}/{x}/{y}.png
        ModActionEvent tileEndpoint = new ModActionEvent("httpserver:register_endpoint");
        tileEndpoint.set("modName", "livemap");
        tileEndpoint.set("pattern", Pattern.compile("^/tile/(?<path>.*)$"));
        tileEndpoint.set("handler", (java.util.function.Function<String, InputStream>) this::handleTileRequest);
        EventBus.getInstance().post(tileEndpoint);

        if (!tileEndpoint.isHandled()) {
            logger.warning("Failed to register tile endpoint - httpserver not loaded?");
        } else {
            logger.info("Registered tile endpoint at: " + tileEndpoint.getString("prefix"));
        }

        // Register data API endpoint: /livemap/api/data
        ModActionEvent dataEndpoint = new ModActionEvent("httpserver:register_endpoint");
        dataEndpoint.set("modName", "livemap");
        dataEndpoint.set("pattern", Pattern.compile("^/api/(?<path>.*)$"));
        dataEndpoint.set("handler", (java.util.function.Function<String, InputStream>) this::handleApiRequest);
        EventBus.getInstance().post(dataEndpoint);

        if (!dataEndpoint.isHandled()) {
            logger.warning("Failed to register API endpoint - httpserver not loaded?");
        } else {
            logger.info("Registered API endpoint at: " + dataEndpoint.getString("prefix"));
        }

        // Register web UI endpoint: /livemap/
        ModActionEvent uiEndpoint = new ModActionEvent("httpserver:register_endpoint");
        uiEndpoint.set("modName", "livemap");
        uiEndpoint.set("pattern", Pattern.compile("^/(?<path>|index\\.html)$"));
        uiEndpoint.set("handler", (java.util.function.Function<String, InputStream>) this::handleUiRequest);
        EventBus.getInstance().post(uiEndpoint);

        if (!uiEndpoint.isHandled()) {
            logger.warning("Failed to register UI endpoint - httpserver not loaded?");
        } else {
            logger.info("Registered UI endpoint at: " + uiEndpoint.getString("prefix"));
        }
    }

    /**
     * Handle tile requests: /livemap/tile/{z}/{x}/{y}.png
     */
    private InputStream handleTileRequest(String path) {
        try {
            // Parse tile coordinates from path (allow negative coordinates)
            Pattern pattern = Pattern.compile("^(-?\\d+)/(-?\\d+)/(-?\\d+)\\.png$");
            Matcher matcher = pattern.matcher(path);

            if (!matcher.matches()) {
                return null; // Silently ignore invalid paths
            }

            int z = Integer.parseInt(matcher.group(1));
            int x = Integer.parseInt(matcher.group(2));
            int y = Integer.parseInt(matcher.group(3));

            // Validate zoom level. Negative zooms are "zoomed out" overview
            // tiles where each tile covers 2^|z| more world per pixel.
            if (z < MIN_ZOOM || z > MAX_ZOOM) {
                return null;
            }

            int mapSize = Server.surfaceMesh.getSize();
            int basetilesPerSide = (mapSize + tileSize - 1) / tileSize;
            int tilesPerSide = tilesPerSideAt(basetilesPerSide, z);

            // Validate tile coordinates are within bounds
            if (x < 0 || x >= tilesPerSide || y < 0 || y >= tilesPerSide) {
                return null; // Out of bounds, return null (404)
            }

            return getTile(z, x, y);

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error handling tile request: " + path, e);
            return null;
        }
    }

    /**
     * Handle API requests: /livemap/api/data
     */
    private InputStream handleApiRequest(String path) {
        try {
            if (path.equals("data")) {
                // Public payload — no player positions. Browser UI path.
                return getMapData(null);
            } else if (path.startsWith("data/me/")) {
                // Client-authenticated view: filter players to the viewer's village.
                String viewer = path.substring("data/me/".length());
                try { viewer = java.net.URLDecoder.decode(viewer, "UTF-8"); } catch (Throwable ignored) {}
                return getMapData(viewer);
            } else if (path.equals("config")) {
                return getMapConfig();
            }

            logger.warning("Unknown API path: " + path);
            return null;

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error handling API request: " + path, e);
            return null;
        }
    }

    /**
     * Handle UI requests: /livemap/ or /livemap/index.html
     */
    private InputStream handleUiRequest(String path) {
        try {
            // Return embedded HTML for map viewer
            return getClass().getResourceAsStream("/livemap/index.html");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error handling UI request: " + path, e);
            // Return minimal HTML if resource not found
            return new ByteArrayInputStream(getMinimalHtml().getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Get map tile as PNG
     */
    private InputStream getTile(int z, int x, int y) {
        try {
            String key = z + "/" + x + "/" + y;

            // Check cache
            CachedTile cached = tileCache.get(key);
            if (cached != null && !cached.isExpired()) {
                return new ByteArrayInputStream(cached.data);
            }

            // Generate tile
            BufferedImage tile = generateTile(z, x, y);
            if (tile == null) {
                return null;
            }

            // Encode as PNG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(tile, "PNG", baos);
            byte[] data = baos.toByteArray();

            // Cache it
            tileCache.put(key, new CachedTile(data, cacheTimeMinutes * 60 * 1000));

            return new ByteArrayInputStream(data);

        } catch (IOException e) {
            logger.log(Level.WARNING, "Error generating tile", e);
            return null;
        }
    }

    /**
     * Generate map tile
     */
    private BufferedImage generateTile(int z, int x, int y) {
        try {
            // Calculate world coordinates from tile coordinates
            int mapSize = Server.surfaceMesh.getSize();

            // Zoom level 0 = base tiles (e.g., 2048/256 = 8 tiles per side)
            // Each zoom level doubles the tiles per side
            int baseTilesPerSide = (mapSize + tileSize - 1) / tileSize; // Ceiling division
            int tilesPerSide = tilesPerSideAt(baseTilesPerSide, z);
            int tileWorldSize = Math.max(1, mapSize / tilesPerSide);

            int worldX = x * tileWorldSize;
            int worldY = y * tileWorldSize;

            // Always render directly at tile size to avoid huge memory usage
            // Sample every Nth pixel for lower zoom levels
            int sampleRate = Math.max(1, tileWorldSize / tileSize);

            logger.fine(String.format("Generating tile %d/%d/%d: world area [%d,%d] to [%d,%d], sample rate: %d",
                z, x, y, worldX, worldY, worldX + tileWorldSize, worldY + tileWorldSize, sampleRate));

            BufferedImage tile = renderer.createMapDump(worldX, worldY, tileWorldSize, tileWorldSize);

            if (tile == null) {
                logger.warning(String.format("Renderer returned null for tile %d/%d/%d", z, x, y));
                return createBlankTile();
            }

            // Scale to tile size if needed
            if (tile.getWidth() != tileSize || tile.getHeight() != tileSize) {
                BufferedImage scaled = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D g = scaled.createGraphics();
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(tile, 0, 0, tileSize, tileSize, null);
                g.dispose();
                logger.fine(String.format("Scaled tile %d/%d/%d from %dx%d to %dx%d",
                    z, x, y, tile.getWidth(), tile.getHeight(), tileSize, tileSize));
                return scaled;
            }

            logger.fine(String.format("Generated tile %d/%d/%d successfully", z, x, y));
            return tile;

        } catch (Exception e) {
            logger.log(Level.SEVERE, String.format("Error generating tile %d/%d/%d", z, x, y), e);
            return createBlankTile();
        }
    }

    /**
     * Create a blank error tile
     */
    private BufferedImage createBlankTile() {
        BufferedImage blank = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = blank.createGraphics();
        g.setColor(java.awt.Color.DARK_GRAY);
        g.fillRect(0, 0, tileSize, tileSize);
        g.setColor(java.awt.Color.RED);
        g.drawString("Error", 10, tileSize / 2);
        g.dispose();
        return blank;
    }

    /**
     * Get live map data as JSON
     */
    private InputStream getMapData(String viewerName) {
        StringBuilder json = new StringBuilder();
        json.append("{");

        // Players — village-gated. Empty array if no viewer (browser path).
        if (enablePlayerTracking) {
            json.append("\"players\":[");
            List<PlayerData> players = MapData.getPlayersForViewer(viewerName);
            for (int i = 0; i < players.size(); i++) {
                PlayerData player = players.get(i);
                if (i > 0) json.append(",");
                json.append("{")
                    .append("\"name\":\"").append(escapeJson(player.name)).append("\",")
                    .append("\"x\":").append(player.x).append(",")
                    .append("\"y\":").append(player.y).append(",")
                    .append("\"surface\":").append(player.surface).append(",")
                    .append("\"kingdom\":").append(player.kingdom)
                    .append("}");
            }
            json.append("],");
        }

        // Villages
        if (enableVillageDisplay) {
            json.append("\"villages\":[");
            List<VillageData> villages = MapData.getVillages();
            for (int i = 0; i < villages.size(); i++) {
                VillageData village = villages.get(i);
                if (i > 0) json.append(",");
                json.append("{")
                    .append("\"name\":\"").append(escapeJson(village.name)).append("\",")
                    .append("\"startX\":").append(village.startX).append(",")
                    .append("\"startY\":").append(village.startY).append(",")
                    .append("\"endX\":").append(village.endX).append(",")
                    .append("\"endY\":").append(village.endY).append(",")
                    .append("\"type\":\"").append(village.type).append("\",")
                    .append("\"mayor\":\"").append(escapeJson(village.mayor)).append("\",")
                    .append("\"motto\":\"").append(escapeJson(village.motto)).append("\",")
                    .append("\"citizens\":").append(village.citizens).append(",")
                    .append("\"permanent\":").append(village.permanent).append(",")
                    .append("\"tokenX\":").append(village.tokenX).append(",")
                    .append("\"tokenY\":").append(village.tokenY)
                    .append("}");
            }
            json.append("],");
        }

        // Guard towers
        json.append("\"towers\":[");
        List<MapData.GuardTowerData> towers = MapData.getGuardTowers();
        for (int i = 0; i < towers.size(); i++) {
            MapData.GuardTowerData t = towers.get(i);
            if (i > 0) json.append(",");
            json.append("{")
                .append("\"name\":\"").append(escapeJson(t.name)).append("\",")
                .append("\"x\":").append(t.x).append(",")
                .append("\"y\":").append(t.y).append(",")
                .append("\"kingdom\":").append(t.kingdom).append(",")
                .append("\"damage\":").append(t.damage)
                .append("}");
        }
        json.append("],");

        // Altars
        json.append("\"altars\":[");
        List<MapData.AltarData> altars = MapData.getAltars();
        for (int i = 0; i < altars.size(); i++) {
            MapData.AltarData altar = altars.get(i);
            if (i > 0) json.append(",");
            json.append("{")
                .append("\"name\":\"").append(escapeJson(altar.name)).append("\",")
                .append("\"x\":").append(altar.x).append(",")
                .append("\"y\":").append(altar.y).append(",")
                .append("\"type\":\"").append(altar.type).append("\"")
                .append("}");
        }
        json.append("]");

        json.append("}");

        return new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Get map configuration as JSON
     */
    private InputStream getMapConfig() {
        int mapSize = Server.surfaceMesh.getSize();
        String json = String.format(
            "{\"mapSize\":%d,\"tileSize\":%d,\"minZoom\":%d,\"maxZoom\":%d,\"enablePlayers\":%b,\"enableVillages\":%b}",
            mapSize, tileSize, MIN_ZOOM, MAX_ZOOM, enablePlayerTracking, enableVillageDisplay
        );
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Escape JSON string
     */
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Get minimal HTML viewer (fallback if resource not found)
     */
    private String getMinimalHtml() {
        return "<!DOCTYPE html><html><head><title>LiveMap</title></head><body>" +
               "<h1>LiveMap</h1><p>Map viewer not yet implemented. API available at:</p>" +
               "<ul><li>/livemap/api/data - Live data (players, villages)</li>" +
               "<li>/livemap/api/config - Map configuration</li>" +
               "<li>/livemap/tile/{z}/{x}/{y}.png - Map tiles</li></ul>" +
               "</body></html>";
    }

    /**
     * Cached tile with expiration
     */
    private static class CachedTile {
        final byte[] data;
        final long expiresAt;

        CachedTile(byte[] data, long ttlMs) {
            this.data = data;
            this.expiresAt = System.currentTimeMillis() + ttlMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
