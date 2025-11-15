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
 * Lets mods inspect and adjust Skill.checkAdvance calculations via SkillAdvanceEvent.
 */
public final class SkillAdvancePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(SkillAdvancePatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.skills.Skill";
    }

    @Override
    public String methodName() {
        return "checkAdvance";
    }

    @Override
    public String methodDescriptor() {
        return "(DLcom/wurmonline/server/items/Item;DZFZD)D";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctSkill = classPool.get(targetClassName());
            CtMethod checkAdvance = ctSkill.getDeclaredMethod("checkAdvance");

            checkAdvance.insertBefore(String.format(
                "{ com.garward.wurmmodloader.api.events.skill.SkillAdvanceEvent _wmlEvt = %s.fireSkillAdvanceEvent(this, $2, $1, $3); $1 = _wmlEvt.getDifficulty(); $3 = _wmlEvt.getPower(); }",
                ProxyServerHook.class.getName()
            ));

            LOGGER.info("[BytecodePatch] Registered SkillAdvancePatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install SkillAdvancePatch", e);
        }
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.SKILL_ADVANCE);
    }
}
