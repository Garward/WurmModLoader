package com.garward.wurmmodloader.modsupport.spawn;

import com.wurmonline.mesh.Tiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Surface-tile spawn-filter config block. Read from a mod's
 * {@code .properties} file via {@link #load(Properties, String)}.
 *
 * <p>Recognized keys (all prefixed with {@code spawn} so they don't
 * collide with the rest of the mod's config):
 * <pre>
 *   spawnAreaCenterX        (int, tile coord; -1 = unset)
 *   spawnAreaCenterY        (int, tile coord; -1 = unset)
 *   spawnAreaRadius         (int, tiles;       -1 = unset)
 *   spawnMinHeight          (short; default 1)        // 0 = water
 *   spawnMaxHeight          (short; default 999)
 *   spawnMaxSlope           (int;   default 30)
 *   spawnTileTypes          (csv; e.g. "TILE_SNOW,TILE_GRASS"; empty = any)
 *   useDefaultArenaRing     (bool;  default false)    // 20-80% world ring
 * </pre>
 *
 * If centerX/centerY/radius are all set, {@link SpawnFilter} restricts to
 * the disc. Otherwise it picks anywhere on the surface mesh (or, when
 * {@code useDefaultArenaRing=true}, within 20-80% of {@code worldTileSize}).
 */
public final class SpawnFilterConfig {

    private static final Logger logger = Logger.getLogger(SpawnFilterConfig.class.getName());

    public int areaCenterX = -1;
    public int areaCenterY = -1;
    public int areaRadius = -1;
    public short minHeight = 1;
    public short maxHeight = 999;
    public int maxSlope = 30;
    public byte[] allowedTileTypes;
    public boolean useDefaultArenaRing = false;

    public void load(Properties p, String modTag) {
        areaCenterX = intVal(p, "spawnAreaCenterX", areaCenterX, modTag);
        areaCenterY = intVal(p, "spawnAreaCenterY", areaCenterY, modTag);
        areaRadius = intVal(p, "spawnAreaRadius", areaRadius, modTag);
        minHeight = (short) intVal(p, "spawnMinHeight", minHeight, modTag);
        maxHeight = (short) intVal(p, "spawnMaxHeight", maxHeight, modTag);
        maxSlope = intVal(p, "spawnMaxSlope", maxSlope, modTag);
        useDefaultArenaRing = bool(p, "useDefaultArenaRing", useDefaultArenaRing);

        String tileCsv = p.getProperty("spawnTileTypes");
        allowedTileTypes = parseTileTypes(tileCsv, modTag);
    }

    private static byte[] parseTileTypes(String csv, String modTag) {
        if (csv == null || csv.trim().isEmpty()) return null;
        List<Byte> ids = new ArrayList<>();
        for (String token : csv.split(",")) {
            String name = token.trim();
            if (name.isEmpty()) continue;
            try {
                Tiles.Tile tile = Tiles.Tile.valueOf(name);
                ids.add(tile.id);
            } catch (IllegalArgumentException e) {
                logger.warning("[" + modTag + "] unknown tile type in spawnTileTypes: " + name);
            }
        }
        if (ids.isEmpty()) return null;
        byte[] out = new byte[ids.size()];
        for (int i = 0; i < ids.size(); i++) out[i] = ids.get(i);
        return out;
    }

    private static boolean bool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v.trim());
    }

    private static int intVal(Properties p, String key, int def, String modTag) {
        String v = p.getProperty(key);
        if (v == null) return def;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            logger.warning("[" + modTag + "] invalid int for " + key + "=" + v + " (using " + def + ")");
            return def;
        }
    }
}
