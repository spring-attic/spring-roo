package org.springframework.roo.addon.configurable;

import static org.springframework.roo.model.RooJavaType.ROO_CONFIGURABLE;

import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.AbstractHashCodeTrackingMetadataNotifier;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link ConfigurableMetadataProvider}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class ConfigurableMetadataProviderImpl extends
        AbstractItdMetadataProvider implements ConfigurableMetadataProvider {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(ConfigurableMetadataProviderImpl.class);
	
    protected void activate(final ComponentContext cContext) {
    	context = cContext.getBundleContext();
    	getMetadataDependencyRegistry().registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        addMetadataTrigger(ROO_CONFIGURABLE);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return ConfigurableMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        getMetadataDependencyRegistry().deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        removeMetadataTrigger(ROO_CONFIGURABLE);
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = ConfigurableMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = ConfigurableMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "Configurable";
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String itdFilename) {
        return new ConfigurableMetadata(metadataIdentificationString,
                aspectName, governorPhysicalTypeMetadata);
    }

    public String getProvidesType() {
        return ConfigurableMetadata.getMetadataIdentiferType();
    }
}
