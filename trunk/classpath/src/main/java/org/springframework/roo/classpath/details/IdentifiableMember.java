package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;

/**
 * Allows a member to be traced back to its declaring type.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface IdentifiableMember {

	/**
	 * @return the ID of the metadata that declared this member (never null)
	 */
	String getDeclaredByMetadataId();

	/**
	 * Indicates the access modifier of the member. The integer is formatted in accordance with
	 * {@link Modifier}.
	 * 
	 * @return the modifier, if available (required) 
	 */
	int getModifier();
	
}
