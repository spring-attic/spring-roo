package org.springframework.roo.addon.cloud.foundry;

import com.vmware.appcloud.client.AppCloudClient;

/**
 * A factory for {@link AppCloudClient}s.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public interface AppCloudClientFactory {

    /**
     * Returns a UAA-aware {@link AppCloudClient} based on the given credentials
     * 
     * @param credentials the credentials to use (required)
     * @return a non-<code>null</code> instance
     */
    UaaAwareAppCloudClient getUaaAwareInstance(CloudCredentials credentials);
}