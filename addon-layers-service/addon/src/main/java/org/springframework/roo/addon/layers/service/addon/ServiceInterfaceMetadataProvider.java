package org.springframework.roo.addon.layers.service.addon;

import static org.springframework.roo.model.RooJavaType.ROO_PERMISSION_EVALUATOR;
import static org.springframework.roo.model.RooJavaType.ROO_SERVICE;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.layers.service.annotations.RooService;
import org.springframework.roo.addon.plural.addon.PluralMetadata;
import org.springframework.roo.addon.security.addon.PermissionEvaluatorMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerTypeMatcher;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataProvider;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * {@link MetadataProvider} providing {@link ServiceInterfaceMetadata}
 * 
 * @author Stefan Schmidt
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 1.2.0
 */
@Component
@Service
public class ServiceInterfaceMetadataProvider extends
        AbstractMemberDiscoveringItdMetadataProvider {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(ServiceInterfaceMetadataProvider.class);
	
    private final Map<JavaType, String> managedEntityTypes = new HashMap<JavaType, String>();

    protected MetadataDependencyRegistryTracker registryTracker = null;
    protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

    /**
     * This service is being activated so setup it:
     * <ul>
     * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
     * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
     * <li>Registers {@link RooJavaType#ROO_SERVICE} as additional 
     * JavaType that will trigger metadata registration.</li>
     * <li>Set ensure the governor type details represent a class.</li>
     * </ul>
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void activate(final ComponentContext cContext) {
    	context = cContext.getBundleContext();
        this.registryTracker = 
                new MetadataDependencyRegistryTracker(context, this,
                        PhysicalTypeIdentifier.getMetadataIdentiferType(),
                        getProvidesType());
        this.registryTracker.open();

        setDependsOnGovernorBeingAClass(false);
        addMetadataTrigger(ROO_SERVICE);

        this.keyDecoratorTracker = new CustomDataKeyDecoratorTracker(context, 
                getClass(),
                new LayerTypeMatcher(ROO_SERVICE, new JavaSymbolName(
                        RooService.DOMAIN_TYPES_ATTRIBUTE)));
        this.keyDecoratorTracker.open();
    }

    /**
     * This service is being deactivated so unregister upstream-downstream 
     * dependencies, triggers, matchers and listeners.
     * 
     * @param context
     */
    protected void deactivate(final ComponentContext context) {
        MetadataDependencyRegistry registry = this.registryTracker.getService();
        registry.removeNotificationListener(this);
        registry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        this.registryTracker.close();

        removeMetadataTrigger(ROO_SERVICE);

        CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
        keyDecorator.unregisterMatchers(getClass());
        this.keyDecoratorTracker.close();
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return ServiceInterfaceMetadata.createIdentifier(javaType, path);
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
}
