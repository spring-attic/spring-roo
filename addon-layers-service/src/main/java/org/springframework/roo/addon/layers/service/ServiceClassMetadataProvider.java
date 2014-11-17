package org.springframework.roo.addon.layers.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Provides {@link ServiceClassMetadata} for building the ITD for the
 * implementation class of a user project's service.
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class ServiceClassMetadataProvider extends
        AbstractMemberDiscoveringItdMetadataProvider {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(ServiceClassMetadataProvider.class);

    private static final int LAYER_POSITION = LayerType.SERVICE.getPosition();

    ProjectOperations projectOperations;
    FileManager fileManager;
    ServiceLayerTemplateService templateService;

    private LayerService layerService;

    private final Map<JavaType, String> managedEntityTypes = new HashMap<JavaType, String>();

    protected void activate(final ComponentContext cContext) {
    	context = cContext.getBundleContext();
    	getMetadataDependencyRegistry().addNotificationListener(this);
        getMetadataDependencyRegistry().registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        setIgnoreTriggerAnnotations(true);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return ServiceClassMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        getMetadataDependencyRegistry().removeNotificationListener(this);
        getMetadataDependencyRegistry().deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = ServiceClassMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = ServiceClassMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "Service";
    }

    @Override
    protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
        // Determine the governor for this ITD, and whether any metadata is even
        // hoping to hear about changes to that JavaType and its ITDs
        final JavaType governor = itdTypeDetails.getName();
        final String localMid = managedEntityTypes.get(governor);
        if (localMid != null) {
            return localMid;
        }

        final MemberHoldingTypeDetails memberHoldingTypeDetails = getTypeLocationService()
                .getTypeDetails(governor);
        if (memberHoldingTypeDetails != null) {
            for (final JavaType type : memberHoldingTypeDetails
                    .getLayerEntities()) {
                final String localMidType = managedEntityTypes.get(type);
                if (localMidType != null) {
                    return localMidType;
                }
            }
        }
        return null;
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String itdFilename) {
        final ClassOrInterfaceTypeDetails serviceClass = governorPhysicalTypeMetadata
                .getMemberHoldingTypeDetails();
        if (serviceClass == null) {
            return null;
        }

        ServiceInterfaceMetadata serviceInterfaceMetadata = null;
        ClassOrInterfaceTypeDetails serviceInterface = null;
        for (final JavaType implementedType : serviceClass.getImplementsTypes()) {
            final ClassOrInterfaceTypeDetails potentialServiceInterfaceTypeDetails = getTypeLocationService()
                    .getTypeDetails(implementedType);
            if (potentialServiceInterfaceTypeDetails != null) {
                final LogicalPath path = PhysicalTypeIdentifier
                        .getPath(potentialServiceInterfaceTypeDetails
                                .getDeclaredByMetadataId());
                final String implementedTypeId = ServiceInterfaceMetadata
                        .createIdentifier(implementedType, path);
                if ((serviceInterfaceMetadata = (ServiceInterfaceMetadata) getMetadataService()
                        .get(implementedTypeId)) != null) {
                    // Found the metadata for the service interface
                    serviceInterface = potentialServiceInterfaceTypeDetails;
                    break;
                }
            }
        }
        if (serviceInterface == null || serviceInterfaceMetadata == null
                || !serviceInterfaceMetadata.isValid()) {
            return null;
        }

        // Register this provider for changes to the service interface // TODO
        // move this down in case we return null early below?
        getMetadataDependencyRegistry().registerDependency(
                serviceInterfaceMetadata.getId(), metadataIdentificationString);

        final ServiceAnnotationValues serviceAnnotationValues = serviceInterfaceMetadata
                .getServiceAnnotationValues();
        final JavaType[] domainTypes = serviceAnnotationValues.getDomainTypes();
        if (domainTypes == null) {
            return null;
        }

        /*
         * For each domain type, collect (1) the plural and (2) the additions to
         * make to the service class for calling a lower layer when implementing
         * each service layer method. We use LinkedHashMaps for the latter
         * nested map to ensure repeatable order of code generation.
         */
        final Map<JavaType, String> domainTypePlurals = new HashMap<JavaType, String>();
        final Map<JavaType, JavaType> domainTypeToIdTypeMap = new HashMap<JavaType, JavaType>();
        // Collect the additions for each method for each supported domain type
        final Map<JavaType, Map<ServiceLayerMethod, MemberTypeAdditions>> allCrudAdditions = new LinkedHashMap<JavaType, Map<ServiceLayerMethod, MemberTypeAdditions>>();
        for (final JavaType domainType : domainTypes) {

            final JavaType idType = getPersistenceMemberLocator()
                    .getIdentifierType(domainType);
            if (idType == null) {
                return null;
            }
            domainTypeToIdTypeMap.put(domainType, idType);
            // Collect the plural for this domain type

            final ClassOrInterfaceTypeDetails domainTypeDetails = getTypeLocationService()
                    .getTypeDetails(domainType);
            if (domainTypeDetails == null) {
                return null;
            }
            final LogicalPath path = PhysicalTypeIdentifier
                    .getPath(domainTypeDetails.getDeclaredByMetadataId());
            final String pluralId = PluralMetadata.createIdentifier(domainType,
                    path);
            final PluralMetadata pluralMetadata = (PluralMetadata) getMetadataService()
                    .get(pluralId);
            if (pluralMetadata == null) {
                return null;
            }
            domainTypePlurals.put(domainType, pluralMetadata.getPlural());

            // Maintain a list of entities that are being handled by this layer
            managedEntityTypes.put(domainType, metadataIdentificationString);

            // Collect the additions the service class needs in order to invoke
            // each service layer method
            final Map<ServiceLayerMethod, MemberTypeAdditions> methodAdditions = new LinkedHashMap<ServiceLayerMethod, MemberTypeAdditions>();
            for (final ServiceLayerMethod method : ServiceLayerMethod.values()) {
                final Collection<MethodParameter> methodParameters = MethodParameter
                        .asList(method.getParameters(domainType, idType));
                final MemberTypeAdditions memberTypeAdditions = getLayerService()
                        .getMemberTypeAdditions(metadataIdentificationString,
                                method.getKey(), domainType, idType,
                                LAYER_POSITION, methodParameters);
                if (memberTypeAdditions != null) {
                    // A lower layer implements this method
                    methodAdditions.put(method, memberTypeAdditions);
                }
            }
            allCrudAdditions.put(domainType, methodAdditions);

            // Register this provider for changes to the domain type or its
            // plural
            getMetadataDependencyRegistry().registerDependency(
                    domainTypeDetails.getDeclaredByMetadataId(),
                    metadataIdentificationString);
            getMetadataDependencyRegistry().registerDependency(pluralId,
                    metadataIdentificationString);
        }

        final MemberDetails serviceClassDetails = getMemberDetailsScanner()
                .getMemberDetails(getClass().getName(), serviceClass);

        // Adds or removes service from XML configuration
        if (serviceAnnotationValues.useXmlConfiguration()) {
            getTemplateService().addServiceToXmlConfiguration(serviceInterface,
                    serviceClass);
        }
        else {
            getTemplateService().removeServiceFromXmlConfiguration(serviceInterface);
        }

        return new ServiceClassMetadata(metadataIdentificationString,
                aspectName, governorPhysicalTypeMetadata, serviceClassDetails,
                serviceAnnotationValues, domainTypeToIdTypeMap,
                allCrudAdditions, domainTypePlurals, serviceInterface.getName()
                        .getSimpleTypeName());

    }

    public String getProvidesType() {
        return ServiceClassMetadata.getMetadataIdentiferType();
    }
    
    public ServiceLayerTemplateService getTemplateService(){
    	if(templateService == null){
    		// Get all Services implement ServiceLayerTemplateService interface
    		try {
    			ServiceReference<?>[] references = context.getAllServiceReferences(ServiceLayerTemplateService.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (ServiceLayerTemplateService) context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load ServiceLayerTemplateService on SecurityOperationsImpl.");
    			return null;
    		}
    	}else{
    		return templateService;
    	}
    }
    
    public LayerService getLayerService(){
    	if(layerService == null){
    		// Get all Services implement LayerService interface
    		try {
    			ServiceReference<?>[] references = context.getAllServiceReferences(LayerService.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (LayerService) context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load LayerService on SecurityOperationsImpl.");
    			return null;
    		}
    	}else{
    		return layerService;
    	}
    }
}
