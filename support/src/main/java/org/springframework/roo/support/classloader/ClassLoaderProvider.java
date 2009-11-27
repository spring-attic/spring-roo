package org.springframework.roo.support.classloader;


/**
 * Implementations offer a convenient way to obtain the current {@link ClassLoader}.
 * 
 * <p>
 * This interface exists to ensure {@link ClassLoader} instances can be obtained in a safe
 * manner. This is particularly important for compatibility with complex environments such
 * as when Roo is embedded in IDEs etc.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface ClassLoaderProvider {
	/**
	 * @return the current {@link ClassLoader} (never null)
	 */
	ClassLoader getClassLoader();
}
