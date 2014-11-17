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
@Component
@Service
public class JsonMetadataProvider extends AbstractItdMetadataProvider {

    protected void activate(final ComponentContext cContext) {
    	context = cContext.getBundleContext();
        getMetadataDependencyRegistry().registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        addMetadataTriggers(ROO_JSON, ROO_IDENTIFIER);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return JsonMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        getMetadataDependencyRegistry().deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        removeMetadataTriggers(ROO_JSON, ROO_IDENTIFIER);
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = JsonMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = JsonMetadata
                .getPath(metadataIdentificationString);
        final String physicalTypeIdentifier = PhysicalTypeIdentifier
                .createIdentifier(javaType, path);
        return physicalTypeIdentifier;
    }

    public String getItdUniquenessFilenameSuffix() {
        return "Json";
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String itdFilename) {
        // Acquire bean info (we need getters details, specifically)
        final JavaType javaType = JsonMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = JsonMetadata
                .getPath(metadataIdentificationString);

        // We need to parse the annotation, if it is not present we will simply
        // get the default annotation values
        final JsonAnnotationValues annotationValues = new JsonAnnotationValues(
                governorPhysicalTypeMetadata);

        String plural = javaType.getSimpleTypeName() + "s";
        final PluralMetadata pluralMetadata = (PluralMetadata) getMetadataService()
                .get(PluralMetadata.createIdentifier(javaType, path));
        if (pluralMetadata != null) {
            plural = pluralMetadata.getPlural();
        }

        return new JsonMetadata(metadataIdentificationString, aspectName,
                governorPhysicalTypeMetadata, plural, annotationValues);
    }

    public String getProvidesType() {
        return JsonMetadata.getMetadataIdentiferType();
    }
}