package com.garward.wurmmodloader.core.registry;

import com.garward.wurmmodloader.api.registry.ResourceLocation;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

/**
 * Manages automatic ID assignment for game resources in the WurmModLoader registry system.
 *
 * <p>The {@code IdAllocator} class provides a mechanism to allocate unique IDs to {@link ResourceLocation}
 * instances. It supports automatic ID generation starting from a predefined value and manual reservation
 * of specific IDs. The allocations can be saved to and loaded from a JSON file for persistence.</p>
 *
 * <p>This class is implemented as a singleton to ensure a single point of management for all ID allocations
 * within the application. All methods are thread-safe, making it suitable for use in multi-threaded environments.</p>
 *
 * <p>Usage example:
 * <pre>{@code
 * // Get singleton instance
 * IdAllocator allocator = IdAllocator.getInstance();
 *
 * // Allocate automatic IDs
 * int swordId = allocator.allocate(new ResourceLocation("mymod", "magic_sword"));
 * int shieldId = allocator.allocate(new ResourceLocation("mymod", "shield"));
 *
 * // Reserve specific ID
 * allocator.reserve(new ResourceLocation("mymod", "special_item"), 30500);
 *
 * // Save to file for persistence
 * allocator.save(Paths.get("config/id_allocations.json"));
 *
 * // Load on startup
 * allocator.load(Paths.get("config/id_allocations.json"));
 *
 * // Check if ID exists
 * Optional<Integer> existingId = allocator.getId(new ResourceLocation("mymod", "magic_sword"));
 * }</pre>
 * </p>
 *
 * @since 1.0.0
 */
public final class IdAllocator {

    private static final int STARTING_ID = 30000; // After vanilla Wurm IDs
    private static final Path DEFAULT_FILE = Paths.get("mods", "WurmModLoader", "id_allocations.json");

    private final AtomicInteger nextId = new AtomicInteger(STARTING_ID);
    private final Map<ResourceLocation, Integer> allocations = new ConcurrentHashMap<>();

    private static final IdAllocator INSTANCE = new IdAllocator();

    private IdAllocator() {
        // Private constructor for singleton pattern
        // Auto-load existing allocations on initialization
        try {
            load(DEFAULT_FILE);
        } catch (IOException e) {
            // Ignore - file may not exist yet
        }
    }

    /**
     * Returns the singleton instance of {@code IdAllocator}.
     *
     * @return the singleton instance
     * @since 1.0.0
     */
    public static IdAllocator getInstance() {
        return INSTANCE;
    }

    /**
     * Allocates the next available ID for the given key.
     *
     * <p>If the key is already allocated, the existing ID is returned. Otherwise, a new ID is generated
     * and associated with the key.</p>
     *
     * @param key the resource location key
     * @return the allocated ID
     * @throws IllegalArgumentException if the key is null
     * @since 1.0.0
     */
    public int allocate(ResourceLocation key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        return allocations.computeIfAbsent(key, k -> nextId.getAndIncrement());
    }

    /**
     * Returns the ID associated with the given key, if it exists.
     *
     * @param key the resource location key
     * @return an {@link Optional} containing the ID if it exists, or {@link Optional#empty()} otherwise
     * @throws IllegalArgumentException if the key is null
     * @since 1.0.0
     */
    public Optional<Integer> getId(ResourceLocation key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        return Optional.ofNullable(allocations.get(key));
    }

    /**
     * Reserves a specific ID for the given key.
     *
     * <p>If the ID is already allocated to a different key, an {@link IllegalStateException} is thrown.
     * If the ID is less than the starting ID, an {@link IllegalArgumentException} is thrown.</p>
     *
     * @param key the resource location key
     * @param id the ID to reserve
     * @throws IllegalArgumentException if the key is null or the ID is less than the starting ID
     * @throws IllegalStateException if the ID is already allocated to a different key
     * @since 1.0.0
     */
    public void reserve(ResourceLocation key, int id) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (id < STARTING_ID) {
            throw new IllegalArgumentException("ID must be greater than or equal to " + STARTING_ID);
        }

        // Check if another key already has this ID
        for (Map.Entry<ResourceLocation, Integer> entry : allocations.entrySet()) {
            if (!entry.getKey().equals(key) && entry.getValue().equals(id)) {
                throw new IllegalStateException("ID " + id + " is already allocated to " + entry.getKey());
            }
        }

        allocations.compute(key, (k, existingId) -> {
            if (existingId != null && existingId != id) {
                throw new IllegalStateException("Key " + key + " already has a different ID: " + existingId);
            }
            nextId.updateAndGet(current -> Math.max(current, id + 1));
            return id;
        });
    }

    /**
     * Saves the current allocations to a JSON file.
     *
     * <p>The JSON format is a simple key-value mapping: {"namespace:path": id, "namespace2:path2": id2}</p>
     *
     * @param file the path to the JSON file
     * @throws IOException if an I/O error occurs while writing the file
     * @since 1.0.0
     */
    public void save(Path file) throws IOException {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");
        allocations.forEach((key, value) -> {
            jsonBuilder.append("\"").append(key.toString()).append("\": ").append(value).append(", ");
        });
        if (jsonBuilder.length() > 1) {
            jsonBuilder.setLength(jsonBuilder.length() - 2); // Remove trailing comma and space
        }
        jsonBuilder.append("}");

        Files.createDirectories(file.getParent());
        Files.write(file, jsonBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Loads allocations from a JSON file.
     *
     * <p>The JSON format is expected to be a simple key-value mapping: {"key": value}</p>
     * <p>If the file does not exist, this method returns silently.</p>
     *
     * @param file the path to the JSON file
     * @throws IOException if an I/O error occurs while reading the file
     * @since 1.0.0
     */
    public void load(Path file) throws IOException {
        if (!Files.exists(file)) {
            return;
        }

        String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        content = content.replaceAll("\\s+", "").replaceAll("[{}]", "");
        if (content.isEmpty()) {
            return;
        }

        allocations.clear();
        String[] entries = content.split(",");
        for (String entry : entries) {
            // Find the position of ": after the closing quote of the key
            // JSON format is "namespace:path": value, so we need to find the last ": pattern
            int colonPos = entry.lastIndexOf("\":");
            if (colonPos == -1) {
                throw new IOException("Malformed JSON entry: " + entry);
            }

            // Extract key (remove quotes) and value
            String keyPart = entry.substring(0, colonPos).replaceAll("\"", "");
            String valuePart = entry.substring(colonPos + 2); // Skip ":

            ResourceLocation key = ResourceLocation.of(keyPart);
            int id = Integer.parseInt(valuePart.trim());
            reserve(key, id);
        }
    }

    /**
     * Saves the current allocations to the default JSON file.
     *
     * <p>Default location: mods/WurmModLoader/id_allocations.json</p>
     *
     * @throws IOException if an I/O error occurs while writing the file
     * @since 2.0.0
     */
    public void save() throws IOException {
        save(DEFAULT_FILE);
    }

    /**
     * Loads allocations from the default JSON file.
     *
     * <p>Default location: mods/WurmModLoader/id_allocations.json</p>
     * <p>If the file does not exist, this method returns silently.</p>
     *
     * @throws IOException if an I/O error occurs while reading the file
     * @since 2.0.0
     */
    public void load() throws IOException {
        load(DEFAULT_FILE);
    }

    /**
     * Returns an unmodifiable view of all current ID allocations.
     *
     * @return unmodifiable map of all allocations
     * @since 2.0.0
     */
    public Map<ResourceLocation, Integer> getAllocations() {
        return java.util.Collections.unmodifiableMap(allocations);
    }

    /**
     * Returns the number of allocated IDs.
     *
     * @return the number of allocated IDs
     * @since 1.0.0
     */
    public int size() {
        return allocations.size();
    }

    /**
     * Clears all allocations and resets the next ID to the starting ID.
     *
     * @since 1.0.0
     */
    public void clear() {
        allocations.clear();
        nextId.set(STARTING_ID);
    }
}