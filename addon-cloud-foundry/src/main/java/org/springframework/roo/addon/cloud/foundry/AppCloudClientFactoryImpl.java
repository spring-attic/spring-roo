package org.springframework.roo.addon.cloud.foundry;

import java.net.URL;
import java.util.Dictionary;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.uaa.client.UaaService;
import org.springframework.uaa.client.VersionHelper;
import org.springframework.uaa.client.internal.BasicProxyService;
import org.springframework.uaa.client.protobuf.UaaClient.Product;

/**
 * The OSGi {@link AppCloudClientFactory} implementation.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class AppCloudClientFactoryImpl implements AppCloudClientFactory {

    private static final String CLOUD_FOUNDRY_PRODUCT_NAME = "Cloud Foundry Java API";
    private static final String DEFAULT_BUNDLE_VERSION = "0.0.0.RELEASE";

    // TODO is this value ever used, or is it always overwritten by activate()?
    private Product product = VersionHelper.getProduct(
            CLOUD_FOUNDRY_PRODUCT_NAME, DEFAULT_BUNDLE_VERSION);
    @Reference UaaService uaaService;

    protected void activate(final ComponentContext context) {
        product = getCloudFoundryProduct(context.getBundleContext().getBundle()
                .getHeaders());
    }

    private Product getCloudFoundryProduct(final Dictionary<?, ?> bundleHeaders) {
        // TODO: Replace with call to VersionHelper.getProductFromDictionary(..)
        // available in UAA 1.0.3
        final String bundleVersion = ObjectUtils.toString(
                bundleHeaders.get("Bundle-Version"), DEFAULT_BUNDLE_VERSION);
        final String gitCommitHash = ObjectUtils.toString(
                bundleHeaders.get("Git-Commit-Hash"), null);
        return VersionHelper.getProduct(CLOUD_FOUNDRY_PRODUCT_NAME,
                bundleVersion, gitCommitHash);
    }

    public UaaAwareAppCloudClient getUaaAwareInstance(
            final CloudCredentials credentials) {
        final URL loginUrl = credentials.getUrlObject();
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setProxy(new BasicProxyService().setupProxy(loginUrl));
        return new UaaAwareAppCloudClient(product, uaaService, credentials,
                requestFactory);
    }
}
