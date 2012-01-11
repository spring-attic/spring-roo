package org.springframework.roo.addon.jpa.identifier;

import static org.springframework.roo.model.RooJavaType.ROO_IDENTIFIER;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.jpa.AbstractIdentifierServiceAwareMetadataProvider;
import org.springframework.roo.addon.serializable.SerializableMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;

/**
 * Implementation of {@link IdentifierMetadataProvider}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class IdentifierMetadataProviderImpl extends
        AbstractIdentifierServiceAwareMetadataProvider implements
        IdentifierMetadataProvider {

    @Reference private ConfigurableMetadataProvider configurableMetadataProvider;
    @Reference private ProjectOperations projectOperations;
    @Reference private SerializableMetadataProvider serializableMetadataProvider;

    protected void activate(final ComponentContext context) {
        metadataDependencyRegistry.registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        addMetadataTrigger(ROO_IDENTIFIER);
        configurableMetadataProvider.addMetadataTrigger(ROO_IDENTIFIER);
        serializableMetadataProvider.addMetadataTrigger(ROO_IDENTIFIER);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return IdentifierMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        metadataDependencyRegistry.deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        removeMetadataTrigger(ROO_IDENTIFIER);
        configurableMetadataProvider.removeMetadataTrigger(ROO_IDENTIFIER);
        serializableMetadataProvider.removeMetadataTrigger(ROO_IDENTIFIER);
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = IdentifierMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = IdentifierMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "Identifier";
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String itdFilename) {
        final IdentifierAnnotationValues annotationValues = new IdentifierAnnotationValues(
                governorPhysicalTypeMetadata);
        if (!annotationValues.isAnnotationFound()) {
            return null;
        }

        // We know governor type details are non-null and can be safely cast
        final JavaType javaType = IdentifierMetadata
                .getJavaType(metadataIdentificationString);
        final List<Identifier> identifierServiceResult = getIdentifiersForType(javaType);

        final LogicalPath path = PhysicalTypeIdentifierNamingUtils
                .getPath(metadataIdentificationString);
        if (projectOperations.isProjectAvailable(path.getModule())) {
            // If the project itself changes, we want a chance to refresh this
            // item
            metadataDependencyRegistry.registerDependency(
                    ProjectMetadata.getProjectIdentifier(path.getModule()),
                    metadataIdentificationString);
        }

        return new IdentifierMetadata(metadataIdentificationString, aspectName,
                governorPhysicalTypeMetadata, annotationValues,
                identifierServiceResult);
    }

    public String getProvidesType() {
        return IdentifierMetadata.getMetadataIdentifierType();
    }
}
