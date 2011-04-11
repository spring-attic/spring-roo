package org.springframework.roo.model;

import org.springframework.roo.support.util.Assert;

/**
 * Assists in the creation of a {@link Builder} for types that eventually implement {@link CustomDataAccessor}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public abstract class AbstractCustomDataAccessorBuilder<T extends CustomDataAccessor> implements Builder<T> {
	private CustomDataBuilder customData = new CustomDataBuilder();

	protected AbstractCustomDataAccessorBuilder() {}
	
	protected AbstractCustomDataAccessorBuilder(CustomDataAccessor existing) {
		Assert.notNull(existing, "Custom data accessor required");
		this.customData = new CustomDataBuilder(existing.getCustomData());
	}
	
	public Object putCustomData(Object key, Object value) {
		return customData.put(key, value);
	}

	public CustomDataBuilder getCustomData() {
		return this.customData;
	}

	public void setCustomData(CustomDataBuilder customData) {
		this.customData = customData;
	}
}
