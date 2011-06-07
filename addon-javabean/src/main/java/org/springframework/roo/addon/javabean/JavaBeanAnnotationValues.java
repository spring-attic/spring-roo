package org.springframework.roo.addon.javabean;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;

/**
 * Represents a parsed {@link RooJavaBean} annotation.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public class JavaBeanAnnotationValues extends AbstractAnnotationValues {
	// From annotation
	@AutoPopulate private boolean gettersByDefault = true;
	@AutoPopulate private boolean settersByDefault = true;

	public JavaBeanAnnotationValues(PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(governorPhysicalTypeMetadata, new JavaType(RooJavaBean.class.getName()));
		AutoPopulationUtils.populate(this, annotationMetadata);
	}

	public boolean isGettersByDefault() {
		return gettersByDefault;
	}

	public boolean isSettersByDefault() {
		return settersByDefault;
	}
}
