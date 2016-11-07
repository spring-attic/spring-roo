package org.springframework.roo.addon.web.mvc.controller.addon.finder;

import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaMetadata;
import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

import java.util.List;

/**
 * Provides operations for Web MVC finder functionality.
 *
 * @author Stefan Schmidt
 * @author Paula Navarro
 * @since 1.2.0
 */
public interface WebFinderOperations {

  boolean isWebFinderInstallationPossible();

  /**
   * Creates or updates the controller associated to specified entity with the specified
   * query methods.
   *
   * @param entity the JavaType representing the entity which associated repository
   *            has the finders to publish in web layer.
   * @param queryMethods the List<String> of finder names to publish in web layer.
   * @param responseType the ControllerMVCResponseService to be used by generated
   *            controller.
   * @param controllerPackage the JavaPackage where controller should be generated.
   * @param pathPrefix the String with the default controller path for finder requests.
   */
  void createOrUpdateSearchControllerForEntity(JavaType entity, List<String> queryMethods,
      ControllerMVCResponseService responseType, JavaPackage controllerPackage, String pathPrefix);

  /**
   * Creates or updates all controllers with it's associated entities query methods.
   *
   * @param responseType the ControllerMVCResponseService to be used by generated
   *            controllers.
   * @param controllerPackage the JavaPackage where controllers should be generated.
   * @param pathPrefix the String with the default controller paths for finder requests.
   */
  void createOrUpdateSearchControllerForAllEntities(ControllerMVCResponseService responseType,
      JavaPackage controllerPackage, String pathPrefix);

  /**
   * Returns finders names which can be publish
   *
   * @param entity
   * @param responseType (optional, can be null)
   * @return
   */
  List<String> getFindersWhichCanBePublish(JavaType entity,
      ControllerMVCResponseService responseType);

  /**
   * Returns finders names which can be publish
   *
   * @param repositoryMetadata
   * @param responseType
   * @return
   */
  List<String> getFindersWhichCanBePublish(RepositoryJpaMetadata repositoryMetadata,
      ControllerMVCResponseService responseType);

}
