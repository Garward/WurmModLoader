package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.Modifier;
import javassist.NotFoundException;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Strips the {@code final} modifier from every non-static instance field on
 * {@code com.wurmonline.server.creatures.CreatureTemplate}, allowing submods
 * (e.g. Wyvern Titan / RareSpawn, Tyoda's CustomCreatures) to set fields like
 * {@code handDamage}, {@code naturalArmour}, {@code speed},
 * {@code centimetersHigh/Long/Wide}, {@code butcheredItems}, and the various
 * sound name fields via reflection without each mod shipping its own copy of
 * this Javassist trick.
 *
 * <p>Static finals (interning constants) are left alone — only instance state
 * needs to be writable for template population.</p>
 */
public final class CreatureTemplateFieldWideningPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CreatureTemplateFieldWideningPatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.creatures.CreatureTemplate"; }
    @Override public String methodName()       { return null; }
    @Override public String methodDescriptor() { return null; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            int stripped = 0;
            for (CtField field : ct.getDeclaredFields()) {
                int mods = field.getModifiers();
                if (Modifier.isStatic(mods)) continue;
                if (!Modifier.isFinal(mods)) continue;
                field.setModifiers(Modifier.clear(mods, Modifier.FINAL));
                stripped++;
            }
            LOGGER.info("Registered CreatureTemplateFieldWideningPatch (stripped final from "
                    + stripped + " instance field(s))");
        } catch (NotFoundException e) {
            throw new IllegalStateException("Unable to install CreatureTemplateFieldWideningPatch", e);
        }
    }

    @Override
    public int priority() {
        return 190;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.emptyList();
    }
}
