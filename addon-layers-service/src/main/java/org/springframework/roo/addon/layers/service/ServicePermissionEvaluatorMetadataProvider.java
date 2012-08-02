package org.springframework.roo.addon.layers.service;

import static org.springframework.roo.model.RooJavaType.ROO_SERVICE_PERMISSION;
import static org.springframework.roo.model.SpringJavaType.PERMISSION_EVALUATOR;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

@Component
@Service
public class ServicePermissionEvaluatorMetadataProvider extends
        AbstractMemberDiscoveringItdMetadataProvider {
    @Reference protected TypeManagementService typeManagementService;

    private final Map<JavaType, String> managedEntityTypes = new HashMap<JavaType, String>();

    protected void activate(final ComponentContext context) {
        metadataDependencyRegistry.addNotificationListener(this);
        metadataDependencyRegistry.registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        setIgnoreTriggerAnnotations(true);
        // addMetadataTrigger(ROO_SERVICE);
    }

    protected void deactivate(final ComponentContext context) {
        metadataDependencyRegistry.removeNotificationListener(this);
        metadataDependencyRegistry.deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        // removeMetadataTrigger(ROO_SERVICE);
    }

    @Override
    public String getItdUniquenessFilenameSuffix() {
        return "PermissionEvaluator";
    }

    @Override
    public String getProvidesType() {
        return ServicePermissionEvaluatorMetadata.getMetadataIdentiferType();
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

        final MemberHoldingTypeDetails memberHoldingTypeDetails = typeLocationService
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
        return ServicePermissionEvaluatorMetadata.createIdentifier(javaType,
                path);
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            String metadataIdentificationString) {
        final JavaType javaType = ServicePermissionEvaluatorMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = ServicePermissionEvaluatorMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            String metadataIdentificationString, JavaType aspectName,
            PhysicalTypeMetadata governorPhysicalTypeMetadata,
            String itdFilename) {
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

        if (permissionEvaluatorInterface == null) {
            return null;
        }
        //
        // ServiceInterfaceMetadata serviceInterfaceMetadata = null;
        // for (final ClassOrInterfaceTypeDetails service : typeLocationService
        // .findClassesOrInterfaceDetailsWithAnnotation(ROO_SERVICE)) {
        // if (service != null) {
        // final LogicalPath path = PhysicalTypeIdentifier.getPath(service
        // .getDeclaredByMetadataId());
        // final String implementedTypeId = ServiceInterfaceMetadata
        // .createIdentifier(service.getName(), path);
        // serviceInterfaceMetadata = (ServiceInterfaceMetadata) metadataService
        // .get(implementedTypeId);
        // if (serviceInterfaceMetadata != null
        // && serviceInterfaceMetadata.isValid()) {
        // AnnotationAttributeValue<Boolean> annotationAttributeValue = service
        // .getAnnotation(ROO_SERVICE).getAttribute("secure");
        // if (annotationAttributeValue == null
        // || annotationAttributeValue.getValue().equals(
        // Boolean.FALSE)) {
        // metadataDependencyRegistry.deregisterDependency(
        // serviceInterfaceMetadata.getId(),
        // metadataIdentificationString);
        // }
        // else {
        // metadataDependencyRegistry.registerDependency(
        // serviceInterfaceMetadata.getId(),
        // metadataIdentificationString);
        // }
        // }
        // }
        // }

        Map<String, ClassOrInterfaceTypeDetails> servicePermissions = new HashMap<String, ClassOrInterfaceTypeDetails>();
        for (ClassOrInterfaceTypeDetails permission : typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(ROO_SERVICE_PERMISSION)) {
            servicePermissions.put(
                    new JavaType(permission
                            .getAnnotation(ROO_SERVICE_PERMISSION)
                            .getAttribute("value").getValue().toString())
                            .getSimpleTypeName(), permission);
        }

        return new ServicePermissionEvaluatorMetadata(
                metadataIdentificationString, aspectName,
                governorPhysicalTypeMetadata, servicePermissions);
    }
}
