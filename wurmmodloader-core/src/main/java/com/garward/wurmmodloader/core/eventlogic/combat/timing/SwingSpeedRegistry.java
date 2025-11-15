package com.garward.wurmmodloader.core.eventlogic.combat.timing;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wurmonline.server.items.Item;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SwingSpeedRegistry {

    private static final Logger LOGGER = Logger.getLogger(SwingSpeedRegistry.class.getName());
    private static final SwingSpeedRegistry INSTANCE = new SwingSpeedRegistry();

    private final List<SwingSpeedProfile> profiles = new CopyOnWriteArrayList<>();
    private final Gson gson = new Gson();

    private SwingSpeedRegistry() {}

    public static SwingSpeedRegistry getInstance() {
        return INSTANCE;
    }

    public void register(SwingSpeedProfile profile) {
        if (profile == null) {
            return;
        }
        profiles.add(profile);
    }

    public Optional<SwingSpeedProfile> findProfile(Item weapon) {
        return profiles.stream().filter(p -> p.matches(weapon)).findFirst();
    }

    public void load(Path file) {
        if (file == null || !Files.exists(file)) {
            return;
        }
        try {
            JsonElement root = gson.fromJson(Files.newBufferedReader(file, StandardCharsets.UTF_8), JsonElement.class);
            if (root == null) {
                return;
            }
            if (root.isJsonArray()) {
                readArray(root.getAsJsonArray());
            } else if (root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();
                if (obj.has("profiles") && obj.get("profiles").isJsonArray()) {
                    readArray(obj.getAsJsonArray("profiles"));
                } else {
                    register(SwingSpeedProfile.fromJson(obj));
                }
            }
            LOGGER.log(Level.INFO, "Loaded swing speed profiles from {0}", file);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load swing speed profiles from " + file, e);
        }
    }

    private void readArray(JsonArray array) {
        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                register(SwingSpeedProfile.fromJson(element.getAsJsonObject()));
            }
        }
    }
}
