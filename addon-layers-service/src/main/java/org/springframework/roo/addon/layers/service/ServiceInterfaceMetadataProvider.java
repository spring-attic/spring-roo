package org.springframework.roo.addon.layers.service;

import static org.springframework.roo.model.RooJavaType.ROO_PERMISSION_EVALUATOR;
import static org.springframework.roo.model.RooJavaType.ROO_SERVICE;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.addon.security.PermissionEvaluatorMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
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
import org.springframework.roo.project.LogicalPath;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * {@link MetadataProvider} providing {@link ServiceInterfaceMetadata}
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component
@Service
public class ServiceInterfaceMetadataProvider extends
        AbstractMemberDiscoveringItdMetadataProvider {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(ServiceInterfaceMetadataProvider.class);
	
    private CustomDataKeyDecorator customDataKeyDecorator;

    private final Map<JavaType, String> managedEntityTypes = new HashMap<JavaType, String>();

    @SuppressWarnings("unchecked")
    protected void activate(final ComponentContext cContext) {
    	context = cContext.getBundleContext();
        super.setDependsOnGovernorBeingAClass(false);
        getMetadataDependencyRegistry().addNotificationListener(this);
        getMetadataDependencyRegistry().registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        addMetadataTrigger(ROO_SERVICE);
        getCustomDataKeyDecorator().registerMatchers(getClass(),
                new LayerTypeMatcher(ROO_SERVICE, new JavaSymbolName(
                        RooService.DOMAIN_TYPES_ATTRIBUTE)));
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return ServiceInterfaceMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        getMetadataDependencyRegistry().removeNotificationListener(this);
        getMetadataDependencyRegistry().deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        removeMetadataTrigger(ROO_SERVICE);
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = ServiceInterfaceMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = ServiceInterfaceMetadata
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
        final ServiceAnnotationValues annotationValues = new ServiceAnnotationValues(
                governorPhysicalTypeMetadata);
        final ClassOrInterfaceTypeDetails cid = governorPhysicalTypeMetadata
                .getMemberHoldingTypeDetails();
        if (cid == null) {
            return null;
        }
        final MemberDetails memberDetails = getMemberDetailsScanner()
                .getMemberDetails(getClass().getName(), cid);
        final JavaType[] domainTypes = annotationValues.getDomainTypes();
        if (domainTypes == null || domainTypes.length == 0) {
            return null;
        }
        final Map<JavaType, String> domainTypePlurals = new HashMap<JavaType, String>();
        final Map<JavaType, JavaType> domainTypeToIdTypeMap = new HashMap<JavaType, JavaType>();
        for (final JavaType type : domainTypes) {
            final JavaType idType = getPersistenceMemberLocator()
                    .getIdentifierType(type);
            if (idType == null) {
                continue;
            }
            // We simply take the first disregarding any further fields which
            // may be identifiers
            domainTypeToIdTypeMap.put(type, idType);
            final String domainTypeId = getTypeLocationService()
                    .getPhysicalTypeIdentifier(type);
            if (domainTypeId == null) {
                return null;
            }
            final LogicalPath path = PhysicalTypeIdentifier
                    .getPath(domainTypeId);
            final String pluralId = PluralMetadata.createIdentifier(type, path);
            final PluralMetadata pluralMetadata = (PluralMetadata) getMetadataService()
                    .get(pluralId);
            if (pluralMetadata == null) {
                return null;
            }
            // Maintain a list of entities that are being handled by this layer
            managedEntityTypes.put(type, metadataIdentificationString);
            getMetadataDependencyRegistry().registerDependency(pluralId,
                    metadataIdentificationString);
            domainTypePlurals.put(type, pluralMetadata.getPlural());
        }

        PermissionEvaluatorMetadata permissionEvaluatorMetadata = null;
        for (final ClassOrInterfaceTypeDetails permissionEvaluator : getTypeLocationService()
                .findClassesOrInterfaceDetailsWithAnnotation(ROO_PERMISSION_EVALUATOR)) {
            if (permissionEvaluator != null) {
                final LogicalPath path = PhysicalTypeIdentifier
                        .getPath(permissionEvaluator.getDeclaredByMetadataId());
                final String permissionEvaluatorId = PermissionEvaluatorMetadata
                        .createIdentifier(permissionEvaluator.getName(), path);
                permissionEvaluatorMetadata = (PermissionEvaluatorMetadata) getMetadataService()
                        .get(permissionEvaluatorId);
                if (permissionEvaluatorMetadata != null
                        && permissionEvaluatorMetadata.isValid()) {
                    if (annotationValues.usePermissionEvaluator()) {
                        getMetadataDependencyRegistry().registerDependency(
                                metadataIdentificationString,
                                permissionEvaluatorMetadata.getId());
                    }
                    else {
                        getMetadataDependencyRegistry().deregisterDependency(
                                metadataIdentificationString,
                                permissionEvaluatorMetadata.getId());
                    }

                }
            }
        }

        return new ServiceInterfaceMetadata(metadataIdentificationString,
                aspectName, governorPhysicalTypeMetadata, memberDetails,
                domainTypeToIdTypeMap, annotationValues, domainTypePlurals);
    }

    public String getProvidesType() {
        return ServiceInterfaceMetadata.getMetadataIdentiferType();
    }
    
    public CustomDataKeyDecorator getCustomDataKeyDecorator(){
    	if(customDataKeyDecorator == null){
    		// Get all Services implement CustomDataKeyDecorator interface
    		try {
    			ServiceReference<?>[] references = context.getAllServiceReferences(CustomDataKeyDecorator.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (CustomDataKeyDecorator) context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load CustomDataKeyDecorator on ServiceInterfaceMetadataProvider.");
    			return null;
    		}
    	}else{
    		return customDataKeyDecorator;
    	}
    	
    }
    
}
