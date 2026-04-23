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
 * Fires {@link com.garward.wurmmodloader.api.events.server.ServerPreInitEvent}
 * at the top of {@code Villages.loadVillages()} — before any village, token,
 * or kingdom row is read from {@code wurmzones.db} into memory.
 *
 * <p>This is load-bearing for the custom-map world seeder: seeding has to
 * land in the DB before Wurm reads it, otherwise the in-memory registry
 * won't see the new rows until next restart.
 */
public final class ServerPreInitPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ServerPreInitPatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.villages.Villages"; }
    @Override public String methodName()       { return "loadVillages"; }
    @Override public String methodDescriptor() { return "()V"; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hm = (HookManager) hookManagerObj;
        try {
            ClassPool pool = hm.getClassPool();
            CtClass target = pool.get(targetClassName());
            CtMethod m = target.getMethod(methodName(), methodDescriptor());

            String code =
                "try {\n" +
                "    " + ProxyServerHook.class.getName() + ".fireServerPreInitEvent();\n" +
                "} catch (Exception e) {\n" +
                "    java.util.logging.Logger.getLogger(\"" + ServerPreInitPatch.class.getName() + "\")\n" +
                "        .log(java.util.logging.Level.WARNING,\n" +
                "             \"fireServerPreInitEvent failed\", e);\n" +
                "}\n";

            m.insertBefore(code);
            LOGGER.info("Registered ServerPreInitPatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ServerPreInitPatch", e);
        }
    }

    @Override public int priority() { return 10; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.SERVER_LIFECYCLE_PRE_INIT);
    }
}
