package com.garward.wurmmodloader.config;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Validator for ServerConfig instances.
 *
 * <p>Performs type checking, range validation, and logical dependency validation.
 * Validation errors are fatal (throw ConfigException), while warnings are logged.</p>
 *
 * <p><strong>Classloader Isolation:</strong> This class has NO dependencies on Wurm classes.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class ConfigValidator {

    private static final Logger logger = Logger.getLogger(ConfigValidator.class.getName());

    /**
     * Validate a ServerConfig object.
     *
     * @param config The config to validate
     * @throws ConfigException if validation fails
     */
    public static void validate(ServerConfig config) throws ConfigException {
        logger.info("[ConfigValidator] Validating server configuration...");

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Version validation
        validateVersion(config, errors);

        // Server identity validation
        validateServerIdentity(config.server, errors, warnings);

        // Skills validation
        validateSkills(config.skills, errors, warnings);

        // Combat validation
        validateCombat(config.combat, errors, warnings);

        // Creatures validation
        validateCreatures(config.creatures, errors, warnings);

        // World validation
        validateWorld(config.world, errors, warnings);

        // Economy validation
        validateEconomy(config.economy, errors, warnings);

        // Players validation
        validatePlayers(config.players, errors, warnings);

        // Log warnings
        for (String warning : warnings) {
            logger.warning("[ConfigValidator] WARNING: " + warning);
        }

        // Throw exception if any errors
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("Config validation failed:\n");
            for (String error : errors) {
                sb.append("  - ").append(error).append("\n");
            }
            throw new ConfigException(sb.toString());
        }

        logger.info("[ConfigValidator] Config validation passed (" + warnings.size() + " warnings)");
    }

    private static void validateVersion(ServerConfig config, List<String> errors) {
        if (config.version < 1) {
            errors.add("version must be >= 1 (got: " + config.version + ")");
        }
        if (config.version > 1) {
            logger.warning("[ConfigValidator] Config version " + config.version + " is newer than supported (1). May have compatibility issues.");
        }
    }

    private static void validateServerIdentity(ServerConfig.ServerIdentityConfig server,
                                                List<String> errors,
                                                List<String> warnings) {
        if (server.name == null || server.name.trim().isEmpty()) {
            errors.add("server.name cannot be empty");
        }
        if (server.name != null && server.name.length() > 255) {
            errors.add("server.name too long (max 255 characters)");
        }
        if (server.motd != null && server.motd.length() > 1000) {
            errors.add("server.motd too long (max 1000 characters)");
        }
        if (server.kingdom < 0 || server.kingdom > 4) {
            errors.add("server.kingdom must be 0-4 (got: " + server.kingdom + ")");
        }
    }

    private static void validateSkills(ServerConfig.SkillsConfig skills,
                                        List<String> errors,
                                        List<String> warnings) {
        // Skill gain rate
        if (skills.gainRate <= 0.0f) {
            errors.add("skills.gainRate must be > 0 (got: " + skills.gainRate + ")");
        }
        if (skills.gainRate > 1000.0f) {
            errors.add("skills.gainRate exceeds maximum 1000 (got: " + skills.gainRate + ")");
        }
        if (skills.gainRate > 100.0f) {
            warnings.add("skills.gainRate is very high (" + skills.gainRate + "x), server may feel too easy");
        }

        // Starting skills
        ServerConfig.SkillsConfig.StartingSkillsConfig starting = skills.starting;

        if (starting.basic < 1.0f || starting.basic > 100.0f) {
            errors.add("skills.starting.basic must be 1-100 (got: " + starting.basic + ")");
        }
        if (starting.mindLogic < 1.0f || starting.mindLogic > 100.0f) {
            errors.add("skills.starting.mindLogic must be 1-100 (got: " + starting.mindLogic + ")");
        }
        if (starting.fighting < 1.0f || starting.fighting > 100.0f) {
            errors.add("skills.starting.fighting must be 1-100 (got: " + starting.fighting + ")");
        }
        if (starting.bodyControl < 1.0f || starting.bodyControl > 100.0f) {
            errors.add("skills.starting.bodyControl must be 1-100 (got: " + starting.bodyControl + ")");
        }
        if (starting.overall < 1.0f || starting.overall > 100.0f) {
            errors.add("skills.starting.overall must be 1-100 (got: " + starting.overall + ")");
        }

        if (starting.fighting > 50.0f) {
            warnings.add("skills.starting.fighting is very high (" + starting.fighting + "), new players may be overpowered");
        }
    }

    private static void validateCombat(ServerConfig.CombatConfig combat,
                                        List<String> errors,
                                        List<String> warnings) {
        if (combat.actionSpeed <= 0.0f) {
            errors.add("combat.actionSpeed must be > 0 (got: " + combat.actionSpeed + ")");
        }
        if (combat.actionSpeed > 10.0f) {
            errors.add("combat.actionSpeed exceeds maximum 10 (got: " + combat.actionSpeed + ")");
        }
        if (combat.actionSpeed < 0.1f) {
            warnings.add("combat.actionSpeed is very low (" + combat.actionSpeed + "), actions will be very slow");
        }

        if (combat.ratingModifier <= 0.0f) {
            errors.add("combat.ratingModifier must be > 0 (got: " + combat.ratingModifier + ")");
        }
        if (combat.ratingModifier > 10.0f) {
            errors.add("combat.ratingModifier exceeds maximum 10 (got: " + combat.ratingModifier + ")");
        }

        if (combat.hotaDelay < 1) {
            errors.add("combat.hotaDelay must be >= 1 (got: " + combat.hotaDelay + ")");
        }
    }

    private static void validateCreatures(ServerConfig.CreaturesConfig creatures,
                                           List<String> errors,
                                           List<String> warnings) {
        if (creatures.maxTotal < 10) {
            errors.add("creatures.maxTotal must be >= 10 (got: " + creatures.maxTotal + ")");
        }
        if (creatures.maxTotal > 100000) {
            errors.add("creatures.maxTotal exceeds maximum 100000 (got: " + creatures.maxTotal + ")");
        }
        if (creatures.maxTotal < 100) {
            warnings.add("creatures.maxTotal is very low (" + creatures.maxTotal + "), server may feel empty");
        }

        if (creatures.percentAggressive < 0.0f || creatures.percentAggressive > 100.0f) {
            errors.add("creatures.percentAggressive must be 0-100 (got: " + creatures.percentAggressive + ")");
        }
        if (creatures.percentAggressive > 50.0f) {
            warnings.add("creatures.percentAggressive is very high (" + creatures.percentAggressive + "%), server may be very dangerous");
        }

        if (creatures.breedingTimer < 0) {
            errors.add("creatures.breedingTimer must be >= 0 (got: " + creatures.breedingTimer + ")");
        }
    }

    private static void validateWorld(ServerConfig.WorldConfig world,
                                       List<String> errors,
                                       List<String> warnings) {
        if (world.treeGrowth < 1) {
            errors.add("world.treeGrowth must be >= 1 (got: " + world.treeGrowth + ")");
        }
        if (world.treeGrowth > 1000) {
            errors.add("world.treeGrowth exceeds maximum 1000 (got: " + world.treeGrowth + ")");
        }

        if (world.fieldGrowthTime < 1000) {
            errors.add("world.fieldGrowthTime must be >= 1000ms (got: " + world.fieldGrowthTime + ")");
        }
        if (world.fieldGrowthTime < 60000) {
            warnings.add("world.fieldGrowthTime is very low (" + (world.fieldGrowthTime / 1000) + "s), crops will grow very fast");
        }

        if (world.tunnelingHits < 1) {
            errors.add("world.tunnelingHits must be >= 1 (got: " + world.tunnelingHits + ")");
        }
        if (world.tunnelingHits > 1000) {
            errors.add("world.tunnelingHits exceeds maximum 1000 (got: " + world.tunnelingHits + ")");
        }
    }

    private static void validateEconomy(ServerConfig.EconomyConfig economy,
                                         List<String> errors,
                                         List<String> warnings) {
        if (economy.maxDeedSize < 0) {
            errors.add("economy.maxDeedSize must be >= 0 (got: " + economy.maxDeedSize + ")");
        }
        if (economy.maxDeedSize > 200) {
            warnings.add("economy.maxDeedSize is very large (" + economy.maxDeedSize + "x" + economy.maxDeedSize + " tiles)");
        }

        if (economy.traders.maxMoney < 0) {
            errors.add("economy.traders.maxMoney must be >= 0 (got: " + economy.traders.maxMoney + ")");
        }
        if (economy.traders.startingMoney < 0) {
            errors.add("economy.traders.startingMoney must be >= 0 (got: " + economy.traders.startingMoney + ")");
        }
        if (economy.traders.startingMoney > economy.traders.maxMoney) {
            errors.add("economy.traders.startingMoney cannot exceed maxMoney");
        }

        if (economy.kingdomStartingMoney < 0) {
            errors.add("economy.kingdomStartingMoney must be >= 0 (got: " + economy.kingdomStartingMoney + ")");
        }

        // Logical warnings
        if (economy.freeDeeds && economy.upkeepEnabled) {
            warnings.add("economy.freeDeeds=true and upkeepEnabled=true may cause confusion (deeds are free but have upkeep)");
        }
    }

    private static void validatePlayers(ServerConfig.PlayersConfig players,
                                         List<String> errors,
                                         List<String> warnings) {
        if (players.maxPlayers < 1) {
            errors.add("players.maxPlayers must be >= 1 (got: " + players.maxPlayers + ")");
        }
        if (players.maxPlayers > 10000) {
            errors.add("players.maxPlayers exceeds maximum 10000 (got: " + players.maxPlayers + ")");
        }
    }
}
