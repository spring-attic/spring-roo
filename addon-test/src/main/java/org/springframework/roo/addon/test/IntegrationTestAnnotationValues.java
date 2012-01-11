package org.springframework.roo.addon.test;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * Represents a parsed {@link RooIntegrationTest} annotation.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class IntegrationTestAnnotationValues extends AbstractAnnotationValues {

    @AutoPopulate private boolean count = true;
    @AutoPopulate private JavaType entity;
    @AutoPopulate private boolean find = true;
    @AutoPopulate private boolean findAll = true;
    @AutoPopulate private int findAllMaximum = 250;
    @AutoPopulate private boolean findEntries = true;
    @AutoPopulate private boolean flush = true;
    @AutoPopulate private boolean merge = true;
    @AutoPopulate private boolean persist = true;
    @AutoPopulate private boolean remove = true;
    @AutoPopulate private boolean transactional = true;

    public IntegrationTestAnnotationValues(
            final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
        super(governorPhysicalTypeMetadata, RooJavaType.ROO_INTEGRATION_TEST);
        AutoPopulationUtils.populate(this, annotationMetadata);
    }

    public JavaType getEntity() {
        return entity;
    }

    public int getFindAllMaximum() {
        return findAllMaximum;
    }

    public boolean isCount() {
        return count;
    }

    public boolean isFind() {
        return find;
    }

    public boolean isFindAll() {
        return findAll;
    }

    public boolean isFindEntries() {
        return findEntries;
    }

    public boolean isFlush() {
        return flush;
    }

    public boolean isMerge() {
        return merge;
    }

    public boolean isPersist() {
        return persist;
    }

    public boolean isRemove() {
        return remove;
    }

    public boolean isTransactional() {
        return transactional;
    }
}
