package org.springframework.roo.uaa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.support.osgi.BundleFindingUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.uaa.client.UaaService;
import org.springframework.uaa.client.protobuf.UaaClient.Product;
import org.springframework.uaa.client.protobuf.UaaClient.Privacy.PrivacyLevel;

/**
 * Default implementation of {@link UaaRegistrationService}.
 * 
 * @author Ben Alex
 * @since 1.1.1
 *
 */
@Service
@Component
public class UaaRegistrationServiceImpl implements UaaRegistrationService {

	@Reference private UaaService uaaService;
	@Reference private PublicFeatureResolver publicFeatureResolver;
	/** key: bundleSymbolicName, value: customJson */
	private Map<String, String> bsnBuffer = new HashMap<String, String>();
	/** key: projectId, value: list of products*/
	private Map<String, List<Product>> projectIdBuffer = new HashMap<String, List<Product>>();
	
	protected void activate(ComponentContext context) {
		// Attempt to store the SPRING_ROO Product (via the registerBSN method so as to be included via possibly-active buffering)
		String bundleSymbolicName = BundleFindingUtils.findFirstBundleForTypeName(context.getBundleContext(), UaaRegistrationServiceImpl.class.getName());
		registerBundleSymbolicNameUse(bundleSymbolicName, null);
	}
	
	protected void deactivate(ComponentContext context) {
		// Last effort to store the data given we're shutting down
		flushIfPossible();
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
		if (!isPrivacyLevelAllowingPersistence()) {
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
		uaaService.registerFeatureUsage(SPRING_ROO, bundleSymbolicName, featureData);
		
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
		if (!isPrivacyLevelAllowingPersistence()) {
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
		if (bsnBuffer.size() == 0 && projectIdBuffer.size() == 0) {
			// Nothing to flush
			return;
		}
		
		if (!isPrivacyLevelAllowingPersistence()) {
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

	private boolean isPrivacyLevelAllowingPersistence() {
		PrivacyLevel level = uaaService.getPrivacyLevel();
		return level != PrivacyLevel.DECLINE_TOU && level != PrivacyLevel.UNDECIDED_TOU;
	}
	
}
