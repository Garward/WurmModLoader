package com.garward.wurmmodloader.core.registry;

import com.garward.wurmmodloader.api.icon.Icon;
import com.garward.wurmmodloader.api.registry.Registry;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * Central registry holder for the WurmModLoader system, providing access to various game content registries.
 *
 * <p>This class manages and provides access to registries for different game elements such as items,
 * creatures, actions, spells, structures, vehicles, and icons. Each registry is a {@link Registry} instance
 * that ensures unique identification of game objects using {@link ResourceLocation} keys.</p>
 *
 * <p>Usage example:
 * <pre>{@code
 * // Access a registry
 * Registry<Object> items = Registries.ITEMS;
 *
 * // Register an object
 * items.register(new ResourceLocation("mymod", "custom_item"), myItemObject);
 *
 * // Freeze all registries after mod initialization
 * Registries.freezeAll();
 *
 * // Get all registries for iteration
 * for (Registry<?> registry : Registries.getAllRegistries()) {
 *     System.out.println(registry.getRegistryName() + ": " + registry.size() + " entries");
 * }
 * }</pre>
 * </p>
 *
 * <p>Thread Safety: This class is thread-safe. All registries are initialized once and can be safely
 * accessed and frozen from multiple threads.</p>
 *
 * @since 1.0.0
 */
public class Registries {
    /**
     * Registry for game items.
     * @since 1.0.0
     */
    public static final Registry<Object> ITEMS = new SimpleRegistry<>("items");

    /**
     * Registry for creature types.
     * @since 1.0.0
     */
    public static final Registry<Object> CREATURES = new SimpleRegistry<>("creatures");

    /**
     * Registry for player actions.
     * @since 1.0.0
     */
    public static final Registry<Object> ACTIONS = new SimpleRegistry<>("actions");

    /**
     * Registry for magical spells.
     * @since 1.0.0
     */
    public static final Registry<Object> SPELLS = new SimpleRegistry<>("spells");

    /**
     * Registry for game structures.
     * @since 1.0.0
     */
    public static final Registry<Object> STRUCTURES = new SimpleRegistry<>("structures");

    /**
     * Registry for vehicles.
     * @since 1.0.0
     */
    public static final Registry<Object> VEHICLES = new SimpleRegistry<>("vehicles");

    /**
     * Registry for icon metadata.
     *
     * <p>This registry manages icon allocations and metadata for the game's sprite sheet system.
     * Icons are identified by {@link ResourceLocation}s and mapped to numeric icon IDs.</p>
     *
     * <p>Usage:
     * <pre>{@code
     * // Register a custom icon
     * ResourceLocation iconId = new ResourceLocation("mymod", "flame_sword");
     * Icon icon = new Icon(iconId, 1680, "mymod_icons.png", IconType.CUSTOM);
     * Registries.ICONS.register(iconId, icon);
     *
     * // Look up icon
     * Optional<Icon> found = Registries.ICONS.get(iconId);
     * }</pre>
     * </p>
     *
     * @since 1.0.0
     */
    public static final Registry<Icon> ICONS = new SimpleRegistry<>("icons");

    private static final List<Registry<?>> REGISTRIES;

    static {
        List<Registry<?>> list = new ArrayList<>();
        list.add(ITEMS);
        list.add(CREATURES);
        list.add(ACTIONS);
        list.add(SPELLS);
        list.add(STRUCTURES);
        list.add(VEHICLES);
        list.add(ICONS);
        REGISTRIES = Collections.unmodifiableList(list);
    }

    /**
     * Returns an unmodifiable list of all registries managed by this class.
     *
     * <p>The list contains all registry instances in a consistent order, allowing for batch operations
     * like freezing or validation.</p>
     *
     * @return an unmodifiable list of all registries
     * @since 1.0.0
     */
    public static List<Registry<?>> getAllRegistries() {
        return REGISTRIES;
    }

    /**
     * Freezes all registries, preventing further registrations.
     *
     * <p>This should be called after all mod initialization is complete. Once frozen, all registries
     * will reject any new registration attempts.</p>
     *
     * @throws IllegalStateException if any registry is already frozen
     * @since 1.0.0
     */
    public static void freezeAll() {
        for (Registry<?> registry : REGISTRIES) {
            registry.freeze();
        }
    }

    /**
     * Private constructor for utility class.
     * @since 1.0.0
     */
    private Registries() {
        // Prevent instantiation
    }
}