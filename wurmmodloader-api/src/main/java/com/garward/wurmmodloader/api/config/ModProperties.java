package com.garward.wurmmodloader.api.config;

import java.util.Properties;

/**
 * Minimal typed wrapper for {@link Properties} mirroring the legacy Prop helper.
 */
public final class ModProperties {

    private final Properties properties;

    private ModProperties(Properties properties) {
        this.properties = properties != null ? properties : new Properties();
    }

    public static ModProperties from(Properties properties) {
        return new ModProperties(properties);
    }

    public boolean getBoolean(String key, boolean def) {
        return Boolean.parseBoolean(properties.getProperty(key, Boolean.toString(def)));
    }

    public String getString(String key, String def) {
        return properties.getProperty(key, def);
    }

    public int getInt(String key, int def) {
        return Integer.parseInt(properties.getProperty(key, Integer.toString(def)));
    }

    public long getLong(String key, long def) {
        return Long.parseLong(properties.getProperty(key, Long.toString(def)));
    }

    public float getFloat(String key, float def) {
        return Float.parseFloat(properties.getProperty(key, Float.toString(def)));
    }
}
