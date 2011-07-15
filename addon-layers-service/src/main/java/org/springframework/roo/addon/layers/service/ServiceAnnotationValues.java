package org.springframework.roo.addon.layers.service;

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
public class ServiceAnnotationValues extends AbstractAnnotationValues {

	@AutoPopulate private JavaType[] domainTypes = null;
	@AutoPopulate private String findAllMethod = RooService.FIND_ALL_METHOD;
	@AutoPopulate private String saveMethod = RooService.SAVE_METHOD;
	@AutoPopulate private String updateMethod = RooService.UPDATE_METHOD;
	@AutoPopulate private boolean transactional = true;
	
	public ServiceAnnotationValues(PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(governorPhysicalTypeMetadata, new JavaType(RooService.class.getName()));
		AutoPopulationUtils.populate(this, annotationMetadata);
	}

	public JavaType[] getDomainTypes() {
		return domainTypes;
	}

	public String getFindAllMethod() {
		return findAllMethod;
	}
	
	public String getSaveMethod() {
		return saveMethod;
	}
	
	public String getUpdateMethod() {
		return updateMethod;
	}
	
	public boolean isTransactional() {
		return transactional;
	}
}
