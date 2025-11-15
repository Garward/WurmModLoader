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
 * Injects SpecialMoveHandleEvent when a special move is executed.
 * Allows mods to completely override special move handling.
 */
public final class SpecialMoveHandlePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(SpecialMoveHandlePatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.behaviours.CreatureBehaviour";
    }

    @Override
    public String methodName() {
        return "handle_SPECMOVE";
    }

    @Override
    public String methodDescriptor() {
        return "(Lcom/wurmonline/server/creatures/Creature;"
            + "Lcom/wurmonline/server/creatures/Creature;SF)Z";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctCreatureBehaviour = classPool.get(targetClassName());
            CtMethod method = ctCreatureBehaviour.getDeclaredMethod(methodName());

            // Insert at beginning of method
            StringBuilder code = new StringBuilder();
            code.append("{\n");
            code.append("    try {\n");
            code.append("        com.garward.wurmmodloader.modloader.server.ServerHook$SpecialMoveResult result = ")
                .append(ProxyServerHook.class.getName())
                .append(".fireSpecialMoveHandleEvent($1, $2, $3, $4);\n");
            code.append("        if (result.cancelled) {\n");
            code.append("            return result.handlerResult;\n");
            code.append("        }\n");
            code.append("    } catch (Exception e) {\n");
            code.append("        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n");
            code.append("            .log(java.util.logging.Level.WARNING,\n");
            code.append("                 \"Failed to fire SpecialMoveHandleEvent\", e);\n");
            code.append("    }\n");
            code.append("}\n");

            method.insertBefore(code.toString());
            LOGGER.info("Registered SpecialMoveHandleEvent patch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install SpecialMoveHandlePatch", e);
        }
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.COMBAT_SPECIAL_MOVE_HANDLE);
    }
}
