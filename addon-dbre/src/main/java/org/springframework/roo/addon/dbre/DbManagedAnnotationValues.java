package org.springframework.roo.addon.dbre;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.RooJavaType;

/**
 * Represents a parsed {@link RooDbManaged} annotation.
 * 
 * @author Alan Stewart
 * @since 1.1.4
 */
public class DbManagedAnnotationValues extends AbstractAnnotationValues {
	
	// From annotation
	@AutoPopulate private boolean automaticallyDelete = true;

	/**
	 * Constructor
	 *
	 * @param governorPhysicalTypeMetadata
	 */
	public DbManagedAnnotationValues(PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(governorPhysicalTypeMetadata, RooJavaType.ROO_DB_MANAGED);
		AutoPopulationUtils.populate(this, annotationMetadata);
	}

	public boolean isAutomaticallyDelete() {
		return automaticallyDelete;
	}
}
