package com.garward.wurmmodloader.core.bytecode.patches.vanillafixes;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;

import java.util.logging.Logger;

/**
 * Bytecode surgery for {@code Flattening.flatten} — replaces the hardcoded tick cadence
 * with a {@link ActionTimerHooks#shouldFlattenTick} call that scales by
 * {@code ServerEntry.getActionTimer()}.
 *
 * <p>Ported from bdew's TimerFix (GPL-2.0). The original flatten method has two
 * near-identical tick checks plus a hardcoded {@code sendActionControl} time — the
 * patch rewrites the first check to call our hook, scales the action-control duration,
 * and rewrites the second check.</p>
 */
final class FlattenPatcher {

    private static final Logger LOGGER = Logger.getLogger(FlattenPatcher.class.getName());

    private FlattenPatcher() {}

    static void patchFlatten(ClassPool classPool) throws Exception {
        CtClass ctFlattening = classPool.getCtClass("com.wurmonline.server.behaviours.Flattening");
        CtMethod ctFlatten = ctFlattening.getMethod(
            "flatten",
            "(JLcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;IIIIIIFLcom/wurmonline/server/behaviours/Action;)Z"
        );
        doPatch(ctFlatten);
    }

    private static int findNextOp(int op, CodeIterator ci) throws BadBytecode {
        while (ci.hasNext()) {
            int pos = ci.next();
            if (ci.byteAt(pos) == op) return pos;
        }
        throw new RuntimeException("Bytecode not found");
    }

    private static void writeCall(CodeIterator ci, ConstPool cp, int instaVar, int counterVar,
                                  int typeVar, int start, int next, int endif, boolean first) {
        Bytecode newCode = new Bytecode(cp);
        newCode.addIload(instaVar);
        newCode.addFload(counterVar);
        newCode.addIload(typeVar);
        newCode.addIconst(first ? 1 : 0);
        newCode.addInvokestatic(
            "com.garward.wurmmodloader.core.bytecode.patches.vanillafixes.ActionTimerHooks",
            "shouldFlattenTick",
            "(Lcom/wurmonline/server/behaviours/Action;ZFBZ)Z"
        );
        newCode.add(Bytecode.IFEQ);
        newCode.addIndex(endif - (start + newCode.currentPc() - 1));
        while (start + newCode.currentPc() < next) {
            newCode.add(Bytecode.NOP);
        }
        ci.write(newCode.get(), start);
    }

    private static void doPatch(CtMethod m) throws BadBytecode {
        MethodInfo mi = m.getMethodInfo();
        CodeAttribute ca = mi.getCodeAttribute();
        ConstPool constPool = ca.getConstPool();
        CodeIterator codeIterator = ca.iterator();

        int actionVar = -1;
        int instaVar = -1;
        int counterVar = -1;
        int typeVar = -1;

        boolean appliedPatch1 = false, appliedPatch2 = false, appliedActionControl = false;

        while (codeIterator.hasNext()) {
            int pos = codeIterator.next();
            int op = codeIterator.byteAt(pos);
            if (op == CodeIterator.INVOKESTATIC) {
                int ref = codeIterator.u16bitAt(pos + 1);
                String methodName = constPool.getMethodrefName(ref);
                if (methodName.equals("decodeType")) {
                    pos = codeIterator.next();
                    op = codeIterator.byteAt(pos);
                    if (op == CodeIterator.ISTORE) {
                        typeVar = codeIterator.byteAt(pos + 1);
                        break;
                    }
                }
            }
        }
        if (typeVar == -1) throw new RuntimeException("Type local variable not found");

        while (codeIterator.hasNext()) {
            int pos = codeIterator.next();
            int op = codeIterator.byteAt(pos);
            if (op == CodeIterator.ALOAD) {
                actionVar = codeIterator.byteAt(pos + 1);
            } else if (op == CodeIterator.INVOKEVIRTUAL) {
                int ref = codeIterator.u16bitAt(pos + 1);
                if (constPool.getMethodrefName(ref).equals("currentSecond")) {
                    int start = pos;
                    pos = findNextOp(CodeIterator.ILOAD, codeIterator);
                    instaVar = codeIterator.byteAt(pos + 1);
                    pos = findNextOp(CodeIterator.FLOAD, codeIterator);
                    counterVar = codeIterator.byteAt(pos + 1);
                    pos = findNextOp(CodeIterator.IFNE, codeIterator);
                    int endif = codeIterator.u16bitAt(pos + 1) + pos;
                    int next = codeIterator.next();
                    LOGGER.info(String.format(
                        "[VanillaFix/ActionTimer/Flatten] vars act=%d insta=%d counter=%d type=%d",
                        actionVar, instaVar, counterVar, typeVar));
                    writeCall(codeIterator, constPool, instaVar, counterVar, typeVar, start, next, endif, true);
                    appliedPatch1 = true;
                    break;
                }
            }
        }
        if (!appliedPatch1) throw new RuntimeException("Flatten patch 1 failed");

        while (codeIterator.hasNext()) {
            int pos = codeIterator.next();
            int op = codeIterator.byteAt(pos);
            if (op == CodeIterator.INVOKEVIRTUAL) {
                int ref = codeIterator.u16bitAt(pos + 1);
                if (constPool.getMethodrefName(ref).equals("sendActionControl")) {
                    Bytecode newCode = new Bytecode(constPool);
                    newCode.add(Bytecode.I2F);
                    newCode.addGetstatic("com.wurmonline.server.Servers", "localServer",
                        "Lcom/wurmonline/server/ServerEntry;");
                    newCode.addInvokevirtual("com.wurmonline.server.ServerEntry",
                        "getActionTimer", "()F");
                    newCode.add(Bytecode.FDIV);
                    newCode.add(Bytecode.F2I);
                    codeIterator.move(pos);
                    codeIterator.insert(newCode.get());
                    appliedActionControl = true;
                    break;
                }
            }
        }
        if (!appliedActionControl) throw new RuntimeException("Flatten sendActionControl patch failed");

        while (codeIterator.hasNext()) {
            int pos = codeIterator.next();
            int op = codeIterator.byteAt(pos);
            if (op == CodeIterator.INVOKEVIRTUAL) {
                int ref = codeIterator.u16bitAt(pos + 1);
                if (constPool.getMethodrefName(ref).equals("currentSecond")) {
                    int start = pos;
                    findNextOp(CodeIterator.IFEQ, codeIterator);
                    pos = findNextOp(CodeIterator.IFEQ, codeIterator);
                    int endif = codeIterator.u16bitAt(pos + 1) + pos;
                    int next = codeIterator.next();
                    writeCall(codeIterator, constPool, instaVar, counterVar, typeVar, start, next, endif, false);
                    appliedPatch2 = true;
                    break;
                }
            }
        }
        if (!appliedPatch2) throw new RuntimeException("Flatten patch 2 failed");
    }
}
