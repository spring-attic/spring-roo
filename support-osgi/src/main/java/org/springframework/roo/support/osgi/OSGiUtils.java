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
 * Utility methods relating to OSGi
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public final class OSGiUtils {

	/**
	 * The root path within an OSGi bundle
	 */
	public static final String ROOT_PATH = "/";
	
	private static final PathMatcher PATH_MATCHER = new AntPathMatcher();
	
	/**
	 * Searches the bundles in the given context for entries with the given path.
	 * 
	 * @param context that can be used to obtain bundles to search (can be <code>null</code>)
	 * @param path the path of the resource to locate (as per {@link Bundle#getEntry},
	 * e.g. "/foo.txt" will find foo.txt in the root of each bundle)
	 * @return null if there was a failure or a set containing zero or more entries (zero entries
	 * means the search was successful but the resource was simply not found)
	 */
	public static final Set<URI> findEntriesByPath(final BundleContext context, final String path) {
		Assert.hasText(path, "Path to locate is required");
		final Set<URI> results = new HashSet<URI>();
		OSGiUtils.execute(
				new BundleCallback() {
					public void execute(final Bundle bundle) {
						try {
							final URL url = bundle.getEntry(path);
							if (url != null) {
								results.add(url.toURI());
							}
						} catch (final IllegalStateException e) {
							// The bundle has been uninstalled - ignore it
						} catch (final URISyntaxException e) {
							// The URL can't be converted to a URI - ignore it
						}
					}
				},
				context
		);
		
		return results;
	}

	/**
	 * Returns the URIs of any entries among the given bundles whose URLs match
	 * the given Ant-style path.
	 * 
	 * @param context the context whose bundles to search (can be <code>null</code>)
	 * @param antPathExpression the pattern for matching URLs against (required)
	 * @return <code>null</code> if the search can't be performed, otherwise a
	 * non-<code>null</code> Set
	 * @see AntPathMatcher#match(String, String)
	 */
	@SuppressWarnings("unchecked")
	public static final Set<URI> findEntriesByPattern(final BundleContext context, final String antPathExpression) {
		Assert.hasText(antPathExpression, "Ant path expression to match is required");
		final Set<URI> results = new HashSet<URI>();
		OSGiUtils.execute(
				new BundleCallback() {
					public void execute(final Bundle bundle) {
						try {
							final Enumeration<URL> enumeration = bundle.findEntries(ROOT_PATH, "*", true);
							if (enumeration != null) {
								while (enumeration.hasMoreElements()) {
									final URL url = enumeration.nextElement();
									if (PATH_MATCHER.match(antPathExpression, url.getPath())) {
										try {
											results.add(url.toURI());
										} catch (URISyntaxException e) {
											// This URL can't be converted to a URI - ignore it
										}
									}
								}
							}
						} catch (final IllegalStateException e) {
							// The bundle has been uninstalled - ignore it
						}
					}
				},
				context
		);
		return results;
	}
	
	/**
	 * Executes the given callback on any bundles in the given context
	 * 
	 * @param callback can be <code>null</code> to do nothing
	 * @param context can be <code>null</code> to do nothing
	 */
	public static void execute(final BundleCallback callback, final BundleContext context) {
		if (callback == null || context == null) {
			return;
		}
		final Bundle[] bundles = context.getBundles();
		if (bundles == null) {
			return;
		}
		for (final Bundle bundle : bundles) {
			callback.execute(bundle);
		}
	}
}
