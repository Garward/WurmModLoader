package com.garward.wurmmodloader.modsupport.actions.area;

import com.garward.wurmmodloader.api.events.eventlogic.area.ActionEntryOverride;
import com.garward.wurmmodloader.api.events.eventlogic.area.AreaActionType;
import com.garward.wurmmodloader.api.events.eventlogic.area.ItemAreaHandler;
import com.garward.wurmmodloader.api.events.eventlogic.area.TileAreaHandler;
import com.garward.wurmmodloader.modsupport.actions.ActionEntryBuilder;
import com.garward.wurmmodloader.modsupport.actions.ActionPerformer;
import com.garward.wurmmodloader.modsupport.actions.ActionPropagation;
import com.garward.wurmmodloader.modsupport.actions.ModActions;
import com.wurmonline.mesh.MeshIO;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.Constants;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * Generic radius-based action performer. One instance per (action type, radius)
 * tier. Looks up per-tile / per-item handlers via {@link AreaActionRegistry}
 * when invoked; mods only supply the handlers and the radius/skill table.
 */
public class AreaActionPerformer implements ActionPerformer {

    public final int radius;
    public final float skillLevel;
    public final ActionEntry actionEntry;
    private final AreaActionType type;
    private final WeakHashMap<Action, ItemRun> itemRuns = new WeakHashMap<>();
    private final WeakHashMap<Action, TileRun> tileRuns = new WeakHashMap<>();

    public AreaActionPerformer(AreaActionType type, float skillLevel, int radius) {
        this(type, skillLevel, radius, null);
    }

    /**
     * @param onRegister optional hook run after this performer is added to
     *                   {@link ModActions}; used e.g. to opt the generated
     *                   action id into a mounted-action allowlist.
     */
    public AreaActionPerformer(AreaActionType type, float skillLevel, int radius, Consumer<AreaActionPerformer> onRegister) {
        actionEntry = new ActionEntryBuilder(
                (short) ModActions.getNextActionId(),
                String.format("%s (%dx%d)", type.name, 2 * radius + 1, 2 * radius + 1),
                type.verb,
                new int[]{1, 4, 48, 35}
        ).range(4).build();

        this.radius = radius;
        this.skillLevel = skillLevel;
        this.type = type;

        ModActions.registerAction(actionEntry);
        ModActions.registerActionPerformer(this);

        if (onRegister != null) onRegister.accept(this);
    }

    // === Items ===

    @Override
    public boolean action(Action action, Creature performer, Item target, short num, float counter) {
        return action(action, performer, null, target, num, counter);
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Item target, short num, float counter) {
        ItemRun data = itemRuns.get(action);

        if (data == null) {
            Map<AreaActionType, ItemAreaHandler> actions = AreaActionRegistry.getItemHandlers(target);
            if (actions == null)
                return propagate(action, ActionPropagation.SERVER_PROPAGATION, ActionPropagation.ACTION_PERFORMER_PROPAGATION);

            ItemAreaHandler handler = actions.get(type);
            if (handler == null)
                return propagate(action, ActionPropagation.SERVER_PROPAGATION, ActionPropagation.ACTION_PERFORMER_PROPAGATION);

            if (!handler.checkSkill(performer, skillLevel) || !handler.canStartOn(performer, source, target))
                return finish(action);

            ActionEntryOverride override = handler.getOverride(performer, source, target);
            String verb = override == null ? type.verb : override.verb;

            LinkedList<ItemEntry> items = new LinkedList<>();
            int tilex = target.getTileX();
            int tiley = target.getTileY();
            float totalTime = 0;

            for (int x = tilex - radius; x <= tilex + radius; x++) {
                for (int y = tiley - radius; y <= tiley + radius; y++) {
                    VolaTile tile = Zones.getTileOrNull(x, y, target.isOnSurface());
                    if (tile == null) continue;
                    for (Item item : tile.getItems()) {
                        if (handler.canActOn(performer, source, item, true)) {
                            float time = handler.getActionTime(performer, source, item);
                            items.add(new ItemEntry(item, time));
                            totalTime += time;
                        }
                    }
                }
            }

            if (items.isEmpty()) {
                performer.getCommunicator().sendNormalServerMessage(
                        String.format("You stop %s as you can't find any valid targets", verb));
                return finish(action);
            }

            action.setTimeLeft((int) totalTime);
            performer.sendActionControl(verb, true, (int) totalTime);
            action.setNextTick(1);

            itemRuns.put(action, data = new ItemRun(handler, items.iterator()));
        }

        if (counter >= action.getNextTick()) {
            if (data.current != null) {
                if (!data.handler.actionCompleted(performer, source, data.current.item, action.getRarity())) {
                    itemRuns.remove(action);
                    return finish(action);
                }
                if (data.targets.hasNext()) action.setRarity(performer.getRarity());
            }
            if (!data.targets.hasNext()) {
                itemRuns.remove(action);
                return finish(action);
            }
            ItemEntry next = data.targets.next();
            if (data.handler.canActOn(performer, source, next.item, true)) {
                if (data.handler.actionStarted(performer, source, next.item)) {
                    data.current = next;
                    action.incNextTick(next.time / 10f);
                } else {
                    itemRuns.remove(action);
                    return finish(action);
                }
            } else {
                data.current = null;
            }
        }

        return propagate(action,
                ActionPropagation.CONTINUE_ACTION,
                ActionPropagation.NO_SERVER_PROPAGATION,
                ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);
    }

    // === Tiles ===

    @Override
    public boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface,
                          int heightOffset, int tile, short num, float counter) {
        return action(action, performer, null, tilex, tiley, onSurface, heightOffset, tile, num, counter);
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface,
                          int heightOffset, int tile, short num, float counter) {
        if ((tilex - radius < 0) || (tilex + radius > 1 << Constants.meshSize)
                || (tiley - radius < 0) || (tiley + radius > 1 << Constants.meshSize)) {
            return finish(action);
        }

        TileRun data = tileRuns.get(action);

        if (data == null) {
            Map<AreaActionType, TileAreaHandler> actions = AreaActionRegistry.getTileHandlers(Tiles.decodeType(tile));
            if (actions == null)
                return propagate(action, ActionPropagation.SERVER_PROPAGATION, ActionPropagation.ACTION_PERFORMER_PROPAGATION);

            TileAreaHandler handler = actions.get(type);
            if (handler == null)
                return propagate(action, ActionPropagation.SERVER_PROPAGATION, ActionPropagation.ACTION_PERFORMER_PROPAGATION);

            ActionEntryOverride override = handler.getOverride(performer, source, tilex, tiley, onSurface, tile);
            String verb = override == null ? type.verb : override.verb;

            if (!handler.checkSkill(performer, skillLevel)
                    || !handler.canStartOn(performer, source, tilex, tiley, onSurface, tile))
                return finish(action);

            MeshIO mesh = (onSurface ? Server.surfaceMesh : Server.caveMesh);
            LinkedList<TileEntry> items = new LinkedList<>();
            float totalTime = 0;

            for (int x = tilex - radius; x <= tilex + radius; x++) {
                for (int y = tiley - radius; y <= tiley + radius; y++) {
                    int t = mesh.getTile(x, y);
                    if (handler.canActOn(performer, source, x, y, onSurface, t, true)) {
                        float time = handler.getActionTime(performer, source, x, y, onSurface, t);
                        items.add(new TileEntry(x, y, time));
                        totalTime += time;
                    }
                }
            }

            if (totalTime <= 0) {
                performer.getCommunicator().sendNormalServerMessage(
                        String.format("You stop %s as you can't find any valid targets", verb));
                return finish(action);
            }

            action.setTimeLeft((int) totalTime);
            performer.sendActionControl(verb, true, (int) totalTime);
            action.setNextTick(1);

            tileRuns.put(action, data = new TileRun(handler, mesh, items.iterator()));
        }

        if (counter >= action.getNextTick()) {
            if (data.current != null) {
                int t = data.mesh.getTile(data.current.x, data.current.y);
                if (!data.handler.actionCompleted(performer, source, data.current.x, data.current.y, onSurface, t, action.getRarity())) {
                    tileRuns.remove(action);
                    return finish(action);
                }
                if (data.targets.hasNext()) action.setRarity(performer.getRarity());
            }
            if (!data.targets.hasNext()) {
                tileRuns.remove(action);
                return finish(action);
            }
            TileEntry next = data.targets.next();
            int t = data.mesh.getTile(next.x, next.y);
            if (data.handler.canActOn(performer, source, next.x, next.y, onSurface, t, true)) {
                if (data.handler.actionStarted(performer, source, next.x, next.y, onSurface, t)) {
                    data.current = next;
                    action.incNextTick(next.time / 10f);
                } else {
                    tileRuns.remove(action);
                    return finish(action);
                }
            } else {
                data.current = null;
            }
        }

        return propagate(action,
                ActionPropagation.CONTINUE_ACTION,
                ActionPropagation.NO_SERVER_PROPAGATION,
                ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);
    }

    @Override
    public short getActionId() {
        return actionEntry.getNumber();
    }

    private boolean finish(Action action) {
        return propagate(action,
                ActionPropagation.FINISH_ACTION,
                ActionPropagation.NO_SERVER_PROPAGATION,
                ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);
    }

    // ---- per-run state ----

    private static final class ItemEntry {
        final Item item;
        final float time;
        ItemEntry(Item item, float time) { this.item = item; this.time = time; }
    }

    private static final class ItemRun {
        final ItemAreaHandler handler;
        final Iterator<ItemEntry> targets;
        ItemEntry current;
        ItemRun(ItemAreaHandler h, Iterator<ItemEntry> t) { handler = h; targets = t; }
    }

    private static final class TileEntry {
        final int x, y;
        final float time;
        TileEntry(int x, int y, float time) { this.x = x; this.y = y; this.time = time; }
    }

    private static final class TileRun {
        final TileAreaHandler handler;
        final MeshIO mesh;
        final Iterator<TileEntry> targets;
        TileEntry current;
        TileRun(TileAreaHandler h, MeshIO m, Iterator<TileEntry> t) { handler = h; mesh = m; targets = t; }
    }
}
