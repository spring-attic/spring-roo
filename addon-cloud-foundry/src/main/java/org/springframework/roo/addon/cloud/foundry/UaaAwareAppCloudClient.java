package org.springframework.roo.addon.cloud.foundry;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.json.simple.JSONObject;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.uaa.client.TransmissionEventListener;
import org.springframework.uaa.client.UaaService;
import org.springframework.uaa.client.VersionHelper;
import org.springframework.uaa.client.protobuf.UaaClient.FeatureUse;
import org.springframework.uaa.client.protobuf.UaaClient.Product;
import org.springframework.uaa.client.util.HexUtils;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;

import com.vmware.appcloud.client.AppCloudClient;
import com.vmware.appcloud.client.AppCloudException;
import com.vmware.appcloud.client.ApplicationStats;
import com.vmware.appcloud.client.CloudApplication;
import com.vmware.appcloud.client.CloudInfo;
import com.vmware.appcloud.client.CloudService;
import com.vmware.appcloud.client.CrashesInfo;
import com.vmware.appcloud.client.InstancesInfo;
import com.vmware.appcloud.client.ServiceConfiguration;
import com.vmware.appcloud.client.UploadStatusCallback;

public class UaaAwareAppCloudClient extends AppCloudClient implements
        TransmissionEventListener {

    public static final String CLOUD_FOUNDRY_URL = "http://api.cloudfoundry.com";

    private static final int CLOUD_MAJOR_VERSION = 0;

    private static final int CLOUD_MINOR_VERSION = 0;
    private static final int CLOUD_PATCH_VERSION = 0;
    private static final Product DEFAULT_PRODUCT = VersionHelper.getProduct(
            "Cloud Foundry Java API", "0.0.0.RELEASE");
    private static final int HTTP_SUCCESS_CODE = 200;

    private final Set<String> discoveredAppNames;
    // key = method name; value = sorted map of HTTP response code keys to count
    // of that response code
    private final Map<String, SortedMap<Integer, Integer>> methodToResponses = new HashMap<String, SortedMap<Integer, Integer>>();
    private final Product product;

    private final UaaService uaaService;

    /**
     * Constructor; consider using
     * {@link AppCloudClientFactory#getUaaAwareInstance(CloudCredentials)}
     * instead.
     * 
     * @param product can be <code>null</code> to use the default
     *            {@link Product}
     * @param uaaService the UAA service (required)
     * @param credentials the cloud login credentials (required)
     * @param requestFactory
     */
    public UaaAwareAppCloudClient(final Product product,
            final UaaService uaaService, final CloudCredentials credentials,
            final ClientHttpRequestFactory requestFactory) {
        super(credentials.getEmail(), credentials.getPassword(), null,
                credentials.getUrlObject(), requestFactory);
        Validate.notNull(uaaService, "UAA Service required");
        discoveredAppNames = new HashSet<String>();
        this.product = ObjectUtils.defaultIfNull(product, DEFAULT_PRODUCT);
        this.uaaService = uaaService;
    }

    public void afterTransmission(final TransmissionType type,
            final boolean successful) {
        if (type == TransmissionType.UPLOAD && successful) {
            discoveredAppNames.clear();
            methodToResponses.clear();
        }
    }

    public void beforeTransmission(final TransmissionType type) {
        if (type == TransmissionType.UPLOAD) {
            flushToUaa();
        }
    }

    @Override
    public void bindService(final String appName, final String serviceName) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.bindService(appName, serviceName);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("bindService", resultCode, appName);
        }
    }

    @Override
    public void createAndUploadAndStartApplication(final String appName,
            final String framework, final int memory, final File warFile,
            final List<String> uris, final List<String> serviceNames)
            throws IOException {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.createAndUploadAndStartApplication(appName, framework,
                    memory, warFile, uris, serviceNames);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("createAndUploadAndStartApplication", resultCode,
                    appName);
        }
    }

    @Override
    public void createApplication(final String appName, final String framework,
            final int memory, final List<String> uris,
            final List<String> serviceNames) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.createApplication(appName, framework, memory, uris,
                    serviceNames);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("createApplication", resultCode, appName);
        }
    }

    @Override
    public void createApplication(final String appName, final String framework,
            final int memory, final List<String> uris,
            final List<String> serviceNames, final boolean checkExists) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.createApplication(appName, framework, memory, uris,
                    serviceNames, checkExists);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.getMessage(), he);
        }
        finally {
            recordHttpResult("createApplication", resultCode, appName);
        }
    }

    @Override
    public void createService(final CloudService service) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.createService(service);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("createService", resultCode);
        }
    }

    public void deactivate() {
        flushToUaa();
    }

    @Override
    public void deleteAllApplications() {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.deleteAllApplications();
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("deleteAllApplications", resultCode);
        }
    }

    @Override
    public void deleteAllServices() {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.deleteAllServices();
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("deleteAllServices", resultCode);
        }
    }

    @Override
    public void deleteApplication(final String appName) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.deleteApplication(appName);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("deleteApplication", resultCode, appName);
        }
    }

    @Override
    public void deleteService(final String service) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.deleteService(service);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("deleteService", resultCode);
        }
    }

    private void flushToUaa() {
        // Store the app names being used
        for (final String appName : discoveredAppNames) {
            uaaService.registerProductUsage(product, appName);
        }

        // Store the cloud controller URL being used
        String ccType = "Cloud Controller: Custom";
        final String cloudHost = getCloudControllerUrl().getHost();
        if (CLOUD_FOUNDRY_URL.equals(getCloudControllerUrl().toExternalForm())) {
            ccType = "Cloud Controller: Public Cloud";
        }
        else if (cloudHost.equals("localhost")) {
            ccType = "Cloud Controller: Localhost";
        }
        else if (cloudHost.equals("127.0.0.1")) {
            ccType = "Cloud Controller: Localhost";
        }
        // Store the cloud controller hostname SHA 256
        final String ccUrlHashed = sha256(cloudHost);

        // Create a feature use record for the cloud controller
        final Map<String, Object> ccJson = new HashMap<String, Object>();
        ccJson.put("type", "cc_info");
        ccJson.put("cc_hostname_sha256", JSONObject.escape(ccUrlHashed));
        registerFeatureUse(ccType, ccJson);

        // Crate feature uses for each method name
        for (final String methodName : methodToResponses.keySet()) {
            final SortedMap<Integer, Integer> resultCounts = methodToResponses
                    .get(methodName);
            final Map<String, Object> methodCallInfo = new HashMap<String, Object>();
            methodCallInfo.put("type", "method_call_info");
            methodCallInfo.put("cc_hostname_sha256",
                    JSONObject.escape(ccUrlHashed));
            methodCallInfo.put("http_results_to_counts", resultCounts);
            registerFeatureUse(methodName, methodCallInfo);
        }
    }

    @Override
    public CloudApplication getApplication(final String appName) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            return super.getApplication(appName);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("getApplication", resultCode, appName);
        }
    }

    @Override
    public InstancesInfo getApplicationInstances(final String appName) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            return super.getApplicationInstances(appName);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("getApplicationInstances", resultCode, appName);
        }
    }

    @Override
    public int[] getApplicationMemoryChoices() {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            return super.getApplicationMemoryChoices();
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("getApplicationMemoryChoices", resultCode);
        }
    }

    @Override
    public List<CloudApplication> getApplications() {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            return super.getApplications();
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("getApplications", resultCode);
        }
    }

    @Override
    public ApplicationStats getApplicationStats(final String appName) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            return super.getApplicationStats(appName);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("getApplicationStats", resultCode, appName);
        }
    }

    @Override
    public URL getCloudControllerUrl() {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            return super.getCloudControllerUrl();
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("getCloudControllerUrl", resultCode);
        }
    }

    @Override
    public CloudInfo getCloudInfo() {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            return super.getCloudInfo();
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("getCloudInfo", resultCode);
        }
    }

    @Override
    public CrashesInfo getCrashes(final String appName) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            return super.getCrashes(appName);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("getCrashes", resultCode, appName);
        }
    }

    @Override
    public int getDefaultApplicationMemory(final String framework) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            return super.getDefaultApplicationMemory(framework);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("getDefaultApplicationMemory", resultCode);
        }
    }

    @Override
    public String getFile(final String appName, final int instanceIndex,
            final String filePath) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            return super.getFile(appName, instanceIndex, filePath);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("getFile", resultCode, appName);
        }
    }

    @Override
    public <T> T getFile(final String appName, final int instanceIndex,
            final String filePath, final RequestCallback requestCallback,
            final ResponseExtractor<T> responseHandler) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            return super.getFile(appName, instanceIndex, filePath,
                    requestCallback, responseHandler);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("getFile", resultCode, appName);
        }
    }

    @Override
    public CloudService getService(final String service) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            return super.getService(service);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("getService", resultCode);
        }
    }

    @Override
    public List<ServiceConfiguration> getServiceConfigurations() {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            return super.getServiceConfigurations();
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("getServiceConfigurations", resultCode);
        }
    }

    @Override
    public List<CloudService> getServices() {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            return super.getServices();
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("getServices", resultCode);
        }
    }

    @Override
    public String login() {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            return super.login();
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("login", resultCode);
        }
    }

    @Override
    public String loginIfNeeded() {
        int resultCode = 200;
        try {
            return super.loginIfNeeded();
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("loginIfNeeded", resultCode);
        }
    }

    private void recordHttpResult(final String methodName, final int resultCode) {
        recordHttpResult(methodName, resultCode, null);
    }

    private void recordHttpResult(final String methodName,
            final int resultCode, final String appName) {
        if (appName != null) {
            discoveredAppNames.add(appName);
        }
        SortedMap<Integer, Integer> results = methodToResponses.get(methodName);
        if (results == null) {
            results = new TreeMap<Integer, Integer>();
            methodToResponses.put(methodName, results);
        }
        Integer countSoFar = results.get(resultCode);
        if (countSoFar == null) {
            countSoFar = 0;
        }
        results.put(resultCode, countSoFar + 1);
    }

    @Override
    public void register(final String email, final String password) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.register(email, password);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("register", resultCode);
        }
    }

    private void registerFeatureUse(final String featureName,
            final Map<String, Object> jsonPayload) {
        jsonPayload.put("version",
                product.getMajorVersion() + "." + product.getMinorVersion()
                        + "." + product.getPatchVersion());
        final String jsonAsString = JSONObject.toJSONString(jsonPayload);
        final FeatureUse featureToRegister = FeatureUse.newBuilder()
                .setName(featureName)
                .setDateLastUsed(System.currentTimeMillis())
                .setMajorVersion(CLOUD_MAJOR_VERSION)
                .setMinorVersion(CLOUD_MINOR_VERSION)
                .setPatchVersion(CLOUD_PATCH_VERSION).build();
        try {
            uaaService.registerFeatureUsage(product, featureToRegister,
                    jsonAsString.getBytes("UTF-8"));
        }
        catch (final UnsupportedEncodingException ignore) {
        }
    }

    @Override
    public void rename(final String appName, final String newName) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.rename(appName, newName);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("rename", resultCode, appName);
        }
    }

    @Override
    public void restartApplication(final String appName) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.restartApplication(appName);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("restartApplication", resultCode, appName);
        }
    }

    private String sha256(final String input) {
        try {
            final MessageDigest sha1 = MessageDigest.getInstance("SHA-256");
            final byte[] digest = sha1.digest(input.getBytes("UTF-8"));
            return HexUtils.toHex(digest);
        }
        catch (final NoSuchAlgorithmException e) {
            // This can't happen as we know that there is an SHA-256 algorithm
        }
        catch (final UnsupportedEncodingException e) {
            // This can't happen as we know that there is an UTF-8 encoding
        }
        return null;
    }

    @Override
    public void startApplication(final String appName) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.startApplication(appName);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("startApplication", resultCode, appName);
        }
    }

    @Override
    public void stopApplication(final String appName) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.stopApplication(appName);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("stopApplication", resultCode, appName);
        }
    }

    @Override
    public void unbindService(final String appName, final String serviceName) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.unbindService(appName, serviceName);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("unbindService", resultCode, appName);
        }
    }

    @Override
    public void unregister() {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.unregister();
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("unregister", resultCode);
        }
    }

    @Override
    public void updateApplicationInstances(final String appName,
            final int instances) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.updateApplicationInstances(appName, instances);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("updateApplicationInstances", resultCode, appName);
        }
    }

    @Override
    public void updateApplicationMemory(final String appName, final int memory) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.updateApplicationMemory(appName, memory);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("updateApplicationMemory", resultCode, appName);
        }
    }

    @Override
    public void updateApplicationServices(final String appName,
            final List<String> services) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.updateApplicationServices(appName, services);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("updateApplicationServices", resultCode, appName);
        }
    }

    @Override
    public void updateApplicationUris(final String appName,
            final List<String> uris) {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.updateApplicationUris(appName, uris);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("updateApplicationUris", resultCode, appName);
        }
    }

    @Override
    public void uploadApplication(final String appName, final File warFile)
            throws IOException {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.uploadApplication(appName, warFile);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("uploadApplication", resultCode, appName);
        }
    }

    @Override
    public void uploadApplication(final String appName, final File warFile,
            final UploadStatusCallback callback) throws IOException {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.uploadApplication(appName, warFile, callback);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("uploadApplication", resultCode, appName);
        }
    }

    @Override
    public void uploadApplication(final String appName, final String warFilePath)
            throws IOException {
        int resultCode = HTTP_SUCCESS_CODE;
        try {
            super.uploadApplication(appName, warFilePath);
        }
        catch (final AppCloudException he) {
            resultCode = he.getStatusCode().value();
            throw new IllegalStateException(
                    "Operation could not be completed: " + he.toString(), he);
        }
        finally {
            recordHttpResult("uploadApplication", resultCode, appName);
        }
    }
}
