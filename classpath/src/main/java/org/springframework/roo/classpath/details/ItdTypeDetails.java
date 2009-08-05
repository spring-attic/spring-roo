package org.springframework.roo.classpath.details;

import java.util.Set;

import org.springframework.roo.model.JavaType;

/**
 * Provides information about an ITD.
 * 
 * <p>
 * For simplicity of implementation this is not a complete representation of all members and other
 * information available via Java bytecode. For example, static initialisers and inner classes
 * are unsupported.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface ItdTypeDetails extends MemberHoldingTypeDetails {
	
	boolean isPrivilegedAspect();
	
	/**
	 * Returns the name of type which holds the aspect itself.
	 * 
	 * <p>
	 * Note that the type receiving the introductions can be determined via {@link #getName()}.
	 * 
	 * @return the aspect {@link JavaType} (never null)
	 */
	JavaType getAspect();
	
	/**
	 * @return the explicitly-registered imports this user wishes to have defined in the ITD (cannot be null, but may be empty)
	 */
	Set<JavaType> getRegisteredImports();
	
}
