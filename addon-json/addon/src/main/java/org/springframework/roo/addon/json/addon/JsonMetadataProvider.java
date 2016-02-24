package org.springframework.roo.addon.json.addon;

import static org.springframework.roo.model.RooJavaType.ROO_IDENTIFIER;
import static org.springframework.roo.model.RooJavaType.ROO_JSON;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.plural.addon.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Provides {@link JsonMetadata}.
 * 
 * @author Stefan Schmidt
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 1.1
 */
@Component
@Service
public class JsonMetadataProvider extends AbstractItdMetadataProvider {

    protected MetadataDependencyRegistryTracker registryTracker = null;

    /**
     * This service is being activated so setup it:
     * <ul>
     * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
     * <li>Registers {@link RooJavaType#ROO_JSON} and 
     * {@link RooJavaType#ROO_IDENTIFIER} as additional JavaTypes that will 
     * trigger metadata registration.</li>
     * </ul>
     */
    @Override
    protected void activate(final ComponentContext cContext) {
    	context = cContext.getBundleContext();
        this.registryTracker = new MetadataDependencyRegistryTracker(context,
                null, PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        this.registryTracker.open();
        addMetadataTriggers(ROO_JSON, ROO_IDENTIFIER);
    }

    /**
     * This service is being deactivated so unregister upstream-downstream 
     * dependencies, triggers, matchers and listeners.
     * 
     * @param context
     */
    protected void deactivate(final ComponentContext context) {
        MetadataDependencyRegistry registry = this.registryTracker.getService();
        registry.deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        this.registryTracker.close();
        removeMetadataTriggers(ROO_JSON, ROO_IDENTIFIER);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return JsonMetadata.createIdentifier(javaType, path);
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