package org.springframework.roo.model;

import org.springframework.roo.support.util.Assert;

/**
 * Convenience superclass for {@link CustomDataAccessor} implementations.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public abstract class AbstractCustomDataAccessorProvider implements CustomDataAccessor {
	
	// Fields
	private final CustomData customData;

	/**
	 * Constructor
	 *
	 * @param customData
	 */
	protected AbstractCustomDataAccessorProvider(final CustomData customData) {
		Assert.notNull(customData, "Custom data required");
		this.customData = customData;
	}

	public final CustomData getCustomData() {
		return customData;
	}
}
