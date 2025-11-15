package com.garward.wurmmodloader.api.events;

import com.garward.wurmmodloader.api.events.base.Event;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic query event for mod-to-mod API communication.
 *
 * <p>Mods can fire this event to query data from other mods without direct dependencies.
 * Each mod defines its own event types (namespaced strings) and data contracts.</p>
 *
 * <p><strong>Example Usage (PowerScaling):</strong></p>
 * <pre>{@code
 * // Query a player's power level
 * ModQueryEvent query = new ModQueryEvent("powerscaling:power_level");
 * query.set("playerWurmId", player.getWurmId());
 * EventBus.getInstance().post(query);
 *
 * if (query.isHandled()) {
 *     int power = query.getInt("powerLevel");
 *     player.sendMessage("You have " + power + " power");
 * }
 * }</pre>
 *
 * <p><strong>Example Handler (PowerScaling):</strong></p>
 * <pre>{@code
 * @SubscribeEvent
 * public void onModQuery(ModQueryEvent event) {
 *     if (event.getEventType().equals("powerscaling:power_level")) {
 *         long playerId = event.getLong("playerWurmId");
 *         int power = manager.getPlayerPowerLevel(playerId);
 *         event.set("powerLevel", power);
 *         event.setHandled(true);
 *     }
 * }
 * }</pre>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class ModQueryEvent extends Event {

    private final String eventType;
    private final Map<String, Object> data;
    private boolean handled = false;

    /**
     * Create a mod query event.
     *
     * @param eventType The event type (namespace:action format recommended, e.g., "powerscaling:power_level")
     */
    public ModQueryEvent(String eventType) {
        this.eventType = eventType;
        this.data = new HashMap<>();
    }

    /**
     * Get the event type identifier.
     *
     * @return The event type (e.g., "powerscaling:power_level")
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
     * Check if the query was handled by a mod.
     *
     * @return true if a mod responded to this query
     */
    public boolean isHandled() {
        return handled;
    }

    /**
     * Mark this query as handled (called by responding mod).
     *
     * @param handled true if handled
     */
    public void setHandled(boolean handled) {
        this.handled = handled;
    }
}
