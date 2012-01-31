package org.springframework.roo.addon.layers.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultPhysicalTypeMetadata;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * Locates interfaces annotated with {@link RooService} that meet certain
 * criteria. Factored out of {@link ServiceLayerProvider} to simplify unit
 * testing of that class.
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class ServiceInterfaceLocatorImpl implements ServiceInterfaceLocator {

    @Reference private TypeLocationService typeLocationService;

    public Collection<ClassOrInterfaceTypeDetails> getServiceInterfaces(
            final JavaType domainType) {
        final Set<ClassOrInterfaceTypeDetails> located = typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_SERVICE);
        final Map<String, ClassOrInterfaceTypeDetails> toReturn = new HashMap<String, ClassOrInterfaceTypeDetails>();
        for (final ClassOrInterfaceTypeDetails cid : located) {
            final ServiceAnnotationValues annotationValues = new ServiceAnnotationValues(
                    new DefaultPhysicalTypeMetadata(
                            cid.getDeclaredByMetadataId(),
                            typeLocationService
                                    .getPhysicalTypeCanonicalPath(cid
                                            .getDeclaredByMetadataId()), cid));
            for (final JavaType javaType : annotationValues.getDomainTypes()) {
                if (javaType != null && javaType.equals(domainType)) {
                    toReturn.put(cid.getDeclaredByMetadataId(), cid);
                }
            }
        }
        return toReturn.values();
    }
}