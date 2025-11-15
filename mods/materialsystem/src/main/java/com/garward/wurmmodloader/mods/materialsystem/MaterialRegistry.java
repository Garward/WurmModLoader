package com.garward.wurmmodloader.mods.materialsystem;

import com.garward.wurmmodloader.api.registry.ResourceLocation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Public API for other mods to query material data.
 *
 * <p>Thread-safe singleton registry for all material bonuses and metadata.
 * SoulboundGear and PowerScaling mods use this to calculate infusion bonuses.</p>
 *
 * @author Power Fantasy RPG Team
 * @version 1.0.0
 */
public final class MaterialRegistry {

    private static final MaterialRegistry INSTANCE = new MaterialRegistry();

    // Material data storage
    private final Map<ResourceLocation, MaterialBonus> bonuses = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, MaterialTier> tiers = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Integer> maxStacks = new ConcurrentHashMap<>();

    /**
     * Private constructor - singleton pattern.
     */
    private MaterialRegistry() {
    }

    /**
     * Get the singleton instance.
     *
     * @return MaterialRegistry instance
     */
    public static MaterialRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Register a material with bonus data.
     *
     * @param id Material ResourceLocation (e.g., "powerfantasy:ifrit_core")
     * @param bonus Bonuses granted by this material
     * @param tier Material tier (COMMON, RARE, LEGENDARY)
     * @param maxStacksPerItem Maximum stacks of this material per item
     */
    public static void register(ResourceLocation id, MaterialBonus bonus,
                                MaterialTier tier, int maxStacksPerItem) {
        INSTANCE.bonuses.put(id, bonus);
        INSTANCE.tiers.put(id, tier);
        INSTANCE.maxStacks.put(id, maxStacksPerItem);
    }

    /**
     * Get material bonus data.
     *
     * @param id Material ResourceLocation
     * @return Material bonus, or empty if not found
     */
    public static Optional<MaterialBonus> getBonus(ResourceLocation id) {
        return Optional.ofNullable(INSTANCE.bonuses.get(id));
    }

    /**
     * Get material tier.
     *
     * @param id Material ResourceLocation
     * @return Material tier, or empty if not found
     */
    public static Optional<MaterialTier> getTier(ResourceLocation id) {
        return Optional.ofNullable(INSTANCE.tiers.get(id));
    }

    /**
     * Get maximum stacks for material.
     *
     * @param id Material ResourceLocation
     * @return Max stacks, or 0 if not found
     */
    public static int getMaxStacks(ResourceLocation id) {
        return INSTANCE.maxStacks.getOrDefault(id, 0);
    }

    /**
     * Check if ResourceLocation is a registered material.
     *
     * @param id ResourceLocation to check
     * @return true if material exists
     */
    public static boolean isMaterial(ResourceLocation id) {
        return INSTANCE.bonuses.containsKey(id);
    }

    /**
     * Get all registered materials.
     *
     * @return Unmodifiable collection of material IDs
     */
    public static Collection<ResourceLocation> getAllMaterials() {
        return Collections.unmodifiableSet(INSTANCE.bonuses.keySet());
    }

    /**
     * Get all materials of a specific tier.
     *
     * @param tier Material tier to filter
     * @return Unmodifiable collection of material IDs
     */
    public static Collection<ResourceLocation> getMaterialsByTier(MaterialTier tier) {
        return INSTANCE.tiers.entrySet().stream()
            .filter(entry -> entry.getValue() == tier)
            .map(Map.Entry::getKey)
            .collect(Collectors.collectingAndThen(
                Collectors.toSet(),
                Collections::unmodifiableSet
            ));
    }

    /**
     * Get total number of registered materials.
     *
     * @return Material count
     */
    public static int size() {
        return INSTANCE.bonuses.size();
    }

    /**
     * Clear all registered materials.
     * Used for testing only.
     */
    static void clear() {
        INSTANCE.bonuses.clear();
        INSTANCE.tiers.clear();
        INSTANCE.maxStacks.clear();
    }
}
