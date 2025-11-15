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
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * Hooks into crafting methods (improve, polish, temper) to fire PriestRestrictionCheckEvent.
 * Allows mods to override priest crafting restrictions.
 */
public final class PriestRestrictionPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(PriestRestrictionPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.behaviours.MethodsItems";
    }

    @Override
    public String methodName() {
        // Target multiple methods - handled in apply()
        return null;
    }

    @Override
    public String methodDescriptor() {
        return null;
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctMethodsItems = classPool.get(targetClassName());

            // Check if class is already frozen (loaded by legacy mod)
            if (ctMethodsItems.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping PriestRestrictionPatch - MethodsItems class already frozen (likely by legacy mod)");
                LOGGER.warning("[BytecodePatch] PriestRestrictionCheckEvent will not fire - consider porting legacy mod to event system");
                return;
            }

            String proxyClass = ProxyServerHook.class.getName();

            // Hook improve, polish, and temper methods
            String[] methodNames = {"improveItem", "polishItem", "temper"};

            for (String methodName : methodNames) {
                try {
                    CtMethod method = ctMethodsItems.getDeclaredMethod(methodName);

                    // Use ExprEditor to intercept isPriest() calls
                    method.instrument(new ExprEditor() {
                        @Override
                        public void edit(MethodCall m) throws CannotCompileException {
                            if (m.getClassName().equals("com.wurmonline.server.creatures.Creature")
                                && m.getMethodName().equals("isPriest")) {
                                // Replace isPriest() with event-based check
                                String replacement = String.format(
                                    "$_ = %s.firePriestRestrictionCheckEvent($0, \"%s\", $proceed($$));",
                                    proxyClass, methodName
                                );
                                m.replace(replacement);
                            }
                        }
                    });

                    LOGGER.info("Registered PriestRestrictionCheckEvent patch for " + methodName);
                } catch (NotFoundException e) {
                    LOGGER.warning("Could not find method " + methodName + " in MethodsItems");
                }
            }

        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install PriestRestrictionPatch", e);
        } catch (RuntimeException e) {
            // Catch "class is frozen" errors from legacy mods
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.warning("[BytecodePatch] Skipping PriestRestrictionPatch - " + e.getMessage());
                LOGGER.warning("[BytecodePatch] PriestRestrictionCheckEvent will not fire - legacy mod conflict");
            } else {
                throw e;
            }
        }
    }

    @Override
    public int priority() {
        return 55;  // Early priority - before action patches
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.PRIEST_RESTRICTION);
    }
}
