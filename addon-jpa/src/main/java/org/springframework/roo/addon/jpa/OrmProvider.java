package org.springframework.roo.addon.jpa;

import org.springframework.roo.support.style.ToStringCreator;

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

	private String adapter;
	private String alternateAdapter;

	private OrmProvider(String adapter, String alternateAdapter) {
		this.adapter = adapter;
		this.alternateAdapter = alternateAdapter;
	}

	private OrmProvider(String adapter) {
		this(adapter, "");
	}

	public String getAdapter() {
		return adapter;
	}

	public String getAlternateAdapter() {
		return alternateAdapter;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("provider", name());
		return tsc.toString();
	}
}
