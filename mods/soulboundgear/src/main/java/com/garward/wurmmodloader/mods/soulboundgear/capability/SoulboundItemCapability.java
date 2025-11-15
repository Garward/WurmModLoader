package com.garward.wurmmodloader.mods.soulboundgear.capability;

import com.garward.wurmmodloader.api.capability.Capability;
import com.garward.wurmmodloader.api.registry.ResourceLocation;
import com.garward.wurmmodloader.mods.soulboundgear.SoulboundItem;

import java.util.*;

/**
 * Capability for storing soulbound item data using the WurmModLoader Capabilities System.
 *
 * <p>This replaces the old SoulboundDatabase + SoulboundItemDAO pattern (~600 lines)
 * with the framework's automatic persistence system.</p>
 *
 * <p><b>Before (manual database):</b></p>
 * <ul>
 *   <li>SoulboundDatabase - 200 lines</li>
 *   <li>SoulboundItemDAO - 250 lines</li>
 *   <li>SoulboundGearManager database methods - 150+ lines</li>
 *   <li>Manual caching, dirty tracking, batch writes</li>
 * </ul>
 *
 * <p><b>After (capabilities):</b></p>
 * <ul>
 *   <li>SoulboundItemCapability - 120 lines (this file)</li>
 *   <li>Framework handles ALL persistence automatically</li>
 *   <li>Lazy loading, dirty tracking, thread-safe storage</li>
 * </ul>
 *
 * <p><b>Serialization Format:</b> Pipe-delimited with escaped values</p>
 * <pre>
 * ownerWurmId|level|currentXP|totalXP|upgradePoints|customName|customDescription|
 * upgradeNodes|materialInfusions|kills|deathsSaved|createdAt|lastUsedAt
 * </pre>
 *
 * @author Power Fantasy RPG Team
 * @since 2.0.0 (Capabilities Migration)
 * @see SoulboundItem The data this capability manages
 */
public class SoulboundItemCapability implements Capability<SoulboundItem> {

    /** Singleton instance - use this everywhere */
    public static final SoulboundItemCapability INSTANCE = new SoulboundItemCapability();

    /** Unique ID: "soulboundgear:soulbound_item" */
    private static final ResourceLocation ID = new ResourceLocation("soulboundgear", "soulbound_item");

    private SoulboundItemCapability() {
        // Private constructor - singleton pattern
    }

    @Override
    public Class<SoulboundItem> getType() {
        return SoulboundItem.class;
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public SoulboundItem createDefaultInstance() {
        // Non-soulbound items get a default instance with owner -1 (not bound)
        // The framework passes itemWurmId when creating, but we don't have it here
        // So we use 0 as placeholder - the actual itemWurmId is set by the framework
        return new SoulboundItem(0L, -1L, System.currentTimeMillis());
    }

    @Override
    public String serialize(SoulboundItem item) {
        // Pipe-delimited format with escaped values
        // Format: ownerWurmId|level|currentXP|totalXP|upgradePoints|customName|customDescription|
        //         upgradeNodes|materialInfusions|kills|deathsSaved|createdAt|lastUsedAt

        StringBuilder sb = new StringBuilder();

        // Basic fields
        sb.append(item.getOwnerWurmId()).append("|");
        sb.append(item.getLevel()).append("|");
        sb.append(item.getCurrentXP()).append("|");
        sb.append(item.getTotalXP()).append("|");
        sb.append(item.getUpgradePoints()).append("|");
        sb.append(escape(item.getCustomName())).append("|");
        sb.append(escape(item.getCustomDescription())).append("|");

        // Upgrade nodes (comma-separated)
        List<String> nodes = item.getUpgradeTreeNodeIds();
        sb.append(nodes.isEmpty() ? "" : String.join(",", nodes)).append("|");

        // Material infusions (format: materialId:stacks,materialId:stacks)
        Map<String, Integer> infusions = item.getMaterialInfusions();
        if (infusions.isEmpty()) {
            sb.append("");
        } else {
            StringJoiner infusionJoiner = new StringJoiner(",");
            for (Map.Entry<String, Integer> entry : infusions.entrySet()) {
                infusionJoiner.add(entry.getKey() + ":" + entry.getValue());
            }
            sb.append(infusionJoiner.toString());
        }
        sb.append("|");

        // Statistics
        sb.append(item.getKills()).append("|");
        sb.append(item.getDeathsSaved()).append("|");
        sb.append(item.getCreatedAt()).append("|");
        sb.append(item.getLastUsedAt());

        return sb.toString();
    }

    @Override
    public SoulboundItem deserialize(String data) {
        try {
            String[] parts = data.split("\\|", -1); // -1 keeps trailing empties
            if (parts.length < 13) {
                return createDefaultInstance();
            }

            long ownerWurmId = Long.parseLong(parts[0]);
            int level = Integer.parseInt(parts[1]);
            int currentXP = Integer.parseInt(parts[2]);
            long totalXP = Long.parseLong(parts[3]);
            int upgradePoints = Integer.parseInt(parts[4]);
            String customName = unescape(parts[5]);
            String customDescription = unescape(parts[6]);

            // Parse upgrade nodes
            List<String> upgradeNodes = new ArrayList<>();
            if (!parts[7].isEmpty()) {
                upgradeNodes.addAll(Arrays.asList(parts[7].split(",")));
            }

            // Parse material infusions
            Map<String, Integer> materialInfusions = new HashMap<>();
            if (!parts[8].isEmpty()) {
                for (String infusion : parts[8].split(",")) {
                    String[] infusionParts = infusion.split(":", 2);
                    if (infusionParts.length == 2) {
                        materialInfusions.put(infusionParts[0], Integer.parseInt(infusionParts[1]));
                    }
                }
            }

            int kills = Integer.parseInt(parts[9]);
            int deathsSaved = Integer.parseInt(parts[10]);
            long createdAt = Long.parseLong(parts[11]);
            long lastUsedAt = Long.parseLong(parts[12]);

            return new SoulboundItem(
                0L, // itemWurmId set by framework
                ownerWurmId,
                level,
                currentXP,
                totalXP,
                upgradePoints,
                customName,
                customDescription,
                upgradeNodes,
                materialInfusions,
                kills,
                deathsSaved,
                createdAt,
                lastUsedAt
            );

        } catch (Exception e) {
            // If parsing fails, return default (not bound)
            return createDefaultInstance();
        }
    }

    /**
     * Escape null and pipe characters for serialization.
     */
    private String escape(String value) {
        if (value == null) {
            return "~NULL~";
        }
        return value.replace("|", "~PIPE~");
    }

    /**
     * Unescape serialized string.
     */
    private String unescape(String value) {
        if (value == null || value.equals("~NULL~")) {
            return null;
        }
        return value.replace("~PIPE~", "|");
    }
}
