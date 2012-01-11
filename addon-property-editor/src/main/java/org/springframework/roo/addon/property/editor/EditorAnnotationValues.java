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

    @AutoPopulate private JavaType providePropertyEditorFor;

    /**
     * Constructor
     * 
     * @param governorPhysicalTypeMetadata
     */
    public EditorAnnotationValues(
            final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
        super(governorPhysicalTypeMetadata, RooJavaType.ROO_EDITOR);
        AutoPopulationUtils.populate(this, annotationMetadata);
    }

    /**
     * Returns the {@link JavaType} to which the property editor applies
     * 
     * @return <code>null</code> if not set
     */
    public JavaType getEditedType() {
        return providePropertyEditorFor;
    }
}
