package org.springframework.roo.addon.jpa;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * ORM providers known to the JPA add-on.
 * 
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
public enum OrmProvider {

    DATANUCLEUS("org.datanucleus.api.jpa.PersistenceProviderImpl"), ECLIPSELINK(
            "org.eclipse.persistence.jpa.PersistenceProvider"), HIBERNATE(
            "org.hibernate.ejb.HibernatePersistence"), OPENJPA(
            "org.apache.openjpa.persistence.PersistenceProviderImpl");

    private final String adapter;

    /**
     * Constructor
     * 
     * @param adapter (required)
     */
    private OrmProvider(final String adapter) {
        Validate.notBlank(adapter, "Adapter is required");
        this.adapter = adapter;
    }

    public String getAdapter() {
        return adapter;
    }

    public String getConfigPrefix() {
        return "/configuration/ormProviders/provider[@id='" + name() + "']";
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("provider", name());
        builder.append("adapter", adapter);
        return builder.toString();
    }
}
