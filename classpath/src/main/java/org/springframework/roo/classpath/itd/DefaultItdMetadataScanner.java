package org.springframework.roo.classpath.itd;

import java.util.HashSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataProvider;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.support.util.Assert;

/**
 * Default implementation of {@link ItdMetadataScanner}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Component
@Service
public class DefaultItdMetadataScanner implements ItdMetadataScanner {

	@Reference private MetadataService metadataService;
	
	public Set<MetadataItem> getMetadata(String physicalTypeIdentifier) {
		Assert.isTrue(PhysicalTypeIdentifier.isValid(physicalTypeIdentifier), "Metadata identification string '" + physicalTypeIdentifier + "' is not valid for this metadata provider");
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(physicalTypeIdentifier);
		
		Set<MetadataItem> result = new HashSet<MetadataItem>();
		
		// If this physical type isn't known, just quit
		if (physicalTypeMetadata == null) {
			return result;
		}
		
		// Add the found metadata, as per the interface contract
		result.add(physicalTypeMetadata);
		
		// Iterate over each provider
		Set<MetadataProvider> providers = metadataService.getRegisteredProviders();
		for (MetadataProvider provider : providers) {
			// Skip any provider that isn't an ItdMetadataProvider
			if (!(provider instanceof ItdMetadataProvider)) {
				continue;
			}
			
			// We have a provider, so convert the physical type ID into an ID applicable to that provider
			ItdMetadataProvider itdProvider = (ItdMetadataProvider) provider;
			String itdMetadataId = itdProvider.getIdForPhysicalJavaType(physicalTypeIdentifier);
			
			// Now attempt to locate the corresponding metadata for the ITD ID
			MetadataItem itdMetadataItem = metadataService.get(itdMetadataId);
			
			if (itdMetadataItem != null) {
				result.add(itdMetadataItem);
			}
		}

		return result;
	}

}
