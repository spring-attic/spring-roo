package org.springframework.roo.classpath.itd;

import org.springframework.roo.support.util.Assert;

/**
 * Indicates a role that other {@link ItdMetadataProvider}s may wish to discover.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class ItdProviderRole implements Comparable<ItdProviderRole> {
	public static final ItdProviderRole ACCESSOR_MUTATOR = new ItdProviderRole("ACCESSOR_MUTATOR");
	
	private String name;
	
	/**
	 * Creates a name with the specified string.
	 * 
	 * @param name the name (required)
	 */
	public ItdProviderRole(String name) {
		Assert.hasText(name, "Name required");
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	public int hashCode() {
		return this.name.hashCode();
	}

	public boolean equals(Object obj) {
		return obj != null && obj instanceof ItdProviderRole && this.compareTo((ItdProviderRole)obj) == 0;
	}

	public int compareTo(ItdProviderRole o) {
		if (o == null) {
			throw new NullPointerException();
		}
		return name.compareTo(o.name);
	}
	
	public final String toString() {
		return name;
	}
}
