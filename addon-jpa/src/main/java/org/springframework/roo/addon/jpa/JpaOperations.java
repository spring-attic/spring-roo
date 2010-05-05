package org.springframework.roo.addon.jpa;

/**
 * Interface to commands available in {@link JpaOperationsImpl}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface JpaOperations {

	boolean isJpaInstallationPossible();

	boolean isJpaInstalled();
	
	/**
	 * This method is responsible for managing all JPA related artifacts (META-INF/persistence.xml, applicationContext.xml, database.properties and the project pom.xml)
	 * 
	 * @param ormProvider the ORM provider selected (Hibernate, OpenJpa, EclipseLink)
	 * @param database the database (HSQL, H2, MySql, etc)
	 * @param jndi the JNDI datasource
	 * @param applicationId the Google App Engine application identifier. Defaults to the project's name if not specified.
	 */
	void configureJpa(OrmProvider ormProvider, JdbcDatabase database, String jndi, String applicationId);
}