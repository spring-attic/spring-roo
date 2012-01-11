package org.springframework.roo.addon.cloud.foundry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit test of {@link CloudCredentials}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class CloudCredentialsTest {

    private static final String EMAIL = "bob@example.com";
    private static final String PASSWORD = "hello";
    private static final String URL = "http://api.example.com";
    private static final CloudCredentials CREDENTIALS = new CloudCredentials(
            EMAIL, PASSWORD, URL);

    @Test
    public void testDifferentEmailAddressIsNotSameAccount() {
        assertFalse(CREDENTIALS.isSameAccount(URL, EMAIL + "x"));
    }

    @Test
    public void testDifferentUrlIsNotSameAccount() {
        assertFalse(CREDENTIALS.isSameAccount(URL + "x", EMAIL));
    }

    @Test
    public void testEncodeAndDecodeRoundTrip() {
        // Set up
        final String encoded = CREDENTIALS.encode();

        // Invoke
        final CloudCredentials decoded = CloudCredentials.decode(encoded);

        // Check
        assertEquals(URL, decoded.getUrl());
        assertEquals(EMAIL, decoded.getEmail());
        assertEquals(PASSWORD, decoded.getPassword());
    }

    @Test
    public void testSameDetailsIsSameAccount() {
        assertTrue(CREDENTIALS.isSameAccount(URL, EMAIL));
    }
}
