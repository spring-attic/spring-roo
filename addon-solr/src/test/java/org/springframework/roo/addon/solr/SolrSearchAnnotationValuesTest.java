package org.springframework.roo.addon.solr;

import org.springframework.roo.classpath.details.annotations.populator.AnnotationValuesTestCase;

/**
 * Unit test of {@link SolrSearchAnnotationValues}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class SolrSearchAnnotationValuesTest extends
        AnnotationValuesTestCase<RooSolrSearchable, SolrSearchAnnotationValues> {

    @Override
    protected Class<RooSolrSearchable> getAnnotationClass() {
        return RooSolrSearchable.class;
    }

    @Override
    protected Class<SolrSearchAnnotationValues> getValuesClass() {
        return SolrSearchAnnotationValues.class;
    }
}
