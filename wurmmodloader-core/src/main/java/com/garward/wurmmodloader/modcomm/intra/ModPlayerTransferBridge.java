package com.garward.wurmmodloader.modcomm.intra;

import java.lang.reflect.Method;

import com.garward.wurmmodloader.modloader.internal.classhooks.HookException;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.Loader;

/**
 * Reflective bridge that ensures ModPlayerTransfer lives inside the HookManager
 * loader.
 */
public final class ModPlayerTransferBridge {

    private static final String TARGET_CLASS = "org.gotti.wurmunlimited.modcomm.intra.ModPlayerTransferBootstrap";

    private ModPlayerTransferBridge() {
    }

    public static void initialize() {
        invokeStatic("init");
    }

    public static void onServerStarted() {
        invokeStatic("serverStarted");
    }

    private static void invokeStatic(String methodName) {
        try {
            HookManager hookManager = HookManager.getInstance();
            Loader loader = hookManager.getLoader();
            loader.delegateLoadingOf("org.gotti.wurmunlimited.modloader.classhooks.");
            loader.delegateLoadingOf("javassist.");
            Class<?> target = Class.forName(TARGET_CLASS, true, loader);
            Method method = target.getMethod(methodName);
            method.invoke(null);
        } catch (HookException e) {
            throw e;
        } catch (ReflectiveOperationException e) {
            throw new HookException(e);
        }
    }
}
