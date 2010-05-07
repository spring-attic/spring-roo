package org.springframework.roo.addon.solr;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;

/**
 * Represents a parsed {@link RooSolrWebSearchable} annotation.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
public class SolrWebSearchAnnotationValues extends AbstractAnnotationValues {
	
	@AutoPopulate String searchMethod = "search";
	@AutoPopulate String autoCompleteMethod = "autoComplete";
	
	public SolrWebSearchAnnotationValues(PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(governorPhysicalTypeMetadata, new JavaType(RooSolrWebSearchable.class.getName()));
		AutoPopulationUtils.populate(this, annotationMetadata);
	}

	public String getSearchMethod() {
		return searchMethod;
	}

	public String getAutoCompleteMethod() {
		return autoCompleteMethod;
	}
}
