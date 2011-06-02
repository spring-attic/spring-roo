package org.springframework.roo.addon.solr;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;

import org.junit.Test;
import org.springframework.roo.model.JavaType;

/**
 * Unit test of {@link SolrUtils}
 *
 * @author Andrew Swan
 * @since 1.1.5
 */
public class SolrUtilsTest {

	// Constants
	private static final JavaType JAVA_UTIL_CALENDAR = new JavaType(Calendar.class.getName());

	@Test
	public void testGetSolrDynamicFieldPostFixForJavaUtilCalendar() {
		assertEquals("_dt", SolrUtils.getSolrDynamicFieldPostFix(JAVA_UTIL_CALENDAR));
	}
}
