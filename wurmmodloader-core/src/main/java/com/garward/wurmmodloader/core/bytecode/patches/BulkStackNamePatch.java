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
 * Rewrites {@code Item.getActualName()} call sites inside
 * {@code Item.AddBulkItem} and {@code Item.AddBulkItemToCrate} so listeners
 * can canonicalize the stacking key. Fires
 * {@link com.garward.wurmmodloader.api.events.item.BulkStackNameEvent}.
 *
 * <p>Typical use: strip the {@code "pile of "} prefix so dug dirt/sand piles
 * stack with their plain-template counterparts under one bulk row.</p>
 */
public final class BulkStackNamePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(BulkStackNamePatch.class.getName());

    private static final String DESC =
        "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z";

    @Override public String targetClassName() { return "com.wurmonline.server.items.Item"; }
    @Override public String methodName()      { return "AddBulkItem/AddBulkItemToCrate"; }
    @Override public String methodDescriptor(){ return "(multiple)"; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            final String proxy = ProxyServerHook.class.getName();

            ExprEditor nameRewriter = new ExprEditor() {
                @Override
                public void edit(MethodCall mc) throws CannotCompileException {
                    if ("com.wurmonline.server.items.Item".equals(mc.getClassName())
                            && "getActualName".equals(mc.getMethodName())) {
                        mc.replace(
                            "{ String __n = $proceed($$); " +
                            "$_ = " + proxy + ".fireBulkStackNameStatic($0, __n); }");
                    }
                }
            };

            ct.getMethod("AddBulkItem", DESC).instrument(nameRewriter);
            ct.getMethod("AddBulkItemToCrate", DESC).instrument(nameRewriter);

            LOGGER.info("Registered BulkStackNamePatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install BulkStackNamePatch", e);
        }
    }

    @Override public int priority() { return 54; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.BULK_STACK_NAME);
    }
}
