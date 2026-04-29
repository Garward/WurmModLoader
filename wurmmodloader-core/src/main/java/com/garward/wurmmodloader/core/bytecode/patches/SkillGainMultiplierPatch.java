package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * Replaces {@code Skill.doSkillGainNew} with an event-aware body so mods can
 * override the skill-gain multiplier formula via
 * {@link com.garward.wurmmodloader.api.events.skill.SkillGainMultiplierEvent}
 * without per-mod bytecode surgery.
 *
 * <p>With no subscribers, behavior is bit-for-bit identical to vanilla.</p>
 */
public final class SkillGainMultiplierPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(SkillGainMultiplierPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.skills.Skill";
    }

    @Override
    public String methodName() {
        return "doSkillGainNew";
    }

    @Override
    public String methodDescriptor() {
        return "(DDDFD)V";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctSkill = classPool.get(targetClassName());

            if (ctSkill.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping SkillGainMultiplierPatch - Skill class already frozen");
                return;
            }

            CtMethod method = ctSkill.getDeclaredMethod(methodName());
            String proxy = ProxyServerHook.class.getName();

            String body =
                "{\n" +
                "    double _bonus = 1.0;\n" +
                "    double _diff = Math.abs($1 - this.knowledge);\n" +
                "    short _sType = com.wurmonline.server.skills.SkillSystem.getTypeFor(this.number);\n" +
                "    boolean _awardBonus = true;\n" +
                "    if (_sType == 1 || _sType == 0) { _awardBonus = false; }\n" +
                "    if (_diff <= 15.0 && _awardBonus) { _bonus = 1.0 + 0.1 * (_diff / 15.0); }\n" +
                "    double _vanillaMultiplier = (100.0 - this.knowledge)\n" +
                "        / (this.getDifficulty(this.parent.priest) * this.knowledge * this.knowledge)\n" +
                "        * $3 * _bonus;\n" +
                "    boolean _vanillaWouldAdvance = ($2 >= 0.0) || (this.knowledge < 20.0);\n" +
                "    com.garward.wurmmodloader.api.events.skill.SkillGainMultiplierEvent _evt =\n" +
                "        " + proxy + ".fireSkillGainMultiplierEvent(\n" +
                "            this, $1, $2, $3, $4, $5,\n" +
                "            _bonus, _vanillaMultiplier, _vanillaWouldAdvance\n" +
                "        );\n" +
                "    if (_evt.shouldAdvance()) {\n" +
                "        this.alterSkill(_evt.getMultiplier(), false, $4, true, $5);\n" +
                "    }\n" +
                "}\n";

            method.setBody(body);
            LOGGER.info("Registered SkillGainMultiplierEvent patch for Skill.doSkillGainNew");

        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install SkillGainMultiplierPatch", e);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.warning("[BytecodePatch] Skipping SkillGainMultiplierPatch - " + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.SKILL_GAIN_MULTIPLIER);
    }
}
