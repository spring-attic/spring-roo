package org.springframework.roo.addon.security.addon.security.providers;

import org.springframework.roo.model.JavaPackage;
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
   * @param profile the profile where the included configuration will be used
   * @param configPackage the package where the configuration files will be included
   *
   * @return true if installation process is available
   */
  boolean isInstallationAvailable(String profile, JavaPackage configPackage);

  /**
  * This operation will install all the necessary elements related with the
  * specified provider.
  *
  * @param configPackage
  * @param profile
  * @param module
  */
  void install(JavaPackage configPackage, String profile, Pom module);

}
