package org.springframework.roo.web.ui.domain;

/**
 * 
 * @author Juan Carlos García
 * @since 2.0
 *
 */
public class PersistenceConfiguration {

	private String persistenceProvider;
	private String database;
	private String databaseUrl;
	private String username;
	private String password;

	public PersistenceConfiguration(String fPersistenceProvider, String fDatabase, String fDatabaseUrl, String fUsername, String fPassword) {
		this.setPersistenceProvider(fPersistenceProvider);
		this.setDatabase(fDatabase);
		this.setDatabaseUrl(fDatabaseUrl);
		this.setUsername(fUsername);
		this.setPassword(fPassword);
	}

	/**
	 * @return the persistenceProvider
	 */
	public String getPersistenceProvider() {
		return persistenceProvider;
	}

	/**
	 * @param persistenceProvider the persistenceProvider to set
	 */
	public void setPersistenceProvider(String persistenceProvider) {
		this.persistenceProvider = persistenceProvider;
	}

	/**
	 * @return the databaseUrl
	 */
	public String getDatabaseUrl() {
		return databaseUrl;
	}

	/**
	 * @param databaseUrl the databaseUrl to set
	 */
	public void setDatabaseUrl(String databaseUrl) {
		this.databaseUrl = databaseUrl;
	}

	/**
	 * @return the database
	 */
	public String getDatabase() {
		return database;
	}

	/**
	 * @param database the database to set
	 */
	public void setDatabase(String database) {
		this.database = database;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

}
