package com.garward.wurmmodloader.core.eventlogic.combat.timing;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

import java.util.HashSet;
import java.util.Set;

public final class DualWieldProfile {

    private final String id;
    private final boolean requirePlayer;
    private final boolean skipHugeWeapons;
    private final Set<Integer> templateBlacklist;

    public DualWieldProfile(String id, boolean requirePlayer, boolean skipHugeWeapons, Set<Integer> templateBlacklist) {
        this.id = id;
        this.requirePlayer = requirePlayer;
        this.skipHugeWeapons = skipHugeWeapons;
        this.templateBlacklist = templateBlacklist;
    }

    public boolean matches(Creature attacker) {
        return attacker != null && (!requirePlayer || attacker.isPlayer());
    }

    public boolean allowWeapon(Item weapon) {
        if (weapon == null) {
            return false;
        }
        if (skipHugeWeapons && (weapon.getTemplateId() == 12 || weapon.getTemplateId() == 17)) {
            return false;
        }
        return !templateBlacklist.contains(weapon.getTemplateId());
    }

    public String getId() {
        return id;
    }

    public static DualWieldProfile fromJson(JsonObject json) {
        String id = json.has("id") ? json.get("id").getAsString() : "dualwield-profile";
        boolean requirePlayer = !json.has("requirePlayer") || json.get("requirePlayer").getAsBoolean();
        boolean skipHuge = json.has("skipHugeWeapons") ? json.get("skipHugeWeapons").getAsBoolean() : true;
        Set<Integer> blacklist = new HashSet<>();
        if (json.has("templateBlacklist") && json.get("templateBlacklist").isJsonArray()) {
            JsonArray arr = json.getAsJsonArray("templateBlacklist");
            for (JsonElement element : arr) {
                blacklist.add(element.getAsInt());
            }
        }
        return new DualWieldProfile(id, requirePlayer, skipHuge, blacklist);
    }
}
