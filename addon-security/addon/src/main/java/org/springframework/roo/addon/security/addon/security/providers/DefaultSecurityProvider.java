package org.springframework.roo.addon.security.addon.security.providers;

import java.lang.reflect.Modifier;
import java.util.Set;
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
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
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
  private TypeManagementService typeManagementService;
  private FileManager fileManager;

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

    if (starter != null) {
      isInstalledInModule = true;
    }

    return isInstalledInModule;
  }

  @Override
  public boolean isInstallationAvailable(String profile, JavaPackage configPackage) {
    return getProjectOperations().isFocusedProjectAvailable()
        && getProjectOperations().isFeatureInstalled(FeatureNames.MVC);
  }

  @Override
  public void install(JavaPackage configPackage, String profile, Pom module) {

    // Including dependency with Spring Boot Starter Security
    if (getProjectOperations().isMultimoduleProject()) {
      getProjectOperations().addDependencyToDependencyManagement(module.getModuleName(),
          SPRING_SECURITY_STARTER);
    }

    getProjectOperations().addDependency(module.getModuleName(), SPRING_SECURITY_STARTER);

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

    // Generating WebSecurityConfig.java class
    createWebSecurityConfigClass(configPackage, profile, module);

  }

  /**
   * Method that creates WebSecurityConfig.java class annotated with @RooWebSecurityConfiguration
   * 
   * @param configPackage
   * @param profile
   * @param module
   */
  public void createWebSecurityConfigClass(JavaPackage configPackage, String profile, Pom module) {

    // Create JavaType
    JavaType fileName =
        new JavaType(configPackage.getFullyQualifiedPackageName().concat(".").concat(profile)
            .concat(".WebSecurityConfig"), module.getModuleName());

    // Create file identifier
    final String fileIdentifier =
        PhysicalTypeIdentifier.createIdentifier(fileName,
            LogicalPath.getInstance(Path.SRC_MAIN_JAVA, fileName.getModule()));

    // Check if exists 
    if (getFileManager().exists(fileIdentifier)) {
      LOGGER.log(Level.INFO, String.format(
          "INFO: WebSecurityConfig.java already exists on '%s' package for '%s' profile",
          configPackage.getFullyQualifiedPackageName(), profile));
      return;
    }

    // Create physical type
    final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(fileIdentifier, Modifier.PUBLIC, fileName,
            PhysicalTypeCategory.CLASS);

    // Add annotation @RooWebSecurityConfiguration
    AnnotationMetadataBuilder webSecurityAnnotation =
        new AnnotationMetadataBuilder(RooJavaType.ROO_WEB_SECURITY_CONFIGURATION);
    webSecurityAnnotation.addStringAttribute("profile", profile);
    cidBuilder.addAnnotation(webSecurityAnnotation);

    // Write changes to disk
    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
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
}
