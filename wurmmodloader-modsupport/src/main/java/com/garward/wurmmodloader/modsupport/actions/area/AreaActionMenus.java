package com.garward.wurmmodloader.modsupport.actions.area;

import com.garward.wurmmodloader.api.events.eventlogic.area.ActionEntryOverride;
import com.garward.wurmmodloader.api.events.eventlogic.area.AreaActionType;
import com.garward.wurmmodloader.api.events.eventlogic.area.ItemAreaHandler;
import com.garward.wurmmodloader.api.events.eventlogic.area.TileAreaHandler;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Menu-building helpers to inject area-action entries into tile/item menus.
 * Call from mod {@code @SubscribeEvent} handlers on
 * {@code TileMenuBuildEvent} / {@code ItemMenuBuildEvent}.
 */
public final class AreaActionMenus {

    private AreaActionMenus() {}

    /**
     * Mutates {@code actions} in place with area-action entries applicable to
     * the given tile.
     *
     * @param performers lookup from action type to its sorted list of performer
     *                   tiers (usually {@code AreaActionRegistrar}-owned).
     */
    public static void mutateTileMenu(List<ActionEntry> actions, Creature performer, long target,
                                      boolean onSurface, Item source,
                                      Map<AreaActionType, List<AreaActionPerformer>> performers) {
        int x = Tiles.decodeTileX(target);
        int y = Tiles.decodeTileY(target);
        int tileData = (onSurface ? Server.surfaceMesh : Server.caveMesh).getTile(x, y);
        byte tileType = Tiles.decodeType(tileData);

        Map<AreaActionType, TileAreaHandler> handlers = AreaActionRegistry.getTileHandlers(tileType);
        if (handlers == null) return;

        handlers.forEach((type, act) -> {
            if (!act.canStartOn(performer, source, x, y, onSurface, tileData)) return;
            List<AreaActionPerformer> tiers = performers.get(type);
            if (tiers == null || tiers.isEmpty()) return;

            ActionEntryOverride override = act.getOverride(performer, source, x, y, onSurface, tileData);
            short base = override == null ? type.baseAction : override.number;
            String name = override == null ? type.name : override.name;
            String goesUnder = override == null ? type.goesUnder : override.goesUnder;

            addOrReplace(actions, base, name,
                    tiers.stream().filter(a -> act.checkSkill(performer, a.skillLevel)).collect(Collectors.toList()),
                    "Tile", goesUnder);
        });
    }

    public static void mutateItemMenu(List<ActionEntry> actions, Creature performer, long targetId, Item source,
                                      Map<AreaActionType, List<AreaActionPerformer>> performers) {
        try {
            Item target = Items.getItem(targetId);
            Map<AreaActionType, ItemAreaHandler> handlers = AreaActionRegistry.getItemHandlers(target);
            if (handlers == null) return;

            handlers.forEach((type, act) -> {
                if (!act.canStartOn(performer, source, target)) return;
                List<AreaActionPerformer> tiers = performers.get(type);
                if (tiers == null || tiers.isEmpty()) return;

                ActionEntryOverride override = act.getOverride(performer, source, target);
                short base = override == null ? type.baseAction : override.number;
                String name = override == null ? type.name : override.name;
                String goesUnder = override == null ? type.goesUnder : override.goesUnder;

                addOrReplace(actions, base, name,
                        tiers.stream().filter(a -> act.checkSkill(performer, a.skillLevel)).collect(Collectors.toList()),
                        "Single", goesUnder);
            });
        } catch (NoSuchItemException e) {
            // Target vanished between dispatch and here — nothing to add.
        }
    }

    /**
     * Inserts tiered area-action entries into {@code list}, either expanding
     * the existing base action into (Single, 3x3, 5x5, ...) form or appending
     * a new submenu under {@code goesUnder}.
     */
    public static void addOrReplace(List<ActionEntry> list, int actionNumber, String name,
                                    List<AreaActionPerformer> available, String singleName, String goesUnder) {
        if (available.isEmpty()) return;

        if (actionNumber > 0) {
            int p = 0;
            for (ActionEntry actionEntry : list) {
                if (actionEntry.getNumber() == actionNumber) {
                    ActionEntry old = list.remove(p);
                    list.add(p++, new ActionEntry((short) (-1 - available.size()), old.getActionString(), ""));
                    list.add(p++, new ActionEntry(old.getNumber(), singleName, old.getVerbString()));
                    for (AreaActionPerformer act : available) {
                        list.add(p++, new ActionEntry(act.actionEntry.getNumber(),
                                String.format("%dx%d Area", act.radius * 2 + 1, act.radius * 2 + 1),
                                act.actionEntry.getVerbString()));
                    }
                    return;
                }
                p++;
            }
        }

        if (goesUnder != null) {
            int p = 0;
            for (ActionEntry actionEntry : list) {
                if (actionEntry.getActionString().equals(goesUnder) && actionEntry.getNumber() < 0) {
                    ActionEntry old = list.remove(p);
                    list.add(p++, new ActionEntry((short) (old.getNumber() - 1), old.getActionString(), old.getVerbString()));
                    for (int toSkip = -old.getNumber(); toSkip > 0; toSkip--) {
                        if (list.get(p).getNumber() < 0) toSkip += -list.get(p).getNumber();
                        p++;
                    }
                    if (available.size() > 1) {
                        list.add(p++, new ActionEntry((short) -available.size(), name, ""));
                        for (AreaActionPerformer act : available) {
                            list.add(p++, new ActionEntry(act.actionEntry.getNumber(),
                                    String.format("%dx%d Area", act.radius * 2 + 1, act.radius * 2 + 1),
                                    act.actionEntry.getVerbString()));
                        }
                    } else {
                        AreaActionPerformer act = available.get(0);
                        list.add(p, new ActionEntry(act.actionEntry.getNumber(),
                                String.format("%s (%dx%x)", name, act.radius * 2 + 1, act.radius * 2 + 1), ""));
                    }
                    return;
                }
                p++;
            }
            list.add(new ActionEntry((short) -1, goesUnder, ""));
            if (available.size() > 1) {
                list.add(new ActionEntry((short) -available.size(), name, ""));
                for (AreaActionPerformer act : available) {
                    list.add(new ActionEntry(act.actionEntry.getNumber(),
                            String.format("%dx%d Area", act.radius * 2 + 1, act.radius * 2 + 1),
                            act.actionEntry.getVerbString()));
                }
            } else {
                list.add(available.get(0).actionEntry);
            }
        } else {
            if (available.size() > 1) {
                list.add(new ActionEntry((short) -available.size(), name, ""));
                for (AreaActionPerformer act : available) {
                    list.add(new ActionEntry(act.actionEntry.getNumber(),
                            String.format("%dx%d Area", act.radius * 2 + 1, act.radius * 2 + 1),
                            act.actionEntry.getVerbString()));
                }
            } else {
                AreaActionPerformer act = available.get(0);
                list.add(new ActionEntry(act.actionEntry.getNumber(),
                        String.format("%s (%dx%x)", name, act.radius * 2 + 1, act.radius * 2 + 1), ""));
            }
        }
    }
}
