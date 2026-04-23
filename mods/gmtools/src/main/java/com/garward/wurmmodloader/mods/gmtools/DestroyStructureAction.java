package com.garward.wurmmodloader.mods.gmtools;

import com.garward.wurmmodloader.modsupport.actions.ActionPerformer;
import com.garward.wurmmodloader.modsupport.actions.BehaviourProvider;
import com.garward.wurmmodloader.modsupport.actions.ModAction;
import com.garward.wurmmodloader.modsupport.actions.ModActions;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.structures.Floor;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.structures.Structures;
import com.wurmonline.server.structures.Wall;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DestroyStructureAction implements ModAction {

    private static final Logger LOGGER = Logger.getLogger("GmTools.DestroyStructure");

    private final short actionId;
    private final ActionEntry actionEntry;

    public DestroyStructureAction() {
        this.actionId = (short) ModActions.getNextActionId();
        this.actionEntry = ActionEntry.createEntry(
                actionId,
                "GM: destroy structure",
                "destroying",
                new int[] { 6 /* ACTION_TYPE_NOMOVE */, 48 /* ACTION_TYPE_ENEMY_ALWAYS */ }
        );
        ModActions.registerAction(actionEntry);
    }

    @Override
    public BehaviourProvider getBehaviourProvider() {
        return new BehaviourProvider() {
            @Override
            public List<ActionEntry> getBehavioursFor(Creature performer, Wall target) {
                return gate(performer, target == null ? -1L : target.getStructureId());
            }
            @Override
            public List<ActionEntry> getBehavioursFor(Creature performer, Item source, Wall target) {
                return gate(performer, target == null ? -1L : target.getStructureId());
            }
            @Override
            public List<ActionEntry> getBehavioursFor(Creature performer, boolean onSurface, Floor target) {
                return gate(performer, target == null ? -1L : target.getStructureId());
            }
            @Override
            public List<ActionEntry> getBehavioursFor(Creature performer, Item source, boolean onSurface, Floor target) {
                return gate(performer, target == null ? -1L : target.getStructureId());
            }
        };
    }

    private List<ActionEntry> gate(Creature performer, long structureId) {
        if (performer == null) return null;
        if (performer.getPower() < GmToolsMod.minPower) return null;
        if (structureId <= 0) return null;
        return Collections.singletonList(actionEntry);
    }

    @Override
    public ActionPerformer getActionPerformer() {
        return new ActionPerformer() {
            @Override public short getActionId() { return actionId; }

            @Override
            public boolean action(Action act, Creature performer, Wall target, short num, float counter) {
                return destroy(performer, target == null ? -1L : target.getStructureId());
            }
            @Override
            public boolean action(Action act, Creature performer, Item source, Wall target, short num, float counter) {
                return destroy(performer, target == null ? -1L : target.getStructureId());
            }
            @Override
            public boolean action(Action act, Creature performer, boolean onSurface, Floor target, int encodedTile, short num, float counter) {
                return destroy(performer, target == null ? -1L : target.getStructureId());
            }
            @Override
            public boolean action(Action act, Creature performer, Item source, boolean onSurface, Floor target, int encodedTile, short num, float counter) {
                return destroy(performer, target == null ? -1L : target.getStructureId());
            }
        };
    }

    private boolean destroy(Creature performer, long structureId) {
        if (performer == null) return true;
        if (performer.getPower() < GmToolsMod.minPower) return true;
        if (structureId <= 0) return true;
        try {
            Structure structure = Structures.getStructureOrNull(structureId);
            if (structure == null) {
                performer.getCommunicator().sendNormalServerMessage("Structure not found.");
                return true;
            }
            String name = structure.getName();
            structure.totallyDestroy();
            performer.getCommunicator().sendNormalServerMessage("You obliterate " + name + ".");
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "GmTools: destroy structure failed", t);
        }
        return true;
    }
}
