package com.garward.wurmmodloader.mods.soulboundgear.hooks;

import com.garward.wurmmodloader.mods.soulboundgear.SoulboundConfig;
import com.garward.wurmmodloader.mods.soulboundgear.SoulboundGearManager;
import com.wurmonline.server.Items;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hook for awarding XP to soulbound weapons on creature kills.
 *
 * <p>Called from Creature.die() via Javassist hook. Awards XP to all
 * soulbound weapons equipped by the killer.</p>
 *
 * @author Power Fantasy RPG Team
 * @version 1.0.0
 */
public class XPAwardHook {

    private static final Logger logger = Logger.getLogger(XPAwardHook.class.getName());

    /**
     * Award XP to soulbound items when a creature dies.
     * Called from injected code in Creature.die().
     *
     * @param deadCreature The creature that died
     * @param killer The creature that killed it (may be null)
     */
    public static void onCreatureDeath(Creature deadCreature, Creature killer) {
        // Validate inputs
        if (deadCreature == null || killer == null) {
            return;
        }

        // Only award XP for player kills
        if (!killer.isPlayer()) {
            return;
        }

        // Don't award XP for killing other players (PvP)
        if (deadCreature.isPlayer()) {
            return;
        }

        try {
            SoulboundGearManager manager = SoulboundGearManager.getInstance();
            SoulboundConfig config = SoulboundConfig.getInstance();

            // Get killer's primary weapon
            Item primaryWeapon = killer.getPrimWeapon();

            if (primaryWeapon == null) {
                // No weapon equipped
                return;
            }

            // Check if weapon is soulbound
            if (!manager.isSoulbound(primaryWeapon.getWurmId())) {
                return;
            }

            // Calculate XP to award
            float creatureCR = deadCreature.getBaseCombatRating();
            boolean isChampion = deadCreature.isChampion();
            boolean isUnique = deadCreature.isUnique();

            // Check for titan flag (custom creature type, may not exist yet)
            boolean isTitan = false;
            // TODO: When titan system is implemented, check:
            // isTitan = deadCreature.getTemplate().getName().contains("Titan");

            // Award XP
            boolean leveledUp = manager.awardKillXP(
                primaryWeapon.getWurmId(),
                creatureCR,
                isChampion,
                isUnique,
                isTitan
            );

            // Notify player
            if (config.isNotifyOnXPGain()) {
                int xpGained = calculateXPAmount(config, creatureCR, isChampion, isUnique, isTitan);
                if (xpGained >= config.getMinXPForNotification()) {
                    killer.getCommunicator().sendNormalServerMessage(
                        String.format("Your %s gains %d XP from the kill!",
                            primaryWeapon.getName(), xpGained)
                    );
                }
            }

            if (leveledUp && config.isNotifyOnLevelUp()) {
                killer.getCommunicator().sendSafeServerMessage(
                    "§§§Your " + primaryWeapon.getName() + " has leveled up!");
                killer.getCommunicator().sendNormalServerMessage(
                    "You feel the weapon grow stronger in your hands.");
            }

            if (config.isLogXPCalculations()) {
                logger.fine(String.format("Awarded XP to item %d from kill of %s (CR=%.1f, champion=%b, unique=%b)",
                    primaryWeapon.getWurmId(), deadCreature.getName(), creatureCR, isChampion, isUnique));
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error awarding XP on creature death", e);
        }
    }

    /**
     * Calculate XP amount for display purposes.
     * Duplicates logic from SoulboundGearManager.awardKillXP().
     *
     * @param config Config instance
     * @param creatureCR Creature combat rating
     * @param isChampion true if champion
     * @param isUnique true if unique
     * @param isTitan true if titan
     * @return XP amount
     */
    private static int calculateXPAmount(SoulboundConfig config, float creatureCR,
                                        boolean isChampion, boolean isUnique, boolean isTitan) {
        int baseXP = config.getBaseXPPerKill();
        float crBonus = creatureCR * config.getCrXPMultiplier();
        float multiplier = 1.0f;

        if (isTitan) {
            multiplier = config.getTitanXPMultiplier();
        } else if (isUnique) {
            multiplier = config.getUniqueXPMultiplier();
        } else if (isChampion) {
            multiplier = config.getChampionXPMultiplier();
        }

        return (int) ((baseXP + crBonus) * multiplier);
    }
}
