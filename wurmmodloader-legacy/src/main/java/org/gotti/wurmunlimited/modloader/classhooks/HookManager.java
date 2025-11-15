package org.gotti.wurmunlimited.modloader.classhooks;

import java.lang.reflect.InvocationHandler;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.Loader;

/**
 * Legacy HookManager wrapper for backward compatibility.
 * Delegates all calls to the internal HookManager singleton.
 */
public class HookManager {

	/**
	 * Get the HookManager singleton instance.
	 * Delegates to the internal implementation.
	 */
	public static synchronized HookManager getInstance() {
		return INSTANCE;
	}

	private static final HookManager INSTANCE = new HookManager();

	private HookManager() {
	}

	private com.garward.wurmmodloader.modloader.internal.classhooks.HookManager getInternal() {
		return com.garward.wurmmodloader.modloader.internal.classhooks.HookManager.getInstance();
	}

	public ClassPool getClassPool() {
		return getInternal().getClassPool();
	}

	public Loader getLoader() {
		return getInternal().getLoader();
	}

	public void registerHook(String className, String methodName, String methodType, InvocationHandlerFactory invocationHandlerFactory) {
		getInternal().registerHook(className, methodName, methodType, invocationHandlerFactory);
	}

	public void registerHook(String className, String methodName, String methodType, InvocationHandler invocationHandler) {
		getInternal().registerHook(className, methodName, methodType, invocationHandler);
	}

	public Object invoke(Object object, String wrappedMethod, Object[] args) throws Throwable {
		return getInternal().invoke(object, wrappedMethod, args);
	}

	public static <T> T getCallback(String callbackId) {
		return com.garward.wurmmodloader.modloader.internal.classhooks.HookManager.getCallback(callbackId);
	}

	public void addCallback(CtClass targetClass, String callbackName, Object callbackTarget) {
		getInternal().addCallback(targetClass, callbackName, callbackTarget);
	}

	public void initCallbacks() {
		getInternal().initCallbacks();
	}

}
