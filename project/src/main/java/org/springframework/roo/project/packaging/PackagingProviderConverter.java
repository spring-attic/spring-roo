package org.springframework.roo.project.packaging;

import java.util.Collections;
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
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.support.util.StringUtils;

/**
 * A {@link Converter} for {@link PackagingProvider}s
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
@Reference(name = "packagingType", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = PackagingProvider.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE)
public class PackagingProviderConverter implements Converter<PackagingProvider> {

	// Fields
	private final Object mutex = new Object();
	// Using a map avoids each PackagingType having to implement equals() properly
	private final Map<String, PackagingProvider> packagingTypes = new HashMap<String, PackagingProvider>();

	protected void bindPackagingType(final PackagingProvider packagingType) {
		synchronized (mutex) {
			packagingTypes.put(packagingType.getId(), packagingType);
		}
	}

	protected void unbindPackagingType(final PackagingProvider packagingType) {
		synchronized (mutex) {
			packagingTypes.remove(packagingType.getId());
		}
	}

	public boolean supports(final Class<?> type, final String optionContext) {
		return PackagingProvider.class.isAssignableFrom(type);
	}

	public PackagingProvider convertFromText(final String value, final Class<?> targetType, final String optionContext) {
		for (final Entry<String, PackagingProvider> entry : packagingTypes.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(value)) {
				return entry.getValue();
			}
		}
		throw new UnsupportedOperationException("Unsupported packaging type '" + value + "'");
	}

	public boolean getAllPossibleValues(final List<String> completions, final Class<?> targetType, final String existingData, final String optionContext, final MethodTarget target) {
		for (final String id : packagingTypes.keySet()) {
			if (!StringUtils.hasText(existingData) || id.toLowerCase().startsWith(existingData.toLowerCase())) {
				completions.add(id.toUpperCase());
			}
		}
		Collections.sort(completions);
		return true;
	}
}