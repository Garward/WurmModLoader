package com.garward.wurmmodloader.config;

/**
 * Exception thrown when configuration loading or validation fails.
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class ConfigException extends Exception {

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
