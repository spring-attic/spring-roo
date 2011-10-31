package org.springframework.roo.addon.jpa;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * ORM providers known to the JPA add-on.
 *
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
public enum OrmProvider {

	HIBERNATE("org.hibernate.ejb.HibernatePersistence"),
	OPENJPA("org.apache.openjpa.persistence.PersistenceProviderImpl"),
	ECLIPSELINK("org.eclipse.persistence.jpa.PersistenceProvider"),
	DATANUCLEUS("org.datanucleus.api.jpa.PersistenceProviderImpl");

	// Fields
	private final String adapter;

	/**
	 * Constructor
	 *
	 * @param adapter (required)
	 */
	private OrmProvider(final String adapter) {
		Assert.hasText(adapter, "Adapter is required");
		this.adapter = adapter;
	}

	public String getAdapter() {
		return adapter;
	}

	@Override
	public String toString() {
		final ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("provider", name());
		tsc.append("adapter", adapter);
		return tsc.toString();
	}
}
