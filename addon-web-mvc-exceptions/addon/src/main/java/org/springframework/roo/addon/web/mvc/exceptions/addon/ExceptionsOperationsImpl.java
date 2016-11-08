package org.springframework.roo.addon.web.mvc.exceptions.addon;

import static java.lang.reflect.Modifier.PUBLIC;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.annotations.RooController;
import org.springframework.roo.addon.web.mvc.exceptions.annotations.RooExceptionHandler;
import org.springframework.roo.addon.web.mvc.exceptions.annotations.RooExceptionHandlers;
import org.springframework.roo.addon.web.mvc.i18n.components.I18n;
import org.springframework.roo.addon.web.mvc.i18n.components.I18nSupport;
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
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.NestedAnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.propfiles.manager.PropFilesManagerService;
import org.springframework.roo.support.ant.AntPathMatcher;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Implementation of {@link ExceptionsOperations} interface.
 *
 * @author Fran Cardoso
 * @since 2.0
 */
@Component
@Service
public class ExceptionsOperationsImpl implements ExceptionsOperations {

  private static final Logger LOGGER = HandlerUtils.getLogger(ExceptionsOperationsImpl.class);

  private static final String ERROR_VIEW = "errorView";
  private static final String EXCEPTION = "exception";
  private static final String LABEL_PREFIX = "label_";
  private static final String LABEL_MESSAGE = "TODO Auto-generated %s message";
  private static final String VALUE = "value";

  @Reference
  TypeLocationService typeLocationService;

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    serviceInstaceManager.activate(this.context);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addExceptionHandler(JavaType exception, JavaType controller, JavaType adviceClass,
      String errorView) {

    // Check if exception class exists
    if (typeLocationService.getTypeDetails(exception) == null) {
      LOGGER.warning(String.format("Can't found class: %s", exception.getFullyQualifiedTypeName()));
      return;
    }

    // Check that both parameters 'controller' and 'class' are not defined
    if (controller != null && adviceClass != null) {
      LOGGER.warning("Only one of \"controller\" or \"class\" parameters must be defined");
      return;
    }

    // Error view or exception response status is required
    if (errorView == null) {
      if (!isExceptionAnnotatedWithResponseStatus(exception)) {
        LOGGER.warning("Exception must be annotated with @ResponseStatus or an error view "
            + "must be provided");
        return;
      }
    }

    if (controller == null && adviceClass != null) {
      // Create or update advice class if its correctly annotated
      if (createControllerAdviceIfRequired(adviceClass)) {
        addHandlersAnnotations(exception, adviceClass, errorView);
        addExceptionLabel(exception, adviceClass.getModule());
      }
    } else if (controller != null) {
      if (typeLocationService.getTypeDetails(controller) == null) {
        LOGGER.warning(String.format("Can't found class: %s",
            controller.getFullyQualifiedTypeName()));
        return;
      }
      // Update controller class if its correctly annotated
      if (isRooController(controller)) {
        addHandlersAnnotations(exception, controller, errorView);
        addExceptionLabel(exception, controller.getModule());
      } else {
        LOGGER.warning("Controller class must be annotated with @RooController");
        return;
      }
    } else {
      LOGGER.warning("Target class is required");
      return;
    }
  }

  /**
   * Writes a label with a default exception message on messages.properties files
   *
   * @param exception
   * @param moduleName
   */
  private void addExceptionLabel(JavaType exception, String moduleName) {
    if (getProjectOperations().isMultimoduleProject()) {
      Validate.notBlank(moduleName, "Module name is required");
    }

    final LogicalPath resourcesPath = LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, moduleName);
    final String targetDirectory = getPathResolver().getIdentifier(resourcesPath, "");

    final String exceptionName = exception.getSimpleTypeName();
    final String labelKey = LABEL_PREFIX.concat(exceptionName.toLowerCase());

    Set<I18n> supportedLanguages = getI18nSupport().getSupportedLanguages();
    for (I18n i18n : supportedLanguages) {
      String messageBundle =
          String.format("messages_%s.properties", i18n.getLocale().getLanguage());
      String bundlePath =
          String.format("%s%s%s", targetDirectory, AntPathMatcher.DEFAULT_PATH_SEPARATOR,
              messageBundle);

      if (getFileManager().exists(bundlePath)) {
        getPropFilesManager().addPropertyIfNotExists(resourcesPath, messageBundle, labelKey,
            String.format(LABEL_MESSAGE, exceptionName), true);
      }
    }
    // Always update english message bundles
    getPropFilesManager().addPropertyIfNotExists(resourcesPath, "messages.properties", labelKey,
        String.format(LABEL_MESSAGE, exceptionName), true);
  }

  /**
   * Creates a new class annotated with @ControllerAdvice if not exists. Returns true if success
   * or class already exists.
   *
   * Returns false if class already exists but it's not annotated with @ControllerAdvice.
   *
   * @param controllerAdviceClass
   */
  private boolean createControllerAdviceIfRequired(JavaType controllerAdviceClass) {

    // Checks if new service interface already exists.
    final String controllerAdviceClassIdentifier =
        getPathResolver().getCanonicalPath(controllerAdviceClass.getModule(), Path.SRC_MAIN_JAVA,
            controllerAdviceClass);

    if (!getFileManager().exists(controllerAdviceClassIdentifier)) {

      // Creating class builder
      final String mid =
          PhysicalTypeIdentifier.createIdentifier(controllerAdviceClass,
              getPathResolver().getPath(controllerAdviceClassIdentifier));
      final ClassOrInterfaceTypeDetailsBuilder typeBuilder =
          new ClassOrInterfaceTypeDetailsBuilder(mid, PUBLIC, controllerAdviceClass,
              PhysicalTypeCategory.CLASS);
      typeBuilder.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.CONTROLLER_ADVICE)
          .build());

      // Write new class disk
      getTypeManagementService().createOrUpdateTypeOnDisk(typeBuilder.build());
    } else {
      // Check if class is annotated with @ControllerAdvice
      ClassOrInterfaceTypeDetails typeDetails =
          typeLocationService.getTypeDetails(controllerAdviceClass);
      AnnotationMetadata annotation = typeDetails.getAnnotation(SpringJavaType.CONTROLLER_ADVICE);
      if (annotation == null) {
        LOGGER.warning("Class must be annotated with @ControllerAdvice");
        return false;
      }
    }
    return true;
  }

  /**
   * Generates {@link RooExceptionHandlers} and {@link RooExceptionHandler} annotations
   * and adds or updates it on specified class.
   *
   * @param exception
   * @param targetClass
   * @param errorView
   */
  private void addHandlersAnnotations(JavaType exception, JavaType targetClass, String errorView) {
    Validate.notNull(targetClass,
        "Target class is required to add @RooExceptionHandlers annotation");

    // Create @RooExceptionHandler Annotation
    final AnnotationMetadataBuilder exceptionHandlerAnnotationBuilder =
        new AnnotationMetadataBuilder(RooJavaType.ROO_EXCEPTION_HANDLER);

    final List<AnnotationAttributeValue<?>> exceptionHandlerAnnotationAttributes =
        new ArrayList<AnnotationAttributeValue<?>>();
    exceptionHandlerAnnotationAttributes.add(new ClassAttributeValue(new JavaSymbolName(EXCEPTION),
        exception));
    if (errorView != null) {
      exceptionHandlerAnnotationAttributes.add(new StringAttributeValue(new JavaSymbolName(
          ERROR_VIEW), errorView));
    }
    exceptionHandlerAnnotationBuilder.setAttributes(exceptionHandlerAnnotationAttributes);

    // Check if container annotation already exists
    ClassOrInterfaceTypeDetails typeDetails = typeLocationService.getTypeDetails(targetClass);
    ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(typeDetails);
    AnnotationMetadata exceptionHandlersAnnotation =
        typeDetails.getAnnotation(RooJavaType.ROO_EXCEPTION_HANDLERS);

    AnnotationMetadataBuilder exceptionHandlersAnnotationBuilder = null;

    if (exceptionHandlersAnnotation != null) {
      exceptionHandlersAnnotationBuilder =
          new AnnotationMetadataBuilder(exceptionHandlersAnnotation);
    } else {
      exceptionHandlersAnnotationBuilder =
          new AnnotationMetadataBuilder(RooJavaType.ROO_EXCEPTION_HANDLERS);
    }

    Validate.notNull(exceptionHandlersAnnotationBuilder);

    // Add @RooExceptionHandler annotation into @RooExceptionHandlers
    final List<NestedAnnotationAttributeValue> exceptionHandlersArrayValues =
        new ArrayList<NestedAnnotationAttributeValue>();
    exceptionHandlersArrayValues.add(new NestedAnnotationAttributeValue(new JavaSymbolName(VALUE),
        exceptionHandlerAnnotationBuilder.build()));

    final List<AnnotationAttributeValue<?>> attributeValues =
        new ArrayList<AnnotationAttributeValue<?>>();
    attributeValues.add(new ArrayAttributeValue<NestedAnnotationAttributeValue>(new JavaSymbolName(
        VALUE), exceptionHandlersArrayValues));

    if (exceptionHandlersAnnotation == null) {
      // Add new @RooExceptionHandlers annotation with given values
      exceptionHandlersAnnotationBuilder.setAttributes(attributeValues);
      typeDetailsBuilder.addAnnotation(exceptionHandlersAnnotationBuilder.build());
    } else {
      // Get current annotation values from @RooExceptionHandlers annotation
      AnnotationAttributeValue<?> currentHandlers = exceptionHandlersAnnotation.getAttribute(VALUE);
      if (currentHandlers != null) {
        List<?> values = (List<?>) currentHandlers.getValue();
        Iterator<?> it = values.iterator();
        while (it.hasNext()) {
          NestedAnnotationAttributeValue handler = (NestedAnnotationAttributeValue) it.next();
          if (handler.getValue() != null) {
            // Check if there is a @RooExceptionHandlers with same 'exception' value
            if (exceptionHandlerAnnotationBuilder.build().getAttribute(EXCEPTION).getValue()
                .equals(handler.getValue().getAttribute(EXCEPTION).getValue())) {
              LOGGER.warning(String.format(
                  "There is already a handler for exception %s in class %s",
                  exception.getSimpleTypeName(), targetClass.getSimpleTypeName()));
              return;
            }
            exceptionHandlersArrayValues.add(handler);
          }
        }
      }

      // Add found values
      attributeValues.add(new ArrayAttributeValue<NestedAnnotationAttributeValue>(
          new JavaSymbolName(VALUE), exceptionHandlersArrayValues));

      exceptionHandlersAnnotationBuilder.setAttributes(attributeValues);

      // Update annotation
      typeDetailsBuilder.updateTypeAnnotation(exceptionHandlersAnnotationBuilder.build());
    }

    // Write to disk
    getTypeManagementService().createOrUpdateTypeOnDisk(typeDetailsBuilder.build());
  }

  /**
   * Returns true if a class is annotated with {@link RooController}
   *
   * @param klass
   * @return
   */
  private boolean isRooController(JavaType klass) {
    ClassOrInterfaceTypeDetails typeDetails = typeLocationService.getTypeDetails(klass);
    Validate.notNull(typeDetails,
        String.format("Can't found class: %s", klass.getFullyQualifiedTypeName()));
    AnnotationMetadata annotation = typeDetails.getAnnotation(RooJavaType.ROO_CONTROLLER);
    if (annotation == null) {
      return false;
    }
    return true;
  }

  private boolean isExceptionAnnotatedWithResponseStatus(JavaType exception) {
    ClassOrInterfaceTypeDetails exceptionCid = typeLocationService.getTypeDetails(exception);
    Validate.notNull(exceptionCid,
        String.format("Can't found class: %s", exception.getFullyQualifiedTypeName()));
    AnnotationMetadata annotation = exceptionCid.getAnnotation(SpringJavaType.RESPONSE_STATUS);
    if (annotation == null) {
      return false;
    }
    return true;
  }

  // Get OSGi services

  private ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);
  }

  private PropFilesManagerService getPropFilesManager() {
    return serviceInstaceManager.getServiceInstance(this, PropFilesManagerService.class);
  }

  private I18nSupport getI18nSupport() {
    return serviceInstaceManager.getServiceInstance(this, I18nSupport.class);
  }

  private FileManager getFileManager() {
    return serviceInstaceManager.getServiceInstance(this, FileManager.class);
  }

  private PathResolver getPathResolver() {
    return serviceInstaceManager.getServiceInstance(this, PathResolver.class);
  }

  private TypeManagementService getTypeManagementService() {
    return serviceInstaceManager.getServiceInstance(this, TypeManagementService.class);
  }
}
