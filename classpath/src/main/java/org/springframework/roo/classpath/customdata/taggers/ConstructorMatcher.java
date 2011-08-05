package org.springframework.roo.classpath.customdata.taggers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.model.CustomDataKey;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * {@link ConstructorMetadata}-specific implementation of {@link Matcher}. Currently
 * ConstructorMetadata instances are only matched based on parameter types.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public class ConstructorMatcher implements Matcher<ConstructorMetadata> {
	
	/**
	 * A {@link ConstructorMatcher} that matches on no-arg constructors.
	 * 
	 * @since 1.2
	 */
	public static final ConstructorMatcher NO_ARG = new ConstructorMatcher(PersistenceCustomDataKeys.NO_ARG_CONSTRUCTOR);
	
	// Fields
	private final CustomDataKey<ConstructorMetadata> customDataKey;
	private final List<JavaType> parameterTypes;
	
	/**
	 * Constructor
	 *
	 * @param <T> {@link JavaType} or any subclass
	 * @param customDataKey (required)
	 * @param parameterTypes
	 * @since 1.2
	 */
	public <T extends JavaType> ConstructorMatcher(final CustomDataKey<ConstructorMetadata> customDataKey, final T... parameterTypes) {
		this(customDataKey, Arrays.asList(parameterTypes));
	}
	
	/**
	 * Constructor
	 *
	 * @param customDataKey (required)
	 * @param parameterTypes can be <code>null</code> for none
	 */
	public ConstructorMatcher(final CustomDataKey<ConstructorMetadata> customDataKey, final Collection<? extends JavaType> parameterTypes) {
		Assert.notNull(customDataKey, "Custom data key is required, e.g. a ConstructorMetadataCustomDataKey");
		this.customDataKey = customDataKey;
		this.parameterTypes = new ArrayList<JavaType>();
		if (parameterTypes != null) {
			this.parameterTypes.addAll(parameterTypes);
		}
	}

	public List<ConstructorMetadata> matches(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
		List<ConstructorMetadata> constructors = new ArrayList<ConstructorMetadata>();
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberHoldingTypeDetailsList) {
			for (ConstructorMetadata constructor : memberHoldingTypeDetails.getDeclaredConstructors()) {
				if (parameterTypes.equals(AnnotatedJavaType.convertFromAnnotatedJavaTypes(constructor.getParameterTypes()))) {
					constructors.add(constructor);
				}
			}
		}
		return constructors;
	}

	public CustomDataKey<ConstructorMetadata> getCustomDataKey() {
		return customDataKey;
	}

	public Object getTagValue(ConstructorMetadata key) {
		return null;
	}
}
