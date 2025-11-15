package org.tyoda.wurm.Iconzz;

import com.garward.wurmmodloader.api.icon.Icon;
import com.garward.wurmmodloader.api.registry.ResourceLocation;
import com.garward.wurmmodloader.core.icon.IconRegistry;

import java.util.logging.Logger;

/**
 * Compatibility shim for legacy mods using the old TYODAS ICONZZ API.
 *
 * <p>This class redirects calls to the unified WurmModLoader Icon Registry system,
 * allowing old mods to work without code changes. All icons registered through this
 * shim are automatically converted to the modern ResourceLocation-based system.</p>
 *
 * <p><strong>For New Mods:</strong> Use {@link IconRegistry} directly instead of this
 * compatibility layer.</p>
 *
 * <p>Thread Safety: This class is thread-safe, delegating to the thread-safe
 * {@link IconRegistry} implementation.</p>
 *
 * @deprecated Use {@link IconRegistry} directly for new mods. This class exists
 *             only for backward compatibility with legacy ICONZZ-dependent mods.
 * @since 1.0.0
 */
@Deprecated
public class Iconzz {

    private static final Logger logger = Logger.getLogger(Iconzz.class.getName());
    private static final Iconzz INSTANCE = new Iconzz();

    /**
     * Private constructor for singleton pattern.
     */
    private Iconzz() {
        logger.info("ICONZZ compatibility shim initialized - redirecting to WurmModLoader Icon Registry");
    }

    /**
     * Gets the singleton instance of the ICONZZ compatibility shim.
     *
     * @return the singleton Iconzz instance
     * @since 1.0.0
     */
    public static Iconzz getInstance() {
        return INSTANCE;
    }

    /**
     * Registers an icon with the given name and file path.
     *
     * <p>This method automatically converts the old ICONZZ API to the modern
     * Icon Registry format:</p>
     * <ul>
     *   <li>Icon name format "modname.icon_name" → ResourceLocation("modname", "icon_name")</li>
     *   <li>File path "mods/modname/icons/file.png" → "file.png" (filename only)</li>
     *   <li>Returns icon ID as short for compatibility</li>
     * </ul>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Old ICONZZ API
     * short iconId = Iconzz.getInstance().addIcon(
     *     "mymod.custom_icon",
     *     "mods/mymod/icons/custom.png"
     * );
     *
     * // Automatically converts to:
     * // ResourceLocation("mymod", "custom_icon")
     * // with packFile "custom.png"
     * }</pre>
     *
     * @param iconName the icon identifier (format: "namespace.path" or just "name")
     * @param filePath the icon file path (full path or just filename)
     * @return the allocated icon ID as a short
     * @throws IllegalArgumentException if iconName or filePath is null
     * @throws IllegalStateException if the icon registry is frozen
     * @since 1.0.0
     */
    public short addIcon(String iconName, String filePath) {
        if (iconName == null) {
            throw new IllegalArgumentException("Icon name cannot be null");
        }
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }

        // Extract namespace and path from icon name
        // Format: "modname.icon_name" or just "icon_name"
        String namespace;
        String path;

        int dotIndex = iconName.indexOf('.');
        if (dotIndex > 0 && dotIndex < iconName.length() - 1) {
            namespace = iconName.substring(0, dotIndex);
            path = iconName.substring(dotIndex + 1);
        } else {
            // No namespace specified, use "iconzz" as default
            namespace = "iconzz";
            path = iconName;
        }

        // Extract filename from path
        // Handle formats: "mods/modname/icons/file.png" or "org/package/path/file.png" or "file.png"
        String filename = filePath;
        int lastSlash = filePath.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < filePath.length() - 1) {
            filename = filePath.substring(lastSlash + 1);
        }

        // Log the conversion for debugging
        logger.fine(String.format(
            "ICONZZ compat: Converting '%s' (file: '%s') → ResourceLocation('%s', '%s') with packFile '%s'",
            iconName, filePath, namespace, path, filename
        ));

        // Register with modern Icon Registry
        ResourceLocation id = new ResourceLocation(namespace, path);
        Icon icon = IconRegistry.registerCustom(id, filename);

        return (short) icon.getIconId();
    }

    /**
     * Gets the icon ID for a previously registered icon.
     *
     * <p>This is a convenience method for looking up icons by their original
     * ICONZZ-style name. Returns -1 if the icon is not found.</p>
     *
     * @param iconName the icon identifier (format: "namespace.path")
     * @return the icon ID, or -1 if not found
     * @since 1.0.0
     */
    public short getIcon(String iconName) {
        if (iconName == null) {
            return -1;
        }

        // Parse namespace and path
        String namespace;
        String path;

        int dotIndex = iconName.indexOf('.');
        if (dotIndex > 0 && dotIndex < iconName.length() - 1) {
            namespace = iconName.substring(0, dotIndex);
            path = iconName.substring(dotIndex + 1);
        } else {
            namespace = "iconzz";
            path = iconName;
        }

        ResourceLocation id = new ResourceLocation(namespace, path);
        return IconRegistry.get(id)
            .map(icon -> (short) icon.getIconId())
            .orElse((short) -1);
    }

    /**
     * Checks if an icon with the given name is registered.
     *
     * @param iconName the icon identifier (format: "namespace.path")
     * @return true if the icon exists, false otherwise
     * @since 1.0.0
     */
    public boolean hasIcon(String iconName) {
        return getIcon(iconName) != -1;
    }

    /**
     * Dumps all registered icons to the log for debugging.
     *
     * @since 1.0.0
     */
    public void debugList() {
        logger.info("=== ICONZZ Compatibility Shim - Registered Icons ===");
        logger.info("Total icons: " + IconRegistry.size());
        logger.info("Custom icons: " + IconRegistry.countCustomIcons());
        logger.info("Vanilla refs: " + IconRegistry.countVanillaIcons());
        logger.info("Next custom ID: " + IconRegistry.getNextCustomIconId());
        logger.info("Registry frozen: " + IconRegistry.isFrozen());
    }
}
