package com.garward.wurmmodloader.mods.powerscaling.capability;

import com.garward.wurmmodloader.api.capability.Capability;
import com.garward.wurmmodloader.api.registry.ResourceLocation;
import com.garward.wurmmodloader.mods.powerscaling.PowerLevel;

/**
 * Capability for storing player power levels using the WurmModLoader Capabilities System.
 *
 * <p>This replaces the old PowerScalingDatabase + PowerLevelDAO pattern (~800 lines)
 * with the framework's automatic persistence system.</p>
 *
 * <p><b>Before (manual database):</b></p>
 * <ul>
 *   <li>PowerScalingDatabase - 200 lines</li>
 *   <li>PowerLevelDAO - 250 lines</li>
 *   <li>PowerScalingManager database methods - 300+ lines</li>
 *   <li>Manual caching, dirty tracking, batch writes</li>
 * </ul>
 *
 * <p><b>After (capabilities):</b></p>
 * <ul>
 *   <li>PowerLevelCapability - 50 lines (this file)</li>
 *   <li>Framework handles ALL persistence automatically</li>
 *   <li>Lazy loading, dirty tracking, thread-safe storage</li>
 * </ul>
 *
 * @author Power Fantasy RPG Team
 * @since 2.0.0 (Capabilities Migration)
 * @see PowerLevel The data this capability manages
 */
public class PowerLevelCapability implements Capability<PowerLevel> {

    /** Singleton instance - use this everywhere */
    public static final PowerLevelCapability INSTANCE = new PowerLevelCapability();

    /** Unique ID: "powerscaling:power_level" */
    private static final ResourceLocation ID = new ResourceLocation("powerscaling", "power_level");

    private PowerLevelCapability() {
        // Private constructor - singleton pattern
    }

    @Override
    public Class<PowerLevel> getType() {
        return PowerLevel.class;
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public PowerLevel createDefaultInstance() {
        // New players start with default power level (constructor handles defaults)
        return new PowerLevel(0L); // Note: playerWurmId not needed, framework handles it
    }

    @Override
    public String serialize(PowerLevel powerLevel) {
        // CSV format: basePower,killPowerAccumulated,totalKills,achievementPower,questPower,spentPower,lastUpdated
        return powerLevel.getBasePower() + "," +
               powerLevel.getKillPowerAccumulated() + "," +
               powerLevel.getTotalKills() + "," +
               powerLevel.getAchievementPower() + "," +
               powerLevel.getQuestPower() + "," +
               powerLevel.getSpentPower() + "," +
               powerLevel.getLastUpdated();
    }

    @Override
    public PowerLevel deserialize(String data) {
        try {
            String[] parts = data.split(",");
            int basePower = Integer.parseInt(parts[0]);
            float killPowerAccumulated = Float.parseFloat(parts[1]);
            int totalKills = Integer.parseInt(parts[2]);
            int achievementPower = Integer.parseInt(parts[3]);
            int questPower = Integer.parseInt(parts[4]);

            // Handle backward compatibility - old saves won't have spentPower
            int spentPower = 0;
            long lastUpdated;
            if (parts.length >= 7) {
                spentPower = Integer.parseInt(parts[5]);
                lastUpdated = Long.parseLong(parts[6]);
            } else {
                // Old format without spentPower
                lastUpdated = Long.parseLong(parts[5]);
            }

            return new PowerLevel(
                0L, // playerWurmId not needed
                basePower,
                killPowerAccumulated,
                totalKills,
                achievementPower,
                questPower,
                spentPower,
                lastUpdated
            );
        } catch (Exception e) {
            // If parsing fails, return default
            return createDefaultInstance();
        }
    }
}
