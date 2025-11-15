package com.garward.wurmmodloader.api.events.player;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fires when prayer faith counts are being set or checked.
 *
 * <p>Allows mods to modify prayer restrictions (count limits and time delays).</p>
 *
 * <p>Example use cases:</p>
 * <ul>
 *   <li>Remove prayer count limits (unlimited prayers)</li>
 *   <li>Remove prayer time delays (instant prayers)</li>
 *   <li>Modify prayer costs based on conditions</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class PrayerFaithEvent extends Event {

    private final long playerId;
    private byte numFaith;
    private long lastFaith;

    /**
     * Creates a new PrayerFaithEvent.
     *
     * @param playerId The player ID
     * @param numFaith The current prayer count (0-5 normally)
     * @param lastFaith The timestamp of last prayer
     */
    public PrayerFaithEvent(long playerId, byte numFaith, long lastFaith) {
        this.playerId = playerId;
        this.numFaith = numFaith;
        this.lastFaith = lastFaith;
    }

    /**
     * Gets the player ID.
     *
     * @return The player ID
     */
    public long getPlayerId() {
        return playerId;
    }

    /**
     * Gets the prayer count.
     *
     * <p>Normal range is 0-5. When numFaith reaches 5, player cannot pray until time passes.</p>
     *
     * @return The prayer count
     */
    public byte getNumFaith() {
        return numFaith;
    }

    /**
     * Sets the prayer count.
     *
     * <p>Set to 0 to allow unlimited prayers.</p>
     *
     * @param numFaith The new prayer count
     */
    public void setNumFaith(byte numFaith) {
        this.numFaith = numFaith;
    }

    /**
     * Gets the last prayer timestamp.
     *
     * <p>Used to enforce time delays between prayers.</p>
     *
     * @return The timestamp (System.currentTimeMillis() format)
     */
    public long getLastFaith() {
        return lastFaith;
    }

    /**
     * Sets the last prayer timestamp.
     *
     * <p>Set to 0 to remove prayer time delays.</p>
     *
     * @param lastFaith The new timestamp
     */
    public void setLastFaith(long lastFaith) {
        this.lastFaith = lastFaith;
    }
}
