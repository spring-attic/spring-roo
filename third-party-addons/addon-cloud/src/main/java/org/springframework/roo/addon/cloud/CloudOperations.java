package org.springframework.roo.addon.cloud;

import java.util.List;

import org.springframework.roo.addon.cloud.providers.CloudProviderId;

/**
 * Provides operations to install Cloud Provider that provides functions to
 * deploy Spring Roo Application on Cloud Servers.
 * 
 * @author Juan Carlos García del Canto
 * @since 1.2.6
 */
public interface CloudOperations {
	
	/**
	 * This method checks if setup command is available
	 * 
	 * @return (boolean) 
	 */
	boolean isSetupCommandAvailable();

	/**
	 * This method execute install provider method
	 * 
	 * @param provider
	 * @param configuration 
	 */
	void installProvider(CloudProviderId provider, String configuration);

	/**
	 * 
	 * Get available providers on the system
	 * 
	 * @return A CloudProviderId List
	 */
	List<CloudProviderId> getProvidersId();

	/**
	 * Gets the current provider by name
	 * 
	 * @param name
	 *            Provider Name
	 * @return CloudProviderId
	 */
	CloudProviderId getProviderIdByName(String name);

}