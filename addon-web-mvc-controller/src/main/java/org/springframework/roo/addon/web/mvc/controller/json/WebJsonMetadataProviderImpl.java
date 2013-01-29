package org.springframework.roo.addon.web.mvc.controller.json;

import static org.springframework.roo.model.RooJavaType.ROO_WEB_JSON;
import static org.springframework.roo.model.RooJavaType.ROO_WEB_SCAFFOLD;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.json.JsonMetadata;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.addon.web.mvc.controller.details.FinderMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.JavaTypePersistenceMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.WebMetadataService;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Implementation of {@link WebJsonMetadataProvider}.
 * 
 * @author Stefan Schmidt
 * @since 1.1.3
 */
@Component(immediate = true)
@Service
public class WebJsonMetadataProviderImpl extends
        AbstractMemberDiscoveringItdMetadataProvider implements
        WebJsonMetadataProvider {

    @Reference private WebMetadataService webMetadataService;

    // Maps entities to the IDs of their WebJsonMetadata
    private final Map<JavaType, String> managedEntityTypes = new HashMap<JavaType, String>();

    protected void activate(final ComponentContext context) {
        metadataDependencyRegistry.addNotificationListener(this);
        metadataDependencyRegistry.registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        addMetadataTrigger(ROO_WEB_JSON);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return WebJsonMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        metadataDependencyRegistry.removeNotificationListener(this);
        metadataDependencyRegistry.deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        removeMetadataTrigger(ROO_WEB_JSON);
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = WebJsonMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = WebJsonMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "Controller_Json";
    }

    @Override
    protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
        final ClassOrInterfaceTypeDetails governorTypeDetails = typeLocationService
                .getTypeDetails(itdTypeDetails.getName());
        if (governorTypeDetails == null) {
            return null;
        }

        // Check whether a relevant layer component has appeared, changed, or
        // disappeared
        final String localMidForLayerManagedEntity = getWebJsonMidIfLayerComponent(governorTypeDetails);
        if (StringUtils.isNotBlank(localMidForLayerManagedEntity)) {
            return localMidForLayerManagedEntity;
        }

        // Check whether the relevant MVC controller has appeared, changed, or
        // disappeared
        return getWebJsonMidIfMvcController(governorTypeDetails);
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String itdFilename) {
        // We need to parse the annotation, which we expect to be present
        final WebJsonAnnotationValues annotationValues = new WebJsonAnnotationValues(
                governorPhysicalTypeMetadata);
        if (!annotationValues.isAnnotationFound()
                || annotationValues.getJsonObject() == null
                || governorPhysicalTypeMetadata.getMemberHoldingTypeDetails() == null) {
            return null;
        }

        // Lookup the form backing object's metadata
        final JavaType jsonObject = annotationValues.getJsonObject();
        final ClassOrInterfaceTypeDetails jsonTypeDetails = typeLocationService
                .getTypeDetails(jsonObject);
        if (jsonTypeDetails == null) {
            return null;
        }
        final LogicalPath jsonObjectPath = PhysicalTypeIdentifier
                .getPath(jsonTypeDetails.getDeclaredByMetadataId());
        final JsonMetadata jsonMetadata = (JsonMetadata) metadataService
                .get(JsonMetadata.createIdentifier(jsonObject, jsonObjectPath));
        if (jsonMetadata == null) {
            return null;
        }

        final PhysicalTypeMetadata backingObjectPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService
                .get(PhysicalTypeIdentifier.createIdentifier(jsonObject,
                        typeLocationService.getTypePath(jsonObject)));
        Validate.notNull(backingObjectPhysicalTypeMetadata,
                "Unable to obtain physical type metadata for type %s",
                jsonObject.getFullyQualifiedTypeName());
        final MemberDetails formBackingObjectMemberDetails = getMemberDetails(backingObjectPhysicalTypeMetadata);
        final MemberHoldingTypeDetails backingMemberHoldingTypeDetails = MemberFindingUtils
                .getMostConcreteMemberHoldingTypeDetailsWithTag(
                        formBackingObjectMemberDetails,
                        CustomDataKeys.PERSISTENT_TYPE);
        if (backingMemberHoldingTypeDetails == null) {
            return null;
        }

        // We need to be informed if our dependent metadata changes
        metadataDependencyRegistry.registerDependency(
                backingMemberHoldingTypeDetails.getDeclaredByMetadataId(),
                metadataIdentificationString);

        final Set<FinderMetadataDetails> finderDetails = webMetadataService
                .getDynamicFinderMethodsAndFields(jsonObject,
                        formBackingObjectMemberDetails,
                        metadataIdentificationString);
        if (finderDetails == null) {
            return null;
        }
        final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> persistenceAdditions = webMetadataService
                .getCrudAdditions(jsonObject, metadataIdentificationString);
        final JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataDetails = webMetadataService
                .getJavaTypePersistenceMetadataDetails(jsonObject,
                        getMemberDetails(jsonObject),
                        metadataIdentificationString);
        final PluralMetadata pluralMetadata = (PluralMetadata) metadataService
                .get(PluralMetadata.createIdentifier(jsonObject,
                        typeLocationService.getTypePath(jsonObject)));
        if (persistenceAdditions.isEmpty()
                || javaTypePersistenceMetadataDetails == null
                || pluralMetadata == null) {
            return null;
        }

        // Maintain a list of entities that are being tested
        managedEntityTypes.put(jsonObject, metadataIdentificationString);

        return new WebJsonMetadata(metadataIdentificationString, aspectName,
                governorPhysicalTypeMetadata, annotationValues,
                persistenceAdditions,
                javaTypePersistenceMetadataDetails.getIdentifierField(),
                pluralMetadata.getPlural(), finderDetails, jsonMetadata,
                introduceLayerComponents(governorPhysicalTypeMetadata));
    }

    public String getProvidesType() {
        return WebJsonMetadata.getMetadataIdentiferType();
    }

    /**
     * If the given type is a layer component (e.g. repository or service),
     * returns the ID of the WebJsonMetadata for the first (!) domain type it
     * manages, in case this is a new layer component that the JSON ITD needs to
     * use.
     * 
     * @param governorTypeDetails the type to check (required)
     */
    private String getWebJsonMidIfLayerComponent(
            final ClassOrInterfaceTypeDetails governorTypeDetails) {
        for (final JavaType domainType : governorTypeDetails.getLayerEntities()) {
            final String webJsonMetadataId = managedEntityTypes.get(domainType);
            if (webJsonMetadataId != null) {
                return webJsonMetadataId;
            }
        }
        return null;
    }

    /**
     * If the given type is a web MVC controller, returns the ID of the
     * WebJsonMetadata for its form backing type, to ensure that any required
     * layer components are injected. This is a workaround to AspectJ not
     * handling multiple ITDs introducing the same field (in our case the layer
     * component) into one Java class.
     * 
     * @param governorTypeDetails the type to check (required)
     */
    private String getWebJsonMidIfMvcController(
            final ClassOrInterfaceTypeDetails governorTypeDetails) {
        final AnnotationMetadata controllerAnnotation = governorTypeDetails
                .getAnnotation(ROO_WEB_SCAFFOLD);
        if (controllerAnnotation != null) {
            final JavaType formBackingType = (JavaType) controllerAnnotation
                    .getAttribute("formBackingObject").getValue();
            final String webJsonMetadataId = managedEntityTypes
                    .get(formBackingType);
            if (webJsonMetadataId != null) {
                /*
                 * We've been notified of a change to an MVC controller for
                 * whose backing object we produce WebJsonMetadata; refresh that
                 * MD to ensure our ITD does or does not introduce any required
                 * layer components, as appropriate.
                 */
                metadataService.get(webJsonMetadataId);
            }
        }
        return null;
    }

    /**
     * Indicates whether the web JSON ITD should introduce any required layer
     * components (services, repositories, etc.). This information is necessary
     * for so long as AspectJ does not allow the same field to be introduced
     * into a given Java class by more than one ITD.
     * 
     * @param governor the governor, i.e. the controller (required)
     * @return see above
     */
    private boolean introduceLayerComponents(final PhysicalTypeMetadata governor) {
        // If no MVC ITD is going to be created, we have to introduce any
        // required layer components
        return MemberFindingUtils.getAnnotationOfType(governor
                .getMemberHoldingTypeDetails().getAnnotations(),
                ROO_WEB_SCAFFOLD) == null;
    }
}