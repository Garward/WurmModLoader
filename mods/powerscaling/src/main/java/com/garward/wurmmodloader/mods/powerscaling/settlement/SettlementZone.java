package com.garward.wurmmodloader.mods.powerscaling.settlement;

/**
 * Represents a settlement (village or deed) zone for power suppression.
 *
 * <p>Settlements create "safe zones" where creature power is suppressed.
 * This encourages players to explore dangerous wilderness areas while
 * keeping starter zones relatively safe.</p>
 *
 * <p><strong>Design Note:</strong> This is an RPG power fantasy feature,
 * not a vanilla game mechanic. It lives in the mod, not the framework.</p>
 *
 * @author Power Fantasy RPG Team
 * @since 1.0.0
 */
public class SettlementZone {

    private final int id;
    private final String name;
    private final int centerX;
    private final int centerY;
    private final int radius;
    private final SettlementType type;
    private final long createdTimestamp;

    /**
     * Type of settlement zone.
     */
    public enum SettlementType {
        /** Player village (with token) */
        VILLAGE,

        /** Player deed (individual ownership) */
        DEED,

        /** Starter town (Freedom Isles spawn zones) */
        STARTER_TOWN,

        /** Custom zone (admin-defined) */
        CUSTOM
    }

    /**
     * Create a new settlement zone.
     *
     * @param id Unique settlement ID (village ID or deed ID)
     * @param name Settlement name
     * @param centerX Center tile X coordinate
     * @param centerY Center tile Y coordinate
     * @param radius Effective radius in tiles
     * @param type Settlement type
     */
    public SettlementZone(int id, String name, int centerX, int centerY, int radius, SettlementType type) {
        this.id = id;
        this.name = name;
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.type = type;
        this.createdTimestamp = System.currentTimeMillis();
    }

    /**
     * Get settlement ID.
     *
     * @return The unique ID
     */
    public int getId() {
        return id;
    }

    /**
     * Get settlement name.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Get center X coordinate.
     *
     * @return Center tile X
     */
    public int getCenterX() {
        return centerX;
    }

    /**
     * Get center Y coordinate.
     *
     * @return Center tile Y
     */
    public int getCenterY() {
        return centerY;
    }

    /**
     * Get effective radius.
     *
     * @return Radius in tiles
     */
    public int getRadius() {
        return radius;
    }

    /**
     * Get settlement type.
     *
     * @return The type
     */
    public SettlementType getType() {
        return type;
    }

    /**
     * Get creation timestamp.
     *
     * @return Timestamp in milliseconds
     */
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    /**
     * Calculate distance from this settlement to a point.
     *
     * <p>Uses Euclidean distance formula:
     * <pre>
     * distance = sqrt((x2 - x1)^2 + (y2 - y1)^2)
     * </pre>
     *
     * @param tileX Target X coordinate
     * @param tileY Target Y coordinate
     * @return Distance in tiles
     */
    public int getDistanceFrom(int tileX, int tileY) {
        int dx = tileX - centerX;
        int dy = tileY - centerY;
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Check if a point is within this settlement's radius.
     *
     * @param tileX Target X coordinate
     * @param tileY Target Y coordinate
     * @return true if within radius
     */
    public boolean contains(int tileX, int tileY) {
        return getDistanceFrom(tileX, tileY) <= radius;
    }

    /**
     * Get the grid cell this settlement belongs to.
     *
     * <p>Used by SettlementIndexer for spatial partitioning.</p>
     *
     * @param gridSize The grid cell size
     * @return Grid cell coordinates [cellX, cellY]
     */
    public int[] getGridCell(int gridSize) {
        return new int[] {
            centerX / gridSize,
            centerY / gridSize
        };
    }

    @Override
    public String toString() {
        return String.format("Settlement[id=%d, name='%s', type=%s, center=(%d,%d), radius=%d]",
            id, name, type, centerX, centerY, radius);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SettlementZone)) return false;
        SettlementZone other = (SettlementZone) obj;
        return id == other.id && type == other.type;
    }

    @Override
    public int hashCode() {
        return 31 * id + type.hashCode();
    }
}
