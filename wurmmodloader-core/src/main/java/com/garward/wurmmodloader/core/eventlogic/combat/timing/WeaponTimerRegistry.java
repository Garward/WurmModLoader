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
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class WeaponTimerRegistry {

    private static final Logger LOGGER = Logger.getLogger(WeaponTimerRegistry.class.getName());
    private static final WeaponTimerRegistry INSTANCE = new WeaponTimerRegistry();

    private final CopyOnWriteArrayList<WeaponTimerPolicy> policies = new CopyOnWriteArrayList<>();
    private final Gson gson = new Gson();

    private WeaponTimerRegistry() {}

    public static WeaponTimerRegistry getInstance() {
        return INSTANCE;
    }

    public void register(WeaponTimerPolicy policy) {
        if (policy != null) {
            policies.add(policy);
        }
    }

    public Optional<WeaponTimerPolicy> findPolicy(Item weapon) {
        return policies.stream().filter(policy -> policy.matches(weapon)).findFirst();
    }

    public void load(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try {
            JsonElement element = gson.fromJson(Files.newBufferedReader(path, StandardCharsets.UTF_8), JsonElement.class);
            if (element == null) {
                return;
            }
            if (element.isJsonArray()) {
                readArray(element.getAsJsonArray());
            } else if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                if (obj.has("policies") && obj.get("policies").isJsonArray()) {
                    readArray(obj.getAsJsonArray("policies"));
                } else {
                    register(WeaponTimerPolicy.fromJson(obj));
                }
            }
            LOGGER.log(Level.INFO, "Loaded weapon timer policies from {0}", path);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load weapon timer policies from " + path, e);
        }
    }

    private void readArray(JsonArray array) {
        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                register(WeaponTimerPolicy.fromJson(element.getAsJsonObject()));
            }
        }
    }
}
