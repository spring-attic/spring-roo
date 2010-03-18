package org.springframework.roo.addon.javabean;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdProviderRole;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;

/**
 * Provides {@link JavaBeanMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public final class JavaBeanMetadataProvider extends AbstractItdMetadataProvider {

	public JavaBeanMetadataProvider(MetadataService metadataService, MetadataDependencyRegistry metadataDependencyRegistry, FileManager fileManager, BeanInfoMetadataProvider beanInfoMetadataProvider) {
		super(metadataService, metadataDependencyRegistry, fileManager);
		Assert.notNull(beanInfoMetadataProvider, "Bean info metadata provider required");
		beanInfoMetadataProvider.addMetadataTrigger(new JavaType(RooJavaBean.class.getName()));
		addProviderRole(ItdProviderRole.ACCESSOR_MUTATOR);
		addMetadataTrigger(new JavaType(RooJavaBean.class.getName()));
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		return new JavaBeanMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "JavaBean";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = JavaBeanMetadata.getJavaType(metadataIdentificationString);
		Path path = JavaBeanMetadata.getPath(metadataIdentificationString);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return JavaBeanMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return JavaBeanMetadata.getMetadataIdentiferType();
	}
	
}
