package org.springframework.roo.addon.plural;

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
 * Provides {@link PluralMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public final class PluralMetadataProvider extends AbstractItdMetadataProvider {

	public PluralMetadataProvider(MetadataService metadataService, MetadataDependencyRegistry metadataDependencyRegistry, FileManager fileManager) {
		super(metadataService, metadataDependencyRegistry, fileManager);
		addMetadataTrigger(new JavaType(RooPlural.class.getName()));
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		return new PluralMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "Plural";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = PluralMetadata.getJavaType(metadataIdentificationString);
		Path path = PluralMetadata.getPath(metadataIdentificationString);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return PluralMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return PluralMetadata.getMetadataIdentiferType();
	}
	
}
