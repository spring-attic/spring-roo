package org.springframework.roo.addon.layers.service;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
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

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
@Component(immediate=true)
@Service
public class ServiceInterfaceMetadataProvider extends AbstractItdMetadataProvider {
	
	protected void activate(ComponentContext context) {
		super.setDependsOnGovernorBeingAClass(false);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooService.class.getName()));
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(new JavaType(RooService.class.getName()));
	}
	
	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		ClassOrInterfaceTypeDetails classGovernor = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		List<JavaType> implementsTypes = classGovernor.getImplementsTypes();
		JavaType interfaceType = implementsTypes.get(0);
		PhysicalTypeMetadata interfaceTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(interfaceType, Path.SRC_MAIN_JAVA));
		if (interfaceTypeMetadata == null) {
			return null;
		}
		MemberDetails interfaceMemberDetails = memberDetailsScanner.getMemberDetails(getClass().getName(), (ClassOrInterfaceTypeDetails) interfaceTypeMetadata.getMemberHoldingTypeDetails());

		ServiceAnnotationValues annotationValues = new ServiceAnnotationValues(governorPhysicalTypeMetadata);
		ClassOrInterfaceTypeDetails coitd = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		if (coitd == null) {
			return null;
		}
		JavaType[] domainTypes = annotationValues.getDomainTypes();
		if (domainTypes == null) {
			return null;
		}

		return new ServiceInterfaceMetadata(metadataIdentificationString, aspectName, interfaceTypeMetadata, interfaceMemberDetails, annotationValues);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "Service_Interface";
	}

	public String getProvidesType() {
		return ServiceInterfaceMetadata.getMetadataIdentiferType();
	}

	@Override
	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return ServiceInterfaceMetadata.createIdentifier(javaType, path);
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = ServiceInterfaceMetadata.getJavaType(metadataIdentificationString);
		Path path = ServiceInterfaceMetadata.getPath(metadataIdentificationString);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}
}
