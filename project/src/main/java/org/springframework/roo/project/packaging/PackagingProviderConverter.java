package org.springframework.roo.project.packaging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * A {@link Converter} for {@link PackagingProvider}s
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
@Reference(name = "packagingProvider", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = PackagingProvider.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE)
public class PackagingProviderConverter implements Converter<PackagingProvider>, PackagingProviderRegistry {

	// Fields
	private final Object mutex = new Object();
	// Using a map avoids each PackagingProvider having to implement equals() properly
	private final Map<String, PackagingProvider> packagingProviders = new HashMap<String, PackagingProvider>();

	protected void bindPackagingProvider(final PackagingProvider packagingProvider) {
		synchronized (mutex) {
			final PackagingProvider previousPackagingProvider = packagingProviders.put(packagingProvider.getId(), packagingProvider);
			Assert.isNull(previousPackagingProvider, "More than one PackagingProvider with ID = '" + packagingProvider.getId() + "'");
		}
	}

	protected void unbindPackagingProvider(final PackagingProvider packagingProvider) {
		synchronized (mutex) {
			packagingProviders.remove(packagingProvider.getId());
		}
	}

	public boolean supports(final Class<?> type, final String optionContext) {
		return PackagingProvider.class.isAssignableFrom(type);
	}

	public PackagingProvider convertFromText(final String value, final Class<?> targetType, final String optionContext) {
		for (final Entry<String, PackagingProvider> entry : packagingProviders.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(value)) {
				return entry.getValue();
			}
		}
		throw new UnsupportedOperationException("Unsupported packaging type '" + value + "'");
	}

	public boolean getAllPossibleValues(final List<Completion> completions, final Class<?> targetType, final String existingData, final String optionContext, final MethodTarget target) {
		for (final String id : packagingProviders.keySet()) {
			if (!StringUtils.hasText(existingData) || id.toLowerCase().startsWith(existingData.toLowerCase())) {
				completions.add(new Completion(id.toUpperCase()));
			}
		}
		return true;
	}

	public PackagingProvider getDefaultPackagingProvider() {
		PackagingProvider defaultCoreProvider = null;
		for (final PackagingProvider packagingProvider : packagingProviders.values()) {
			if (packagingProvider.isDefault()) {
				if (packagingProvider instanceof CorePackagingProvider) {
					defaultCoreProvider = packagingProvider;
				} else {
					return packagingProvider;
				}
			}
		}
		Assert.state(defaultCoreProvider != null, "Should have found a default core PackagingProvider");
		return defaultCoreProvider;
	}
}