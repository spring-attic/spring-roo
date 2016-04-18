package org.springframework.roo.addon.security.addon.security;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Provides security installation services.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @author Sergio Clares
 * @since 1.0
 */
@Component
@Service
public class SecurityOperationsImpl implements SecurityOperations {

  protected final static Logger LOGGER = HandlerUtils.getLogger(SecurityOperationsImpl.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  public static final JavaType ROO_SECURITY_CONFIGURATION = new JavaType(
      "org.springframework.roo.addon.security.annotations.RooSecurityConfiguration");

  private FileManager fileManager;
  private PathResolver pathResolver;
  private ProjectOperations projectOperations;
  private TypeManagementService typeManagementService;
  private MetadataService metadataService;
  private TypeLocationService typeLocationService;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  @Override
  public void installSecurity(Pom module) {

    Validate.notNull(module, "Module required");

    // Parse the configuration.xml file
    final Element configuration = XmlUtils.getConfiguration(getClass());

    // Add dependencies to POM
    updateDependencies(configuration, module.getModuleName());

    // Create security config class
    createSecurityConfigClass(getProjectOperations().getPomFromModuleName(module.getModuleName()));

  }

  @Override
  public boolean isSecurityInstallationPossible() {
    return getProjectOperations().isFocusedProjectAvailable()
        && !getProjectOperations().isFeatureInstalled(SECURITY_FEATURE_NAME);
  }

  private void updateDependencies(final Element configuration, final String moduleName) {
    final List<Dependency> dependencies = new ArrayList<Dependency>();
    final List<Element> securityDependencies =
        XmlUtils.findElements("/configuration/spring-security/dependencies/dependency",
            configuration);
    for (final Element dependencyElement : securityDependencies) {
      dependencies.add(new Dependency(dependencyElement));
    }
    getProjectOperations().addDependencies(moduleName, dependencies);
  }

  @Override
  public String getName() {
    return SECURITY_FEATURE_NAME;
  }

  @Override
  public boolean isInstalledInModule(String moduleName) {
    Pom pom = getProjectOperations().getPomFromModuleName(moduleName);
    if (pom == null) {
      return false;
    }

    // Check if spring-boot-starter-data-jpa has been included
    Set<Dependency> dependencies = pom.getDependencies();
    Dependency starter =
        new Dependency("org.springframework.boot", "spring-boot-starter-security", "");

    boolean hasStarter = dependencies.contains(starter);

    return hasStarter;
  }

  /**
   * Creates config class for managing security with @RooSecurityConfiguration
   * 
   * @param module where security config file should be installed
   */
  public void createSecurityConfigClass(Pom module) {

    // Create JavaType
    JavaType fileName =
        new JavaType(module.getGroupId().concat(".config.SecurityConfiguration"),
            module.getModuleName());

    // Create file identifier
    final String fileIdentifier =
        PhysicalTypeIdentifier.createIdentifier(fileName,
            LogicalPath.getInstance(Path.SRC_MAIN_JAVA, fileName.getModule()));

    // Create physical type
    final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(fileIdentifier, Modifier.PUBLIC, fileName,
            PhysicalTypeCategory.CLASS);

    // Add annotation @RooSecurityConfiguration
    cidBuilder.addAnnotation(new AnnotationMetadataBuilder(ROO_SECURITY_CONFIGURATION));

    // Write changes to disk
    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
  }

  public FileManager getFileManager() {
    if (fileManager == null) {
      // Get all Services implement FileManager interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(FileManager.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (FileManager) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load FileManager on SecurityOperationsImpl.");
        return null;
      }
    } else {
      return fileManager;
    }
  }

  public PathResolver getPathResolver() {
    if (pathResolver == null) {
      // Get all Services implement PathResolver interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(PathResolver.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (PathResolver) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load PathResolver on SecurityOperationsImpl.");
        return null;
      }
    } else {
      return pathResolver;
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

  public TypeManagementService getTypeManagementService() {
    if (typeManagementService == null) {
      // Get all Services implement TypeManagementService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TypeManagementService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (TypeManagementService) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeManagementService on SecurityOperationsImpl.");
        return null;
      }
    } else {
      return typeManagementService;
    }
  }

  public MetadataService getMetadataService() {
    if (metadataService == null) {
      // Get all Services implement MetadataService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(MetadataService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (MetadataService) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load MetadataService on SecurityOperationsImpl.");
        return null;
      }
    } else {
      return metadataService;
    }
  }

  public TypeLocationService getTypeLocationService() {
    if (typeLocationService == null) {
      // Get all Services implement TypeLocationService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TypeLocationService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (TypeLocationService) this.context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeLocationService on SecurityOperationsImpl.");
        return null;
      }
    } else {
      return typeLocationService;
    }
  }

}
