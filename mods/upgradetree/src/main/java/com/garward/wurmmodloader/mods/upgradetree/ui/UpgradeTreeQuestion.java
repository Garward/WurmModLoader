package com.garward.wurmmodloader.mods.upgradetree.ui;

import com.garward.wurmmodloader.modsupport.questions.ModQuestion;
import com.garward.wurmmodloader.modsupport.questions.ModQuestions;
import com.garward.wurmmodloader.mods.upgradetree.Upgrade;
import com.garward.wurmmodloader.mods.upgradetree.UpgradeTreeManager;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.questions.Question;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * UI question for displaying upgrade tree to players.
 * Shows available upgrades and allows unlocking with power points.
 *
 * @author Power Fantasy RPG Team
 * @since 1.0.0
 */
public class UpgradeTreeQuestion implements ModQuestion {

    private final Creature player;
    private int currentPage = 0;
    private static final int UPGRADES_PER_PAGE = 5;

    public UpgradeTreeQuestion(Creature player) {
        this.player = player;
    }

    @Override
    public void answer(Question question, Properties answers) {
        // Check if user clicked navigation buttons
        String prevPage = answers.getProperty("prevpage");
        String nextPage = answers.getProperty("nextpage");
        String unlock = answers.getProperty("unlock");

        if (prevPage != null && currentPage > 0) {
            // Go to previous page
            currentPage--;
            reopenWindow();
            return;
        }

        if (nextPage != null) {
            // Go to next page
            UpgradeTreeManager manager = UpgradeTreeManager.getInstance();
            int totalUpgrades = manager.getAllUpgrades().size();
            int totalPages = (int) Math.ceil((double) totalUpgrades / UPGRADES_PER_PAGE);
            if (currentPage < totalPages - 1) {
                currentPage++;
                reopenWindow();
                return;
            }
        }

        if (unlock != null) {
            // Get selected upgrade from radio button
            String selectedUpgrade = answers.getProperty("upgrade");
            if (selectedUpgrade != null && !selectedUpgrade.isEmpty()) {
                // TODO: Implement unlock logic here
                player.getCommunicator().sendNormalServerMessage("Selected upgrade: " + selectedUpgrade + " (unlock not yet implemented)");
            } else {
                player.getCommunicator().sendNormalServerMessage("Please select an upgrade first.");
            }
            // Refresh the window
            reopenWindow();
        }
    }

    /**
     * Create a new question window (required to avoid "old question" timeout).
     */
    private void reopenWindow() {
        UpgradeTreeQuestion newQuestion = new UpgradeTreeQuestion(player);
        newQuestion.currentPage = this.currentPage; // Preserve page state
        com.wurmonline.server.questions.Question question = com.garward.wurmmodloader.modsupport.questions.ModQuestions.createQuestion(
            player, "Upgrade Tree", "View and unlock permanent upgrades",
            player.getWurmId(), newQuestion);
        question.sendQuestion();
    }

    @Override
    public void sendQuestion(Question question) {
        UpgradeTreeManager manager = UpgradeTreeManager.getInstance();

        // Get player data
        Set<String> playerUpgrades = manager.getPlayerUpgrades(player.getWurmId());
        int unlockedCount = playerUpgrades.size();

        // Get all upgrades as list for pagination
        List<Upgrade> allUpgrades = new ArrayList<>(manager.getAllUpgrades());
        int totalUpgrades = allUpgrades.size();
        int totalPages = (int) Math.ceil((double) totalUpgrades / UPGRADES_PER_PAGE);

        // Calculate page bounds
        int startIdx = currentPage * UPGRADES_PER_PAGE;
        int endIdx = Math.min(startIdx + UPGRADES_PER_PAGE, totalUpgrades);
        List<Upgrade> pageUpgrades = allUpgrades.subList(startIdx, endIdx);

        // Build BML using exact vanilla pattern
        StringBuilder buf = new StringBuilder();
        buf.append(ModQuestions.getBmlHeader(question));

        // Header
        buf.append("text{type='bold';text='Upgrade Tree - Page " + (currentPage + 1) + " / " + totalPages + "'}");
        buf.append("text{text=''}");
        buf.append("text{text='Unlocked: " + unlockedCount + " / " + totalUpgrades + "'}");
        buf.append("text{text=''}");

        // List upgrades on current page with radio buttons
        for (Upgrade upgrade : pageUpgrades) {
            boolean unlocked = playerUpgrades.contains(upgrade.getId());
            boolean canUnlock = !unlocked && manager.meetsRequirements(player.getWurmId(), upgrade);

            // Status indicator
            String status = unlocked ? "[UNLOCKED]" : (canUnlock ? "[AVAILABLE]" : "[LOCKED]");

            // Radio button (only for available upgrades)
            if (canUnlock) {
                buf.append("radio{group='upgrade';id='" + upgrade.getId() + "';text='" + status + " " + upgrade.getName() + "'}");
            } else {
                buf.append("text{type='bold';text='" + status + " " + upgrade.getName() + "'}");
            }

            // Description and cost
            buf.append("text{text='  " + upgrade.getDescription() + "'}");
            buf.append("text{text='  Cost: " + upgrade.getCost() + " power | Tier: " + upgrade.getTier() + "'}");

            // Requirements
            if (upgrade.hasRequirements()) {
                StringBuilder reqText = new StringBuilder("  Requires: ");
                if (!upgrade.getRequirements().isEmpty()) {
                    reqText.append(String.join(", ", upgrade.getRequirements()));
                }
                if (upgrade.getMinUpgradesRequired() > 0) {
                    if (!upgrade.getRequirements().isEmpty()) {
                        reqText.append(" + ");
                    }
                    reqText.append(upgrade.getMinUpgradesRequired()).append(" total upgrades");
                }
                buf.append("text{text='" + reqText.toString() + "'}");
            }

            buf.append("text{text=''}");
        }

        // Action and navigation buttons
        buf.append("harray{");
        buf.append("button{text='UNLOCK SELECTED';id='unlock'}");
        if (currentPage > 0) {
            buf.append("button{text='<< Previous';id='prevpage'}");
        }
        if (currentPage < totalPages - 1) {
            buf.append("button{text='Next >>';id='nextpage'}");
        }
        buf.append("}");

        buf.append(ModQuestions.createAnswerButton2(question));

        player.getCommunicator().sendBml(500, 400, true, true, buf.toString(), 200, 200, 200, "Upgrade Tree");
    }
}
