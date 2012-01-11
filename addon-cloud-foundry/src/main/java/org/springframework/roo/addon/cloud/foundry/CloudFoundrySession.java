package org.springframework.roo.addon.cloud.foundry;

import java.util.List;
import java.util.Map;

import com.vmware.appcloud.client.AppCloudClient;
import com.vmware.appcloud.client.CloudService;
import com.vmware.appcloud.client.ServiceConfiguration;

public interface CloudFoundrySession {

    void clearStoredLoginDetails();

    List<Integer> getApplicationMemoryOptions();

    List<String> getApplicationNames();

    Map<String, List<String>> getBoundUrlMap();

    AppCloudClient getClient();

    CloudService getProvisionedService(String provisionedServiceName);

    List<String> getProvisionedServices();

    ServiceConfiguration getService(String serviceVendor);

    List<String> getServiceTypes();

    List<String> getStoredEmails();

    List<String> getStoredUrls();

    void login(String email, String password, String cloudControllerUrl);
}
