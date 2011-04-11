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
 */
public final class CustomDataBuilder<T extends CustomDataAccessor> implements Builder<CustomData<T>> {
	private Map<TagKey<T>, Object> customData = new HashMap<TagKey<T>, Object>();
	
	public CustomDataBuilder() {}
	
	public CustomDataBuilder(CustomData<T> existing) {
		append(existing);
	}
	
	public CustomData<T> build() {
		return new CustomDataImpl<T>(customData);
	}
	
	public void clear() {
		customData.clear();
	}

	public void append(CustomData<T> existing) {
		Assert.notNull(existing, "Existing custom data required");
		for (TagKey<T> key : existing) {
			customData.put(key, existing.get(key));
		}
	}
	
	public Object get(TagKey<T> key) {
		return customData.get(key);
	}

	public Set<TagKey<T>> keySet() {
		return customData.keySet();
	}

	public Object put(TagKey<T> key, Object value) {
		return customData.put(key, value);
	}

	public Object remove(TagKey<T> key) {
		return customData.remove(key);
	}

	public int size() {
		return customData.size();
	}

	public Collection<Object> values() {
		return customData.values();
	}
}
