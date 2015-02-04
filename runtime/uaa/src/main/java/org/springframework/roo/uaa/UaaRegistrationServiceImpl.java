package org.springframework.roo.uaa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.support.osgi.BundleFindingUtils;
import org.springframework.uaa.client.TransmissionAwareUaaService;
import org.springframework.uaa.client.TransmissionEventListener;
import org.springframework.uaa.client.UaaService;
import org.springframework.uaa.client.protobuf.UaaClient.FeatureUse;
import org.springframework.uaa.client.protobuf.UaaClient.FeatureUse.Builder;
import org.springframework.uaa.client.protobuf.UaaClient.Product;

/**
 * Default implementation of {@link UaaRegistrationService}.
 * 
 * @author Ben Alex
 * @since 1.1.1
 */
@Service
@Component
public class UaaRegistrationServiceImpl implements UaaRegistrationService,
        TransmissionEventListener {

    /** key: bundleSymbolicName, value: customJson */
    private final Map<String, String> bsnBuffer = new HashMap<String, String>();
    /** key: BSN, value: git commit hash, if available */
    private final Map<String, String> bsnCommitHashCache = new HashMap<String, String>();

    /** key: BSN, value: version */
    private final Map<String, Version> bsnVersionCache = new HashMap<String, Version>();
    private BundleContext bundleContext;
    /** key: projectId, value: list of products */
    private final Map<String, List<Product>> projectIdBuffer = new HashMap<String, List<Product>>();
    @Reference private PublicFeatureResolver publicFeatureResolver;
    @Reference private UaaService uaaService;

    protected void activate(final ComponentContext context) {
        // Attempt to store the SPRING_ROO Product (via the registerBSN method
        // so as to be included via possibly-active buffering)
        bundleContext = context.getBundleContext();
        final String bundleSymbolicName = BundleFindingUtils
                .findFirstBundleForTypeName(context.getBundleContext(),
                        UaaRegistrationServiceImpl.class.getName());
        registerBundleSymbolicNameUse(bundleSymbolicName, null);
        if (uaaService instanceof TransmissionAwareUaaService) {
            ((TransmissionAwareUaaService) uaaService)
                    .addTransmissionEventListener(this);
        }
    }

    public void afterTransmission(final TransmissionType type,
            final boolean successful) {
    }

    public void beforeTransmission(final TransmissionType type) {
        if (type == TransmissionType.UPLOAD) {
            // Good time to flush through to UAA API, so the latest data is
            // included in the upload
            flushIfPossible();
        }
    }

    protected void deactivate(final ComponentContext context) {
        // Last effort to store the data given we're shutting down
        flushIfPossible();
        if (uaaService instanceof TransmissionAwareUaaService) {
            ((TransmissionAwareUaaService) uaaService)
                    .removeTransmissionEventListener(this);
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
        for (final String bundleSymbolicName : bsnBuffer.keySet()) {
            final String customJson = bsnBuffer.get(bundleSymbolicName);
            registerBundleSymbolicNameUse(bundleSymbolicName, customJson, false);
        }
        bsnBuffer.clear();

        // Flush the projects
        for (final String projectId : projectIdBuffer.keySet()) {
            for (final Product product : projectIdBuffer.get(projectId)) {
                registerProject(product, projectId, false);
            }
        }

        projectIdBuffer.clear();
    }

    /**
     * Populates the version information in the passed {@link Builder}. This
     * information is obtained by locating the bundle and using its version
     * metadata. The Git hash code is acquired from the manifest.
     * <p>
     * The method returns without error if the bundle could not be found.
     * 
     * @param featureUseBuilder to insert feature use information into
     *            (required)
     * @param bundleSymbolicName to locate (required)
     */
    private void populateVersionInfoIfPossible(final Builder featureUseBuilder,
            final String bundleSymbolicName) {
        Version version = bsnVersionCache.get(bundleSymbolicName);
        String commitHash = bsnCommitHashCache.get(bundleSymbolicName);

        if (version == null) {
            for (final Bundle b : bundleContext.getBundles()) {
                if (bundleSymbolicName.equals(b.getSymbolicName())) {
                    version = b.getVersion();
                    bsnVersionCache.put(bundleSymbolicName, version);
                    final Object manifestResult = b.getHeaders().get(
                            "Git-Commit-Hash");
                    if (manifestResult != null) {
                        commitHash = manifestResult.toString();
                        bsnCommitHashCache.put(bundleSymbolicName, commitHash);
                    }
                    break;
                }
            }
        }

        if (version == null) {
            // Can't acquire OSGi version information for this bundle, so give
            // up now
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

    public void registerBundleSymbolicNameUse(final String bundleSymbolicName,
            final String customJson) {
        registerBundleSymbolicNameUse(bundleSymbolicName, customJson, true);
    }

    private void registerBundleSymbolicNameUse(final String bundleSymbolicName,
            String customJson, final boolean flushWhenDone) {
        Validate.notBlank(bundleSymbolicName, "Bundle symbolic name required");

        // Ensure it's a public feature (we do not want to log or buffer private
        // features)
        if (!publicFeatureResolver.isPublic(bundleSymbolicName)) {
            return;
        }

        // Turn a null custom JSON into "" for simplicity later, including
        // buffering
        if (customJson == null) {
            customJson = "";
        }

        // If we cannot persist it at present, buffer it for potential
        // persistence later on
        if (!uaaService.isUaaTermsOfUseAccepted()) {
            bsnBuffer.put(bundleSymbolicName, customJson);
            return;
        }

        // Create feature data bytes if possible
        byte[] featureData = null;
        if (!"".equals(customJson)) {
            try {
                featureData = customJson.getBytes("UTF-8");
            }
            catch (final Exception ignore) {
            }
        }

        // Go and register it
        final FeatureUse.Builder featureUseBuilder = FeatureUse.newBuilder();
        featureUseBuilder.setName(bundleSymbolicName);
        populateVersionInfoIfPossible(featureUseBuilder, bundleSymbolicName);

        if (featureData == null) {
            // Use this UaaService method, as we want to preserve any feature
            // data we might have presented previously but hasn't yet been
            // communicated (important since UAA 1.0.1 due to its delayed
            // uploads)
            uaaService.registerFeatureUsage(SPRING_ROO,
                    featureUseBuilder.build());
        }
        else {
            // New feature data is available, so treat this as overwriting any
            // existing feature data we might have stored previously
            uaaService.registerFeatureUsage(SPRING_ROO,
                    featureUseBuilder.build(), featureData);
        }

        // Try to flush the buffer while we're at it, given persistence seems to
        // be OK at present
        if (flushWhenDone) {
            flushIfPossible();
        }
    }

    public void registerProject(final Product product, final String projectId) {
        registerProject(product, projectId, true);
    }

    private void registerProject(final Product product, final String projectId,
            final boolean flushWhenDone) {
        Validate.notNull(product, "Product required");
        Validate.notBlank(projectId, "Project ID required");

        // If we cannot persist it at present, buffer it for potential
        // persistence later on
        if (!uaaService.isUaaTermsOfUseAccepted()) {
            List<Product> value = projectIdBuffer.get(projectId);
            if (value == null) {
                value = new ArrayList<Product>();
                projectIdBuffer.put(projectId, value);
            }
            // We don't buffer it if there's an "identical" (by name and
            // version) product in there already
            boolean add = true;
            for (final Product existing : value) {
                if (existing.getName().equals(product.getName())
                        && existing.getMajorVersion() == product
                                .getMajorVersion()
                        && existing.getMinorVersion() == product
                                .getMinorVersion()
                        && existing.getPatchVersion() == product
                                .getPatchVersion()
                        && existing.getReleaseQualifier().equals(
                                product.getReleaseQualifier())
                        && existing.getSourceControlIdentifier().equals(
                                product.getSourceControlIdentifier())) {
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

        // Try to flush the buffer while we're at it, given persistence seems to
        // be OK at present
        if (flushWhenDone) {
            flushIfPossible();
        }
    }

    public void requestTransmission() {
        if (uaaService instanceof TransmissionAwareUaaService) {
            final TransmissionAwareUaaService ta = (TransmissionAwareUaaService) uaaService;
            ta.requestTransmission();
        }
    }
}
