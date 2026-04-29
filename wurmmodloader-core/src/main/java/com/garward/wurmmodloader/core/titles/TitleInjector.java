package com.garward.wurmmodloader.core.titles;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;

/**
 * Walks {@code Titles$Title}'s {@code <clinit>} bytecode and prepares it for
 * additional enum entries: it locates the final {@code AASTORE}, the
 * {@code ANEWARRAY} that backs {@code $VALUES}, and the highest existing
 * ordinal so subsequent {@link #addTitle} calls can append new entries
 * with consistent ordinals and a resized backing array.
 *
 * <p>Adapted from {@code net.bdew.wurm.tools.server.internal.TitleInjector}
 * (bdew_server_mod_tools, MIT). All actual class manipulation happens once,
 * driven by {@link TitleInjectionPatch}.</p>
 */
public final class TitleInjector {

    private final ConstPool constPool;
    private final CodeIterator codeIterator;
    private int insertPos = -1;
    private int lastOrd = -1;
    private int arraySizePos = -1;

    public TitleInjector(CtClass titleCls) throws BadBytecode, NotFoundException {
        CtConstructor initializer = titleCls.getClassInitializer();
        CodeAttribute codeAttr = initializer.getMethodInfo().getCodeAttribute();
        constPool = codeAttr.getConstPool();
        codeIterator = codeAttr.iterator();

        // Reserve a bit more stack space than javac generated.
        codeAttr.setMaxStack(codeAttr.getMaxStack() + 3);

        while (codeIterator.hasNext()) {
            int pos = codeIterator.next();
            int op = codeIterator.byteAt(pos);
            if (op == Bytecode.AASTORE) {
                insertPos = codeIterator.next();
            } else if (op == Bytecode.ANEWARRAY) {
                arraySizePos = pos - 2;
            } else if (op == Bytecode.NEW) {
                pos = codeIterator.next(); // dup
                pos = codeIterator.next(); // ldc of ident
                pos = codeIterator.next(); // ordinal slot
                lastOrd = TitleByteCodeUtils.getInteger(constPool, codeIterator, pos);
            }
        }

        if (insertPos == -1) throw new IllegalStateException("Failed to find AASTORE in Titles$Title <clinit>");
        if (lastOrd == -1) throw new IllegalStateException("Failed to find existing ordinal in Titles$Title <clinit>");
        if (arraySizePos == -1) throw new IllegalStateException("Failed to find $VALUES array size in Titles$Title <clinit>");
    }

    public void addTitle(int id, String name, String femaleName, int skillId, String type)
            throws BadBytecode, CannotCompileException, NotFoundException {

        int ordinal = ++lastOrd;
        Bytecode code = new Bytecode(constPool);

        // Backing $VALUES array is on stack — dup it for AASTORE later.
        code.add(Bytecode.DUP);

        // Ordinal slot for AASTORE.
        TitleByteCodeUtils.putInteger(constPool, code, ordinal);

        // new Titles$Title(...)
        code.addNew("com.wurmonline.server.players.Titles$Title");
        code.add(Bytecode.DUP);

        // Constructor args: enum name, ordinal, id, male, female, skill, type.
        code.addLdc("CUSTOM_" + id);
        TitleByteCodeUtils.putInteger(constPool, code, ordinal);
        TitleByteCodeUtils.putInteger(constPool, code, id);
        code.addLdc(name);
        code.addLdc(femaleName);
        TitleByteCodeUtils.putInteger(constPool, code, skillId);
        code.addGetstatic(
            "com.wurmonline.server.players.Titles$TitleType",
            type,
            "Lcom/wurmonline/server/players/Titles$TitleType;");

        code.addInvokespecial(
            "com.wurmonline.server.players.Titles$Title",
            "<init>",
            "(Ljava/lang/String;IILjava/lang/String;Ljava/lang/String;ILcom/wurmonline/server/players/Titles$TitleType;)V");

        // Stash into $VALUES[ordinal].
        code.add(Bytecode.AASTORE);

        byte[] bytes = code.get();
        codeIterator.insertAt(insertPos, bytes);
        insertPos += bytes.length;

        // Bump $VALUES length to match.
        codeIterator.write16bit(codeIterator.u16bitAt(arraySizePos) + 1, arraySizePos);
    }
}
