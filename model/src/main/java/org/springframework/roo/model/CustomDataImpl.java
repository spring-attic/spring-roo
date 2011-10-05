package org.springframework.roo.model;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.support.util.Assert;

/**
 * Default implementation of {@link CustomData}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public class CustomDataImpl implements CustomData {
	
	// Constants
	public static final CustomData NONE = new CustomDataImpl(new LinkedHashMap<Object, Object>());
	
	// Fields
	private Map<Object, Object> customData;
	
	public CustomDataImpl(Map<Object, Object> customData) {
		Assert.notNull(customData, "Custom data required");
		this.customData = Collections.unmodifiableMap(customData);
	}
	
	public Object get(Object key) {
		return customData.get(key);
	}

	public Set<Object> keySet() {
		return customData.keySet();
	}

	public Iterator<Object> iterator() {
		return customData.keySet().iterator();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		return prime * result + ((customData == null) ? 0 : customData.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CustomDataImpl other = (CustomDataImpl) obj;
		if (customData == null) {
			if (other.customData != null)
				return false;
		} else if (!customData.equals(other.customData))
			return false;
		return true;
	}

	public String toString() {
		return customData.toString();
	}
}
