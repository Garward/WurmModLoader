package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Hooks the two {@code BehaviourDispatcher.requestActionFor*} builders so
 * mods can inject/remove context-menu entries at the point where the target
 * tile coords (or target item-id) and source tool are still in scope. Fires
 * {@link com.garward.wurmmodloader.api.events.action.TileMenuBuildEvent} and
 * {@link com.garward.wurmmodloader.api.events.action.ItemMenuBuildEvent}.
 *
 * <p>Pairs with {@link CommunicatorMenuPatch}, which fires the context-free
 * {@code ActionMenuBuildEvent} at the Communicator sink. Use this patch when
 * target-aware menu injection is needed; use the sink patch when only the
 * action list matters.</p>
 */
public final class BehaviourDispatcherMenuPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(BehaviourDispatcherMenuPatch.class.getName());

    private static final String DESC_TILE =
        "(Lcom/wurmonline/server/creatures/Creature;JZLcom/wurmonline/server/items/Item;Lcom/wurmonline/server/behaviours/Behaviour;)Lcom/wurmonline/server/behaviours/BehaviourDispatcher$RequestParam;";
    private static final String DESC_ITEM =
        "(Lcom/wurmonline/server/creatures/Creature;JLcom/wurmonline/server/items/Item;Lcom/wurmonline/server/behaviours/Behaviour;)Lcom/wurmonline/server/behaviours/BehaviourDispatcher$RequestParam;";

    @Override public String targetClassName() { return "com.wurmonline.server.behaviours.BehaviourDispatcher"; }
    @Override public String methodName()      { return "requestActionFor"; }
    @Override public String methodDescriptor(){ return "(multiple)"; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            final String proxy = ProxyServerHook.class.getName();

            CtMethod tileReq = ct.getMethod("requestActionForTiles", DESC_TILE);
            tileReq.insertAfter(
                "{ try { if ($_ != null) { " +
                    proxy + ".fireTileMenuBuild($1, $2, $3, $4, $_.getAvailableActions(), $_.getHelpString()); " +
                "} } catch (Throwable _t) { java.util.logging.Logger.getLogger(\"ProxyServerHook\")" +
                ".log(java.util.logging.Level.WARNING, \"Failed to fire TileMenuBuildEvent\", _t); } }");

            CtMethod itemReq = ct.getMethod("requestActionForItemsBodyIdsCoinIds", DESC_ITEM);
            itemReq.insertAfter(
                "{ try { if ($_ != null) { " +
                    proxy + ".fireItemMenuBuild($1, $2, $3, $_.getAvailableActions(), $_.getHelpString()); " +
                "} } catch (Throwable _t) { java.util.logging.Logger.getLogger(\"ProxyServerHook\")" +
                ".log(java.util.logging.Level.WARNING, \"Failed to fire ItemMenuBuildEvent\", _t); } }");

            LOGGER.info("Registered BehaviourDispatcherMenuPatch (requestActionForTiles + requestActionForItemsBodyIdsCoinIds)");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install BehaviourDispatcherMenuPatch", e);
        }
    }

    @Override public int priority() { return 52; }

    @Override
    public Collection<String> conflictKeys() {
        return Arrays.asList(BytecodeConflictKeys.TILE_MENU_BUILD, BytecodeConflictKeys.ITEM_MENU_BUILD);
    }
}
