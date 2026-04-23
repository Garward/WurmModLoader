package com.garward.wurmmodloader.mods.gmtools;

import com.garward.wurmmodloader.modsupport.actions.ActionPerformer;
import com.garward.wurmmodloader.modsupport.actions.BehaviourProvider;
import com.garward.wurmmodloader.modsupport.actions.ModAction;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.structures.Floor;
import com.wurmonline.shared.constants.StructureConstants;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Emits the nested "GM: plan floor" menu on an unfinished floor/roof/staircase
 * placeholder. Groups variants by floor-type bucket (Floor / Roof / Opening /
 * Staircase) so the ~60 variants fit as a handful of submenus.
 */
public class FloorMenuCoordinator implements ModAction {

    private final List<PlanAndFinishFloorAction> variants;

    public FloorMenuCoordinator(List<PlanAndFinishFloorAction> variants) {
        this.variants = variants;
    }

    @Override
    public BehaviourProvider getBehaviourProvider() {
        return new BehaviourProvider() {
            @Override
            public List<ActionEntry> getBehavioursFor(Creature performer, boolean onSurface, Floor target) {
                return build(performer, target);
            }
            @Override
            public List<ActionEntry> getBehavioursFor(Creature performer, Item source, boolean onSurface, Floor target) {
                return build(performer, target);
            }
        };
    }

    @Override public ActionPerformer getActionPerformer() { return null; }

    private List<ActionEntry> build(Creature performer, Floor target) {
        if (performer == null || performer.getPower() < GmToolsMod.minPower) return null;
        if (target == null || variants.isEmpty()) return null;

        Map<String, List<PlanAndFinishFloorAction>> byBucket = new LinkedHashMap<>();
        byBucket.put("Floors", new ArrayList<>());
        byBucket.put("Roofs", new ArrayList<>());
        byBucket.put("Openings", new ArrayList<>());
        byBucket.put("Staircases", new ArrayList<>());

        for (PlanAndFinishFloorAction v : variants) {
            byBucket.get(bucketFor(v.getFloorType().getType())).add(v);
        }

        List<ActionEntry> flat = new ArrayList<>();
        int topChildren = 0;
        for (List<PlanAndFinishFloorAction> g : byBucket.values()) if (!g.isEmpty()) topChildren++;
        flat.add(new ActionEntry((short) -topChildren, "GM: plan floor", ""));
        for (Map.Entry<String, List<PlanAndFinishFloorAction>> e : byBucket.entrySet()) {
            List<PlanAndFinishFloorAction> group = e.getValue();
            if (group.isEmpty()) continue;
            flat.add(new ActionEntry((short) -group.size(), e.getKey(), ""));
            for (PlanAndFinishFloorAction v : group) {
                flat.add(v.getActionEntry());
            }
        }
        return flat;
    }

    private static String bucketFor(StructureConstants.FloorType t) {
        if (t == null) return "Floors";
        if (t.isStair()) return "Staircases";
        switch (t) {
            case ROOF:    return "Roofs";
            case OPENING: return "Openings";
            default:      return "Floors";
        }
    }

}
