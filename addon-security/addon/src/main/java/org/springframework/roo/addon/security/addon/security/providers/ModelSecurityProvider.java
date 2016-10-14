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
import org.springframework.roo.project.Property;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of SecurityProvider to work with the domain
 * model during the authentication process.
 * 
 * The name of this provider is "MODEL" and must be unique. It will be used to 
 * recognize this Spring Security Provider.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class ModelSecurityProvider implements SecurityProvider {

  private static final Property SPRINGLETS_VERSION_PROPERTY = new Property("springlets.version",
      "1.0.0.BUILD-SNAPSHOT");
  private static final Dependency SPRINGLETS_SECURITY_AUTHENTICATION_STARTER_WITH_VERSION =
      new Dependency("io.springlets", "springlets-boot-starter-security-authentication",
          "${springlets.version}");
  private static final Dependency SPRINGLETS_SECURITY_AUTHENTICATION_STARTER_WITHOUT_VERSION =
      new Dependency("io.springlets", "springlets-boot-starter-security-authentication", null);


  protected final static Logger LOGGER = HandlerUtils.getLogger(ModelSecurityProvider.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private ProjectOperations projectOperations;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  @Override
  public String getName() {
    return "MODEL";
  }

  @Override
  public boolean isInstalledInModule(String moduleName) {
    boolean isInstalledInModule = false;
    Pom module = getProjectOperations().getPomFromModuleName(moduleName);
    Set<Dependency> starter =
        module
            .getDependenciesExcludingVersion(SPRINGLETS_SECURITY_AUTHENTICATION_STARTER_WITH_VERSION);

    if (!starter.isEmpty()) {
      isInstalledInModule = true;
    }

    return isInstalledInModule;
  }

  @Override
  public boolean isInstallationAvailable() {
    return getProjectOperations().isFocusedProjectAvailable()
        && getProjectOperations().isFeatureInstalled(FeatureNames.MVC)
        && !getProjectOperations().isFeatureInstalled("MODEL");
  }

  @Override
  public void install(Pom module) {

    // Include Springlets Starter project dependencies and properties
    getProjectOperations().addProperty("", SPRINGLETS_VERSION_PROPERTY);

    if (getProjectOperations().isMultimoduleProject()) {

      // If current project is a multimodule project, include dependencies first
      // on dependencyManagement and then on current module
      getProjectOperations().addDependencyToDependencyManagement("",
          SPRINGLETS_SECURITY_AUTHENTICATION_STARTER_WITH_VERSION);
      getProjectOperations().addDependency(module.getModuleName(),
          SPRINGLETS_SECURITY_AUTHENTICATION_STARTER_WITHOUT_VERSION);

    } else {

      // If not multimodule, include dependencies on root
      getProjectOperations().addDependency("",
          SPRINGLETS_SECURITY_AUTHENTICATION_STARTER_WITH_VERSION);
    }

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
