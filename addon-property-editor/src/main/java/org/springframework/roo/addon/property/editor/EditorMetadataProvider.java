package org.springframework.roo.addon.property.editor;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Provides {@link EditorMetadata}.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component(immediate = true) 
@Service 
public final class EditorMetadataProvider extends AbstractItdMetadataProvider {

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooEditor.class.getName()));
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We know governor type details are non-null and can be safely cast

		// We need to parse the annotation, which we expect to be present
		EditorAnnotationValues annotationValues = new EditorAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound() || annotationValues.providePropertyEditorFor == null) {
			return null;
		}

		// Lookup the form backing object's metadata
		JavaType javaType = annotationValues.providePropertyEditorFor;
		Path path = Path.SRC_MAIN_JAVA;
		String entityMetadataKey = EntityMetadata.createIdentifier(javaType, path);

		// We need to lookup the metadata we depend on
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(entityMetadataKey);

		// We need to abort if we couldn't find dependent metadata
		if (entityMetadata == null || !entityMetadata.isValid()) {
			return null;
		}

		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(entityMetadataKey, metadataIdentificationString);

		// We do not need to monitor the parent, as any changes to the java type associated with the parent will trickle down to
		// the governing java type

		return new EditorMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, javaType, entityMetadata.getIdentifierField(), entityMetadata.getIdentifierAccessor(), entityMetadata.getFindMethod());
	}

	public String getItdUniquenessFilenameSuffix() {
		return "Editor";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = EditorMetadata.getJavaType(metadataIdentificationString);
		Path path = EditorMetadata.getPath(metadataIdentificationString);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return EditorMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return EditorMetadata.getMetadataIdentiferType();
	}
}
