package com.garward.wurmmodloader.modloader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.garward.wurmmodloader.modloader.internal.ModInfo;

/**
 * Modern mod discovery system that supports multiple mod layout patterns.
 *
 * <p>Supported layouts:
 * <ul>
 *   <li>Legacy subfolder: {@code mods/modname/modname.jar} + {@code mods/modname.properties}</li>
 *   <li>Flat with properties: {@code mods/modname.jar} + {@code mods/modname.properties}</li>
 *   <li>Modern flat: {@code mods/modname.jar} (metadata from JAR manifest)</li>
 *   <li>Versioned flat: {@code mods/modname-1.0.0.jar} (auto-detected)</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class ModDiscovery {

    private static final Logger logger = Logger.getLogger(ModDiscovery.class.getName());
    private static final String PROPERTIES_META_INF = "META-INF/org.gotti.wurmunlimited.modloader";

    /**
     * Discovers all mods in the specified directory using flexible layout detection.
     *
     * @param modDir the directory to scan for mods
     * @return list of discovered mod metadata
     * @throws IOException if directory cannot be read
     */
    public List<ModInfo> discoverMods(Path modDir) throws IOException {
        List<ModInfo> discoveredMods = new ArrayList<>();
        Set<String> handledMods = new HashSet<>();

        logger.info("[ModDiscovery] Scanning for mods in: " + modDir);

        // Phase 1: Legacy layout - properties file + subfolder/jar
        discoveredMods.addAll(discoverLegacySubfolderMods(modDir, handledMods));

        // Phase 2: Flat layout - properties file + jar in same directory
        discoveredMods.addAll(discoverFlatPropertiesMods(modDir, handledMods));

        // Phase 3: Modern flat layout - JAR with embedded metadata
        discoveredMods.addAll(discoverModernFlatMods(modDir, handledMods));

        // Phase 4: Subfolder without properties (legacy fallback)
        discoveredMods.addAll(discoverSubfolderWithoutProperties(modDir, handledMods));

        logger.info("[ModDiscovery] Discovered " + discoveredMods.size() + " mods");
        return discoveredMods;
    }

    /**
     * Phase 1: Legacy Ago layout - {@code modname.properties} + {@code modname/modname.jar}
     */
    private List<ModInfo> discoverLegacySubfolderMods(Path modDir, Set<String> handledMods) throws IOException {
        List<ModInfo> mods = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modDir, "*.properties")) {
            for (Path propsFile : stream) {
                String modName = propsFile.getFileName().toString().replaceAll("\\.properties$", "");

                if (handledMods.contains(modName)) {
                    continue;
                }

                // Try subfolder structure first: mods/modname/modname.jar
                Path subfolderJar = modDir.resolve(modName).resolve(modName + ".jar");

                if (Files.exists(subfolderJar)) {
                    logger.info("[ModDiscovery] Found legacy subfolder mod: " + modName);
                    ModInfo modInfo = loadModFromInfo(modName, propsFile, subfolderJar);
                    mods.add(modInfo);
                    handledMods.add(modName);
                }
            }
        }

        return mods;
    }

    /**
     * Phase 2: Flat layout with properties - {@code modname.properties} + {@code modname.jar}
     */
    private List<ModInfo> discoverFlatPropertiesMods(Path modDir, Set<String> handledMods) throws IOException {
        List<ModInfo> mods = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modDir, "*.properties")) {
            for (Path propsFile : stream) {
                String modName = propsFile.getFileName().toString().replaceAll("\\.properties$", "");

                if (handledMods.contains(modName)) {
                    continue;
                }

                // Try flat jar: mods/modname.jar
                Path flatJar = modDir.resolve(modName + ".jar");

                if (Files.exists(flatJar)) {
                    logger.info("[ModDiscovery] Found flat mod with properties: " + modName);
                    ModInfo modInfo = loadModFromInfo(modName, propsFile, flatJar);
                    mods.add(modInfo);
                    handledMods.add(modName);
                }
            }
        }

        return mods;
    }

    /**
     * Phase 3: Modern flat layout - {@code modname.jar} or {@code modname-version.jar} with embedded metadata
     */
    private List<ModInfo> discoverModernFlatMods(Path modDir, Set<String> handledMods) throws IOException {
        List<ModInfo> mods = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modDir, "*.jar")) {
            for (Path jarFile : stream) {
                String fileName = jarFile.getFileName().toString();
                String modName = extractModNameFromJar(fileName);

                if (handledMods.contains(modName)) {
                    continue;
                }

                // Check if JAR has embedded properties in META-INF
                Properties embeddedProps = loadPropertiesFromJar(modName, jarFile);

                if (embeddedProps.containsKey("classname")) {
                    logger.info("[ModDiscovery] Found modern flat mod: " + modName + " (from " + fileName + ")");
                    ModInfo modInfo = loadModFromInfo(modName, null, jarFile, embeddedProps);
                    mods.add(modInfo);
                    handledMods.add(modName);
                } else {
                    logger.fine("[ModDiscovery] Skipping JAR without metadata: " + fileName);
                }
            }
        }

        return mods;
    }

    /**
     * Phase 4: Subfolder without properties (legacy on-demand mods)
     */
    private List<ModInfo> discoverSubfolderWithoutProperties(Path modDir, Set<String> handledMods) throws IOException {
        List<ModInfo> mods = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modDir,
                path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) && !handledMods.contains(path.getFileName().toString()))) {

            for (Path subDir : stream) {
                String modName = subDir.getFileName().toString();

                if (handledMods.contains(modName)) {
                    continue;
                }

                Path jarFile = subDir.resolve(modName + ".jar");

                if (Files.exists(jarFile)) {
                    logger.info("[ModDiscovery] Found subfolder mod without properties: " + modName);
                    ModInfo modInfo = loadModFromInfo(modName, null, jarFile);
                    mods.add(modInfo);
                    handledMods.add(modName);
                }
            }
        }

        return mods;
    }

    /**
     * Extracts mod name from JAR filename, handling versioned names.
     * Examples:
     * - armoury.jar -> armoury
     * - wurmmodloader-armoury-0.8.0.jar -> armoury
     * - bagofholding.jar -> bagofholding
     */
    private String extractModNameFromJar(String jarFileName) {
        String baseName = jarFileName.replaceAll("\\.jar$", "");

        // Strip common version patterns: -1.0.0, -v1.0, etc.
        baseName = baseName.replaceAll("-\\d+\\.\\d+.*$", "");

        // Strip wurmmodloader- prefix if present
        baseName = baseName.replaceAll("^wurmmodloader-", "");

        return baseName;
    }

    /**
     * Loads mod metadata from properties file, JAR, and config file.
     */
    private ModInfo loadModFromInfo(String modName, Path propsFile, Path jarFile) throws IOException {
        return loadModFromInfo(modName, propsFile, jarFile, null);
    }

    /**
     * Loads mod metadata with optional pre-loaded embedded properties.
     */
    private ModInfo loadModFromInfo(String modName, Path propsFile, Path jarFile, Properties embeddedProps) throws IOException {
        Path configFile = Paths.get("mods", modName + ".config");
        Path modsDir = Paths.get("mods");
        Properties properties = new Properties();

        // Step 1: Load embedded properties from JAR if available
        if (jarFile != null && Files.exists(jarFile, LinkOption.NOFOLLOW_LINKS)) {
            if (embeddedProps != null) {
                properties.putAll(embeddedProps);
            } else {
                Properties loaded = loadPropertiesFromJar(modName, jarFile);
                properties.putAll(loaded);
            }

            // Mark as on-demand if no external properties file
            if (propsFile == null || !Files.exists(propsFile, LinkOption.NOFOLLOW_LINKS)) {
                properties.putIfAbsent("depend.ondemand", "true");
            }

            // Copy config template from JAR if not exists
            if (!Files.exists(configFile, LinkOption.NOFOLLOW_LINKS)) {
                copyConfigFromJar(modName, jarFile, configFile);
            }

            // Build classpath - use subfolder if JAR is in subfolder, else flat
            Path jarParent = jarFile.getParent();
            String relativePath;
            if (jarParent != null && !jarParent.equals(modsDir)) {
                // Subfolder layout: modname/modname.jar
                relativePath = jarParent.getFileName().toString() + "/" + jarFile.getFileName().toString();
            } else {
                // Flat layout: modname.jar
                relativePath = jarFile.getFileName().toString();
            }
            properties.putIfAbsent("classpath", relativePath);
        }

        // Step 2: Override with external properties file if present
        if (propsFile != null && Files.exists(propsFile, LinkOption.NOFOLLOW_LINKS)) {
            logger.log(Level.INFO, "Reading " + propsFile.toString());
            try (InputStream in = Files.newInputStream(propsFile)) {
                Properties fileProps = new Properties();
                fileProps.load(in);
                properties.putAll(fileProps);
            }
        }

        // Step 3: Load config file settings
        if (Files.exists(configFile, LinkOption.NOFOLLOW_LINKS)) {
            logger.log(Level.INFO, "Reading " + configFile.toString());
            try (InputStream in = Files.newInputStream(configFile)) {
                properties.load(in);
            }
        }

        return new ModInfo(properties, modName);
    }

    /**
     * Loads properties from JAR's META-INF directory.
     */
    private Properties loadPropertiesFromJar(String modName, Path jarFile) {
        Properties properties = new Properties();

        try (JarFile jar = new JarFile(jarFile.toFile())) {
            String propsPath = PROPERTIES_META_INF + "/" + modName + ".properties";

            java.util.jar.JarEntry entry = jar.getJarEntry(propsPath);
            if (entry != null) {
                logger.log(Level.INFO, "Reading embedded properties: " + jarFile + "!" + propsPath);
                try (InputStream in = jar.getInputStream(entry)) {
                    properties.load(in);
                }
            }
        } catch (IOException e) {
            logger.log(Level.FINE, "Could not read embedded properties from " + jarFile, e);
        }

        return properties;
    }

    /**
     * Copies config template from JAR to filesystem if present.
     */
    private void copyConfigFromJar(String modName, Path jarFile, Path configFile) {
        try (JarFile jar = new JarFile(jarFile.toFile())) {
            String configPath = PROPERTIES_META_INF + "/" + modName + ".config";

            java.util.jar.JarEntry entry = jar.getJarEntry(configPath);
            if (entry != null) {
                logger.log(Level.INFO, "Copying config template: " + jarFile + "!" + configPath + " to " + configFile);
                try (InputStream in = jar.getInputStream(entry)) {
                    Files.copy(in, configFile);
                }
            }
        } catch (IOException e) {
            logger.log(Level.FINE, "Could not copy config template from " + jarFile, e);
        }
    }
}
