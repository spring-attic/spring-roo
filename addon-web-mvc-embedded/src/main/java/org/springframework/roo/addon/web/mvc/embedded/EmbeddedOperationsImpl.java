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
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of operations that are available via the Roo shell.
 *
 * @author Stefan Schmidt
 * @since 1.1
 */
@Service
@Component
@Reference(name = "embeddedProvider", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = EmbeddedProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
public class EmbeddedOperationsImpl implements EmbeddedOperations {

	// Constants
	private static final Logger logger = HandlerUtils.getLogger(EmbeddedOperationsImpl.class);

	// Fields
	@Reference private ProjectOperations projectOperations;

	private final Object mutex = new Object();
	private final Set<EmbeddedProvider> providers = new HashSet<EmbeddedProvider>();

	public boolean isCommandAvailable() {
		return projectOperations.isFocusedProjectAvailable();
	}

	public boolean embed(final String url, final String viewName) {
		for (EmbeddedProvider provider: getEmbeddedProviders()) {
			if (provider.embed(url, viewName)) {
				return true;
			}
		}
		logger.warning("Could not find a matching provider for this URL");
		return false;
	}

	public boolean install(final String viewName, final Map<String, String> options) {
		for (EmbeddedProvider provider: getEmbeddedProviders()) {
			if (provider.install(viewName, options)) {
				return true;
			}
		}
		logger.warning("Could not find a matching implementation for this 'web mvc embed' type");
		return false;
	}

	private Set<EmbeddedProvider> getEmbeddedProviders() {
		synchronized (mutex) {
			return Collections.unmodifiableSet(providers);
		}
	}
	protected void bindEmbeddedProvider(final EmbeddedProvider provider) {
		synchronized (mutex) {
			providers.add(provider);
		}
	}

	protected void unbindEmbeddedProvider(final EmbeddedProvider provider) {
		synchronized (mutex) {
			if (providers.contains(provider)) {
				providers.remove(provider);
			}
		}
	}
}