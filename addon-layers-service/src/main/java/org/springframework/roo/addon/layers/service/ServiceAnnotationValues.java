package org.springframework.roo.addon.layers.service;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;

/**
 * The values of a given {@link RooService} annotation.
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2
 */
public class ServiceAnnotationValues extends AbstractAnnotationValues {

	@AutoPopulate private JavaType[] domainTypes;
	@AutoPopulate private String countAllMethod = RooService.COUNT_ALL_METHOD;
	@AutoPopulate private String findAllMethod = RooService.FIND_ALL_METHOD;
	@AutoPopulate private String findEntriesMethod = RooService.FIND_ENTRIES_METHOD;
	@AutoPopulate private String saveMethod = RooService.SAVE_METHOD;
	@AutoPopulate private String updateMethod = RooService.UPDATE_METHOD;
	@AutoPopulate private String deleteMethod = RooService.DELETE_METHOD;
	@AutoPopulate private boolean transactional = true;
	
	/**
	 * Constructor
	 *
	 * @param governorPhysicalTypeMetadata to parse (required)
	 */
	public ServiceAnnotationValues(final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(governorPhysicalTypeMetadata, new JavaType(RooService.class.getName()));
		AutoPopulationUtils.populate(this, annotationMetadata);
	}

	public JavaType[] getDomainTypes() {
		return domainTypes;
	}

	public String getCountAllMethod() {
		return countAllMethod;
	}
	
	public String getFindAllMethod() {
		return findAllMethod;
	}
	
	public String getFindEntriesMethod() {
		return findEntriesMethod;
	}
	
	public String getSaveMethod() {
		return saveMethod;
	}
	
	public String getUpdateMethod() {
		return updateMethod;
	}
	
	public String getDeleteMethod() {
		return deleteMethod;
	}
	
	public boolean isTransactional() {
		return transactional;
	}
}
