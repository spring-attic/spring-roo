package org.springframework.roo.addon.layers.repository.mongo;

import java.math.BigInteger;

import org.springframework.roo.model.JavaType;

/**
 * Operations for Spring Data MongoDB repository add-on.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public interface MongoOperations {

	/**
	 * Indicate if the 'mongo setup' command should be available for this project.
	 * 
	 * @return true if command should be made available
	 */
	boolean isSetupCommandAvailable();
	
	/**
	 * Indicate if the 'repository mongo' command should be available for this project.
	 * 
	 * @return true if command should be made available
	 */
	boolean isRepositoryCommandAvailable();
	
	/**
	 * Setup current project for Spring Data MongoDB configuration.
	 * 
	 * @param username (optional)
	 * @param password (optional)
	 * @param name database name (optional, defaults to project name)
	 * @param port (optional, defaults to 27017)
	 * @param host (optional, defaults to 127.0.0.1)
	 * @param cloudFoundry indicate if project should be deployable on VMware CloudFoundry (optional, defaults to false)
	 */
	void setup(String username, String password, String name, String port, String host, boolean cloudFoundry);
	
	/**
	 * Creates a new Repository interface for Spring Data JPA MongoDB integration.
	 * 
	 * @param interfaceType (required)
	 * @param classType (optional)
	 * @param domainType (required)
	 */
	void setupRepository(JavaType interfaceType, JavaType classType, JavaType domainType);

	/**
	 * Creates a new domain type ready for backing a Spring Data MongoDB repository
	 * 
	 * @param classType (required)
	 * @param idType (optional, defaults to {@link BigInteger}
	 */
	void createType(JavaType classType, JavaType idType);

}
