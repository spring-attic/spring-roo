package org.springframework.roo.addon.layers.service;

import java.util.HashMap;
import java.util.Map;

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
import org.springframework.roo.project.layers.CrudKey;
import org.springframework.roo.project.layers.LayerService;
import org.springframework.roo.project.layers.LayerType;
import org.springframework.roo.project.layers.MemberTypeAdditions;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.StringUtils;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
@Component(immediate=true)
@Service
public class ServiceClassMetadataProvider extends AbstractItdMetadataProvider {
	
	@Reference private LayerService layerService;
	
	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooService.class.getName()));
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(new JavaType(RooService.class.getName()));
	}
	
	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		ServiceAnnotationValues annotationValues = new ServiceAnnotationValues(governorPhysicalTypeMetadata);
		ClassOrInterfaceTypeDetails coitd = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		if (coitd == null) {
			return null;
		}
		JavaType[] domainTypes = annotationValues.getDomainTypes();
		if (domainTypes == null) {
			return null;
		}
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(ServiceClassMetadataProvider.class.getName(), coitd);
		Map<JavaType,Map<CrudKey, MemberTypeAdditions>> allCrudAdditions = new HashMap<JavaType,Map<CrudKey,MemberTypeAdditions>>();
		for (JavaType domainType : annotationValues.getDomainTypes()) {
			metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.createIdentifier(domainType, Path.SRC_MAIN_JAVA), metadataIdentificationString);
			allCrudAdditions.put(domainType, layerService.collectMemberTypeAdditions(metadataIdentificationString, new JavaSymbolName(StringUtils.uncapitalize(domainType.getSimpleTypeName())), domainType, LayerType.SERVICE.getPosition()));
		}
		return new ServiceClassMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, memberDetails, annotationValues, allCrudAdditions);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "Service_Class";
	}

	public String getProvidesType() {
		return ServiceClassMetadata.getMetadataIdentiferType();
	}

	@Override
	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return ServiceClassMetadata.createIdentifier(javaType, path);
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = ServiceClassMetadata.getJavaType(metadataIdentificationString);
		Path path = ServiceClassMetadata.getPath(metadataIdentificationString);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}
}
