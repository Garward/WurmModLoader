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
 * Intercepts TradingWindow.addItem to allow mods to cancel trades.
 */
public final class ItemTradePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ItemTradePatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.items.TradingWindow";
    }

    @Override
    public String methodName() {
        return "addItem";
    }

    @Override
    public String methodDescriptor() {
        return "(Lcom/wurmonline/server/items/Item;)V";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctWindow = classPool.get(targetClassName());
            if (ctWindow.isFrozen()) {
                ctWindow.defrost();
            }
            CtMethod addItem = ctWindow.getMethod(methodName(), methodDescriptor());

            StringBuilder code = new StringBuilder();
            code.append("{\n");
            code.append("    try {\n");
            code.append("        boolean cancelled = ").append(ProxyServerHook.class.getName())
                .append(".fireItemTradeEvent($1, this);\n");
            code.append("        if (cancelled) {\n");
            code.append("            return;\n");
            code.append("        }\n");
            code.append("    } catch (Exception e) {\n");
            code.append("        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n");
            code.append("            .log(java.util.logging.Level.WARNING,\n");
            code.append("                 \"Failed to fire ItemTradeEvent\", e);\n");
            code.append("    }\n");
            code.append("}\n");

            addItem.insertBefore(code.toString());
            LOGGER.info("Registered ItemTradeEvent patch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ItemTradePatch", e);
        }
    }

    @Override
    public int priority() {
        return 45;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.ITEM_TRADE);
    }
}
