package com.garward.wurmmodloader.modloader.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ReflectionUtil {

	private static List<Field> getAllFields(Class<?> clazz) {
		List<Field> currentClassFields = new ArrayList<>(Arrays.asList(clazz.getDeclaredFields()));
		Class<?> parentClass = clazz.getSuperclass();

		if (parentClass != null && !parentClass.equals(Object.class)) {
			List<Field> parentClassFields = getAllFields(parentClass);
			currentClassFields.addAll(parentClassFields);
		}

		return currentClassFields;
	}

	private static List<Method> getAllMethods(Class<?> clazz) {
		List<Method> currentClassMethods = new ArrayList<>(Arrays.asList(clazz.getDeclaredMethods()));
		Class<?> parentClass = clazz.getSuperclass();

		if (parentClass != null && !parentClass.equals(Object.class)) {
			List<Method> parentClassFields = getAllMethods(parentClass);
			currentClassMethods.addAll(parentClassFields);
		}

		return currentClassMethods;
	}
	
	public static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
		for (Field field : getAllFields(clazz)) {
			if (field.getName().equals(fieldName)) {
				return field;
			}
		}
		throw new NoSuchFieldException(fieldName);
	}

	public static Method getMethod(Class<?> clazz, String methodName) throws NoSuchMethodException {
		return getMethod(clazz, methodName, null);
	}
	
	public static Method getMethod(Class<?> clazz, String methodName, Class<?>[] signature) throws NoSuchMethodException {
		for (Method method : getAllMethods(clazz)) {
			if (method.getName().equals(methodName)) {
				if (signature == null || Arrays.equals(signature, method.getParameterTypes())) {
					return method;
				}
			}
		}
		throw new NoSuchMethodException(methodName);
	}
	
	public static <T> void setPrivateField(Object object, Field field, T value) throws IllegalArgumentException, IllegalAccessException, ClassCastException {
		boolean isAccesible = field.isAccessible();
		field.setAccessible(true);

		// Handle final fields (Java 9+ compatibility)
		Field modifiersField = null;
		int originalModifiers = field.getModifiers();
		boolean isFinal = java.lang.reflect.Modifier.isFinal(originalModifiers);

		try {
			if (isFinal) {
				// Remove final modifier to allow setting the field
				try {
					modifiersField = Field.class.getDeclaredField("modifiers");
					modifiersField.setAccessible(true);
					modifiersField.setInt(field, originalModifiers & ~java.lang.reflect.Modifier.FINAL);
				} catch (NoSuchFieldException e) {
					// Java 12+ - modifiers field doesn't exist, try anyway
				}
			}

			field.set(object, value);
		} finally {
			// Restore final modifier if we removed it
			if (isFinal && modifiersField != null) {
				try {
					modifiersField.setInt(field, originalModifiers);
				} catch (IllegalAccessException ignored) {
				}
			}
			field.setAccessible(isAccesible);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getPrivateField(Object object, Field field) throws IllegalArgumentException, IllegalAccessException, ClassCastException {
		boolean isAccesible = field.isAccessible();
		field.setAccessible(true);
		try {
			return (T) field.get(object);
		} finally {
			field.setAccessible(isAccesible);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T callPrivateMethod(Object target, Method method, Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		boolean isAccesible = method.isAccessible();
		method.setAccessible(true);
		try {
			return (T) method.invoke(target, args);
		} finally {
			method.setAccessible(isAccesible);
		}
	}
	
	public static <T> T callPrivateConstructor(Constructor<T> constructor, Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
		boolean isAccesible = constructor.isAccessible();
		constructor.setAccessible(true);
		try {
			return constructor.newInstance(args);
		} finally {
			constructor.setAccessible(isAccesible);
		}
	}
}
