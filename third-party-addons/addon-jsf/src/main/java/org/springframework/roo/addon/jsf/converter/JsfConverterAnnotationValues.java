package org.springframework.roo.addon.jsf.converter;

import static org.springframework.roo.model.RooJavaType.ROO_JSF_CONVERTER;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;

/**
 * Represents a parsed {@link RooJsfConverter} annotation.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public class JsfConverterAnnotationValues extends AbstractAnnotationValues {

    @AutoPopulate private JavaType entity;

    public JsfConverterAnnotationValues(
            final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
        super(governorPhysicalTypeMetadata, ROO_JSF_CONVERTER);
        AutoPopulationUtils.populate(this, annotationMetadata);
    }

    public JavaType getEntity() {
        return entity;
    }
}
