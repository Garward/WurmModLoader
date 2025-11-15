package com.garward.wurmmodloader.mods.powerscaling.settlement;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spatial index for fast settlement distance lookups.
 *
 * <p>Uses grid-based partitioning to achieve O(1) average case performance
 * for distance queries. Without this, checking distance to all settlements
 * would be O(N) per creature spawn, which becomes expensive with many villages.</p>
 *
 * <p><strong>Algorithm:</strong></p>
 * <ul>
 *   <li>Divide world into grid cells (default 100×100 tiles)</li>
 *   <li>Each settlement is indexed into grid cells it overlaps</li>
 *   <li>Distance query: check settlements in target cell + adjacent cells</li>
 *   <li>Average complexity: O(1), worst case: O(K) where K = settlements per cell</li>
 * </ul>
 *
 * <p><strong>Example:</strong></p>
 * <pre>
 * World: 2048×2048 tiles, grid size 100
 * Grid: 21×21 cells
 * Village at (500, 500) with radius 50
 *   - Indexes into cells: (4,4), (4,5), (5,4), (5,5)
 * Query at (520, 520):
 *   - Check cell (5,5) - finds village
 *   - Distance = 28 tiles
 * </pre>
 *
 * @author Power Fantasy RPG Team
 * @since 1.0.0
 */
public class SettlementIndexer {

    /** Grid cell size in tiles (100×100 tile cells) */
    public static final int GRID_SIZE = 100;

    /** Grid storage: [cellX][cellY] -> list of settlements */
    private final Map<GridCell, List<SettlementZone>> grid = new ConcurrentHashMap<>();

    /**
     * Grid cell coordinate.
     */
    private static class GridCell {
        final int cellX;
        final int cellY;

        GridCell(int cellX, int cellY) {
            this.cellX = cellX;
            this.cellY = cellY;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof GridCell)) return false;
            GridCell other = (GridCell) obj;
            return cellX == other.cellX && cellY == other.cellY;
        }

        @Override
        public int hashCode() {
            return 31 * cellX + cellY;
        }

        @Override
        public String toString() {
            return String.format("(%d,%d)", cellX, cellY);
        }
    }

    /**
     * Add a settlement to the index.
     *
     * <p>Indexes the settlement into all grid cells it overlaps based on its radius.</p>
     *
     * @param zone The settlement to add
     */
    public void add(SettlementZone zone) {
        // Calculate grid cells covered by this settlement
        int minCellX = (zone.getCenterX() - zone.getRadius()) / GRID_SIZE;
        int maxCellX = (zone.getCenterX() + zone.getRadius()) / GRID_SIZE;
        int minCellY = (zone.getCenterY() - zone.getRadius()) / GRID_SIZE;
        int maxCellY = (zone.getCenterY() + zone.getRadius()) / GRID_SIZE;

        // Add to all overlapping cells
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
                GridCell cell = new GridCell(cellX, cellY);
                grid.computeIfAbsent(cell, k -> new ArrayList<>()).add(zone);
            }
        }
    }

    /**
     * Remove a settlement from the index.
     *
     * @param zone The settlement to remove
     */
    public void remove(SettlementZone zone) {
        // Calculate grid cells covered by this settlement
        int minCellX = (zone.getCenterX() - zone.getRadius()) / GRID_SIZE;
        int maxCellX = (zone.getCenterX() + zone.getRadius()) / GRID_SIZE;
        int minCellY = (zone.getCenterY() - zone.getRadius()) / GRID_SIZE;
        int maxCellY = (zone.getCenterY() + zone.getRadius()) / GRID_SIZE;

        // Remove from all overlapping cells
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
                GridCell cell = new GridCell(cellX, cellY);
                List<SettlementZone> zones = grid.get(cell);
                if (zones != null) {
                    zones.remove(zone);
                    if (zones.isEmpty()) {
                        grid.remove(cell);
                    }
                }
            }
        }
    }

    /**
     * Get the distance to the nearest settlement from a point.
     *
     * <p>Checks the target grid cell and all adjacent cells for settlements,
     * then returns the minimum distance found.</p>
     *
     * @param tileX Target X coordinate
     * @param tileY Target Y coordinate
     * @return Distance to nearest settlement in tiles, or Integer.MAX_VALUE if none found
     */
    public int getDistanceToNearest(int tileX, int tileY) {
        GridCell targetCell = new GridCell(tileX / GRID_SIZE, tileY / GRID_SIZE);

        // Collect settlements from target cell and adjacent cells
        Set<SettlementZone> nearbySettlements = new HashSet<>();

        // Check 3×3 grid around target cell
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                GridCell cell = new GridCell(targetCell.cellX + dx, targetCell.cellY + dy);
                List<SettlementZone> zones = grid.get(cell);
                if (zones != null) {
                    nearbySettlements.addAll(zones);
                }
            }
        }

        // No settlements nearby
        if (nearbySettlements.isEmpty()) {
            return Integer.MAX_VALUE;
        }

        // Find minimum distance
        int minDistance = Integer.MAX_VALUE;
        for (SettlementZone zone : nearbySettlements) {
            int distance = zone.getDistanceFrom(tileX, tileY);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }

        return minDistance;
    }

    /**
     * Get the nearest settlement to a point.
     *
     * @param tileX Target X coordinate
     * @param tileY Target Y coordinate
     * @return The nearest settlement, or null if none found
     */
    public SettlementZone getNearest(int tileX, int tileY) {
        GridCell targetCell = new GridCell(tileX / GRID_SIZE, tileY / GRID_SIZE);

        // Collect settlements from target cell and adjacent cells
        Set<SettlementZone> nearbySettlements = new HashSet<>();

        // Check 3×3 grid around target cell
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                GridCell cell = new GridCell(targetCell.cellX + dx, targetCell.cellY + dy);
                List<SettlementZone> zones = grid.get(cell);
                if (zones != null) {
                    nearbySettlements.addAll(zones);
                }
            }
        }

        // No settlements nearby
        if (nearbySettlements.isEmpty()) {
            return null;
        }

        // Find nearest settlement
        SettlementZone nearest = null;
        int minDistance = Integer.MAX_VALUE;

        for (SettlementZone zone : nearbySettlements) {
            int distance = zone.getDistanceFrom(tileX, tileY);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = zone;
            }
        }

        return nearest;
    }

    /**
     * Get all settlements within a certain distance of a point.
     *
     * @param tileX Target X coordinate
     * @param tileY Target Y coordinate
     * @param maxDistance Maximum distance in tiles
     * @return List of settlements within range
     */
    public List<SettlementZone> getWithinDistance(int tileX, int tileY, int maxDistance) {
        GridCell targetCell = new GridCell(tileX / GRID_SIZE, tileY / GRID_SIZE);

        // Calculate grid radius to check
        int gridRadius = (maxDistance / GRID_SIZE) + 1;

        // Collect settlements from all cells within range
        Set<SettlementZone> nearbySettlements = new HashSet<>();

        for (int dx = -gridRadius; dx <= gridRadius; dx++) {
            for (int dy = -gridRadius; dy <= gridRadius; dy++) {
                GridCell cell = new GridCell(targetCell.cellX + dx, targetCell.cellY + dy);
                List<SettlementZone> zones = grid.get(cell);
                if (zones != null) {
                    nearbySettlements.addAll(zones);
                }
            }
        }

        // Filter by actual distance
        List<SettlementZone> result = new ArrayList<>();
        for (SettlementZone zone : nearbySettlements) {
            if (zone.getDistanceFrom(tileX, tileY) <= maxDistance) {
                result.add(zone);
            }
        }

        return result;
    }

    /**
     * Clear all settlements from the index.
     */
    public void clear() {
        grid.clear();
    }

    /**
     * Get total number of settlements indexed.
     *
     * @return Settlement count
     */
    public int getSettlementCount() {
        Set<SettlementZone> unique = new HashSet<>();
        for (List<SettlementZone> zones : grid.values()) {
            unique.addAll(zones);
        }
        return unique.size();
    }

    /**
     * Get total number of grid cells in use.
     *
     * @return Cell count
     */
    public int getCellCount() {
        return grid.size();
    }

    /**
     * Get debug statistics.
     *
     * @return Debug string
     */
    public String getDebugStats() {
        int uniqueSettlements = getSettlementCount();
        int cells = getCellCount();
        double avgPerCell = cells > 0 ? (double) uniqueSettlements / cells : 0.0;

        return String.format(
            "SettlementIndexer[settlements=%d, cells=%d, avg=%.2f per cell]",
            uniqueSettlements, cells, avgPerCell
        );
    }
}
