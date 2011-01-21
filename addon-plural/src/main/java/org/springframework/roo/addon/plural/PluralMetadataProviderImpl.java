package org.springframework.roo.addon.plural;

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
 * Provides {@link PluralMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true) 
@Service 
public final class PluralMetadataProviderImpl extends AbstractItdMetadataProvider implements PluralMetadataProvider {

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		setIgnoreTriggerAnnotations(true);
		setDependsOnGovernorBeingAClass(false);
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
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
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return PluralMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return PluralMetadata.getMetadataIdentiferType();
	}
}
