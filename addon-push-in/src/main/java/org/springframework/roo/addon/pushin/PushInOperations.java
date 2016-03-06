package org.springframework.roo.addon.pushin;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Interface to {@link PushInOperationsImpl}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface PushInOperations {

  /**
   * Method that checks if push-in operation is available or not.
   * 
   * "push-in" command will be available only if some project was generated.
   * 
   * @return true if some project was created on focused directory.
   */
  boolean isPushInCommandAvailable();

  /**
   * Method that register push-in command on Spring Roo Shell.
   * 
   * Push-in all methods, fields and annotations declared on project ITDs to its .java
   * files.
   * 
   * @param package 
   *            JavaPackage with the specified package where push-in will be applied.
   * @param klass 
   *            JavaType with the specified class where push-in will be applied.
   */
  void pushInAll(JavaPackage packageName, JavaType klass);

  /**
   * Method that register "push-in method" command on Spring Roo Shell.
   * 
   * Push-in an specific method declared on an specified class ITDs to its .java files
   * 
   * @param klass
   *            JavaType with the specified class where developer wants to
   *            make push-in
   * @param method
   *            JavaSymbolName with the specified name of the method that
   *            developer wants to push-in
   * 
   */
  void pushInMethod(JavaType klass, JavaSymbolName method);
}
