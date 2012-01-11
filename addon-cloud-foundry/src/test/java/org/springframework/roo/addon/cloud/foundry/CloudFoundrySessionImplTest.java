package org.springframework.roo.addon.cloud.foundry;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit test of {@link CloudFoundrySessionImpl}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class CloudFoundrySessionImplTest {

    private static final String CUSTOM_CLOUD_URL = "http://cloud.example.com";
    private static final String EMAIL = "bob@example.com";
    private static final String PASSWORD = "letmein";

    @Mock private AppCloudClientFactory mockAppCloudClientFactory;
    @Mock private CloudPreferences mockPreferences;
    // Fixture
    private CloudFoundrySessionImpl session;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        session = new CloudFoundrySessionImpl();
        session.appCloudClientFactory = mockAppCloudClientFactory;
        session.preferences = mockPreferences;
    }

    @Test
    public void testLoginWithBlankEmailAndNoStoredEmail() {
        // Set up
        final UaaAwareAppCloudClient mockClient = mock(UaaAwareAppCloudClient.class);
        when(mockPreferences.getStoredEmails(CUSTOM_CLOUD_URL)).thenReturn(
                Collections.<String> emptyList());

        // Invoke
        session.login(null, PASSWORD, CUSTOM_CLOUD_URL);

        // Check
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    public void testLoginWithBlankEmailAndStoredEmail() {
        // Set up
        when(mockPreferences.getStoredEmails(CUSTOM_CLOUD_URL)).thenReturn(
                Collections.singletonList(EMAIL));
        final CloudCredentials credentials = new CloudCredentials(EMAIL,
                PASSWORD, CUSTOM_CLOUD_URL);
        final UaaAwareAppCloudClient mockClient = mock(UaaAwareAppCloudClient.class);
        when(mockAppCloudClientFactory.getUaaAwareInstance(credentials))
                .thenReturn(mockClient);

        // Invoke
        session.login(null, PASSWORD, CUSTOM_CLOUD_URL);

        // Check
        verify(mockClient).loginIfNeeded();
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    public void testLoginWithBlankPasswordAndNoStoredPassword() {
        // Set up
        final UaaAwareAppCloudClient mockClient = mock(UaaAwareAppCloudClient.class);
        when(mockPreferences.getStoredPassword(CUSTOM_CLOUD_URL, EMAIL))
                .thenReturn(null);

        // Invoke
        session.login(EMAIL, null, CUSTOM_CLOUD_URL);

        // Check
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    public void testLoginWithBlankPasswordAndStoredPassword() {
        // Set up
        when(mockPreferences.getStoredPassword(CUSTOM_CLOUD_URL, EMAIL))
                .thenReturn(PASSWORD);

        final CloudCredentials credentials = new CloudCredentials(EMAIL,
                PASSWORD, CUSTOM_CLOUD_URL);
        final UaaAwareAppCloudClient mockClient = mock(UaaAwareAppCloudClient.class);
        when(mockAppCloudClientFactory.getUaaAwareInstance(credentials))
                .thenReturn(mockClient);

        // Invoke
        session.login(EMAIL, null, CUSTOM_CLOUD_URL);

        // Check
        verify(mockClient).loginIfNeeded();
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    public void testLoginWithNonBlankEmailNonBlankPasswordAndCustomUrl() {
        // Set up
        final CloudCredentials credentials = new CloudCredentials(EMAIL,
                PASSWORD, CUSTOM_CLOUD_URL);
        final UaaAwareAppCloudClient mockClient = mock(UaaAwareAppCloudClient.class);
        when(mockAppCloudClientFactory.getUaaAwareInstance(credentials))
                .thenReturn(mockClient);

        // Invoke
        session.login(EMAIL, PASSWORD, CUSTOM_CLOUD_URL);

        // Check
        verify(mockClient).loginIfNeeded();
        verify(mockPreferences).storeCredentials(credentials);
        verifyNoMoreInteractions(mockClient, mockPreferences);
    }
}
