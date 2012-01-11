package org.springframework.roo.addon.dod;

import static org.springframework.roo.model.RooJavaType.ROO_DATA_ON_DEMAND;

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

    @AutoPopulate private JavaType entity;
    @AutoPopulate private int quantity = 10;

    public DataOnDemandAnnotationValues(
            final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
        super(governorPhysicalTypeMetadata, ROO_DATA_ON_DEMAND);
        AutoPopulationUtils.populate(this, annotationMetadata);
    }

    public JavaType getEntity() {
        return entity;
    }

    public int getQuantity() {
        return quantity;
    }
}
