package com.garward.wurmmodloader.core.eventlogic.combat.timing;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wurmonline.server.items.Item;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Data descriptor for swing-speed rules that can be shared across mods.
 */
public final class SwingSpeedProfile {

    private final String id;
    private final float minimumSwingSeconds;
    private final boolean rarityEnabled;
    private final float rarityReductionPerTier;
    private final Set<Integer> templateWhitelist;
    private final Set<Byte> materialWhitelist;

    private SwingSpeedProfile(Builder builder) {
        this.id = builder.id;
        this.minimumSwingSeconds = builder.minimumSwingSeconds;
        this.rarityEnabled = builder.rarityEnabled;
        this.rarityReductionPerTier = builder.rarityReductionPerTier;
        this.templateWhitelist = Collections.unmodifiableSet(new HashSet<>(builder.templateWhitelist));
        this.materialWhitelist = Collections.unmodifiableSet(new HashSet<>(builder.materialWhitelist));
    }

    public String getId() {
        return id;
    }

    public float getMinimumSwingSeconds() {
        return minimumSwingSeconds;
    }

    public boolean isRarityEnabled() {
        return rarityEnabled;
    }

    public float getRarityReductionPerTier() {
        return rarityReductionPerTier;
    }

    public boolean matches(Item weapon) {
        if (weapon == null) {
            return templateWhitelist.isEmpty() && materialWhitelist.isEmpty();
        }
        if (!templateWhitelist.isEmpty() && !templateWhitelist.contains(weapon.getTemplateId())) {
            return false;
        }
        if (!materialWhitelist.isEmpty() && !materialWhitelist.contains(weapon.getMaterial())) {
            return false;
        }
        return true;
    }

    public static SwingSpeedProfile fromJson(JsonObject json) {
        Builder builder = new Builder(json.has("id") ? json.get("id").getAsString() : "swing-profile");
        if (json.has("minSwing")) {
            builder.minimumSwingSeconds(json.get("minSwing").getAsFloat());
        }
        if (json.has("rarityReduction")) {
            builder.rarityReductionPerTier(json.get("rarityReduction").getAsFloat());
            builder.rarityEnabled(true);
        }
        if (json.has("rarityEnabled")) {
            builder.rarityEnabled(json.get("rarityEnabled").getAsBoolean());
        }
        if (json.has("templates") && json.get("templates").isJsonArray()) {
            JsonArray arr = json.getAsJsonArray("templates");
            for (JsonElement element : arr) {
                builder.templateId(element.getAsInt());
            }
        }
        if (json.has("materials") && json.get("materials").isJsonArray()) {
            JsonArray arr = json.getAsJsonArray("materials");
            for (JsonElement element : arr) {
                builder.materialId((byte) element.getAsInt());
            }
        }
        return builder.build();
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private float minimumSwingSeconds = 3.0f;
        private boolean rarityEnabled;
        private float rarityReductionPerTier = 0.0f;
        private final Set<Integer> templateWhitelist = new HashSet<>();
        private final Set<Byte> materialWhitelist = new HashSet<>();

        private Builder(String id) {
            this.id = id;
        }

        public Builder minimumSwingSeconds(float seconds) {
            this.minimumSwingSeconds = seconds;
            return this;
        }

        public Builder rarityEnabled(boolean enabled) {
            this.rarityEnabled = enabled;
            return this;
        }

        public Builder rarityReductionPerTier(float seconds) {
            this.rarityReductionPerTier = seconds;
            return this;
        }

        public Builder templateId(int templateId) {
            this.templateWhitelist.add(templateId);
            return this;
        }

        public Builder materialId(byte material) {
            this.materialWhitelist.add(material);
            return this;
        }

        public SwingSpeedProfile build() {
            return new SwingSpeedProfile(this);
        }
    }
}
