package com.garward.wurmmodloader.mods.gmtools;

import com.garward.wurmmodloader.api.events.action.TileMenuBuildEvent;
import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.structure.StructureGateCheckEvent;
import com.garward.wurmmodloader.api.events.eventlogic.area.AreaActionDef;
import com.garward.wurmmodloader.api.events.eventlogic.area.AreaActionType;
import com.garward.wurmmodloader.api.events.eventlogic.area.TileAreaHandler;
import com.garward.wurmmodloader.modloader.interfaces.Configurable;
import com.garward.wurmmodloader.modloader.interfaces.Initable;
import com.garward.wurmmodloader.modloader.interfaces.PreInitable;
import com.garward.wurmmodloader.modloader.interfaces.ServerStartedListener;
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;
import com.garward.wurmmodloader.modsupport.actions.ModActions;
import com.garward.wurmmodloader.modsupport.actions.area.AreaActionMenus;
import com.garward.wurmmodloader.modsupport.actions.area.AreaActionRegistrar;
import com.garward.wurmmodloader.modsupport.actions.area.AreaActionRegistry;

import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.structures.RoofFloorEnum;
import com.wurmonline.server.structures.WallEnum;
import com.wurmonline.shared.constants.StructureTypeEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GM tools — unified admin toolkit shipped with WurmModLoader. Supersedes the
 * community-ports {@code adminterrain} mod (same terrain features, same UX)
 * and extends it with building tools (instant-construct walls / floors /
 * structure removal) and — later — recording to reusable blueprints.
 *
 * <p>All actions gated on {@code performer.getPower() >= minPower} so regular
 * players never see the menu entries. Menu category is "GM tools".
 *
 * <p>Terrain: level-to-me, raise/lower by configurable dirt steps, each with
 * a radius variant menu. Building tools: scaffolded, registration wires up
 * on {@code onServerStarted} once handlers are implemented.
 */
public class GmToolsMod implements WurmServerMod, Configurable, PreInitable, Initable, ServerStartedListener {

    private static final Logger LOGGER = Logger.getLogger("GmTools");

    public static final AreaActionType LEVEL =
            new AreaActionType("gmtools:terrain:level", (short) -1, "GM: level to me", "leveling", "GM tools");

    static int minPower = 2;
    private final List<AreaActionDef> radiusDefs = new ArrayList<>();
    private int[] steps = {5, 20, 100};
    private AreaActionRegistrar registrar;

    // Toggle groups — future tools can be enabled/disabled independently.
    private boolean enableTerrain = true;
    private boolean enableBuilding = false; // scaffolded; placeholder until BuildHandlers land

    @Override
    public void configure(Properties p) {
        minPower = Integer.parseInt(p.getProperty("minPower", "2"));
        enableTerrain = Boolean.parseBoolean(p.getProperty("enableTerrain", "true"));
        enableBuilding = Boolean.parseBoolean(p.getProperty("enableBuilding", "false"));

        radiusDefs.clear();
        for (String r : p.getProperty("radii", "0,1,2,3,7").split(",")) {
            String t = r.trim();
            if (t.isEmpty()) continue;
            radiusDefs.add(new AreaActionDef(Integer.parseInt(t), 0f));
        }

        String stepStr = p.getProperty("dirtSteps", "5,20,100");
        List<Integer> parsed = new ArrayList<>();
        for (String s : stepStr.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) parsed.add(Integer.parseInt(t));
        }
        steps = parsed.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override public void preInit() { ModActions.init(); }
    @Override public void init()    {}

    @Override
    public void onServerStarted() {
        registrar = new AreaActionRegistrar();

        if (enableTerrain) registerTerrainTools();
        if (enableBuilding) registerBuildingTools();

        LOGGER.log(Level.INFO, "GmTools ready: terrain=" + enableTerrain
                + " building=" + enableBuilding
                + " radii=" + radiusDefs.size()
                + " steps=" + java.util.Arrays.toString(steps)
                + " minPower=" + minPower);
    }

    private void registerTerrainTools() {
        // Menu order mirrors registration order (registrar uses LinkedHashMap).
        // Level → all raise ascending → all lower ascending. GMs can still hotkey
        // directly, so deep grouping doesn't cost discoverability.
        TileAreaHandler levelHandler = new TerrainHandlers.Level();
        registerOnAllTerrain(LEVEL, levelHandler);
        registrar.register(LEVEL, radiusDefs);

        for (int units : steps) {
            AreaActionType raise = new AreaActionType(
                    "gmtools:terrain:raise:" + units, (short) -1,
                    "GM: raise +" + units + " dirt", "raising", "GM tools");
            registerOnAllTerrain(raise, new TerrainHandlers.Raise(units));
            registrar.register(raise, radiusDefs);
        }

        for (int units : steps) {
            AreaActionType lower = new AreaActionType(
                    "gmtools:terrain:lower:" + units, (short) -1,
                    "GM: lower -" + units + " dirt", "lowering", "GM tools");
            registerOnAllTerrain(lower, new TerrainHandlers.Lower(units));
            registrar.register(lower, radiusDefs);
        }
    }

    private void registerBuildingTools() {
        // One ModAction per wall/floor type so every variant is hotkey-able. GMs
        // still plan through the vanilla mallet flow (gates bypassed by the three
        // StructureGateCheckEvent patches for planning skill/material/level);
        // right-clicking an unfinished plan yields "GM: finish as <type>" for
        // every wall or floor variant.
        int wallCount = 0;
        List<PlanAndFinishWallAction> wallVariants = new ArrayList<>();
        for (WallEnum w : WallEnum.values()) {
            if (w.getType() == StructureTypeEnum.PLAN) continue;
            PlanAndFinishWallAction pf = new PlanAndFinishWallAction(w);
            ModActions.registerAction(pf);
            wallVariants.add(pf);
            wallCount++;
        }
        ModActions.registerAction(new WallMenuCoordinator(wallVariants));
        ModActions.registerAction(new DestroyWallAction());
        ModActions.registerAction(new DestroyStructureAction());

        int floorCount = 0;
        List<PlanAndFinishFloorAction> floorVariants = new ArrayList<>();
        for (RoofFloorEnum f : RoofFloorEnum.values()) {
            if (f == RoofFloorEnum.UNKNOWN) continue;
            PlanAndFinishFloorAction pf = new PlanAndFinishFloorAction(f);
            ModActions.registerAction(pf);
            floorVariants.add(pf);
            floorCount++;
        }
        ModActions.registerAction(new FloorMenuCoordinator(floorVariants));
        ModActions.registerAction(new DestroyFloorAction());
        LOGGER.log(Level.INFO, "GmTools: registered " + wallCount + " plan-walls, "
                + floorCount + " plan-floors");
    }

    private void registerOnAllTerrain(AreaActionType type, TileAreaHandler handler) {
        for (Tiles.Tile t : Tiles.Tile.values()) {
            if (!isTerraformable(t)) continue;
            AreaActionRegistry.registerTileHandler(t.id, type, handler);
        }
    }

    private static boolean isTerraformable(Tiles.Tile t) {
        if (t == null) return false;
        if (Tiles.isTree(t.id) || Tiles.isBush(t.id)) return false;
        switch (t) {
            case TILE_HOLE:
            case TILE_LAVA:
            case TILE_MINE_DOOR_WOOD:
            case TILE_MINE_DOOR_STONE:
            case TILE_MINE_DOOR_GOLD:
            case TILE_MINE_DOOR_SILVER:
                return false;
            default:
                return true;
        }
    }

    @SubscribeEvent
    public void onStructureGateCheck(StructureGateCheckEvent e) {
        if (!enableBuilding) return;
        if (e.getPerformer() == null) return;
        if (e.getPerformer().getPower() >= minPower) {
            e.setBypass(true);
        }
    }

    @SubscribeEvent
    public void onTileMenuBuild(TileMenuBuildEvent e) {
        if (registrar == null) return;
        if (e.getPerformer() == null || e.getPerformer().getPower() < minPower) return;
        AreaActionMenus.mutateTileMenu(
                e.getAvailableActions(), e.getPerformer(),
                e.getTarget(), e.isOnSurface(), e.getSource(),
                registrar.performers());
    }
}
