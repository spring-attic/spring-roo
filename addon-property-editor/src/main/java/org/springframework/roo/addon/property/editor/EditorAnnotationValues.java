package org.springframework.roo.addon.property.editor;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * Represents a parsed {@link RooEditor} annotation.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
public class EditorAnnotationValues extends AbstractAnnotationValues {
	
	// From annotation
	@AutoPopulate JavaType providePropertyEditorFor = null;

	/**
	 * Constructor
	 *
	 * @param governorPhysicalTypeMetadata
	 */
	public EditorAnnotationValues(PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(governorPhysicalTypeMetadata, RooJavaType.ROO_EDITOR);
		AutoPopulationUtils.populate(this, annotationMetadata);
	}
}
