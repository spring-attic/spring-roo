package org.springframework.roo.addon.layers.service;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.project.Path;

/**
 * Factory for {@link ServiceAnnotationValues} instances.
 *
 * @author Andrew Swan
 * @since 1.2
 */
@Component
@Service
public class ServiceAnnotationValuesFactoryImpl implements ServiceAnnotationValuesFactory {
	
	// Fields
	@Reference private MetadataService metadataService;
	
	public ServiceAnnotationValues getInstance(final ClassOrInterfaceTypeDetails serviceInterface) {
		final PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(serviceInterface.getName(), Path.SRC_MAIN_JAVA));
		if (physicalTypeMetadata == null) {
			return null;
		}
		return new ServiceAnnotationValues(physicalTypeMetadata);
	}
}
