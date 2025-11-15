package com.garward.wurmmodloader.core.eventlogic.combat.timing;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wurmonline.server.items.Item;

import java.util.HashSet;
import java.util.Set;

/**
 * Policy describing how weapon timers should be reset when {@link com.garward.wurmmodloader.api.events.combat.WeaponUseEvent} fires.
 */
public final class WeaponTimerPolicy {

    private final String id;
    private final float resetValue;
    private final Set<Integer> templateWhitelist;

    private WeaponTimerPolicy(Builder builder) {
        this.id = builder.id;
        this.resetValue = builder.resetValue;
        this.templateWhitelist = builder.templateWhitelist;
    }

    public boolean matches(Item weapon) {
        if (weapon == null) {
            return templateWhitelist.isEmpty();
        }
        return templateWhitelist.isEmpty() || templateWhitelist.contains(weapon.getTemplateId());
    }

    public float getResetValue() {
        return resetValue;
    }

    public static WeaponTimerPolicy fromJson(JsonObject json) {
        Builder builder = new Builder(json.has("id") ? json.get("id").getAsString() : "weapon-policy");
        if (json.has("resetValue")) {
            builder.resetValue(json.get("resetValue").getAsFloat());
        }
        if (json.has("templates") && json.get("templates").isJsonArray()) {
            JsonArray arr = json.getAsJsonArray("templates");
            for (JsonElement element : arr) {
                builder.templateId(element.getAsInt());
            }
        }
        return builder.build();
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private float resetValue = 0f;
        private final Set<Integer> templateWhitelist = new HashSet<>();

        private Builder(String id) {
            this.id = id;
        }

        public Builder resetValue(float value) {
            this.resetValue = value;
            return this;
        }

        public Builder templateId(int templateId) {
            templateWhitelist.add(templateId);
            return this;
        }

        public WeaponTimerPolicy build() {
            return new WeaponTimerPolicy(this);
        }
    }
}
