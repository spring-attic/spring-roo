package org.springframework.roo.addon.web.mvc.controller.addon.finder;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaLocator;
import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaMetadata;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.parser.FinderMethod;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.parser.PartTree;
import org.springframework.roo.addon.layers.service.addon.ServiceLocator;
import org.springframework.roo.addon.plural.addon.PluralService;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerAnnotationValues;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerLocator;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerOperations;
import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.classpath.ModuleFeatureName;
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
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of {@link WebFinderOperations}
 *
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @author Paula Navarro
 * @author Sergio Clares
 * @since 1.2.0
 */
@Component
@Service
public class WebFinderOperationsImpl implements WebFinderOperations {

  private static Logger LOGGER = HandlerUtils.getLogger(WebFinderOperationsImpl.class);

  @Reference
  private ControllerOperations controllerOperations;
  @Reference
  private MetadataService metadataService;
  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private TypeManagementService typeManagementService;
  @Reference
  private ProjectOperations projectOperations;
  @Reference
  private RepositoryJpaLocator repositoryJpaLocator;
  @Reference
  private PluralService pluralService;
  @Reference
  private ServiceLocator serviceLocator;
  @Reference
  private ControllerLocator controllerLocator;

  public boolean isWebFinderInstallationPossible() {
    return projectOperations.isFeatureInstalled(FeatureNames.MVC);
  }

  @Override
  public void createOrUpdateSearchControllerForEntity(JavaType entity, List<String> queryMethods,
      ControllerMVCResponseService responseType, JavaPackage controllerPackage, String pathPrefix) {
    Validate.notNull(entity, "Entity type required");
    Validate.notNull(responseType, "Response type required");
    Validate.notNull(controllerPackage, "Package required");
    Validate
        .notNull(
            typeLocationService.getTypeDetails(entity).getAnnotation(RooJavaType.ROO_JPA_ENTITY),
            "The provided JavaType %s must be annotated with @RooJpaEntity",
            entity.getSimpleTypeName());

    // Check if module has an application class
    controllerPackage = checkAndUseApplicationModule(controllerPackage);

    // Check if entity has any associated repository
    final ClassOrInterfaceTypeDetails relatedRepositoryDetails =
        repositoryJpaLocator.getRepository(entity);
    Validate
        .notNull(
            relatedRepositoryDetails,
            "Entity %s doesn't have an associated repository. You "
                + "need at least an associated repository with finders to publish them to the web layer."
                + "Please, create one associated repository with 'repository jpa' command.", entity);

    // Get repository finder methods
    RepositoryJpaMetadata repositoryMetadata = repositoryJpaLocator.getRepositoryMetadata(entity);

    // Get finders in @RooRepositoryJpa
    // List<String> entityFinders = repositoryMetadata.getDeclaredFinderNames();
    List<String> entityFinders = getFindersWhichCanBePublish(repositoryMetadata, responseType);

    // Check if specified finder methods exists in associated repository
    for (String finder : queryMethods) {
      if (!entityFinders.contains(finder)) {
        LOGGER.log(Level.INFO, String.format(
            "ERROR: Provided finder '%s' doesn't exists on the repository '%s' "
                + "related to entity '%s'", finder, relatedRepositoryDetails.getType()
                .getSimpleTypeName(), entity.getSimpleTypeName()));
        return;
      }
    }

    // Check if entity has any associated service
    JavaType relatedService = null;
    ClassOrInterfaceTypeDetails service = serviceLocator.getService(entity);
    if (service == null) {
      LOGGER.log(Level.INFO, String.format("Entity %s doesn't have associated services, "
          + "necessary to create controllers. Please, create one associated service with "
          + "'service' command before publish finders to web layer.", entity.getSimpleTypeName()));
      return;
    }
    relatedService = service.getType();

    // Seek for search type controllers related to entity
    Collection<ClassOrInterfaceTypeDetails> entitySearchControllers =
        controllerLocator.getControllers(entity, ControllerType.SEARCH,
            responseType.getAnnotation());

    // Check if any of the search controllers have the same pathPrefix.
    // If so, and controllerPackage is as well the same, update the controller.
    ClassOrInterfaceTypeDetails controllerToUpdateOrCreate = null;
    for (ClassOrInterfaceTypeDetails entitySearchController : entitySearchControllers) {
      ControllerAnnotationValues controllerValues =
          new ControllerAnnotationValues(entitySearchController);
      if (StringUtils.equals(pathPrefix, controllerValues.getPathPrefix())) {
        if (controllerPackage.equals(entitySearchController.getType().getPackage())) {

          // The controller exists, so choose it for updating.
          controllerToUpdateOrCreate = entitySearchController;
          break;
        } else {

          // A related controller already exists for the same entity, with the same 'pathPrefix',
          // but in a different package.
          LOGGER.log(Level.INFO, String.format("ERROR: Already exists a controller associated "
              + "to entity '%s' with the pathPrefix '%s', in a different package. Specify "
              + "a different pathPrefix to create a new one, or the same 'package' and "
              + "'pathPrefix' to update the existing controller.", entity.getSimpleTypeName(),
              pathPrefix));
          return;
        }
      } else if (entitySearchController.getType().getPackage().equals(controllerPackage)) {

        // A related controller already exists for the same entity, in the same package
        LOGGER
            .log(
                Level.INFO,
                String
                    .format(
                        "ERROR: Already exists a controller associated to entity '%s' in the "
                            + "same package '%s', with different 'pathPrefix'. Specify a different 'pathPrefix' "
                            + "and a different package that the existing one to create a new one, or the same "
                            + "'package' and 'pathPrefix' to update the existing controller.",
                        entity.getSimpleTypeName(),
                        controllerPackage.getFullyQualifiedPackageName()));
        return;
      }
    }

    // Update or create the search controller
    ClassOrInterfaceTypeDetailsBuilder controllerBuilder = null;
    if (controllerToUpdateOrCreate == null) {

      // Create controller builder for a new file
      controllerBuilder =
          buildNewSearchController(entity, queryMethods, responseType, controllerPackage,
              pathPrefix);

    } else {

      // Controller already exists, so create builder with it
      controllerBuilder = new ClassOrInterfaceTypeDetailsBuilder(controllerToUpdateOrCreate);

      // Update existing controller
      boolean findersAdded =
          updateExistingSearchController(queryMethods, controllerToUpdateOrCreate,
              controllerBuilder);

      // Check if response type is already added
      AnnotationMetadata responseTypeAnnotation =
          controllerToUpdateOrCreate.getAnnotation(responseType.getAnnotation());
      if (responseTypeAnnotation != null) {
        if (!findersAdded) {

          // Controller already had same response type annotation and same finders added
          LOGGER.log(Level.WARNING, String.format(
              "Controller %s already has specified finders and specified response type.",
              controllerToUpdateOrCreate.getType().getFullyQualifiedTypeName()));
          return;
        }
      } else {

        // Add annotation for the new response type
        controllerBuilder
            .addAnnotation(new AnnotationMetadataBuilder(responseType.getAnnotation()));
      }
    }

    // Add dependencies between modules if required
    addModuleDependencies(entity, relatedRepositoryDetails, relatedService,
        controllerBuilder.build());

    // Write changes to disk
    typeManagementService.createOrUpdateTypeOnDisk(controllerBuilder.build());

    // Create LinkFactory class for the search controler
    controllerOperations.createLinkFactoryClass(controllerBuilder.getName());
  }

  @Override
  public List<String> getFindersWhichCanBePublish(RepositoryJpaMetadata repositoryMetadata,
      ControllerMVCResponseService responseType) {
    // Just support finders in custom
    List<String> finders = new ArrayList<String>();
    for (Pair<FinderMethod, PartTree> item : repositoryMetadata.getFindersToAddInCustom()) {
      finders.add(item.getKey().getMethodName().getSymbolName());
    }
    Collections.sort(finders);
    return finders;
  }

  @Override
  public List<String> getFindersWhichCanBePublish(JavaType entity,
      ControllerMVCResponseService responseType) {
    RepositoryJpaMetadata repositoryMetadata = repositoryJpaLocator.getRepositoryMetadata(entity);
    // Just support finders in custom
    return getFindersWhichCanBePublish(repositoryMetadata, responseType);
  }

  @Override
  public void createOrUpdateSearchControllerForAllEntities(
      ControllerMVCResponseService responseType, JavaPackage controllerPackage, String pathPrefix) {
    Validate.notNull(responseType, "responseType required");
    Validate.notNull(controllerPackage, "package required");
    Validate.notNull(pathPrefix, "pathPrefix required");

    // Check if module has an application class
    controllerPackage = checkAndUseApplicationModule(controllerPackage);

    // Search all entities with associated repository
    for (ClassOrInterfaceTypeDetails entityDetails : typeLocationService
        .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_ENTITY)) {
      JavaType entity = entityDetails.getType();

      // Ignore abstract classes
      if (entityDetails.isAbstract()) {
        continue;
      }

      // Seek the repositories of each entity
      RepositoryJpaMetadata repositoryMetadata = repositoryJpaLocator.getRepositoryMetadata(entity);
      if (repositoryMetadata == null) {
        LOGGER
            .log(
                Level.INFO,
                String
                    .format(
                        "Entity %s hasn't any repository associated. Web "
                            + "finder generation won't have effects. Use 'repository jpa' command to create repositories.",
                        entity.getSimpleTypeName()));
      }

      List<String> entityFinders = getFindersWhichCanBePublish(repositoryMetadata, responseType);
      if (!entityFinders.isEmpty()) {
        this.createOrUpdateSearchControllerForEntity(entity, entityFinders, responseType,
            controllerPackage, pathPrefix);
      }
    }
  }

  /**
   * Add dependencies between modules if needed
   *
   * @param entity
   * @param relatedRepository
   * @param relatedService
   * @param controllerToUpdateOrCreate
   */
  private void addModuleDependencies(JavaType entity,
      ClassOrInterfaceTypeDetails relatedRepository, JavaType relatedService,
      ClassOrInterfaceTypeDetails controllerToUpdateOrCreate) {
    if (projectOperations.isMultimoduleProject()) {

      // Add service module dependency
      projectOperations.addModuleDependency(controllerToUpdateOrCreate.getType().getModule(),
          relatedService.getModule());

      // Add repository module dependency
      projectOperations.addModuleDependency(controllerToUpdateOrCreate.getType().getModule(),
          relatedRepository.getType().getModule());

      // Add model module dependency
      projectOperations.addModuleDependency(controllerToUpdateOrCreate.getType().getModule(),
          entity.getModule());
    }
  }

  /**
   * Build a new search controller for provided entity, with provided response type,
   * package, path prefix and query methods.
   *
   * @param entity
   * @param queryMethods
   * @param responseType
   * @param controllerPackage
   * @param pathPrefix
   * @return
   */
  private ClassOrInterfaceTypeDetailsBuilder buildNewSearchController(JavaType entity,
      List<String> queryMethods, ControllerMVCResponseService responseType,
      JavaPackage controllerPackage, String pathPrefix) {
    ClassOrInterfaceTypeDetailsBuilder controllerBuilder;
    JavaType searchController =
        new JavaType(String.format("%s.%sSearch%sController",
            controllerPackage.getFullyQualifiedPackageName(), pluralService.getPlural(entity),
            responseType.getControllerNameModifier()), controllerPackage.getModule());
    final String physicalPath =
        PhysicalTypeIdentifier.createIdentifier(searchController,
            LogicalPath.getInstance(Path.SRC_MAIN_JAVA, controllerPackage.getModule()));
    controllerBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(physicalPath, Modifier.PUBLIC, searchController,
            PhysicalTypeCategory.CLASS);

    // Create @RooController
    AnnotationMetadataBuilder controllerAnnotation =
        new AnnotationMetadataBuilder(RooJavaType.ROO_CONTROLLER);
    controllerAnnotation.addClassAttribute("entity", entity);
    if (StringUtils.isNotBlank(pathPrefix)) {
      controllerAnnotation.addStringAttribute("pathPrefix", pathPrefix);
    }
    controllerAnnotation.addEnumAttribute("type", new EnumDetails(
        RooJavaType.ROO_ENUM_CONTROLLER_TYPE, new JavaSymbolName("SEARCH")));
    controllerBuilder.addAnnotation(controllerAnnotation.build());

    // Create @RooSearch
    AnnotationMetadataBuilder searchAnnotation =
        new AnnotationMetadataBuilder(RooJavaType.ROO_SEARCH);
    List<AnnotationAttributeValue<?>> findersToAdd = new ArrayList<AnnotationAttributeValue<?>>();
    for (String finder : queryMethods) {
      findersToAdd.add(new StringAttributeValue(new JavaSymbolName("value"), finder));
    }
    searchAnnotation.addAttribute(new ArrayAttributeValue<AnnotationAttributeValue<?>>(
        new JavaSymbolName("finders"), findersToAdd));
    controllerBuilder.addAnnotation(searchAnnotation);

    // Add response type annotation
    AnnotationMetadataBuilder responseTypeAnnotation =
        new AnnotationMetadataBuilder(responseType.getAnnotation());
    controllerBuilder.addAnnotation(responseTypeAnnotation);
    return controllerBuilder;
  }

  /**
   * Checks if module provided in package is an application module. If not, find an
   * application module and use it with default package.
   *
   * @param controllerPackage the provided JavaPackage to check
   * @return
   */
  private JavaPackage checkAndUseApplicationModule(JavaPackage controllerPackage) {

    if (!typeLocationService.hasModuleFeature(
        projectOperations.getPomFromModuleName(controllerPackage.getModule()),
        ModuleFeatureName.APPLICATION)) {

      LOGGER
          .log(
              Level.WARNING,
              "Focused or specified module isn't an application module (containing a class "
                  + "annotated with @SpringBootApplication). Looking for an application module and default package...");

      // Validate that project has at least one application module
      Validate.notEmpty(typeLocationService.getModuleNames(ModuleFeatureName.APPLICATION),
          "The project must have at least one application module to publish web finders.");

      // Get the first application module
      String moduleName =
          typeLocationService.getModuleNames(ModuleFeatureName.APPLICATION).iterator().next();
      Pom module = projectOperations.getPomFromModuleName(moduleName);
      controllerPackage =
          new JavaPackage(typeLocationService.getTopLevelPackageForModule(module).concat(".web"),
              moduleName);
    }
    return controllerPackage;
  }

  /**
   * Update existing controller with new query methods
   * @param queryMethods the finder names to add
   * @param controllerToUpdateOrCreate the existing controller
   * @param controllerBuilder the builder to apply the changes
   * @return
   */
  private boolean updateExistingSearchController(List<String> queryMethods,
      ClassOrInterfaceTypeDetails controllerToUpdateOrCreate,
      ClassOrInterfaceTypeDetailsBuilder controllerBuilder) {
    // Get @RooSearch and build necessary variables
    AnnotationMetadata searchAnnotation =
        controllerToUpdateOrCreate.getAnnotation(RooJavaType.ROO_SEARCH);
    List<AnnotationAttributeValue<?>> findersToAdd = new ArrayList<AnnotationAttributeValue<?>>();
    List<String> finderNames = new ArrayList<String>();
    boolean findersAdded = false;

    // Get existent finder values
    if (searchAnnotation != null && searchAnnotation.getAttribute("finders") != null) {
      List<?> existingFinders = (List<?>) searchAnnotation.getAttribute("finders").getValue();
      Iterator<?> it = existingFinders.iterator();

      // Add existent finders to new attributes array to merge with new ones
      while (it.hasNext()) {
        StringAttributeValue attributeValue = (StringAttributeValue) it.next();
        findersToAdd.add(attributeValue);
        finderNames.add(attributeValue.getValue());
      }

      // Add new finders to new attributes array
      for (String finder : queryMethods) {
        if (!finderNames.contains(finder)) {
          findersToAdd.add(new StringAttributeValue(new JavaSymbolName("value"), finder));
          findersAdded = true;
        }
      }

      // Add attributes array to @RooSearch
      AnnotationMetadataBuilder searchAnnotationBuilder =
          new AnnotationMetadataBuilder(searchAnnotation);
      ArrayAttributeValue<AnnotationAttributeValue<?>> allFinders =
          new ArrayAttributeValue<AnnotationAttributeValue<?>>(new JavaSymbolName("finders"),
              findersToAdd);
      searchAnnotationBuilder.addAttribute(allFinders);
      controllerBuilder.updateTypeAnnotation(searchAnnotationBuilder);
    }
    return findersAdded;
  }
}
