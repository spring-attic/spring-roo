package org.springframework.roo.addon.serializable;

import static org.springframework.roo.model.RooJavaType.ROO_SERIALIZABLE;

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
 * Implementation of {@link SerializableMetadataProvider}.
 *
 * @author Alan Stewart
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class SerializableMetadataProviderImpl extends AbstractItdMetadataProvider implements SerializableMetadataProvider {

	protected void activate(final ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_SERIALIZABLE);
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_SERIALIZABLE);
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String metadataIdentificationString, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
		return new SerializableMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata);
	}

	public String getItdUniquenessFilenameSuffix() {
		return "Serializable";
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
		JavaType javaType = SerializableMetadata.getJavaType(metadataIdentificationString);
		LogicalPath path = SerializableMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	@Override
	protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
		return SerializableMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return SerializableMetadata.getMetadataIdentiferType();
	}
}
