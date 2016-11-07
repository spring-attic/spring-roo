package org.springframework.roo.addon.web.mvc.exceptions.addon;

import org.springframework.roo.addon.web.mvc.exceptions.annotations.RooExceptionHandler;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;

/**
 * Registers the attributes values of a {@link RooExceptionHandler} annotation
 *
 * @author Fran Cardoso
 * @since 2.0
 */
public class ExceptionHandlerAnnotationValues {

  /**
   * Exception to handle.
   */
  private final ClassOrInterfaceTypeDetails exception;

  /**
   * HTTP code to return when defined exception is thrown.
   */
  private final String errorView;

  /**
   * Default constructor
   *
   * @param exception
   * @param errorView
   */
  public ExceptionHandlerAnnotationValues(ClassOrInterfaceTypeDetails exception, String errorView) {
    this.exception = exception;
    this.errorView = errorView;
  }

  public ClassOrInterfaceTypeDetails getException() {
    return exception;
  }

  public String getErrorView() {
    return errorView;
  }
}
