package org.springframework.roo.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.support.util.Assert;


/**
 * Builder for {@link CustomData}.
 * 
 * <p>
 * Can be used to create new instances from scratch, or based on an existing {@link CustomData} instance.
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
public final class CustomDataBuilder implements Builder<CustomData> {
	private Map<Object, Object> customData = new HashMap<Object, Object>();
	
	public CustomDataBuilder() {}
	
	public CustomDataBuilder(CustomData existing) {
		Assert.notNull(existing, "Existing custom data required");
		for (Object key : existing.keySet()) {
			customData.put(key, existing.get(key));
		}
	}
	
	public CustomData build() {
		return new CustomDataImpl(customData);
	}
	
	public void clear() {
		customData.clear();
	}

	public Object get(Object key) {
		return customData.get(key);
	}

	public Set<Object> keySet() {
		return customData.keySet();
	}

	public Object put(Object key, Object value) {
		return customData.put(key, value);
	}

	public Object remove(Object key) {
		return customData.remove(key);
	}

	public int size() {
		return customData.size();
	}

	public Collection<Object> values() {
		return customData.values();
	}

}
