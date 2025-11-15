package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * Re-routes vanilla action timer helpers to ActionTimeEvent so mods can adjust durations.
 */
public final class ActionTimingPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ActionTimingPatch.class.getName());

    private static final class Mapping {
        private final String method;
        private final String performerArg;
        private final String sourceArg;
        private final String targetArg;

        private Mapping(String method, String performerArg, String sourceArg, String targetArg) {
            this.method = method;
            this.performerArg = performerArg;
            this.sourceArg = sourceArg;
            this.targetArg = targetArg;
        }
    }

    private static final Mapping[] MAPPINGS = new Mapping[] {
        new Mapping("getStandardActionTime", "$1", "$3", "null"),
        new Mapping("getQuickActionTime", "$1", "$3", "null"),
        new Mapping("getVariableActionTime", "$1", "$3", "null"),
        new Mapping("getSlowActionTime", "$1", "$3", "null"),
        new Mapping("getPickActionTime", "$1", "$3", "null"),
        new Mapping("getItemCreationTime", "$2", "$5", "$6")
    };

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.behaviours.Actions";
    }

    @Override
    public String methodName() {
        return "getStandardActionTime";
    }

    @Override
    public String methodDescriptor() {
        return "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/skills/Skill;Lcom/wurmonline/server/items/Item;D)I";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctActions = classPool.get(targetClassName());
            String proxy = ProxyServerHook.class.getName();

            for (Mapping mapping : MAPPINGS) {
                CtMethod method = ctActions.getDeclaredMethod(mapping.method);
                method.insertAfter(String.format(
                    "{ $_ = (int)%s.fireActionTimeEvent(%s, %s, %s, (float)$_); }",
                    proxy,
                    mapping.performerArg,
                    mapping.sourceArg,
                    mapping.targetArg
                ));
            }

            LOGGER.info("[BytecodePatch] Registered ActionTimingPatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ActionTimingPatch", e);
        }
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.ACTION_TIME);
    }
}
