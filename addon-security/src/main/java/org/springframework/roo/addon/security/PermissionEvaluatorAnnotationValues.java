package org.springframework.roo.addon.security;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.RooJavaType;

public class PermissionEvaluatorAnnotationValues extends AbstractAnnotationValues {
    @AutoPopulate private final boolean defaultReturnValue = false;
    
    /**
     * Constructor
     * 
     * @param governorPhysicalTypeMetadata to parse (required)
     */
    public PermissionEvaluatorAnnotationValues(
            final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
        super(governorPhysicalTypeMetadata, RooJavaType.ROO_PERMISSION_EVALUATOR);
        AutoPopulationUtils.populate(this, annotationMetadata);
    }

	public boolean getDefaultReturnValue() {
		return defaultReturnValue;
	}
}
