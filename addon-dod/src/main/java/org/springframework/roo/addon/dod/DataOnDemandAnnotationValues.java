package org.springframework.roo.addon.dod;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;

/**
 * Represents a parsed {@link RooDataOnDemand} annotation.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class DataOnDemandAnnotationValues extends AbstractAnnotationValues {
	// From annotation
	@AutoPopulate private JavaType entity = null;
	@AutoPopulate private int quantity = 10;

	public DataOnDemandAnnotationValues(PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(governorPhysicalTypeMetadata, new JavaType(RooDataOnDemand.class.getName()));
		AutoPopulationUtils.populate(this, annotationMetadata);
	}

	public JavaType getEntity() {
		return entity;
	}

	public int getQuantity() {
		return quantity;
	}
}
