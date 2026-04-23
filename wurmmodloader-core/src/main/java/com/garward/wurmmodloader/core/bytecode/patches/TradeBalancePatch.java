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
 * Patches {@code TradeHandler.balance()} to fire {@code TradeBalanceEvent}.
 * Cancellation skips the vanilla balance logic — merchant mods can then run
 * their own pricing/balancing from the event handler.
 */
public final class TradeBalancePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(TradeBalancePatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.creatures.TradeHandler";
    }

    @Override
    public String methodName() {
        return "balance";
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
            CtClass ctHandler = classPool.get(targetClassName());
            if (ctHandler.isFrozen()) {
                ctHandler.defrost();
            }
            CtMethod method = ctHandler.getMethod(methodName(), methodDescriptor());

            String proxy = ProxyServerHook.class.getName();
            String code =
                "{\n" +
                "    try {\n" +
                "        if (" + proxy + ".fireTradeBalanceEvent(this)) {\n" +
                "            return;\n" +
                "        }\n" +
                "    } catch (Exception _e) {\n" +
                "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                "            .log(java.util.logging.Level.WARNING,\n" +
                "                 \"Failed to fire TradeBalanceEvent\", _e);\n" +
                "    }\n" +
                "}\n";

            method.insertBefore(code);
            LOGGER.info("Registered TradeBalancePatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install TradeBalancePatch", e);
        }
    }

    @Override
    public int priority() {
        return 45;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.TRADE_BALANCE);
    }
}
