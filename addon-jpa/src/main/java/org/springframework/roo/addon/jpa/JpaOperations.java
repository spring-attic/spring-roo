package org.springframework.roo.addon.jpa;

import java.util.SortedSet;

/**
 * Provides JPA configuration operations.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface JpaOperations {

	boolean isJpaInstallationPossible();

	boolean isJpaInstalled();
	
	boolean hasDatabaseProperties();
	
	/**
	 * This method is responsible for managing all JPA related artifacts (META-INF/persistence.xml, applicationContext.xml, database.properties and the project pom.xml)
	 * 
	 * @param ormProvider the ORM provider selected (Hibernate, OpenJPA, EclipseLink)
	 * @param database the database (HSQL, H2, MySql, etc)
	 * @param jndi the JNDI datasource
	 * @param applicationId the Google App Engine application identifier. Defaults to the project's name if not specified.
	 * @param hostName the host name where the database is
	 * @param databaseName the name of the database
	 * @param userName the username to connect to the database
	 * @param password the password to connect to the database
	 * @param transactionManager the transaction manager name defined in the applicationContext.xml file
	 * @param persistenceUnit the name of the persistence unit defined in the persistence.xml file
	 */
	void configureJpa(OrmProvider ormProvider, JdbcDatabase database, String jndi, String applicationId, String hostName, String databaseName, String userName, String password, String transactionManager, String persistenceUnit);
	
	SortedSet<String> getDatabaseProperties();
}