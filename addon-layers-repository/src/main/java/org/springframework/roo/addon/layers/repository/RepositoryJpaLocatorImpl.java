package org.springframework.roo.addon.layers.repository;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultPhysicalTypeMetadata;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The {@link RepositoryJpaLocator} implementation.
 *
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class RepositoryJpaLocatorImpl implements RepositoryJpaLocator{
	
	// Fields
	@Reference private TypeLocationService typeLocationService;

	private HashMap<JavaType, HashSet<ClassOrInterfaceTypeDetails>> cacheMap = new HashMap<JavaType, HashSet<ClassOrInterfaceTypeDetails>>();

	public Collection<ClassOrInterfaceTypeDetails> getRepositories(JavaType domainType) {
		if (!cacheMap.containsKey(domainType)) {
			cacheMap.put(domainType, new HashSet<ClassOrInterfaceTypeDetails>());
		}
		Set<ClassOrInterfaceTypeDetails> existing = cacheMap.get(domainType);
		Set<ClassOrInterfaceTypeDetails> located = typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_REPOSITORY_JPA);
		if (existing.containsAll(located)) {
			return existing;
		}
		Map<String, ClassOrInterfaceTypeDetails> toReturn = new HashMap<String, ClassOrInterfaceTypeDetails>();
		for (ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails : located) {
			RepositoryJpaAnnotationValues annotationValues = new RepositoryJpaAnnotationValues(new DefaultPhysicalTypeMetadata(classOrInterfaceTypeDetails.getDeclaredByMetadataId(), typeLocationService.getPhysicalTypeCanonicalPath(classOrInterfaceTypeDetails.getDeclaredByMetadataId()), classOrInterfaceTypeDetails));
			if (annotationValues.getDomainType() != null && annotationValues.getDomainType().equals(domainType)) {
				toReturn.put(classOrInterfaceTypeDetails.getDeclaredByMetadataId(), classOrInterfaceTypeDetails);
			}
		}
		return toReturn.values();
	}
}
