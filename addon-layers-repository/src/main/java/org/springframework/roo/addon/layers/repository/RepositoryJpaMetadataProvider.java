package org.springframework.roo.addon.layers.repository;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.layers.LayerProvider;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
@Component(immediate=true)
@Service
public class RepositoryJpaMetadataProvider extends AbstractItdMetadataProvider {
	
	protected void activate(ComponentContext context) {
		super.setDependsOnGovernorBeingAClass(false);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooRepositoryJpa.class.getName()));
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(new JavaType(RooRepositoryJpa.class.getName()));
	}
	
	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		RepositoryJpaAnnotationValues annotationValues = new RepositoryJpaAnnotationValues(governorPhysicalTypeMetadata);
		ClassOrInterfaceTypeDetails coitd = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		if (coitd == null) {
			return null;
		}
		JavaType domainType = annotationValues.getDomainType();
		if (domainType == null) {
			return null;
		}
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.createIdentifier(domainType, Path.SRC_MAIN_JAVA), metadataIdentificationString);
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(RepositoryJpaMetadataProvider.class.getName(), coitd);
		return new RepositoryJpaMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, memberDetails, annotationValues);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "Jpa_Repository";
	}

	public String getProvidesType() {
		return RepositoryJpaMetadata.getMetadataIdentiferType();
	}

	@Override
	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return RepositoryJpaMetadata.createIdentifier(javaType, path);
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = RepositoryJpaMetadata.getJavaType(metadataIdentificationString);
		Path path = RepositoryJpaMetadata.getPath(metadataIdentificationString);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}
}
