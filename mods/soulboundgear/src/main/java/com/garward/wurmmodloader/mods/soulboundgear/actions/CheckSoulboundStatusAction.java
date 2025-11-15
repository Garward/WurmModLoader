package com.garward.wurmmodloader.mods.soulboundgear.actions;

import com.garward.wurmmodloader.mods.soulboundgear.SoulboundBonuses;
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
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Action to check detailed soulbound status of an item.
 *
 * <p>Right-click soulbound item → "Check Soulbound Status"</p>
 *
 * @author Power Fantasy RPG Team
 * @version 1.0.0
 */
public class CheckSoulboundStatusAction implements ModAction {

    private static final Logger logger = Logger.getLogger(CheckSoulboundStatusAction.class.getName());

    private final short actionId;
    private final ActionEntry actionEntry;

    public CheckSoulboundStatusAction() {
        logger.info("CheckSoulboundStatusAction()");

        actionId = (short) ModActions.getNextActionId();
        actionEntry = ActionEntry.createEntry(
            actionId,
            "Check Soulbound Status",
            "checking status",
            new int[0]
        );
        ModActions.registerAction(actionEntry);
    }

    @Override
    public BehaviourProvider getBehaviourProvider() {
        return new BehaviourProvider() {
            // Single-item action: right-click item
            @Override
            public List<ActionEntry> getBehavioursFor(Creature performer, Item target) {
                // Show action only on soulbound items
                SoulboundGearManager manager = SoulboundGearManager.getInstance();
                if (manager.isSoulbound(target.getWurmId())) {
                    return Collections.singletonList(actionEntry);
                }
                return null;
            }

            // Also handle activated item case
            @Override
            public List<ActionEntry> getBehavioursFor(Creature performer, Item source, Item target) {
                return getBehavioursFor(performer, target);
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

            // Single-item action
            @Override
            public boolean action(Action act, Creature performer, Item target, short action, float counter) {
                SoulboundGearManager manager = SoulboundGearManager.getInstance();
                Optional<SoulboundItem> itemOpt = manager.getItem(target.getWurmId());

                if (!itemOpt.isPresent()) {
                    performer.getCommunicator().sendNormalServerMessage("This item is not soulbound.");
                    return true;
                }

                SoulboundItem item = itemOpt.get();
                SoulboundConfig config = SoulboundConfig.getInstance();

                // Build detailed status message
                StringBuilder sb = new StringBuilder();
                sb.append("═══════════════════════════════════════\n");
                sb.append("SOULBOUND ITEM STATUS\n");
                sb.append("═══════════════════════════════════════\n\n");

                // Basic info
                sb.append("Item: ").append(target.getName()).append("\n");
                sb.append("Level: ").append(item.getLevel());
                if (item.getLevel() < config.getMaxItemLevel()) {
                    int xpNeeded = config.getXPRequiredForLevel(item.getLevel() + 1);
                    sb.append(" (").append(item.getCurrentXP()).append("/").append(xpNeeded).append(" XP)\n");
                } else {
                    sb.append(" (MAX)\n");
                }

                sb.append("Total XP Earned: ").append(item.getTotalXP()).append("\n");
                sb.append("Upgrade Points: ").append(item.getUpgradePoints()).append("\n\n");

                // Bonuses
                SoulboundBonuses bonuses = item.getBonuses();
                sb.append("--- BONUSES ---\n");
                sb.append(bonuses.toDisplayString());
                sb.append("\n");

                // Material infusions
                Map<String, Integer> infusions = item.getMaterialInfusions();
                if (!infusions.isEmpty()) {
                    sb.append("--- MATERIAL INFUSIONS ---\n");
                    for (Map.Entry<String, Integer> entry : infusions.entrySet()) {
                        sb.append("  ").append(entry.getKey()).append(" x").append(entry.getValue()).append("\n");
                    }
                    sb.append("\n");
                }

                // Upgrade tree nodes
                List<String> nodes = item.getUpgradeTreeNodeIds();
                if (!nodes.isEmpty()) {
                    sb.append("--- UPGRADE NODES ---\n");
                    for (String nodeId : nodes) {
                        sb.append("  ").append(nodeId).append("\n");
                    }
                    sb.append("\n");
                }

                // Statistics
                sb.append("--- STATISTICS ---\n");
                sb.append("Kills: ").append(item.getKills()).append("\n");
                if (item.getDeathsSaved() > 0) {
                    sb.append("Deaths Saved: ").append(item.getDeathsSaved()).append("\n");
                }

                // Calculate age
                long ageMs = System.currentTimeMillis() - item.getCreatedAt();
                long ageDays = ageMs / (1000 * 60 * 60 * 24);
                sb.append("Bound: ").append(ageDays).append(" days ago\n");

                sb.append("\n═══════════════════════════════════════\n");

                // Send to player (split into chunks to avoid truncation)
                String message = sb.toString();
                String[] lines = message.split("\n");
                StringBuilder chunk = new StringBuilder();

                for (String line : lines) {
                    chunk.append(line).append("\n");

                    // Send chunks of ~500 chars
                    if (chunk.length() > 500) {
                        performer.getCommunicator().sendNormalServerMessage(chunk.toString());
                        chunk = new StringBuilder();
                    }
                }

                // Send remaining chunk
                if (chunk.length() > 0) {
                    performer.getCommunicator().sendNormalServerMessage(chunk.toString());
                }

                return true;
            }

            // Also handle activated item case
            @Override
            public boolean action(Action act, Creature performer, Item source, Item target, short action, float counter) {
                return this.action(act, performer, target, action, counter);
            }
        };
    }
}
