package com.garward.wurmmodloader.core.eventlogic.materials;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central registry that stores {@link MaterialProfile} definitions by material id.
 * Mods can register or replace profiles at runtime, and JSON helpers are provided for
 * loading large profile sets from disk.
 */
public final class MaterialProfileRegistry {

    private static final Logger LOGGER = Logger.getLogger(MaterialProfileRegistry.class.getName());
    private static final MaterialProfileRegistry INSTANCE = new MaterialProfileRegistry();

    private final Map<Byte, MaterialProfile> profiles = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private MaterialProfileRegistry() {}

    public static MaterialProfileRegistry getInstance() {
        return INSTANCE;
    }

    public void register(MaterialProfile profile) {
        if (profile == null) {
            return;
        }
        profiles.put(profile.getMaterialId(), profile);
        LOGGER.log(Level.FINE, "Registered material profile for id {0}", profile.getMaterialId());
    }

    public void registerAll(Collection<MaterialProfile> profileCollection) {
        if (profileCollection == null || profileCollection.isEmpty()) {
            return;
        }
        profileCollection.forEach(this::register);
    }

    public Optional<MaterialProfile> findProfile(byte materialId) {
        return Optional.ofNullable(profiles.get(materialId));
    }

    public Map<Byte, MaterialProfile> snapshot() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(profiles));
    }

    public void clear() {
        profiles.clear();
    }

    /**
     * Loads profiles from a JSON file. The file can either be a JSON array of profile objects
     * or an object containing a {@code profiles} array.
     */
    public void loadFromJson(Path path) {
        if (path == null || !Files.exists(path)) {
            LOGGER.log(Level.FINE, "Material profile path not found: {0}", path);
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            loadFromJson(reader);
            LOGGER.log(Level.INFO, "Loaded material profiles from {0}", path);
        } catch (IOException | JsonParseException e) {
            LOGGER.log(Level.WARNING, "Failed to load material profiles from " + path, e);
        }
    }

    public void loadFromJson(Reader reader) {
        JsonElement root = gson.fromJson(reader, JsonElement.class);
        if (root == null) {
            return;
        }
        if (root.isJsonArray()) {
            registerAll(fromArray(root.getAsJsonArray()));
        } else if (root.isJsonObject()) {
            JsonObject object = root.getAsJsonObject();
            if (object.has("profiles") && object.get("profiles").isJsonArray()) {
                registerAll(fromArray(object.getAsJsonArray("profiles")));
            } else {
                register(MaterialProfile.fromJson(object));
            }
        }
    }

    private Collection<MaterialProfile> fromArray(JsonArray array) {
        if (array == null || array.size() == 0) {
            return Collections.emptyList();
        }
        java.util.List<MaterialProfile> list = new java.util.ArrayList<>(array.size());
        for (JsonElement element : array) {
            if (element != null && element.isJsonObject()) {
                list.add(MaterialProfile.fromJson(element.getAsJsonObject()));
            }
        }
        return list;
    }
}
