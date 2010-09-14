package org.springframework.roo.addon.test;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;

/**
 * Represents a parsed {@link RooIntegrationTest} annotation.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class IntegrationTestAnnotationValues extends AbstractAnnotationValues {
	
	// From annotation
	@AutoPopulate private JavaType entity = null;
	@AutoPopulate private boolean count = true; 
	@AutoPopulate private boolean find = true; 
	@AutoPopulate private boolean findEntries = true; 
	@AutoPopulate private boolean findAll = true;
	@AutoPopulate private int findAllMaximum = 250;
	@AutoPopulate private boolean flush = true; 
	@AutoPopulate private boolean persist = true; 
	@AutoPopulate private boolean remove = true; 
	@AutoPopulate private boolean merge = true; 

	public IntegrationTestAnnotationValues(PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(governorPhysicalTypeMetadata, new JavaType(RooIntegrationTest.class.getName()));
		AutoPopulationUtils.populate(this, annotationMetadata);
	}

	public JavaType getEntity() {
		return entity;
	}

	public boolean isCount() {
		return count;
	}

	public boolean isFind() {
		return find;
	}

	public boolean isFindEntries() {
		return findEntries;
	}

	public boolean isFindAll() {
		return findAll;
	}

	public int getFindAllMaximum() {
		return findAllMaximum;
	}

	public boolean isFlush() {
		return flush;
	}

	public boolean isPersist() {
		return persist;
	}

	public boolean isRemove() {
		return remove;
	}

	public boolean isMerge() {
		return merge;
	}
}
