package com.garward.wurmmodloader.mods.livemap.data;

import java.util.ArrayList;
import java.util.List;

import com.wurmonline.server.Items;
import com.wurmonline.server.Players;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.villages.Villages;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zone;
import com.wurmonline.server.zones.Zones;

/**
 * Collects live map data from the server.
 */
public class MapData {

    /**
     * Get all online players with their positions
     */
    public static List<PlayerData> getPlayers() {
        return getPlayersForViewer(null);
    }

    /**
     * Online-player positions filtered to fellow villagers of {@code viewerName}.
     * Passing {@code null} returns an empty list — player positions are a
     * privileged piece of data; only the in-game client sends a viewer name.
     * If the viewer has no citizen village, returns only the viewer themself.
     */
    public static List<PlayerData> getPlayersForViewer(String viewerName) {
        List<PlayerData> players = new ArrayList<>();
        if (viewerName == null || viewerName.isEmpty()) return players;

        Village viewerVillage = null;
        try {
            Player viewer = Players.getInstance().getPlayer(viewerName);
            if (viewer != null) viewerVillage = viewer.getCitizenVillage();
        } catch (Throwable ignored) { /* not logged in / lookup failed */ }

        for (Player player : Players.getInstance().getPlayers()) {
            if (player == null || player.isOffline()) continue;
            try {
                // Include self always; others only if sharing a village.
                boolean isSelf = viewerName.equals(player.getName());
                if (!isSelf) {
                    if (viewerVillage == null) continue;
                    Village pv = player.getCitizenVillage();
                    if (pv == null || pv.getId() != viewerVillage.getId()) continue;
                }
                VolaTile tile = Zones.getTileOrNull(player.getTileX(), player.getTileY(), player.isOnSurface());
                if (tile == null) continue;
                players.add(new PlayerData(
                    player.getName(),
                    player.getTileX(),
                    player.getTileY(),
                    player.isOnSurface(),
                    player.getKingdomId()
                ));
            } catch (Exception ignored) { /* skip invalid */ }
        }
        return players;
    }

    /**
     * Get all villages/deeds
     */
    public static List<VillageData> getVillages() {
        List<VillageData> villages = new ArrayList<>();

        for (Village village : Villages.getVillages()) {
            if (village == null) continue;
            String motto = "";
            try { motto = village.getMotto() == null ? "" : village.getMotto(); } catch (Throwable ignored) {}
            int citizens = 0;
            try {
                com.wurmonline.server.villages.Citizen[] c = village.getCitizens();
                citizens = c == null ? 0 : c.length;
            } catch (Throwable ignored) {}
            int tokenX = (village.getStartX() + village.getEndX()) / 2;
            int tokenY = (village.getStartY() + village.getEndY()) / 2;
            try { tokenX = village.getTokenX(); tokenY = village.getTokenY(); } catch (Throwable ignored) {}
            villages.add(new VillageData(
                village.getName(),
                village.getStartX(), village.getStartY(),
                village.getEndX(),   village.getEndY(),
                village.isDemocracy() ? "democracy" : "monarchy",
                village.getMayor() != null ? village.getMayor().getName() : "Unknown",
                motto,
                citizens,
                village.isPermanent,
                tokenX, tokenY
            ));
        }

        return villages;
    }

    /** All guard towers on the surface — location, kingdom, damage. */
    public static List<GuardTowerData> getGuardTowers() {
        List<GuardTowerData> towers = new ArrayList<>();
        try {
            for (Item item : Items.getAllItems()) {
                if (item == null || item.deleted) continue;
                if (!item.isOnSurface()) continue;
                if (!item.isGuardTower()) continue;
                towers.add(new GuardTowerData(
                    item.getName(),
                    item.getTileX(),
                    item.getTileY(),
                    item.getKingdom(),
                    item.getDamage()
                ));
            }
        } catch (Throwable ignored) {}
        return towers;
    }

    /**
     * Get all special altars and structures
     */
    public static List<AltarData> getAltars() {
        List<AltarData> altars = new ArrayList<>();

        try {
            // Search through all items for altars
            for (Item item : Items.getAllItems()) {
                if (item != null && !item.deleted && item.isOnSurface()) {
                    String name = item.getName().toLowerCase();

                    // Check for specific altar types
                    if (name.contains("altar of three") || name.contains("huge bone altar")) {
                        int tileX = item.getTileX();
                        int tileY = item.getTileY();

                        altars.add(new AltarData(
                            item.getName(),
                            tileX,
                            tileY,
                            getAltarType(name)
                        ));
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors, return what we found
        }

        return altars;
    }

    /**
     * Determine altar type from name
     */
    private static String getAltarType(String name) {
        if (name.contains("altar of three")) {
            return "altar_of_three";
        } else if (name.contains("huge bone altar")) {
            return "huge_bone_altar";
        }
        return "unknown";
    }

    /**
     * Player position data
     */
    public static class PlayerData {
        public final String name;
        public final int x;
        public final int y;
        public final boolean surface;
        public final byte kingdom;

        public PlayerData(String name, int x, int y, boolean surface, byte kingdom) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.surface = surface;
            this.kingdom = kingdom;
        }
    }

    /**
     * Village/deed data
     */
    public static class VillageData {
        public final String name;
        public final int startX;
        public final int startY;
        public final int endX;
        public final int endY;
        public final String type;
        public final String mayor;
        public final String motto;
        public final int citizens;
        public final boolean permanent;
        public final int tokenX;
        public final int tokenY;

        public VillageData(String name, int startX, int startY, int endX, int endY,
                           String type, String mayor, String motto, int citizens,
                           boolean permanent, int tokenX, int tokenY) {
            this.name = name;
            this.startX = startX; this.startY = startY;
            this.endX = endX;     this.endY = endY;
            this.type = type;
            this.mayor = mayor;
            this.motto = motto;
            this.citizens = citizens;
            this.permanent = permanent;
            this.tokenX = tokenX; this.tokenY = tokenY;
        }
    }

    public static class GuardTowerData {
        public final String name;
        public final int x;
        public final int y;
        public final byte kingdom;
        public final float damage;

        public GuardTowerData(String name, int x, int y, byte kingdom, float damage) {
            this.name = name;
            this.x = x; this.y = y;
            this.kingdom = kingdom;
            this.damage = damage;
        }
    }

    /**
     * Altar/structure data
     */
    public static class AltarData {
        public final String name;
        public final int x;
        public final int y;
        public final String type;

        public AltarData(String name, int x, int y, String type) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }
}
