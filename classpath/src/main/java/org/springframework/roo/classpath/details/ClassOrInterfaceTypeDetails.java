package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;

/**
 * Provides information about the different components of a class or interface.
 * 
 * <p>
 * For simplicity of implementation this is not a complete representation of all members and other
 * information available via Java bytecode. For example, static initialisers and inner classes
 * are unsupported.
 * 
 * @author Ben Alexextends
 * @since 1.0
 *
 */
public interface ClassOrInterfaceTypeDetails extends MemberHoldingTypeDetails {
	
	/**
	 * Obtains the superclass if this is a class and it is available.
	 * 
	 * @return the superclass, if available (null will be returned for interfaces, or if the class isn't available)
	 */
	ClassOrInterfaceTypeDetails getSuperclass();
	
	/**
	 * Obtains the modifiers, in a format consistent with {@link Modifier}.
	 * 
	 * @return the modifiers (zero means no modifiers)
	 */
	int getModifier();
	
	/**
	 * Obtains the physical type identifier that included this {@link ClassOrInterfaceTypeDetails}.
	 * 
	 * @return the physical type identifier (never null)
	 */
	String getDeclaredByMetadataId();
}
