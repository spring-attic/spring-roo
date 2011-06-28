package org.springframework.roo.addon.entity;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;
import org.springframework.roo.project.layers.LayerProvider;
import org.springframework.roo.support.util.ClassUtils;

/**
 * Unit test of {@link EntityLayerProvider}
 *
 * @author Andrew Swan
 * @since 1.2
 */
public class EntityLayerProviderTest {

	@Test
	public void testEntityLayerProviderIsALayerProvider() {
		// Invoke
		@SuppressWarnings("rawtypes")
		final Set<Class> interfaces = ClassUtils.getAllInterfacesForClassAsSet(EntityLayerProvider.class);
		
		// Check
		assertTrue(interfaces.contains(LayerProvider.class));
	}
}
