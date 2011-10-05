package org.springframework.roo.support.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

	@Test
	public void testHasText1() {
		assertTrue(StringUtils.hasText("11111"));
	}

	@Test
	public void testHasText2() {
		assertFalse(StringUtils.hasText("     "));
	}

	@Test
	public void testRepeatNull() {
		assertNull(StringUtils.repeat(null, 27));
	}

	@Test
	public void testRepeatEmptyString() {
		assertEquals("", StringUtils.repeat("", 42));
	}

	@Test
	public void testRepeatSpace() {
		assertEquals("    ", StringUtils.repeat(" ", 4));
	}

	@Test
	public void testRepeatSingleCharacter() {
		assertEquals("qqq", StringUtils.repeat("q", 3));
	}

	@Test
	public void testRepeatMultipleCharacters() {
		assertEquals("xyzxyzxyzxyz", StringUtils.repeat("xyz", 4));
	}
}
