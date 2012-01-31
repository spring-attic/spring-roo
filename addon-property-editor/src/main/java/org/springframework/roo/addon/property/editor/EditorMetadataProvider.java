package org.springframework.roo.addon.property.editor;

import static org.springframework.roo.model.RooJavaType.ROO_EDITOR;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.jpa.activerecord.JpaActiveRecordMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Provides {@link EditorMetadata}.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component(immediate = true)
@Service
public class EditorMetadataProvider extends AbstractItdMetadataProvider {

    protected void activate(final ComponentContext context) {
        metadataDependencyRegistry.registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        addMetadataTrigger(ROO_EDITOR);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return EditorMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        metadataDependencyRegistry.deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        removeMetadataTrigger(ROO_EDITOR);
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = EditorMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = EditorMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "Editor";
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String itdFilename) {
        // We know governor type details are non-null and can be safely cast

        // We need to parse the annotation, which we expect to be present
        final EditorAnnotationValues annotationValues = new EditorAnnotationValues(
                governorPhysicalTypeMetadata);
        if (!annotationValues.isAnnotationFound()
                || annotationValues.getEditedType() == null) {
            return null;
        }

        // Lookup the form backing object's metadata
        final JavaType javaType = annotationValues.getEditedType();
        if (!typeLocationService.isInProject(javaType)) {
            return null;
        }

        final LogicalPath path = EditorMetadata
                .getPath(metadataIdentificationString);
        final String jpaActiveRecordMetadataKey = JpaActiveRecordMetadata
                .createIdentifier(javaType, path);

        // We need to lookup the metadata we depend on
        final JpaActiveRecordMetadata jpaActiveRecordMetadata = (JpaActiveRecordMetadata) metadataService
                .get(jpaActiveRecordMetadataKey);
        if (jpaActiveRecordMetadata == null
                || !jpaActiveRecordMetadata.isValid()) {
            return null;
        }

        // We need to be informed if our dependent metadata changes
        metadataDependencyRegistry.registerDependency(
                jpaActiveRecordMetadataKey, metadataIdentificationString);

        // We do not need to monitor the parent, as any changes to the java type
        // associated with the parent will trickle down to
        // the governing java type
        final JavaType identifierType = persistenceMemberLocator
                .getIdentifierType(javaType);
        if (identifierType == null) {
            return null;
        }

        final MethodMetadata identifierAccessor = persistenceMemberLocator
                .getIdentifierAccessor(javaType);
        if (identifierAccessor == null) {
            return null;
        }

        final MethodMetadata findMethod = jpaActiveRecordMetadata
                .getFindMethod();

        return new EditorMetadata(metadataIdentificationString, aspectName,
                governorPhysicalTypeMetadata, javaType, identifierType,
                identifierAccessor, findMethod);
    }

    public String getProvidesType() {
        return EditorMetadata.getMetadataIdentiferType();
    }
}
