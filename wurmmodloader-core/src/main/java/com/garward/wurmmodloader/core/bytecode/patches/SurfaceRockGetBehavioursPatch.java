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

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Patches both overloads of {@code TileRockBehaviour.getBehavioursFor} with
 * {@code insertAfter}. Symmetric to {@link CaveTileGetBehavioursPatch} — lets
 * mods edit the surface-rock context menu without writing their own patch.
 *
 * <ul>
 *   <li>5-arg: {@code getBehavioursFor(Creature, int, int, boolean, int)}</li>
 *   <li>6-arg: {@code getBehavioursFor(Creature, Item, int, int, boolean, int)}</li>
 * </ul>
 */
public final class SurfaceRockGetBehavioursPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(SurfaceRockGetBehavioursPatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.behaviours.TileRockBehaviour"; }
    @Override public String methodName()       { return "getBehavioursFor"; }
    @Override public String methodDescriptor() { return "(overloaded)"; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            CtClass creatureCt = classPool.get("com.wurmonline.server.creatures.Creature");
            CtClass itemCt = classPool.get("com.wurmonline.server.items.Item");

            String proxy = ProxyServerHook.class.getName();
            int patched = 0;

            // 5-arg no-source: (Creature, int, int, boolean, int)
            try {
                CtMethod m = ct.getDeclaredMethod(methodName(), new CtClass[]{
                    creatureCt,
                    CtClass.intType, CtClass.intType,
                    CtClass.booleanType,
                    CtClass.intType
                });
                String code =
                    "{\n" +
                    "    try {\n" +
                    "        " + proxy + ".fireSurfaceRockGetBehavioursEvent(\n" +
                    "            $1, null, $2, $3, $4, $5, $_);\n" +
                    "    } catch (Exception _e) {\n" +
                    "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                    "            .log(java.util.logging.Level.WARNING,\n" +
                    "                 \"Failed to fire SurfaceRockGetBehavioursEvent (no-source)\", _e);\n" +
                    "    }\n" +
                    "}\n";
                m.insertAfter(code);
                patched++;
            } catch (NotFoundException nfe) {
                LOGGER.warning("SurfaceRockGetBehavioursPatch: no-source overload not found");
            }

            // 6-arg with-source: (Creature, Item, int, int, boolean, int)
            try {
                CtMethod m = ct.getDeclaredMethod(methodName(), new CtClass[]{
                    creatureCt, itemCt,
                    CtClass.intType, CtClass.intType,
                    CtClass.booleanType,
                    CtClass.intType
                });
                String code =
                    "{\n" +
                    "    try {\n" +
                    "        " + proxy + ".fireSurfaceRockGetBehavioursEvent(\n" +
                    "            $1, $2, $3, $4, $5, $6, $_);\n" +
                    "    } catch (Exception _e) {\n" +
                    "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                    "            .log(java.util.logging.Level.WARNING,\n" +
                    "                 \"Failed to fire SurfaceRockGetBehavioursEvent (with-source)\", _e);\n" +
                    "    }\n" +
                    "}\n";
                m.insertAfter(code);
                patched++;
            } catch (NotFoundException nfe) {
                LOGGER.warning("SurfaceRockGetBehavioursPatch: with-source overload not found");
            }

            LOGGER.info("Registered SurfaceRockGetBehavioursPatch (" + patched + " overload(s))");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install SurfaceRockGetBehavioursPatch", e);
        }
    }

    @Override public int priority() { return 55; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.SURFACE_ROCK_GET_BEHAVIOURS);
    }
}
