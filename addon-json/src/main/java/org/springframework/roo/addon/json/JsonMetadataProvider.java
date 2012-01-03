package org.springframework.roo.addon.json;

import static org.springframework.roo.model.RooJavaType.ROO_IDENTIFIER;
import static org.springframework.roo.model.RooJavaType.ROO_JSON;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Provides {@link JsonMetadata}.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class JsonMetadataProvider extends AbstractItdMetadataProvider {

    protected void activate(final ComponentContext context) {
        metadataDependencyRegistry.registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        addMetadataTriggers(ROO_JSON, ROO_IDENTIFIER);
    }

    protected void deactivate(final ComponentContext context) {
        metadataDependencyRegistry.deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        removeMetadataTriggers(ROO_JSON, ROO_IDENTIFIER);
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String itdFilename) {
        // Acquire bean info (we need getters details, specifically)
        JavaType javaType = JsonMetadata
                .getJavaType(metadataIdentificationString);
        LogicalPath path = JsonMetadata.getPath(metadataIdentificationString);

        // We need to parse the annotation, if it is not present we will simply
        // get the default annotation values
        JsonAnnotationValues annotationValues = new JsonAnnotationValues(
                governorPhysicalTypeMetadata);

        String plural = javaType.getSimpleTypeName() + "s";
        PluralMetadata pluralMetadata = (PluralMetadata) metadataService
                .get(PluralMetadata.createIdentifier(javaType, path));
        if (pluralMetadata != null) {
            plural = pluralMetadata.getPlural();
        }

        return new JsonMetadata(metadataIdentificationString, aspectName,
                governorPhysicalTypeMetadata, plural, annotationValues);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "Json";
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        JavaType javaType = JsonMetadata
                .getJavaType(metadataIdentificationString);
        LogicalPath path = JsonMetadata.getPath(metadataIdentificationString);
        String physicalTypeIdentifier = PhysicalTypeIdentifier
                .createIdentifier(javaType, path);
        return physicalTypeIdentifier;
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return JsonMetadata.createIdentifier(javaType, path);
    }

    public String getProvidesType() {
        return JsonMetadata.getMetadataIdentiferType();
    }
}