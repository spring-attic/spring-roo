package org.springframework.roo.addon.security.addon.security.providers;

import org.springframework.roo.project.Feature;
import org.springframework.roo.project.maven.Pom;

/**
 * That @Component that implement this interface, will be 
 * provided as Spring Security Provider.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface SecurityProvider extends Feature {

  /**
   * This operation will check if the installation process is available in
   * the specified provider.
   *
   * @return true if installation process is available
   */
  boolean isInstallationAvailable();

  /**
  * This operation will install all the necessary elements related with the
  * specified provider.
  *
  * @param module
  */
  void install(Pom module);

}
