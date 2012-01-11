package org.springframework.roo.addon.cloud.foundry;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.addon.cloud.foundry.model.CloudControllerUrl;

/**
 * Unit test of {@link CloudFoundryCommands}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class CloudFoundryCommandsTest {

    // Fixture
    private CloudFoundryCommands commands;
    @Mock private CloudFoundryOperations mockCloudFoundryOperations;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        commands = new CloudFoundryCommands();
        commands.cloudFoundryOperations = mockCloudFoundryOperations;
    }

    @Test
    public void testLoginWithCustomUrl() {
        // Set up
        final CloudControllerUrl mockUrl = mock(CloudControllerUrl.class);
        final String customUrl = "http://api.lalyos.info";
        when(mockUrl.getUrl()).thenReturn(customUrl);

        // Invoke
        commands.login(null, null, mockUrl);

        // Check
        verify(mockCloudFoundryOperations).login(null, null, customUrl);
        verifyNoMoreInteractions(mockCloudFoundryOperations);
    }

    @Test
    public void testLoginWithDefaultParameters() {
        // Invoke
        commands.login(null, null, null);

        // Check
        verify(mockCloudFoundryOperations).login(null, null, null);
        verifyNoMoreInteractions(mockCloudFoundryOperations);
    }
}
