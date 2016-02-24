package org.springframework.roo.addon.web.mvc.embedded;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link EmbeddedOperations).
 *
 * @author Stefan Schmidt
 * @since 1.1
 */
@Service
@Component
@Reference(name = "embeddedProvider", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = EmbeddedProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
public class EmbeddedOperationsImpl implements EmbeddedOperations {

    private static final Logger LOGGER = HandlerUtils
            .getLogger(EmbeddedOperationsImpl.class);

    private final Object mutex = new Object();

    @Reference private ProjectOperations projectOperations;
    private final Set<EmbeddedProvider> providers = new HashSet<EmbeddedProvider>();

    protected void bindEmbeddedProvider(final EmbeddedProvider provider) {
        synchronized (mutex) {
            providers.add(provider);
        }
    }

    public boolean embed(final String url, final String viewName) {
        for (final EmbeddedProvider provider : getEmbeddedProviders()) {
            if (provider.embed(url, viewName)) {
                return true;
            }
        }
        LOGGER.warning("Could not find a matching provider for this URL");
        return false;
    }

    private Set<EmbeddedProvider> getEmbeddedProviders() {
        synchronized (mutex) {
            return Collections.unmodifiableSet(providers);
        }
    }

    public boolean install(final String viewName,
            final Map<String, String> options) {
        for (final EmbeddedProvider provider : getEmbeddedProviders()) {
            if (provider.install(viewName, options)) {
                return true;
            }
        }
        LOGGER.warning("Could not find a matching implementation for this 'web mvc embed' type");
        return false;
    }

    public boolean isEmbeddedInstallationPossible() {
        return projectOperations.isFocusedProjectAvailable()
                && !projectOperations
                        .isFeatureInstalledInFocusedModule(FeatureNames.JSF);
    }

    protected void unbindEmbeddedProvider(final EmbeddedProvider provider) {
        synchronized (mutex) {
            if (providers.contains(provider)) {
                providers.remove(provider);
            }
        }
    }
}