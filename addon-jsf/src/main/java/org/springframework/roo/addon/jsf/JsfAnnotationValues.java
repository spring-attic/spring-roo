package org.springframework.roo.addon.jsf;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;

/**
 * Represents a parsed {@link RooJsfManagedBean} annotation.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public class JsfAnnotationValues extends AbstractAnnotationValues {
	// From annotation
	@AutoPopulate private JavaType entity = null;
	@AutoPopulate private boolean includeOnMenu = true;

	public JsfAnnotationValues(PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(governorPhysicalTypeMetadata, new JavaType(RooJsfManagedBean.class.getName()));
		AutoPopulationUtils.populate(this, annotationMetadata);
	}

	public JavaType getEntity() {
		return entity;
	}

	public boolean isIncludeOnMenu() {
		return includeOnMenu;
	}
}
