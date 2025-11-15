package com.garward.wurmmodloader.core.console;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Auto-discovers all GM commands from Wurm's Communicator class.
 *
 * <p>Scans for all methods matching pattern: handleHashMessage*()
 * These are the internal handlers for all # commands in Wurm.</p>
 *
 * <p><strong>Discovery Process:</strong></p>
 * <ul>
 *   <li>Loads com.wurmonline.server.creatures.Communicator via reflection</li>
 *   <li>Scans all methods for handleHashMessage* pattern</li>
 *   <li>Extracts command names (e.g., handleHashMessageKick → "kick")</li>
 *   <li>Stores methods for auto-invocation</li>
 * </ul>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class GMCommandDiscovery {

    private static final Logger LOGGER = Logger.getLogger(GMCommandDiscovery.class.getName());

    private static final Map<String, Method> discoveredCommands = new HashMap<>();
    private static boolean initialized = false;

    /**
     * Discover all GM commands from Communicator class.
     *
     * @return Map of command names to handler methods
     */
    public static synchronized Map<String, Method> discoverCommands() {
        if (initialized) {
            return new HashMap<>(discoveredCommands);
        }

        try {
            Class<?> communicatorClass = Class.forName("com.wurmonline.server.creatures.Communicator");

            int discovered = 0;
            for (Method method : communicatorClass.getDeclaredMethods()) {
                String methodName = method.getName();

                // Look for handleHashMessage* methods
                if (methodName.startsWith("handleHashMessage")) {
                    String commandName = extractCommandName(methodName);
                    if (commandName != null) {
                        discoveredCommands.put(commandName, method);
                        discovered++;
                    }
                }
            }

            initialized = true;
            System.out.println("[Console GM] Auto-discovered " + discovered + " GM commands from Communicator");

        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to load Communicator class for command discovery", e);
        }

        return new HashMap<>(discoveredCommands);
    }

    /**
     * Extract command name from method name.
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>handleHashMessageKick → "kick"</li>
     *   <li>handleHashMessageBan → "ban"</li>
     *   <li>handleHashMessageCreateItem → "createitem"</li>
     * </ul>
     *
     * @param methodName Method name
     * @return Command name (lowercase) or null if invalid
     */
    private static String extractCommandName(String methodName) {
        if (!methodName.startsWith("handleHashMessage")) {
            return null;
        }

        // Remove "handleHashMessage" prefix
        String suffix = methodName.substring("handleHashMessage".length());

        if (suffix.isEmpty()) {
            return null;
        }

        // Convert to lowercase
        return suffix.toLowerCase();
    }

    /**
     * Get all discovered command names.
     *
     * @return Sorted list of command names
     */
    public static List<String> getAllCommandNames() {
        if (!initialized) {
            discoverCommands();
        }
        List<String> names = new ArrayList<>(discoveredCommands.keySet());
        Collections.sort(names);
        return names;
    }

    /**
     * Get handler method for a command.
     *
     * @param commandName Command name (case-insensitive)
     * @return Handler method or null if not found
     */
    public static Method getCommandHandler(String commandName) {
        if (!initialized) {
            discoverCommands();
        }
        return discoveredCommands.get(commandName.toLowerCase());
    }

    /**
     * Check if command was auto-discovered.
     *
     * @param commandName Command name
     * @return true if discovered, false otherwise
     */
    public static boolean isDiscoveredCommand(String commandName) {
        if (!initialized) {
            discoverCommands();
        }
        return discoveredCommands.containsKey(commandName.toLowerCase());
    }

    /**
     * Get count of discovered commands.
     *
     * @return Number of commands
     */
    public static int getCommandCount() {
        if (!initialized) {
            discoverCommands();
        }
        return discoveredCommands.size();
    }
}
