package com.garward.wurmmodloader.mods.powerscaling.actions;

import com.garward.wurmmodloader.modsupport.actions.ActionPerformer;
import com.garward.wurmmodloader.modsupport.actions.ActionPropagation;
import com.garward.wurmmodloader.modsupport.questions.ModQuestions;
import com.garward.wurmmodloader.mods.powerscaling.PowerScalingMod;
import com.garward.wurmmodloader.mods.powerscaling.ui.PowerScalingStatsQuestion;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.questions.Question;

/**
 * Action performer that opens the Power Scaling stats UI when triggered.
 * This is called when a player clicks "Power Scaling" in the body context menu.
 *
 * @author Power Fantasy RPG Team
 * @since 1.0.0
 */
public class ViewStatsActionPerformer implements ActionPerformer {

    @Override
    public short getActionId() {
        return (short) PowerScalingMod.ACTION_VIEW_POWER_STATS;
    }

    @Override
    public boolean action(Action action, Creature performer, Item target, short num, float counter) {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("PowerScaling");
        logger.warning("[ViewStatsActionPerformer] action() called! num=" + num + ", actionId=" + getActionId());

        // Verify this is the correct action
        if (num != getActionId()) {
            logger.warning("[ViewStatsActionPerformer] Wrong action ID, propagating");
            return propagate(action, ActionPropagation.SERVER_PROPAGATION, ActionPropagation.ACTION_PERFORMER_PROPAGATION);
        }

        logger.warning("[ViewStatsActionPerformer] Correct action ID! Checking target...");

        // Verify target is the player's body
        if (!target.isBodyPartAttached() || target.getOwnerId() != performer.getWurmId()) {
            logger.warning("[ViewStatsActionPerformer] Target check failed");
            performer.getCommunicator().sendNormalServerMessage("You can only view your own power stats.");
            return propagate(action, ActionPropagation.FINISH_ACTION, ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);
        }

        logger.warning("[ViewStatsActionPerformer] Target check passed, creating question...");

        // Open the Power Scaling stats UI window
        PowerScalingStatsQuestion statsQuestion = new PowerScalingStatsQuestion(performer);
        Question question = ModQuestions.createQuestion(performer, "Power Scaling Stats", "View your power level and combat bonuses",
            performer.getWurmId(), statsQuestion);

        logger.warning("[ViewStatsActionPerformer] Question created, sending to player...");

        // Send the question to the player (this triggers sendQuestion() which shows the UI)
        question.sendQuestion();

        logger.warning("[ViewStatsActionPerformer] Question sent!");
        performer.getCommunicator().sendNormalServerMessage("Opening Power Scaling stats window...");

        // Finish the action - don't propagate to server or other performers
        return propagate(action, ActionPropagation.FINISH_ACTION, ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);
    }
}
