package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;

/**
 * Redirects communicator message writes through {@link ProxyServerHook#communicatorMessageHook}
 * so mods can inspect/override player chat.
 */
public final class CommunicatorMessagePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CommunicatorMessagePatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.creatures.Communicator";
    }

    @Override
    public String methodName() {
        return "reallyHandle_CMD_MESSAGE";
    }

    @Override
    public String methodDescriptor() {
        return "(Ljava/nio/ByteBuffer;)V";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctCommunicator = classPool.get(targetClassName());

            CtClass[] paramTypes = { classPool.get("java.nio.ByteBuffer") };
            CtMethod method = resolveMessageHandler(ctCommunicator, paramTypes);

            method.instrument(new ExprEditor() {
                @Override
                public void edit(FieldAccess f) throws CannotCompileException {
                    if (f.isWriter()
                        && "com.wurmonline.server.creatures.Communicator".equals(f.getClassName())
                        && "commandMessage".equals(f.getFieldName())) {
                        StringBuilder code = new StringBuilder();
                        code.append("$proceed($$);\n");
                        code.append(String.format(
                            "if (%s#communicatorMessageHook(this, $1, title)) { return; };\n",
                            ProxyServerHook.class.getName()));
                        f.replace(code.toString());
                    }
                }
            });

            LOGGER.fine("Communicator message patch installed.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to instrument communicator message handling", e);
        }
    }

    private CtMethod resolveMessageHandler(CtClass communicator, CtClass[] paramTypes) throws NotFoundException {
        try {
            return communicator.getMethod(
                "reallyHandle_CMD_MESSAGE",
                Descriptor.ofMethod(CtPrimitiveType.voidType, paramTypes)
            );
        } catch (NotFoundException e) {
            // Older builds use reallyHandle(ByteBuffer)
            return communicator.getMethod(
                "reallyHandle",
                Descriptor.ofMethod(CtPrimitiveType.voidType, paramTypes)
            );
        }
    }

    @Override
    public int priority() {
        return 30;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.COMMUNICATOR_MESSAGE);
    }
}
