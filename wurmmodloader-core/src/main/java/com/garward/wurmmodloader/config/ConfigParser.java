package com.garward.wurmmodloader.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.inspector.TagInspector;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration parser utility that wraps SnakeYAML.
 *
 * <p>This class provides a controlled interface for YAML parsing, preventing
 * direct SnakeYAML usage from mod code. It enforces safe parsing practices
 * and provides clear error messages.</p>
 *
 * <p><strong>Classloader Isolation:</strong> This class is loaded in the modloader's
 * classloader ONLY and has NO dependencies on Wurm classes.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class ConfigParser {

    private static final Logger logger = Logger.getLogger(ConfigParser.class.getName());

    /**
     * Parse a YAML file into a ServerConfig object.
     *
     * @param filePath Absolute path to the YAML file
     * @return Parsed ServerConfig object
     * @throws ConfigException if parsing fails
     */
    public static ServerConfig parseFile(String filePath) throws ConfigException {
        logger.info("[ConfigParser] Parsing config file: " + filePath);

        try (InputStream input = new FileInputStream(filePath)) {
            return parseStream(input);
        } catch (FileNotFoundException e) {
            throw new ConfigException("Config file not found: " + filePath, e);
        } catch (Exception e) {
            throw new ConfigException("Failed to read config file: " + filePath, e);
        }
    }

    /**
     * Parse a YAML input stream into a ServerConfig object.
     *
     * @param input Input stream containing YAML data
     * @return Parsed ServerConfig object
     * @throws ConfigException if parsing fails
     */
    public static ServerConfig parseStream(InputStream input) throws ConfigException {
        try {
            // Configure safe YAML loading
            LoaderOptions loaderOptions = new LoaderOptions();

            // Only allow specific tags for security
            loaderOptions.setTagInspector(new TagInspector() {
                @Override
                public boolean isGlobalTagAllowed(org.yaml.snakeyaml.nodes.Tag tag) {
                    // Only allow standard YAML types and our config classes
                    return tag.getClassName().startsWith("com.garward.wurmmodloader.config.")
                        || tag.getClassName().startsWith("java.lang.")
                        || tag.getClassName().startsWith("java.util.");
                }
            });

            // Prevent excessive nesting (DoS protection)
            loaderOptions.setNestingDepthLimit(10);

            // Create YAML parser with type-safe constructor
            Constructor constructor = new Constructor(ServerConfig.class, loaderOptions);
            Yaml yaml = new Yaml(constructor);

            // Parse YAML
            ServerConfig config = yaml.load(input);

            if (config == null) {
                throw new ConfigException("Config file is empty or invalid");
            }

            logger.info("[ConfigParser] Successfully parsed config: version=" + config.version);
            return config;

        } catch (org.yaml.snakeyaml.scanner.ScannerException e) {
            throw new ConfigException("YAML syntax error: " + e.getMessage(), e);
        } catch (org.yaml.snakeyaml.constructor.ConstructorException e) {
            throw new ConfigException("YAML type error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ConfigException("Failed to parse YAML: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a default YAML config string for templates.
     *
     * @return YAML string with default values and comments
     */
    public static String generateDefaultYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);

        Yaml yaml = new Yaml(options);
        ServerConfig defaultConfig = new ServerConfig();

        return yaml.dump(defaultConfig);
    }

    /**
     * Validate config file syntax without fully loading it.
     *
     * @param filePath Absolute path to the YAML file
     * @return true if syntax is valid, false otherwise
     */
    public static boolean validateSyntax(String filePath) {
        try {
            parseFile(filePath);
            return true;
        } catch (ConfigException e) {
            logger.log(Level.WARNING, "[ConfigParser] Config validation failed: " + e.getMessage());
            return false;
        }
    }
}
