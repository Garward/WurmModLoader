package com.garward.wurmmodloader.mods.powerscaling.events;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

/**
 * Request event for spending a player's power.
 *
 * <p>Other mods can fire this event to spend power without directly depending
 * on PowerScaling classes (avoids classloader issues).</p>
 *
 * <p><strong>Cancellable:</strong> PowerScaling will cancel this event if:
 * <ul>
 *   <li>Player doesn't have enough power</li>
 *   <li>Power system is not initialized</li>
 *   <li>Amount is invalid (negative or zero)</li>
 * </ul>
 * </p>
 *
 * <p><strong>Usage Pattern:</strong></p>
 * <pre>{@code
 * // Fire the event to request spending power
 * PowerSpendRequestEvent request = new PowerSpendRequestEvent(playerWurmId, cost, "Upgrade Tree");
 * EventBus.getInstance().post(request);
 *
 * if (!request.isCancelled()) {
 *     // Power was successfully spent!
 *     player.sendMessage("Spent " + cost + " power");
 * } else {
 *     // Spending failed
 *     player.sendMessage("Not enough power! " + request.getCancelReason());
 * }
 * }</pre>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class PowerSpendRequestEvent extends CancellableEvent {

    private final long playerWurmId;
    private final int amount;
    private final String reason;
    private boolean success = false;
    private int remainingPower = 0;
    private String cancelReason = null;

    /**
     * Create a power spend request event.
     *
     * @param playerWurmId The player's wurm ID
     * @param amount Amount of power to spend
     * @param reason Description of what the power is being spent on
     */
    public PowerSpendRequestEvent(long playerWurmId, int amount, String reason) {
        this.playerWurmId = playerWurmId;
        this.amount = amount;
        this.reason = reason;
    }

    /**
     * Get the player wurm ID.
     *
     * @return The player wurm ID
     */
    public long getPlayerWurmId() {
        return playerWurmId;
    }

    /**
     * Get the amount of power to spend.
     *
     * @return The power cost
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Get the reason for spending power.
     *
     * @return Description (e.g., "Upgrade Tree", "Skill Purchase")
     */
    public String getReason() {
        return reason;
    }

    /**
     * Check if the power was successfully spent.
     *
     * @return true if spending succeeded
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Set the success status (called by PowerScaling mod).
     *
     * @param success true if power was spent
     * @param remainingPower Player's remaining power after spending
     */
    public void setSuccess(boolean success, int remainingPower) {
        this.success = success;
        this.remainingPower = remainingPower;
    }

    /**
     * Get the player's remaining power after spending.
     *
     * @return Remaining power (only valid if success = true)
     */
    public int getRemainingPower() {
        return remainingPower;
    }

    /**
     * Get the reason why the request was cancelled.
     *
     * @return Cancel reason (null if not cancelled)
     */
    public String getCancelReason() {
        return cancelReason;
    }

    /**
     * Set the reason for cancelling (called by PowerScaling mod).
     *
     * @param reason The cancellation reason
     */
    public void setCancelReason(String reason) {
        this.cancelReason = reason;
    }
}
