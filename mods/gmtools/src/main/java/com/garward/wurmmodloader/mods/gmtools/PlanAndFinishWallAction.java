package com.garward.wurmmodloader.mods.gmtools;

import com.garward.wurmmodloader.modsupport.actions.ActionPerformer;
import com.garward.wurmmodloader.modsupport.actions.BehaviourProvider;
import com.garward.wurmmodloader.modsupport.actions.ModAction;
import com.garward.wurmmodloader.modsupport.actions.ModActions;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.structures.DbDoor;
import com.wurmonline.server.structures.Door;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.structures.Structures;
import com.wurmonline.server.structures.Wall;
import com.wurmonline.server.structures.WallEnum;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import com.wurmonline.shared.constants.StructureStateEnum;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mutates an existing NO_WALL / plan-wall placeholder into a finished wall of
 * the chosen type — the analog of the vanilla mallet+wall-plan crafting step.
 * BehaviourProvider is null; menu entries are emitted by {@link WallMenuCoordinator}
 * so variants group under a nested header. Each instance still gets a unique
 * action id so GMs can hotkey a specific wall type.
 */
public class PlanAndFinishWallAction implements ModAction {

    private static final Logger LOGGER = Logger.getLogger("GmTools.PlanWall");

    private final WallEnum wallType;
    private final short actionId;
    private final ActionEntry actionEntry;

    public PlanAndFinishWallAction(WallEnum wallType) {
        this.wallType = wallType;
        this.actionId = (short) ModActions.getNextActionId();
        this.actionEntry = ActionEntry.createEntry(
                actionId,
                wallType.getName(),
                "finishing",
                new int[] { 6 /* ACTION_TYPE_NOMOVE */, 48 /* ACTION_TYPE_ENEMY_ALWAYS */ }
        );
        ModActions.registerAction(actionEntry);
    }

    public WallEnum getWallType()      { return wallType; }
    public ActionEntry getActionEntry(){ return actionEntry; }

    @Override public BehaviourProvider getBehaviourProvider() { return null; }

    @Override
    public ActionPerformer getActionPerformer() {
        return new ActionPerformer() {
            @Override public short getActionId() { return actionId; }

            @Override
            public boolean action(Action act, Creature performer, Wall target, short num, float counter) {
                return mutateWall(performer, target);
            }

            @Override
            public boolean action(Action act, Creature performer, Item source, Wall target, short num, float counter) {
                return mutateWall(performer, target);
            }
        };
    }

    private boolean mutateWall(Creature performer, Wall target) {
        if (performer == null || target == null) return true;
        if (performer.getPower() < GmToolsMod.minPower) return true;
        try {
            target.setType(wallType.getType());
            target.setMaterial(wallType.getMaterial());
            target.setState(StructureStateEnum.FINISHED);
            target.setQualityLevel(80.0f);
            target.setDamage(0.0f);

            VolaTile vt = Zones.getTileOrNull(target.getTileX(), target.getTileY(), true);
            Structure structure = Structures.getStructureOrNull(target.getStructureId());

            if (target.isDoor() && structure != null) {
                try {
                    DbDoor door = new DbDoor(target);
                    door.setStructureId(structure.getOwnerId());
                    structure.addDoor(door);
                    ((Door) door).save();
                    door.addToTiles();
                } catch (Throwable t) {
                    LOGGER.log(Level.WARNING, "GmTools: door save failed on mutate for " + wallType.name(), t);
                }
            }

            if (vt != null) vt.updateWall(target);
            if (structure != null) structure.updateStructureFinishFlag();

            performer.getCommunicator().sendNormalServerMessage(
                    "You conjure a " + wallType.getName().toLowerCase() + " into being.");
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "GmTools: mutate wall (" + wallType.name() + ") failed", t);
        }
        return true;
    }
}
