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
import com.wurmonline.server.structures.RoofFloorEnum;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import com.wurmonline.shared.constants.StructureConstants;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mutates an existing floor/roof/staircase placeholder into a finished piece
 * of the chosen type — analog of the vanilla mallet+plank crafting step for
 * floors. BehaviourProvider is null; menu entries are emitted by
 * {@link FloorMenuCoordinator} so variants group under a nested header.
 */
public class PlanAndFinishFloorAction implements ModAction {

    private static final Logger LOGGER = Logger.getLogger("GmTools.PlanFloor");

    private final RoofFloorEnum floorType;
    private final short actionId;
    private final ActionEntry actionEntry;

    public PlanAndFinishFloorAction(RoofFloorEnum floorType) {
        this.floorType = floorType;
        this.actionId = (short) ModActions.getNextActionId();
        this.actionEntry = ActionEntry.createEntry(
                actionId,
                floorType.getName(),
                "finishing",
                new int[] { 6, 48 }
        );
        ModActions.registerAction(actionEntry);
    }

    public RoofFloorEnum getFloorType()  { return floorType; }
    public ActionEntry getActionEntry()  { return actionEntry; }

    @Override public BehaviourProvider getBehaviourProvider() { return null; }

    @Override
    public ActionPerformer getActionPerformer() {
        return new ActionPerformer() {
            @Override public short getActionId() { return actionId; }

            @Override
            public boolean action(Action act, Creature performer, boolean onSurface, Floor target, int encodedTile, short num, float counter) {
                return mutateFloor(performer, onSurface, target);
            }

            @Override
            public boolean action(Action act, Creature performer, Item source, boolean onSurface, Floor target, int encodedTile, short num, float counter) {
                return mutateFloor(performer, onSurface, target);
            }
        };
    }

    private boolean mutateFloor(Creature performer, boolean onSurface, Floor target) {
        if (performer == null || target == null) return true;
        if (performer.getPower() < GmToolsMod.minPower) return true;
        try {
            target.setType(floorType.getType());
            target.setMaterial(floorType.getMaterial());
            target.setFloorState(StructureConstants.FloorState.COMPLETED);
            target.setQualityLevel(80.0f);
            target.setDamage(0.0f);

            VolaTile vt = Zones.getTileOrNull(target.getTileX(), target.getTileY(), onSurface);
            if (vt != null) vt.updateFloor(target);

            performer.getCommunicator().sendNormalServerMessage(
                    "You conjure a " + floorType.getName().toLowerCase() + " into being.");
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "GmTools: mutate floor (" + floorType.name() + ") failed", t);
        }
        return true;
    }
}
