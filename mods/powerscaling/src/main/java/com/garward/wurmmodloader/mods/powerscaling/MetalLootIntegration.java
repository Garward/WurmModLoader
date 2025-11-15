package com.garward.wurmmodloader.mods.powerscaling;

import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.players.Player;
import com.garward.wurmmodloader.modsupport.loot.LootManager;
import com.garward.wurmmodloader.modsupport.loot.LootRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Metal loot integration with bdew LootManager.
 *
 * <p>Generates power-scaled metal bar drops from creatures to eliminate
 * the mining grind and encourage combat-focused gameplay.</p>
 *
 * <p><strong>Quality Scaling:</strong></p>
 * <ul>
 *   <li>Power 1-10: 30-40 QL (early game)</li>
 *   <li>Power 11-25: 40-50 QL (mid-early)</li>
 *   <li>Power 26-50: 50-70 QL (mid game)</li>
 *   <li>Power 51-75: 70-85 QL (late game)</li>
 *   <li>Power 76-100: 85-95 QL (endgame)</li>
 *   <li>Power 100+: 95-99 QL (legendary)</li>
 * </ul>
 *
 * <p><strong>Metal Types:</strong></p>
 * <ul>
 *   <li>Regular: Iron (50%), Copper (30%), Tin (15%), Zinc (5%)</li>
 *   <li>Champions: Steel (40%), Iron (40%), Silver (15%), Gold (5%)</li>
 *   <li>Uniques: Adamantine (30%), Steel (30%), Gold (20%), Silver (20%)</li>
 * </ul>
 *
 * @author Power Fantasy RPG Team
 * @version 1.0.0
 */
public class MetalLootIntegration {
    private static final Logger logger = Logger.getLogger(MetalLootIntegration.class.getName());

    /**
     * Register metal loot rules with bdew LootManager.
     *
     * <p>Creates loot rules that add metal bars to all non-player creature kills.
     * Quality and type scale with creature power level.</p>
     */
    public static void registerLootRules() {
        // Metal drops for all non-player creatures
        LootRule rule = LootRule.create()
            .requireCreature(c -> !c.isPlayer())  // Any non-player creature
            .addDrop((creature, killer) -> generateMetalLoot(creature, killer));

        LootManager.add(rule);

        logger.info("Registered power-scaled metal loot rules");
    }

    /**
     * Generate metal loot for a killed creature.
     *
     * @param creature The killed creature
     * @param killer The player who killed the creature
     * @return Collection of metal lump items
     */
    private static Collection<Item> generateMetalLoot(Creature creature, Player killer) {
        Collection<Item> loot = new ArrayList<Item>();

        try {
            PowerScalingManager manager = PowerScalingManager.getInstance();
            PowerScalingConfig config = manager.getConfig();

            // Check if metal loot is enabled
            if (!config.isEnableMetalLoot()) {
                return loot;
            }

            int creaturePower = manager.getCreaturePowerLevel(creature);

            // Calculate quality based on power
            float baseQL = config.getMetalLootBaseQL() + (creaturePower * config.getMetalLootPowerMultiplier());
            float variance = (Server.rand.nextFloat() * config.getMetalLootQLVariance() * 2) - config.getMetalLootQLVariance(); // ±variance
            float finalQL = Math.min(99, Math.max(1, baseQL + variance));

            // Determine metal type and amount
            int metalTemplate = selectMetalType(creature);
            int amount = calculateAmount(creature, config);

            // Create bars
            for (int i = 0; i < amount; i++) {
                Item bar = ItemFactory.createItem(
                    metalTemplate,
                    finalQL,
                    null // material (unused for bars)
                );
                loot.add(bar);
            }

            // Debug log
            if (config.isDebugLogging()) {
                logger.info(String.format(
                    "Generated %d metal bar(s) (QL %.1f) from %s (power %d)",
                    amount, finalQL, creature.getName(), creaturePower
                ));
            }

        } catch (NoSuchTemplateException e) {
            logger.warning("Failed to create metal bar item: " + e.getMessage());
        } catch (Exception e) {
            logger.warning("Failed to generate metal loot: " + e.getMessage());
        }

        return loot;
    }

    /**
     * Select metal type based on creature type.
     *
     * @param creature The creature
     * @return Item template ID for the metal bar
     */
    private static int selectMetalType(Creature creature) {
        float roll = Server.rand.nextFloat();

        if (creature.isUnique()) {
            // Uniques drop exotic metals
            if (roll < 0.30f) return ItemList.adamantineBar;
            if (roll < 0.60f) return ItemList.steelBar;
            if (roll < 0.80f) return ItemList.goldBar;
            return ItemList.silverBar;

        } else if (creature.isChampion()) {
            // Champions drop rare metals
            if (roll < 0.40f) return ItemList.steelBar;
            if (roll < 0.80f) return ItemList.ironBar;
            if (roll < 0.95f) return ItemList.silverBar;
            return ItemList.goldBar;

        } else {
            // Regular mobs drop common metals
            if (roll < 0.50f) return ItemList.ironBar;
            if (roll < 0.80f) return ItemList.copperBar;
            if (roll < 0.95f) return ItemList.tinBar;
            return ItemList.zincBar;
        }
    }

    /**
     * Calculate drop amount based on creature type.
     *
     * @param creature The creature
     * @param config Configuration instance
     * @return Number of bars to drop
     */
    private static int calculateAmount(Creature creature, PowerScalingConfig config) {
        if (creature.isUnique()) {
            return 3 + Server.rand.nextInt(config.getUniqueBonusAmount() - 2); // 3-5 bars by default
        } else if (creature.isChampion()) {
            return 2 + Server.rand.nextInt(config.getChampionBonusAmount() - 1); // 2-3 bars by default
        } else {
            return 1; // 1 bar
        }
    }
}
