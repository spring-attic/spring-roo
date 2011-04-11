package org.springframework.roo.model;

import org.springframework.roo.support.util.Assert;

/**
 * Assists in the creation of a {@link Builder} for types that eventually implement {@link CustomDataAccessor}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public abstract class AbstractCustomDataAccessorBuilder<T extends CustomDataAccessor> implements Builder<T> {
	private CustomDataBuilder<T> customData = new CustomDataBuilder<T>();

	protected AbstractCustomDataAccessorBuilder() {}
	
	protected AbstractCustomDataAccessorBuilder(CustomDataAccessor<T> existing) {
		Assert.notNull(existing, "Custom data accessor required");
		this.customData = new CustomDataBuilder<T>(existing.getCustomData());
	}
	
	public Object putCustomData(TagKey<T> key, Object value) {
		return customData.put(key, value);
	}

	public CustomDataBuilder<T> getCustomData() {
		return this.customData;
	}

	public void setCustomData(CustomDataBuilder<T> customData) {
		this.customData = customData;
	}
}
