package com.garward.wurmmodloader.mods.powerscaling.events;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Request event for querying a player's current power level.
 *
 * <p>Other mods can fire this event to get a player's power level without
 * directly depending on PowerScaling classes (avoids classloader issues).</p>
 *
 * <p><strong>Usage Pattern:</strong></p>
 * <pre>{@code
 * // Fire the event to request power level
 * PowerQueryEvent query = new PowerQueryEvent(playerWurmId);
 * EventBus.getInstance().post(query);
 *
 * // PowerScaling will populate the power level
 * int currentPower = query.getPowerLevel();
 * }</pre>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class PowerQueryEvent extends Event {

    private final long playerWurmId;
    private int powerLevel = 0;
    private boolean handled = false;

    /**
     * Create a power query event.
     *
     * @param playerWurmId The player's wurm ID
     */
    public PowerQueryEvent(long playerWurmId) {
        this.playerWurmId = playerWurmId;
    }

    /**
     * Get the player wurm ID being queried.
     *
     * @return The player wurm ID
     */
    public long getPlayerWurmId() {
        return playerWurmId;
    }

    /**
     * Get the power level (set by PowerScaling mod).
     *
     * @return The player's current power level
     */
    public int getPowerLevel() {
        return powerLevel;
    }

    /**
     * Set the power level (called by PowerScaling mod).
     *
     * @param powerLevel The power level
     */
    public void setPowerLevel(int powerLevel) {
        this.powerLevel = powerLevel;
        this.handled = true;
    }

    /**
     * Check if the query was handled by PowerScaling.
     *
     * @return true if PowerScaling responded
     */
    public boolean isHandled() {
        return handled;
    }
}
