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
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * Injects hooks for alliance/kingdom/village channel broadcasts so mods can intercept/override chat.
 */
public final class CommunicatorChannelPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CommunicatorChannelPatch.class.getName());

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

            CtMethod methodHandleMessage = classPool
                .get(targetClassName())
                .getMethod(methodName(), "(Ljava/nio/ByteBuffer;)V");

            methodHandleMessage.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if ("com.wurmonline.server.Players".equals(m.getClassName())
                        && "getPlayers".equals(m.getMethodName())) {
                        StringBuilder code = new StringBuilder();
                        code.append("{\n");
                        code.append(String.format(
                            "%s#communicatorChannelHook(mess);\n",
                            ProxyServerHook.class.getName()));
                        code.append("$_ = $proceed($$);\n");
                        code.append("}\n");
                        m.replace(code.toString());
                    }
                }
            });

            CtMethod methodBroadcast = classPool
                .get("com.wurmonline.server.villages.Village")
                .getMethod("broadCastMessage", "(Lcom/wurmonline/server/Message;Z)V");
            methodBroadcast.insertAfter(String.format(
                "%s#communicatorChannelHook($0, $1);\n",
                ProxyServerHook.class.getName()));

            LOGGER.fine("Communicator channel patch installed.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to instrument communicator channel handling", e);
        }
    }

    @Override
    public int priority() {
        return 25;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.COMMUNICATOR_CHANNEL);
    }
}
