package org.springframework.roo.addon.tostring;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.beaninfo.BeanInfoMetadataProvider;
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
import org.springframework.roo.support.util.Assert;

/**
 * Provides {@link ToStringMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public final class ToStringMetadataProvider extends AbstractItdMetadataProvider {

	public ToStringMetadataProvider(MetadataService metadataService, MetadataDependencyRegistry metadataDependencyRegistry, FileManager fileManager, BeanInfoMetadataProvider beanInfoMetadataProvider) {
		super(metadataService, metadataDependencyRegistry, fileManager);
		Assert.notNull(beanInfoMetadataProvider, "Bean info metadata provider required");
		beanInfoMetadataProvider.addMetadataTrigger(new JavaType(RooToString.class.getName()));
		addMetadataTrigger(new JavaType(RooToString.class.getName()));
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// Acquire bean info (we need getters details, specifically)
		JavaType javaType = ToStringMetadata.getJavaType(metadataIdentificationString);
		Path path = ToStringMetadata.getPath(metadataIdentificationString);
		String beanInfoMetadataKey = BeanInfoMetadata.createIdentifier(javaType, path);

		// We want to be notified if the getter info changes in any way 
		metadataDependencyRegistry.registerDependency(beanInfoMetadataKey, metadataIdentificationString);
		BeanInfoMetadata beanInfoMetadata = (BeanInfoMetadata) metadataService.get(beanInfoMetadataKey);
		
		// Abort if we don't have getter information available
		if (beanInfoMetadata == null) {
			return null;
		}
		
		// Otherwise go off and create the to String metadata
		return new ToStringMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, beanInfoMetadata);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "ToString";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = ToStringMetadata.getJavaType(metadataIdentificationString);
		Path path = ToStringMetadata.getPath(metadataIdentificationString);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}
	
	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return ToStringMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return ToStringMetadata.getMetadataIdentiferType();
	}
	
}
