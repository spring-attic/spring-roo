package org.springframework.roo.uaa;

/**
 * Encapsulates the ability to determine if a given bundle symbolic name or type name
 * is part of a "public" feature. This is important in ensuring UAA does not accidentally
 * log details related to non-public features (as this might identify the user, which we
 * do not want to happen).
 * 
 * @author Ben Alex
 * @since 1.1.1
 *
 */
public interface PublicFeatureResolver {

	/**
	 * Indicates whether the presented bundle symbolic name or type name is believed to be a "public"
	 * feature. Both bundle symbolic name and type names are represented as package names with an
	 * optional type name at the end.
	 * 
	 * @param bundleSymbolicNameOrTypeName the type name or bundle name (required)
	 * @return true if the bundle is public, false otherwise
	 */
	boolean isPublic(String bundleSymbolicNameOrTypeName);
}
