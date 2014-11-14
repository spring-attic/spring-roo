package org.springframework.roo.addon.gwt.request;

import static org.springframework.roo.model.RooJavaType.ROO_GWT_REQUEST;

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.gwt.GwtTypeService;
import org.springframework.roo.addon.gwt.GwtUtils;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Triggers the generation of {@link GwtRequestMetadata} upon being notified of
 * changes to {@link PhysicalTypeMetadata} within the user project.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class GwtRequestMetadataNotificationListener implements
        MetadataNotificationListener {

    @Reference GwtTypeService gwtTypeService;
    @Reference MetadataDependencyRegistry metadataDependencyRegistry;
    @Reference MetadataService metadataService;
    @Reference TypeLocationService typeLocationService;

    protected void activate(final ComponentContext context) {
        metadataDependencyRegistry.addNotificationListener(this);
    }

    protected void deactivate(final ComponentContext context) {
        metadataDependencyRegistry.removeNotificationListener(this);
    }

    private String getDownstreamInstanceId(final String upstreamDependency) {
        final ClassOrInterfaceTypeDetails upstreamType = typeLocationService
                .getTypeDetails(upstreamDependency);
        if (upstreamType == null) {
            return null;
        }

        final String downstreamOfGwtEntityLayerComponent = getDownstreamOfGwtEntityLayerComponent(upstreamType);
        if (downstreamOfGwtEntityLayerComponent != null) {
            return downstreamOfGwtEntityLayerComponent;
        }

        if (upstreamType.getAnnotation(ROO_GWT_REQUEST) == null) {
            final String downstreamOfGwtEntity = getDownstreamOfGwtEntity(upstreamType
                    .getType());
            if (downstreamOfGwtEntity != null) {
                return downstreamOfGwtEntity;
            }
        }

        return null;
    }

    private String getDownstreamOfGwtEntity(final JavaType upstreamType) {
        for (final ClassOrInterfaceTypeDetails request : typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(ROO_GWT_REQUEST)) {
            final AnnotationMetadata gwtRequestAnnotation = request
                    .getAnnotation(ROO_GWT_REQUEST);
            if (gwtRequestAnnotation != null) {
                final AnnotationAttributeValue<?> attributeValue = gwtRequestAnnotation
                        .getAttribute("value");
                Validate.validState(attributeValue != null,
                        "The x annotation should have a '%s' attribute",
                        "value");
                final String entityClass = GwtUtils
                        .getStringValue(attributeValue);
                if (upstreamType.getFullyQualifiedTypeName()
                        .equals(entityClass)) {
                    // The upstream type is an entity with an associated GWT
                    // request; make that request the downstream
                    return getLocalMid(request.getDeclaredByMetadataId());
                }
            }
        }
        return null;
    }

    private String getDownstreamOfGwtEntityLayerComponent(
            final MemberHoldingTypeDetails upstreamType) {
        final List<JavaType> layerEntities = upstreamType.getLayerEntities();
        if (!layerEntities.isEmpty()) {
            // Look for a GWT request that manages one of these entities
            for (final ClassOrInterfaceTypeDetails request : typeLocationService
                    .findClassesOrInterfaceDetailsWithAnnotation(ROO_GWT_REQUEST)) {
                final ClassOrInterfaceTypeDetails entity = gwtTypeService
                        .lookupEntityFromRequest(request);
                if (entity != null && layerEntities.contains(entity.getType())) {
                    // This layer component has an associated GWT request; make
                    // that request the downstream
                    return getLocalMid(request.getDeclaredByMetadataId());
                }
            }
        }
        return null;
    }

    private String getLocalMid(final String physicalTypeId) {
        final JavaType typeName = PhysicalTypeIdentifier
                .getJavaType(physicalTypeId);
        final LogicalPath typePath = PhysicalTypeIdentifier
                .getPath(physicalTypeId);
        return GwtRequestMetadata.createIdentifier(typeName, typePath);
    }

    public void notify(final String upstreamMID, final String downstreamMID) {
        if (!PhysicalTypeIdentifier.isValid(upstreamMID)) {
            return;
        }

        final String downstreamInstanceId;
        if (MetadataIdentificationUtils.isIdentifyingInstance(downstreamMID)) {
            downstreamInstanceId = downstreamMID;
        }
        else {
            downstreamInstanceId = getDownstreamInstanceId(upstreamMID);
            if (downstreamInstanceId == null) {
                return;
            }
        }

        // We should now have an instance-specific "downstream dependency" that
        // can be processed by this class
        Validate.isTrue(PhysicalTypeIdentifierNamingUtils.isValid(
                GwtRequestMetadata.class.getName(), downstreamInstanceId));
        metadataService.evictAndGet(downstreamInstanceId);
    }
}
