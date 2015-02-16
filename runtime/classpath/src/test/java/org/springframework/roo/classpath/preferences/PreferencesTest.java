package org.springframework.roo.classpath.preferences;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit test of the {@link Preferences} class.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class PreferencesTest {

    private static final String INVALID_KEY = "this-key-does-not-exist";

    // Fixture
    @Mock private java.util.prefs.Preferences mockPreferences;
    private Preferences preferences;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        preferences = new Preferences(mockPreferences);
    }

    @Test
    public void testGetByteArrayWithNullKey() {
        assertNull(preferences.getByteArray(null));
    }

    @Test
    public void testGetByteArrayWithUnknownKeyAndNoDefault() {
        // Set up
        final byte[] expectedValue = { 1, 2, 3 }; // Arbitrary
        when(mockPreferences.getByteArray(INVALID_KEY, new byte[0]))
                .thenReturn(expectedValue);

        // Invoke
        final byte[] actualValue = preferences.getByteArray(INVALID_KEY);

        // Check
        assertSame(expectedValue, actualValue);
    }
}
