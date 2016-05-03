package org.springframework.roo.addon.web.mvc.controller.addon.responses.json;

import static java.lang.reflect.Modifier.PUBLIC;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.addon.web.mvc.controller.annotations.config.RooWebMvcJSONConfiguration;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.FileUtils;

/**
 * Implementation of ControllerMVCResponseService that provides
 * JSON Response types.
 * 
 * With this implementation, Spring Roo will be able to provide JSON response
 * types during controller generations.
 * 
 * @author Juan Carlos García
 * @author Paula Navarro
 * @since 2.0
 */
@Component
@Service
public class JSONMVCResponseService implements ControllerMVCResponseService {

  private static Logger LOGGER = HandlerUtils.getLogger(JSONMVCResponseService.class);
  private static final String RESPONSE_TYPE = "JSON";

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }


  private ProjectOperations projectOperations;
  private TypeLocationService typeLocationService;
  private TypeManagementService typeManagementService;
  private PathResolver pathResolver;
  private FileManager fileManager;

  /**
   * This operation returns the Feature name. In this case,
   * the Feature name is the same as the response type.
   * 
   * @return String with JSON as Feature name
   */
  @Override
  public String getName() {
    return getResponseType();
  }

  /**
   * This operation checks if this feature is installed in module.
   * JSON is installed in module if Spring MVC has been installed before.
   * 
   * @return true if Spring MVC has been installed, if not return false.
   */
  @Override
  public boolean isInstalledInModule(String moduleName) {

    // Check if JSON MVC and Spring MVC config exists
    return getProjectOperations().isFeatureInstalled(FeatureNames.MVC);
  }

  /**
   * This operation returns the JSON response type.
   * 
   * @return String with JSON as response type
   */
  @Override
  public String getResponseType() {
    return RESPONSE_TYPE;
  }

  /**
   * This operation returns the annotation type @RooJSON
   * 
   * @return JavaType with the JSON annotation type
   */
  @Override
  public JavaType getAnnotation() {
    // Generating @RooJSON annotation
    return RooJavaType.ROO_JSON;
  }

  /**
   * This operation annotates a controller with the JSON annotation
   * 
   * @param controller JavaType with the controller to be annotated.
   */
  @Override
  public void annotate(JavaType controller) {

    Validate.notNull(controller, "ERROR: You must provide a valid controller");

    ClassOrInterfaceTypeDetails controllerDetails =
        getTypeLocationService().getTypeDetails(controller);

    // Check if provided controller exists on current project
    Validate.notNull(controllerDetails, "ERROR: You must provide an existing controller");

    // Check if provided controller has been annotated with @RooController
    Validate.notNull(controllerDetails.getAnnotation(RooJavaType.ROO_CONTROLLER),
        "ERROR: You must provide a controller annotated with @RooController");

    // Add JSON annotation
    ClassOrInterfaceTypeDetailsBuilder typeBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(controllerDetails);
    typeBuilder.addAnnotation(new AnnotationMetadataBuilder(getAnnotation()));

    // Write changes on provided controller
    getTypeManagementService().createOrUpdateTypeOnDisk(typeBuilder.build());

  }

  /**
   * This operation adds finders to the @RooJSON annotation.
   * 
   * @param controller JavaType with the controller to be updated with
   *                   the finders.
   * @param finders List with finder names to be added.
   */
  @Override
  public void addFinders(JavaType controller, List<String> finders) {
    Validate.notNull(controller, "ERROR: You must provide a valid controller");

    ClassOrInterfaceTypeDetails controllerDetails =
        getTypeLocationService().getTypeDetails(controller);

    // Check if provided controller exists on current project
    Validate.notNull(controllerDetails, "ERROR: You must provide an existing controller");

    // Check if provided controller has been annotated with @RooJson
    AnnotationMetadata jsonAnnotation = controllerDetails.getAnnotation(RooJavaType.ROO_JSON);
    Validate
        .notNull(jsonAnnotation, "ERROR: You must provide a controller annotated with @RooJson");

    // Create list that will include finders to add
    List<AnnotationAttributeValue<?>> findersToAdd = new ArrayList<AnnotationAttributeValue<?>>();

    // Add finder already generated
    AnnotationAttributeValue<?> currentFinders = jsonAnnotation.getAttribute("finders");
    if (currentFinders != null) {
      List<?> values = (List<?>) currentFinders.getValue();
      Iterator<?> it = values.iterator();

      while (it.hasNext()) {
        StringAttributeValue finder = ((StringAttributeValue) it.next());
        finders.remove(finder.getValue());
        findersToAdd.add(finder);
      }
    }

    // Add new finders
    for (String finderMethod : finders) {
      findersToAdd.add(new StringAttributeValue(new JavaSymbolName("value"), finderMethod));
    }

    // Add finder list to currentFinders
    ArrayAttributeValue<AnnotationAttributeValue<?>> newFinders =
        new ArrayAttributeValue<AnnotationAttributeValue<?>>(new JavaSymbolName("finders"),
            findersToAdd);

    // Include finders to annotation
    AnnotationMetadataBuilder jsonAnnotationBuilder = new AnnotationMetadataBuilder(jsonAnnotation);
    jsonAnnotationBuilder.addAttribute(newFinders);

    final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(controllerDetails);

    // Update annotation
    cidBuilder.updateTypeAnnotation(jsonAnnotationBuilder);

    // Save changes on disk
    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());

  }

  /**
   * This operation will check if some controller has the @RooJSON annotation
   * 
   * @param controller JavaType with controller to check
   * @return true if provided controller has the JSON responseType.
   *        If not, return false.
   */
  @Override
  public boolean hasResponseType(JavaType controller) {
    Validate.notNull(controller, "ERROR: You must provide a valid controller");

    ClassOrInterfaceTypeDetails controllerDetails =
        getTypeLocationService().getTypeDetails(controller);

    if (controllerDetails == null) {
      return false;
    }

    return controllerDetails.getAnnotation(getAnnotation()) != null;
  }

  /**
   * This operation will install all the necessary items to be able to use
   * JSON response type.
   * 
   * @param module Pom with the module where this response type should
   *        be installed.
   */
  @Override
  public void install(Pom module) {
    // Managed by ControllerOperationsImpl
  }

  @Override
  public JavaType getMainControllerAnnotation() {
    // JSON Response Service doesn't provide main controller
    // annotation
    return null;
  }

  @Override
  public JavaType getMainController() {
    // JSON Response Service doesn't provide main controller
    return null;
  }

  // Getting OSGi services

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
        LOGGER.warning("Cannot load ProjectOperations on JSONMVCResponseService.");
        return null;
      }
    } else {
      return projectOperations;
    }
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
        LOGGER.warning("Cannot load TypeLocationService on JSONMVCResponseService.");
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
          typeManagementService = (TypeManagementService) this.context.getService(ref);
          return typeManagementService;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeManagementService on JSONMVCResponseService.");
        return null;
      }
    } else {
      return typeManagementService;
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
}
