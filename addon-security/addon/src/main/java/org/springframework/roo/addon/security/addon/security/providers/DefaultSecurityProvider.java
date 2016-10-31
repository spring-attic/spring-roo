package org.springframework.roo.addon.security.addon.security.providers;

import java.util.Set;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.application.config.ApplicationConfigService;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

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


  protected final static Logger LOGGER = HandlerUtils.getLogger(DefaultSecurityProvider.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private static final Dependency SPRING_SECURITY_STARTER = new Dependency(
      "org.springframework.boot", "spring-boot-starter-security", null);

  private ServiceInstaceManager serviceManager = new ServiceInstaceManager();

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    this.serviceManager.activate(this.context);
  }

  @Override
  public String getName() {
    return "DEFAULT";
  }

  @Override
  public boolean isInstalledInModule(String moduleName) {

    boolean isInstalledInModule = false;
    Pom module = getProjectOperations().getPomFromModuleName(moduleName);
    Set<Dependency> starter = module.getDependenciesExcludingVersion(SPRING_SECURITY_STARTER);

    if (!starter.isEmpty()) {
      isInstalledInModule = true;
    }

    return isInstalledInModule;
  }

  @Override
  public boolean isInstallationAvailable() {
    return getProjectOperations().isFocusedProjectAvailable()
        && getProjectOperations().isFeatureInstalled(FeatureNames.MVC)
        && !getProjectOperations().isFeatureInstalled("DEFAULT");
  }

  @Override
  public void install(Pom module) {

    // Including dependency with Spring Boot Starter Security
    getProjectOperations().addDependency(module.getModuleName(), SPRING_SECURITY_STARTER);

    // Add property security.enable-csrf with true value to enable CSRF
    getApplicationConfigService().addProperty(module.getModuleName(), "security.enable-csrf",
        "true", "", true);
    getApplicationConfigService().addProperty(module.getModuleName(), "security.enable-csrf",
        "true", "dev", true);

    // Add thymeleaf-extras-springsecurity4 dependency with Thymeleaf 3 support
    getProjectOperations().addProperty(module.getModuleName(),
        new Property("thymeleaf-extras-springsecurity4.version", "3.0.0.RELEASE"));
    getProjectOperations().addDependency(module.getModuleName(),
        new Dependency("org.thymeleaf.extras", "thymeleaf-extras-springsecurity4", null));

  }

  // Service references

  private ProjectOperations getProjectOperations() {
    return serviceManager.getServiceInstance(this, ProjectOperations.class);
  }

  private ApplicationConfigService getApplicationConfigService() {
    return serviceManager.getServiceInstance(this, ApplicationConfigService.class);
  }
}
