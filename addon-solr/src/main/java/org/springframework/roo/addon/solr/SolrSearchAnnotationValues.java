package org.springframework.roo.addon.solr;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.RooJavaType;

/**
 * Represents a parsed {@link RooSolrSearchable} annotation.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public class SolrSearchAnnotationValues extends AbstractAnnotationValues {

    @AutoPopulate String deleteIndexMethod = "deleteIndex";
    @AutoPopulate String indexMethod = "index";
    @AutoPopulate String postPersistOrUpdateMethod = "postPersistOrUpdate";
    @AutoPopulate String preRemoveMethod = "preRemove";
    @AutoPopulate String searchMethod = "search";
    @AutoPopulate String simpleSearchMethod = "search";

    /**
     * Constructor
     * 
     * @param governorPhysicalTypeMetadata
     */
    public SolrSearchAnnotationValues(
            final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
        super(governorPhysicalTypeMetadata, RooJavaType.ROO_SOLR_SEARCHABLE);
        AutoPopulationUtils.populate(this, annotationMetadata);
    }

    public String getDeleteIndexMethod() {
        return deleteIndexMethod;
    }

    public String getIndexMethod() {
        return indexMethod;
    }

    public String getPostPersistOrUpdateMethod() {
        return postPersistOrUpdateMethod;
    }

    public String getPreRemoveMethod() {
        return preRemoveMethod;
    }

    public String getSearchMethod() {
        return searchMethod;
    }

    public String getSimpleSearchMethod() {
        return simpleSearchMethod;
    }
}
