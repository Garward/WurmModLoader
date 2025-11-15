package com.garward.wurmmodloader.api.registry;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents a namespaced identifier for game resources in the format "namespace:path".
 *
 * <p>ResourceLocations provide unique identifiers for mod-registered content, ensuring
 * no conflicts between mods. The namespace typically represents the mod ID, and the path
 * represents the specific resource within that mod.</p>
 *
 * <p>Format rules:
 * <ul>
 *   <li>Namespace and path must contain only lowercase letters, numbers, underscores, hyphens, and dots</li>
 *   <li>Must be in the format "namespace:path" (colon-separated)</li>
 *   <li>Example: "mymod:magic_sword", "wurm:iron_ore"</li>
 * </ul>
 * </p>
 *
 * <p>Usage example:
 * <pre>{@code
 * // Creating a resource location
 * ResourceLocation loc = new ResourceLocation("mymod", "magic_sword");
 * String fullName = loc.toString(); // "mymod:magic_sword"
 *
 * // Parsing from string
 * ResourceLocation parsed = ResourceLocation.of("mymod:magic_sword");
 * }</pre>
 * </p>
 *
 * <p>This class is immutable and thread-safe.</p>
 *
 * @since 1.0.0
 */
public final class ResourceLocation implements Comparable<ResourceLocation> {

    /**
     * Pattern for valid namespace and path components.
     * Allows: lowercase letters, numbers, underscores, hyphens, and dots.
     */
    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-z0-9_.-]+$");

    /**
     * Default namespace for vanilla Wurm content.
     */
    public static final String DEFAULT_NAMESPACE = "wurm";

    private final String namespace;
    private final String path;

    /**
     * Constructs a new ResourceLocation with the specified namespace and path.
     *
     * @param namespace the namespace (typically the mod ID)
     * @param path the resource path within the namespace
     * @throws IllegalArgumentException if namespace or path are null, empty, or contain invalid characters
     * @since 1.0.0
     */
    public ResourceLocation(String namespace, String path) {
        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException("Namespace cannot be null or empty");
        }
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        if (!VALID_PATTERN.matcher(namespace).matches()) {
            throw new IllegalArgumentException(
                "Invalid namespace '" + namespace + "': must contain only lowercase letters, numbers, underscores, hyphens, and dots"
            );
        }
        if (!VALID_PATTERN.matcher(path).matches()) {
            throw new IllegalArgumentException(
                "Invalid path '" + path + "': must contain only lowercase letters, numbers, underscores, hyphens, and dots"
            );
        }

        this.namespace = namespace;
        this.path = path;
    }

    /**
     * Creates a ResourceLocation by parsing a combined string in the format "namespace:path".
     *
     * <p>If no colon is present, the entire string is treated as the path with the default
     * namespace ({@value #DEFAULT_NAMESPACE}).</p>
     *
     * @param combined the combined string to parse
     * @return a new ResourceLocation
     * @throws IllegalArgumentException if the string is null, empty, or malformed
     * @since 1.0.0
     */
    public static ResourceLocation of(String combined) {
        if (combined == null || combined.isEmpty()) {
            throw new IllegalArgumentException("Combined resource location cannot be null or empty");
        }

        int colonIndex = combined.indexOf(':');
        if (colonIndex == -1) {
            // No namespace specified, use default
            return new ResourceLocation(DEFAULT_NAMESPACE, combined);
        }

        if (colonIndex == 0) {
            throw new IllegalArgumentException("Namespace cannot be empty in '" + combined + "'");
        }

        if (colonIndex == combined.length() - 1) {
            throw new IllegalArgumentException("Path cannot be empty in '" + combined + "'");
        }

        String namespace = combined.substring(0, colonIndex);
        String path = combined.substring(colonIndex + 1);

        return new ResourceLocation(namespace, path);
    }

    /**
     * Gets the namespace component of this resource location.
     *
     * @return the namespace (typically the mod ID)
     * @since 1.0.0
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Gets the path component of this resource location.
     *
     * @return the resource path
     * @since 1.0.0
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the string representation in the format "namespace:path".
     *
     * @return the full resource location string
     * @since 1.0.0
     */
    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    /**
     * Checks if this ResourceLocation equals another object.
     *
     * <p>Two ResourceLocations are equal if both their namespace and path are equal.</p>
     *
     * @param obj the object to compare with
     * @return true if equal, false otherwise
     * @since 1.0.0
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ResourceLocation)) return false;
        ResourceLocation other = (ResourceLocation) obj;
        return namespace.equals(other.namespace) && path.equals(other.path);
    }

    /**
     * Returns the hash code for this ResourceLocation.
     *
     * @return the hash code
     * @since 1.0.0
     */
    @Override
    public int hashCode() {
        return Objects.hash(namespace, path);
    }

    /**
     * Compares this ResourceLocation with another for ordering.
     *
     * <p>Comparison is done first by namespace, then by path (lexicographically).</p>
     *
     * @param other the ResourceLocation to compare with
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object
     * @since 1.0.0
     */
    @Override
    public int compareTo(ResourceLocation other) {
        int namespaceCompare = this.namespace.compareTo(other.namespace);
        if (namespaceCompare != 0) {
            return namespaceCompare;
        }
        return this.path.compareTo(other.path);
    }
}
