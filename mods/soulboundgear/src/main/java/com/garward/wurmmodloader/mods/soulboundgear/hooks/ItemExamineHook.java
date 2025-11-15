package com.garward.wurmmodloader.mods.soulboundgear.hooks;

import com.garward.wurmmodloader.mods.soulboundgear.SoulboundConfig;
import com.garward.wurmmodloader.mods.soulboundgear.SoulboundGearManager;
import com.garward.wurmmodloader.mods.soulboundgear.SoulboundItem;
import com.wurmonline.server.Players;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Hook for adding soulbound information to item examine text.
 *
 * <p>Injected into Item.examine() or similar to append soulbound status,
 * level, XP, and bonuses to the examine text.</p>
 *
 * @author Power Fantasy RPG Team
 * @version 1.0.0
 */
public class ItemExamineHook {

    private static final Logger logger = Logger.getLogger(ItemExamineHook.class.getName());

    /**
     * Append soulbound information to item examine text.
     * Called from injected code in item examine methods.
     *
     * @param item Item being examined
     * @param originalText Original examine text
     * @param examiner Creature examining the item
     * @return Modified examine text with soulbound info appended
     */
    public static String appendSoulboundInfo(Item item, String originalText, Creature examiner) {
        if (item == null || originalText == null) {
            return originalText;
        }

        try {
            SoulboundGearManager manager = SoulboundGearManager.getInstance();

            // Check if item is soulbound
            Optional<SoulboundItem> soulboundOpt = manager.getItem(item);
            if (!soulboundOpt.isPresent()) {
                return originalText; // Not soulbound
            }

            SoulboundItem soulbound = soulboundOpt.get();
            SoulboundConfig config = SoulboundConfig.getInstance();

            // Build soulbound info text
            StringBuilder sb = new StringBuilder(originalText);
            sb.append("\n\n"); // Separator

            // Header
            sb.append("=== SOULBOUND ===\n");

            // Owner
            try {
                Player owner = Players.getInstance().getPlayerOrNull(soulbound.getOwnerWurmId());
                if (owner != null) {
                    sb.append("Bound to: ").append(owner.getName()).append("\n");
                } else {
                    sb.append("Bound to: Unknown\n");
                }
            } catch (Exception e) {
                sb.append("Bound to: <Player ID ").append(soulbound.getOwnerWurmId()).append(">\n");
            }

            // Level and XP
            int level = soulbound.getLevel();
            int currentXP = soulbound.getCurrentXP();
            int xpNeeded = config.getXPRequiredForLevel(level + 1);

            sb.append("Level: ").append(level);
            if (level < config.getMaxItemLevel()) {
                sb.append(" (").append(currentXP).append("/").append(xpNeeded).append(" XP)\n");
            } else {
                sb.append(" (MAX)\n");
            }

            // Upgrade points
            if (soulbound.getUpgradePoints() > 0) {
                sb.append("Upgrade Points: ").append(soulbound.getUpgradePoints()).append("\n");
            }

            // Statistics
            if (soulbound.getKills() > 0) {
                sb.append("Kills: ").append(soulbound.getKills()).append("\n");
            }

            // Material infusions
            if (!soulbound.getMaterialInfusions().isEmpty()) {
                sb.append("Infusions: ").append(soulbound.getMaterialInfusions().size())
                  .append("/").append(config.getMaxInfusionSlots()).append("\n");
            }

            // Bonuses (brief summary)
            sb.append("\nRight-click item for detailed stats.\n");

            return sb.toString();

        } catch (Exception e) {
            logger.warning("Error appending soulbound info to examine text: " + e.getMessage());
            return originalText; // Return original on error
        }
    }
}
