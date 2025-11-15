package com.garward.wurmmodloader.core.icon;

import com.garward.wurmmodloader.api.icon.Icon;
import com.garward.wurmmodloader.api.icon.IconType;
import com.garward.wurmmodloader.api.registry.ResourceLocation;
import com.garward.wurmmodloader.core.registry.Registries;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Helper class for registering and managing icons in the icon registry.
 *
 * <p>This class provides convenient methods for mod developers to register custom icons
 * without needing to directly interact with the underlying {@link Registry} system.
 * It handles icon ID allocation, conflict detection, and provides easy-to-use registration
 * methods for different icon types.</p>
 *
 * <p>Icon System Overview:</p>
 * <ul>
 *   <li><strong>Vanilla icons:</strong> IDs 0-1679 (built into game client)</li>
 *   <li><strong>Custom icons:</strong> IDs 1680+ (auto-allocated, distributed via icon packs)</li>
 *   <li><strong>Sheets:</strong> 240 icons per sheet (20 wide × 12 tall)</li>
 *   <li><strong>Distribution:</strong> Automatic via httpserver + serverpacks (optional)</li>
 * </ul>
 *
 * <p>Basic usage example:
 * <pre>{@code
 * // Register a custom icon
 * ResourceLocation iconId = new ResourceLocation("mymod", "flame_sword");
 * Icon icon = IconRegistry.registerCustom(iconId, "mymod_icons.png");
 *
 * // Use in ItemTemplateBuilder
 * ItemTemplateBuilder builder = new ItemTemplateBuilder("mymod.flame_sword");
 * builder.icon(iconId);  // Will auto-resolve to icon.getIconId()
 * builder.build();
 * }</pre>
 * </p>
 *
 * <p>Reusing vanilla icons:
 * <pre>{@code
 * // Register a reference to vanilla plate armor icon
 * ResourceLocation plateIcon = new ResourceLocation("wurm", "armor_plate_chest");
 * IconRegistry.registerVanilla(plateIcon, 1080);
 *
 * // Use in your mod
 * builder.icon(plateIcon);  // Uses vanilla icon 1080
 * }</pre>
 * </p>
 *
 * <p>Thread Safety: This class is thread-safe. Icon ID allocation is atomic and
 * registration is synchronized through the underlying registry.</p>
 *
 * @since 1.0.0
 */
public final class IconRegistry {

    private static final Logger logger = Logger.getLogger(IconRegistry.class.getName());

    /**
     * Starting icon ID for custom mod icons.
     */
    private static final int CUSTOM_ICON_START = Icon.FIRST_CUSTOM_ICON;

    /**
     * Atomic counter for allocating sequential custom icon IDs.
     */
    private static final AtomicInteger nextCustomIconId = new AtomicInteger(CUSTOM_ICON_START);

    /**
     * Private constructor to prevent instantiation.
     */
    private IconRegistry() {
        throw new AssertionError("IconRegistry should not be instantiated");
    }

    /**
     * Registers a vanilla icon reference.
     *
     * <p>This creates a named reference to an existing vanilla icon ID (0-1679).
     * Use this when you want to reference vanilla icons by name in your code
     * rather than using magic numbers.</p>
     *
     * <p>Example:
     * <pre>{@code
     * // Register reference to vanilla sword icon
     * ResourceLocation swordIcon = new ResourceLocation("wurm", "sword_long");
     * IconRegistry.registerVanilla(swordIcon, 1200);
     *
     * // Later, use by name
     * Optional<Icon> icon = IconRegistry.get(swordIcon);
     * }</pre>
     * </p>
     *
     * @param id the resource location identifier (e.g., "wurm:armor_plate_chest")
     * @param vanillaIconId the vanilla icon ID (must be 0-1679)
     * @return the registered Icon
     * @throws IllegalArgumentException if id is null or vanillaIconId is out of range
     * @throws IllegalStateException if the registry is frozen or ID is already registered
     * @since 1.0.0
     */
    public static Icon registerVanilla(ResourceLocation id, int vanillaIconId) {
        if (id == null) {
            throw new IllegalArgumentException("Icon ID cannot be null");
        }
        if (vanillaIconId < 0 || vanillaIconId > Icon.MAX_VANILLA_ICON) {
            throw new IllegalArgumentException("Vanilla icon ID must be 0-1679, got: " + vanillaIconId);
        }

        Icon icon = new Icon(id, vanillaIconId, "vanilla", IconType.VANILLA);
        Registries.ICONS.register(id, icon);
        logger.info("Registered vanilla icon: " + id + " → iconId=" + vanillaIconId);
        return icon;
    }

    /**
     * Registers a custom mod icon with automatic ID allocation.
     *
     * <p>This is the primary method for registering custom mod icons. The icon ID
     * is automatically allocated starting from 1680, ensuring no conflicts with
     * vanilla icons or other mods.</p>
     *
     * <p>The {@code packFile} parameter specifies the source image file containing
     * this icon. This file should be packaged with your mod JAR in the {@code icons/}
     * directory.</p>
     *
     * <p>Example:
     * <pre>{@code
     * // Register custom icon (gets auto-allocated ID like 1680)
     * ResourceLocation iconId = new ResourceLocation("mymod", "flame_sword");
     * Icon icon = IconRegistry.registerCustom(iconId, "mymod_icons.png");
     *
     * // Icon pack file should be at: src/main/resources/icons/mymod_icons.png
     * // And packaged in JAR at: icons/mymod_icons.png
     * }</pre>
     * </p>
     *
     * @param id the resource location identifier (e.g., "mymod:flame_sword")
     * @param packFile the icon pack file name (e.g., "mymod_icons.png")
     * @return the registered Icon with auto-allocated ID
     * @throws IllegalArgumentException if id or packFile is null
     * @throws IllegalStateException if the registry is frozen or ID is already registered
     * @since 1.0.0
     */
    public static Icon registerCustom(ResourceLocation id, String packFile) {
        if (id == null) {
            throw new IllegalArgumentException("Icon ID cannot be null");
        }
        if (packFile == null) {
            throw new IllegalArgumentException("Pack file cannot be null");
        }

        int iconId = nextCustomIconId.getAndIncrement();
        Icon icon = new Icon(id, iconId, packFile, IconType.CUSTOM);
        Registries.ICONS.register(id, icon);
        logger.info("Registered custom icon: " + id + " → iconId=" + iconId + ", sheet=" + icon.getSheetNumber() + ", packFile=" + packFile);
        return icon;
    }

    /**
     * Registers a placeholder/fallback icon.
     *
     * <p>Placeholder icons show the default question mark icon (ID 60) when
     * the actual icon is not found or not available. This is useful for
     * creating graceful fallbacks.</p>
     *
     * <p>Example:
     * <pre>{@code
     * // Register placeholder for future icon
     * ResourceLocation iconId = new ResourceLocation("mymod", "future_item");
     * IconRegistry.registerPlaceholder(iconId);
     *
     * // Item will show question mark until real icon is added
     * }</pre>
     * </p>
     *
     * @param id the resource location identifier
     * @return the registered placeholder Icon
     * @throws IllegalArgumentException if id is null
     * @throws IllegalStateException if the registry is frozen or ID is already registered
     * @since 1.0.0
     */
    public static Icon registerPlaceholder(ResourceLocation id) {
        if (id == null) {
            throw new IllegalArgumentException("Icon ID cannot be null");
        }

        Icon icon = new Icon(id, Icon.FALLBACK_ICON_ID, "vanilla", IconType.PLACEHOLDER);
        Registries.ICONS.register(id, icon);
        logger.info("Registered placeholder icon: " + id + " → iconId=" + Icon.FALLBACK_ICON_ID);
        return icon;
    }

    /**
     * Registers a custom icon with an explicit icon ID.
     *
     * <p><strong>Warning:</strong> This method should be used sparingly. Prefer
     * {@link #registerCustom(ResourceLocation, String)} which auto-allocates IDs
     * to prevent conflicts.</p>
     *
     * <p>Use cases for explicit IDs:
     * <ul>
     *   <li>Migrating from legacy icon systems</li>
     *   <li>Maintaining backward compatibility with specific icon IDs</li>
     *   <li>Advanced scenarios requiring precise icon placement</li>
     * </ul>
     * </p>
     *
     * @param id the resource location identifier
     * @param iconId the explicit icon ID to use (recommended: 1680+)
     * @param packFile the icon pack file name
     * @param type the icon type
     * @return the registered Icon
     * @throws IllegalArgumentException if any parameter is null or iconId is negative
     * @throws IllegalStateException if the registry is frozen or ID is already registered
     * @since 1.0.0
     */
    public static Icon register(ResourceLocation id, int iconId, String packFile, IconType type) {
        if (id == null) {
            throw new IllegalArgumentException("Icon ID cannot be null");
        }
        if (iconId < 0) {
            throw new IllegalArgumentException("Icon ID cannot be negative: " + iconId);
        }
        if (packFile == null) {
            throw new IllegalArgumentException("Pack file cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Icon type cannot be null");
        }

        Icon icon = new Icon(id, iconId, packFile, type);
        Registries.ICONS.register(id, icon);
        return icon;
    }

    /**
     * Retrieves an icon by its resource location.
     *
     * @param id the resource location to look up
     * @return an Optional containing the icon, or empty if not found
     * @throws IllegalArgumentException if id is null
     * @since 1.0.0
     */
    public static Optional<Icon> get(ResourceLocation id) {
        if (id == null) {
            throw new IllegalArgumentException("Icon ID cannot be null");
        }
        return Registries.ICONS.get(id);
    }

    /**
     * Retrieves an icon by its resource location, with fallback.
     *
     * <p>If the requested icon is not found, this returns a fallback icon
     * (question mark, ID 60) instead of an empty Optional. This is useful
     * when you need a guaranteed icon result.</p>
     *
     * @param id the resource location to look up
     * @return the icon if found, or a fallback icon if not found
     * @throws IllegalArgumentException if id is null
     * @since 1.0.0
     */
    public static Icon getOrFallback(ResourceLocation id) {
        return get(id).orElse(createFallbackIcon(id));
    }

    /**
     * Checks if an icon is registered with the specified resource location.
     *
     * @param id the resource location to check
     * @return true if the icon is registered, false otherwise
     * @throws IllegalArgumentException if id is null
     * @since 1.0.0
     */
    public static boolean contains(ResourceLocation id) {
        if (id == null) {
            throw new IllegalArgumentException("Icon ID cannot be null");
        }
        return Registries.ICONS.containsKey(id);
    }

    /**
     * Gets the total number of registered icons.
     *
     * @return the number of icons in the registry
     * @since 1.0.0
     */
    public static int size() {
        return Registries.ICONS.size();
    }

    /**
     * Gets the number of custom (mod-provided) icons.
     *
     * @return the count of custom icons (ID >= 1680)
     * @since 1.0.0
     */
    public static long countCustomIcons() {
        return Registries.ICONS.values()
                .filter(Icon::isCustom)
                .count();
    }

    /**
     * Gets the number of vanilla icon references.
     *
     * @return the count of vanilla icons (ID 0-1679)
     * @since 1.0.0
     */
    public static long countVanillaIcons() {
        return Registries.ICONS.values()
                .filter(Icon::isVanilla)
                .count();
    }

    /**
     * Gets the next icon ID that will be allocated for custom icons.
     *
     * <p>This is primarily useful for debugging and logging purposes to
     * see how many custom icon IDs have been allocated.</p>
     *
     * @return the next custom icon ID
     * @since 1.0.0
     */
    public static int getNextCustomIconId() {
        return nextCustomIconId.get();
    }

    /**
     * Checks if the icon registry is frozen (no longer accepting registrations).
     *
     * @return true if frozen, false otherwise
     * @since 1.0.0
     */
    public static boolean isFrozen() {
        return Registries.ICONS.isFrozen();
    }

    /**
     * Creates a fallback icon for the specified ID.
     *
     * <p>Internal method used by {@link #getOrFallback(ResourceLocation)}.</p>
     */
    private static Icon createFallbackIcon(ResourceLocation id) {
        return new Icon(id, Icon.FALLBACK_ICON_ID, "vanilla", IconType.PLACEHOLDER);
    }
}
