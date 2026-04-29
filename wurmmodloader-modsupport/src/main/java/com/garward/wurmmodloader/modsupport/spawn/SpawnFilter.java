package com.garward.wurmmodloader.modsupport.spawn;

import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.villages.Villages;
import com.wurmonline.server.zones.Zones;

import java.util.Random;

/**
 * Reusable surface-tile spawn picker for periodic world-boss / depot /
 * encounter spawners. Honors the constraints in {@link SpawnFilterConfig}
 * and always rejects deeded tiles (within ±50 tile sweep).
 *
 * <p>Promoted from {@code mods/supplydepot} once {@code titan} and
 * {@code rarespawn} adopted the same shape. Callers pass in their own
 * {@link SpawnFilterConfig} (typically loaded from {@code mod.properties}
 * via {@link SpawnFilterConfig#load(java.util.Properties, String)}).
 */
public final class SpawnFilter {

    /**
     * @return {@code (tileX, tileY)} surface coords, or {@code null} if no
     *         tile satisfied every constraint within {@code maxAttempts}.
     */
    public static int[] pickSurfaceTile(SpawnFilterConfig cfg, int maxAttempts) {
        int meshSize = Server.surfaceMesh.getSize();
        Random rand = Server.rand;

        int minX = 0, minY = 0, maxX = meshSize, maxY = meshSize;
        boolean restrictToRegion = cfg.areaCenterX >= 0 && cfg.areaCenterY >= 0 && cfg.areaRadius > 0;
        if (restrictToRegion) {
            minX = Math.max(0, cfg.areaCenterX - cfg.areaRadius);
            minY = Math.max(0, cfg.areaCenterY - cfg.areaRadius);
            maxX = Math.min(meshSize, cfg.areaCenterX + cfg.areaRadius);
            maxY = Math.min(meshSize, cfg.areaCenterY + cfg.areaRadius);
        } else if (cfg.useDefaultArenaRing) {
            // upstream WyvernMods default: random tile within 20%..80% of world size
            float worldSizeX = Zones.worldTileSizeX;
            float worldSizeY = Zones.worldTileSizeY;
            minX = (int) (worldSizeX * 0.2f);
            minY = (int) (worldSizeY * 0.2f);
            maxX = (int) (worldSizeX * 0.8f);
            maxY = (int) (worldSizeY * 0.8f);
        }

        int rangeX = Math.max(1, maxX - minX);
        int rangeY = Math.max(1, maxY - minY);

        for (int i = 0; i < maxAttempts; i++) {
            int x = minX + rand.nextInt(rangeX);
            int y = minY + rand.nextInt(rangeY);

            if (restrictToRegion) {
                int dx = x - cfg.areaCenterX;
                int dy = y - cfg.areaCenterY;
                if (dx * dx + dy * dy > cfg.areaRadius * cfg.areaRadius) continue;
            }

            int encoded = Server.surfaceMesh.getTile(x, y);
            short height = Tiles.decodeHeight(encoded);
            if (height < cfg.minHeight || height > cfg.maxHeight) continue;
            if (Creature.getTileSteepness(x, y, true)[1] >= cfg.maxSlope) continue;

            if (cfg.allowedTileTypes != null && cfg.allowedTileTypes.length > 0) {
                byte type = Tiles.decodeType(encoded);
                boolean ok = false;
                for (byte allowed : cfg.allowedTileTypes) {
                    if (type == allowed) { ok = true; break; }
                }
                if (!ok) continue;
            }

            if (hasNearbyVillage(x, y)) continue;
            return new int[] { x, y };
        }
        return null;
    }

    private static boolean hasNearbyVillage(int x, int y) {
        Village v = Villages.getVillage(x, y, true);
        if (v != null) return true;
        for (int vx = -50; vx < 50; vx += 5) {
            for (int vy = -50; vy < 50; vy += 5) {
                v = Villages.getVillage(x + vx, y + vy, true);
                if (v != null) return true;
            }
        }
        return false;
    }

    private SpawnFilter() {}
}
