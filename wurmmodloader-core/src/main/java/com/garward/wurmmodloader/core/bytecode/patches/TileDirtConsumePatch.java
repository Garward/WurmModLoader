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
 * Rewrites {@code source.destroyItem()} inside {@code TileDirtBehaviour.action}
 * so listeners can short-circuit destruction and consume by weight instead.
 * Fires {@link com.garward.wurmmodloader.api.events.farming.TileDirtConsumeEvent}.
 * If a listener sets {@code consumed = true}, vanilla destroy is skipped and
 * one template-weight is deducted from the pile.
 *
 * <p>Lets large dirt piles terraform many tiles without vanishing per action
 * tick — the BetterFarm terraform-by-weight contract.</p>
 *
 * <p>References enclosing-method locals {@code act, performer, source} — depends
 * on the LVT debug info shipped in the vanilla jar.</p>
 */
public final class TileDirtConsumePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(TileDirtConsumePatch.class.getName());

    private static final String DESC =
        "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;IIZIISF)Z";

    @Override public String targetClassName() { return "com.wurmonline.server.behaviours.TileDirtBehaviour"; }
    @Override public String methodName()      { return "action"; }
    @Override public String methodDescriptor(){ return DESC; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            final String proxy = ProxyServerHook.class.getName();
            CtMethod m = ct.getMethod(methodName(), DESC);

            m.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall mc) throws CannotCompileException {
                    if ("com.wurmonline.server.items.Item".equals(mc.getClassName())
                            && "destroyItem".equals(mc.getMethodName())) {
                        mc.replace(
                            "{ if (!" + proxy + ".fireTileDirtConsume(act, performer, source)) { " +
                                "$_ = $proceed($$); " +
                            "} }");
                    }
                }
            });

            LOGGER.info("Registered TileDirtConsumePatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install TileDirtConsumePatch", e);
        }
    }

    @Override public int priority() { return 54; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.TILE_DIRT_CONSUME);
    }
}
