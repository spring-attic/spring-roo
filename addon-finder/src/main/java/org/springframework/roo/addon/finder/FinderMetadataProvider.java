package org.springframework.roo.addon.finder;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.entity.RooEntity;
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
 * Provides {@link FinderMetadata}.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public final class FinderMetadataProvider extends AbstractItdMetadataProvider {

	public FinderMetadataProvider(MetadataService metadataService, MetadataDependencyRegistry metadataDependencyRegistry, FileManager fileManager) {
		super(metadataService, metadataDependencyRegistry, fileManager);
		addMetadataTrigger(new JavaType(RooEntity.class.getName()));
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We know governor type details are non-null and can be safely cast
		
		// Work out the MIDs of the other metadata we depend on
		JavaType javaType = FinderMetadata.getJavaType(metadataIdentificationString);
		Path path = FinderMetadata.getPath(metadataIdentificationString);
		String beanInfoMetadataKey = BeanInfoMetadata.createIdentifier(javaType, path);
		String entityMetadataKey = EntityMetadata.createIdentifier(javaType, path);
		
		// We need to lookup the metadata we depend on
		BeanInfoMetadata beanInfoMetadata = (BeanInfoMetadata) metadataService.get(beanInfoMetadataKey);
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(entityMetadataKey);
		
		// We need to abort if we couldn't find dependent metadata
		if (beanInfoMetadata == null || !beanInfoMetadata.isValid() || entityMetadata == null || !entityMetadata.isValid()) {
			return null;
		}
		
		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(beanInfoMetadataKey, metadataIdentificationString);
		metadataDependencyRegistry.registerDependency(entityMetadataKey, metadataIdentificationString);
		
		return new FinderMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, beanInfoMetadata, entityMetadata);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "Finder";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = FinderMetadata.getJavaType(metadataIdentificationString);
		Path path = FinderMetadata.getPath(metadataIdentificationString);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return FinderMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return FinderMetadata.getMetadataIdentiferType();
	}
	
}
