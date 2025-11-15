package com.garward.wurmmodloader.core.testing.eventsim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JSON-based test definition for event simulation.
 *
 * <p>Allows users to define custom test scenarios via JSON files:
 * <pre>{@code
 * {
 *   "mode": "RARE",
 *   "iterations": 100,
 *   "events": ["CreatureDeathEvent", "PlayerLoginEvent"]
 * }
 * }</pre>
 *
 * <p>JSON files should be placed in: {@code config/event_tests/}
 *
 * @since 1.0.0
 */
public class TestDefinition {

    private static final Logger LOGGER = Logger.getLogger(TestDefinition.class.getName());

    private SimulationMode mode = SimulationMode.NORMAL;
    private int iterations = 10;
    private List<String> events = new ArrayList<>();

    /**
     * Creates a default test definition.
     */
    public TestDefinition() {
    }

    /**
     * Creates a test definition with specified parameters.
     *
     * @param mode simulation mode
     * @param iterations number of iterations
     * @param events list of event names to test (empty = all)
     */
    public TestDefinition(SimulationMode mode, int iterations, List<String> events) {
        this.mode = mode;
        this.iterations = iterations;
        this.events = events != null ? new ArrayList<>(events) : new ArrayList<>();
    }

    /**
     * Loads a test definition from a JSON file.
     *
     * <p>This is a simple JSON parser - for production use, consider using a proper JSON library.
     *
     * @param file the JSON file to load
     * @return test definition, or default if loading fails
     */
    public static TestDefinition fromFile(File file) {
        TestDefinition definition = new TestDefinition();

        if (!file.exists() || !file.isFile()) {
            LOGGER.warning("Test definition file not found: " + file.getAbsolutePath());
            return definition;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line.trim());
            }

            // Simple JSON parsing (no library dependency)
            String json = jsonContent.toString();
            definition = parseSimpleJson(json);

            LOGGER.info("Loaded test definition from: " + file.getName());

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load test definition from " + file.getName(), e);
        }

        return definition;
    }

    /**
     * Simple JSON parser (basic implementation without external dependencies).
     *
     * @param json the JSON string
     * @return parsed test definition
     */
    private static TestDefinition parseSimpleJson(String json) {
        TestDefinition definition = new TestDefinition();

        try {
            // Extract "mode"
            String modeValue = extractJsonString(json, "mode");
            if (modeValue != null) {
                definition.mode = SimulationMode.parse(modeValue);
            }

            // Extract "iterations"
            String iterValue = extractJsonString(json, "iterations");
            if (iterValue != null) {
                try {
                    definition.iterations = Integer.parseInt(iterValue);
                } catch (NumberFormatException e) {
                    LOGGER.warning("Invalid iterations value: " + iterValue);
                }
            }

            // Extract "events" array
            List<String> events = extractJsonArray(json, "events");
            if (events != null && !events.isEmpty()) {
                definition.events = events;
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse JSON test definition", e);
        }

        return definition;
    }

    /**
     * Extracts a string value from simple JSON.
     *
     * @param json the JSON string
     * @param key the key to extract
     * @return the value, or null if not found
     */
    private static String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"?([^\",}\\]]+)\"?";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    /**
     * Extracts an array from simple JSON.
     *
     * @param json the JSON string
     * @param key the key to extract
     * @return list of array elements, or null if not found
     */
    private static List<String> extractJsonArray(String json, String key) {
        List<String> result = new ArrayList<>();

        String pattern = "\"" + key + "\"\\s*:\\s*\\[([^\\]]+)\\]";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);

        if (m.find()) {
            String arrayContent = m.group(1);
            // Split by comma and clean up
            String[] items = arrayContent.split(",");
            for (String item : items) {
                String cleaned = item.trim().replaceAll("^\"|\"$", "");
                if (!cleaned.isEmpty()) {
                    result.add(cleaned);
                }
            }
        }

        return result;
    }

    /**
     * Gets the simulation mode.
     *
     * @return simulation mode
     */
    public SimulationMode getMode() {
        return mode;
    }

    /**
     * Sets the simulation mode.
     *
     * @param mode simulation mode
     */
    public void setMode(SimulationMode mode) {
        this.mode = mode;
    }

    /**
     * Gets the number of iterations.
     *
     * @return iterations
     */
    public int getIterations() {
        return iterations;
    }

    /**
     * Sets the number of iterations.
     *
     * @param iterations iterations (must be positive)
     */
    public void setIterations(int iterations) {
        this.iterations = Math.max(1, iterations);
    }

    /**
     * Gets the list of events to test.
     *
     * @return event names (empty = test all events)
     */
    public List<String> getEvents() {
        return new ArrayList<>(events);
    }

    /**
     * Sets the list of events to test.
     *
     * @param events event names
     */
    public void setEvents(List<String> events) {
        this.events = events != null ? new ArrayList<>(events) : new ArrayList<>();
    }

    /**
     * Checks if this definition filters specific events.
     *
     * @return true if specific events are specified
     */
    public boolean hasEventFilter() {
        return !events.isEmpty();
    }

    @Override
    public String toString() {
        return "TestDefinition{" +
               "mode=" + mode +
               ", iterations=" + iterations +
               ", events=" + events +
               '}';
    }
}
