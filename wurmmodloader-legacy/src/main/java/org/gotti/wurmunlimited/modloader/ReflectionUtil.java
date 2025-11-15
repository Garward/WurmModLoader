package org.gotti.wurmunlimited.modloader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Legacy ReflectionUtil wrapper for backward compatibility.
 * Delegates all calls to the internal implementation.
 */
public final class ReflectionUtil {

	public static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
		return com.garward.wurmmodloader.modloader.internal.ReflectionUtil.getField(clazz, fieldName);
	}

	public static Method getMethod(Class<?> clazz, String methodName) throws NoSuchMethodException {
		return com.garward.wurmmodloader.modloader.internal.ReflectionUtil.getMethod(clazz, methodName);
	}

	public static Method getMethod(Class<?> clazz, String methodName, Class<?>[] signature) throws NoSuchMethodException {
		return com.garward.wurmmodloader.modloader.internal.ReflectionUtil.getMethod(clazz, methodName, signature);
	}

	public static <T> void setPrivateField(Object object, Field field, T value) throws IllegalArgumentException, IllegalAccessException, ClassCastException {
		com.garward.wurmmodloader.modloader.internal.ReflectionUtil.setPrivateField(object, field, value);
	}

	public static <T> T getPrivateField(Object object, Field field) throws IllegalArgumentException, IllegalAccessException, ClassCastException {
		return com.garward.wurmmodloader.modloader.internal.ReflectionUtil.getPrivateField(object, field);
	}

	public static <T> T callPrivateMethod(Object target, Method method, Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return com.garward.wurmmodloader.modloader.internal.ReflectionUtil.callPrivateMethod(target, method, args);
	}

	public static <T> T callPrivateConstructor(Constructor<T> constructor, Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
		return com.garward.wurmmodloader.modloader.internal.ReflectionUtil.callPrivateConstructor(constructor, args);
	}
}
