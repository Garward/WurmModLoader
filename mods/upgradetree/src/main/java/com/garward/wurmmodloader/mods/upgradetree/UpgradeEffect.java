package com.garward.wurmmodloader.mods.upgradetree;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single effect from an upgrade.
 * Effects are data-driven from JSON config.
 *
 * @author Power Fantasy RPG Team
 * @since 1.0.0
 */
public class UpgradeEffect {
    private final String type;
    private final Map<String, Object> parameters;

    public UpgradeEffect(String type, Map<String, Object> parameters) {
        this.type = type;
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Get parameter as double (for numeric values).
     */
    public double getDouble(String key, double defaultValue) {
        Object val = parameters.get(key);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return defaultValue;
    }

    /**
     * Get parameter as float (for numeric values).
     */
    public float getFloat(String key, float defaultValue) {
        return (float) getDouble(key, defaultValue);
    }

    /**
     * Get parameter as int (for counts/thresholds).
     */
    public int getInt(String key, int defaultValue) {
        Object val = parameters.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return defaultValue;
    }

    /**
     * Get parameter as string.
     */
    public String getString(String key, String defaultValue) {
        Object val = parameters.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    @Override
    public String toString() {
        return "UpgradeEffect{" +
               "type='" + type + '\'' +
               ", parameters=" + parameters +
               '}';
    }
}
