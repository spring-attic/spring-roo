package org.springframework.roo.addon.cloud.foundry;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.roo.support.util.Base64;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.uaa.client.TransmissionAwareUaaService;
import org.springframework.uaa.client.TransmissionEventListener;
import org.springframework.uaa.client.UaaService;
import org.springframework.uaa.client.VersionHelper;
import org.springframework.uaa.client.internal.BasicProxyService;
import org.springframework.uaa.client.protobuf.UaaClient;

import com.vmware.appcloud.client.AppCloudClient;
import com.vmware.appcloud.client.CloudApplication;
import com.vmware.appcloud.client.CloudService;
import com.vmware.appcloud.client.ServiceConfiguration;

@Component
@Service
public class CloudFoundrySessionImpl implements CloudFoundrySession, TransmissionEventListener {
	private static final Logger logger = Logger.getLogger(CloudFoundryOperationsImpl.class.getName());
	private static final String EMPTY_STRING = "";
	private static final String EMAIL_KEY = "email";
	private static final String PASSWORD_KEY = "password";
	private static final String URL_KEY = "url";
	private static final String ROO_KEY = "Roo == Java + Productivity";
	private static final String VCLOUD_KEY = "vCloud Prefs";
	
	@Reference private UaaService uaaService;
	private Preferences preferences = getPreferencesFor(CloudFoundrySessionImpl.class);
	UaaClient.Product product = VersionHelper.getProduct("Cloud Foundry Java API", "0.0.0.RELEASE");
	private UaaAwareAppCloudClient client;
	private List<String> appNames = new ArrayList<String>();
	private List<String> provisionedServices = new ArrayList<String>();
	private List<String> serviceTypes = new ArrayList<String>();
	private Map<String, List<String>> boundUrlMap = new HashMap<String, List<String>>();
	private List<Integer> memoryOptions = new LinkedList<Integer>();

	protected void deactivate(ComponentContext cc) {
		if (uaaService instanceof TransmissionAwareUaaService) {
			((TransmissionAwareUaaService) uaaService).removeTransmissionEventListener(this);
		}

		if (client != null) {
			client.deactivate();
		}
		try {
			preferences.flush();
		} catch (BackingStoreException ignored) {
		}
	}

	protected void activate(ComponentContext context) {
		// TODO: Replace with call to VersionHelper.getProductFromDictionary(..) available in UAA 1.0.3
		@SuppressWarnings("rawtypes")
		Dictionary d = context.getBundleContext().getBundle().getHeaders();
		Object bundleVersion = d.get("Bundle-Version");
		Object gitCommitHash = d.get("Git-Commit-Hash");
		product = VersionHelper.getProduct("Cloud Foundry Java API", bundleVersion == null ? "0.0.0.RELEASE" : bundleVersion.toString(), gitCommitHash == null ? null : gitCommitHash.toString());
		if (uaaService instanceof TransmissionAwareUaaService) {
			((TransmissionAwareUaaService) uaaService).addTransmissionEventListener(this);
		}
	}

	public void beforeTransmission(TransmissionType type) {
		if (client != null) {
			client.beforeTransmission(type);
		}
	}

	public void afterTransmission(TransmissionType type, boolean successful) {
		if (client != null) {
			client.afterTransmission(type, successful);
		}
	}

	public void login(String email, String password, String cloudControllerUrl) {
		boolean storeCredentials = StringUtils.hasText(email) && StringUtils.hasText(password);

		if (!StringUtils.hasText(cloudControllerUrl)) {
			List<String> urlMatches = getStoredUrlsForEmail(email);
			if (urlMatches.size() > 1) {
				logger.warning("Multiple cloud controller URLs are stored for the email address '" + email + "'. Please specify a cloud controller.");
				return;
			} else if (urlMatches.size() == 1) {
				cloudControllerUrl = urlMatches.get(0);
			} else {
				// We set the default URL only after no stored URL is found
				cloudControllerUrl = UaaAwareAppCloudClient.VCLOUD_URL;
			}
		}

		if (!StringUtils.hasText(email)) {
			List<String> emailMatches = getStoredEmailsForUrl(cloudControllerUrl);
			if (emailMatches.size() > 1) {
				logger.warning("Multiple email addresses are stored for the cloud controller URL '" + cloudControllerUrl + "'. Please specify an email address.");
				return;
			} else if (emailMatches.size() == 1) {
				email = emailMatches.get(0);
			} else {
				logger.warning("An email address is required.");
				return;
			}
		}

		if (StringUtils.hasText(email) && StringUtils.hasText(cloudControllerUrl) && !StringUtils.hasText(password)) {
			List<Map<String, String>> list = getStoredLoginPrefs();
			for (Map<String, String> map : list) {
				if (email.equals(map.get(EMAIL_KEY)) && cloudControllerUrl.equals(map.get(URL_KEY))) {
					if (!StringUtils.hasText(password)) {
						password = map.get(PASSWORD_KEY);
						break;
					}
				}
			}
		}

		try {
			if (client != null) {
				client.deactivate();
			}
			URL vCloudUrl = new URL(UaaAwareAppCloudClient.VCLOUD_URL);
			SimpleClientHttpRequestFactory simpleFactory = new SimpleClientHttpRequestFactory();
			simpleFactory.setProxy(new BasicProxyService().setupProxy(vCloudUrl));
			client = new UaaAwareAppCloudClient(product, uaaService, email, password, null, vCloudUrl, simpleFactory);
			client.loginIfNeeded();

			if (storeCredentials) {
				List<Map<String, String>> list = getStoredLoginPrefs();
				Set<String> entries = new LinkedHashSet<String>();
				for (Map<String, String> map : list) {
					if (StringUtils.hasText(map.get(EMAIL_KEY)) && StringUtils.hasText(map.get(PASSWORD_KEY)) && StringUtils.hasText(map.get(URL_KEY))) {
						entries.add(encodeLoginPrefEntry(map.get(EMAIL_KEY), map.get(PASSWORD_KEY), map.get(URL_KEY)));
					}
				}
				entries.add(encodeLoginPrefEntry(email, password, cloudControllerUrl));
				try {
					putPreference(VCLOUD_KEY, crypt(encodeLoginPrefEntries(new ArrayList<String>(entries)).getBytes(), Cipher.ENCRYPT_MODE));
					logger.info("Credentials saved.");
				} catch (Exception ignored) {
				}

			}
			logger.info("Logged in successfully with email address '" + email + "'");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public void putPreference(String prefKey, String prefValue) throws BackingStoreException, UnsupportedEncodingException {
		preferences.putByteArray(prefKey, prefValue.getBytes("UTF-8"));
		preferences.flush();
	}

	public String getPreference(String prefKey) {
		try {
			return new String(preferences.getByteArray(prefKey, EMPTY_STRING.getBytes("UTF-8")), "UTF-8");
		} catch (UnsupportedEncodingException ignored) {
		}
		return "";
	}

	public AppCloudClient getClient() {
		return client;
	}

	public List<String> getApplicationNames() {
		updateApplicationNames();
		return appNames;
	}

	public List<String> getProvisionedServices() {
		updateProvisionedServices();
		return provisionedServices;
	}

	public Map<String, List<String>> getBoundUrlMap() {
		updateUrlMap();
		return boundUrlMap;
	}

	public List<Integer> getApplicationMemoryOptions() {
		updateMemoryOptions();
		return memoryOptions;
	}

	public CloudService getProvisionedService(String provisionedServiceName) {
		return client.getService(provisionedServiceName);
	}

	public ServiceConfiguration getService(String serviceVendor) {
		for (ServiceConfiguration serviceConfiguration : client.getServiceConfigurations()) {
			if (serviceConfiguration.getVendor().equals(serviceVendor)) {
				return serviceConfiguration;
			}
		}
		return null;
	}

	public List<String> getServiceTypes() {
		updateServiceTypes();
		return serviceTypes;
	}

	public List<String> getStoredEmails() {
		return getStoredData(EMAIL_KEY);
	}

	public List<String> getStoredUrls() {
		return getStoredData(URL_KEY);
	}

	private List<String> getStoredData(String key) {
		Set<String> storedData = new LinkedHashSet<String>();
		List<Map<String, String>> list = getStoredLoginPrefs();
		for (Map<String, String> map : list) {
			storedData.add(map.get(key));
		}
		return new ArrayList<String>(storedData);
	}

	private List<String> getStoredUrlsForEmail(String email) {
		return getStoredMatches(email, EMAIL_KEY, URL_KEY);
	}

	private List<String> getStoredEmailsForUrl(String url) {
		return getStoredMatches(url, URL_KEY, EMAIL_KEY);
	}

	private List<String> getStoredMatches(String value, String valueKey, String dataKey) {
		List<Map<String, String>> list = getStoredLoginPrefs();
		if (list != null && StringUtils.hasText(value)) {
			List<String> matches = new ArrayList<String>();
			for (Map<String, String> map : list) {
				if (value.equals(map.get(valueKey)) && !matches.contains(map.get(dataKey))) {
					matches.add(map.get(dataKey));
				}
			}
			return matches;
		}
		return new ArrayList<String>();
	}

	private List<Map<String, String>> getStoredLoginPrefs() {
		String encodedPrefs = getPreference(VCLOUD_KEY);
		if (StringUtils.hasText(encodedPrefs)) {
			encodedPrefs = crypt(encodedPrefs.getBytes(), Cipher.DECRYPT_MODE);
		}
		return decodeLoginPrefEntries(encodedPrefs);
	}

	private String encodeLoginPrefEntry(String email, String password, String cloudControllerUrl) {
		if (!StringUtils.hasText(email) && !StringUtils.hasText(password) && StringUtils.hasText(cloudControllerUrl)) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		String encodedEmail = Base64.encodeBytes(email.getBytes());
		String encodedPassword = Base64.encodeBytes(password.getBytes());
		String encodedUrl = Base64.encodeBytes(cloudControllerUrl.getBytes());
		sb.append(EMAIL_KEY).append(":").append(encodedEmail).append(",");
		sb.append(PASSWORD_KEY).append(":").append(encodedPassword).append(",");
		sb.append(URL_KEY).append(":").append(encodedUrl);
		return sb.toString();
	}

	private Map<String, String> decodeLoginPrefEntry(String encodedEntry) {
		Map<String, String> map = new HashMap<String, String>();
		if (!StringUtils.hasText(encodedEntry)) {
			return map;
		}
		String[] encodedFields = encodedEntry.split(",");
		for (String encodedField : encodedFields) {
			String[] valuePair = encodedField.split(":");
			if (valuePair.length == 2) {
				try {
					map.put(valuePair[0], new String(Base64.decode(valuePair[1].getBytes())));
				} catch (IOException ignored) {}
			}
		}
		return map;
	}

	private String encodeLoginPrefEntries(List<String> entries) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < entries.size(); i++) {
			sb.append(entries.get(i));
			if (i < entries.size() - 1) {
				sb.append("|");
			}
		}
		return sb.toString();
	}

	private List<Map<String, String>> decodeLoginPrefEntries(String entries) {
		if (!StringUtils.hasText(entries)) {
			return new ArrayList<Map<String, String>>();
		}
		String[] encodedEntries = entries.split("\\|");
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		for (String encodedEntry : encodedEntries) {
			list.add(decodeLoginPrefEntry(encodedEntry));
		}
		return list;
	}

	private String crypt(byte[] input, int opmode) {
		Cipher cipher = getCipher(opmode);
		try {
			return new String(cipher.doFinal(input));
		} catch (Exception ignored) {
		}
		return "";
	}

	private Cipher getCipher(int opmode) {
		try {
			DESKeySpec keySpec = new DESKeySpec(ROO_KEY.getBytes());
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			SecretKey skey = keyFactory.generateSecret(keySpec);
			Cipher cipher = Cipher.getInstance("DES");
			cipher.init(opmode, skey);
			return cipher;
		} catch (InvalidKeySpecException e) {
			throw new IllegalStateException(e);
		} catch (NoSuchPaddingException e) {
			throw new IllegalStateException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		} catch (InvalidKeyException e) {
			throw new IllegalStateException(e);
		}
	}

	private void updateApplicationNames() {
		appNames.clear();
		for (CloudApplication app : client.getApplications()) {
			appNames.add(app.getName());
		}
	}

	private void updateProvisionedServices() {
		provisionedServices.clear();
		for (CloudService provisionedService : client.getServices()) {
			provisionedServices.add(provisionedService.getName());
		}
	}

	private void updateServiceTypes() {
		serviceTypes.clear();
		for (ServiceConfiguration serviceType : client.getServiceConfigurations()) {
			serviceTypes.add(serviceType.getVendor());
		}
	}

	private void updateUrlMap() {
		Map<String, List<String>> boundUrlMap = new HashMap<String, List<String>>();
		for (CloudApplication app : client.getApplications()) {
			boundUrlMap.put(app.getName(), app.getUris());
		}
		this.boundUrlMap = boundUrlMap;
	}

	private void updateMemoryOptions() {
		memoryOptions.clear();
		for (int memoryOption : client.getApplicationMemoryChoices()) {
			memoryOptions.add(memoryOption);
		}
		Collections.sort(memoryOptions);
	}

	// TODO: Switch to UAA's PreferencesUtils (but must wait for UAA 1.0.3 due to bug in UAA 1.0.2 and earlier)
	private Preferences getPreferencesFor(Class<?> clazz) {
		// Create the Preferences object, suppressing "Created user preferences directory" messages if there is no Java preferences directory
		Logger l = Logger.getLogger("java.util.prefs");
		Level original = l.getLevel();
		try {
			l.setLevel(Level.WARNING);
			return Preferences.userNodeForPackage(clazz);
		} finally {
			l.setLevel(original);
		}
	}
}
