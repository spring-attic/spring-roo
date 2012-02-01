package org.springframework.roo.support.osgi;

import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.roo.support.ant.AntPathMatcher;
import org.springframework.roo.support.ant.PathMatcher;

/**
 * Utility methods for locating resources within OSGi bundles.
 */
public final class UrlFindingUtils {

    private static final PathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String ROOT_PATH = "/";

    /**
     * Returns the URLs of any entries among the given bundles whose URLs match
     * the given Ant-style path.
     * 
     * @param context the context whose bundles to search (can be
     *            <code>null</code>)
     * @param antPathExpression the pattern for matching URLs against (required)
     * @return <code>null</code> if the search can't be performed, otherwise a
     *         non-<code>null</code> Set
     * @see AntPathMatcher#match(String, String)
     * @deprecated sets of URLs are slow and unreliable; use
     *             {@link OSGiUtils#findEntriesByPattern(BundleContext, String)}
     *             instead
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static Set<URL> findMatchingClasspathResources(
            final BundleContext context, final String antPathExpression) {
        Validate.notBlank(antPathExpression,
                "Ant path expression to match is required");
        final Set<URL> results = new HashSet<URL>();
        OSGiUtils.execute(new BundleCallback() {
            public void execute(final Bundle bundle) {
                try {
                    final Enumeration<URL> enumeration = bundle.findEntries(
                            ROOT_PATH, "*", true);
                    if (enumeration != null) {
                        while (enumeration.hasMoreElements()) {
                            final URL url = enumeration.nextElement();
                            if (PATH_MATCHER.match(antPathExpression,
                                    url.getPath())) {
                                results.add(url);
                            }
                        }
                    }
                }
                catch (final IllegalStateException e) {
                    // The bundle has been uninstalled - ignore it
                }
            }
        }, context);
        return results;
    }

    /**
     * Searches the bundles in the given context for the given resource.
     * 
     * @param context that can be used to obtain bundles to search (can be
     *            <code>null</code>)
     * @param resourceName the path of the resource to locate (as per
     *            {@link Bundle#getEntry}, e.g. "/foo.txt" will find foo.txt in
     *            the root of each bundle)
     * @return null if there was a failure or a set containing zero or more
     *         entries (zero entries means the search was successful but the
     *         resource was simply not found)
     * @deprecated sets of URLs are slow and unreliable; use
     *             {@link OSGiUtils#findEntriesByPath(BundleContext, String)}
     *             instead
     */
    @Deprecated
    public static Set<URL> findUrls(final BundleContext context,
            final String resourceName) {
        Validate.notBlank(resourceName, "Resource name to locate is required");
        final Set<URL> results = new HashSet<URL>();
        OSGiUtils.execute(new BundleCallback() {
            public void execute(final Bundle bundle) {
                try {
                    final URL url = bundle.getEntry(resourceName);
                    if (url != null) {
                        results.add(url);
                    }
                }
                catch (final IllegalStateException e) {
                    // The bundle has been uninstalled - ignore it
                }
            }
        }, context);
        return results;
    }

    /**
     * Constructor is private to prevent instantiation
     */
    private UrlFindingUtils() {
    }
}