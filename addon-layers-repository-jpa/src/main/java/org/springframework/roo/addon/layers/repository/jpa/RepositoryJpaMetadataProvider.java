package org.springframework.roo.addon.layers.repository.jpa;

import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_JPA;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerTypeMatcher;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
/**
 * Provides the metadata for an ITD that implements a Spring Data JPA repository
 *
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component(immediate = true)
@Service
public class RepositoryJpaMetadataProvider extends AbstractItdMetadataProvider {

	// Fields
	@Reference private CustomDataKeyDecorator customDataKeyDecorator;

	@SuppressWarnings("unchecked")
	protected void activate(final ComponentContext context) {
		super.setDependsOnGovernorBeingAClass(false);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_REPOSITORY_JPA);
		customDataKeyDecorator.registerMatchers(getClass(), new LayerTypeMatcher(ROO_REPOSITORY_JPA, new JavaSymbolName(RooJpaRepository.DOMAIN_TYPE_ATTRIBUTE)));
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_REPOSITORY_JPA);
		customDataKeyDecorator.unregisterMatchers(getClass());
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String metadataId, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
		final RepositoryJpaAnnotationValues annotationValues = new RepositoryJpaAnnotationValues(governorPhysicalTypeMetadata);
		final ClassOrInterfaceTypeDetails coitd = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		if (coitd == null) {
			return null;
		}
		final JavaType domainType = annotationValues.getDomainType();
		if (domainType == null) {
			return null;
		}
		final JavaType idType = persistenceMemberLocator.getIdentifierType(domainType);
		if (idType == null) {
			return null;
		}
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.createIdentifier(domainType, Path.SRC_MAIN_JAVA), metadataId);
		return new RepositoryJpaMetadata(metadataId, aspectName, governorPhysicalTypeMetadata, idType, annotationValues);
	}

	public String getItdUniquenessFilenameSuffix() {
		return "Jpa_Repository";
	}

	public String getProvidesType() {
		return RepositoryJpaMetadata.getMetadataIdentiferType();
	}

	@Override
	protected String createLocalIdentifier(final JavaType javaType, final Path path) {
		return RepositoryJpaMetadata.createIdentifier(javaType, path);
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
		final JavaType javaType = RepositoryJpaMetadata.getJavaType(metadataIdentificationString);
		final Path path = RepositoryJpaMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}
}
