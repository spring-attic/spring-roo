package org.springframework.roo.support.osgi;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

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
	public static Collection<URL> findEntriesByPath(final BundleContext context, final String path) {
		Assert.hasText(path, "Path to locate is required");
		final Collection<URL> urls = new ArrayList<URL>();
		// We use a collection of URIs to avoid duplication in the collection of
		// URLs; we can't simply use a Set of URLs because URL#equals is broken.
		final Collection<URI> uris = new ArrayList<URI>();
		OSGiUtils.execute(
				new BundleCallback() {
					public void execute(final Bundle bundle) {
						try {
							final URL url = bundle.getEntry(path);
							if (url != null) {
								final URI uri = url.toURI();
								if (!uris.contains(uri)) {
									// We haven't seen this URL before; add it
									urls.add(url);
									uris.add(uri);
								}
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
		
		return urls;
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
	public static Collection<URL> findEntriesByPattern(final BundleContext context, final String antPathExpression) {
		Assert.hasText(antPathExpression, "Ant path expression to match is required");
		final Collection<URL> urls = new ArrayList<URL>();
		// We use a collection of URIs to avoid duplication in the collection of
		// URLs; we can't simply use a Set of URLs because URL#equals is broken.
		final Collection<URI> uris = new ArrayList<URI>();
		OSGiUtils.execute(new BundleCallback() {
			public void execute(final Bundle bundle) {
				try {
					final Enumeration<URL> enumeration = bundle.findEntries(ROOT_PATH, "*", true);
					if (enumeration != null) {
						while (enumeration.hasMoreElements()) {
							final URL url = enumeration.nextElement();
							if (PATH_MATCHER.match(antPathExpression, url.getPath())) {
								try {
									final URI uri = url.toURI();
									if (!uris.contains(uri)) {
										urls.add(url);
										uris.add(uri);
									}
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
		}, context);
		return urls;
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
