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
 * Patches {@code Creature.startTrading()} to fire both {@code TradeInitiateEvent}
 * and {@code NpcTradePermissionCheckEvent} before vanilla opens the trade window.
 * Cancellation in either handler aborts the trade.
 */
public final class TradeSessionStartPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(TradeSessionStartPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.creatures.Creature";
    }

    @Override
    public String methodName() {
        return "startTrading";
    }

    @Override
    public String methodDescriptor() {
        return "()V";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctCreature = classPool.get(targetClassName());
            if (ctCreature.isFrozen()) {
                ctCreature.defrost();
            }
            CtMethod method = ctCreature.getMethod(methodName(), methodDescriptor());

            String proxy = ProxyServerHook.class.getName();
            String code =
                "{\n" +
                "    try {\n" +
                "        if (" + proxy + ".fireTradeSessionStartEvent(this)) {\n" +
                "            if (this.getTrade() != null) { this.getTrade().end(this, false); }\n" +
                "            return;\n" +
                "        }\n" +
                "    } catch (Exception _e) {\n" +
                "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                "            .log(java.util.logging.Level.WARNING,\n" +
                "                 \"Failed to fire TradeSessionStart events\", _e);\n" +
                "    }\n" +
                "}\n";

            method.insertBefore(code);
            LOGGER.info("Registered TradeSessionStartPatch (TradeInitiateEvent + NpcTradePermissionCheckEvent)");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install TradeSessionStartPatch", e);
        }
    }

    @Override
    public int priority() {
        return 45;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Arrays.asList(BytecodeConflictKeys.TRADE_INITIATE,
                             BytecodeConflictKeys.TRADE_PERMISSION_CHECK);
    }
}
