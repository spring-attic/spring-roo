package org.springframework.roo.classpath.itd;

import java.util.Set;

import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.metadata.MetadataItem;

/**
 * Scans all {@link ItdMetadataProvider}s and locates every {@link MetadataItem} related to a particular
 * {@link PhysicalTypeIdentifier} (as indicated by its metadata identification string).
 * 
 * <p>
 * This interface is very useful for any facilities which need to discover any potentially-introduced members.
 * 
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface ItdMetadataScanner {

	/**
	 * Locates identifiers for all metadata that, at the time of the invocation, provided a non-null {@link MetadataItem}
	 * compatible with the physical type identifier. This will include the original {@link PhysicalTypeMetadata}.
	 *  
	 * @param physicalTypeIdentifier to locate (required)
	 * @return a set of identifiers related to individual metadata instances (may be empty, but never null)
	 */
	Set<MetadataItem> getMetadata(String physicalTypeIdentifier);
	
}
