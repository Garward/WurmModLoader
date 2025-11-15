package com.garward.wurmmodloader.core.eventlogic.combat.timing;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wurmonline.server.creatures.Creature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DualWieldRegistry {

    private static final Logger LOGGER = Logger.getLogger(DualWieldRegistry.class.getName());
    private static final DualWieldRegistry INSTANCE = new DualWieldRegistry();

    private final CopyOnWriteArrayList<DualWieldProfile> profiles = new CopyOnWriteArrayList<>();
    private final Gson gson = new Gson();

    private DualWieldRegistry() {}

    public static DualWieldRegistry getInstance() {
        return INSTANCE;
    }

    public void register(DualWieldProfile profile) {
        if (profile != null) {
            profiles.add(profile);
        }
    }

    public Optional<DualWieldProfile> findProfile(Creature attacker) {
        return profiles.stream().filter(profile -> profile.matches(attacker)).findFirst();
    }

    public void load(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try {
            JsonElement root = gson.fromJson(Files.newBufferedReader(path, StandardCharsets.UTF_8), JsonElement.class);
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
                    register(DualWieldProfile.fromJson(obj));
                }
            }
            LOGGER.log(Level.INFO, "Loaded dual wield profiles from {0}", path);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load dual wield profiles from " + path, e);
        }
    }

    private void readArray(JsonArray array) {
        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                register(DualWieldProfile.fromJson(element.getAsJsonObject()));
            }
        }
    }
}
