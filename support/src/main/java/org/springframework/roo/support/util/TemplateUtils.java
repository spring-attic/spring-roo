package org.springframework.roo.support.util;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.roo.support.ant.AntPathMatcher;
import org.springframework.roo.support.ant.PathMatcher;

/**
 * Utilities for dealing with "templates", which are commonly used by ROO add-ons.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public abstract class TemplateUtils {
	
	private static final PathMatcher pathMatcher = new AntPathMatcher();

	/**
	 * Determines the path to the requested template.
	 * 
	 * @param clazz which owns the template (required)
	 * @param templateFilename the filename of the template (required)
	 * @return the full classloader-specific path to the template (never null)
	 */
	public static final String getTemplatePath(Class<?> clazz, String templateFilename) {
		Assert.notNull(clazz, "Owning class required");
		Assert.hasText(templateFilename, "Template filename required");
		Assert.isTrue(!templateFilename.startsWith("/"), "Template filename shouldn't start with a slash");
		// Slashes instead of File.separatorChar is correct here, as these are classloader paths (not file system paths)
		return "/" + clazz.getPackage().getName().replace('.', '/') + "/" + templateFilename;
	}

	/**
	 * Acquires an {@link InputStream} to the requested classloader-derived template.
	 * 
	 * @param clazz which owns the template (required)
	 * @param templateFilename the filename of the template (required)
	 * @return the input stream (never null; an exception is thrown if cannot be found)
	 */
	public static final InputStream getTemplate(Class<?> clazz, String templateFilename) {
		String templatePath = getTemplatePath(clazz, templateFilename);
		InputStream result = clazz.getResourceAsStream(templatePath);
		Assert.notNull(result, "Could not locate '" + templatePath + "' in classloader");
		return result;
	}
	
	/**
	 * Locates {@link URL}s that represent a search of all bundles for a given resource.
	 * 
	 * @param context that can be used to obtain bundles to search (required)
	 * @param resourceName the resource to locate (eg "/foo.txt" will find foo.txt in the root of each bundle)
	 * @return null if there was a failure or a set containing zero or more entries (zero entries
	 *              means the search was successful but the resource was simply not found)
	 */
	public static final Set<URL> findUrls(BundleContext context, String resourceName) {
		Assert.notNull(context, "Bundle context required to perform the search");
		Assert.hasText(resourceName, "Resource name to locate is required");
		Bundle[] bundles = context.getBundles();
		if (bundles == null) {
			return null;
		}
		
		Set<URL> results = new HashSet<URL>();
		for (Bundle bundle : bundles) {
			try {
				URL url = bundle.getEntry(resourceName);
				if (url != null) {
					results.add(url);
				}
			} catch (RuntimeException e) {
				return null;
			}
		}
		
		return results;
	}
	
	
	@SuppressWarnings("unchecked")
	public static final Set<URL> findMatchingClasspathResources(BundleContext context, String antPathExpression) {
		Assert.notNull(context, "Bundle context required to perform the search");
		Assert.hasText(antPathExpression, "Ant path expression to match is required");
		Bundle[] bundles = context.getBundles();
		if (bundles == null) {
			Thread.dumpStack();
			return null;
		}
		
		Set<URL> results = new HashSet<URL>();
		for (Bundle bundle : bundles) {
			try {
				Enumeration<URL> enumeration = bundle.findEntries("/", "*", true);
				if (enumeration == null) {
					continue;
				}
				while (enumeration.hasMoreElements()) {
					URL url = enumeration.nextElement();
					String candidatePath = url.getPath();
					if (pathMatcher.match(antPathExpression, candidatePath)) {
						results.add(url);
					}
				}
			} catch (RuntimeException e) {
				return null;
			}
		}
		
		return results;
	}
}
