package org.springframework.roo.addon.cloud.foundry.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.roo.addon.cloud.foundry.model.CloudControllerUrl;

/**
 * Unit test of {@link CloudControllerUrlConverter}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class CloudControllerUrlConverterTest {

    // Fixture
    private CloudControllerUrlConverter converter;

    @Before
    public void setUp() throws Exception {
        converter = new CloudControllerUrlConverter();
    }

    @Test
    public void testConvertEmptyUrl() {
        // Invoke
        final CloudControllerUrl url = converter
                .convertFromText("", null, null);

        // Check
        assertNull(url);
    }

    @Test
    public void testConvertNonEmptyUrl() {
        // Set up
        final String value = "http://api.lalyos.info";

        // Invoke
        final CloudControllerUrl url = converter.convertFromText(value, null,
                null);

        // Check
        assertEquals(value, url.getUrl());
    }

    @Test
    public void testConvertNullUrl() {
        // Invoke
        final CloudControllerUrl url = converter.convertFromText(null, null,
                null);

        // Check
        assertNull(url);
    }
}
