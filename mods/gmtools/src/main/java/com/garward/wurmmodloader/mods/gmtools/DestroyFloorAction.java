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
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DestroyFloorAction implements ModAction {

    private static final Logger LOGGER = Logger.getLogger("GmTools.DestroyFloor");

    private final short actionId;
    private final ActionEntry actionEntry;

    public DestroyFloorAction() {
        this.actionId = (short) ModActions.getNextActionId();
        this.actionEntry = ActionEntry.createEntry(
                actionId,
                "GM: destroy floor",
                "destroying",
                new int[] { 6, 48 }
        );
        ModActions.registerAction(actionEntry);
    }

    @Override
    public BehaviourProvider getBehaviourProvider() {
        return new BehaviourProvider() {
            @Override
            public List<ActionEntry> getBehavioursFor(Creature performer, boolean onSurface, Floor target) {
                return gate(performer, target);
            }
            @Override
            public List<ActionEntry> getBehavioursFor(Creature performer, Item source, boolean onSurface, Floor target) {
                return gate(performer, target);
            }
        };
    }

    private List<ActionEntry> gate(Creature performer, Floor target) {
        if (performer == null || target == null) return null;
        if (performer.getPower() < GmToolsMod.minPower) return null;
        return Collections.singletonList(actionEntry);
    }

    @Override
    public ActionPerformer getActionPerformer() {
        return new ActionPerformer() {
            @Override public short getActionId() { return actionId; }

            @Override
            public boolean action(Action act, Creature performer, boolean onSurface, Floor target, int encodedTile, short num, float counter) {
                return destroy(performer, onSurface, target);
            }
            @Override
            public boolean action(Action act, Creature performer, Item source, boolean onSurface, Floor target, int encodedTile, short num, float counter) {
                return destroy(performer, onSurface, target);
            }
        };
    }

    private boolean destroy(Creature performer, boolean onSurface, Floor target) {
        if (performer == null || target == null) return true;
        if (performer.getPower() < GmToolsMod.minPower) return true;
        try {
            VolaTile vt = Zones.getTileOrNull(target.getTileX(), target.getTileY(), onSurface);
            if (vt != null) {
                vt.removeFloor(target);
            }
            target.delete();
            performer.getCommunicator().sendNormalServerMessage("You destroy the floor.");
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "GmTools: destroy floor failed", t);
        }
        return true;
    }
}
