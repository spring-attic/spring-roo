package org.springframework.roo.project.packaging;

/**
 * A registry for {@link PackagingProvider}s.
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public interface PackagingProviderRegistry {
	
	/**
	 * Returns the {@link PackagingProvider} to be used when the user doesn't
	 * specify one.
	 * 
	 * @return a non-<code>null</code> instance
	 */
	PackagingProvider getDefaultPackagingProvider();
}