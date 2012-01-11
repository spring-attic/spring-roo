package org.springframework.roo.addon.dbre;

import static org.springframework.roo.model.RooJavaType.ROO_DB_MANAGED;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;

/**
 * Represents a parsed {@link RooDbManaged} annotation.
 * 
 * @author Alan Stewart
 * @since 1.1.4
 */
public class DbManagedAnnotationValues extends AbstractAnnotationValues {

    @AutoPopulate private boolean automaticallyDelete = true;

    /**
     * Constructor
     * 
     * @param governorPhysicalTypeMetadata
     */
    public DbManagedAnnotationValues(
            final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
        super(governorPhysicalTypeMetadata, ROO_DB_MANAGED);
        AutoPopulationUtils.populate(this, annotationMetadata);
    }

    public boolean isAutomaticallyDelete() {
        return automaticallyDelete;
    }
}
