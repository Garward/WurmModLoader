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
 * Patches {@code Trade.addShopDiff(long)} to fire {@code ShopDiffEvent} before
 * the vanilla {@code shopDiff += money} accumulation. Mods can mutate the
 * incoming amount via {@link
 * com.garward.wurmmodloader.api.events.trade.ShopDiffEvent#setMoney(long)}.
 */
public final class ShopDiffPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ShopDiffPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.items.Trade";
    }

    @Override
    public String methodName() {
        return "addShopDiff";
    }

    @Override
    public String methodDescriptor() {
        return "(J)V";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctTrade = classPool.get(targetClassName());
            if (ctTrade.isFrozen()) {
                ctTrade.defrost();
            }
            CtMethod method = ctTrade.getMethod(methodName(), methodDescriptor());

            String proxy = ProxyServerHook.class.getName();
            String code =
                "{\n" +
                "    try {\n" +
                "        $1 = " + proxy + ".fireShopDiffEvent(this, $1, this.shopDiff);\n" +
                "    } catch (Exception _e) {\n" +
                "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                "            .log(java.util.logging.Level.WARNING,\n" +
                "                 \"Failed to fire ShopDiffEvent\", _e);\n" +
                "    }\n" +
                "}\n";

            method.insertBefore(code);
            LOGGER.info("Registered ShopDiffPatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ShopDiffPatch", e);
        }
    }

    @Override
    public int priority() {
        return 45;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.TRADE_SHOP_DIFF);
    }
}
