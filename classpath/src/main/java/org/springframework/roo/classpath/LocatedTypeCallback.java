package org.springframework.roo.classpath;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;

/**
 * Callback interface to process a {@link ClassOrInterfaceTypeDetails} type.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface LocatedTypeCallback {
	
	/**
	 * Callback method to process the located {@link ClassOrInterfaceTypeDetails} type.
	 * 
	 * @param located the {@link ClassOrInterfaceTypeDetails} type.
	 */
	 void process(ClassOrInterfaceTypeDetails located);
}
