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
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Patches {@code CreatureTemplate.getColorRed/Green/Blue()} with post-hooks
 * that fire {@code CreatureTemplateColorEvent}. Each patch passes the channel
 * enum name so listeners can discriminate.
 */
public final class CreatureTemplateColorPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CreatureTemplateColorPatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.creatures.CreatureTemplate"; }
    @Override public String methodName()       { return "getColor*"; }
    @Override public String methodDescriptor() { return "()I"; }

    private static final List<String[]> TARGETS = Arrays.asList(
        new String[]{"getColorRed",   "RED"},
        new String[]{"getColorGreen", "GREEN"},
        new String[]{"getColorBlue",  "BLUE"}
    );

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            String proxy = ProxyServerHook.class.getName();
            int patched = 0;
            for (String[] target : TARGETS) {
                String name = target[0];
                String channel = target[1];
                try {
                    CtMethod method = ct.getMethod(name, "()I");
                    String code =
                        "{\n" +
                        "    try {\n" +
                        "        $_ = " + proxy + ".fireCreatureTemplateColorEvent($0, \"" + channel + "\", $_);\n" +
                        "    } catch (Exception _e) {\n" +
                        "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                        "            .log(java.util.logging.Level.WARNING,\n" +
                        "                 \"Failed to fire CreatureTemplateColorEvent\", _e);\n" +
                        "    }\n" +
                        "}\n";
                    method.insertAfter(code);
                    patched++;
                } catch (NotFoundException nfe) {
                    LOGGER.warning("CreatureTemplate." + name + " not found — skipping");
                }
            }
            if (patched == 0) {
                throw new IllegalStateException("None of CreatureTemplate.getColorRed/Green/Blue were found");
            }
            LOGGER.info("Registered CreatureTemplateColorPatch (" + patched + " channel(s))");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CreatureTemplateColorPatch", e);
        }
    }

    @Override public int priority() { return 45; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.CREATURE_TEMPLATE_COLOR);
    }
}
