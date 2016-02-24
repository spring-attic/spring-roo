package org.springframework.roo.addon.suite;


/**
 * Interface for operations offered by this addon.
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
public interface AddonSuiteOperations {

  /**
   * Install and Deploy Roo Addon Suite on Spring Roo Shell
   * 
   * @param suiteSymbolicName
   * @throws Exception 
   */
  void installRooAddonSuiteByName(ObrAddonSuiteSymbolicName suiteSymbolicName) throws Exception;

  /**
   * Install Roo Addon Suite using a URL
   * 
   * @param url
   * @throws Exception
   */
  void installRooAddonSuiteByUrl(String url) throws Exception;


  /**
   * Starts Roo Addon Suite
   * 
   * @param symbolicName
   */
  void startRooAddonSuite(AddonSuiteSymbolicName symbolicName);

  /**
   * Stop Roo Addon Suite
   * 
   * @param symbolicName
   */
  void stopRooAddonSuite(AddonSuiteSymbolicName symbolicName);

  /**
   * Uninstall some Roo Addon Suite
   * 
   * @param symbolicName
   */
  void uninstallRooAddonSuite(AddonSuiteSymbolicName symbolicName);

  /**
   * Lists all Subsystems installed on Roo Shell
   */
  void listAllInstalledSubsystems();


  /**
   * Lists all Subsystems located on obrRepository
   * 
   * @param obrRepository
   */
  void listAllSubsystemsOnRepository(ObrRepositorySymbolicName obrRepository);

}
