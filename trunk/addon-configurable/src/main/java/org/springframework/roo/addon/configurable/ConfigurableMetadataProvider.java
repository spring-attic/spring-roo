package org.springframework.roo.addon.configurable;

import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;

/**
 * Provides {@link ConfigurableMetadata}.
 * 
 * <p>
 * Generally other add-ons which depend on Spring's @Configurable annotation being present will add their
 * annotation as a trigger annotation to instances of {@ink ConfigurableMetadataProvider}. This action will
 * guarantee any class with the added trigger annotation will made @Configurable.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public final class ConfigurableMetadataProvider extends AbstractItdMetadataProvider {

	public ConfigurableMetadataProvider(MetadataService metadataService, MetadataDependencyRegistry metadataDependencyRegistry, FileManager fileManager) {
		super(metadataService, metadataDependencyRegistry, fileManager);
		addMetadataTrigger(new JavaType(RooConfigurable.class.getName()));
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
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return ConfigurableMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return ConfigurableMetadata.getMetadataIdentiferType();
	}
	
}
