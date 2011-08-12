package org.springframework.roo.support.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * Unit test of {@link CollectionUtils}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class CollectionUtilsTest {

	// A simple filter for testing the filtering methods
	private static final Filter<String> NON_BLANK_FILTER = new Filter<String>() {
		public boolean include(final String instance) {
			return StringUtils.hasText(instance);
		}
	};

	@Test
	public void testFilterNullCollection() {
		assertEquals(0, CollectionUtils.filter(null, NON_BLANK_FILTER).size());
	}

	@Test
	public void testFilterNonNullIterableWithNullFilter() {
		// Set up
		final Iterable<String> inputs = Arrays.asList("a", "");
		
		// Invoke
		final List<? extends String> results = CollectionUtils.filter(inputs, null);
		
		// Check
		assertEquals(inputs, results);
	}

	@Test
	public void testFilterNonNullIterableWithNonNullFilter() {
		// Set up
		final Iterable<String> inputs = Arrays.asList("a", "", null, "b");
		
		// Invoke
		final List<? extends String> results = CollectionUtils.filter(inputs, NON_BLANK_FILTER);
		
		// Check
		assertEquals(Arrays.asList("a", "b"), results);
	}
}
