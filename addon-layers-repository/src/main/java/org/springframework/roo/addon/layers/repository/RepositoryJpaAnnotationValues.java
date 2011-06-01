package org.springframework.roo.addon.layers.repository;

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
public class RepositoryJpaAnnotationValues extends AbstractAnnotationValues {

	@AutoPopulate private JavaType domainType = null;
	@AutoPopulate private String removeMethod = RooRepositoryJpa.REMOVE_METHOD;
	
	public RepositoryJpaAnnotationValues(PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(governorPhysicalTypeMetadata, new JavaType(RooRepositoryJpa.class.getName()));
		AutoPopulationUtils.populate(this, annotationMetadata);
	}

	public JavaType getDomainType() {
		return domainType;
	}

	public String getRemoveMethod() {
		return removeMethod;
	}
}
