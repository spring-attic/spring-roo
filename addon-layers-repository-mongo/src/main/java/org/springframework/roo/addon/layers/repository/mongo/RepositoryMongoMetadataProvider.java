package org.springframework.roo.addon.layers.repository.mongo;

import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_MONGO;

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
import org.springframework.roo.classpath.layers.LayerCustomDataKeys;
import org.springframework.roo.classpath.layers.LayerTypeMatcher;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Provides the metadata for an ITD that implements a Spring Data Mongo repository
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component(immediate = true)
@Service
public class RepositoryMongoMetadataProvider extends AbstractItdMetadataProvider {
	
	// Fields
	@Reference private CustomDataKeyDecorator customDataKeyDecorator;
	@Reference private PersistenceMemberLocator persistenceMemberLocator;
	
	@SuppressWarnings("unchecked")
	protected void activate(final ComponentContext context) {
		super.setDependsOnGovernorBeingAClass(false);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_REPOSITORY_MONGO);
		customDataKeyDecorator.registerMatchers(getClass(), new LayerTypeMatcher(LayerCustomDataKeys.LAYER_TYPE, ROO_REPOSITORY_MONGO, new JavaSymbolName(RooRepositoryMongo.DOMAIN_TYPE_ATTRIBUTE), ROO_REPOSITORY_MONGO));
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_REPOSITORY_MONGO);
		customDataKeyDecorator.unregisterMatchers(getClass());
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String metadataId, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
		final RepositoryMongoAnnotationValues annotationValues = new RepositoryMongoAnnotationValues(governorPhysicalTypeMetadata);
		final ClassOrInterfaceTypeDetails coitd = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		final JavaType domainType = annotationValues.getDomainType();
		if(!annotationValues.isAnnotationFound() || coitd == null || domainType == null) {
			System.out.println("null found for " + domainType);
			return null;
		}
		JavaType idType = persistenceMemberLocator.getIdentifierType(domainType);
		if(idType == null) {
			
			System.out.println("no id found for " + domainType);
			return null;
		}
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.createIdentifier(domainType, Path.SRC_MAIN_JAVA), metadataId);
		return new RepositoryMongoMetadata(metadataId, aspectName, governorPhysicalTypeMetadata, idType, annotationValues);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "Mongo_Repository";
	}

	public String getProvidesType() {
		return RepositoryMongoMetadata.getMetadataIdentiferType();
	}

	@Override
	protected String createLocalIdentifier(final JavaType javaType, final Path path) {
		return RepositoryMongoMetadata.createIdentifier(javaType, path);
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
		final JavaType javaType = RepositoryMongoMetadata.getJavaType(metadataIdentificationString);
		final Path path = RepositoryMongoMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}
}
