package org.springframework.roo.addon.displaystring;

import static org.springframework.roo.model.RooJavaType.ROO_DISPLAY_STRING;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;

/**
 * Represents a parsed {@link RooDisplayString} annotation.
 *
 * @author Alan Stewart
 * @since 1.2.0
 */
public class DisplayStringAnnotationValues extends AbstractAnnotationValues {

	// From annotation
	@AutoPopulate private String[] fields;
	@AutoPopulate private String separator;

	/**
	 * Constructor
	 *
	 * @param governorPhysicalTypeMetadata
	 */
	public DisplayStringAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(governorPhysicalTypeMetadata, ROO_DISPLAY_STRING);
		AutoPopulationUtils.populate(this, annotationMetadata);
	}

	public String[] getFields() {
		return fields;
	}

	public String getSeparator() {
		return separator;
	}
}
