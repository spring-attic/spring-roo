package org.springframework.roo.layers.external.dao;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public class DaoJpaAnnotationValues extends AbstractAnnotationValues {

	@AutoPopulate private JavaType[] domainTypes = null;
	@AutoPopulate private String removeMethod = RooDaoJpa.REMOVE_METHOD;
	
	public DaoJpaAnnotationValues(PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(governorPhysicalTypeMetadata, new JavaType(RooDaoJpa.class.getName()));
		AutoPopulationUtils.populate(this, annotationMetadata);
	}

	public JavaType[] getDomainTypes() {
		return domainTypes;
	}

	public String getRemoveMethod() {
		return removeMethod;
	}
}
