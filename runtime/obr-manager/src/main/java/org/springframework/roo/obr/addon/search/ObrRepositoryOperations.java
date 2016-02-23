package org.springframework.roo.obr.addon.search;

import java.io.IOException;


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
   * @throws Exception 
   */
  void removeRepo(String url) throws Exception;


  /**
   * Lists all installed OBR repositories
   * @throws Exception 
   */
  void listRepos() throws Exception;


  /**
   * Introspects all installed OBR Repositories and list all their addons
   * @throws Exception 
   */
  void introspectRepos() throws Exception;

}
