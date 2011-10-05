package org.springframework.roo.addon.displayname;

import static org.springframework.roo.model.RooJavaType.ROO_DISPLAY_NAME;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;

/**
 * Represents a parsed {@link RooDisplayName} annotation.
 *
 * @author Alan Stewart
 * @since 1.2.0
 */
public class DisplayNameAnnotationValues extends AbstractAnnotationValues {

	// From annotation
	@AutoPopulate private String methodName = "getDisplayName";
	@AutoPopulate private String[] fields;
	@AutoPopulate private String separator;

	/**
	 * Constructor
	 *
	 * @param governorPhysicalTypeMetadata
	 */
	public DisplayNameAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(governorPhysicalTypeMetadata, ROO_DISPLAY_NAME);
		AutoPopulationUtils.populate(this, annotationMetadata);
	}

	public String getMethodName() {
		return methodName;
	}

	public String[] getFields() {
		return fields;
	}

	public String getSeparator() {
		return separator;
	}
}
