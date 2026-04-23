package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.internal.classhooks.CodeReplacer;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.Descriptor;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hooks the per-tick "lower slope vs chip away" decision in
 * {@code TileRockBehaviour.mine}. Vanilla rolls
 * {@code Server.rand.nextFloat() < chance} where {@code chance} is a local
 * derived from mining knowledge. We swap that test for a call to
 * {@link ProxyServerHook#fireSurfaceMiningSlopeLowerCheck(Object, Object, float)}
 * so listeners (Azbantium-Fist enchant, Azbantium pickaxe, "always lower"
 * configs, etc.) can override.
 *
 * <p>The replacement keeps the original byte length (NOPs pad the slack)
 * so jump offsets stay valid; the IFGE branch becomes IFEQ since the
 * proxy returns the desired-action boolean directly.</p>
 *
 * <p>If the byte-pattern isn't found (newer/older WU build with restructured
 * bytecode) this patch logs and continues — the framework boots, the event
 * just won't fire.</p>
 */
public final class SurfaceMiningSlopeLowerPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(SurfaceMiningSlopeLowerPatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.behaviours.TileRockBehaviour"; }
    @Override public String methodName()       { return "mine"; }
    @Override public String methodDescriptor() {
        return "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;"
             + "Lcom/wurmonline/server/items/Item;IISFII)Z";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool cp = hookManager.getClassPool();
            CtClass ctRock   = cp.get(targetClassName());
            CtClass ctServer = cp.get("com.wurmonline.server.Server");
            CtClass ctRandom = cp.get("java.util.Random");
            CtClass ctProxy  = cp.get(ProxyServerHook.class.getName());
            CtClass ctCreature = cp.get("com.wurmonline.server.creatures.Creature");
            CtClass ctItem     = cp.get("com.wurmonline.server.items.Item");

            if (ctRock.isFrozen()) ctRock.defrost();

            CtMethod m;
            try {
                m = ctRock.getDeclaredMethod(methodName());
            } catch (NotFoundException nfe) {
                LOGGER.warning("[SurfaceMiningSlopeLowerPatch] mine() not found — skipping");
                return;
            }

            MethodInfo mi = m.getMethodInfo();
            CodeAttribute ca = mi.getCodeAttribute();
            if (ca == null) {
                LOGGER.warning("[SurfaceMiningSlopeLowerPatch] no code attribute — skipping");
                return;
            }

            LocalVariableAttribute lva =
                (LocalVariableAttribute) ca.getAttribute(LocalVariableAttribute.tag);
            int chanceIdx  = lookupLocal(lva, "chance");
            int performerIdx = lookupLocal(lva, "performer");
            int sourceIdx    = lookupLocal(lva, "source");

            if (chanceIdx < 0 || performerIdx < 0 || sourceIdx < 0) {
                LOGGER.warning("[SurfaceMiningSlopeLowerPatch] required locals missing"
                    + " (chance=" + chanceIdx + ", performer=" + performerIdx
                    + ", source=" + sourceIdx + ") — skipping");
                return;
            }

            // Search: GETSTATIC Server.rand; INVOKEVIRTUAL Random.nextFloat; FLOAD chance; FCMPG; IFGE
            Bytecode search = new Bytecode(mi.getConstPool());
            search.addGetstatic(ctServer, "rand", Descriptor.of(ctRandom));
            search.addInvokevirtual(ctRandom, "nextFloat", "()F");
            search.addFload(chanceIdx);
            search.add(Opcode.FCMPG);
            search.add(Opcode.IFGE);
            byte[] searchBytes = search.get();

            // Replacement: ALOAD performer; ALOAD source; FLOAD chance; INVOKESTATIC fire; IFEQ
            // (proxy returns true when we WANT the slope-lower path; IFEQ skips when false)
            Bytecode repl = new Bytecode(mi.getConstPool());
            repl.addAload(performerIdx);
            repl.addAload(sourceIdx);
            repl.addFload(chanceIdx);
            repl.addInvokestatic(ctProxy, "fireSurfaceMiningSlopeLowerCheck",
                Descriptor.ofMethod(CtClass.booleanType,
                    new CtClass[] { cp.get("java.lang.Object"), cp.get("java.lang.Object"), CtClass.floatType }));
            repl.add(Opcode.IFEQ);
            byte[] replBytes = repl.get();

            try {
                new CodeReplacer(ca).replaceCode(searchBytes, replBytes);
                mi.rebuildStackMap(cp);
                LOGGER.info("[BytecodePatch] Registered SurfaceMiningSlopeLowerPatch successfully.");
            } catch (NotFoundException nfe) {
                LOGGER.log(Level.WARNING,
                    "[SurfaceMiningSlopeLowerPatch] vanilla nextFloat<chance pattern not found — "
                  + "WU build differs; SurfaceMiningSlopeLowerCheckEvent won't fire.", nfe);
            }
        } catch (NotFoundException | BadBytecode e) {
            LOGGER.log(Level.WARNING, "[SurfaceMiningSlopeLowerPatch] install failed", e);
        }
    }

    private static int lookupLocal(LocalVariableAttribute lva, String name) {
        if (lva == null) return -1;
        for (int i = 0; i < lva.tableLength(); i++) {
            if (name.equals(lva.variableName(i))) return lva.index(i);
        }
        return -1;
    }

    @Override public int priority() { return 55; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.SURFACE_MINING_SLOPE_LOWER);
    }
}
