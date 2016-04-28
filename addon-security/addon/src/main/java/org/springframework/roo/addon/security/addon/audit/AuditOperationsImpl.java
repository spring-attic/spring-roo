package org.springframework.roo.addon.security.addon.audit;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.*;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.security.addon.security.SecurityOperationsImpl;
import org.springframework.roo.classpath.*;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.*;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Provides audit installation services.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class AuditOperationsImpl implements AuditOperations {

  protected final static Logger LOGGER = HandlerUtils.getLogger(SecurityOperationsImpl.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private final static Dependency SPRING_DATA_JPA = new Dependency("org.springframework.data",
      "spring-data-jpa", "");
  private final static Dependency SPRING_DATA_COMMONS = new Dependency("org.springframework.data",
      "spring-data-commons", "");
  private final static Dependency SPRING_ASPECTS = new Dependency("org.springframework",
      "spring-aspects", "");

  private FileManager fileManager;
  private ProjectOperations projectOperations;
  private PathResolver pathResolver;
  private TypeLocationService typeLocationService;
  private TypeManagementService typeManagementService;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  @Override
  public String getName() {
    return AUDIT_FEATURE_NAME;
  }

  @Override
  public boolean isInstalledInModule(String moduleName) {
    Pom pom = getProjectOperations().getPomFromModuleName(moduleName);
    if (pom == null) {
      return false;
    }

    // Check if audit dependencies are in pom
    Set<Dependency> dependencies = pom.getDependencies();

    boolean hasDependencies =
        dependencies.contains(SPRING_DATA_JPA) && dependencies.contains(SPRING_DATA_COMMONS)
            && dependencies.contains(SPRING_ASPECTS);

    return hasDependencies;
  }

  @Override
  public boolean isAuditSetupPossible() {
    return getProjectOperations().isFeatureInstalled(FeatureNames.SECURITY)
        && getProjectOperations().isFeatureInstalled(FeatureNames.JPA)
        && !getProjectOperations().isFeatureInstalled(AUDIT_FEATURE_NAME);
  }

  @Override
  public boolean isAuditAddPossible() {
    return getProjectOperations().isFeatureInstalled(FeatureNames.SECURITY)
        && getProjectOperations().isFeatureInstalled(FeatureNames.AUDIT);
  }

  @Override
  public void setupAudit(JavaPackage javaPackage) {

    // If package is not defined, get the top level package of the application module
    if (javaPackage == null) {
      Pom module = getProjectOperations().getFocusedModule();

      if (!getTypeLocationService().hasModuleFeature(module, ModuleFeatureName.APPLICATION)) {
        List<Pom> modules =
            (List<Pom>) getTypeLocationService().getModules(ModuleFeatureName.APPLICATION);
        if (modules.size() == 0) {
          throw new RuntimeException(String.format("ERROR: Not found a module with %s feature",
              ModuleFeatureName.APPLICATION));
        }
        module = modules.get(0);
      }
      javaPackage = getProjectOperations().getTopLevelPackage(module.getModuleName());

    } else if (!getTypeLocationService().hasModuleFeature(
        getProjectOperations().getPomFromModuleName(javaPackage.getModule()),
        ModuleFeatureName.APPLICATION)) {
      throw new IllegalArgumentException(String.format(
          "ERROR: Module %s does not have installed the %s feature", javaPackage.getModule(),
          ModuleFeatureName.APPLICATION));
    }

    // Add dependencies to selected pom.xml
    updateDependenciesToSpecifiedModule(javaPackage.getModule());

    // Create class for being aware of changes to entities
    createAuditorAware(javaPackage);

    // Update @RooSecurityConfiguration with enableJpaAuditing
    updateRooSecurityAnnotation();

  }

  @Override
  public void auditAdd(JavaType entity, String createdDateColumn, String modifiedDateColumn,
      String createdByColumn, String modifiedByColumn) {

    // Create @RooAudit
    AnnotationMetadataBuilder rooAudit = new AnnotationMetadataBuilder(RooJavaType.ROO_AUDIT);

    // Add parameters if required
    if (createdDateColumn != null) {
      rooAudit.addStringAttribute("createdDateColumn", createdDateColumn);
    }
    if (modifiedDateColumn != null) {
      rooAudit.addStringAttribute("modifiedDateColumn", modifiedDateColumn);
    }
    if (createdByColumn != null) {
      rooAudit.addStringAttribute("createdByColumn", createdByColumn);
    }
    if (modifiedByColumn != null) {
      rooAudit.addStringAttribute("modifiedByColumn", modifiedByColumn);
    }

    // Add annotation
    ClassOrInterfaceTypeDetails cid = getTypeLocationService().getTypeDetails(entity);
    ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(cid);
    cidBuilder.addAnnotation(rooAudit.build());

    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
  }

  /**
   * Add required dependencies for audit operations from configuration.xml file, if required
   *
   * @param moduleName which needs the dependencies to be added.
   */
  private void updateDependenciesToSpecifiedModule(String moduleName) {

    Validate.notNull(moduleName, "Module required");

    // Parse the configuration.xml file
    final Element configuration = XmlUtils.getConfiguration(getClass());
    final List<Dependency> dependencies = new ArrayList<Dependency>();
    final List<Element> auditDependencies =
        XmlUtils.findElements("/configuration/dependencies/dependency", configuration);
    for (final Element dependencyElement : auditDependencies) {
      dependencies.add(new Dependency(dependencyElement));
    }
    getProjectOperations().addDependencies(moduleName, dependencies);
  }

  /**
   * Create an implementation of AuditionAware in the project for being aware
   * of the user who make changes to entities.
   * 
   * @param javaPackage
   */
  private void createAuditorAware(JavaPackage javaPackage) {

    Validate.notNull(javaPackage, "JavaPackage required");

    String packageName = javaPackage.getFullyQualifiedPackageName();

    final JavaType authenticationAuditorType =
        new JavaType(String.format("%s.AuthenticationAuditorAware", packageName),
            javaPackage.getModule());
    final String physicalPath =
        getTypeLocationService().getPhysicalTypeCanonicalPath(authenticationAuditorType,
            LogicalPath.getInstance(Path.SRC_MAIN_JAVA, javaPackage.getModule()));

    // Include implementation for AuditionAware from template
    InputStream inputStream = null;
    try {

      // Use defined template
      inputStream =
          FileUtils.getInputStream(getClass(), "AuthenticationAuditorAware-template._java");
      String input = IOUtils.toString(inputStream);

      // Replacing package
      input = input.replace("__PACKAGE__", packageName);

      // Creating AuthenticationAuditorAware implementation
      getFileManager().createOrUpdateTextFileIfRequired(physicalPath, input, true);
    } catch (final IOException e) {
      throw new IllegalStateException(String.format("Unable to create '%s'", physicalPath), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  /**
   * Finds all @RooSecurityConfiguration in the project and set its attribute "enableJpaAuditing" to true.
   */
  private void updateRooSecurityAnnotation() {
    Set<ClassOrInterfaceTypeDetails> securityConfigClasses =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_SECURITY_CONFIGURATION);
    for (ClassOrInterfaceTypeDetails securityConfigClass : securityConfigClasses) {

      // Update annotation
      AnnotationMetadataBuilder annotation =
          new AnnotationMetadataBuilder(
              securityConfigClass.getAnnotation(RooJavaType.ROO_SECURITY_CONFIGURATION));
      annotation.addBooleanAttribute("enableJpaAuditing", true);
      annotation.build();

      // Update class with updated annotation
      ClassOrInterfaceTypeDetailsBuilder securityConfigClassBuilder =
          new ClassOrInterfaceTypeDetailsBuilder(securityConfigClass);
      securityConfigClassBuilder.updateTypeAnnotation(annotation);

      // Save changes to disk
      getTypeManagementService().createOrUpdateTypeOnDisk(securityConfigClassBuilder.build());
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
        LOGGER.warning("Cannot load FileManager on SecurityOperationsImpl.");
        return null;
      }
    } else {
      return fileManager;
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
}
