package org.springframework.roo.support.osgi;

import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.roo.support.ant.AntPathMatcher;
import org.springframework.roo.support.ant.PathMatcher;
import org.springframework.roo.support.util.Assert;

public abstract class UrlFindingUtils {

	private static final PathMatcher pathMatcher = new AntPathMatcher();

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
