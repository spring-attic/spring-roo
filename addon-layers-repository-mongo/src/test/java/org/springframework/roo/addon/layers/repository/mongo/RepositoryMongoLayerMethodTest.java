package org.springframework.roo.addon.layers.repository.mongo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.StringUtils;

/**
 * Unit test of the {@link RepositoryMongoLayerMethod} enum.
 *
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public class RepositoryMongoLayerMethodTest {

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
		for (final RepositoryMongoLayerMethod method : RepositoryMongoLayerMethod.values()) {
			final String name = method.getName();
			names.add(name);
			assertTrue(StringUtils.hasText(name));
		}
		assertEquals(RepositoryMongoLayerMethod.values().length, names.size());
	}

	@Test
	public void testParameterTypesAreNotNull() {
		for (final RepositoryMongoLayerMethod method : RepositoryMongoLayerMethod.values()) {
			assertNotNull(method.getParameterTypes(mockTargetEntity, mockIdType));
		}
	}
}
