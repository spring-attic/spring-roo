package org.springframework.roo.addon.layers.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.layers.LayerCustomDataKeys;
import org.springframework.roo.project.layers.LayerService;
import org.springframework.roo.project.layers.LayerType;
import org.springframework.roo.project.layers.LayerUtils;
import org.springframework.roo.project.layers.MemberTypeAdditions;
import org.springframework.roo.support.util.Pair;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
@Component(immediate=true)
@Service
public class ServiceClassMetadataProvider extends AbstractMemberDiscoveringItdMetadataProvider {
	
	// Constants
	private static final int LAYER_POSITION = LayerType.SERVICE.getPosition();
	private static final Path SRC = Path.SRC_MAIN_JAVA;
	private static final String FIND_ALL_METHOD = PersistenceCustomDataKeys.FIND_ALL_METHOD.name();
	private static final String PERSIST_METHOD = PersistenceCustomDataKeys.PERSIST_METHOD.name();
	private static final String MERGE_METHOD = PersistenceCustomDataKeys.MERGE_METHOD.name();
	private Map<JavaType, String> managedEntityTypes = new HashMap<JavaType, String>();
	
	// Fields
	@Reference private LayerService layerService;
	
	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		setIgnoreTriggerAnnotations(true);
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
	}
	
	@Override
	protected String getLocalMidToRequest(ItdTypeDetails itdTypeDetails) {
		// Determine the governor for this ITD, and whether any metadata is even hoping to hear about changes to that JavaType and its ITDs
		JavaType governor = itdTypeDetails.getName();
		String localMid = managedEntityTypes.get(governor);
		if (localMid != null) {
			return localMid;
		}
		
		//TODO: review need for member details scanning to pick up newly added tags (ideally these should be added automatically during MD processing; 
		MemberDetails details = memberDetailsScanner.getMemberDetails(getClass().getName(), itdTypeDetails.getGovernor());
		MemberHoldingTypeDetails memberHoldingTypeDetails = MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(details, LayerCustomDataKeys.LAYER_TYPE);
		if (memberHoldingTypeDetails != null) {
			List<JavaType> domainTypes = (List<JavaType>) memberHoldingTypeDetails.getCustomData().get(LayerCustomDataKeys.LAYER_TYPE);
			if (domainTypes != null) {
				for (JavaType type : domainTypes) {
					String localMidType = managedEntityTypes.get(type);
					if (localMidType != null) {
						return localMidType;
					}
				}
			}
		}
		return null;	
	}
	
	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		ClassOrInterfaceTypeDetails coitd = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		if (coitd == null) {
			return null;
		}
		ServiceInterfaceMetadata serviceInterfaceMetadata = null;
		for (JavaType type : coitd.getImplementsTypes()) {
			if ((serviceInterfaceMetadata = (ServiceInterfaceMetadata) metadataService.get(ServiceInterfaceMetadata.createIdentifier(type, SRC))) != null) {
				break;
			}
		}
		if (serviceInterfaceMetadata == null || !serviceInterfaceMetadata.isValid()) {
			return null;
		}
		metadataDependencyRegistry.registerDependency(serviceInterfaceMetadata.getId(), metadataIdentificationString);
		ServiceAnnotationValues serviceAnnotationValues = serviceInterfaceMetadata.getServiceAnnotationValues();
		JavaType[] domainTypes = serviceAnnotationValues.getDomainTypes();
		if (domainTypes == null) {
			return null;
		}
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(getClass().getName(), coitd);
		
		Map<JavaType, String> domainTypePlurals = new HashMap<JavaType, String>();
		Map<JavaType,Map<String, MemberTypeAdditions>> allCrudAdditions = new HashMap<JavaType,Map<String, MemberTypeAdditions>>();
		for (JavaType domainType : domainTypes) {
			metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.createIdentifier(domainType, SRC), metadataIdentificationString);
			Map<String, MemberTypeAdditions> methodAdditions = new HashMap<String, MemberTypeAdditions>();
			methodAdditions.put(FIND_ALL_METHOD, layerService.getMemberTypeAdditions(metadataIdentificationString, FIND_ALL_METHOD, domainType, LAYER_POSITION));
			methodAdditions.put(PERSIST_METHOD, layerService.getMemberTypeAdditions(metadataIdentificationString, PERSIST_METHOD, domainType, LAYER_POSITION, new Pair<JavaType, JavaSymbolName>(domainType, LayerUtils.getTypeName(domainType))));
			methodAdditions.put(MERGE_METHOD, layerService.getMemberTypeAdditions(metadataIdentificationString, MERGE_METHOD, domainType, LAYER_POSITION, new Pair<JavaType, JavaSymbolName>(domainType, LayerUtils.getTypeName(domainType))));

			allCrudAdditions.put(domainType, methodAdditions);
			
			String pluralId = PluralMetadata.createIdentifier(domainType, Path.SRC_MAIN_JAVA);
			PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(pluralId);
			if (pluralMetadata == null) {
				return null;
			}
			metadataDependencyRegistry.registerDependency(pluralId, metadataIdentificationString);
			domainTypePlurals.put(domainType, pluralMetadata.getPlural());
			
			// maintain a list of entities that are being handled by this layer
			managedEntityTypes.put(domainType, metadataIdentificationString);
		}
		return new ServiceClassMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, memberDetails, serviceAnnotationValues, allCrudAdditions, domainTypePlurals);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "Service";
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
