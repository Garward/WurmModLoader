package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * Wraps the three static {@code MethodsReligion} functions that drive altar sacrifice
 * economics so mods can tune acceptance and favor values without re-patching each
 * case branch:
 *
 * <ul>
 *   <li>{@code canBeSacrificed(Item)} — {@link com.garward.wurmmodloader.api.events.priest.SacrificeAcceptanceEvent}</li>
 *   <li>{@code getFavorValue(Deity, Item)} — {@link com.garward.wurmmodloader.api.events.priest.SacrificeFavorValueEvent}</li>
 *   <li>{@code getFavorModifier(Deity, Item)} — {@link com.garward.wurmmodloader.api.events.priest.SacrificeFavorModifierEvent}</li>
 * </ul>
 */
public final class SacrificePatches implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(SacrificePatches.class.getName());
    private static final String TARGET_CLASS = "com.wurmonline.server.behaviours.MethodsReligion";

    @Override
    public String targetClassName() {
        return TARGET_CLASS;
    }

    @Override
    public String methodName() {
        return "canBeSacrificed";
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
            CtClass ct = classPool.get(TARGET_CLASS);

            if (ct.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping SacrificePatches - MethodsReligion already frozen");
                return;
            }

            String proxy = ProxyServerHook.class.getName();

            CtMethod canBe = ct.getMethod("canBeSacrificed",
                "(Lcom/wurmonline/server/items/Item;)Z");
            canBe.insertAfter(
                "{\n" +
                "    $_ = " + proxy + ".fireSacrificeAcceptanceEvent(\n" +
                "        $1.getWurmId(), $1.getTemplateId(), $_\n" +
                "    );\n" +
                "}\n");

            CtMethod getVal = ct.getMethod("getFavorValue",
                "(Lcom/wurmonline/server/deities/Deity;Lcom/wurmonline/server/items/Item;)F");
            getVal.insertAfter(
                "{\n" +
                "    $_ = " + proxy + ".fireSacrificeFavorValueEvent(\n" +
                "        $1 == null ? -1 : $1.number,\n" +
                "        $2.getWurmId(), $2.getTemplateId(), $_\n" +
                "    );\n" +
                "}\n");

            CtMethod getMod = ct.getMethod("getFavorModifier",
                "(Lcom/wurmonline/server/deities/Deity;Lcom/wurmonline/server/items/Item;)F");
            getMod.insertAfter(
                "{\n" +
                "    $_ = " + proxy + ".fireSacrificeFavorModifierEvent(\n" +
                "        $1 == null ? -1 : $1.number,\n" +
                "        $2.getWurmId(), $2.getTemplateId(), $_\n" +
                "    );\n" +
                "}\n");

            LOGGER.info("Registered sacrifice patches — canBeSacrificed / getFavorValue / getFavorModifier");

        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install SacrificePatches", e);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.warning("[BytecodePatch] Skipping SacrificePatches - " + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Arrays.asList(
            BytecodeConflictKeys.SACRIFICE_ACCEPTANCE,
            BytecodeConflictKeys.SACRIFICE_FAVOR_VALUE,
            BytecodeConflictKeys.SACRIFICE_FAVOR_MODIFIER
        );
    }
}
