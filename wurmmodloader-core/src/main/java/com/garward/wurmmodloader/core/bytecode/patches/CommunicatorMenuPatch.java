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

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Universal menu-build hook. Fires
 * {@link com.garward.wurmmodloader.api.events.action.ActionMenuBuildEvent}
 * at the single chokepoint every context menu passes through on its way to
 * the client: {@code Communicator.sendAvailableActions} (and its selectBar
 * twin).
 *
 * <p>Catching here — rather than on {@code BehaviourDispatcher.sendRequestResponse}
 * — covers both the mainline paths (tiles, items, creatures, caves, etc.)
 * AND the specialized paths that bypass {@code sendRequestResponse}:
 * wounds, planets, mission-performed, bridge parts, tickets, skill-ids,
 * menu-bar. Any future menu surface that calls these sink methods is
 * automatically covered.</p>
 */
public final class CommunicatorMenuPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CommunicatorMenuPatch.class.getName());

    @Override public String targetClassName() { return "com.wurmonline.server.creatures.Communicator"; }
    @Override public String methodName()      { return "sendAvailableActions"; }
    @Override public String methodDescriptor(){ return "(multiple)"; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            final String proxy = ProxyServerHook.class.getName();

            CtClass listCt   = classPool.get("java.util.List");
            CtClass stringCt = classPool.get("java.lang.String");

            CtMethod sendAvail = ct.getDeclaredMethod("sendAvailableActions", new CtClass[] {
                CtClass.byteType, listCt, stringCt
            });
            sendAvail.insertBefore(
                "{ try { " + proxy + ".fireActionMenuBuild($0, $2, $3, false); } " +
                "catch (Throwable _t) { java.util.logging.Logger.getLogger(\"ProxyServerHook\")" +
                ".log(java.util.logging.Level.WARNING, \"Failed to fire ActionMenuBuildEvent\", _t); } }");

            CtMethod sendSelBar = ct.getDeclaredMethod("sendAvailableSelectBarActions", new CtClass[] {
                CtClass.byteType, listCt
            });
            sendSelBar.insertBefore(
                "{ try { " + proxy + ".fireActionMenuBuild($0, $2, \"\", true); } " +
                "catch (Throwable _t) { java.util.logging.Logger.getLogger(\"ProxyServerHook\")" +
                ".log(java.util.logging.Level.WARNING, \"Failed to fire ActionMenuBuildEvent\", _t); } }");

            LOGGER.info("Registered CommunicatorMenuPatch (sendAvailableActions + selectBar)");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CommunicatorMenuPatch", e);
        }
    }

    @Override public int priority() { return 52; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.ACTION_MENU_BUILD);
    }
}
