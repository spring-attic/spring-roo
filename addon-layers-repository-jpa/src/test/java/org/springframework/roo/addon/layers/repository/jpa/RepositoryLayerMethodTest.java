package org.springframework.roo.addon.layers.repository.jpa;

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
import org.springframework.roo.addon.layers.repository.jpa.RepositoryJpaLayerMethod;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.StringUtils;

/**
 * Unit test of the {@link RepositoryLayerMethod} enum.
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class RepositoryLayerMethodTest {

	// Fixture
	@Mock private JavaType mockTargetEntity;
	@Mock private JavaType mockIdType;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testNamesAreUniqueAndNotBlank() {
		final Set<String> names = new HashSet<String>();
		for (final RepositoryJpaLayerMethod method : RepositoryJpaLayerMethod.values()) {
			final String name = method.getName();
			names.add(name);
			assertTrue(StringUtils.hasText(name));
		}
		assertEquals(RepositoryJpaLayerMethod.values().length, names.size());
	}
	
	@Test
	public void testParameterTypesAreNotNull() {
		for (final RepositoryJpaLayerMethod method : RepositoryJpaLayerMethod.values()) {
			assertNotNull(method.getParameterTypes(mockTargetEntity, mockIdType));
		}
	}
	
	@Test
	public void testCallFlushMethod() {
		// Invoke
		final String methodCall = RepositoryJpaLayerMethod.FLUSH.getCall(Collections.<JavaSymbolName>emptyList());
		
		// Check
		assertEquals("flush()", methodCall);
	}
}
