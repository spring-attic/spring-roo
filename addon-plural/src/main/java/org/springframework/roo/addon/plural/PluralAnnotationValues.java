package org.springframework.roo.addon.plural;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;

/**
 * The values of a {@link RooPlural} annotation.
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class PluralAnnotationValues extends AbstractAnnotationValues {
	
	// Fields
	@AutoPopulate private String value = "";

	/**
	 * Constructor that reads the {@link RooPlural} annotation on the given
	 * governor
	 *
	 * @param governor the governor's metadata (required)
	 */
	public PluralAnnotationValues(final PhysicalTypeMetadata governor) {
		super(governor, RooPlural.class);
		AutoPopulationUtils.populate(this, annotationMetadata);
	}

	/**
	 * Returns the plural provided by the annotation
	 * 
	 * @return
	 */
	public String getValue() {
		return value;
	}
}
