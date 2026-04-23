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
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Rewrites the {@code Item.isRaw()} / {@code Item.isSpice()} gates inside
 * {@code PlanterBehaviour.getBehavioursFor} and {@code PlanterBehaviour.action}
 * so listeners can accept custom "potable" items (mod seeds, fruit pits, etc.)
 * as plantable in planter racks. Fires
 * {@link com.garward.wurmmodloader.api.events.farming.PlanterItemAcceptEvent}.
 *
 * <p>References enclosing-method locals {@code performer, source, target} —
 * {@code source} is the herb item the player holds, {@code target} is the
 * planter rack.</p>
 */
public final class PlanterItemAcceptPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(PlanterItemAcceptPatch.class.getName());

    private static final String DESC_GET =
        "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;)Ljava/util/List;";
    private static final String DESC_ACT =
        "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;SF)Z";

    @Override public String targetClassName() { return "com.wurmonline.server.behaviours.PlanterBehaviour"; }
    @Override public String methodName()      { return "getBehavioursFor/action"; }
    @Override public String methodDescriptor(){ return "(multiple)"; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            final String proxy = ProxyServerHook.class.getName();

            ExprEditor rawEditor = new ExprEditor() {
                @Override
                public void edit(MethodCall mc) throws CannotCompileException {
                    if ("com.wurmonline.server.items.Item".equals(mc.getClassName())
                            && "isRaw".equals(mc.getMethodName())) {
                        mc.replace(
                            "{ boolean __v = $proceed($$); " +
                            "$_ = " + proxy + ".firePlanterItemAccept(performer, $0, target, \"RAW\", __v); }");
                    }
                }
            };

            ExprEditor spiceEditor = new ExprEditor() {
                @Override
                public void edit(MethodCall mc) throws CannotCompileException {
                    if ("com.wurmonline.server.items.Item".equals(mc.getClassName())
                            && "isSpice".equals(mc.getMethodName())) {
                        mc.replace(
                            "{ boolean __v = $proceed($$); " +
                            "$_ = " + proxy + ".firePlanterItemAccept(performer, $0, target, \"SPICE\", __v); }");
                    }
                }
            };

            ct.getMethod("getBehavioursFor", DESC_GET).instrument(rawEditor);
            ct.getMethod("action", DESC_ACT).instrument(spiceEditor);

            LOGGER.info("Registered PlanterItemAcceptPatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install PlanterItemAcceptPatch", e);
        }
    }

    @Override public int priority() { return 54; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.PLANTER_ITEM_ACCEPT);
    }
}
