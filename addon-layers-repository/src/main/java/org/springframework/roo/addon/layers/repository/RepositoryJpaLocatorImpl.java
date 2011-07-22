package org.springframework.roo.addon.layers.repository;

import java.util.Collection;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.CollectionUtils;
import org.springframework.roo.support.util.Filter;

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
		final Set<ClassOrInterfaceTypeDetails> allRepositories = typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(REPOSITORY_ANNOTATION);
		final Filter<ClassOrInterfaceTypeDetails> domainTypeFilter = new Filter<ClassOrInterfaceTypeDetails>() {
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
		return CollectionUtils.filter(allRepositories, domainTypeFilter);
	}
}
