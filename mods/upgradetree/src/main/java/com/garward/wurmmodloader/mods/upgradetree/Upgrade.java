package com.garward.wurmmodloader.mods.upgradetree;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single upgrade that players can unlock.
 * Loaded from JSON configuration.
 *
 * @author Garward
 * @since 1.0.0
 */
public class Upgrade {
    private final String id;
    private final String name;
    private final String description;
    private final int tier;
    private final int cost;
    private final List<String> requirements;
    private final int minUpgradesRequired;
    private final List<UpgradeEffect> effects;

    public Upgrade(String id, String name, String description, int tier, int cost,
                   List<String> requirements, int minUpgradesRequired, List<UpgradeEffect> effects) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.tier = tier;
        this.cost = cost;
        this.requirements = requirements != null ? requirements : new ArrayList<>();
        this.minUpgradesRequired = minUpgradesRequired;
        this.effects = effects != null ? effects : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getTier() {
        return tier;
    }

    public int getCost() {
        return cost;
    }

    public List<String> getRequirements() {
        return requirements;
    }

    public int getMinUpgradesRequired() {
        return minUpgradesRequired;
    }

    public List<UpgradeEffect> getEffects() {
        return effects;
    }

    /**
     * Check if this upgrade has any prerequisite upgrades.
     */
    public boolean hasRequirements() {
        return !requirements.isEmpty() || minUpgradesRequired > 0;
    }

    @Override
    public String toString() {
        return "Upgrade{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", tier=" + tier +
               ", cost=" + cost +
               '}';
    }
}
