package org.springframework.roo.addon.cloud.foundry;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.uaa.client.UaaService;

/**
 * Unit test of {@link AppCloudClientFactoryImpl}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class AppCloudClientFactoryImplTest {

    // Fixture
    private AppCloudClientFactoryImpl factory;
    @Mock private UaaService mockUaaService;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        factory = new AppCloudClientFactoryImpl();
        factory.uaaService = mockUaaService;
    }

    @Test
    public void testGetInstance() throws Exception {
        // Set up
        final CloudCredentials mockCredentials = mock(CloudCredentials.class);

        // final, can't be mocked
        final URL url = new URL("http://www.springsource.org");
        when(mockCredentials.getUrlObject()).thenReturn(url);

        // Invoke
        final UaaAwareAppCloudClient instance = factory
                .getUaaAwareInstance(mockCredentials);

        // Check
        assertNotNull(instance);
    }
}
