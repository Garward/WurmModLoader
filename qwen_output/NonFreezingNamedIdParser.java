package com.garward.wurmmodloader.modsupport;

import org.gotti.wurmunlimited.modloader.DefrostingClassLoader;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

import javassist.CannotCompileException;
import javassist.NotFoundException;

/**
 * Abstract base class for creating NamedIdParser implementations that utilize
 * the {@link DefrostingClassLoader} to load name mapping classes.
 * 
 * <p>This class provides a framework for parsing named ID mappings where the
 * underlying class containing the mappings is loaded through a defrosting
 * classloader. This allows the generated class to be modified after initial
 * loading, enabling further transformations or enhancements by other mods.
 * 
 * <p><strong>Purpose:</strong>
 * This class is designed to support mod development scenarios where named ID
 * mappings need to be accessible for modification after their initial creation.
 * By using the {@link DefrostingClassLoader}, the class definition can be
 * "defrosted" and made available for additional bytecode modifications.
 * 
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * public class MyItemParser extends NonFreezingNamedIdParser {
 *     @Override
 *     protected String getNamesClassName() {
 *         return "com.example.mod.ItemNames";
 *     }
 *     
 *     public String getItemName(int itemId) {
 *         return getNameForId(itemId);
 *     }
 * }
 * }</pre>
 * 
 * <p><strong>Lifecycle:</strong>
 * Instances of this class follow the standard object lifecycle. The class
 * loading occurs during the first call to {@link #getNamesClass()}, typically
 * triggered by methods in the parent {@link NamedIdParser} class.
 * 
 * <p><strong>Thread Safety:</strong>
 * This implementation is thread-safe assuming the subclass implementation of
 * {@link #getNamesClassName()} is thread-safe. The {@link DefrostingClassLoader}
 * usage is contained within a try-with-resources block ensuring proper cleanup.
 * 
 * @since 1.0.0
 * @see NamedIdParser
 * @see DefrostingClassLoader
 * @see HookManager
 */
public abstract class NonFreezingNamedIdParser extends NamedIdParser {
	
	/**
	 * Gets the fully qualified name of the class that contains the name mappings.
	 * 
	 * <p>This method must be implemented by subclasses to provide the class name
	 * of the generated class that holds the ID-to-name mappings. The class will
	 * be loaded using a {@link DefrostingClassLoader} which allows it to be
	 * modified after loading.
	 * 
	 * @return the fully qualified class name containing the name mappings
	 * @since 1.0.0
	 * @see #getNamesClass()
	 */
	protected abstract String getNamesClassName();
	
	/**
	 * {@inheritDoc}
	 * 
	 * <p>This implementation uses a {@link DefrostingClassLoader} to load the
	 * names class, allowing it to be defrosted and available for further
	 * modifications. The classloader is automatically closed after use.
	 * 
	 * <p><strong>Thread Safety:</strong> This method is thread-safe as it creates
	 * a new {@link DefrostingClassLoader} instance for each invocation.
	 * 
	 * @return the loaded class containing the name mappings
	 * @throws IllegalStateException if the class cannot be loaded due to
	 *         {@link ClassNotFoundException}, {@link NotFoundException}, or
	 *         {@link CannotCompileException}
	 * @since 1.0.0
	 * @see DefrostingClassLoader
	 * @see HookManager#getInstance()
	 */
	@Override
	protected final Class<?> getNamesClass() {
		try (DefrostingClassLoader classLoader =  new DefrostingClassLoader(HookManager.getInstance().getClassPool())) {
			return classLoader.loadClass(getNamesClassName());
		} catch (ClassNotFoundException | NotFoundException | CannotCompileException e) {
			throw new IllegalStateException(e);
		}
	}
}