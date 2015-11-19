package org.springframework.roo.project.packaging;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * Convenient superclass for writing tests of concrete {@link PackagingProvider}
 * implementations.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public abstract class PackagingProviderTestCase<T extends AbstractPackagingProvider> {

    // Fixture
    private T provider;

    /**
     * Subclasses must return an instance of the provider being tested
     * 
     * @return a non-<code>null</code> instance
     */
    protected abstract T getProvider();

    @Before
    public void setUp() throws Exception {
        this.provider = getProvider();
    }

    @Test
    public void testIdIsNotBlank() {
        assertTrue(StringUtils.isNotBlank(provider.getId()));
    }

    @Test
    public void testTemplateExists() {
        // Set up
        final String pomTemplate = provider.getPomTemplate();

        // Invoke
        final URL pomTemplateUrl = provider.getClass().getResource(pomTemplate);

        // Check
        assertNotNull("Can't find POM template '" + pomTemplate + "'",
                pomTemplateUrl);
    }
}
