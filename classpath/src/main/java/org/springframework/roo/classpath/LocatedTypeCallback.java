package org.springframework.roo.classpath;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;

/**
 * Callback interface to process a {@link ClassOrInterfaceTypeDetails} type.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface LocatedTypeCallback {
	
	 void process(ClassOrInterfaceTypeDetails located);
}
