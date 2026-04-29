package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.api.titles.TitleDefinition;
import com.garward.wurmmodloader.api.titles.TitleRegistry;
import com.garward.wurmmodloader.core.titles.TitleInjector;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.ClassPool;
import javassist.CtClass;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Drains all titles registered via
 * {@link com.garward.wurmmodloader.api.titles.TitleRegistry TitleRegistry}
 * and injects them into the {@code Titles$Title} enum's static initializer.
 *
 * <p>Runs after mod {@code preInit()}, alongside other framework patches.
 * Skipped silently if no titles are queued.</p>
 */
public final class TitleInjectionPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(TitleInjectionPatch.class.getName());

    private static final String TARGET = "com.wurmonline.server.players.Titles$Title";

    @Override public String targetClassName() { return TARGET; }
    @Override public String methodName() { return "<clinit>"; }
    @Override public String methodDescriptor() { return "()V"; }

    @Override
    public void apply(Object hookManagerObj) {
        List<TitleDefinition> titles = TitleRegistry.drain();
        if (titles.isEmpty()) {
            return;
        }

        HookManager hookManager = (HookManager) hookManagerObj;
        ClassPool classPool = hookManager.getClassPool();

        try {
            CtClass titleCls = classPool.get(TARGET);
            if (titleCls.isFrozen()) {
                titleCls.defrost();
            }

            TitleInjector injector = new TitleInjector(titleCls);

            for (TitleDefinition def : titles) {
                injector.addTitle(def.getId(), def.getMaleName(), def.getFemaleName(), def.getSkillId(), def.getType());
                LOGGER.log(Level.INFO,
                    "[TitleInjectionPatch] Registered title id={0} male=\"{1}\" female=\"{2}\" skill={3} type={4}",
                    new Object[] { def.getId(), def.getMaleName(), def.getFemaleName(), def.getSkillId(), def.getType() });
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to inject custom titles into " + TARGET, e);
        }
    }

    /**
     * Run after most other patches; the Titles class is rarely touched, so the
     * standard priority of zero is fine.
     */
    @Override public int priority() { return 0; }

    @Override public Collection<String> conflictKeys() {
        return Arrays.asList(BytecodeConflictKeys.TITLES_CLINIT);
    }

    @Override public String displayName() {
        return "TitleInjectionPatch[Titles$Title.<clinit>]";
    }
}
