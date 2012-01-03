package org.springframework.roo.addon.solr;

import org.springframework.roo.classpath.details.annotations.populator.AnnotationValuesTestCase;

/**
 * Unit test of {@link SolrWebSearchAnnotationValues}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class SolrWebSearchAnnotationValuesTest
        extends
        AnnotationValuesTestCase<RooSolrWebSearchable, SolrWebSearchAnnotationValues> {

    @Override
    protected Class<RooSolrWebSearchable> getAnnotationClass() {
        return RooSolrWebSearchable.class;
    }

    @Override
    protected Class<SolrWebSearchAnnotationValues> getValuesClass() {
        return SolrWebSearchAnnotationValues.class;
    }
}
