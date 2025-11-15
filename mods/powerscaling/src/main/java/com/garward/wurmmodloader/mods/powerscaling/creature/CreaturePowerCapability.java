package com.garward.wurmmodloader.mods.powerscaling.creature;

import com.garward.wurmmodloader.api.capability.Capability;
import com.garward.wurmmodloader.api.registry.ResourceLocation;

/**
 * Capability for storing dynamic power data on creatures.
 *
 * <p>This capability replaces the old static CR-based power calculation with
 * a dynamic system featuring:
 * <ul>
 *   <li>Randomized base power at spawn</li>
 *   <li>Age-based growth (creatures get stronger over time)</li>
 *   <li>Settlement suppression (weaker near villages)</li>
 *   <li>Performance-optimized caching (30-second TTL)</li>
 * </ul>
 *
 * <p><strong>Persistence:</strong> The framework automatically saves/loads
 * basePower, spawnTimestamp, and spawn location to SQLite. Cached values
 * are transient and recalculated on load.</p>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * CreaturePowerData data = CapabilityManager.getInstance()
 *     .getCapability(creature, CreaturePowerCapability.INSTANCE);
 *
 * if (data != null) {
 *     int power = data.getCachedPower(creature, config);
 *     PowerTier tier = PowerTier.fromPower(power);
 * }
 * }</pre>
 *
 * @author Power Fantasy RPG Team
 * @since 1.0.0
 */
public class CreaturePowerCapability implements Capability<CreaturePowerData> {

    public static final CreaturePowerCapability INSTANCE = new CreaturePowerCapability();
    private static final ResourceLocation ID = new ResourceLocation("powerscaling", "creature_power");

    private CreaturePowerCapability() {
        // Private constructor - singleton pattern
    }

    @Override
    public Class<CreaturePowerData> getType() {
        return CreaturePowerData.class;
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public CreaturePowerData createDefaultInstance() {
        return new CreaturePowerData();
    }

    @Override
    public String serialize(CreaturePowerData data) {
        // Format: basePower,spawnTimestamp,spawnTileX,spawnTileY
        return String.format("%d,%d,%d,%d",
            data.basePower,
            data.spawnTimestamp,
            data.spawnTileX,
            data.spawnTileY
        );
    }

    @Override
    public CreaturePowerData deserialize(String serialized) {
        CreaturePowerData data = new CreaturePowerData();

        try {
            String[] parts = serialized.split(",");
            if (parts.length >= 4) {
                data.basePower = Integer.parseInt(parts[0]);
                data.spawnTimestamp = Long.parseLong(parts[1]);
                data.spawnTileX = Integer.parseInt(parts[2]);
                data.spawnTileY = Integer.parseInt(parts[3]);
            }
        } catch (NumberFormatException e) {
            // Corrupted data, return default
            data = new CreaturePowerData();
        }

        return data;
    }
}
