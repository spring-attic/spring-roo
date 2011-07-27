package org.springframework.roo.addon.layers.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.StringUtils;

/**
 * Unit test of the {@link RepositoryLayerMethod} enum.
 *
 * @author Andrew Swan
 * @since 1.2
 */
public class RepositoryLayerMethodTest {

	// Fixture
	@Mock private JavaType mockTargetEntity;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testNamesAreUniqueAndNotBlank() {
		final Set<String> names = new HashSet<String>();
		for (final RepositoryLayerMethod method : RepositoryLayerMethod.values()) {
			final String name = method.getName();
			names.add(name);
			assertTrue(StringUtils.hasText(name));
		}
		assertEquals(RepositoryLayerMethod.values().length, names.size());
	}
	
	@Test
	public void testParameterTypesAreNotNull() {
		for (final RepositoryLayerMethod method : RepositoryLayerMethod.values()) {
			assertNotNull(method.getParameterTypes(mockTargetEntity));
		}
	}
	
	@Test
	public void testCallFlushMethod() {
		// Invoke
		final String methodCall = RepositoryLayerMethod.FLUSH.getCall(Collections.<JavaSymbolName>emptyList());
		
		// Check
		assertEquals("flush()", methodCall);
	}
}
