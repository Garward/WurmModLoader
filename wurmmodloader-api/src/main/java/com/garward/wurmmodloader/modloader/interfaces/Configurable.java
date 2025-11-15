package com.garward.wurmmodloader.modloader.interfaces;

import java.util.Properties;

/**
 * Interface for mods that need to load configuration from properties files.
 *
 * <p>This interface extends the internal Configurable interface to ensure compatibility
 * with the ModLoader's lifecycle management.</p>
 *
 * <p>Mods implementing this interface will have their {@link #configure(Properties)}
 * method called during mod loading, before {@link WurmServerMod#preInit()}.
 * The properties object contains all key-value pairs from both the mod's
 * {@code .properties} file and optional {@code .config} file.</p>
 *
 * <p><strong>Lifecycle Order:</strong></p>
 * <ol>
 *   <li>{@link #configure(Properties)} - Load configuration</li>
 *   <li>{@link WurmServerMod#preInit()} - Register bytecode hooks</li>
 *   <li>{@link WurmServerMod#init()} - Register content and actions</li>
 * </ol>
 *
 * <p><strong>Configuration Files:</strong></p>
 * <ul>
 *   <li>{@code modname.properties} - Required file with classname and classpath</li>
 *   <li>{@code modname.config} - Optional configuration file for mod settings</li>
 * </ul>
 *
 * <p><strong>Example:</strong></p>
 * <pre>{@code
 * public class MyMod implements WurmServerMod, Configurable {
 *     private boolean enableFeature;
 *     private int maxLevel;
 *
 *     @Override
 *     public void configure(Properties properties) {
 *         enableFeature = Boolean.parseBoolean(
 *             properties.getProperty("enableFeature", "true"));
 *         maxLevel = Integer.parseInt(
 *             properties.getProperty("maxLevel", "100"));
 *     }
 *
 *     @Override
 *     public void init() {
 *         if (enableFeature) {
 *             // Register content based on config
 *         }
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @see WurmServerMod
 */
public interface Configurable {

    /**
     * Configure the mod from properties files.
     *
     * <p>This method is called during mod loading, before {@link WurmServerMod#preInit()}.
     * The properties object contains merged key-value pairs from:</p>
     * <ul>
     *   <li>{@code mods/modname.properties} - Required mod metadata</li>
     *   <li>{@code mods/modname.config} - Optional mod configuration</li>
     * </ul>
     *
     * <p><strong>Thread Safety:</strong> This method is called from the main server
     * thread during initialization. Implementations do not need to be thread-safe
     * for this call, but should properly initialize any shared state used by
     * later event handlers.</p>
     *
     * <p><strong>Error Handling:</strong> If configuration loading fails, implementations
     * should throw a {@link RuntimeException} to prevent the mod from loading with
     * invalid configuration.</p>
     *
     * @param properties merged properties from .properties and .config files
     * @throws RuntimeException if configuration is invalid or required settings are missing
     */
    void configure(Properties properties);
}
