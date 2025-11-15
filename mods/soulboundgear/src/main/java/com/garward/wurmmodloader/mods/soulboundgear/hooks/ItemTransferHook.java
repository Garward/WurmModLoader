package com.garward.wurmmodloader.mods.soulboundgear.hooks;

import com.garward.wurmmodloader.mods.soulboundgear.SoulboundGearManager;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

import java.util.logging.Logger;

/**
 * Hook for preventing soulbound item transfers.
 *
 * <p>Injected into trade, drop, and mail actions to prevent soulbound items
 * from being transferred to other players.</p>
 *
 * @author Power Fantasy RPG Team
 * @version 1.0.0
 */
public class ItemTransferHook {

    private static final Logger logger = Logger.getLogger(ItemTransferHook.class.getName());

    /**
     * Check if an item can be transferred (traded, dropped, mailed).
     * Called from injected code in trade/drop actions.
     *
     * @param item Item being transferred
     * @param performer Player attempting the transfer
     * @return true if transfer is allowed, false if blocked
     */
    public static boolean canTransferItem(Item item, Creature performer) {
        if (item == null) {
            return true;
        }

        try {
            SoulboundGearManager manager = SoulboundGearManager.getInstance();

            // Check if item is soulbound
            if (!manager.isSoulbound(item.getWurmId())) {
                return true; // Not soulbound, allow transfer
            }

            // Item is soulbound - block transfer
            if (performer != null) {
                performer.getCommunicator().sendNormalServerMessage(
                    "The " + item.getName() + " is bound to your soul and cannot be transferred.");
            }

            return false;

        } catch (Exception e) {
            logger.warning("Error checking soulbound status during transfer: " + e.getMessage());
            return true; // Allow transfer on error to avoid breaking gameplay
        }
    }

    /**
     * Check if an item can be dropped.
     * Special case that allows dropping soulbound items (they'll be in corpse on death).
     *
     * @param item Item being dropped
     * @param performer Player dropping the item
     * @param allowDeath true if this is a death drop (corpse)
     * @return true if drop is allowed, false if blocked
     */
    public static boolean canDropItem(Item item, Creature performer, boolean allowDeath) {
        if (item == null) {
            return true;
        }

        try {
            SoulboundGearManager manager = SoulboundGearManager.getInstance();

            // Check if item is soulbound
            if (!manager.isSoulbound(item.getWurmId())) {
                return true; // Not soulbound, allow drop
            }

            // Allow dropping into corpse on death
            if (allowDeath) {
                return true;
            }

            // Block intentional drops
            if (performer != null) {
                performer.getCommunicator().sendNormalServerMessage(
                    "The " + item.getName() + " is bound to your soul and cannot be dropped.");
            }

            return false;

        } catch (Exception e) {
            logger.warning("Error checking soulbound status during drop: " + e.getMessage());
            return true; // Allow drop on error
        }
    }
}
