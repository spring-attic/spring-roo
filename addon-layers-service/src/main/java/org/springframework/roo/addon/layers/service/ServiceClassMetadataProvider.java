package org.springframework.roo.addon.layers.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerCustomDataKeys;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Pair;

/**
 * Provides {@link ServiceClassMetadata} for building the ITD for the
 * implementation class of a user project's service.
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component(immediate=true)
@Service
public class ServiceClassMetadataProvider extends AbstractMemberDiscoveringItdMetadataProvider {
	
	// Constants
	private static final int LAYER_POSITION = LayerType.SERVICE.getPosition();
	private static final Path SRC = Path.SRC_MAIN_JAVA;
	private Map<JavaType, String> managedEntityTypes = new HashMap<JavaType, String>();
	
	// Fields
	@Reference private LayerService layerService;
	@Reference private PersistenceMemberLocator persistenceMemberLocator;
	
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
			@SuppressWarnings("unchecked")
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
		final ClassOrInterfaceTypeDetails serviceClass = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		if (serviceClass == null) {
			return null;
		}
		ServiceInterfaceMetadata serviceInterfaceMetadata = null;
		for (final JavaType implementedType : serviceClass.getImplementsTypes()) {
			final String implementedTypeId = ServiceInterfaceMetadata.createIdentifier(implementedType, SRC);
			if ((serviceInterfaceMetadata = (ServiceInterfaceMetadata) metadataService.get(implementedTypeId)) != null) {
				// Found the metadata for the service interface
				break;
			}
		}
		if (serviceInterfaceMetadata == null || !serviceInterfaceMetadata.isValid()) {
			return null;
		}
		
		// Register this provider for changes to the service interface // TODO move this down in case we return null early below?
		metadataDependencyRegistry.registerDependency(serviceInterfaceMetadata.getId(), metadataIdentificationString);
		
		final ServiceAnnotationValues serviceAnnotationValues = serviceInterfaceMetadata.getServiceAnnotationValues();
		final JavaType[] domainTypes = serviceAnnotationValues.getDomainTypes();
		if (domainTypes == null) {
			return null;
		}

		/*
		 * For each domain type, collect (1) the plural and (2) the additions to
		 * make to the service class for calling a lower layer when implementing
		 * each service layer method.
		 * We use LinkedHashMaps for the latter nested map to ensure repeatable
		 * order of code generation.
		 */
		final Map<JavaType, String> domainTypePlurals = new HashMap<JavaType, String>();
		final Map<JavaType, JavaType> domainTypeToIdTypeMap = new HashMap<JavaType, JavaType>();
		// Collect the additions for each method for each supported domain type
		final Map<JavaType, Map<ServiceLayerMethod, MemberTypeAdditions>> allCrudAdditions = new LinkedHashMap<JavaType, Map<ServiceLayerMethod, MemberTypeAdditions>>();
		for (final JavaType domainType : domainTypes) {
			
			List<FieldMetadata> idFields = persistenceMemberLocator.getIdentifierFields(domainType);
			if (idFields.isEmpty()) {
				continue;
			}
			JavaType idType = idFields.get(0).getFieldType();
			domainTypeToIdTypeMap.put(domainType, idType);
			// Collect the plural for this domain type
			final String pluralId = PluralMetadata.createIdentifier(domainType, Path.SRC_MAIN_JAVA);
			final PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(pluralId);
			if (pluralMetadata == null) {
				return null;
			}
			domainTypePlurals.put(domainType, pluralMetadata.getPlural());
			
			// maintain a list of entities that are being handled by this layer
			managedEntityTypes.put(domainType, metadataIdentificationString);
			
			// Collect the additions the service class needs in order to invoke each service layer method
			final Map<ServiceLayerMethod, MemberTypeAdditions> methodAdditions = new LinkedHashMap<ServiceLayerMethod, MemberTypeAdditions>();
			for (final ServiceLayerMethod method : ServiceLayerMethod.values()) {
				final Pair<JavaType, JavaSymbolName>[] methodParameters = method.getParameters(domainType, idType).toArray();
				final MemberTypeAdditions memberTypeAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, method.getKey(), domainType, idType, LAYER_POSITION, methodParameters);
				if (memberTypeAdditions != null) {
					// A lower layer implements this method
					methodAdditions.put(method, memberTypeAdditions);
				}
			}
			allCrudAdditions.put(domainType, methodAdditions);
			
			// Register this provider for changes to the domain type or its plural
			metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.createIdentifier(domainType, SRC), metadataIdentificationString);
			metadataDependencyRegistry.registerDependency(pluralId, metadataIdentificationString);
		}
		final MemberDetails serviceClassDetails = memberDetailsScanner.getMemberDetails(getClass().getName(), serviceClass);
		return new ServiceClassMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, serviceClassDetails, serviceAnnotationValues, domainTypeToIdTypeMap, allCrudAdditions, domainTypePlurals);
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
