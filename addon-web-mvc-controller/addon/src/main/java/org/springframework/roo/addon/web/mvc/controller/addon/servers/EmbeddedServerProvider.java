package org.springframework.roo.addon.web.mvc.controller.addon.servers;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Provider to manage the application configuration for embedded servers
 * 
 * @author Paula Navarro
 * @since 2.0
 */
@Component
@Service
public class EmbeddedServerProvider implements ServerProvider {

  private static Logger LOGGER = HandlerUtils.getLogger(EmbeddedServerProvider.class);

  //------------ OSGi component attributes ----------------
  private BundleContext context;

  ProjectOperations projectOperations;
  private List<ServerProvider> serverProviders = new ArrayList<ServerProvider>();


  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  @Override
  public void setup(Pom module) {}

  @Override
  public String getName() {
    return "EMBEDDED";
  }

  @Override
  public boolean isInstalledInModule(String moduleName) {
    return getProjectOperations().isFeatureInstalled(FeatureNames.MVC)
        && !isAnyOtherServerInstalled(moduleName);
  }

  /**
   * Checks if there is a server configuration already installed in the specified module
   * 
   * @param moduleName
   * @return
   */
  private boolean isAnyOtherServerInstalled(String moduleName) {

    if (serverProviders.isEmpty()) {
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ServerProvider.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          serverProviders.add((ServerProvider) this.context.getService(ref));
        }
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ServerProviders on EmbeddedServerProvider.");
        return false;
      }
    }

    for (ServerProvider serverProvider : serverProviders) {
      if (!serverProvider.getName().equals(getName())
          && serverProvider.isInstalledInModule(moduleName)) {
        return true;
      }
    }
    return false;
  }

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
        LOGGER.warning("Cannot load ProjectOperations on EmbeddedServerProvider.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }

}
