package com.garward.wurmmodloader.api.events.eventlogic.area;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Handler contract for area actions that target items on tiles (e.g. pick,
 * prune, harvest trees/trellises). Mods register one per template id via
 * {@code AreaActionRegistry}.
 */
public interface ItemAreaHandler {
    boolean checkSkill(Creature performer, float needed);

    boolean canStartOn(Creature performer, Item source, Item target);

    boolean canActOn(Creature performer, Item source, Item target, boolean sendMsg);

    float getActionTime(Creature performer, Item source, Item target);

    boolean actionStarted(Creature performer, Item source, Item target);

    boolean actionCompleted(Creature performer, Item source, Item target, byte rarity);

    default ActionEntryOverride getOverride(Creature performer, Item source, Item target) {
        return null;
    }
}
