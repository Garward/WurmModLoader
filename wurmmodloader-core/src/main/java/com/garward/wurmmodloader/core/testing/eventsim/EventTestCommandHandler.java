package com.garward.wurmmodloader.core.testing.eventsim;

import com.garward.wurmmodloader.api.events.base.EventPriority;
import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.player.PlayerMessageEvent;
import com.garward.wurmmodloader.core.event.EventBus;
import com.garward.wurmmodloader.core.eventlogic.PlayerUtil;
import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command handler for event simulation testing.
 *
 * <p>Handles the {@code /framework testEvents} command via PlayerMessageEvent.
 *
 * <p>Usage:
 * <ul>
 *   <li>{@code /framework testEvents} - Run with default settings (NORMAL, 10 iterations)</li>
 *   <li>{@code /framework testEvents RARE} - Run in RARE mode</li>
 *   <li>{@code /framework testEvents FULL_SWEEP 5} - Run FULL_SWEEP with 5 iterations</li>
 *   <li>{@code /framework testEvents test.json} - Load test definition from JSON file</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class EventTestCommandHandler {

    private static final Logger LOGGER = Logger.getLogger(EventTestCommandHandler.class.getName());
    private static final String COMMAND_PREFIX = "/framework";
    private static final String TEST_COMMAND = "testEvents";

    private final String serverDirectory;
    private final String logDirectory;
    private volatile boolean running = false;

    /**
     * Creates a new command handler.
     *
     * @param serverDirectory root server directory
     */
    public EventTestCommandHandler(String serverDirectory) {
        this.serverDirectory = serverDirectory;
        this.logDirectory = serverDirectory + File.separator + "logs";
    }

    /**
     * Registers this handler with the event bus.
     */
    public void register() {
        EventBus.getInstance().register(this);
        LOGGER.info("EventTestCommandHandler registered");
    }

    /**
     * Unregisters this handler from the event bus.
     */
    public void unregister() {
        EventBus.getInstance().unregister(this);
        LOGGER.info("EventTestCommandHandler unregistered");
    }

    /**
     * Handles player messages to intercept /framework testEvents commands.
     *
     * @param event the player message event
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerMessage(PlayerMessageEvent event) {
        String message = event.getMessage();
        if (message == null || !message.toLowerCase().startsWith(COMMAND_PREFIX)) {
            return;
        }

        // Parse the command
        String[] parts = message.trim().split("\\s+");
        if (parts.length < 2 || !parts[1].equalsIgnoreCase(TEST_COMMAND)) {
            return;
        }

        // This is a /framework testEvents command - handle it
        event.setCancelled(true); // Prevent further processing

        // Get the player who issued the command
        Object communicatorObj = event.getCommunicator();
        if (communicatorObj == null) {
            LOGGER.warning("No communicator for /framework testEvents command");
            return;
        }

        try {
            // Get the player from communicator
            Player player = getPlayerFromCommunicator(communicatorObj);
            if (player == null) {
                LOGGER.warning("Could not resolve player from communicator");
                return;
            }

            // PERMISSION CHECK: Require POWER_DEMIGOD (2) or higher
            // This is an admin-only debugging/testing command
            if (player.getPower() <= MiscConstants.POWER_HERO) {
                sendMessage(player, "[ERROR] /framework testEvents requires administrator power (POWER_DEMIGOD or higher)");
                LOGGER.warning("Player " + player.getName() + " (power=" + player.getPower() +
                             ") attempted to use /framework testEvents without sufficient permissions");
                return;
            }

            // Check if already running
            if (running) {
                sendMessage(player, "Event simulation is already running. Please wait...");
                return;
            }

            // Parse command arguments
            SimulationMode mode = SimulationMode.NORMAL;
            int iterations = 10;
            String jsonFile = null;

            if (parts.length >= 3) {
                // Check if it's a JSON file
                if (parts[2].endsWith(".json")) {
                    jsonFile = parts[2];
                } else {
                    // Parse mode
                    mode = SimulationMode.parse(parts[2]);
                }
            }

            if (parts.length >= 4 && jsonFile == null) {
                // Parse iterations
                try {
                    iterations = Integer.parseInt(parts[3]);
                    iterations = Math.max(1, Math.min(iterations, 1000)); // Limit to 1000
                } catch (NumberFormatException e) {
                    sendMessage(player, "Invalid iterations value. Using default: 10");
                }
            }

            // Send confirmation to player
            if (jsonFile != null) {
                sendMessage(player, "Starting event simulation from: " + jsonFile);
            } else {
                sendMessage(player, "Starting event simulation: mode=" + mode + ", iterations=" + iterations);
            }

            // Run simulation in background thread
            final SimulationMode finalMode = mode;
            final int finalIterations = iterations;
            final String finalJsonFile = jsonFile;
            final Player finalPlayer = player;

            new Thread(() -> runSimulation(finalPlayer, finalMode, finalIterations, finalJsonFile))
                .start();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling /framework testEvents command", e);
        }
    }

    /**
     * Runs the event simulation in a background thread.
     *
     * @param player the player who issued the command
     * @param mode simulation mode
     * @param iterations number of iterations
     * @param jsonFile optional JSON file path
     */
    private void runSimulation(Player player, SimulationMode mode, int iterations, String jsonFile) {
        running = true;

        try {
            EventSimulator simulator;

            if (jsonFile != null) {
                // Load from JSON file
                File testFile = new File(serverDirectory + File.separator + "config" +
                                       File.separator + "event_tests", jsonFile);
                TestDefinition definition = TestDefinition.fromFile(testFile);
                simulator = new EventSimulator(definition.getMode(), definition.getIterations(), logDirectory);
                sendMessage(player, "Loaded test definition: " + definition);
            } else {
                // Create with command-line parameters
                simulator = new EventSimulator(mode, iterations, logDirectory);
            }

            // Run the simulation
            String summary = simulator.runSimulation();

            // Send results to player
            sendMessage(player, "Event simulation completed!");
            sendMessage(player, "Results written to: logs/event_test.log");

            // Send summary table
            String[] lines = summary.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    sendMessage(player, line);
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Event simulation failed", e);
            sendMessage(player, "Event simulation failed: " + e.getMessage());
        } finally {
            running = false;
        }
    }

    /**
     * Gets a Player object from a Communicator object.
     *
     * @param communicatorObj the communicator object
     * @return player, or null if not found
     */
    private Player getPlayerFromCommunicator(Object communicatorObj) {
        try {
            // Use reflection to get the player from communicator
            java.lang.reflect.Method getPlayerMethod = communicatorObj.getClass().getMethod("getPlayer");
            Object playerObj = getPlayerMethod.invoke(communicatorObj);

            if (playerObj instanceof Player) {
                return (Player) playerObj;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get player from communicator", e);
        }
        return null;
    }

    /**
     * Sends a message to a player.
     *
     * @param player the player
     * @param message the message
     */
    private void sendMessage(Creature player, String message) {
        if (player != null && player.isPlayer()) {
            PlayerUtil.sendServerMessage(player, message, (byte) 2); // Blue color
        }
    }

    /**
     * Checks if a simulation is currently running.
     *
     * @return true if simulation is in progress
     */
    public boolean isRunning() {
        return running;
    }
}
