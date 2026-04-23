package com.garward.wurmmodloader.api.events.eventlogic.area;

import com.wurmonline.server.behaviours.Actions;

/**
 * Descriptor for a family of area actions. Mods can reuse the built-in
 * constants or construct custom types via the public constructor.
 */
public class AreaActionType {

    public static final AreaActionType CULTIVATE =
            new AreaActionType("betterfarm:cultivate", Actions.CULTIVATE, "Cultivate", "cultivating", null);
    public static final AreaActionType SOW =
            new AreaActionType("betterfarm:sow", Actions.SOW, "Sow", "sowing", null);
    public static final AreaActionType FARM =
            new AreaActionType("betterfarm:farm", Actions.FARM, "Farm", "farming", null);
    public static final AreaActionType HARVEST =
            new AreaActionType("betterfarm:harvest", Actions.HARVEST, "Harvest", "harvesting", null);
    public static final AreaActionType HARVEST_AND_REPLANT =
            new AreaActionType("betterfarm:harvest_replant", (short) -1, "Harvest and replant", "harvesting", null);
    public static final AreaActionType PICK_SPROUT =
            new AreaActionType("betterfarm:pick_sprout", Actions.PICKSPROUT, "Pick sprout", "picking", "Nature");
    public static final AreaActionType PRUNE =
            new AreaActionType("betterfarm:prune", Actions.PRUNE, "Prune", "pruning", "Nature");
    public static final AreaActionType PLANT =
            new AreaActionType("betterfarm:plant", Actions.PLANT, "Plant", "planting", "Nature");

    public final String id;
    public final short baseAction;
    public final String name;
    public final String verb;
    public final String goesUnder;

    public AreaActionType(String id, short baseAction, String name, String verb, String goesUnder) {
        this.id = id;
        this.baseAction = baseAction;
        this.name = name;
        this.verb = verb;
        this.goesUnder = goesUnder;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AreaActionType && ((AreaActionType) o).id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
