package com.garward.wurmmodloader.mods.gmtools;

import com.garward.wurmmodloader.modsupport.actions.ActionPerformer;
import com.garward.wurmmodloader.modsupport.actions.BehaviourProvider;
import com.garward.wurmmodloader.modsupport.actions.ModAction;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.structures.Structures;
import com.wurmonline.server.structures.Wall;
import com.wurmonline.shared.constants.StructureMaterialEnum;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Emits the nested "GM: plan wall" menu on a NO_WALL / plan-wall placeholder
 * (the Wall entity that vanilla's tile-border "Plan > wall" step creates).
 * Mirrors the vanilla mallet+wall-plan crafting step — we let vanilla own the
 * placeholder creation and hook only the finish-as-type mutation.
 *
 * <p>Groups every {@link PlanAndFinishWallAction} by {@link StructureMaterialEnum}
 * so ~100 variants fit as ~10 submenus of ~10 entries. Wurm's nested menu
 * convention: a header {@link ActionEntry} with a negative action id whose
 * magnitude equals its immediate child count; nested headers count as one.
 */
public class WallMenuCoordinator implements ModAction {

    private final List<PlanAndFinishWallAction> variants;

    public WallMenuCoordinator(List<PlanAndFinishWallAction> variants) {
        this.variants = variants;
    }

    @Override
    public BehaviourProvider getBehaviourProvider() {
        return new BehaviourProvider() {
            @Override
            public List<ActionEntry> getBehavioursFor(Creature performer, Wall target) {
                return build(performer, target);
            }
            @Override
            public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Wall target) {
                return build(performer, target);
            }
        };
    }

    @Override public ActionPerformer getActionPerformer() { return null; }

    private List<ActionEntry> build(Creature performer, Wall target) {
        if (performer == null || performer.getPower() < GmToolsMod.minPower) return null;
        if (target == null || variants.isEmpty()) return null;
        Structure structure = Structures.getStructureOrNull(target.getStructureId());
        if (structure == null || !structure.isFinalized()) return null;

        Map<StructureMaterialEnum, List<PlanAndFinishWallAction>> byMat = new LinkedHashMap<>();
        for (PlanAndFinishWallAction v : variants) {
            byMat.computeIfAbsent(v.getWallType().getMaterial(), k -> new ArrayList<>()).add(v);
        }

        List<ActionEntry> flat = new ArrayList<>();
        flat.add(new ActionEntry((short) -byMat.size(), "GM: plan wall", ""));
        for (Map.Entry<StructureMaterialEnum, List<PlanAndFinishWallAction>> e : byMat.entrySet()) {
            List<PlanAndFinishWallAction> group = e.getValue();
            String label = capitalize(e.getKey().nameString);
            flat.add(new ActionEntry((short) -group.size(), label, ""));
            for (PlanAndFinishWallAction v : group) {
                flat.add(v.getActionEntry());
            }
        }
        return flat;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
