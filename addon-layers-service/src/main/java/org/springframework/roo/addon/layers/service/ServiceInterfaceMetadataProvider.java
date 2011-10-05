package org.springframework.roo.addon.layers.service;

import static org.springframework.roo.model.RooJavaType.ROO_SERVICE;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerTypeMatcher;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataProvider;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * {@link MetadataProvider} providing {@link ServiceInterfaceMetadata}
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component(immediate = true)
@Service
public class ServiceInterfaceMetadataProvider extends AbstractMemberDiscoveringItdMetadataProvider {

	// Fields
	@Reference private CustomDataKeyDecorator customDataKeyDecorator;
	@Reference private TypeLocationService typeLocationService;
	
	private Map<JavaType, String> managedEntityTypes = new HashMap<JavaType, String>();
	
	@SuppressWarnings("unchecked")
	protected void activate(ComponentContext context) {
		super.setDependsOnGovernorBeingAClass(false);
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_SERVICE);
		customDataKeyDecorator.registerMatchers(getClass(), new LayerTypeMatcher(CustomDataKeys.LAYER_TYPE, ROO_SERVICE, new JavaSymbolName(RooService.DOMAIN_TYPES_ATTRIBUTE), ROO_SERVICE));
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_SERVICE);
	}
	
	@Override
	protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
		// Determine the governor for this ITD, and whether any metadata is even hoping to hear about changes to that JavaType and its ITDs
		final JavaType governor = itdTypeDetails.getName();
		final String localMid = managedEntityTypes.get(governor);
		if (localMid != null) {
			return localMid;
		}
		
		final MemberHoldingTypeDetails memberHoldingTypeDetails = typeLocationService.findClassOrInterface(governor);
		if (memberHoldingTypeDetails != null) {
			for (final JavaType type : memberHoldingTypeDetails.getLayerEntities()) {
				final String localMidType = managedEntityTypes.get(type);
				if (localMidType != null) {
					return localMidType;
				}
			}
		}
		return null;	
	}
	
	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		ServiceAnnotationValues annotationValues = new ServiceAnnotationValues(governorPhysicalTypeMetadata);
		ClassOrInterfaceTypeDetails coitd = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		if (coitd == null) {
			return null;
		}
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(getClass().getName(), coitd);
		JavaType[] domainTypes = annotationValues.getDomainTypes();
		if (domainTypes == null || domainTypes.length == 0) {
			return null;
		}
		Map<JavaType, String> domainTypePlurals = new HashMap<JavaType, String>();
		Map<JavaType, JavaType> domainTypeToIdTypeMap = new HashMap<JavaType, JavaType>();
		for (JavaType type : domainTypes) {
			final JavaType idType = persistenceMemberLocator.getIdentifierType(type);
			if (idType == null) {
				continue;
			}
			// We simply take the first disregarding any further fields which may be identifiers
			domainTypeToIdTypeMap.put(type, idType);
			String pluralId = PluralMetadata.createIdentifier(type);
			PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(pluralId);
			if (pluralMetadata == null) {
				return null;
			}
			// Maintain a list of entities that are being handled by this layer
			managedEntityTypes.put(type, metadataIdentificationString);
			metadataDependencyRegistry.registerDependency(pluralId, metadataIdentificationString);
			domainTypePlurals.put(type, pluralMetadata.getPlural());
		}
		
		return new ServiceInterfaceMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, memberDetails, domainTypeToIdTypeMap, annotationValues, domainTypePlurals);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "Service";
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
