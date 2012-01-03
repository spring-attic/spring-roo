package org.springframework.roo.addon.cloud.foundry;

import java.util.List;
import java.util.Map;

import com.vmware.appcloud.client.AppCloudClient;
import com.vmware.appcloud.client.CloudService;
import com.vmware.appcloud.client.ServiceConfiguration;

public interface CloudFoundrySession {

    void login(String email, String password, String cloudControllerUrl);

    AppCloudClient getClient();

    List<String> getApplicationNames();

    List<String> getServiceTypes();

    List<String> getProvisionedServices();

    CloudService getProvisionedService(String provisionedServiceName);

    ServiceConfiguration getService(String serviceVendor);

    Map<String, List<String>> getBoundUrlMap();

    List<Integer> getApplicationMemoryOptions();

    List<String> getStoredEmails();

    List<String> getStoredUrls();

    void clearStoredLoginDetails();
}
