package org.springframework.roo.classpath;

// TODO: Consider whether this should be an operations object, not an extension of PhysicalTypeMetadataProvider
/**
 * Extends {@link PhysicalTypeMetadataProvider} to include mutable operations.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface MutablePhysicalTypeMetadataProvider extends PhysicalTypeMetadataProvider {

	/**
	 * Creates the physical type on the disk, with the structure shown.
	 * 
	 * <p>
	 * An implementation is not required to support all of the constructs in the presented {@link PhysicalTypeMetadata}.
	 * An implementation must throw an exception if it cannot create the presented type.
	 * 
	 * <p>
	 * An implementation may merge the contents with an existing file, if the type already exists.
	 * 
	 * @param toCreate to create (required)
	 */
	void createPhysicalType(PhysicalTypeMetadata toCreate);
}
