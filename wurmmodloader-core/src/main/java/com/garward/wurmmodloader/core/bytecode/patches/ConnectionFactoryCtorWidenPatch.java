package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.Modifier;
import javassist.NotFoundException;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Widens package-private database-SPI types so mod-side subclasses (loaded in
 * their own URLClassLoader) can use them. Cross-classloader runtime packages
 * are distinct even when source packages match, so package-private access
 * fails with {@link IllegalAccessError}.
 *
 * <p>Affected types:</p>
 * <ul>
 *   <li>{@code com.wurmonline.server.database.ConnectionFactory} — ctor</li>
 *   <li>{@code com.wurmonline.server.database.migrations.Migrator} — both ctors</li>
 *   <li>{@code com.wurmonline.server.database.migrations.Migrator$FlywayConfigurer} — class + method</li>
 * </ul>
 */
public final class ConnectionFactoryCtorWidenPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ConnectionFactoryCtorWidenPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.database.ConnectionFactory";
    }

    @Override public String methodName() { return null; }
    @Override public String methodDescriptor() { return null; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        ClassPool classPool = hookManager.getClassPool();
        widenConstructors(classPool, "com.wurmonline.server.database.ConnectionFactory");
        widenConstructors(classPool, "com.wurmonline.server.database.migrations.Migrator");
        widenClassAndMembers(classPool, "com.wurmonline.server.database.migrations.Migrator$FlywayConfigurer");
        // MigrationResult.newSuccess/newError are package-private static factories
        // that third-party MigrationStrategy implementations need to call.
        widenDeclaredMethods(classPool, "com.wurmonline.server.database.migrations.MigrationResult",
            "newSuccess", "newError");
    }

    private static void widenDeclaredMethods(ClassPool cp, String cls, String... methodNames) {
        try {
            CtClass ct = cp.get(cls);
            if (ct.isFrozen()) ct.defrost();
            int count = 0;
            for (javassist.CtMethod m : ct.getDeclaredMethods()) {
                for (String name : methodNames) {
                    if (m.getName().equals(name)) {
                        int mm = m.getModifiers();
                        if (!Modifier.isPublic(mm)) {
                            m.setModifiers((mm & ~(Modifier.PRIVATE | Modifier.PROTECTED)) | Modifier.PUBLIC);
                            count++;
                        }
                    }
                }
            }
            LOGGER.info("[BytecodePatch] " + cls + ": widened " + count + " method(s) to public");
        } catch (NotFoundException e) {
            throw new IllegalStateException("Widening methods failed for " + cls, e);
        }
    }

    private static void widenConstructors(ClassPool cp, String cls) {
        try {
            CtClass ct = cp.get(cls);
            if (ct.isFrozen()) ct.defrost();
            int count = 0;
            for (CtConstructor ctor : ct.getDeclaredConstructors()) {
                int mods = ctor.getModifiers();
                if (!Modifier.isPublic(mods)) {
                    ctor.setModifiers((mods & ~(Modifier.PRIVATE | Modifier.PROTECTED)) | Modifier.PUBLIC);
                    count++;
                }
            }
            LOGGER.info("[BytecodePatch] " + cls + ": widened " + count + " ctor(s) to public");
        } catch (NotFoundException e) {
            throw new IllegalStateException("Widening ctors failed for " + cls, e);
        }
    }

    private static void widenClassAndMembers(ClassPool cp, String cls) {
        try {
            CtClass ct = cp.get(cls);
            if (ct.isFrozen()) ct.defrost();
            int mods = ct.getModifiers();
            if (!Modifier.isPublic(mods)) {
                ct.setModifiers((mods & ~(Modifier.PRIVATE | Modifier.PROTECTED)) | Modifier.PUBLIC);
            }
            for (javassist.CtMethod m : ct.getDeclaredMethods()) {
                int mm = m.getModifiers();
                if (!Modifier.isPublic(mm)) {
                    m.setModifiers((mm & ~(Modifier.PRIVATE | Modifier.PROTECTED)) | Modifier.PUBLIC);
                }
            }
            LOGGER.info("[BytecodePatch] " + cls + ": widened class + methods to public");
        } catch (NotFoundException e) {
            throw new IllegalStateException("Widening class failed for " + cls, e);
        }
    }

    @Override public int priority() { return 200; }

    @Override public Collection<String> conflictKeys() { return Collections.emptyList(); }
}
