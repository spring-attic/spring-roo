package org.springframework.roo.addon.configurable;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Implementation of {@link ConfigurableMetadataProvider}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public final class ConfigurableMetadataProviderImpl extends AbstractItdMetadataProvider implements ConfigurableMetadataProvider {

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooConfigurable.class.getName()));
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(new JavaType(RooConfigurable.class.getName()));
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		return new ConfigurableMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "Configurable";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = ConfigurableMetadata.getJavaType(metadataIdentificationString);
		Path path = ConfigurableMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return ConfigurableMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return ConfigurableMetadata.getMetadataIdentiferType();
	}
}
