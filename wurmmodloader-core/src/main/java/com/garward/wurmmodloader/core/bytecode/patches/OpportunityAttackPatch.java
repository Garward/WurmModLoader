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
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * Provides OpportunityAttackEvent injection inside Creature.opportunityAttack.
 */
public final class OpportunityAttackPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(OpportunityAttackPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.creatures.Creature";
    }

    @Override
    public String methodName() {
        return "opportunityAttack";
    }

    @Override
    public String methodDescriptor() {
        return "(Lcom/wurmonline/server/creatures/Creature;)Z";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctCreature = classPool.get(targetClassName());
            CtClass creatureClass = classPool.get("com.wurmonline.server.creatures.Creature");
            CtClass combatHandlerClass = classPool.get("com.wurmonline.server.creatures.CombatHandler");
            CtClass skillClass = classPool.get("com.wurmonline.server.skills.Skill");

            CtMethod opportunityAttack = ctCreature.getDeclaredMethod("opportunityAttack", new CtClass[] { creatureClass });
            opportunityAttack.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    String methodName;
                    String className;
                    try {
                        methodName = m.getMethodName();
                        className = m.getClassName();
                    } catch (RuntimeException ex) {
                        LOGGER.fine("[OpportunityAttackPatch] Skipping unexpected call site: " + ex.getMessage());
                        return;
                    }

                    if ("skillCheck".equals(methodName) && skillClass.getName().equals(className)) {
                        m.replace(
                            "{ double _wmlResult = $proceed($$);"
                                + " double _wmlDifficulty = (double)(this.getCombatHandler().getOpportunityAttacks() * 10);"
                                + " " + ProxyServerHook.class.getName() + ".recordOpportunitySkill(_wmlResult, _wmlDifficulty);"
                                + " $_ = _wmlResult; }"
                        );
                    }
                    if ("attack".equals(methodName) && combatHandlerClass.getName().equals(className)) {
                        m.replace(
                            "{ " + ProxyServerHook.class.getName() + ".OpportunityContext _ctx = "
                                + ProxyServerHook.class.getName() + ".getOpportunityContext();"
                                + " com.garward.wurmmodloader.api.events.combat.OpportunityAttackEvent _evt = "
                                + ProxyServerHook.class.getName()
                                + ".fireOpportunityAttackEvent(this, $1, _ctx.getSkillResult(), _ctx.getDifficulty(),"
                                + " this.opportunityAttackCounter, this.getCombatHandler().getOpportunityAttacks(), $2, $4);"
                                + " if (_evt.isCancelled()) { $_ = false; } else {"
                                + "  $_ = $proceed($1, _evt.getCombatCounter(), $3, _evt.getActionCounter(), $5);"
                                + " } }"
                        );
                    }
                }
            });

            LOGGER.info("[BytecodePatch] Registered OpportunityAttackPatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install OpportunityAttackPatch", e);
        }
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.COMBAT_OPPORTUNITY);
    }
}
