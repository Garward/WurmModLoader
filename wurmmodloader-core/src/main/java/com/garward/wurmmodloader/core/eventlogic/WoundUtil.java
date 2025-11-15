package com.garward.wurmmodloader.core.eventlogic;

import com.wurmonline.server.bodys.Body;
import com.wurmonline.server.bodys.Wound;
import com.wurmonline.server.bodys.Wounds;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Framework utility for wound management and healing.
 *
 * <p>This utility provides safe, framework-level access to Wurm's wound system
 * without requiring mods to import Wurm classes directly.</p>
 *
 * <p><strong>Thread Safety:</strong> Thread-safe. Wound operations are synchronized
 * by Wurm's internal wound management.</p>
 *
 * @since 1.0.0
 */
public final class WoundUtil {

    private static final Logger LOGGER = Logger.getLogger(WoundUtil.class.getName());

    private WoundUtil() {
        // Utility class
    }

    /**
     * Get creature's wounds.
     *
     * @param creature The creature
     * @return Wounds object, or null if unavailable
     */
    public static Wounds getWounds(Creature creature) {
        if (creature == null) {
            return null;
        }
        try {
            Body body = creature.getBody();
            if (body == null) {
                return null;
            }
            return body.getWounds();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get wounds for creature " + creature.getWurmId(), e);
            return null;
        }
    }

    /**
     * Get creature's wound array.
     *
     * @param creature The creature
     * @return Array of wounds (never null, but may be empty)
     */
    public static Wound[] getWoundArray(Creature creature) {
        Wounds wounds = getWounds(creature);
        if (wounds == null) {
            return new Wound[0];
        }
        try {
            Wound[] array = wounds.getWounds();
            return array != null ? array : new Wound[0];
        } catch (Exception e) {
            return new Wound[0];
        }
    }

    /**
     * Check if creature has any wounds.
     *
     * @param creature The creature
     * @return true if creature has wounds
     */
    public static boolean hasWounds(Creature creature) {
        Wounds wounds = getWounds(creature);
        if (wounds == null) {
            return false;
        }
        try {
            Wound[] array = wounds.getWounds();
            return array != null && array.length > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Send healing visual effect to creature's tile.
     *
     * <p>This shows the same green sparkle effect as Faith of the Old healing.</p>
     *
     * @param creature The creature to show effect on
     */
    public static void sendHealingEffect(Creature creature) {
        if (creature == null) {
            return;
        }
        try {
            int tileX = creature.getTileX();
            int tileY = creature.getTileY();
            boolean onSurface = creature.isOnSurface();

            VolaTile tile = Zones.getTileOrNull(tileX, tileY, onSurface);
            if (tile != null) {
                // Effect type 11 is the green healing sparkle
                tile.sendAttachCreatureEffect(creature, (byte) 11, (byte) 0, (byte) 0, (byte) 0, (byte) 0);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send healing effect for creature " + creature.getWurmId(), e);
        }
    }
}
