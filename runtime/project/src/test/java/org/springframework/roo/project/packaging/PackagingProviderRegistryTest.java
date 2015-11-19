package org.springframework.roo.project.packaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit test of {@link PackagingProviderRegistryImpl}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class PackagingProviderRegistryTest {

    private static final String CORE_JAR_ID = "jar";
    private static final String CORE_WAR_ID = "war";
    private static final String CUSTOM_JAR_ID = "jar_custom";

    @Mock private CorePackagingProvider mockCoreJarPackaging;
    @Mock private PackagingProvider mockCustomJarPackaging;
    @Mock private CorePackagingProvider mockWarPackaging;
    // Fixture
    private PackagingProviderRegistryImpl registry;

    @Before
    public void setUp() {
        // Mocks
        MockitoAnnotations.initMocks(this);
        setUpMockPackagingProvider(mockCoreJarPackaging, CORE_JAR_ID, true);
        setUpMockPackagingProvider(mockCustomJarPackaging, CUSTOM_JAR_ID, true);
        setUpMockPackagingProvider(mockWarPackaging, CORE_WAR_ID, false);

        // Object under test
        registry = new PackagingProviderRegistryImpl();
        registry.bindPackagingProvider(mockCoreJarPackaging);
        registry.bindPackagingProvider(mockCustomJarPackaging);
        registry.bindPackagingProvider(mockWarPackaging);
    }

    private void setUpMockPackagingProvider(
            final PackagingProvider mockPackagingProvider, final String id,
            final boolean isDefault) {
        when(mockPackagingProvider.getId()).thenReturn(id);
        when(mockPackagingProvider.isDefault()).thenReturn(isDefault);
    }

    @Test
    public void testGetAllPackagingProviders() {
        // Invoke
        final Collection<PackagingProvider> packagingProviders = registry
                .getAllPackagingProviders();

        // Check
        final List<PackagingProvider> expectedProviders = Arrays.asList(
                mockCoreJarPackaging, mockCustomJarPackaging, mockWarPackaging);
        assertEquals(expectedProviders.size(), packagingProviders.size());
        assertTrue(packagingProviders.containsAll(expectedProviders));
    }

    @Test
    public void testGetDefaultPackagingProviderWhenACustomIsDefault() {
        assertEquals(mockCustomJarPackaging,
                registry.getDefaultPackagingProvider());
    }

    @Test
    public void testGetDefaultPackagingProviderWhenNoCustomIsDefault() {
        when(mockCustomJarPackaging.isDefault()).thenReturn(false);
        assertEquals(mockCoreJarPackaging,
                registry.getDefaultPackagingProvider());
    }

    @Test
    public void testGetPackagingProviderByInvalidId() {
        assertNull(registry.getPackagingProvider("no-such-provider"));
    }

    @Test
    public void testGetPackagingProviderByValidId() {
        assertEquals(mockCustomJarPackaging,
                registry.getPackagingProvider(CUSTOM_JAR_ID));
    }
}
