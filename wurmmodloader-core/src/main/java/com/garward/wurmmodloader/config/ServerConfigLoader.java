package com.garward.wurmmodloader.config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orchestrates server configuration loading.
 *
 * <p>Handles two scenarios:
 * <ul>
 *   <li><strong>First-time setup:</strong> If config file doesn't exist, generates it from
 *       current database values (so existing servers get their settings exported)</li>
 *   <li><strong>Normal startup:</strong> Loads config file, validates it, returns ServerConfig</li>
 * </ul>
 *
 * <p><strong>Classloader Isolation:</strong> This class is loaded in the modloader's classloader
 * and does NOT depend on static fields in Wurm classes.</p>
 *
 * <p><strong>Initialization Timing:</strong> Must be called AFTER database connections are ready.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class ServerConfigLoader {

    private static final Logger logger = Logger.getLogger(ServerConfigLoader.class.getName());

    /**
     * Load server configuration.
     *
     * <p>If config file doesn't exist, generates it from current database values.
     * If config file exists, loads and validates it.</p>
     *
     * @param worldFolder The world folder name (e.g., "Adventure", "Riverweave")
     * @param serverId The server ID from the database
     * @return Validated ServerConfig object
     * @throws ConfigException if loading or validation fails
     */
    public static ServerConfig load(String worldFolder, int serverId) throws ConfigException {
        logger.info("[ServerConfigLoader] Loading config for world: " + worldFolder + ", serverId: " + serverId);

        String configPath = getConfigPath(worldFolder);
        File configFile = new File(configPath);

        if (!configFile.exists()) {
            // First-time setup: Generate config from database
            logger.info("[ServerConfigLoader] Config file not found, generating from current database values...");
            return generateFromDatabase(configPath, serverId);
        } else {
            // Normal startup: Load from file
            logger.info("[ServerConfigLoader] Loading config from file: " + configPath);
            return loadFromFile(configPath);
        }
    }

    /**
     * Load configuration from file.
     *
     * @param configPath Absolute path to config file
     * @return Validated ServerConfig object
     * @throws ConfigException if loading or validation fails
     */
    private static ServerConfig loadFromFile(String configPath) throws ConfigException {
        // Parse YAML
        ServerConfig config = ConfigParser.parseFile(configPath);

        // Validate
        ConfigValidator.validate(config);

        logger.info("[ServerConfigLoader] Config loaded successfully: " + config);
        return config;
    }

    /**
     * Generate configuration from current database values.
     *
     * <p>This is called on first-time setup to export existing server settings
     * into a config file. Existing server owners can then review and modify their settings.</p>
     *
     * @param configPath Path to save the generated config
     * @param serverId Server ID
     * @return Generated ServerConfig object
     * @throws ConfigException if generation fails
     */
    private static ServerConfig generateFromDatabase(String configPath, int serverId) throws ConfigException {
        try {
            // Read current database values
            ServerConfig config = ServerConfigGenerator.generateFromDatabase(serverId);

            // Save to file
            saveConfigToFile(config, configPath);

            logger.info("[ServerConfigLoader] Config generated from database and saved to: " + configPath);
            logger.info("[ServerConfigLoader] IMPORTANT: Review and edit this file to customize your server settings!");

            return config;

        } catch (Exception e) {
            throw new ConfigException("Failed to generate config from database: " + e.getMessage(), e);
        }
    }

    /**
     * Save a ServerConfig object to a YAML file with comments.
     *
     * @param config The config to save
     * @param filePath Path to save the file
     * @throws IOException if file writing fails
     */
    private static void saveConfigToFile(ServerConfig config, String filePath) throws IOException {
        // Generate YAML content with comments
        String yaml = ServerConfigGenerator.generateYamlWithComments(config);

        // Write to file
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(yaml);
        }

        logger.info("[ServerConfigLoader] Config saved to: " + filePath);
    }

    /**
     * Get the config file path for a world folder.
     *
     * @param worldFolder World folder name (e.g., "Adventure")
     * @return Absolute path to server_config.yaml
     */
    private static String getConfigPath(String worldFolder) {
        // Get server root directory (parent of world folder)
        String serverRoot = System.getProperty("user.dir");

        // Config file is at: <serverRoot>/<worldFolder>/server_config.yaml
        return serverRoot + File.separator + worldFolder + File.separator + "server_config.yaml";
    }

    /**
     * Get default configuration values.
     *
     * @return ServerConfig with default values
     */
    public static ServerConfig getDefaults() {
        return new ServerConfig();
    }
}
