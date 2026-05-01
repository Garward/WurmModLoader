package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Wraps every {@code item.getImageNumber()} / {@code template.getImageNumber()}
 * call in {@code Communicator} (and its concrete subclass
 * {@code PlayerCommunicatorQueued}) so the result passes through
 * {@link com.garward.wurmmodloader.core.icon.OutboundIconGate#gate(short,
 * com.wurmonline.server.creatures.Communicator) OutboundIconGate.gate} before
 * landing in an outbound packet.
 *
 * <p>Vanilla clients can't render dynamic icon ids (>= 1680) — the gate
 * substitutes {@code 0} for those clients while letting modloader clients see
 * the real id. The check is keyed on the {@code com.garward.icons} ModComm
 * channel registration; {@link com.garward.wurmmodloader.core.icon.IconRegistrySyncChannel
 * IconRegistrySyncChannel} uses the same signal to gate the registry-sync
 * push.
 *
 * <p>Why an ExprEditor walk: vanilla has 18+ {@code bb.putShort(...
 * .getImageNumber())} sites in {@code Communicator} alone, and bytecode rebuilds
 * inline these calls — we have to rewrite at the {@code MethodCall} level
 * rather than wrapping {@code Item.getImageNumber()} itself (which would
 * affect non-outbound callers like template lookups, action dispatch, and the
 * creation-window UI).
 *
 * <p>Constraints honored:
 * <ul>
 *   <li>Only call sites returning {@code short} are touched (filters out
 *       hypothetical overloads and unrelated {@code getImageNumber} methods).
 *   <li>Only call sites whose enclosing method is non-static — the gate needs
 *       {@code this} to identify the recipient.
 *   <li>Only methods on {@code Item} or {@code ItemTemplate} (and subclasses)
 *       — guards against accidentally rewriting an unrelated reflection-style
 *       lookup with a coincidentally-named method.
 * </ul>
 *
 * <p>Coverage gap: outbound writes from {@code CreationWindowMethods} (~16
 * sites, all static) and {@code Ingredient}/{@code ItemTemplateFactory}/
 * {@code ItemTemplateCreator} are not gated by this patch. The creation
 * window will show {@code 0} icons for unknown templates on vanilla clients
 * regardless, since the client sheet doesn't have those slots; the cost of
 * extending the gate there is low if proven necessary in smoke testing.
 *
 * @since 0.4.1
 */
public final class OutboundIconGatePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(OutboundIconGatePatch.class.getName());

    private static final String GATE = "com.garward.wurmmodloader.core.icon.OutboundIconGate";
    private static final String COMMUNICATOR = "com.wurmonline.server.creatures.Communicator";
    private static final String PCQ = "com.wurmonline.server.players.PlayerCommunicatorQueued";

    @Override public String targetClassName()  { return COMMUNICATOR; }
    @Override public String methodName()       { return "*getImageNumber outbound gate*"; }
    @Override public String methodDescriptor() { return "(all methods)"; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        ClassPool classPool = hookManager.getClassPool();

        int total = 0;
        try {
            total += instrumentClass(classPool, COMMUNICATOR);
            total += instrumentClass(classPool, PCQ);
        } catch (NotFoundException e) {
            throw new IllegalStateException("OutboundIconGatePatch: target class missing", e);
        }

        if (total == 0) {
            LOGGER.warning("OutboundIconGatePatch: no getImageNumber() call sites found — "
                + "vanilla clients may receive raw dynamic icon ids");
        } else {
            LOGGER.info("Registered OutboundIconGatePatch (" + total + " call site(s) wrapped)");
        }
    }

    private int instrumentClass(ClassPool classPool, String className) throws NotFoundException {
        CtClass ct = classPool.get(className);
        if (ct.isFrozen()) {
            ct.defrost();
        }
        final int[] count = {0};

        ExprEditor editor = new ExprEditor() {
            @Override
            public void edit(MethodCall call) throws CannotCompileException {
                if (!"getImageNumber".equals(call.getMethodName())) return;

                CtBehavior enclosing = call.where();
                if (Modifier.isStatic(enclosing.getModifiers())) return;

                try {
                    CtClass ret = call.getMethod().getReturnType();
                    if (ret != CtClass.shortType) return;
                } catch (NotFoundException e) {
                    return;
                }

                String calledOn = call.getClassName();
                if (!isItemOrTemplate(classPool, calledOn)) return;

                try {
                    call.replace(
                        "{ $_ = $proceed($$); "
                        + "$_ = " + GATE + ".gate($_, this); }"
                    );
                    count[0]++;
                } catch (CannotCompileException cce) {
                    LOGGER.warning("OutboundIconGatePatch: could not rewrite call in "
                        + enclosing.getLongName() + ": " + cce.getMessage());
                }
            }
        };

        for (CtMethod m : ct.getDeclaredMethods()) {
            try {
                m.instrument(editor);
            } catch (CannotCompileException e) {
                LOGGER.warning("OutboundIconGatePatch: instrument failed on "
                    + m.getLongName() + ": " + e.getMessage());
            }
        }
        for (CtConstructor c : ct.getDeclaredConstructors()) {
            try {
                c.instrument(editor);
            } catch (CannotCompileException e) {
                LOGGER.warning("OutboundIconGatePatch: instrument failed on "
                    + c.getLongName() + ": " + e.getMessage());
            }
        }

        return count[0];
    }

    private boolean isItemOrTemplate(ClassPool classPool, String className) {
        if ("com.wurmonline.server.items.Item".equals(className)
            || "com.wurmonline.server.items.ItemTemplate".equals(className)) {
            return true;
        }
        try {
            CtClass cls = classPool.get(className);
            return cls.subtypeOf(classPool.get("com.wurmonline.server.items.Item"))
                || cls.subtypeOf(classPool.get("com.wurmonline.server.items.ItemTemplate"));
        } catch (NotFoundException e) {
            return false;
        }
    }

    @Override public int priority() { return 60; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.OUTBOUND_ICON_GATE);
    }
}
