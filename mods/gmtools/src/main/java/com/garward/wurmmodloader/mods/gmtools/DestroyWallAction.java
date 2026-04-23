package com.garward.wurmmodloader.mods.gmtools;

import com.garward.wurmmodloader.modsupport.actions.ActionPerformer;
import com.garward.wurmmodloader.modsupport.actions.BehaviourProvider;
import com.garward.wurmmodloader.modsupport.actions.ModAction;
import com.garward.wurmmodloader.modsupport.actions.ModActions;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.structures.Structures;
import com.wurmonline.server.structures.Wall;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DestroyWallAction implements ModAction {

    private static final Logger LOGGER = Logger.getLogger("GmTools.DestroyWall");

    private final short actionId;
    private final ActionEntry actionEntry;

    public DestroyWallAction() {
        this.actionId = (short) ModActions.getNextActionId();
        this.actionEntry = ActionEntry.createEntry(
                actionId,
                "GM: destroy wall",
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
                return build(performer, target);
            }
            @Override
            public List<ActionEntry> getBehavioursFor(Creature performer, Item source, Wall target) {
                return build(performer, target);
            }
        };
    }

    private List<ActionEntry> build(Creature performer, Wall target) {
        if (performer == null || target == null) return null;
        if (performer.getPower() < GmToolsMod.minPower) return null;
        return Collections.singletonList(actionEntry);
    }

    @Override
    public ActionPerformer getActionPerformer() {
        return new ActionPerformer() {
            @Override public short getActionId() { return actionId; }

            @Override
            public boolean action(Action act, Creature performer, Wall target, short num, float counter) {
                return destroy(performer, target);
            }

            @Override
            public boolean action(Action act, Creature performer, Item source, Wall target, short num, float counter) {
                return destroy(performer, target);
            }
        };
    }

    private boolean destroy(Creature performer, Wall target) {
        if (performer == null || target == null) return true;
        if (performer.getPower() < GmToolsMod.minPower) return true;
        try {
            VolaTile vt = Zones.getTileOrNull(target.getTileX(), target.getTileY(), true);
            Structure structure = Structures.getStructureOrNull(target.getStructureId());
            if (vt != null) {
                vt.removeWall(target, false);
            }
            target.delete();
            if (structure != null) {
                structure.updateStructureFinishFlag();
            }
            performer.getCommunicator().sendNormalServerMessage("You destroy the wall.");
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "GmTools: destroy wall failed", t);
        }
        return true;
    }
}
