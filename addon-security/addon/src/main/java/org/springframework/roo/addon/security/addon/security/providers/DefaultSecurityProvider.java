package org.springframework.roo.addon.security.addon.security.providers;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.maven.Pom;

/**
 * Implementation of SecurityProvider to work with the default 
 * configuration provided by Spring Boot.
 * 
 * The name of this provider is "DEFAULT" and must be unique. It will be used to 
 * recognize this Spring Security Provider.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class DefaultSecurityProvider implements SecurityProvider {

  @Override
  public String getName() {
    return "DEFAULT";
  }

  @Override
  public boolean isInstalledInModule(String moduleName) {
    return false;
  }

  @Override
  public boolean isInstallationAvailable(String profile, JavaPackage configPackage) {
    return false;
  }

  @Override
  public void install(JavaPackage configPackage, String profile, Pom module) {
    // TODO Auto-generated method stub

  }
}
