package org.springframework.roo.addon.tostring;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.RooJavaType;

/**
 * Represents a parsed {@link RooToString} annotation.
 *
 * @author Alan Stewart
 * @since 1.2.0
 */
public class ToStringAnnotationValues extends AbstractAnnotationValues {

	// From annotation
	@AutoPopulate private String toStringMethod = "toString";
	@AutoPopulate private String[] excludeFields;
	@AutoPopulate private int maxLength = 25;

	/**
	 * Constructor
	 *
	 * @param governorPhysicalTypeMetadata
	 */
	public ToStringAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(governorPhysicalTypeMetadata, RooJavaType.ROO_TO_STRING);
		AutoPopulationUtils.populate(this, annotationMetadata);
	}

	public String getToStringMethod() {
		return toStringMethod;
	}

	public String[] getExcludeFields() {
		return excludeFields;
	}

	public int getMaxLength() {
		return maxLength;
	}
}
