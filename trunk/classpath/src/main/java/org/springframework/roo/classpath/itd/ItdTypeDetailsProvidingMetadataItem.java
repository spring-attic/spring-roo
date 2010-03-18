package org.springframework.roo.classpath.itd;

import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.metadata.MetadataItem;

/**
 * Indicates a {@link MetadataItem} implementation that can provide {@link ItdTypeDetails}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface ItdTypeDetailsProvidingMetadataItem extends MetadataItem {
	/**
	 * Obtains the {@link ItdTypeDetails}, if available.
	 * 
	 * <p>
	 * An {@link ItdTypeDetails} should be returned even if no members should be introduced. Only return
	 * null if there was a failure during parsing or other unexpected condition.
	 * 
	 * @return the details, or null if the details are unavailable or no ITD is required
	 */
	ItdTypeDetails getItdTypeDetails();

}
