package org.springframework.roo.addon.javabean;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.beaninfo.BeanInfoMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Provides {@link JavaBeanMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Component(immediate=true)
@Service
public final class JavaBeanMetadataProvider extends AbstractItdMetadataProvider {

	@Reference private BeanInfoMetadataProvider beanInfoMetadataProvider;
    @Reference private ClasspathOperations classpathOperations;
	
	protected void activate(ComponentContext context) {
		// Ensure we're notified of all metadata related to physical Java types, in particular their initial creation
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		beanInfoMetadataProvider.addMetadataTrigger(new JavaType(RooJavaBean.class.getName()));
		addMetadataTrigger(new JavaType(RooJavaBean.class.getName()));
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		beanInfoMetadataProvider.removeMetadataTrigger(new JavaType(RooJavaBean.class.getName()));
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		return new JavaBeanMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, metadataService, classpathOperations);
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
