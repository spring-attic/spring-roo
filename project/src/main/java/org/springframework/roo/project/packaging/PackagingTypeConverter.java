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
 * A {@link Converter} for {@link PackagingType}s
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
@Reference(name = "packagingType", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = PackagingType.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE)
public class PackagingTypeConverter implements Converter<PackagingType> {

	// Fields
	private final Object mutex = new Object();
	// Using a map avoids each PackagingType having to implement equals() properly
	private final Map<String, PackagingType> packagingTypes = new HashMap<String, PackagingType>();

	protected void bindPackagingType(final PackagingType packagingType) {
		synchronized (mutex) {
			packagingTypes.put(packagingType.getName(), packagingType);
		}
	}

	protected void unbindPackagingType(final PackagingType packagingType) {
		synchronized (mutex) {
			packagingTypes.remove(packagingType.getName());
		}
	}

	public boolean supports(final Class<?> type, final String optionContext) {
		return PackagingType.class.isAssignableFrom(type);
	}

	public PackagingType convertFromText(final String value, final Class<?> targetType, final String optionContext) {
		for (final Entry<String, PackagingType> entry : packagingTypes.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(value)) {
				return entry.getValue();
			}
		}
		throw new UnsupportedOperationException("Unsupported packaging type '" + value + "'");
	}

	public boolean getAllPossibleValues(final List<String> completions, final Class<?> targetType, final String existingData, final String optionContext, final MethodTarget target) {
		for (final String packaging : packagingTypes.keySet()) {
			if (!StringUtils.hasText(existingData) || packaging.toLowerCase().startsWith(existingData.toLowerCase())) {
				completions.add(packaging.toUpperCase());
			}
		}
		Collections.sort(completions);
		return true;
	}
}