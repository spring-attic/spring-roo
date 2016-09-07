package org.springframework.roo.addon.cache;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.cache.providers.CacheProvider;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Operations for the 'push-in' add-on.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class CacheOperationsImpl implements CacheOperations {

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private ProjectOperations projectOperations;
  private TypeLocationService typeLocationService;
  private TypeManagementService typeManagementService;

  protected void activate(final ComponentContext cContext) {
    this.context = cContext.getBundleContext();
  }

  private static final Logger LOGGER = HandlerUtils.getLogger(CacheOperationsImpl.class);

  @Override
  public boolean isCacheSetupAvailable() {
    return getProjectOperations().isFocusedProjectAvailable()
        && getProjectOperations().isFeatureInstalled(FeatureNames.JPA);
  }

  @Override
  public void setupCache(CacheProvider provider, String profile) {

    // Add spring-boot-starter-cache dependency
    List<Pom> modules =
        (List<Pom>) getTypeLocationService().getModules(ModuleFeatureName.APPLICATION);
    if (modules.size() == 0) {
      throw new RuntimeException(String.format("ERROR: Not found a module with %s feature",
          ModuleFeatureName.APPLICATION));
    }

    // Do the setup for each @SpringBootApplication module
    for (Pom module : modules) {
      addSpringCacheDependency(module);
    }

    // Add @EnableCache annotation to each application file
    Set<ClassOrInterfaceTypeDetails> applicationClasses =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            SpringJavaType.SPRING_BOOT_APPLICATION);
    for (ClassOrInterfaceTypeDetails applicationClass : applicationClasses) {
      if (applicationClass.getAnnotation(SpringJavaType.ENABLE_CACHING) == null) {
        ClassOrInterfaceTypeDetailsBuilder builder =
            new ClassOrInterfaceTypeDetailsBuilder(applicationClass);
        builder.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.ENABLE_CACHING));
        getTypeManagementService().createOrUpdateTypeOnDisk(builder.build());
      }
    }

    if (provider != null) {
      // Do setup of cache provider
      if (!provider.isInstalled()) {
        provider.setup(profile);
      }
    }
  }

  /**
   * Add Spring Cache starter to provided module.
   * 
   * @param module the Pom where the starter should be installed.
   */
  private void addSpringCacheDependency(Pom module) {

    // Parse the configuration.xml file
    final Element configuration = XmlUtils.getConfiguration(getClass());
    final List<Dependency> dependencies = new ArrayList<Dependency>();
    final List<Element> auditDependencies =
        XmlUtils.findElements("/configuration/cache/dependencies/dependency", configuration);
    for (final Element dependencyElement : auditDependencies) {
      dependencies.add(new Dependency(dependencyElement));
    }
    getProjectOperations().addDependencies(module.getModuleName(), dependencies);
  }

  /**
   * Method to obtain projectOperation service implementation
   * 
   * @return
   */
  public ProjectOperations getProjectOperations() {
    if (projectOperations == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          projectOperations = (ProjectOperations) context.getService(ref);
          return projectOperations;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on PushInOperationsImpl.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }

  /**
   * Method to obtain typeLocationService service implementation
   * 
   * @return
   */
  public TypeLocationService getTypeLocationService() {
    if (typeLocationService == null) {
      // Get all Services implement TypeLocationService interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(TypeLocationService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          typeLocationService = (TypeLocationService) context.getService(ref);
          return typeLocationService;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeLocationService on PushInOperationsImpl.");
        return null;
      }
    } else {
      return typeLocationService;
    }
  }

  /**
   * Method to obtain typeManagementService service implementation
   * 
   * @return
   */
  public TypeManagementService getTypeManagementService() {
    if (typeManagementService == null) {
      // Get all Services implement TypeManagementService interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(TypeManagementService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          typeManagementService = (TypeManagementService) context.getService(ref);
          return typeManagementService;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeManagementService on PushInOperationsImpl.");
        return null;
      }
    } else {
      return typeManagementService;
    }
  }

}
