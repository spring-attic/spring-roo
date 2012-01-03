package org.springframework.roo.addon.solr;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.roo.model.JdkJavaType;

/**
 * Unit test of {@link SolrUtils}
 * 
 * @author Andrew Swan
 * @since 1.1.5
 */
public class SolrUtilsTest {

    @Test
    public void testGetSolrDynamicFieldPostFixForJavaUtilCalendar() {
        assertEquals("_dt",
                SolrUtils.getSolrDynamicFieldPostFix(JdkJavaType.CALENDAR));
    }
}
