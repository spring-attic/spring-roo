package org.springframework.roo.addon.web.mvc.controller.addon.responses.json;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

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

  private static final String RESPONSE_TYPE = "JSON";
  private static final String CONTROLLER_NAME_MODIFIER = "Json";

  // ------------ OSGi component attributes ----------------
  private BundleContext context;
  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    serviceInstaceManager.activate(this.context);
  }

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

  private ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);
  }

  private TypeLocationService getTypeLocationService() {
    return serviceInstaceManager.getServiceInstance(this, TypeLocationService.class);
  }

  private TypeManagementService getTypeManagementService() {
    return serviceInstaceManager.getServiceInstance(this, TypeManagementService.class);
  }

  @Override
  public String getControllerNameModifier() {
    return CONTROLLER_NAME_MODIFIER;
  }

  @Override
  public boolean requiresJsonDeserializer() {
    return true;
  }

  @Override
  public boolean requiresJsonMixin() {
    return true;
  }

  @Override
  public boolean providesViews() {
    // JSON doesn't provide views
    return false;
  }
}
