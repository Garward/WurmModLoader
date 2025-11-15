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
import javassist.CtConstructor;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * Hooks into Action constructor to fire ActionFatigueEvent.
 * Allows mods to override whether an action causes fatigue (e.g., to allow harvesting while mounted).
 */
public final class ActionFatiguePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ActionFatiguePatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.behaviours.Action";
    }

    @Override
    public String methodName() {
        // Target the constructor
        return null;
    }

    @Override
    public String methodDescriptor() {
        // Constructor: Action(Creature, long, long, short, float, float, float, float)
        return "(Lcom/wurmonline/server/creatures/Creature;JJSFFFF)V";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctAction = classPool.get(targetClassName());

            // Check if class is already frozen (loaded by legacy mod)
            if (ctAction.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping ActionFatiguePatch - Action class already frozen (likely by legacy mod)");
                LOGGER.warning("[BytecodePatch] ActionFatigueEvent will not fire - consider porting legacy mod to event system");
                return;
            }

            // Get the specific constructor
            CtConstructor constructor = ctAction.getConstructor(methodDescriptor());

            String proxyClass = ProxyServerHook.class.getName();

            // Use ExprEditor to intercept isFatigue() calls within the constructor
            constructor.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall methodCall) throws CannotCompileException {
                    if (methodCall.getClassName().equals("com.wurmonline.server.behaviours.Action")
                        && methodCall.getMethodName().equals("isFatigue")) {
                        // Replace the isFatigue() call with our event-based check
                        // $proceed($$) calls the original isFatigue() to get the default value
                        // Then we pass it to fireActionFatigueEvent which lets mods override it
                        StringBuilder replacement = new StringBuilder();
                        replacement.append("{\n");
                        replacement.append("    boolean defaultFatigue = $proceed($$);\n");
                        replacement.append("    $_ = ").append(proxyClass).append(".fireActionFatigueEvent(\n");
                        replacement.append("        this.performer,\n");
                        replacement.append("        this.subject,\n");
                        replacement.append("        this.target,\n");
                        replacement.append("        this.action,\n");
                        replacement.append("        defaultFatigue\n");
                        replacement.append("    );\n");
                        replacement.append("}\n");
                        methodCall.replace(replacement.toString());
                    }
                }
            });

            LOGGER.info("Registered ActionFatigueEvent patch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ActionFatiguePatch", e);
        } catch (RuntimeException e) {
            // Catch "class is frozen" errors from legacy mods
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.warning("[BytecodePatch] Skipping ActionFatiguePatch - " + e.getMessage());
                LOGGER.warning("[BytecodePatch] ActionFatigueEvent will not fire - legacy mod conflict");
            } else {
                throw e;
            }
        }
    }

    @Override
    public int priority() {
        return 50;  // Early priority - should run before action-related patches
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.ACTION_FATIGUE);
    }
}
