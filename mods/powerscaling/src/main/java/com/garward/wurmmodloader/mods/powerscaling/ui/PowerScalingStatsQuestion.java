package com.garward.wurmmodloader.mods.powerscaling.ui;

import com.garward.wurmmodloader.modsupport.questions.ModQuestion;
import com.garward.wurmmodloader.modsupport.questions.ModQuestions;
import com.garward.wurmmodloader.mods.powerscaling.PowerScalingManager;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.questions.Question;

import java.util.Properties;

/**
 * UI question for displaying power scaling stats to players.
 * Shows current power level, combat bonuses, and power sources.
 *
 * @author Power Fantasy RPG Team
 * @since 1.0.0
 */
public class PowerScalingStatsQuestion implements ModQuestion {

    private final Creature player;

    public PowerScalingStatsQuestion(Creature player) {
        this.player = player;
    }

    @Override
    public void answer(Question question, Properties answers) {
        // No action needed for stats display - it's read-only
        // If we add buttons in future (e.g., "Respec" button), handle them here
    }

    @Override
    public void sendQuestion(Question question) {
        PowerScalingManager manager = PowerScalingManager.getInstance();

        // Get player power stats
        int totalPower = manager.getPlayerPowerLevel(player.getWurmId());
        float damageMultiplier = manager.getDamageMultiplier(totalPower);
        float defenseMultiplier = manager.getDefenseMultiplier(totalPower);
        float hpMultiplier = manager.getHpMultiplier(totalPower);

        // Build BML using exact vanilla pattern (from AlertServerMessageQuestion)
        StringBuilder buf = new StringBuilder();
        buf.append(ModQuestions.getBmlHeader(question));
        buf.append("text{type='bold';text='Power Scaling Stats'}");
        buf.append("text{text=''}");
        buf.append("text{text='Total Power: " + totalPower + "'}");
        buf.append("text{text='Damage Multiplier: " + String.format("%.1fx", damageMultiplier) + "'}");
        buf.append("text{text='Defense Multiplier: " + String.format("%.1fx", defenseMultiplier) + "'}");
        buf.append(ModQuestions.createAnswerButton2(question));

        player.getCommunicator().sendBml(300, 300, true, true, buf.toString(), 200, 200, 200, "Power Scaling Stats");
    }
}
