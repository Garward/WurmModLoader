package com.garward.wurmmodloader.mods.templarpatrol;

import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.movement.CreaturePollMovementPreEvent;
import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.ai.PathTile;

import com.garward.wurmmodloader.modloader.interfaces.Reloadable;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Drives configured creatures along fixed waypoint routes. Uses
 * {@link CreaturePollMovementPreEvent} to override vanilla idle wandering;
 * vanilla combat/chase is left intact when the creature has a target.
 */
public final class TemplarPatrolMod implements WurmServerMod, Configurable, ServerStartedListener, Reloadable {

    private static final Logger LOGGER = Logger.getLogger(TemplarPatrolMod.class.getName());

    private int arrivalDistance = 2;
    private boolean pauseInCombat = true;
    private String routesFile = "mods/templarpatrol/routes.yaml";

    private final ConcurrentHashMap<Long, RouteState> routes = new ConcurrentHashMap<>();

    @Override
    public void configure(Properties properties) {
        this.arrivalDistance = parseInt(properties.getProperty("arrivalDistance"), arrivalDistance);
        this.pauseInCombat = Boolean.parseBoolean(properties.getProperty("pauseInCombat", String.valueOf(pauseInCombat)));
        this.routesFile = properties.getProperty("routesFile", routesFile);
    }

    @Override
    public void onServerStarted() {
        loadRoutes();
    }

    @Override
    public void onReload() {
        LOGGER.info("TemplarPatrol: reloading routes.");
        loadRoutes();
    }

    private void loadRoutes() {
        routes.clear();
        Path path = Paths.get(routesFile);
        if (!Files.isRegularFile(path)) {
            LOGGER.info("TemplarPatrol: no routes file at " + path.toAbsolutePath() + " — mod is idle.");
            return;
        }
        try (Reader reader = new FileReader(path.toFile())) {
            Object parsed = new Yaml().load(reader);
            if (!(parsed instanceof Map)) {
                LOGGER.warning("TemplarPatrol: routes.yaml root is not a map");
                return;
            }
            Object raw = ((Map<?, ?>) parsed).get("routes");
            if (!(raw instanceof Map)) {
                LOGGER.info("TemplarPatrol: no 'routes' section — mod is idle.");
                return;
            }
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
                Long wurmId = toLong(entry.getKey());
                List<int[]> waypoints = toWaypoints(entry.getValue());
                if (wurmId == null || waypoints.isEmpty()) {
                    LOGGER.warning("TemplarPatrol: skipping bad entry " + entry.getKey());
                    continue;
                }
                routes.put(wurmId, new RouteState(waypoints));
            }
            LOGGER.info("TemplarPatrol: loaded " + routes.size() + " route(s).");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "TemplarPatrol: failed to load " + path, e);
        }
    }

    @SubscribeEvent
    public void onPollMovement(CreaturePollMovementPreEvent event) {
        Creature creature = event.getCreature();
        RouteState state = routes.get(creature.getWurmId());
        if (state == null) return;

        if (pauseInCombat && creature.getTarget() != null) return;

        int[] wp = state.currentWaypoint();
        int tx = creature.getTileX();
        int ty = creature.getTileY();
        int dx = tx - wp[0];
        int dy = ty - wp[1];
        int distSq = dx * dx + dy * dy;
        int threshSq = arrivalDistance * arrivalDistance;

        if (distSq <= threshSq) {
            state.advance();
            wp = state.currentWaypoint();
        }

        PathTile target = buildPathTile(creature, wp[0], wp[1]);
        if (target == null) return;

        try {
            creature.startPathingToTile(target);
            event.setCancelled(true);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "TemplarPatrol: startPathingToTile failed for " + creature.getWurmId(), e);
        }
    }

    private PathTile buildPathTile(Creature creature, int tx, int ty) {
        try {
            boolean onSurface = creature.isOnSurface();
            int tile = onSurface
                ? Server.surfaceMesh.getTile(tx, ty)
                : Server.caveMesh.getTile(tx, ty);
            return new PathTile(tx, ty, tile, onSurface, creature.getFloorLevel());
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "TemplarPatrol: buildPathTile failed at " + tx + "," + ty, e);
            return null;
        }
    }

    private static int parseInt(String s, int fallback) {
        if (s == null) return fallback;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return fallback; }
    }

    private static Long toLong(Object key) {
        if (key instanceof Number) return ((Number) key).longValue();
        if (key instanceof String) {
            try { return Long.parseLong(((String) key).trim()); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private static List<int[]> toWaypoints(Object raw) {
        List<int[]> out = new ArrayList<>();
        if (!(raw instanceof List)) return out;
        for (Object wp : (List<?>) raw) {
            if (!(wp instanceof List)) continue;
            List<?> pair = (List<?>) wp;
            if (pair.size() < 2) continue;
            Object x = pair.get(0);
            Object y = pair.get(1);
            if (x instanceof Number && y instanceof Number) {
                out.add(new int[] { ((Number) x).intValue(), ((Number) y).intValue() });
            }
        }
        return out;
    }

    static final class RouteState {
        private final List<int[]> waypoints;
        private int index;

        RouteState(List<int[]> waypoints) {
            this.waypoints = waypoints;
            this.index = 0;
        }

        int[] currentWaypoint() {
            return waypoints.get(index);
        }

        void advance() {
            index = (index + 1) % waypoints.size();
        }
    }
}
