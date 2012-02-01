package org.springframework.roo.addon.layers.repository.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.model.JavaType;

/**
 * Unit test of the {@link RepositoryLayerMethod} enum.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class RepositoryLayerMethodTest {

    @Mock private JavaType mockIdType;
    // Fixture
    @Mock private JavaType mockTargetEntity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCallFlushMethod() {
        // Invoke
        final String methodCall = RepositoryJpaLayerMethod.FLUSH
                .getCall(Collections.<MethodParameter> emptyList());

        // Check
        assertEquals("flush()", methodCall);
    }

    @Test
    public void testNamesAreUniqueAndNotBlank() {
        final Set<String> names = new HashSet<String>();
        for (final RepositoryJpaLayerMethod method : RepositoryJpaLayerMethod
                .values()) {
            final String name = method.getName();
            names.add(name);
            assertTrue(StringUtils.isNotBlank(name));
        }
        assertEquals(RepositoryJpaLayerMethod.values().length, names.size());
    }

    @Test
    public void testParameterTypesAreNotNull() {
        for (final RepositoryJpaLayerMethod method : RepositoryJpaLayerMethod
                .values()) {
            assertNotNull(method
                    .getParameterTypes(mockTargetEntity, mockIdType));
        }
    }
}
