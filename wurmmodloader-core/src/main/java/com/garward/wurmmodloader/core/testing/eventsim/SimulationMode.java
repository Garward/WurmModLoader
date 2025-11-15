package com.garward.wurmmodloader.core.testing.eventsim;

/**
 * Defines the different modes for event simulation testing.
 *
 * <p>Each mode controls which events are fired and with what probability
 * of edge cases and rare scenarios being tested.
 *
 * @since 1.0.0
 */
public enum SimulationMode {
    /**
     * Normal mode - fires common events with typical game scenarios.
     * <ul>
     *   <li>Player login/logout</li>
     *   <li>Creature death with killer</li>
     *   <li>Item examine</li>
     *   <li>Skill checks and advances</li>
     *   <li>Combat events</li>
     * </ul>
     */
    NORMAL,

    /**
     * Rare mode - increases probability of edge cases and rare scenarios.
     * <ul>
     *   <li>Creature death with null killer (environmental death)</li>
     *   <li>Unique creature deaths</li>
     *   <li>Champion creature events</li>
     *   <li>Server start/stop events</li>
     *   <li>Special move events</li>
     *   <li>Vehicle events</li>
     * </ul>
     */
    RARE,

    /**
     * Full sweep mode - systematically fires ALL registered events.
     * <ul>
     *   <li>Discovers all event classes</li>
     *   <li>Fires each event type multiple times</li>
     *   <li>Tests with both valid data and edge cases</li>
     *   <li>Comprehensive validation of all parameters</li>
     * </ul>
     */
    FULL_SWEEP;

    /**
     * Parses a simulation mode from a string.
     *
     * @param value the string value (case-insensitive)
     * @return the simulation mode, or NORMAL if invalid
     */
    public static SimulationMode parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NORMAL;
        }
        try {
            return valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
}
