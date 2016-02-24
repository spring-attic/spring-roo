package org.springframework.roo.addon.suite;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * {@link Converter} for {@link ObrAddonSuiteSymbolicName}.
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
@Component
@Service
public class ObrAddonSuiteSymbolicNameConverter implements Converter<ObrAddonSuiteSymbolicName> {

  private BundleContext context;
  private static final Logger LOGGER = HandlerUtils
      .getLogger(ObrAddonSuiteSymbolicNameConverter.class);

  @Reference
  private AddonSuiteOperations operations;
  private RepositoryAdmin repositoryAdmin;

  private List<Repository> repositories;

  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    repositories = new ArrayList<Repository>();
  }


  public ObrAddonSuiteSymbolicName convertFromText(final String value, final Class<?> requiredType,
      final String optionContext) {
    return new ObrAddonSuiteSymbolicName(value.trim());
  }

  public boolean getAllPossibleValues(final List<Completion> completions,
      final Class<?> requiredType, final String originalUserInput, final String optionContext,
      final MethodTarget target) {

    // Getting installed repositories
    populateRepositories();

    for (Repository repo : repositories) {
      // Getting all resources from repository
      Resource[] repositoryResource = repo.getResources();
      for (Resource resource : repositoryResource) {
        // If current resource ends with .esa, means that is a ROO Addon Suite
        if (resource.getURI().endsWith(".esa")) {
          completions.add(new Completion(resource.getSymbolicName()));
        }
      }
    }
    return false;
  }

  public boolean supports(final Class<?> requiredType, final String optionContext) {
    return ObrAddonSuiteSymbolicName.class.isAssignableFrom(requiredType);
  }

  /**
  * Method to populate current Repositories using OSGi Serive
  */
  private void populateRepositories() {

    // Cleaning Repositories
    repositories.clear();

    // Validating that RepositoryAdmin exists
    Validate.notNull(getRepositoryAdmin(), "RepositoryAdmin not found");

    for (Repository repo : getRepositoryAdmin().listRepositories()) {
      repositories.add(repo);
    }
  }

  /**
   * Method to get RepositoryAdmin Service implementation
   * 
   * @return
   */
  public RepositoryAdmin getRepositoryAdmin() {
    if (repositoryAdmin == null) {
      // Get all Services implement RepositoryAdmin interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(RepositoryAdmin.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          repositoryAdmin = (RepositoryAdmin) context.getService(ref);
          return repositoryAdmin;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load RepositoryAdmin on AddonSearchImpl.");
        return null;
      }
    } else {
      return repositoryAdmin;
    }
  }
}
