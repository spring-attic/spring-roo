package org.springframework.roo.addon.roobot.client;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Display Addon symbolic name for command completion.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public class AddOnBundleSymbolicName implements
        Comparable<AddOnBundleSymbolicName> {

    /**
     * You can change this field name, but ensure getKey() returns a unique
     * value
     */
    private final String key;

    public AddOnBundleSymbolicName(final String key) {
        Validate.notBlank(key, "bundle symbolic name required");
        this.key = key;
    }

    public final int compareTo(final AddOnBundleSymbolicName o) {
        if (o == null) {
            return -1;
        }
        return key.compareTo(o.key);
    }

    @Override
    public final boolean equals(final Object obj) {
        return obj instanceof AddOnBundleSymbolicName
                && compareTo((AddOnBundleSymbolicName) obj) == 0;
    }

    public String getKey() {
        return key;
    }

    @Override
    public final int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("key", key);
        return builder.toString();
    }
}