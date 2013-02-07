package org.springframework.roo.addon.security;

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
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

@Component
@Service
public class PermissionEvaluatorMetadataProvider extends
        AbstractMemberDiscoveringItdMetadataProvider {
    @Reference protected TypeManagementService typeManagementService;

    private final Map<JavaType, String> managedEntityTypes = new HashMap<JavaType, String>();

    protected void activate(final ComponentContext context) {
        metadataDependencyRegistry.addNotificationListener(this);
        metadataDependencyRegistry.registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        setIgnoreTriggerAnnotations(true);
    }

    protected void deactivate(final ComponentContext context) {
        metadataDependencyRegistry.removeNotificationListener(this);
        metadataDependencyRegistry.deregisterDependency(
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

        final MemberDetails permissionEvaluatorClassDetails = memberDetailsScanner
                .getMemberDetails(getClass().getName(),
                        permissionEvaluatorClass);

        return new PermissionEvaluatorMetadata(metadataIdentificationString,
                aspectName, governorPhysicalTypeMetadata,
                permissionEvaluatorClassDetails);
    }
}
