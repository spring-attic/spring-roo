package org.springframework.roo.addon.security.addon.security.providers;

import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
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

  protected final static Logger LOGGER = HandlerUtils.getLogger(ModelSecurityProvider.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private ProjectOperations projectOperations;
  private TypeManagementService typeManagementService;
  private FileManager fileManager;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  @Override
  public String getName() {
    return "MODEL";
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

    // First, delegate on DEFAULT security provider to include the DEFAULT Spring
    // Security configuration
    getDefaultSecurityProvider().install(configPackage, profile, module);

    // After that, continue with own implementation for MODEL Spring Security 
    // provider.

    // Include Springlets project dependencies and properties
    getProjectOperations().addProperty("", SPRINGLETS_VERSION_PROPERTY);

    if (getProjectOperations().isMultimoduleProject()) {

      // If current project is a multimodule project, include dependencies first
      // on dependencyManagement and then on current module
      getProjectOperations().addDependencyToDependencyManagement("",
          new Dependency("io.springlets", "springlets-security", "${springlets.version}"));
      getProjectOperations().addDependency(module.getModuleName(),
          new Dependency("io.springlets", "springlets-security", null));

    } else {

      // If not multimodule, include dependencies on root
      getProjectOperations().addDependency("",
          new Dependency("io.springlets", "springlets-security", "${springlets.version}"));
    }

    // Checking configPackage. 
    // If null, use ~.config by default
    if (configPackage == null) {
      configPackage =
          new JavaPackage(getProjectOperations().getTopLevelPackage(module.getModuleName())
              .getFullyQualifiedPackageName().concat(".config"), module.getModuleName());
    }

    // Checking profile
    // If null or empty, use dev by default
    if (StringUtils.isEmpty(profile)) {
      profile = "dev";
    }

    // Generating GlobalSecurityConfiguration.java class
    createGlobalSecurityConfigurationClass(configPackage, profile, module);
  }

  /**
   * 
   * Method that creates GlobalSecurityConfiguration.java class annotated with 
   * @RooModelGlobalSecurityConfig
   * 
   * @param configPackage
   * @param profile
   * @param module
   */
  public void createGlobalSecurityConfigurationClass(JavaPackage configPackage, String profile,
      Pom module) {

    // Create JavaType
    JavaType fileName =
        new JavaType(configPackage.getFullyQualifiedPackageName().concat(".").concat(profile)
            .concat(".GlobalSecurityConfig"), module.getModuleName());

    // Create file identifier
    final String fileIdentifier =
        PhysicalTypeIdentifier.createIdentifier(fileName,
            LogicalPath.getInstance(Path.SRC_MAIN_JAVA, fileName.getModule()));

    // Check if exists 
    if (getFileManager().exists(fileIdentifier)) {
      LOGGER.log(Level.INFO, String.format(
          "INFO: GlobalSecurityConfig.java already exists on '%s' package for '%s' profile",
          configPackage.getFullyQualifiedPackageName(), profile));
      return;
    }

    // Create physical type
    final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(fileIdentifier, Modifier.PUBLIC, fileName,
            PhysicalTypeCategory.CLASS);

    // Add annotation @RooModelGlobalSecurityConfig
    AnnotationMetadataBuilder modelGlobalSecurityConfig =
        new AnnotationMetadataBuilder(RooJavaType.ROO_MODEL_GLOBAL_SECURITY_CONFIG);
    modelGlobalSecurityConfig.addStringAttribute("profile", profile);
    cidBuilder.addAnnotation(modelGlobalSecurityConfig);

    // Write changes to disk
    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
  }

  /**
   * This method obtains the DEFAULT implementation of Security Provider
   * 
   * @return An implementation of SecurityProvider interface that provides
   * default Spring Security configuration.
   */
  private DefaultSecurityProvider getDefaultSecurityProvider() {
    // Get all Services implement SecurityProvider interface
    try {
      ServiceReference<?>[] references =
          this.context.getAllServiceReferences(SecurityProvider.class.getName(), null);

      for (ServiceReference<?> ref : references) {
        SecurityProvider securityProvider = (SecurityProvider) this.context.getService(ref);
        if (securityProvider.getName().equals("DEFAULT")) {
          return (DefaultSecurityProvider) securityProvider;
        }
      }

      return null;

    } catch (InvalidSyntaxException e) {
      LOGGER.warning("Cannot load DefaultSecurityProvider on ModelSecurityProvider.");
      return null;
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

  public FileManager getFileManager() {
    if (fileManager == null) {
      // Get all Services implement FileManager interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(FileManager.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          fileManager = (FileManager) this.context.getService(ref);
          return fileManager;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load FileManager on DefaultSecurityProvider.");
        return null;
      }
    } else {
      return fileManager;
    }
  }

  public TypeManagementService getTypeManagementService() {
    if (typeManagementService == null) {
      // Get all Services implement TypeManagementService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TypeManagementService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          typeManagementService = (TypeManagementService) this.context.getService(ref);
          return typeManagementService;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeManagementService on DefaultSecurityProvider.");
        return null;
      }
    } else {
      return typeManagementService;
    }
  }

}
