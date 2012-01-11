package org.springframework.roo.addon.solr;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.RooJavaType;

/**
 * Represents a parsed {@link RooSolrWebSearchable} annotation.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public class SolrWebSearchAnnotationValues extends AbstractAnnotationValues {

    @AutoPopulate private String autoCompleteMethod = "autoComplete";
    @AutoPopulate private String searchMethod = "search";

    /**
     * Constructor
     * 
     * @param governorPhysicalTypeMetadata
     */
    public SolrWebSearchAnnotationValues(
            final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
        super(governorPhysicalTypeMetadata, RooJavaType.ROO_SOLR_WEB_SEARCHABLE);
        AutoPopulationUtils.populate(this, annotationMetadata);
    }

    public String getAutoCompleteMethod() {
        return autoCompleteMethod;
    }

    public String getSearchMethod() {
        return searchMethod;
    }
}
