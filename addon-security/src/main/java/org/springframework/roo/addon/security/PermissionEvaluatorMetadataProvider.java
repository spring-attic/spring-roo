package org.springframework.roo.addon.security;

import static org.springframework.roo.model.SpringJavaType.PERMISSION_EVALUATOR;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;

@Component
@Service
public class PermissionEvaluatorMetadataProvider extends
        AbstractMemberDiscoveringItdMetadataProvider {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(PermissionEvaluatorMetadataProvider.class);
	
    private TypeManagementService typeManagementService;

    private final Map<JavaType, String> managedEntityTypes = new HashMap<JavaType, String>();

    protected void activate(final ComponentContext cContext) {
    	context = cContext.getBundleContext();
        getMetadataDependencyRegistry().addNotificationListener(this);
        getMetadataDependencyRegistry().registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        setIgnoreTriggerAnnotations(true);
    }

    protected void deactivate(final ComponentContext context) {
        getMetadataDependencyRegistry().removeNotificationListener(this);
        getMetadataDependencyRegistry().deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
    }

    @Override
    public String getItdUniquenessFilenameSuffix() {
        return "PermissionEvaluator";
    }

    @Override
    public String getProvidesType() {
        return PermissionEvaluatorMetadata.getMetadataIdentiferType();
    }

    @Override
    protected String getLocalMidToRequest(ItdTypeDetails itdTypeDetails) {
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
    protected String createLocalIdentifier(JavaType javaType, LogicalPath path) {
        return PermissionEvaluatorMetadata.createIdentifier(javaType, path);
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            String metadataIdentificationString) {
        final JavaType javaType = PermissionEvaluatorMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = PermissionEvaluatorMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString, 
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String itdFilename) {
    	
        final ClassOrInterfaceTypeDetails permissionEvaluatorClass = governorPhysicalTypeMetadata
                .getMemberHoldingTypeDetails();
        if (permissionEvaluatorClass == null) {
            return null;
        }
        
        JavaType permissionEvaluatorInterface = null;

        for (final JavaType implementedType : permissionEvaluatorClass
                .getImplementsTypes()) {
            if (implementedType.equals(PERMISSION_EVALUATOR)) {
                permissionEvaluatorInterface = implementedType;
                break;
            }
        }

        //Checks to ensure the supposed permission evaluator class actually implements PermissionEvaluator
        if (permissionEvaluatorInterface == null) {
            return null;
        }
        
        final PermissionEvaluatorAnnotationValues annotationValues = new PermissionEvaluatorAnnotationValues(
                governorPhysicalTypeMetadata);
        
        //AnnotationMetadata annotationMetadata = MemberFindingUtils.getAnnotationOfType(permissionEvaluatorClass.getAnnotations(), RooJavaType.ROO_PERMISSION_EVALUATOR);
        //Checks to ensure permission evaluator class includes the @RooPermissionEvaluator annotation
        /*if (annotationValues == null) {
            return null;
        }*/
        
        final MemberDetails permissionEvaluatorClassDetails = memberDetailsScanner
                .getMemberDetails(getClass().getName(),
                        permissionEvaluatorClass);
        
        Map<JavaType, String> domainTypesToPlurals = getDomainTypesToPlurals();
        
        //AnnotationAttributeValue<Boolean> defaultReturnValue = annotationMetadata.getAttribute("defaultReturnValue");

        return new PermissionEvaluatorMetadata(metadataIdentificationString,
                aspectName, governorPhysicalTypeMetadata,
                permissionEvaluatorClassDetails, 
                annotationValues, // == null ? false : defaultReturnValue.getValue(), 
                domainTypesToPlurals);
    }
    
    private Map<JavaType, String> getDomainTypesToPlurals() {
    	
    	Map<JavaType, String>  domainTypesToPlurals = new HashMap<JavaType, String> ();
    	for (ClassOrInterfaceTypeDetails cid : getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_SERVICE)) {
    		AnnotationMetadata annotationMetadata = MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(), RooJavaType.ROO_SERVICE);
    		AnnotationAttributeValue<Boolean> usePermissionEvaluator = annotationMetadata.getAttribute("usePermissionEvaluator");
    		if (usePermissionEvaluator == null || usePermissionEvaluator.getValue() == false){
    			continue;
    		}
    		AnnotationAttributeValue<Collection<ClassAttributeValue>> domainTypes = annotationMetadata.getAttribute("domainTypes");
            for (ClassAttributeValue domainType : domainTypes.getValue()) {
	    		final ClassOrInterfaceTypeDetails domainTypeDetails = getTypeLocationService()
	                    .getTypeDetails(domainType.getValue());
	            if (domainTypeDetails == null) {
	                return null;
	            }
	            final LogicalPath path = PhysicalTypeIdentifier
	                    .getPath(domainTypeDetails.getDeclaredByMetadataId());
	            final String pluralId = PluralMetadata.createIdentifier(domainType.getValue(),
	                    path);
	            final PluralMetadata pluralMetadata = (PluralMetadata) metadataService
	                    .get(pluralId);
	            if (pluralMetadata == null) {
	                continue;
	            }
	    		domainTypesToPlurals.put(domainType.getValue(), pluralMetadata.getPlural());
            }
    	}
    	return domainTypesToPlurals;
    }
}
