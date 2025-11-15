package com.garward.wurmmodloader.api.events;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic action event for mod-to-mod API communication.
 *
 * <p>Mods can fire this event to request actions from other mods. The handling mod
 * can cancel the event if the action cannot be performed, with an optional reason.</p>
 *
 * <p><strong>Example Usage (Spending Power):</strong></p>
 * <pre>{@code
 * // Request to spend power
 * ModActionEvent action = new ModActionEvent("powerscaling:spend_power");
 * action.set("playerWurmId", player.getWurmId());
 * action.set("amount", 100);
 * action.set("reason", "Upgrade Tree");
 * EventBus.getInstance().post(action);
 *
 * if (!action.isCancelled()) {
 *     // Action succeeded!
 *     int remaining = action.getInt("remainingPower");
 *     player.sendMessage("Spent 100 power. Remaining: " + remaining);
 * } else {
 *     // Action failed
 *     player.sendMessage("Failed: " + action.getCancelReason());
 * }
 * }</pre>
 *
 * <p><strong>Example Handler (PowerScaling):</strong></p>
 * <pre>{@code
 * @SubscribeEvent
 * public void onModAction(ModActionEvent event) {
 *     if (event.getEventType().equals("powerscaling:spend_power")) {
 *         long playerId = event.getLong("playerWurmId");
 *         int amount = event.getInt("amount");
 *
 *         if (amount <= 0) {
 *             event.setCancelled(true);
 *             event.setCancelReason("Invalid amount");
 *             return;
 *         }
 *
 *         int current = manager.getPlayerPowerLevel(playerId);
 *         if (current < amount) {
 *             event.setCancelled(true);
 *             event.setCancelReason("Not enough power");
 *             return;
 *         }
 *
 *         manager.spendPower(playerId, amount);
 *         event.set("success", true);
 *         event.set("remainingPower", manager.getPlayerPowerLevel(playerId));
 *         event.setHandled(true);
 *     }
 * }
 * }</pre>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class ModActionEvent extends CancellableEvent {

    private final String eventType;
    private final Map<String, Object> data;
    private boolean handled = false;
    private String cancelReason = null;

    /**
     * Create a mod action event.
     *
     * @param eventType The event type (namespace:action format recommended, e.g., "powerscaling:spend_power")
     */
    public ModActionEvent(String eventType) {
        this.eventType = eventType;
        this.data = new HashMap<>();
    }

    /**
     * Get the event type identifier.
     *
     * @return The event type (e.g., "powerscaling:spend_power")
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Set a data value.
     *
     * @param key The data key
     * @param value The value
     */
    public void set(String key, Object value) {
        data.put(key, value);
    }

    /**
     * Get a data value.
     *
     * @param key The data key
     * @return The value, or null if not set
     */
    public Object get(String key) {
        return data.get(key);
    }

    /**
     * Get an integer value.
     *
     * @param key The data key
     * @return The integer value
     * @throws ClassCastException if value is not an Integer
     * @throws NullPointerException if value is null
     */
    public int getInt(String key) {
        return (Integer) data.get(key);
    }

    /**
     * Get a long value.
     *
     * @param key The data key
     * @return The long value
     * @throws ClassCastException if value is not a Long
     * @throws NullPointerException if value is null
     */
    public long getLong(String key) {
        return (Long) data.get(key);
    }

    /**
     * Get a string value.
     *
     * @param key The data key
     * @return The string value
     * @throws ClassCastException if value is not a String
     */
    public String getString(String key) {
        return (String) data.get(key);
    }

    /**
     * Get a boolean value.
     *
     * @param key The data key
     * @return The boolean value
     * @throws ClassCastException if value is not a Boolean
     * @throws NullPointerException if value is null
     */
    public boolean getBoolean(String key) {
        return (Boolean) data.get(key);
    }

    /**
     * Check if a key exists.
     *
     * @param key The data key
     * @return true if the key has been set
     */
    public boolean has(String key) {
        return data.containsKey(key);
    }

    /**
     * Check if the action was handled by a mod.
     *
     * @return true if a mod responded to this action
     */
    public boolean isHandled() {
        return handled;
    }

    /**
     * Mark this action as handled (called by responding mod).
     *
     * @param handled true if handled
     */
    public void setHandled(boolean handled) {
        this.handled = handled;
    }

    /**
     * Get the reason why the action was cancelled.
     *
     * @return Cancel reason (null if not cancelled)
     */
    public String getCancelReason() {
        return cancelReason;
    }

    /**
     * Set the reason for cancelling.
     *
     * @param reason The cancellation reason
     */
    public void setCancelReason(String reason) {
        this.cancelReason = reason;
    }
}
