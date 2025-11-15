package com.garward.wurmmodloader.mods.soulboundgear.actions;

import com.garward.wurmmodloader.mods.soulboundgear.SoulboundConfig;
import com.garward.wurmmodloader.mods.soulboundgear.SoulboundGearManager;
import com.garward.wurmmodloader.mods.soulboundgear.SoulboundItem;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action to bind an item to a player's soul.
 *
 * <p>For Phase 2, simplified: use weapon on any altar to bind it.
 * In Phase 3, we can add deity requirements.</p>
 *
 * @author Power Fantasy RPG Team
 * @version 1.0.0
 */
public class BindSoulAction implements ModAction {

    private static final Logger logger = Logger.getLogger(BindSoulAction.class.getName());

    private final short actionId;
    private final ActionEntry actionEntry;

    public BindSoulAction() {
        logger.info("BindSoulAction()");

        actionId = (short) ModActions.getNextActionId();
        actionEntry = ActionEntry.createEntry(
            actionId,
            "Bind Soul",
            "binding soul",
            new int[0]
        );
        ModActions.registerAction(actionEntry);
    }

    @Override
    public BehaviourProvider getBehaviourProvider() {
        return new BehaviourProvider() {
            // Two-item action: weapon + altar
            @Override
            public List<ActionEntry> getBehavioursFor(Creature performer, Item source, Item target) {
                // Show action when using a weapon on an altar
                if (source != null && target != null) {
                    if (isWeapon(source) && isAltar(target)) {
                        // Don't show if already soulbound
                        SoulboundGearManager manager = SoulboundGearManager.getInstance();
                        if (!manager.isSoulbound(source.getWurmId())) {
                            return Collections.singletonList(actionEntry);
                        }
                    }
                }
                return null;
            }
        };
    }

    @Override
    public ActionPerformer getActionPerformer() {
        return new ActionPerformer() {
            @Override
            public short getActionId() {
                return actionId;
            }

            // Two-item action: weapon + altar
            @Override
            public boolean action(Action act, Creature performer, Item source, Item target, short action, float counter) {
                try {
                    // Validate weapon
                    if (!isWeapon(source)) {
                        performer.getCommunicator().sendNormalServerMessage(
                            "You can only bind weapons to your soul.");
                        return true;
                    }

                    // Validate altar
                    if (!isAltar(target)) {
                        performer.getCommunicator().sendNormalServerMessage(
                            "You must bind your soul at an altar.");
                        return true;
                    }

                    // Check if already soulbound
                    SoulboundGearManager manager = SoulboundGearManager.getInstance();
                    if (manager.isSoulbound(source.getWurmId())) {
                        performer.getCommunicator().sendNormalServerMessage(
                            "This item is already soulbound.");
                        return true;
                    }

                    // Perform binding
                    SoulboundItem boundItem = manager.bindItem(source, performer.getWurmId());

                    // Success messages
                    performer.getCommunicator().sendNormalServerMessage(
                        "You bind your soul to the " + source.getName() + "!");
                    performer.getCommunicator().sendNormalServerMessage(
                        "The item resonates with your essence. It will grow stronger as you defeat enemies.");

                    // Update item name
                    SoulboundConfig config = SoulboundConfig.getInstance();
                    if (config.isAllowCustomNames()) {
                        String originalName = source.getName();
                        if (!originalName.contains("(Soulbound)")) {
                            source.setName(originalName + " (Soulbound)");
                        }
                    }

                    logger.info("Player " + performer.getName() + " bound item " + source.getWurmId() +
                               " (level " + boundItem.getLevel() + ")");

                } catch (IllegalStateException e) {
                    performer.getCommunicator().sendNormalServerMessage(
                        "Failed to bind soul: " + e.getMessage());
                    logger.log(Level.WARNING, "Failed to bind item", e);
                } catch (Exception e) {
                    performer.getCommunicator().sendNormalServerMessage(
                        "An error occurred while binding your soul.");
                    logger.log(Level.SEVERE, "Unexpected error in soul binding", e);
                }

                return true;
            }
        };
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private static boolean isAltar(Item item) {
        // Check template name for "altar"
        String name = item.getTemplate().getName().toLowerCase();
        return name.contains("altar");
    }

    private static boolean isWeapon(Item item) {
        // Check if item is a weapon type
        return item.isWeapon() || item.isWeaponBow() || item.isWeaponAxe() ||
               item.isWeaponKnife() || item.isWeaponSword();
    }
}
