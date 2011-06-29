package org.springframework.roo.support.osgi;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.roo.support.ant.AntPathMatcher;
import org.springframework.roo.support.ant.PathMatcher;
import org.springframework.roo.support.util.Assert;

/**
 * Utility methods for finding resources.
 *
 * @author Ben Alex
 * @author Andrew Swan
 */
public final class UrlFindingUtils {
	
	// Constants
	private static final PathMatcher PATH_MATCHER = new AntPathMatcher();

	/**
	 * Locates {@link URI}s that represent a search of all bundles for a given
	 * resource. Doesn't return a Set of URLs because {@link URL#equals(Object)}
	 * has known issues with correctness and performance.
	 * 
	 * @param context that can be used to obtain bundles to search (required)
	 * @param resourceName the resource to locate (eg "/foo.txt" will find
	 * foo.txt in the root of each bundle)
	 * @return null if there was a failure or a set containing zero or more
	 * entries (zero entries means the search was successful but the resource
	 * was simply not found)
	 */
	public static Set<URI> findResource(BundleContext context, String resourceName) {
		Assert.notNull(context, "Bundle context required to perform the search");
		Assert.hasText(resourceName, "Resource name to locate is required");
		final Bundle[] bundles = context.getBundles();
		if (bundles == null) {
			return null;
		}
		
		final Set<URI> results = new HashSet<URI>();
		for (final Bundle bundle : bundles) {
			try {
				final URL url = bundle.getEntry(resourceName);
				if (url != null) {
					results.add(url.toURI());
				}
			} catch (RuntimeException e) {
				return null;
			} catch (URISyntaxException e) {
				throw new IllegalStateException(e);
			}
		}
		
		return results;
	}
	
	@SuppressWarnings("unchecked")
	public static Set<URI> findMatchingClasspathResources(BundleContext context, String antPathExpression) {
		Assert.notNull(context, "Bundle context required to perform the search");
		Assert.hasText(antPathExpression, "Ant path expression to match is required");
		final Bundle[] bundles = context.getBundles();
		if (bundles == null) {
			Thread.dumpStack();
			return null;
		}
		
		final Set<URI> results = new HashSet<URI>();
		for (final Bundle bundle : bundles) {
			try {
				final Enumeration<URL> entries = bundle.findEntries("/", "*", true);
				if (entries == null) {
					continue;
				}
				while (entries.hasMoreElements()) {
					final URL url = entries.nextElement();
					final String candidatePath = url.getPath();
					if (PATH_MATCHER.match(antPathExpression, candidatePath)) {
						results.add(url.toURI());
					}
				}
			} catch (RuntimeException e) {
				return null;
			} catch (URISyntaxException e) {
				throw new IllegalStateException(e);
			}
		}
		
		return results;
	}
}
