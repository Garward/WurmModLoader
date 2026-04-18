package com.garward.wurmmodloader.capabilities;

/**
 * Represents a server mod's capability information.
 *
 * <p>This class contains metadata about a server-side mod that clients
 * need to know about to enable/disable corresponding client features.</p>
 *
 * @since 1.0.0
 */
public class ServerModCapability {

    private final String modId;
    private final String version;
    private final String description;

    /**
     * Create a new server mod capability.
     *
     * @param modId Unique mod identifier (e.g., "sprint_system", "power_scaling")
     * @param version Mod version (e.g., "1.0.0", "2.1.3")
     */
    public ServerModCapability(String modId, String version) {
        this(modId, version, "");
    }

    /**
     * Create a new server mod capability with description.
     *
     * @param modId Unique mod identifier
     * @param version Mod version
     * @param description Human-readable description
     */
    public ServerModCapability(String modId, String version, String description) {
        if (modId == null || modId.trim().isEmpty()) {
            throw new IllegalArgumentException("modId cannot be null or empty");
        }
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("version cannot be null or empty");
        }

        this.modId = modId.trim();
        this.version = version.trim();
        this.description = (description != null) ? description.trim() : "";
    }

    /**
     * Get the unique mod identifier.
     *
     * @return Mod ID (e.g., "sprint_system")
     */
    public String getModId() {
        return modId;
    }

    /**
     * Get the mod version.
     *
     * @return Version string (e.g., "1.0.0")
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get the mod description.
     *
     * @return Description or empty string
     */
    public String getDescription() {
        return description;
    }

    /**
     * Check if this mod's version is compatible with a minimum version.
     *
     * <p>Simple string comparison - for semantic versioning, use a proper library.</p>
     *
     * @param minVersion Minimum required version
     * @return true if this version >= minVersion (simple string comparison)
     */
    public boolean isVersionAtLeast(String minVersion) {
        return version.compareTo(minVersion) >= 0;
    }

    @Override
    public String toString() {
        return modId + ":" + version +
               (description.isEmpty() ? "" : " (" + description + ")");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ServerModCapability)) return false;
        ServerModCapability other = (ServerModCapability) obj;
        return modId.equals(other.modId) && version.equals(other.version);
    }

    @Override
    public int hashCode() {
        return modId.hashCode() * 31 + version.hashCode();
    }
}
