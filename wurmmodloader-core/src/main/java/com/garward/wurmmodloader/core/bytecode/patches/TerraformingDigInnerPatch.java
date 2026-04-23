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
 * Rewrites inner method calls inside {@code Terraforming.dig(...)} so mods can
 * redirect the dirt destination and override pile/carry/volume gates without
 * writing their own bytecode. Covers the 9-arg {@code toPile} overload, which
 * is the one BetterDig / DigInCart / DigLikeMining all target.
 *
 * <p>Intercepted call sites (matching vanilla):</p>
 * <ul>
 *   <li>{@code Item.insertItem(Item, boolean)} — routed to
 *       {@link ProxyServerHook#fireDirtDestinationResolve(Object,Object,Object,Object,boolean,boolean,String)}
 *       which fires DirtDestinationResolveEvent and performs the insert on the
 *       resolved target.</li>
 *   <li>{@code Item.getNumItemsNotCoins()} — routed to
 *       {@link ProxyServerHook#fireDigCapacityNumItems} for the pile-count gate.</li>
 *   <li>{@code Creature.canCarry(long)} — routed to
 *       {@link ProxyServerHook#fireDigCapacityCanCarry} for the weight gate.</li>
 *   <li>{@code Item.getFreeVolume()} — routed to
 *       {@link ProxyServerHook#fireDigCapacityFreeVolume} for the container gate.</li>
 * </ul>
 *
 * <p>Replacements reference enclosing-method local variables by name
 * ({@code performer}, {@code tool}, {@code toPile}, {@code dredging}) — this
 * depends on the LocalVariableTable debug info that Wurm's vanilla jar ships
 * with.</p>
 */
public final class TerraformingDigInnerPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(TerraformingDigInnerPatch.class.getName());

    private static final String DESC_9 =
        "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;IIIFZLcom/wurmonline/mesh/MeshIO;Z)Z";

    @Override public String targetClassName() { return "com.wurmonline.server.behaviours.Terraforming"; }
    @Override public String methodName()      { return "dig"; }
    @Override public String methodDescriptor(){ return DESC_9; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            final String proxy = ProxyServerHook.class.getName();
            CtMethod m = ct.getMethod(methodName(), DESC_9);

            m.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall mc) throws CannotCompileException {
                    String cn = mc.getClassName();
                    String mn = mc.getMethodName();
                    if ("com.wurmonline.server.items.Item".equals(cn) && "insertItem".equals(mn)) {
                        mc.replace(
                            "{ $_ = " + proxy + ".fireDirtDestinationResolve(" +
                                "$1, performer, source, $0, dredging, toPile, \"TERRAFORMING_DIG\"); }");
                    } else if ("com.wurmonline.server.items.Item".equals(cn) && "getNumItemsNotCoins".equals(mn)) {
                        mc.replace(
                            "{ int __v = $proceed($$); " +
                            "$_ = " + proxy + ".fireDigCapacityNumItems(" +
                                "performer, source, $0, __v, toPile, dredging); }");
                    } else if ("com.wurmonline.server.creatures.Creature".equals(cn) && "canCarry".equals(mn)) {
                        mc.replace(
                            "{ boolean __v = $proceed($$); " +
                            "$_ = " + proxy + ".fireDigCapacityCanCarry(" +
                                "performer, source, null, __v, toPile, dredging); }");
                    } else if ("com.wurmonline.server.items.Item".equals(cn) && "getFreeVolume".equals(mn)) {
                        mc.replace(
                            "{ int __v = $proceed($$); " +
                            "$_ = " + proxy + ".fireDigCapacityFreeVolume(" +
                                "performer, source, $0, __v, toPile, dredging); }");
                    }
                }
            });

            LOGGER.info("Registered TerraformingDigInnerPatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install TerraformingDigInnerPatch", e);
        }
    }

    @Override public int priority() { return 54; }

    @Override
    public Collection<String> conflictKeys() {
        return Arrays.asList(
            BytecodeConflictKeys.DIRT_DESTINATION_RESOLVE,
            BytecodeConflictKeys.DIG_CAPACITY_OVERRIDE);
    }
}
