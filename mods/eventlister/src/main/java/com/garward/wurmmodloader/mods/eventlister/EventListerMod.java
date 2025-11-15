package com.garward.wurmmodloader.mods.eventlister;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import com.garward.wurmmodloader.api.events.base.*;
import com.garward.wurmmodloader.api.events.creature.*;
import com.garward.wurmmodloader.api.events.item.*;
import com.garward.wurmmodloader.api.events.player.*;
import com.garward.wurmmodloader.api.events.server.*;
import com.garward.wurmmodloader.api.events.combat.*;
import com.garward.wurmmodloader.api.events.skill.*;
import com.garward.wurmmodloader.api.events.action.*;
import com.garward.wurmmodloader.api.events.vehicle.*;
import com.garward.wurmmodloader.modloader.interfaces.ServerStartedListener;

// Nested domains (since * won’t recurse)
import com.garward.wurmmodloader.api.events.combat.weapon.*;
import com.garward.wurmmodloader.api.events.combat.shield.*;
import com.garward.wurmmodloader.api.events.item.material.*;

/**
 * Diagnostic mod that lists all available events in the WurmModLoader framework.
 *
 * <p>This mod displays all event classes available for mod developers to subscribe to.
 * Events are categorized as either CANCELLABLE (can prevent vanilla behavior) or
 * INFORMATIONAL (observe-only).</p>
 *
 * <p>Configuration options:</p>
 * <ul>
 *   <li>outputJson - Set to "true" to generate events.json file (default: false)</li>
 * </ul>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class EventListerMod implements WurmServerMod, ServerStartedListener, Configurable {

    private static final Logger logger = Logger.getLogger(EventListerMod.class.getName());
    private boolean outputJson = false;
    private static final String EVENT_PACKAGE = "com.garward.wurmmodloader.api.events";

    /**
     * Event descriptor with name and type information.
     */
    private static class EventInfo {
        final String name;
        final boolean cancellable;
        final String description;

        EventInfo(Class<? extends Event> eventClass) {
            this.name = eventClass.getSimpleName();
            this.cancellable = CancellableEvent.class.isAssignableFrom(eventClass);
            this.description = getEventDescription(eventClass);
        }

        private String getEventDescription(Class<? extends Event> eventClass) {
            // Extract description from JavaDoc or provide default
            String name = eventClass.getSimpleName();
            if (name.contains("Death")) return "Fires when a creature dies";
            if (name.contains("Spawn")) return "Fires when a creature spawns";
            if (name.contains("Damage")) return "Fires when combat damage is dealt";
            if (name.contains("Mount")) return "Fires when mounting a vehicle/creature";
            if (name.contains("Speed")) return "Fires during vehicle speed calculation";
            if (name.contains("Equipment")) return "Fires during mount equipment checks";
            if (name.contains("Examine")) return "Fires when examining an item";
            if (name.contains("Drop")) return "Fires when dropping an item";
            if (name.contains("Trade")) return "Fires when trading an item";
            if (name.contains("Started")) return "Fires when server finishes starting";
            if (name.contains("Stopping")) return "Fires when server begins shutdown";
            if (name.contains("Poll")) return "Fires every server tick";
            if (name.contains("Templates")) return "Fires after item templates are created";
            if (name.contains("Capability")) return "Fires during capability registration";
            return "Event";
        }
    }

    @Override
    public void configure(Properties properties) {
        this.outputJson = Boolean.parseBoolean(properties.getProperty("outputJson", "false"));
        logger.info("EventLister configuration: outputJson=" + outputJson);
    }

    @Override
    public void onServerStarted() {
        List<EventInfo> events = collectAllEvents();
        Collections.sort(events, Comparator.comparing(e -> e.name));

        // Print formatted list to console
        printConsoleList(events);

        // Generate JSON if enabled
        if (outputJson) {
            generateJsonOutput(events);
        }
    }

    private List<EventInfo> collectAllEvents() {
        List<EventInfo> events = new ArrayList<>();
        Set<String> classNames = new HashSet<>();
        String packagePath = EVENT_PACKAGE.replace('.', '/');

        try {
            ClassLoader loader = EventListerMod.class.getClassLoader();
            Enumeration<URL> resources = loader.getResources(packagePath);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    try {
                        File directory = new File(url.toURI());
                        scanDirectory(directory, EVENT_PACKAGE, classNames);
                    } catch (URISyntaxException e) {
                        logger.log(Level.WARNING, "Failed to scan directory for events: " + url, e);
                    }
                } else if ("jar".equals(protocol)) {
                    scanJar(url, packagePath, classNames);
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to enumerate event resources", e);
        }

        for (String className : classNames) {
            try {
                Class<?> rawClass = Class.forName(className, false, EventListerMod.class.getClassLoader());
                if (!Event.class.isAssignableFrom(rawClass)) {
                    continue;
                }
                if (Modifier.isAbstract(rawClass.getModifiers())) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Class<? extends Event> eventClass = (Class<? extends Event>) rawClass;
                events.add(new EventInfo(eventClass));
            } catch (ClassNotFoundException | LinkageError ex) {
                logger.log(Level.FINE, "Skipping event class " + className, ex);
            }
        }

        if (events.isEmpty()) {
            logger.warning("Event discovery returned no results, falling back to static list");
            addFallbackEvents(events);
        }

        return events;
    }

    private void addFallbackEvents(List<EventInfo> events) {
        events.add(new EventInfo(ServerStartedEvent.class));
        events.add(new EventInfo(ServerStoppingEvent.class));
        events.add(new EventInfo(ServerPollEvent.class));
        events.add(new EventInfo(ItemTemplatesCreatedEvent.class));
        events.add(new EventInfo(CreatureDeathEvent.class));
        events.add(new EventInfo(CreatureSpawnEvent.class));
        events.add(new EventInfo(CombatDamageEvent.class));
        events.add(new EventInfo(PlayerDeathEvent.class));
        events.add(new EventInfo(VehicleMountEvent.class));
        events.add(new EventInfo(VehicleSpeedCalculationEvent.class));
        events.add(new EventInfo(MountEquipmentCheckEvent.class));
        events.add(new EventInfo(ItemExamineEvent.class));
        events.add(new EventInfo(ItemDropEvent.class));
        events.add(new EventInfo(ItemTradeEvent.class));
        events.add(new EventInfo(CapabilityRegistrationEvent.class));
    }

    private void scanDirectory(File directory, String packageName, Set<String> classNames) {
        if (directory == null || !directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), classNames);
            } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                classNames.add(className);
            }
        }
    }

    private void scanJar(URL url, String packagePath, Set<String> classNames) {
        try {
            JarURLConnection connection = (JarURLConnection) url.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(packagePath) && name.endsWith(".class") && !name.contains("$")) {
                        String className = name.replace('/', '.').substring(0, name.length() - 6);
                        classNames.add(className);
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to scan jar for events: " + url, e);
        }
    }

    private void printConsoleList(List<EventInfo> events) {
        logger.info("================================================================================");
        logger.info("WurmModLoader - Available Events");
        logger.info("================================================================================");
        logger.info("");
        logger.info("Events can be subscribed to using @SubscribeEvent annotation:");
        logger.info("");
        logger.info("  @SubscribeEvent");
        logger.info("  public void onEvent(EventNameHere event) {");
        logger.info("      // Your mod logic here");
        logger.info("  }");
        logger.info("");
        logger.info("--------------------------------------------------------------------------------");
        logger.info("");

        int cancellableCount = 0;
        int infoCount = 0;

        for (EventInfo event : events) {
            String type = event.cancellable ? "[CANCELLABLE]" : "[INFO]       ";
            logger.info(String.format("  %s %-35s - %s", type, event.name, event.description));

            if (event.cancellable) {
                cancellableCount++;
            } else {
                infoCount++;
            }
        }

        logger.info("");
        logger.info("--------------------------------------------------------------------------------");
        logger.info(String.format("Total Events: %d (Cancellable: %d, Informational: %d)",
                events.size(), cancellableCount, infoCount));
        logger.info("================================================================================");
    }

    private void generateJsonOutput(List<EventInfo> events) {
        try {
            String outputPath = Paths.get(".", "wurmmodloader_events.json").toString();
            try (FileWriter writer = new FileWriter(outputPath)) {
                writer.write("{\n");
                writer.write("  \"framework\": \"WurmModLoader\",\n");
                writer.write("  \"version\": \"1.0.0\",\n");
                writer.write("  \"eventCount\": " + events.size() + ",\n");
                writer.write("  \"events\": [\n");

                for (int i = 0; i < events.size(); i++) {
                    EventInfo event = events.get(i);
                    writer.write("    {\n");
                    writer.write("      \"name\": \"" + escapeJson(event.name) + "\",\n");
                    writer.write("      \"cancellable\": " + event.cancellable + ",\n");
                    writer.write("      \"description\": \"" + escapeJson(event.description) + "\"\n");
                    writer.write("    }");

                    if (i < events.size() - 1) {
                        writer.write(",");
                    }
                    writer.write("\n");
                }

                writer.write("  ]\n");
                writer.write("}\n");
            }

            logger.info("Event list written to: " + outputPath);

        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to write JSON output", e);
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
