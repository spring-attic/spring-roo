package org.springframework.roo.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Builder for {@link CustomData}.
 * <p>
 * Can be used to create new instances from scratch, or based on an existing
 * {@link CustomData} instance.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public class CustomDataBuilder implements Builder<CustomData> {

    private final Map<Object, Object> customData = new LinkedHashMap<Object, Object>();

    /**
     * Constructor for an empty builder
     */
    public CustomDataBuilder() {
    }

    /**
     * Constructor for a builder initialised with the given contents
     * 
     * @param existing can be <code>null</code>
     */
    public CustomDataBuilder(final CustomData existing) {
        append(existing);
    }

    /**
     * Appends the given custom data to this builder
     * 
     * @param customData the custom data to append; can be <code>null</code> to
     *            make no changes
     */
    public void append(final CustomData customData) {
        if (customData != null) {
            for (final Object key : customData.keySet()) {
                this.customData.put(key, customData.get(key));
            }
        }
    }

    public CustomData build() {
        return new CustomDataImpl(customData);
    }

    public void clear() {
        customData.clear();
    }

    public Object get(final Object key) {
        return customData.get(key);
    }

    public Set<Object> keySet() {
        return customData.keySet();
    }

    public Object put(final Object key, final Object value) {
        return customData.put(key, value);
    }

    public Object remove(final Object key) {
        return customData.remove(key);
    }

    public int size() {
        return customData.size();
    }

    public Collection<Object> values() {
        return customData.values();
    }
}
