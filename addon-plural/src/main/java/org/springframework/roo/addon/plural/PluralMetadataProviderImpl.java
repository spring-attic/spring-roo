package org.springframework.roo.addon.plural;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Implementation of {@link PluralMetadataProvider}.
 * <p>
 * It's odd that this class extends {@link AbstractItdMetadataProvider}, as it
 * doesn't produce an ITD, it just provides a plural String via
 * {@link PluralMetadata#getPlural()}. We should probably refactor it.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class PluralMetadataProviderImpl extends AbstractItdMetadataProvider
        implements PluralMetadataProvider {

    protected void activate(final ComponentContext cContext) {
    	context = cContext.getBundleContext();
        getMetadataDependencyRegistry().registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        setIgnoreTriggerAnnotations(true);
        setDependsOnGovernorBeingAClass(false);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return PluralMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        getMetadataDependencyRegistry().deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = PluralMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = PluralMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "Plural";
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String itdFilename) {
        final PluralAnnotationValues pluralAnnotationValues = new PluralAnnotationValues(
                governorPhysicalTypeMetadata);
        return new PluralMetadata(metadataIdentificationString, aspectName,
                governorPhysicalTypeMetadata, pluralAnnotationValues);
    }

    public String getProvidesType() {
        return PluralMetadata.getMetadataIdentiferType();
    }
}
