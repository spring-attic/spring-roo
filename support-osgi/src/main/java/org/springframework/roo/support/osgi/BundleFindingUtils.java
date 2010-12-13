package org.springframework.roo.support.osgi;

import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.roo.support.util.Assert;

/**
 * Helper to locate bundle symbolic names used by Spring Roo.
 * 
 * @author Ben Alex
 * @since 1.1.1
 *
 */
public abstract class BundleFindingUtils {
	
	/**
	 * Locates the first bundle that contains the presented type name and return its
	 * bundle symbolic name. 
	 * 
	 * @param context that can be used to obtain bundles to search (required)
	 * @param typeNameInExternalForm a type name (eg com.foo.Bar)
	 * @return the bundle symbolic name, if found (or null if not found)
	 */
	public static String findFirstBundleForTypeName(BundleContext context, String typeNameInExternalForm) {
		Assert.notNull(context, "Bundle context required to perform the search");
		Assert.hasText(typeNameInExternalForm, "Resource name to locate is required");
		String resourceName = "/" + typeNameInExternalForm.replace('.', '/') + ".class";

		Bundle[] bundles = context.getBundles();
		if (bundles == null) {
			return null;
		}
		
		for (Bundle bundle : bundles) {
			try {
				URL url = bundle.getEntry(resourceName);
				if (url != null) {
					return bundle.getSymbolicName();
				}
			} catch (RuntimeException e) {
				return null;
			}
		}
		
		return null;
	}

	/**
	 * Locates the first bundle that contains the presented type name and return
	 * that class.
	 * 
	 * @param context that can be used to obtain bundles to search (required)
	 * @param typeNameInExternalForm a type name (eg com.foo.Bar)
	 * @return the class, if found (or null if not found)
	 */
	public static Class<?> findFirstBundleWithType(BundleContext context, String typeNameInExternalForm) {
		Assert.notNull(context, "Bundle context required to perform the search");
		Assert.hasText(typeNameInExternalForm, "Resource name to locate is required");
		String resourceName = "/" + typeNameInExternalForm.replace('.', '/') + ".class";
	
		Bundle[] bundles = context.getBundles();
		if (bundles == null) {
			return null;
		}
		
		for (Bundle bundle : bundles) {
			try {
				URL url = bundle.getEntry(resourceName);
				if (url != null) {
					return bundle.loadClass(typeNameInExternalForm);
				}
			} catch (Throwable e) {
				return null;
			}
		}
		
		return null;
	}
}
