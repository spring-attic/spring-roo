package org.springframework.roo.addon.security.addon.security;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.security.addon.security.providers.SecurityProvider;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Provides security installation services.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @author Sergio Clares
 * @author Juan Carlos García
 * @since 1.0
 */
@Component
@Service
public class SecurityOperationsImpl implements SecurityOperations {

  protected final static Logger LOGGER = HandlerUtils.getLogger(SecurityOperationsImpl.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private ProjectOperations projectOperations;

  private List<SecurityProvider> securityProviders = new ArrayList<SecurityProvider>();

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  @Override
  public void installSecurity(SecurityProvider type, Pom module) {

    Validate.notNull(type, "ERROR: You must provide a valid SecurityProvider to install.");

    // Delegates on the provided SecurityProvider to install Spring Security on current project
    type.install(module);
  }

  @Override
  public List<SecurityProvider> getAllSecurityProviders() {
    if (securityProviders.isEmpty()) {
      // Get all Services implement SecurityProvider interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(SecurityProvider.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          SecurityProvider securityProvider = (SecurityProvider) this.context.getService(ref);
          securityProviders.add(securityProvider);
        }

        return securityProviders;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load SecurityProvider on SecurityOperationsImpl.");
        return null;
      }
    } else {
      return securityProviders;
    }
  }

  public ProjectOperations getProjectOperations() {
    if (projectOperations == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (ProjectOperations) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on SecurityOperationsImpl.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }

  // FEATURE METHODS

  @Override
  public String getName() {
    return SECURITY_FEATURE_NAME;
  }

  @Override
  public boolean isInstalledInModule(String moduleName) {
    List<SecurityProvider> providers = getAllSecurityProviders();

    for (SecurityProvider provider : providers) {
      if (provider.isInstalledInModule(moduleName)) {
        return true;
      }
    }

    return false;
  }
}
