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

public class UaaAwareAppCloudClient extends AppCloudClient implements TransmissionEventListener {
	public final static String CLOUD_FOUNDRY_URL = "http://api.cloudfoundry.com";
	private final static int HTTP_SUCCESS_CODE = 200;
	private UaaService uaaService;
	private Set<String> discoveredAppNames = new HashSet<String>();
	private URL cloudControllerUrl;
	
	/**
	 * key: method name, value: sorted map of HTTP response code keys to count of that response code
	 */
	private Map<String, SortedMap<Integer, Integer>> methodToResponses = new HashMap<String, SortedMap<Integer, Integer>>();
	
	private Product product = VersionHelper.getProduct("Cloud Foundry Java API", "0.0.0.RELEASE");
	private int cloudMajorVersion = 0;
	private int cloudMinorVersion = 0;
	private int cloudPatchVersion = 0;

	public UaaAwareAppCloudClient(Product product, UaaService _uaaService, String email, String password, String token, URL cloudControllerUrl, ClientHttpRequestFactory requestFactory) {
		super(email, password, token, cloudControllerUrl, requestFactory);
		this.uaaService = _uaaService;
		this.cloudControllerUrl = cloudControllerUrl;
		if (product != null) {
			this.product = product;
		}
	}

	public void deactivate() {
		flushToUaa();
	}

	public void afterTransmission(TransmissionType type, boolean successful) {
		if (type == TransmissionType.UPLOAD && successful) {
			discoveredAppNames.clear();
			methodToResponses.clear();
		}
	}

	public void beforeTransmission(TransmissionType type) {
		if (type == TransmissionType.UPLOAD) {
			flushToUaa();
		}
	}

	private void flushToUaa() {
		// Store the app names being used
		for (String appName : discoveredAppNames) {
			uaaService.registerProductUsage(product, appName);
		}

		// Store the cloud controller URL being used
		String ccType = "Cloud Controller: Custom";
		if (CLOUD_FOUNDRY_URL.equals(cloudControllerUrl.toExternalForm())) {
			ccType = "Cloud Controller: Public Cloud";
		} else if (cloudControllerUrl.getHost().equals("localhost")) {
			ccType = "Cloud Controller: Localhost";
		} else if (cloudControllerUrl.getHost().equals("127.0.0.1")) {
			ccType = "Cloud Controller: Localhost";
		}
		// Store the cloud controller hostname SHA 256
		String ccUrlHashed = sha256(cloudControllerUrl.getHost());

		// Create a feature use record for the cloud controller
		Map<String, Object> ccJson = new HashMap<String, Object>();
		ccJson.put("type", "cc_info");
		ccJson.put("cc_hostname_sha256", JSONObject.escape(ccUrlHashed));
		registerFeatureUse(ccType, ccJson);

		// Crate feature uses for each method name
		for (String methodName : methodToResponses.keySet()) {
			SortedMap<Integer, Integer> resultCounts = methodToResponses.get(methodName);
			Map<String, Object> methodCallInfo = new HashMap<String, Object>();
			methodCallInfo.put("type", "method_call_info");
			methodCallInfo.put("cc_hostname_sha256", JSONObject.escape(ccUrlHashed));
			methodCallInfo.put("http_results_to_counts", resultCounts);
			registerFeatureUse(methodName, methodCallInfo);
		}
	}

	private void registerFeatureUse(String featureName, Map<String, Object> jsonPayload) {
		jsonPayload.put("version", product.getMajorVersion() + "." + product.getMinorVersion() + "." + product.getPatchVersion());
		String jsonAsString = JSONObject.toJSONString(jsonPayload);
		FeatureUse featureToRegister = FeatureUse.newBuilder().setName(featureName).setDateLastUsed(System.currentTimeMillis()).setMajorVersion(cloudMajorVersion).setMinorVersion(cloudMinorVersion).setPatchVersion(cloudPatchVersion).build();
		try {
			uaaService.registerFeatureUsage(product, featureToRegister, jsonAsString.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException ignore) {}
	}

	private String sha256(String input) {
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-256");
			byte[] digest = sha1.digest(input.getBytes("UTF-8"));
			return HexUtils.toHex(digest);
		} catch (NoSuchAlgorithmException e) {
			// This can't happen as we know that there is an SHA-256 algorithm
		} catch (UnsupportedEncodingException e) {
			// This can't happen as we know that there is an UTF-8 encoding
		}
		return null;
	}

	private void recordHttpResult(String methodName, int resultCode) {
		recordHttpResult(methodName, resultCode, null);
	}

	private void recordHttpResult(String methodName, int resultCode, String appName) {
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
	public void bindService(String appName, String serviceName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.bindService(appName, serviceName);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("bindService", resultCode, appName);
		}
	}

	@Override
	public void createAndUploadAndStartApplication(String appName, String framework, int memory, File warFile, List<String> uris, List<String> serviceNames) throws IOException {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.createAndUploadAndStartApplication(appName, framework, memory, warFile, uris, serviceNames);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("createAndUploadAndStartApplication", resultCode, appName);
		}
	}

	@Override
	public void createApplication(String appName, String framework, int memory, List<String> uris, List<String> serviceNames, boolean checkExists) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.createApplication(appName, framework, memory, uris, serviceNames, checkExists);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.getMessage(), he);
		} finally {
			recordHttpResult("createApplication", resultCode, appName);
		}
	}

	@Override
	public void createApplication(String appName, String framework, int memory, List<String> uris, List<String> serviceNames) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.createApplication(appName, framework, memory, uris, serviceNames);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("createApplication", resultCode, appName);
		}
	}

	@Override
	public void createService(CloudService service) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.createService(service);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("createService", resultCode);
		}
	}

	@Override
	public void deleteAllApplications() {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.deleteAllApplications();
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("deleteAllApplications", resultCode);
		}
	}

	@Override
	public void deleteAllServices() {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.deleteAllServices();
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("deleteAllServices", resultCode);
		}
	}

	@Override
	public void deleteApplication(String appName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.deleteApplication(appName);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("deleteApplication", resultCode, appName);
		}
	}

	@Override
	public void deleteService(String service) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.deleteService(service);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("deleteService", resultCode);
		}
	}

	@Override
	public CloudApplication getApplication(String appName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getApplication(appName);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("getApplication", resultCode, appName);
		}
	}

	@Override
	public InstancesInfo getApplicationInstances(String appName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getApplicationInstances(appName);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("getApplicationInstances", resultCode, appName);
		}
	}

	@Override
	public int[] getApplicationMemoryChoices() {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getApplicationMemoryChoices();
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("getApplicationMemoryChoices", resultCode);
		}
	}

	@Override
	public List<CloudApplication> getApplications() {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getApplications();
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("getApplications", resultCode);
		}
	}

	@Override
	public ApplicationStats getApplicationStats(String appName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getApplicationStats(appName);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("getApplicationStats", resultCode, appName);
		}
	}

	@Override
	public URL getCloudControllerUrl() {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getCloudControllerUrl();
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("getCloudControllerUrl", resultCode);
		}
	}

	@Override
	public CloudInfo getCloudInfo() {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getCloudInfo();
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("getCloudInfo", resultCode);
		}
	}

	@Override
	public CrashesInfo getCrashes(String appName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getCrashes(appName);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("getCrashes", resultCode, appName);
		}
	}

	@Override
	public int getDefaultApplicationMemory(String framework) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getDefaultApplicationMemory(framework);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("getDefaultApplicationMemory", resultCode);
		}
	}

	@Override
	public <T> T getFile(String appName, int instanceIndex, String filePath, RequestCallback requestCallback, ResponseExtractor<T> responseHandler) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getFile(appName, instanceIndex, filePath, requestCallback, responseHandler);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("getFile", resultCode, appName);
		}
	}

	@Override
	public String getFile(String appName, int instanceIndex, String filePath) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getFile(appName, instanceIndex, filePath);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("getFile", resultCode, appName);
		}
	}

	@Override
	public CloudService getService(String service) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getService(service);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("getService", resultCode);
		}
	}

	@Override
	public List<ServiceConfiguration> getServiceConfigurations() {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getServiceConfigurations();
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("getServiceConfigurations", resultCode);
		}
	}

	@Override
	public List<CloudService> getServices() {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.getServices();
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("getServices", resultCode);
		}
	}

	@Override
	public String login() {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			return super.login();
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("login", resultCode);
		}
	}

	@Override
	public String loginIfNeeded() {
		int resultCode = 200;
		try {
			return super.loginIfNeeded();
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("loginIfNeeded", resultCode);
		}
	}

	@Override
	public void register(String email, String password) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.register(email, password);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("register", resultCode);
		}
	}

	@Override
	public void rename(String appName, String newName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.rename(appName, newName);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("rename", resultCode, appName);
		}
	}

	@Override
	public void restartApplication(String appName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.restartApplication(appName);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("restartApplication", resultCode, appName);
		}
	}

	@Override
	public void startApplication(String appName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.startApplication(appName);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("startApplication", resultCode, appName);
		}
	}

	@Override
	public void stopApplication(String appName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.stopApplication(appName);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("stopApplication", resultCode, appName);
		}
	}

	@Override
	public void unbindService(String appName, String serviceName) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.unbindService(appName, serviceName);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("unbindService", resultCode, appName);
		}
	}

	@Override
	public void unregister() {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.unregister();
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("unregister", resultCode);
		}
	}

	@Override
	public void updateApplicationInstances(String appName, int instances) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.updateApplicationInstances(appName, instances);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("updateApplicationInstances", resultCode, appName);
		}
	}

	@Override
	public void updateApplicationMemory(String appName, int memory) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.updateApplicationMemory(appName, memory);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("updateApplicationMemory", resultCode, appName);
		}
	}

	@Override
	public void updateApplicationServices(String appName, List<String> services) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.updateApplicationServices(appName, services);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("updateApplicationServices", resultCode, appName);
		}
	}

	@Override
	public void updateApplicationUris(String appName, List<String> uris) {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.updateApplicationUris(appName, uris);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("updateApplicationUris", resultCode, appName);
		}
	}

	@Override
	public void uploadApplication(String appName, File warFile, UploadStatusCallback callback) throws IOException {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.uploadApplication(appName, warFile, callback);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("uploadApplication", resultCode, appName);
		}
	}

	@Override
	public void uploadApplication(String appName, File warFile) throws IOException {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.uploadApplication(appName, warFile);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("uploadApplication", resultCode, appName);
		}
	}

	@Override
	public void uploadApplication(String appName, String warFilePath) throws IOException {
		int resultCode = HTTP_SUCCESS_CODE;
		try {
			super.uploadApplication(appName, warFilePath);
		} catch (AppCloudException he) {
			resultCode = he.getStatusCode().value();
			throw new IllegalStateException("Operation could not be completed: " + he.toString(), he);
		} finally {
			recordHttpResult("uploadApplication", resultCode, appName);
		}
	}
}
