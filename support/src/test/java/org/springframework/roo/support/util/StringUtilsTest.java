package org.springframework.roo.support.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link StringUtils}.
 * 
 * @author Alan Stewart
 * @since 1.1.3
 */
public class StringUtilsTest {
	
	@Test
	public void testPadRight1() {
		assertEquals("9999", StringUtils.padRight("9", 4, '9'));
	}
	
	@Test
	public void testPadRight2() {
		assertEquals("Foo999", StringUtils.padRight("Foo", 6, '9'));
	}

	@Test
	public void testPadLeft1() {
		assertEquals("999", StringUtils.padLeft("9", 3, '9'));
	}
	
	@Test
	public void testPadLeft2() {
		assertEquals("99Foo", StringUtils.padLeft("Foo", 5, '9'));
	}
}
