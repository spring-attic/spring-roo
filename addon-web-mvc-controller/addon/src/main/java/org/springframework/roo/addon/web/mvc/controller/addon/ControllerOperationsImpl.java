package org.springframework.roo.addon.web.mvc.controller.addon;

import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.model.RooJavaType.ROO_WEB_MVC_CONFIGURATION;

import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.application.config.ApplicationConfigService;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link ControllerOperations}.
 * 
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @since 1.0
 */
@Component
@Service
public class ControllerOperationsImpl implements ControllerOperations {

  private static final Logger LOGGER = HandlerUtils.getLogger(ControllerOperationsImpl.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private static final JavaSymbolName PATH = new JavaSymbolName("path");
  private static final JavaSymbolName VALUE = new JavaSymbolName("value");

  private ProjectOperations projectOperations;
  private TypeLocationService typeLocationService;
  private PathResolver pathResolver;
  private FileManager fileManager;
  private TypeManagementService typeManagementService;
  private ApplicationConfigService applicationConfigService;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  /**
   * This operation will check if setup operation is available
   * 
   * @return true if setup operation is available. false if not.
   */
  @Override
  public boolean isSetupAvailable() {
    return getProjectOperations().isFocusedProjectAvailable()
        && !getProjectOperations().isFeatureInstalled(FeatureNames.MVC);
  }

  /**
   * This operation will setup Spring MVC on generated project.
   * 
   * @param module 
   *            Pom module where Spring MVC should be included
   */
  @Override
  public void setup(Pom module) {

    // Checks that provided module matches with Application properties
    // modules
    Validate
        .isTrue(
            getTypeLocationService().hasModuleFeature(module, ModuleFeatureName.APPLICATION),
            "ERROR: You are trying to install Spring MVC inside module that doesn't match with APPLICATION modules features.");

    // Add Spring MVC dependency
    getProjectOperations().addDependency(module.getModuleName(),
        new Dependency("org.springframework.boot", "spring-boot-starter-web", null));

    // Create WebMvcConfiguration.java class
    JavaType webMvcConfiguration =
        new JavaType(String.format("%s.config.WebMvcConfiguration", module.getGroupId()),
            module.getModuleName());

    Validate.notNull(webMvcConfiguration.getModule(),
        "ERROR: Module name is required to generate a valid JavaType");

    // Checks if new service interface already exists.
    final String webMvcConfigurationIdentifier =
        getPathResolver().getCanonicalPath(webMvcConfiguration.getModule(), Path.SRC_MAIN_JAVA,
            webMvcConfiguration);

    if (!getFileManager().exists(webMvcConfigurationIdentifier)) {

      // Creating class builder
      final String mid =
          PhysicalTypeIdentifier.createIdentifier(webMvcConfiguration,
              getPathResolver().getPath(webMvcConfigurationIdentifier));
      final ClassOrInterfaceTypeDetailsBuilder typeBuilder =
          new ClassOrInterfaceTypeDetailsBuilder(mid, PUBLIC, webMvcConfiguration,
              PhysicalTypeCategory.CLASS);

      // Generating @RooWebMvcConfiguration annotation
      final AnnotationMetadataBuilder annotationMetadata =
          new AnnotationMetadataBuilder(ROO_WEB_MVC_CONFIGURATION);
      typeBuilder.addAnnotation(annotationMetadata.build());

      // Write new class disk
      getTypeManagementService().createOrUpdateTypeOnDisk(typeBuilder.build());

    }

    // Adding spring.jackson.serialization.indent_output property
    getApplicationConfigService().addProperty(module.getModuleName(),
        "spring.jackson.serialization.indent_output", "true", "", true);

  }

  public TypeLocationService getTypeLocationService() {
    if (typeLocationService == null) {
      // Get all Services implement TypeLocationService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TypeLocationService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          typeLocationService = (TypeLocationService) this.context.getService(ref);
          return typeLocationService;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeLocationService on ControllerOperationsImpl.");
        return null;
      }
    } else {
      return typeLocationService;
    }
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
        LOGGER.warning("Cannot load ProjectOperations on ControllerOperationsImpl.");
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
          pathResolver = (PathResolver) this.context.getService(ref);
          return pathResolver;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load PathResolver on ControllerOperationsImpl.");
        return null;
      }
    } else {
      return pathResolver;
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
        LOGGER.warning("Cannot load FileManager on ControllerOperationsImpl.");
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
        LOGGER.warning("Cannot load TypeManagementService on ControllerOperationsImpl.");
        return null;
      }
    } else {
      return typeManagementService;
    }
  }

  public ApplicationConfigService getApplicationConfigService() {
    if (applicationConfigService == null) {
      // Get all Services implement ApplicationConfigService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ApplicationConfigService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          applicationConfigService = (ApplicationConfigService) this.context.getService(ref);
          return applicationConfigService;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ApplicationConfigService on ControllerOperationsImpl.");
        return null;
      }
    } else {
      return applicationConfigService;
    }
  }


  @Override
  public String getName() {
    return FEATURE_NAME;
  }

  @Override
  public boolean isInstalledInModule(String moduleName) {
    Pom module = projectOperations.getPomFromModuleName(moduleName);
    return module.hasDependencyExcludingVersion(new Dependency("org.springframework.boot",
        "spring-boot-starter-web", null));
  }



  /*public void createAutomaticController(final JavaType controller, final JavaType entity,
      final Set<String> disallowedOperations, final String path) {
    Validate.notNull(controller, "Controller Java Type required");
    Validate.notNull(entity, "Entity Java Type required");
    Validate.notNull(disallowedOperations, "Set of disallowed operations required");
    Validate.notBlank(path, "Controller base path required");
  
    // Look for an existing controller mapped to this path
    final ClassOrInterfaceTypeDetails existingController = getExistingController(path);
  
    webMvcOperations.installConversionService(controller.getPackage());
  
    List<AnnotationMetadataBuilder> annotations = null;
  
    ClassOrInterfaceTypeDetailsBuilder cidBuilder = null;
    if (existingController == null) {
      final LogicalPath controllerPath = pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA);
      final String resourceIdentifier =
          typeLocationService.getPhysicalTypeCanonicalPath(controller, controllerPath);
      final String declaredByMetadataId =
          PhysicalTypeIdentifier.createIdentifier(controller,
              pathResolver.getPath(resourceIdentifier));
  
      // Create annotation @RequestMapping("/myobject/**")
      final List<AnnotationAttributeValue<?>> requestMappingAttributes =
          new ArrayList<AnnotationAttributeValue<?>>();
      requestMappingAttributes.add(new StringAttributeValue(VALUE, "/" + path));
      annotations = new ArrayList<AnnotationMetadataBuilder>();
      annotations.add(new AnnotationMetadataBuilder(REQUEST_MAPPING, requestMappingAttributes));
  
      // Create annotation @Controller
      final List<AnnotationAttributeValue<?>> controllerAttributes =
          new ArrayList<AnnotationAttributeValue<?>>();
      annotations.add(new AnnotationMetadataBuilder(CONTROLLER, controllerAttributes));
  
      // Create annotation @RooWebScaffold(path = "/test",
      // formBackingObject = MyObject.class)
      annotations.add(getRooWebScaffoldAnnotation(entity, disallowedOperations, path, PATH));
      cidBuilder =
          new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, controller,
              PhysicalTypeCategory.CLASS);
    } else {
      cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(existingController);
      annotations = cidBuilder.getAnnotations();
      if (MemberFindingUtils.getAnnotationOfType(existingController.getAnnotations(),
          ROO_WEB_SCAFFOLD) == null) {
        annotations.add(getRooWebScaffoldAnnotation(entity, disallowedOperations, path, PATH));
      }
    }
    cidBuilder.setAnnotations(annotations);
    typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
  }
  
  public void generateAll(final JavaPackage javaPackage) {
    for (final ClassOrInterfaceTypeDetails entityDetails : typeLocationService
        .findClassesOrInterfaceDetailsWithTag(PERSISTENT_TYPE)) {
      if (Modifier.isAbstract(entityDetails.getModifier())) {
        continue;
      }
  
      final JavaType entityType = entityDetails.getType();
      final LogicalPath entityPath =
          PhysicalTypeIdentifier.getPath(entityDetails.getDeclaredByMetadataId());
  
      // Check to see if this persistent type has a web scaffold metadata
      // listening to it
      final String downstreamWebScaffoldMetadataId =
          WebScaffoldMetadata.createIdentifier(entityType, entityPath);
      if (dependencyRegistry.getDownstream(entityDetails.getDeclaredByMetadataId()).contains(
          downstreamWebScaffoldMetadataId)) {
        // There is already a controller for this entity
        continue;
      }
  
      // To get here, there is no listening controller, so add one
      final PluralMetadata pluralMetadata =
          (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(entityType,
              entityPath));
      if (pluralMetadata != null) {
        final JavaType controller =
            new JavaType(javaPackage.getFullyQualifiedPackageName() + "."
                + entityType.getSimpleTypeName() + "Controller");
        createAutomaticController(controller, entityType, new HashSet<String>(), pluralMetadata
            .getPlural().toLowerCase());
      }
    }
  }
  
  
  
  public boolean isNewControllerAvailable() {
    return projectOperations.isFocusedProjectAvailable();
  }
  
  
  /**
   * Looks for an existing controller mapped to the given path
   * 
   * @param path (required)
   * @return <code>null</code> if there is no such controller
   */
  /*private ClassOrInterfaceTypeDetails getExistingController(final String path) {
    for (final ClassOrInterfaceTypeDetails cid : typeLocationService
        .findClassesOrInterfaceDetailsWithAnnotation(REQUEST_MAPPING)) {
      final AnnotationAttributeValue<?> attribute =
          MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(), REQUEST_MAPPING)
              .getAttribute(VALUE);
      if (attribute instanceof ArrayAttributeValue) {
        final ArrayAttributeValue<?> mappingAttribute = (ArrayAttributeValue<?>) attribute;
        if (mappingAttribute.getValue().size() > 1) {
          LOGGER.warning("Skipping controller '" + cid.getName().getFullyQualifiedTypeName()
              + "' as it contains more than one path");
          continue;
        } else if (mappingAttribute.getValue().size() == 1) {
          final StringAttributeValue attr =
              (StringAttributeValue) mappingAttribute.getValue().get(0);
          final String mapping = attr.getValue();
          if (StringUtils.isNotBlank(mapping) && mapping.equalsIgnoreCase("/" + path)) {
            return cid;
          }
        }
      } else if (attribute instanceof StringAttributeValue) {
        final StringAttributeValue mappingAttribute = (StringAttributeValue) attribute;
        if (mappingAttribute != null) {
          final String mapping = mappingAttribute.getValue();
          if (StringUtils.isNotBlank(mapping) && mapping.equalsIgnoreCase("/" + path)) {
            return cid;
          }
        }
      }
    }
    return null;
  }
  
  private AnnotationMetadataBuilder getRooWebScaffoldAnnotation(final JavaType entity,
      final Set<String> disallowedOperations, final String path, final JavaSymbolName pathName) {
    final List<AnnotationAttributeValue<?>> rooWebScaffoldAttributes =
        new ArrayList<AnnotationAttributeValue<?>>();
    rooWebScaffoldAttributes.add(new StringAttributeValue(pathName, path));
    rooWebScaffoldAttributes.add(new ClassAttributeValue(new JavaSymbolName("formBackingObject"),
        entity));
    for (final String operation : disallowedOperations) {
      rooWebScaffoldAttributes.add(new BooleanAttributeValue(new JavaSymbolName(operation), false));
    }
    return new AnnotationMetadataBuilder(ROO_WEB_SCAFFOLD, rooWebScaffoldAttributes);
  }*/
}
