package org.springframework.roo.addon.web.mvc.controller;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.finder.FinderMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
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
 * Provides {@link WebScaffoldMetadata}.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
@ScopeDevelopment
public final class WebScaffoldMetadataProvider extends AbstractItdMetadataProvider {
	
	private ControllerOperations controllerOperations;
	public WebScaffoldMetadataProvider(MetadataService metadataService, MetadataDependencyRegistry metadataDependencyRegistry, FileManager fileManager, ControllerOperations controllerOperations) {
		super(metadataService, metadataDependencyRegistry, fileManager);
		Assert.notNull(controllerOperations, "Controller operations required");
		addMetadataTrigger(new JavaType(RooWebScaffold.class.getName()));
		this.controllerOperations = controllerOperations;
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We know governor type details are non-null and can be safely cast
		
		// We need to parse the annotation, which we expect to be present
		WebScaffoldAnnotationValues annotationValues = new WebScaffoldAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound() || annotationValues.formBackingObject == null) {
			return null;
		}
		
		// Lookup the form backing object's metadata
		JavaType javaType = annotationValues.formBackingObject;
		Path path = Path.SRC_MAIN_JAVA;
		String beanInfoMetadataKey = BeanInfoMetadata.createIdentifier(javaType, path);
		String entityMetadataKey = EntityMetadata.createIdentifier(javaType, path);
		String finderMetdadataKey = FinderMetadata.createIdentifier(javaType, path);
		
		// We need to lookup the metadata we depend on
		BeanInfoMetadata beanInfoMetadata = (BeanInfoMetadata) metadataService.get(beanInfoMetadataKey);
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(entityMetadataKey);
		FinderMetadata finderMetadata = (FinderMetadata) metadataService.get(finderMetdadataKey);
		
		// We need to abort if we couldn't find dependent metadata
		if (beanInfoMetadata == null || !beanInfoMetadata.isValid() || entityMetadata == null || !entityMetadata.isValid()) {
			return null;
		}
		
		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(beanInfoMetadataKey, metadataIdentificationString);
		metadataDependencyRegistry.registerDependency(entityMetadataKey, metadataIdentificationString);
		
		// We also need to be informed if a referenced type is changed
		for (MethodMetadata accessor : beanInfoMetadata.getPublicAccessors(false)) {
					
			//not interested in identifiers and version fields
			if (accessor.equals(entityMetadata.getIdentifierAccessor()) || accessor.equals(entityMetadata.getVersionAccessor())) {
				continue;
			}
			JavaType type = accessor.getReturnType();

			String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(type, Path.SRC_MAIN_JAVA);
			//we are only interested if the type is part of our application and if no editor exists for it already
			if (metadataService.get(physicalTypeIdentifier) != null) {
				metadataDependencyRegistry.registerDependency(BeanInfoMetadata.createIdentifier(type, path), metadataIdentificationString);
			}
		}
		
		// We do not need to monitor the parent, as any changes to the java type associated with the parent will trickle down to
		// the governing java type
		return new WebScaffoldMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, metadataService, annotationValues, beanInfoMetadata, entityMetadata, finderMetadata, controllerOperations);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "Controller";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = WebScaffoldMetadata.getJavaType(metadataIdentificationString);
		Path path = WebScaffoldMetadata.getPath(metadataIdentificationString);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return WebScaffoldMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return WebScaffoldMetadata.getMetadataIdentiferType();
	}
}
