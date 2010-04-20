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
	GOOGLE_APP_ENGINE("org.datanucleus.store.appengine.jpa.DatastorePersistenceProvider");

	private String adapter;

	private OrmProvider(String adapter) {
		this.adapter = adapter;
	}

	public String getAdapter() {
		return adapter;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("provider", name());
		return tsc.toString();
	}
}
