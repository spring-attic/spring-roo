package org.springframework.roo.addon.pushin;

import org.springframework.roo.model.JavaPackage;
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
   * Method that push-in all methods, fields, annotations, imports, extends, etc.. declared 
   * on project ITDs to its .java files.
   * 
   * @param force 
   *            boolean used to know if --force parameter has been used by developer
   */
  void pushInAll(boolean force);

  /**
   * Method that push-in all methods, fields, annotations, imports, extends, etc.. declared on 
   * ITDs to its .java files. You should define package, class or method where wants 
   * to apply push-in operation.
   * 
   * @param package 
   *            JavaPackage with the specified package where developers wants to make 
   *            push-in
   * @param klass
   *            JavaType with the specified class where developer wants to
   *            make push-in
   * @param method
   *            String with the specified name of the method that
   *            developer wants to push-in
   */
  void pushIn(JavaPackage specifiedPackage, JavaType klass, String method);
}
