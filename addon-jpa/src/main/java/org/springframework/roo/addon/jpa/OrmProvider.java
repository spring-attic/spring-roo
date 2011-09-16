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
	DATANUCLEUS("org.datanucleus.jpa.PersistenceProviderImpl", "org.datanucleus.store.appengine.jpa.DatastorePersistenceProvider"),
	DATANUCLEUS_2("org.datanucleus.jpa.PersistenceProviderImpl", "com.force.sdk.jpa.PersistenceProviderImpl");

	// Fields
	private final String adapter;
	private final String alternateAdapter;

	/**
	 * Constructor that accepts an alternate adapter
	 *
	 * @param adapter (required)
	 * @param alternateAdapter (can be blank)
	 */
	private OrmProvider(final String adapter, final String alternateAdapter) {
		Assert.hasText(adapter, "Adapter is required");
		this.adapter = adapter;
		this.alternateAdapter = alternateAdapter;
	}

	/**
	 * Constructor for no alternate adapter
	 *
	 * @param adapter (required)
	 */
	private OrmProvider(final String adapter) {
		this(adapter, "");
	}

	public String getAdapter() {
		return adapter;
	}

	public String getAlternateAdapter() {
		return alternateAdapter;
	}

	@Override
	public String toString() {
		final ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("provider", name());
		return tsc.toString();
	}
}
