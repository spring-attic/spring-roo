package org.springframework.roo.support.osgi;

import org.osgi.framework.Bundle;

/**
 * Callback for operating upon OSGi {@link Bundle}s.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public interface BundleCallback {

    /**
     * Executes this callback on the given OSGi bundle
     * 
     * @param bundle the bundle to operate upon
     */
    void execute(Bundle bundle);
}