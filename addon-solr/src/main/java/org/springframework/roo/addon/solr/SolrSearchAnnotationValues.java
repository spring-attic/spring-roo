package org.springframework.roo.addon.solr;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;

/**
 * Represents a parsed {@link RooSolrSearchable} annotation.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
public class SolrSearchAnnotationValues extends AbstractAnnotationValues {
	
	@AutoPopulate String searchMethod = "search";
	@AutoPopulate String simpleSearchMethod = "search";
	@AutoPopulate String postPersistOrUpdateMethod = "postPersistOrUpdate";
	@AutoPopulate String preRemoveMethod = "preRemove";
	@AutoPopulate String indexMethod = "index";
	@AutoPopulate String deleteIndexMethod = "deleteIndex";
	
	public SolrSearchAnnotationValues(PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(governorPhysicalTypeMetadata, new JavaType(RooSolrSearchable.class.getName()));
		AutoPopulationUtils.populate(this, annotationMetadata);
	}

	public String getSearchMethod() {
		return searchMethod;
	}

	public String getSimpleSearchMethod() {
		return simpleSearchMethod;
	}

	public String getPostPersistOrUpdateMethod() {
		return postPersistOrUpdateMethod;
	}

	public String getPreRemoveMethod() {
		return preRemoveMethod;
	}

	public String getIndexMethod() {
		return indexMethod;
	}

	public String getDeleteIndexMethod() {
		return deleteIndexMethod;
	}
}
