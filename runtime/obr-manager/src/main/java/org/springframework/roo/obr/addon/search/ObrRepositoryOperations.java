package org.springframework.roo.obr.addon.search;


/**
 * Interface for operations offered by this addon.
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
public interface ObrRepositoryOperations {

	/**
	 * Add new OBR Repository to Spring Roo Shell
	 * @param url
	 * @throws Exception 
	 */
	void addRepository(String url) throws Exception;

	/**
	 * Removes an existing OBR Repository from Spring Roo Shell
	 * @param url
	 */
	void removeRepo(String url);
	
	
	/**
	 * Lists all installed OBR repositories
	 */
	void listRepos();


}