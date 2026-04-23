package com.garward.wurmmodloader.api.events.eventlogic.area;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Handler contract for area actions that target ground tiles (e.g. cultivate,
 * sow, dig). Mods register one per tile type via {@code AreaActionRegistry}.
 */
public interface TileAreaHandler {
    boolean checkSkill(Creature performer, float needed);

    boolean canStartOn(Creature performer, Item source, int tilex, int tiley, boolean onSurface, int tile);

    boolean canActOn(Creature performer, Item source, int tilex, int tiley, boolean onSurface, int tile, boolean sendMsg);

    float getActionTime(Creature performer, Item source, int tilex, int tiley, boolean onSurface, int tile);

    boolean actionStarted(Creature performer, Item source, int tilex, int tiley, boolean onSurface, int tile);

    boolean actionCompleted(Creature performer, Item source, int tilex, int tiley, boolean onSurface, int tile, byte rarity);

    default ActionEntryOverride getOverride(Creature performer, Item source, int tilex, int tiley, boolean onSurface, int tile) {
        return null;
    }
}
