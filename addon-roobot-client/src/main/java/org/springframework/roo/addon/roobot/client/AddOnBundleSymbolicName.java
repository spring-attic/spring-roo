package org.springframework.roo.addon.roobot.client;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Display Addon symbolic name for command completion.
 *
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
public class AddOnBundleSymbolicName implements Comparable<AddOnBundleSymbolicName> {

	/** You can change this field name, but ensure getKey() returns a unique value */
	private final String key;

	public AddOnBundleSymbolicName(final String key) {
		Assert.hasText(key, "bundle symbolic name required");
		this.key = key;
	}

	@Override
	public final int hashCode() {
		return this.key.hashCode();
	}

	@Override
	public final boolean equals(final Object obj) {
		return obj instanceof AddOnBundleSymbolicName && this.compareTo((AddOnBundleSymbolicName)obj) == 0;
	}

	public final int compareTo(final AddOnBundleSymbolicName o) {
		if (o == null) return -1;
		return this.key.compareTo(o.key);
	}

	public String getKey() {
		return this.key;
	}

	@Override
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("key", key);
		return tsc.toString();
	}
}