package org.springframework.roo.uaa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.support.osgi.BundleFindingUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.uaa.client.TransmissionAwareUaaService;
import org.springframework.uaa.client.TransmissionEventListener;
import org.springframework.uaa.client.UaaService;
import org.springframework.uaa.client.protobuf.UaaClient.FeatureUse;
import org.springframework.uaa.client.protobuf.UaaClient.Product;
import org.springframework.uaa.client.protobuf.UaaClient.FeatureUse.Builder;

/**
 * Default implementation of {@link UaaRegistrationService}.
 * 
 * @author Ben Alex
 * @since 1.1.1
 *
 */
@Service
@Component
public class UaaRegistrationServiceImpl implements UaaRegistrationService, TransmissionEventListener {

	@Reference private UaaService uaaService;
	@Reference private PublicFeatureResolver publicFeatureResolver;
	/** key: bundleSymbolicName, value: customJson */
	private Map<String, String> bsnBuffer = new HashMap<String, String>();
	/** key: projectId, value: list of products*/
	private Map<String, List<Product>> projectIdBuffer = new HashMap<String, List<Product>>();
	/** key: BSN, value: version */
	private Map<String, Version> bsnVersionCache = new HashMap<String, Version>();
	/** key: BSN, value: git commit hash, if available */
	private Map<String, String> bsnCommitHashCache = new HashMap<String, String>();
	private BundleContext bundleContext;
	
	protected void activate(ComponentContext context) {
		// Attempt to store the SPRING_ROO Product (via the registerBSN method so as to be included via possibly-active buffering)
		this.bundleContext = context.getBundleContext();
		String bundleSymbolicName = BundleFindingUtils.findFirstBundleForTypeName(context.getBundleContext(), UaaRegistrationServiceImpl.class.getName());
		registerBundleSymbolicNameUse(bundleSymbolicName, null);
		if (uaaService instanceof TransmissionAwareUaaService) {
			((TransmissionAwareUaaService)uaaService).addTransmissionEventListener(this);
		}
	}
	
	protected void deactivate(ComponentContext context) {
		// Last effort to store the data given we're shutting down
		flushIfPossible();
		if (uaaService instanceof TransmissionAwareUaaService) {
			((TransmissionAwareUaaService)uaaService).removeTransmissionEventListener(this);
		}
	}
	
	public void registerBundleSymbolicNameUse(String bundleSymbolicName, String customJson) {
		registerBundleSymbolicNameUse(bundleSymbolicName, customJson, true);
	}
	
	private void registerBundleSymbolicNameUse(String bundleSymbolicName, String customJson, boolean flushWhenDone) {
		Assert.hasText(bundleSymbolicName, "Bundle symbolic name required");

		// Ensure it's a public feature (we do not want to log or buffer private features)
		if (!publicFeatureResolver.isPublic(bundleSymbolicName)) {
			return;
		}

		// Turn a null custom JSON into "" for simplicity later, including buffering
		if (customJson == null) {
			customJson = "";
		}

		// If we cannot persist it at present, buffer it for potential persistence later on
		if (!uaaService.isUaaTermsOfUseAccepted()) {
			bsnBuffer.put(bundleSymbolicName, customJson);
			return;
		}

		// Create feature data bytes if possible
		byte[] featureData = null;
		if (!"".equals(customJson)) {
			try {
				featureData = customJson.getBytes("UTF-8");
			} catch (Exception ignore) {}
		}
		
		// Go and register it
		FeatureUse.Builder featureUseBuilder = FeatureUse.newBuilder();
		featureUseBuilder.setName(bundleSymbolicName);
		populateVersionInfoIfPossible(featureUseBuilder, bundleSymbolicName);
		
		if (featureData == null) {
			// Use this UaaService method, as we want to preserve any feature data we might have presented previously but hasn't yet been communicated (important since UAA 1.0.1 due to its delayed uploads)
			uaaService.registerFeatureUsage(SPRING_ROO, featureUseBuilder.build());
		} else {
			// New feature data is available, so treat this as overwriting any existing feature data we might have stored previously
			uaaService.registerFeatureUsage(SPRING_ROO, featureUseBuilder.build(), featureData);
		}
		
		// Try to flush the buffer while we're at it, given persistence seems to be OK at present
		if (flushWhenDone) {
			flushIfPossible();
		}
	}

	public void registerProject(Product product, String projectId) {
		registerProject(product, projectId, true);
	}
	
	private void registerProject(Product product, String projectId, boolean flushWhenDone) {
		Assert.notNull(product, "Product required");
		Assert.hasText(projectId, "Project ID required");
		
		// If we cannot persist it at present, buffer it for potential persistence later on
		if (!uaaService.isUaaTermsOfUseAccepted()) {
			List<Product> value = projectIdBuffer.get(projectId);
			if (value == null) {
				value = new ArrayList<Product>();
				projectIdBuffer.put(projectId, value);
			}
			// We don't buffer it if there's an "identical" (by name and version) product in there already
			boolean add = true;
			for (Product existing : value) {
				if (existing.getName().equals(product.getName()) && 
						existing.getMajorVersion() == product.getMajorVersion() && 
						existing.getMinorVersion() == product.getMinorVersion() &&
						existing.getPatchVersion() == product.getPatchVersion() &&
						existing.getReleaseQualifier().equals(product.getReleaseQualifier()) &&
						existing.getSourceControlIdentifier().equals(product.getSourceControlIdentifier())) {
					add = false;
					break;
				}
			}
			if (add) {
				value.add(product);
			}
			return;
		}
		
		uaaService.registerProductUsage(product, projectId);
		
		// Try to flush the buffer while we're at it, given persistence seems to be OK at present
		if (flushWhenDone) {
			flushIfPossible();
		}
	}

	public void flushIfPossible() {
		if (bsnBuffer.isEmpty() && projectIdBuffer.isEmpty()) {
			// Nothing to flush
			return;
		}
		
		if (!uaaService.isUaaTermsOfUseAccepted()) {
			// We can't flush yet
			return;
		}
	
		// Flush the features
		for (String bundleSymbolicName : bsnBuffer.keySet()) {
			String customJson = bsnBuffer.get(bundleSymbolicName);
			registerBundleSymbolicNameUse(bundleSymbolicName, customJson, false);
		}
		bsnBuffer.clear();
		
		// Flush the projects
		for (String projectId : projectIdBuffer.keySet()) {
			for (Product product : projectIdBuffer.get(projectId)) {
				registerProject(product, projectId, false);
			}
		}
		
		projectIdBuffer.clear();
	}
	
	public void requestTransmission() {
		if (uaaService instanceof TransmissionAwareUaaService) {
			TransmissionAwareUaaService ta = (TransmissionAwareUaaService) uaaService;
			ta.requestTransmission();
		}
	}
	
	public void afterTransmission(TransmissionType type, boolean successful) {}
	
	public void beforeTransmission(TransmissionType type) {
		if (type == TransmissionType.UPLOAD) {
			// Good time to flush through to UAA API, so the latest data is included in the upload
			flushIfPossible();
		}
	}
	
	/**
	 * Populates the version information in the passed {@link Builder}. This information is obtained by
	 * locating the bundle and using its version metadata. The Git hash code is acquired from the manifest.
	 * 
	 * <p>
	 * The method returns without error if the bundle could not be found.
	 * 
	 * @param featureUseBuilder to insert feature use information into (required)
	 * @param bundleSymbolicName to locate (required)
	 */
	private void populateVersionInfoIfPossible(Builder featureUseBuilder, String bundleSymbolicName) {
		Version version = bsnVersionCache.get(bundleSymbolicName);
		String commitHash = bsnCommitHashCache.get(bundleSymbolicName);
		
		if (version == null) {
			for (Bundle b : bundleContext.getBundles()) {
				if (bundleSymbolicName.equals(b.getSymbolicName())) {
					version = b.getVersion();
					bsnVersionCache.put(bundleSymbolicName, version);
					Object manifestResult = b.getHeaders().get("Git-Commit-Hash");
					if (manifestResult != null) {
						commitHash = manifestResult.toString();
						bsnCommitHashCache.put(bundleSymbolicName, commitHash);
					}
					break;
				}
			}
		}
		
		if (version == null) {
			// Can't acquire OSGi version information for this bundle, so give up now
			return;
		}
		
		featureUseBuilder.setMajorVersion(version.getMajor());
		featureUseBuilder.setMinorVersion(version.getMinor());
		featureUseBuilder.setPatchVersion(version.getMicro());
		featureUseBuilder.setReleaseQualifier(version.getQualifier());
		if (commitHash != null && commitHash.length() > 0) {
			featureUseBuilder.setSourceControlIdentifier(commitHash);
		}
	}

}
