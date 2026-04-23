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
import java.util.logging.Logger;

/**
 * Installs the framework's generalized action dispatcher hooks on
 * {@code BehaviourDispatcher}:
 *
 * <ul>
 *   <li>{@code action(Creature,Communicator,long,long,short)} — fires
 *       {@link com.garward.wurmmodloader.api.events.action.ActionPerformRequestEvent}
 *       for every client action; cancel aborts before any Action is constructed.</li>
 *   <li>{@code action(Creature,Communicator,long,long[],short)} — multi-target
 *       overload; fires the same event once per target id.</li>
 *   <li>{@code sendRequestResponse(byte,Communicator,RequestParam,boolean)} —
 *       fires {@link com.garward.wurmmodloader.api.events.action.ActionMenuBuildEvent}
 *       right before the menu is written to the wire, exposing the live
 *       {@code availableActions} list for in-place mutation.</li>
 * </ul>
 *
 * <p>Together these cover the vast majority of "I just want to add/change/
 * gate an action" mod surface with zero per-mod bytecode.</p>
 */
public final class BehaviourDispatcherPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(BehaviourDispatcherPatch.class.getName());

    @Override public String targetClassName() { return "com.wurmonline.server.behaviours.BehaviourDispatcher"; }
    @Override public String methodName()      { return "action"; }
    @Override public String methodDescriptor(){ return "(multiple)"; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            final String proxy = ProxyServerHook.class.getName();

            CtClass creatureCt = classPool.get("com.wurmonline.server.creatures.Creature");
            CtClass commCt     = classPool.get("com.wurmonline.server.creatures.Communicator");
            CtClass longArrCt  = classPool.get("long[]");

            // Single-target action overload.
            CtMethod actSingle = ct.getDeclaredMethod("action", new CtClass[] {
                creatureCt, commCt, CtClass.longType, CtClass.longType, CtClass.shortType
            });
            actSingle.insertBefore(
                "{ try { if (" + proxy + ".fireActionPerformRequest($1, $3, $4, $5)) return; } " +
                "catch (Throwable _t) { java.util.logging.Logger.getLogger(\"ProxyServerHook\")" +
                ".log(java.util.logging.Level.WARNING, \"Failed to fire ActionPerformRequestEvent\", _t); } }");

            // Multi-target overload — fire once per target id.
            CtMethod actMulti = ct.getDeclaredMethod("action", new CtClass[] {
                creatureCt, commCt, CtClass.longType, longArrCt, CtClass.shortType
            });
            actMulti.insertBefore(
                "{ try { if ($4 != null) { " +
                "  long[] __t = $4; " +
                "  java.util.ArrayList __kept = new java.util.ArrayList($4.length); " +
                "  for (int __i = 0; __i < __t.length; __i++) { " +
                "    if (!" + proxy + ".fireActionPerformRequest($1, $3, __t[__i], $5)) " +
                "      __kept.add(Long.valueOf(__t[__i])); " +
                "  } " +
                "  if (__kept.isEmpty()) return; " +
                "  long[] __n = new long[__kept.size()]; " +
                "  for (int __i = 0; __i < __n.length; __i++) __n[__i] = ((Long)__kept.get(__i)).longValue(); " +
                "  $4 = __n; " +
                "} } catch (Throwable _t) { java.util.logging.Logger.getLogger(\"ProxyServerHook\")" +
                ".log(java.util.logging.Level.WARNING, \"Failed to fire ActionPerformRequestEvent (multi)\", _t); } }");

            LOGGER.info("Registered BehaviourDispatcherPatch (action x2)");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install BehaviourDispatcherPatch", e);
        }
    }

    @Override public int priority() { return 53; }

    @Override
    public Collection<String> conflictKeys() {
        return Arrays.asList(BytecodeConflictKeys.ACTION_PERFORM_REQUEST);
    }
}
