package org.springframework.roo.classpath;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;

/**
 * Allows filtering of {@link ClassOrInterfaceTypeDetails}, for example by the
 * {@link TypeLocationService}.
 *
 * @author Andrew Swan
 * @since 1.2
 */
public interface ClassOrInterfaceTypeDetailsFilter {

	/**
	 * Indicates whether to include the given type in the filtered results
	 * 
	 * @param type the type to evaluate; can be <code>null</code>
	 * @return <code>false</code> to exclude the given type
	 */
	boolean include(ClassOrInterfaceTypeDetails type);
}