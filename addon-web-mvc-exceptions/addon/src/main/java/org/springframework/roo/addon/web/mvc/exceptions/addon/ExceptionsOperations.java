package org.springframework.roo.addon.web.mvc.exceptions.addon;

import org.springframework.roo.model.JavaType;

/**
 * Interface of operations to do when executing ExceptionsCommands commands from the
 * Roo shell.
 *
 * @author Fran Cardoso
 * @since 2.0
 */
public interface ExceptionsOperations {

  /**
   * Adds the annotation {@link RooExceptionHandlers} and exception handler methods
   * on a controller or a class annotated with {@link ControllerAdvice}.
   *
   * @param exception Class that extends {@link Exception} or {@link RuntimeException}.
   * @param controller Existing controller to add exception handler methods.
   * @param adviceClass New or existing class annotated with {@link ControllerAdvice}
   *  to add exception handler methods.
   * @param errorView View to be returned when exception is thrown
   */
  void addExceptionHandler(JavaType exception, JavaType controller, JavaType adviceClass,
      String errorView);

}
