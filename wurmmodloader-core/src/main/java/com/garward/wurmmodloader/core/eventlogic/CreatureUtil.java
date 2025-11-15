package com.garward.wurmmodloader.core.eventlogic;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Framework utility for accessing creature stats and properties.
 *
 * <p>This utility provides safe, framework-level access to Wurm's Creature class
 * without requiring mods to import or interact with Wurm classes directly.</p>
 *
 * <p><strong>Thread Safety:</strong> Thread-safe. All methods are stateless and
 * access Wurm's concurrent-safe registries.</p>
 *
 * @since 1.0.0
 */
public final class CreatureUtil {

    private static final Logger LOGGER = Logger.getLogger(CreatureUtil.class.getName());

    // Skill ID for character level (Body Control)
    private static final int SKILL_BODY_CONTROL = 102;

    private CreatureUtil() {
        // Utility class
    }

    /**
     * Get creature's base combat rating.
     *
     * @param creature The creature
     * @return Combat rating, or 1.0 if unavailable
     */
    public static float getCombatRating(Creature creature) {
        if (creature == null) {
            return 1.0f;
        }
        try {
            return creature.getBaseCombatRating();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get combat rating for " + creature.getWurmId(), e);
            return 1.0f;
        }
    }

    /**
     * Check if creature is a champion.
     *
     * @param creature The creature
     * @return true if champion
     */
    public static boolean isChampion(Creature creature) {
        if (creature == null) {
            return false;
        }
        try {
            return creature.isChampion();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if creature is unique.
     *
     * @param creature The creature
     * @return true if unique
     */
    public static boolean isUnique(Creature creature) {
        if (creature == null) {
            return false;
        }
        try {
            return creature.isUnique();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get creature's template.
     *
     * @param creature The creature
     * @return Template, or null if unavailable
     */
    public static CreatureTemplate getTemplate(Creature creature) {
        if (creature == null) {
            return null;
        }
        try {
            return creature.getTemplate();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get character level from Body Control skill.
     *
     * <p>Character level is calculated as: skill knowledge / 10.0</p>
     *
     * @param creature The creature (player or NPC)
     * @return Character level (1-100), or 1 if unavailable
     */
    public static int getCharacterLevel(Creature creature) {
        if (creature == null) {
            return 1;
        }
        try {
            double knowledge = creature.getSkills()
                .getSkillOrLearn(SKILL_BODY_CONTROL)
                .getKnowledge();
            return Math.max(1, (int) (knowledge / 10.0));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get character level for " + creature.getWurmId(), e);
            return 1;
        }
    }

    /**
     * Get skill knowledge value.
     *
     * @param creature The creature
     * @param skillId Skill ID (e.g., 102 for Body Control)
     * @return Skill knowledge (0-100), or 0 if unavailable
     */
    public static double getSkillKnowledge(Creature creature, int skillId) {
        if (creature == null) {
            return 0;
        }
        try {
            return creature.getSkills()
                .getSkillOrLearn(skillId)
                .getKnowledge();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get creature's position X coordinate.
     *
     * @param creature The creature
     * @return Tile X coordinate
     */
    public static int getTileX(Creature creature) {
        if (creature == null) {
            return 0;
        }
        try {
            return creature.getTileX();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get creature's position Y coordinate.
     *
     * @param creature The creature
     * @return Tile Y coordinate
     */
    public static int getTileY(Creature creature) {
        if (creature == null) {
            return 0;
        }
        try {
            return creature.getTileY();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Check if creature is on surface.
     *
     * @param creature The creature
     * @return true if on surface, false if in cave
     */
    public static boolean isOnSurface(Creature creature) {
        if (creature == null) {
            return true;
        }
        try {
            return creature.isOnSurface();
        } catch (Exception e) {
            return true;
        }
    }
}
