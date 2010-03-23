package org.springframework.roo.addon.jpa;

/**
 * Interface to commands available in {@link JpaOperationsImpl}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface JpaOperations {

	public abstract boolean isJpaInstallationPossible();

	public abstract boolean isJpaInstalled();

	/**
	 * This method is responsible for managing all JPA related artifacts (META-INF/persistence.xml, applicationContext.xml, database.properties and the project pom.xml)
	 * 
	 * @param ormProvider the ORM provider selected (Hibernate, OpenJpa, EclipseLink)
	 * @param database the database (HSQL, H2, MySql, etc)
	 */
	public abstract void configureJpa(OrmProvider ormProvider, JdbcDatabase database, String jndi);

}