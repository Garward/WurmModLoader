package com.garward.wurmmodloader.api.icon;

/**
 * Enumeration of icon types in the icon registry.
 *
 * <p>Icons can be categorized into different types based on their source and usage:
 * <ul>
 *   <li>{@link #VANILLA} - Icons from the vanilla game (0-1679)</li>
 *   <li>{@link #CUSTOM} - Icons provided by mods (1680+)</li>
 *   <li>{@link #PLACEHOLDER} - Fallback icons for missing resources</li>
 *   <li>{@link #GENERATED} - Dynamically generated icons</li>
 * </ul>
 * </p>
 *
 * @since 1.0.0
 */
public enum IconType {
    /**
     * Vanilla Wurm Unlimited icon (IDs 0-1679).
     *
     * <p>These icons are built into the game client and require no custom pack distribution.</p>
     *
     * @since 1.0.0
     */
    VANILLA,

    /**
     * Custom mod-provided icon (IDs 1680+).
     *
     * <p>These icons are provided by mods and require distribution to clients via icon packs.
     * They are automatically allocated sequential icon IDs starting from 1680.</p>
     *
     * @since 1.0.0
     */
    CUSTOM,

    /**
     * Placeholder or fallback icon.
     *
     * <p>Used when the requested icon is not found. Typically shows a question mark (icon 60).</p>
     *
     * @since 1.0.0
     */
    PLACEHOLDER,

    /**
     * Dynamically generated icon.
     *
     * <p>Icons that are generated at runtime rather than loaded from static image files.
     * This may include procedurally generated icons or composite images.</p>
     *
     * @since 1.0.0
     */
    GENERATED
}
