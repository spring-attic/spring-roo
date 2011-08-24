package org.springframework.roo.support.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit test of {@link ObjectUtils}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class ObjectUtilsTest {

	@Test
	public void testCompareTwoNulls() {
		assertEquals(0, ObjectUtils.nullSafeComparison(null, null));
	}
	
	@Test
	public void testCompareNullWithNonNull() {
		// Invoke
		final int result = ObjectUtils.nullSafeComparison(null, "");
		
		// Check
		assertTrue(result < 0);
	}
	
	@Test
	public void testCompareNonNullWithNull() {
		// Invoke
		final int result = ObjectUtils.nullSafeComparison("", null);
		
		// Check
		assertTrue(result > 0);
	}
	
	@Test
	public void testCompareLesserWithGreater() {
		// Invoke
		final int result = ObjectUtils.nullSafeComparison(100, 200);
		
		// Check
		assertTrue(result < 0);
	}
	
	@Test
	public void testCompareGreaterWithLesser() {
		// Invoke
		final int result = ObjectUtils.nullSafeComparison(300, 200);
		
		// Check
		assertTrue(result > 0);
	}
	
	@Test
	public void testCompareTwoEqualObjects() {
		assertEquals(0, ObjectUtils.nullSafeComparison(400, 400));
	}
}
