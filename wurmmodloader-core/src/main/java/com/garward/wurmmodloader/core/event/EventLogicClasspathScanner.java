package com.garward.wurmmodloader.core.event;

import com.garward.wurmmodloader.core.registry.RuntimeRegistries;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lightweight scanner that discovers event logic classes on the classpath.
 */
public final class EventLogicClasspathScanner {

    private static final Logger LOGGER = Logger.getLogger(EventLogicClasspathScanner.class.getName());

    private static final String BASE_PACKAGE = "com.garward.wurmmodloader.core.eventlogic";
    private static final String BASE_PATH = BASE_PACKAGE.replace('.', '/');

    private EventLogicClasspathScanner() {}

    public static void scanAndRegister() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = EventLogicClasspathScanner.class.getClassLoader();
        }

        if (loader == null) {
            LOGGER.warning("No classloader available; skipping event logic scan.");
            return;
        }

        Set<Class<?>> discovered = new LinkedHashSet<>();
        try {
            Enumeration<URL> resources = loader.getResources(BASE_PATH);
            if (!resources.hasMoreElements()) {
                LOGGER.fine("No event logic resources found on classpath.");
            }
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    scanDirectory(url, loader, discovered);
                } else if ("jar".equals(protocol)) {
                    scanJar(url, loader, discovered);
                } else {
                    LOGGER.log(Level.FINE,
                        "Skipping event logic resource with unsupported protocol {0}: {1}",
                        new Object[] { protocol, url });
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to enumerate event logic resources", e);
        }

        discovered.stream()
            .filter(EventLogicBootstrap::isEventLogicCandidate)
            .forEach(RuntimeRegistries.EVENT_LOGIC::register);

        LOGGER.log(Level.INFO, "Discovered {0} event logic class(es)", discovered.size());
    }

    private static void scanDirectory(URL url, ClassLoader loader, Set<Class<?>> discovered) {
        try {
            Path root = Paths.get(url.toURI());
            if (!Files.exists(root)) {
                return;
            }
            Files.walk(root, FileVisitOption.FOLLOW_LINKS)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".class"))
                .forEach(path -> addClassFromPath(root, path, loader, discovered));
        } catch (IOException | URISyntaxException e) {
            LOGGER.log(Level.WARNING, "Failed to scan directory resource " + url, e);
        }
    }

    private static void scanJar(URL url, ClassLoader loader, Set<Class<?>> discovered) {
        try {
            JarURLConnection connection = (JarURLConnection) url.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                scanJarEntries(jarFile, loader, discovered);
            }
        } catch (ClassCastException e) {
            scanJarByPath(url, loader, discovered);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to scan jar resource " + url, e);
        }
    }

    private static void scanJarByPath(URL url, ClassLoader loader, Set<Class<?>> discovered) {
        String path = url.getPath();
        int separator = path.indexOf('!');
        if (separator == -1) {
            LOGGER.log(Level.FINE, "Jar URL does not contain separator: {0}", url);
            return;
        }

        String jarPath = path.substring(0, separator);
        if (jarPath.startsWith("file:")) {
            jarPath = jarPath.substring("file:".length());
        }
        try {
            String decodedPath = URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name());
            try (JarFile jarFile = new JarFile(decodedPath)) {
                scanJarEntries(jarFile, loader, discovered);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to open jar file " + jarPath, e);
        }
    }

    private static void scanJarEntries(JarFile jarFile, ClassLoader loader, Set<Class<?>> discovered) {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            String name = entry.getName();
            if (!name.startsWith(BASE_PATH) || !name.endsWith(".class") || name.contains("$")) {
                continue;
            }
            String className = name.substring(0, name.length() - 6).replace('/', '.');
            addClassByName(className, loader, discovered);
        }
    }

    private static void addClassFromPath(Path root, Path file, ClassLoader loader, Set<Class<?>> discovered) {
        Path relative = root.relativize(file);
        String normalized = relative.toString()
            .replace(File.separatorChar, '.')
            .replace('/', '.');
        if (!normalized.endsWith(".class") || normalized.contains("$")) {
            return;
        }
        String className = BASE_PACKAGE + '.' + normalized.substring(0, normalized.length() - 6);
        addClassByName(className, loader, discovered);
    }

    private static void addClassByName(String className, ClassLoader loader, Set<Class<?>> discovered) {
        try {
            Class<?> clazz = Class.forName(className, false, loader);
            discovered.add(clazz);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            LOGGER.log(Level.FINE, "Skipping unloadable event logic class {0}", className);
        }
    }
}
