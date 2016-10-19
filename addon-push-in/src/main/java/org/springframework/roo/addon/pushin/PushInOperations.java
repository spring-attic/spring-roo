package org.springframework.roo.addon.pushin;

import java.util.List;

import org.springframework.roo.classpath.details.MethodMetadata;
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
   * @param writeOnDisk boolean used to indicates if the elements to be pushed should be 
   * 					written on disk or not.
   * @param force 
   *            boolean used to know if --force parameter has been used by developer
   *            
   * @return list of all the elements to be pushed. Doesn't matter if writeOnDisk is true or false.
   */
  List<Object> pushInAll(boolean writeOnDisk, boolean force);

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
   * @param writeOnDisk boolean used to indicates if the elements to be pushed should be 
   * 					written on disk or not.
   * 
   * @return list of all the elements to be pushed. Doesn't matter if writeOnDisk is true or false.
   */
  List<Object> pushIn(JavaPackage specifiedPackage, JavaType klass, String method,
      boolean writeOnDisk);
}
