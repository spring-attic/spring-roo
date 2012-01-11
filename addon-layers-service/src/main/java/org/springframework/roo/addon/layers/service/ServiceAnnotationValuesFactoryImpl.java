package org.springframework.roo.addon.layers.service;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.metadata.MetadataService;

/**
 * Factory for {@link ServiceAnnotationValues} instances.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class ServiceAnnotationValuesFactoryImpl implements
        ServiceAnnotationValuesFactory {

    @Reference private MetadataService metadataService;

    public ServiceAnnotationValues getInstance(
            final ClassOrInterfaceTypeDetails serviceInterface) {
        final PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService
                .get(serviceInterface.getDeclaredByMetadataId());
        if (physicalTypeMetadata == null) {
            return null;
        }
        return new ServiceAnnotationValues(physicalTypeMetadata);
    }
}
