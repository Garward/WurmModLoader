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

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Rewrites inner method calls inside {@code Flattening.getDirt}, {@code useDirt},
 * and {@code checkUseDirt} so mods can override dirt source/destination and
 * capacity gates without their own bytecode. Pairs with
 * {@link TerraformingDigInnerPatch} to cover the whole BetterDig port surface.
 *
 * <p>getDirt rewrites: {@code Item.insertItem}, {@code Item.getNumItemsNotCoins},
 * {@code Creature.canCarry} — same shims as the dig patch, with
 * {@code toPile=false, dredging=false} (the flatten path never piles).</p>
 *
 * <p>useDirt / checkUseDirt rewrite: {@code Creature.getCarriedItem(int)} —
 * routed to {@link ProxyServerHook#fireDirtSourceResolve} so listeners can
 * redirect the lookup to a different container.</p>
 */
public final class FlatteningInnerPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(FlatteningInnerPatch.class.getName());

    private static final String DESC_GET_DIRT =
        "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;IIIIZLcom/wurmonline/server/behaviours/Action;)V";
    private static final String DESC_USE_DIRT =
        "(Lcom/wurmonline/server/creatures/Creature;IIIIZLcom/wurmonline/server/behaviours/Action;)V";
    private static final String DESC_CHECK_USE_DIRT =
        "(IIIILcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;IILcom/wurmonline/server/behaviours/Action;Z)V";

    @Override public String targetClassName() { return "com.wurmonline.server.behaviours.Flattening"; }
    @Override public String methodName()      { return "getDirt"; }
    @Override public String methodDescriptor(){ return DESC_GET_DIRT; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();
            CtClass[] pool = new CtClass[0];

            final String proxy = ProxyServerHook.class.getName();

            CtMethod getDirt = ct.getDeclaredMethod("getDirt", new CtClass[] {
                classPool.get("com.wurmonline.server.creatures.Creature"),
                classPool.get("com.wurmonline.server.items.Item"),
                CtClass.intType, CtClass.intType, CtClass.intType, CtClass.intType,
                CtClass.booleanType,
                classPool.get("com.wurmonline.server.behaviours.Action")
            });
            getDirt.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall mc) throws CannotCompileException {
                    String cn = mc.getClassName();
                    String mn = mc.getMethodName();
                    if ("com.wurmonline.server.items.Item".equals(cn) && "insertItem".equals(mn)) {
                        mc.replace(
                            "{ $_ = " + proxy + ".fireDirtDestinationInsertItem(" +
                                "$1, performer, source, $0, false, false, \"FLATTENING_GETDIRT\"); }");
                    } else if ("com.wurmonline.server.items.Item".equals(cn) && "getNumItemsNotCoins".equals(mn)) {
                        mc.replace(
                            "{ int __v = $proceed($$); " +
                            "$_ = " + proxy + ".fireDigCapacityNumItems(" +
                                "performer, source, $0, __v, false, false); }");
                    } else if ("com.wurmonline.server.creatures.Creature".equals(cn) && "canCarry".equals(mn)) {
                        mc.replace(
                            "{ boolean __v = $proceed($$); " +
                            "$_ = " + proxy + ".fireDigCapacityCanCarry(" +
                                "performer, source, null, __v, false, false); }");
                    }
                }
            });

            CtMethod useDirt = ct.getDeclaredMethod("useDirt", new CtClass[] {
                classPool.get("com.wurmonline.server.creatures.Creature"),
                CtClass.intType, CtClass.intType, CtClass.intType, CtClass.intType,
                CtClass.booleanType,
                classPool.get("com.wurmonline.server.behaviours.Action")
            });
            useDirt.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall mc) throws CannotCompileException {
                    if ("com.wurmonline.server.creatures.Creature".equals(mc.getClassName())
                            && "getCarriedItem".equals(mc.getMethodName())) {
                        mc.replace(
                            "{ com.wurmonline.server.items.Item __v = $proceed($$); " +
                            "$_ = " + proxy + ".fireDirtSourceResolveStatic(" +
                                "performer, $1, __v, \"USE_DIRT\"); }");
                    }
                }
            });

            CtMethod checkUseDirt = ct.getDeclaredMethod("checkUseDirt", new CtClass[] {
                CtClass.intType, CtClass.intType, CtClass.intType, CtClass.intType,
                classPool.get("com.wurmonline.server.creatures.Creature"),
                classPool.get("com.wurmonline.server.items.Item"),
                CtClass.intType, CtClass.intType,
                classPool.get("com.wurmonline.server.behaviours.Action"),
                CtClass.booleanType
            });
            checkUseDirt.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall mc) throws CannotCompileException {
                    if ("com.wurmonline.server.creatures.Creature".equals(mc.getClassName())
                            && "getCarriedItem".equals(mc.getMethodName())) {
                        mc.replace(
                            "{ com.wurmonline.server.items.Item __v = $proceed($$); " +
                            "$_ = " + proxy + ".fireDirtSourceResolveStatic(" +
                                "performer, $1, __v, \"CHECK_USE_DIRT\"); }");
                    }
                }
            });

            LOGGER.info("Registered FlatteningInnerPatch (3 methods)");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install FlatteningInnerPatch", e);
        }
    }

    @Override public int priority() { return 54; }

    @Override
    public Collection<String> conflictKeys() {
        return Arrays.asList(
            BytecodeConflictKeys.DIRT_DESTINATION_RESOLVE,
            BytecodeConflictKeys.DIRT_SOURCE_RESOLVE,
            BytecodeConflictKeys.DIG_CAPACITY_OVERRIDE);
    }
}
