package org.springframework.roo.addon.layers.repository;

import java.util.Collection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.ClassOrInterfaceTypeDetailsFilter;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * The {@link RepositoryJpaLocator} implementation.
 *
 * @author Andrew Swan
 * @since 1.2
 */
@Component
@Service
public class RepositoryJpaLocatorImpl implements RepositoryJpaLocator {
	
	// Constants
	private static final JavaType REPOSITORY_ANNOTATION = new JavaType(RooRepositoryJpa.class.getName());

	// Fields
	@Reference private MetadataService metadataService;
	@Reference private TypeLocationService typeLocationService;

	public Collection<ClassOrInterfaceTypeDetails> getRepositories(final JavaType domainType) {
		final ClassOrInterfaceTypeDetailsFilter domainTypeFilter = new ClassOrInterfaceTypeDetailsFilter() {
			public boolean include(final ClassOrInterfaceTypeDetails repositoryType) {
				final PhysicalTypeMetadata repositoryPhysicalType = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(repositoryType.getName(), Path.SRC_MAIN_JAVA));
				if (repositoryPhysicalType != null) {
					final RepositoryJpaAnnotationValues repositoryJpaAnnotationValues = new RepositoryJpaAnnotationValues(repositoryPhysicalType);
					if (repositoryJpaAnnotationValues.getDomainType().equals(domainType)) {
						return true;
					}
				}
				return false;
			}
		};
		return typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(domainTypeFilter, REPOSITORY_ANNOTATION);
	}
}
