package org.springframework.roo.addon.jsf.converter;

import static org.springframework.roo.addon.jsf.converter.JsfConverterMetadata.ID_FIELD_NAME;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_METHOD;
import static org.springframework.roo.model.RooJavaType.ROO_JSF_CONVERTER;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Implementation of {@link JsfConverterMetadataProvider}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component(immediate = true)
@Service
public class JsfConverterMetadataProviderImpl extends
        AbstractMemberDiscoveringItdMetadataProvider implements
        JsfConverterMetadataProvider {

    private static final int LAYER_POSITION = LayerType.HIGHEST.getPosition();
    @Reference private ConfigurableMetadataProvider configurableMetadataProvider;
    @Reference private LayerService layerService;
    private final Map<String, JavaType> converterMidToEntityMap = new LinkedHashMap<String, JavaType>();
    private final Map<JavaType, String> entityToConverterMidMap = new LinkedHashMap<JavaType, String>();

    protected void activate(final ComponentContext context) {
        metadataDependencyRegistry.addNotificationListener(this);
        metadataDependencyRegistry.registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        addMetadataTrigger(ROO_JSF_CONVERTER);
        configurableMetadataProvider.addMetadataTrigger(ROO_JSF_CONVERTER);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return JsfConverterMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        metadataDependencyRegistry.removeNotificationListener(this);
        metadataDependencyRegistry.deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        removeMetadataTrigger(ROO_JSF_CONVERTER);
        configurableMetadataProvider.removeMetadataTrigger(ROO_JSF_CONVERTER);
    }

    private MemberTypeAdditions getFindMethod(final JavaType entity,
            final String metadataIdentificationString) {
        metadataDependencyRegistry.registerDependency(
                typeLocationService.getPhysicalTypeIdentifier(entity),
                metadataIdentificationString);
        final List<FieldMetadata> idFields = persistenceMemberLocator
                .getIdentifierFields(entity);
        if (idFields.isEmpty()) {
            return null;
        }
        final FieldMetadata idField = idFields.get(0);
        final JavaType idType = persistenceMemberLocator
                .getIdentifierType(entity);
        if (idType == null) {
            return null;
        }
        metadataDependencyRegistry
                .registerDependency(idField.getDeclaredByMetadataId(),
                        metadataIdentificationString);

        final MethodParameter idParameter = new MethodParameter(idType,
                ID_FIELD_NAME);
        return layerService.getMemberTypeAdditions(
                metadataIdentificationString, FIND_METHOD.name(), entity,
                idType, LAYER_POSITION, idParameter);
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = JsfConverterMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = JsfConverterMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "Converter";
    }

    @Override
    protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
        // Determine the governor for this ITD, and whether any metadata is even
        // hoping to hear about changes to that JavaType and its ITDs
        final JavaType governor = itdTypeDetails.getName();
        final String localMid = entityToConverterMidMap.get(governor);
        if (localMid != null) {
            return localMid;
        }

        final MemberHoldingTypeDetails memberHoldingTypeDetails = typeLocationService
                .getTypeDetails(itdTypeDetails.getGovernor().getName());
        if (memberHoldingTypeDetails != null) {
            for (final JavaType type : memberHoldingTypeDetails
                    .getLayerEntities()) {
                final String localMidType = entityToConverterMidMap.get(type);
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
        // We need to parse the annotation, which we expect to be present
        final JsfConverterAnnotationValues annotationValues = new JsfConverterAnnotationValues(
                governorPhysicalTypeMetadata);
        final JavaType entity = annotationValues.getEntity();
        if (!annotationValues.isAnnotationFound() || entity == null) {
            return null;
        }

        // Remember that this entity JavaType matches up with this metadata
        // identification string
        // Start by clearing any previous association
        final JavaType oldEntity = converterMidToEntityMap
                .get(metadataIdentificationString);
        if (oldEntity != null) {
            entityToConverterMidMap.remove(oldEntity);
        }
        entityToConverterMidMap.put(entity, metadataIdentificationString);
        converterMidToEntityMap.put(metadataIdentificationString, entity);

        final MemberTypeAdditions findMethod = getFindMethod(entity,
                metadataIdentificationString);
        final MethodMetadata identifierAccessor = persistenceMemberLocator
                .getIdentifierAccessor(entity);

        return new JsfConverterMetadata(metadataIdentificationString,
                aspectName, governorPhysicalTypeMetadata, annotationValues,
                findMethod, identifierAccessor);
    }

    public String getProvidesType() {
        return JsfConverterMetadata.getMetadataIdentiferType();
    }
}