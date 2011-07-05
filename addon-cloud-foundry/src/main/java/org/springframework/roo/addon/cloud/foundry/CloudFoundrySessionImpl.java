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
import java.util.HashSet;
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
	private static final String CLOUD_FOUNDRY_KEY = "Cloud Foundry Prefs";

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
		} catch (BackingStoreException e) {
			throw new IllegalStateException(e);
		}
	}

	protected void activate(ComponentContext context) {
		// TODO: Replace with call to VersionHelper.getProductFromDictionary(..) available in UAA 1.0.3
		@SuppressWarnings("rawtypes") Dictionary d = context.getBundleContext().getBundle().getHeaders();
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
			cloudControllerUrl = UaaAwareAppCloudClient.CLOUD_FOUNDRY_URL;
		}

		if (!StringUtils.hasText(email)) {
			List<CloudCredentials> emailMatches = getStoredEmailsForUrl(cloudControllerUrl);
			if (emailMatches.size() > 1) {
				logger.warning("Multiple email addresses are stored for the cloud controller URL '" + cloudControllerUrl + "'. Please specify an email address.");
				return;
			} else if (emailMatches.size() == 1) {
				email = emailMatches.get(0).getEmail();
			} else {
				logger.warning("An email address is required.");
				return;
			}
		}

		if (StringUtils.hasText(email) && StringUtils.hasText(cloudControllerUrl) && !StringUtils.hasText(password)) {
			Set<CloudCredentials> cloudCredentialsSet = getStoredLoginPrefs();
			for (CloudCredentials cloudCredentials : cloudCredentialsSet) {
				if (email.equals(cloudCredentials.getEmail()) && cloudControllerUrl.equals(cloudCredentials.getUrl())) {
					password = cloudCredentials.getPassword();
					break;
				}
			}
		}

		CloudCredentials loginCredentials = new CloudCredentials(email, password, cloudControllerUrl);
		if (!loginCredentials.isValid()) {
			logger.info("Login failed");
			return;
		}

		try {
			if (client != null) {
				client.deactivate();
			}
			URL cloudFoundryUrl = new URL(UaaAwareAppCloudClient.CLOUD_FOUNDRY_URL);
			SimpleClientHttpRequestFactory simpleFactory = new SimpleClientHttpRequestFactory();
			simpleFactory.setProxy(new BasicProxyService().setupProxy(cloudFoundryUrl));
			client = new UaaAwareAppCloudClient(product, uaaService, email, password, null, cloudFoundryUrl, simpleFactory);
			client.loginIfNeeded();

			if (storeCredentials) {
				Set<CloudCredentials> list = getStoredLoginPrefs();
				Set<String> entries = new LinkedHashSet<String>();
				for (CloudCredentials cloudCredentials : list) {
					if (cloudCredentials.isValid()) {
						entries.add(encodeLoginPrefEntry(cloudCredentials));
					}
				}
				CloudCredentials cloudCredentials = new CloudCredentials(email, password, cloudControllerUrl);
				entries.add(encodeLoginPrefEntry(cloudCredentials));
				try {
					putPreference(CLOUD_FOUNDRY_KEY, crypt(encodeLoginPrefEntries(new ArrayList<String>(entries)).getBytes("UTF-8"), Cipher.ENCRYPT_MODE));
					logger.info("Credentials saved.");
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}

			}
			logger.info("Logged in successfully with email address '" + email + "'");
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e);
		}
	}

	public void putPreference(String prefKey, byte[] prefValue) {
		try {
			preferences.putByteArray(prefKey, prefValue);
			preferences.flush();
		} catch (BackingStoreException e) {
			throw new IllegalStateException(e);
		}
	}

	public byte[] getPreference(String prefKey) {
		try {
			return preferences.getByteArray(prefKey, EMPTY_STRING.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
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
		Set<String> storedData = new LinkedHashSet<String>();
		Set<CloudCredentials> set = getStoredLoginPrefs();
		for (CloudCredentials cloudCredentials : set) {
			storedData.add(cloudCredentials.getEmail());
		}
		return new ArrayList<String>(storedData);
	}

	public List<String> getStoredUrls() {
		Set<String> storedData = new LinkedHashSet<String>();
		Set<CloudCredentials> set = getStoredLoginPrefs();
		for (CloudCredentials cloudCredentials : set) {
			storedData.add(cloudCredentials.getUrl());
		}
		return new ArrayList<String>(storedData);
	}

	public void clearStoredLoginDetails() {
		try {
			putPreference(CLOUD_FOUNDRY_KEY, EMPTY_STRING.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	private List<CloudCredentials> getStoredEmailsForUrl(String url) {
		Set<CloudCredentials> cloudCredentialsList = getStoredLoginPrefs();
		List<CloudCredentials> found = new ArrayList<CloudCredentials>();
		for (CloudCredentials cloudCredentials : cloudCredentialsList) {
			if (url != null && url.equals(cloudCredentials.getUrl())) {
				found.add(cloudCredentials);
			}
		}
		return found;
	}

	private Set<CloudCredentials> getStoredLoginPrefs() {
		Set<CloudCredentials> decoded = new HashSet<CloudCredentials>();
		byte[] encodedPrefs = getPreference(CLOUD_FOUNDRY_KEY);
		if (encodedPrefs.length > 0) {
			encodedPrefs = crypt(encodedPrefs, Cipher.DECRYPT_MODE);
		} else {
			return decoded;
		}

		try {
			decoded = decodeLoginPrefEntries(new String(encodedPrefs, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
		return decoded;
	}

	private String encodeLoginPrefEntry(CloudCredentials cloudCredentials) {
		return cloudCredentials.encode();
	}

	private CloudCredentials decodeLoginPrefEntry(String encodedEntry) {
		return CloudCredentials.decode(encodedEntry);
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

	private Set<CloudCredentials> decodeLoginPrefEntries(String entries) {
		if (!StringUtils.hasText(entries)) {
			return new HashSet<CloudCredentials>();
		}
		String[] encodedEntries = entries.split("\\|");
		Set<CloudCredentials> set = new HashSet<CloudCredentials>();
		for (String encodedEntry : encodedEntries) {
			set.add(decodeLoginPrefEntry(encodedEntry));
		}
		return set;
	}

	private byte[] crypt(byte[] input, int opmode) {
		Cipher cipher = getCipher(opmode);
		try {
			return cipher.doFinal(input);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private Cipher getCipher(int opmode) {
		try {
			DESKeySpec keySpec = new DESKeySpec(ROO_KEY.getBytes("UTF-8"));
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
		} catch (UnsupportedEncodingException e) {
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

	public static class CloudCredentials {
		private String email;
		private String password;
		private String url;

		public CloudCredentials(String email, String password, String url) {
			this.email = email;
			this.password = password;
			this.url = url;
		}

		public CloudCredentials(Map<String, String> properties) {
			email = properties.get(EMAIL_KEY);
			password = properties.get(PASSWORD_KEY);
			url = properties.get(URL_KEY);
		}

		public boolean isValid() {
			return StringUtils.hasText(email) && StringUtils.hasText(password) && StringUtils.hasText(url);
		}

		public String getEmail() {
			return email;
		}

		public String getPassword() {
			return password;
		}

		public String getUrl() {
			return url;
		}

		public String encode() {
			if (!isValid()) {
				throw new IllegalStateException("Credentials invalid; cannot continue");
			}
			StringBuilder sb = new StringBuilder();
			try {
				sb.append(EMAIL_KEY).append(":").append(Base64.encodeBytes(getEmail().getBytes(), Base64.DO_BREAK_LINES)).append(",");
				sb.append(PASSWORD_KEY).append(":").append(Base64.encodeBytes(getPassword().getBytes(), Base64.DO_BREAK_LINES)).append(",");
				sb.append(URL_KEY).append(":").append(Base64.encodeBytes(getUrl().getBytes(), Base64.DO_BREAK_LINES));
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
			return sb.toString();
		}

		public static CloudCredentials decode(String encoded) {
			if (!StringUtils.hasText(encoded)) {
				throw new IllegalStateException("Stored login invalid; cannot continue");
			}
			Map<String, String> map = new HashMap<String, String>();
			String[] encodedFields = encoded.split(",");
			for (String encodedField : encodedFields) {
				String[] valuePair = encodedField.split(":");
				if (valuePair.length == 2) {
					try {
						map.put(valuePair[0], new String(Base64.decode(valuePair[1], Base64.DO_BREAK_LINES)));
					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
				}
			}
			return new CloudCredentials(map);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			CloudCredentials clouldCredentials = (CloudCredentials) o;

			if (email != null ? !email.equals(clouldCredentials.email) : clouldCredentials.email != null) return false;
			if (url != null ? !url.equals(clouldCredentials.url) : clouldCredentials.url != null) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = email != null ? email.hashCode() : 0;
			result = 31 * result + (url != null ? url.hashCode() : 0);
			return result;
		}
	}
}
