package org.springframework.roo.addon.layers.repository.mongo;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
 * The {@link RepositoryMongoLocator} implementation.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component
@Service
public class RepositoryMongoLocatorImpl implements RepositoryMongoLocator {

    @Reference private TypeLocationService typeLocationService;
    private final Map<JavaType, Set<ClassOrInterfaceTypeDetails>> cacheMap = new HashMap<JavaType, Set<ClassOrInterfaceTypeDetails>>();

    public Collection<ClassOrInterfaceTypeDetails> getRepositories(
            final JavaType domainType) {
        if (!cacheMap.containsKey(domainType)) {
            cacheMap.put(domainType, new HashSet<ClassOrInterfaceTypeDetails>());
        }
        final Set<ClassOrInterfaceTypeDetails> existing = cacheMap
                .get(domainType);
        final Set<ClassOrInterfaceTypeDetails> located = typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_REPOSITORY_MONGO);
        if (existing.containsAll(located)) {
            return existing;
        }
        final Map<String, ClassOrInterfaceTypeDetails> toReturn = new HashMap<String, ClassOrInterfaceTypeDetails>();
        for (final ClassOrInterfaceTypeDetails cid : located) {
            final RepositoryMongoAnnotationValues annotationValues = new RepositoryMongoAnnotationValues(
                    new DefaultPhysicalTypeMetadata(
                            cid.getDeclaredByMetadataId(),
                            typeLocationService
                                    .getPhysicalTypeCanonicalPath(cid
                                            .getDeclaredByMetadataId()), cid));
            if (annotationValues.getDomainType() != null
                    && annotationValues.getDomainType().equals(domainType)) {
                toReturn.put(cid.getDeclaredByMetadataId(), cid);
            }
        }
        return toReturn.values();
    }
}
