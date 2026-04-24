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
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hooks the per-tick "surrounding area must be rock or cliff" abort path in
 * {@code TileRockBehaviour.mine}. Vanilla emits:
 * <pre>
 *   performer.getCommunicator().sendNormalServerMessage(
 *       "The surrounding area needs to be rock before you mine.", (byte)3);
 *   return true;
 * </pre>
 * which compiles to a deterministic 13-byte block. We replace it in-place
 * (same byte count) with a static call to
 * {@link ProxyServerHook#fireSurfaceMiningSurroundCheck(Object, Object)} —
 * if the proxy returns {@code true}, the original "return true" is taken
 * (proxy sends the message itself); if it returns {@code false}, the IFEQ
 * branches past the return so the loop continues normally.
 *
 * <p>If the byte-pattern isn't found (newer/older WU build with restructured
 * bytecode) this patch logs and continues — the framework boots, the event
 * just won't fire.</p>
 */
public final class SurfaceMiningSurroundCheckPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(SurfaceMiningSurroundCheckPatch.class.getName());

    private static final String SURROUND_MSG =
        "The surrounding area needs to be rock before you mine.";

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
            CtClass ctRock        = cp.get(targetClassName());
            CtClass ctCreature    = cp.get("com.wurmonline.server.creatures.Creature");
            CtClass ctCommunicator= cp.get("com.wurmonline.server.creatures.Communicator");
            CtClass ctProxy       = cp.get(ProxyServerHook.class.getName());
            CtClass ctObject      = cp.get("java.lang.Object");

            if (ctRock.isFrozen()) ctRock.defrost();

            CtMethod m;
            try {
                m = ctRock.getDeclaredMethod(methodName());
            } catch (NotFoundException nfe) {
                LOGGER.warning("[SurfaceMiningSurroundCheckPatch] mine() not found — skipping");
                return;
            }

            MethodInfo mi = m.getMethodInfo();
            CodeAttribute ca = mi.getCodeAttribute();
            if (ca == null) {
                LOGGER.warning("[SurfaceMiningSurroundCheckPatch] no code attribute — skipping");
                return;
            }

            // Search: the vanilla 13-byte abort block
            //   ALOAD_1 (performer)
            //   INVOKEVIRTUAL Creature.getCommunicator()Communicator
            //   LDC_W "The surrounding area..."
            //   ICONST_3
            //   INVOKEVIRTUAL Communicator.sendNormalServerMessage(String,B)V
            //   ICONST_1
            //   IRETURN
            Bytecode search = new Bytecode(mi.getConstPool());
            search.add(Opcode.ALOAD_1);
            search.addInvokevirtual(ctCreature, "getCommunicator",
                Descriptor.ofMethod(ctCommunicator, new CtClass[] {}));
            search.addLdc(SURROUND_MSG);
            search.add(Opcode.ICONST_3);
            search.addInvokevirtual(ctCommunicator, "sendNormalServerMessage",
                Descriptor.ofMethod(CtClass.voidType,
                    new CtClass[] { cp.get("java.lang.String"), CtClass.byteType }));
            search.add(Opcode.ICONST_1);
            search.add(Opcode.IRETURN);
            byte[] searchBytes = search.get();

            if (searchBytes.length != 13) {
                LOGGER.warning("[SurfaceMiningSurroundCheckPatch] unexpected search length "
                    + searchBytes.length + " (expected 13) — skipping");
                return;
            }

            // Replacement (13 bytes):
            //   0  ALOAD_1 (performer)
            //   1  ALOAD_2 (source)
            //   2  INVOKESTATIC fireSurfaceMiningSurroundCheck(Obj,Obj)Z
            //   5  IFEQ +8           (skip return when proxy says "allow")
            //   8  ICONST_1
            //   9  IRETURN
            //  10  NOP
            //  11  NOP
            //  12  NOP
            Bytecode repl = new Bytecode(mi.getConstPool());
            repl.add(Opcode.ALOAD_1);
            repl.add(Opcode.ALOAD_2);
            repl.addInvokestatic(ctProxy, "fireSurfaceMiningSurroundCheck",
                Descriptor.ofMethod(CtClass.booleanType, new CtClass[] { ctObject, ctObject }));
            repl.add(Opcode.IFEQ);
            repl.add(0x00);
            repl.add(0x08); // branch +8 from IFEQ at offset 5 → land at offset 13
            repl.add(Opcode.ICONST_1);
            repl.add(Opcode.IRETURN);
            repl.add(Opcode.NOP);
            repl.add(Opcode.NOP);
            repl.add(Opcode.NOP);
            byte[] replBytes = repl.get();

            if (replBytes.length != 13) {
                LOGGER.warning("[SurfaceMiningSurroundCheckPatch] unexpected replacement length "
                    + replBytes.length + " (expected 13) — skipping");
                return;
            }

            try {
                new CodeReplacer(ca).replaceCode(searchBytes, replBytes);
                mi.rebuildStackMap(cp);
                LOGGER.info("[BytecodePatch] Registered SurfaceMiningSurroundCheckPatch successfully.");
            } catch (NotFoundException nfe) {
                LOGGER.log(Level.WARNING,
                    "[SurfaceMiningSurroundCheckPatch] vanilla surround-abort pattern not found — "
                  + "WU build differs; SurfaceMiningSurroundCheckEvent won't fire.", nfe);
            }
        } catch (NotFoundException | BadBytecode e) {
            LOGGER.log(Level.WARNING, "[SurfaceMiningSurroundCheckPatch] install failed", e);
        }
    }

    @Override public int priority() { return 55; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.SURFACE_MINING_SURROUND_CHECK);
    }
}
