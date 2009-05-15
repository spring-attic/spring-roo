package org.springframework.roo.addon.jpa;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * ORM providers known to the JPA add-on.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
public class OrmProvider implements Comparable<OrmProvider> {

	private String provider;
	private String adapter;

	public static final OrmProvider HIBERNATE = new OrmProvider("HIBERNATE", "org.hibernate.ejb.HibernatePersistence");
	public static final OrmProvider OPENJPA = new OrmProvider("OPENJPA", "org.apache.openjpa.persistence.PersistenceProviderImpl");
	public static final OrmProvider ECLIPSELINK = new OrmProvider("ECLIPSELINK", "org.eclipse.persistence.jpa.PersistenceProvider");

	public OrmProvider(String provider, String adapter) {
		Assert.notNull(provider, "Provider required");
		Assert.notNull(adapter, "Adapter required");
		this.provider = provider;
		this.adapter = adapter;
	}
	
	public String getAdapter() {
		return adapter;
	}

	public final boolean equals(Object obj) {
		return obj != null && obj instanceof OrmProvider && this.compareTo((OrmProvider) obj) == 0;
	}

	public final int compareTo(OrmProvider o) {
		if (o == null)
			return -1;
		int result = this.provider.compareTo(o.provider);

		return result;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("provider", provider);
		return tsc.toString();
	}

	public String getKey() {
		return this.provider;
	}
}
