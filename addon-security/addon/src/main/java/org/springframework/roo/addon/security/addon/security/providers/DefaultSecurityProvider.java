package org.springframework.roo.addon.security.addon.security.providers;

import java.util.Set;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;

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

  private ProjectOperations projectOperations;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
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
    if (getProjectOperations().isMultimoduleProject()) {
      getProjectOperations().addDependencyToDependencyManagement(module.getModuleName(),
          SPRING_SECURITY_STARTER);
    }

    getProjectOperations().addDependency(module.getModuleName(), SPRING_SECURITY_STARTER);

  }

  // Service references

  public ProjectOperations getProjectOperations() {
    if (projectOperations == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          projectOperations = (ProjectOperations) this.context.getService(ref);
          return projectOperations;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on DefaultSecurityProvider.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }
}
