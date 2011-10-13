package org.springframework.roo.support.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

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
	
	@Test
	public void testPrefixNullWithNull() {
		assertNull(StringUtils.prefix(null, null));
	}
	
	@Test
	public void testPrefixNullWithEmpty() {
		assertNull(StringUtils.prefix(null, ""));
	}
	
	@Test
	public void testPrefixNullWithNonEmpty() {
		assertNull(StringUtils.prefix(null, "anything"));
	}
	
	@Test
	public void testPrefixEmptyWithNull() {
		assertEquals("", StringUtils.prefix("", null));
	}
	
	@Test
	public void testPrefixEmptyWithEmpty() {
		assertEquals("", StringUtils.prefix("", ""));
	}
	
	@Test
	public void testPrefixEmptyWithNonEmpty() {
		assertEquals("x", StringUtils.prefix("", "x"));
	}
	
	@Test
	public void testPrefixNonEmptyWithNewPrefix() {
		assertEquals("pre-old", StringUtils.prefix("old", "pre-"));
	}
	
	@Test
	public void testPrefixNonEmptyWithExistingPrefix() {
		assertEquals("pre-old", StringUtils.prefix("pre-old", "pre-"));
	}
	
	@Test
	public void testRemoveNullSuffixFromNullString() {
		assertNull(StringUtils.removeSuffix(null, null));
	}
	
	@Test
	public void testRemoveEmptySuffixFromNullString() {
		assertNull(StringUtils.removeSuffix(null, ""));
	}
	
	@Test
	public void testRemoveNonEmptySuffixFromNullString() {
		assertNull(StringUtils.removeSuffix(null, "anything"));
	}
	
	@Test
	public void testRemoveNullSuffixFromEmptyString() {
		assertEquals("", StringUtils.removeSuffix("", null));
	}
	
	@Test
	public void testRemoveEmptySuffixFromEmptyString() {
		assertEquals("", StringUtils.removeSuffix("", ""));
	}
	
	@Test
	public void testRemoveNonEmptySuffixFromEmptyString() {
		assertEquals("", StringUtils.removeSuffix("", "anything"));
	}
	
	@Test
	public void testRemoveMatchingSuffixFromString() {
		assertEquals("a", StringUtils.removeSuffix("abc", "bc"));
	}	
	
	@Test
	public void testRemoveNonMatchingSuffixFromString() {
		assertEquals("abc", StringUtils.removeSuffix("abc", "BC"));
	}
	
	@Test
	public void testRemoveNullPrefixFromNullString() {
		assertNull(StringUtils.removePrefix(null, null));
	}
	
	@Test
	public void testRemoveEmptyPrefixFromNullString() {
		assertNull(StringUtils.removePrefix(null, ""));
	}
	
	@Test
	public void testRemoveNonEmptyPrefixFromNullString() {
		assertNull(StringUtils.removePrefix(null, "anything"));
	}
	
	@Test
	public void testRemoveNullPrefixFromEmptyString() {
		assertEquals("", StringUtils.removePrefix("", null));
	}
	
	@Test
	public void testRemoveEmptyPrefixFromEmptyString() {
		assertEquals("", StringUtils.removePrefix("", ""));
	}
	
	@Test
	public void testRemoveNonEmptyPrefixFromEmptyString() {
		assertEquals("", StringUtils.removePrefix("", "anything"));
	}
	
	@Test
	public void testRemoveMatchingPrefixFromString() {
		assertEquals("c", StringUtils.removePrefix("abc", "ab"));
	}	
	
	@Test
	public void testRemoveNonMatchingPrefixFromString() {
		assertEquals("abc", StringUtils.removePrefix("abc", "AB"));
	}

	@Test
	public void testSuffixNullWithNull() {
		assertNull(StringUtils.suffix(null, null));
	}
	
	@Test
	public void testSuffixNullWithEmpty() {
		assertNull(StringUtils.suffix(null, ""));
	}
	
	@Test
	public void testSuffixNullWithNonEmpty() {
		assertNull(StringUtils.suffix(null, "anything"));
	}
	
	@Test
	public void testSuffixEmptyWithNull() {
		assertEquals("", StringUtils.suffix("", null));
	}
	
	@Test
	public void testSuffixEmptyWithEmpty() {
		assertEquals("", StringUtils.suffix("", ""));
	}
	
	@Test
	public void testSuffixEmptyWithNonEmpty() {
		assertEquals("x", StringUtils.suffix("", "x"));
	}
	
	@Test
	public void testSuffixNonEmptyWithNewSuffix() {
		assertEquals("old-suf", StringUtils.suffix("old", "-suf"));
	}
	
	@Test
	public void testSuffixNonEmptyWithExistingSuffix() {
		assertEquals("old-suf", StringUtils.suffix("old-suf", "-suf"));
	}
	
	@Test
	public void testNullEqualsNull() {
		assertTrue(StringUtils.equals(null, null));
	}
	
	@Test
	public void testEmptyDoesNotEqualNull() {
		assertFalse(StringUtils.equals("", null));
	}
	
	@Test
	public void testNullDoesNotEqualEmpty() {
		assertFalse(StringUtils.equals(null, ""));
	}
	
	@Test
	public void testUpperDoesNotEqualLower() {
		assertFalse(StringUtils.equals("E", "e"));
	}
	
	@Test
	public void testStringEqualsItself() {
		assertTrue(StringUtils.equals("a", "a"));
	}
	
	@Test
	public void testNullCollectionToDelimitedString() {
		assertEquals("", StringUtils.collectionToDelimitedString(null, "anything"));
	}
	
	@Test
	public void testEmptyCollectionToDelimitedString() {
		assertEquals("", StringUtils.collectionToDelimitedString(Collections.emptySet(), "anything"));
	}
	
	@Test
	public void testSingletonCollectionToDelimitedString() {
		assertEquals("foo", StringUtils.collectionToDelimitedString(Collections.singleton("foo"), "anything"));
	}
	
	@Test
	public void testDoubletonCollectionToDelimitedString() {
		assertEquals("foo:bar", StringUtils.collectionToDelimitedString(Arrays.asList("foo", "bar"), ":"));
	}
}
