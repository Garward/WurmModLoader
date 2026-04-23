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
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Patches {@code Communicator} call sites that dispatch {@code Question.answer(Properties)}
 * so they route through {@code ProxyServerHook.dispatchQuestionAnswer}, firing
 * {@code QuestionAnswerEvent}. {@code Question.answer} is abstract, so we cannot
 * patch it directly — we rewrite its call sites instead. Vanilla call sites
 * exist in at least four Communicator message handlers (item/village/buyer/
 * remove-item dialogs).
 */
public final class QuestionAnswerPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(QuestionAnswerPatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.creatures.Communicator"; }
    @Override public String methodName()       { return "*question.answer rewrite*"; }
    @Override public String methodDescriptor() { return "(all methods)"; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            final String proxy = ProxyServerHook.class.getName();
            final int[] replaced = {0};

            // Walk every method in Communicator and replace any call to
            // Question.answer(Properties) with our dispatch shim.
            for (final CtMethod m : ct.getDeclaredMethods()) {
                m.instrument(new ExprEditor() {
                    @Override
                    public void edit(MethodCall call) throws CannotCompileException {
                        if (!"answer".equals(call.getMethodName())) return;
                        String cls = call.getClassName();
                        if (!cls.endsWith("Question") && !cls.equals("com.wurmonline.server.questions.Question")) {
                            // Keep the scan narrow; Question subclasses all extend ...questions.Question.
                            if (!cls.startsWith("com.wurmonline.server.questions.")) return;
                        }
                        try {
                            call.replace(
                                "{ " + proxy + ".dispatchQuestionAnswer($0, $1); }"
                            );
                            replaced[0]++;
                        } catch (CannotCompileException cce) {
                            LOGGER.warning("QuestionAnswerPatch: failed to rewrite call in "
                                + m.getLongName() + ": " + cce.getMessage());
                        }
                    }
                });
            }

            if (replaced[0] == 0) {
                LOGGER.warning("QuestionAnswerPatch: no Question.answer(Properties) call sites found — "
                    + "QuestionAnswerEvent will not fire. Communicator structure may have changed.");
            } else {
                LOGGER.info("Registered QuestionAnswerPatch (" + replaced[0] + " call site(s) rewritten)");
            }
        } catch (NotFoundException e) {
            throw new IllegalStateException("Unable to install QuestionAnswerPatch", e);
        } catch (CannotCompileException e) {
            throw new IllegalStateException("Unable to install QuestionAnswerPatch", e);
        }
    }

    @Override public int priority() { return 55; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.QUESTION_ANSWER);
    }
}
