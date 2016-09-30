package org.springframework.roo.addon.web.mvc.controller.addon.finder;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jvnet.inflector.Noun;
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
import org.springframework.roo.classpath.details.annotations.NestedAnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooEnumDetails;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of {@link WebFinderOperations}
 * 
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @author Paula Navarro
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
    Pom pom = projectOperations.getPomFromModuleName(controllerPackage.getModule());
    Validate.isTrue(typeLocationService.hasModuleFeature(pom, ModuleFeatureName.APPLICATION),
        "Specified module must have a class annotated with @SpringBootApplication module. Please, "
            + "specify it with --package option, or focus it before running this command.");

    // Check if entity has any associated repository
    Set<ClassOrInterfaceTypeDetails> repositories =
        typeLocationService
            .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_REPOSITORY_JPA);
    boolean entityHasRepository = false;
    ClassOrInterfaceTypeDetails relatedRepository = null;
    for (ClassOrInterfaceTypeDetails repository : repositories) {
      if (repository.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA).getAttribute("entity") != null
          && repository.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA).getAttribute("entity")
              .getValue().equals(entity)) {
        entityHasRepository = true;
        relatedRepository = repository;
        break;
      }
    }
    if (!entityHasRepository) {
      LOGGER
          .log(
              Level.INFO,
              String
                  .format(
                      "Entity %s doesn't have an associated repository. You "
                          + "need at least an associated repository with finders to publish them to the web layer."
                          + "Please, create one associated repository with 'repository jpa' command.",
                      entity.getSimpleTypeName()));
      return;
    }

    // Get repository finder methods
    AnnotationMetadata findersAnnotation = relatedRepository.getAnnotation(RooJavaType.ROO_FINDERS);
    if (findersAnnotation != null && findersAnnotation.getAttribute("finders") != null) {
      List<String> entityFinders = new ArrayList<String>();
      List<?> values = (List<?>) findersAnnotation.getAttribute("finders").getValue();
      Iterator<?> it = values.iterator();

      while (it.hasNext()) {
        NestedAnnotationAttributeValue finder = (NestedAnnotationAttributeValue) it.next();
        if (finder.getValue() != null && finder.getValue().getAttribute("finder") != null) {
          entityFinders.add((String) finder.getValue().getAttribute("finder").getValue());
        }
      }

      // Check if specified finder methods exists in associated repository
      for (String finder : queryMethods) {
        if (!entityFinders.contains(finder)) {
          LOGGER.log(Level.INFO, String.format(
              "ERROR: Provided finder '%s' doesn't exists on the repository '%s' "
                  + "related to entity '%s'", finder, relatedRepository.getType()
                  .getSimpleTypeName(), entity.getSimpleTypeName()));
          return;
        }
      }
    }

    // Check if entity has any associated service
    Set<ClassOrInterfaceTypeDetails> services =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_SERVICE);
    boolean entityHasService = false;
    JavaType relatedService = null;
    for (ClassOrInterfaceTypeDetails service : services) {
      if (service.getAnnotation(RooJavaType.ROO_SERVICE).getAttribute("entity") != null
          && service.getAnnotation(RooJavaType.ROO_SERVICE).getAttribute("entity").getValue()
              .equals(entity)) {
        entityHasService = true;
        relatedService = service.getType();
        break;
      }
    }
    if (!entityHasService) {
      LOGGER.log(Level.INFO, String.format("Entity %s doesn't have associated services, "
          + "necessary to create controllers. Please, create one associated service with "
          + "'service' command before publish finders to web layer.", entity.getSimpleTypeName()));
      return;
    }

    // Seek for search type controllers related to entity
    Set<ClassOrInterfaceTypeDetails> controllers =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_CONTROLLER);
    List<ClassOrInterfaceTypeDetails> entitySearchControllers =
        new ArrayList<ClassOrInterfaceTypeDetails>();
    for (ClassOrInterfaceTypeDetails controller : controllers) {
      AnnotationMetadata controllerAnnotation =
          controller.getAnnotation(RooJavaType.ROO_CONTROLLER);

      // Get annotation type enum value
      ControllerType controllerType =
          ControllerType.getControllerType(((EnumDetails) controllerAnnotation.getAttribute("type")
              .getValue()).getField().getSymbolName());
      if (controllerAnnotation.getAttribute("type") != null
          && controllerType.equals(ControllerType
              .getControllerType(RooEnumDetails.CONTROLLER_TYPE_SEARCH.getField().getSymbolName()))
          && controllerAnnotation.getAttribute("entity") != null
          && controllerAnnotation.getAttribute("entity").getValue().equals(entity)) {

        // The controller is a search controller of the current entity
        entitySearchControllers.add(controller);
      }
    }

    // Check if any of the search controllers have the same pathPrefix.
    // If so, and controllerPackage is as well the same, update the controller.
    ClassOrInterfaceTypeDetails controllerToUpdateOrCreate = null;
    for (ClassOrInterfaceTypeDetails entitySearchController : entitySearchControllers) {
      AnnotationMetadata controllerAnnotation =
          entitySearchController.getAnnotation(RooJavaType.ROO_CONTROLLER);
      if (controllerAnnotation.getAttribute("pathPrefix") != null
          && controllerAnnotation.getAttribute("pathPrefix").getValue().equals(pathPrefix)) {
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
      JavaType searchController =
          new JavaType(String.format("%s.%sSearchController",
              controllerPackage.getFullyQualifiedPackageName(),
              Noun.pluralOf(entity.getSimpleTypeName(), Locale.ENGLISH)),
              controllerPackage.getModule());
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
      controllerAnnotation.addStringAttribute("pathPrefix", pathPrefix);
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

    } else {

      // Controller already exists, so create builder with it
      controllerBuilder = new ClassOrInterfaceTypeDetailsBuilder(controllerToUpdateOrCreate);

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
    addModuleDependencies(entity, relatedRepository, relatedService, controllerBuilder.build());

    // Write changes to disk
    typeManagementService.createOrUpdateTypeOnDisk(controllerBuilder.build());
  }

  @Override
  public void createOrUpdateSearchControllerForAllEntities(
      ControllerMVCResponseService responseType, JavaPackage controllerPackage, String pathPrefix) {
    Validate.notNull(responseType, "responseType required");
    Validate.notNull(controllerPackage, "package required");
    Validate.notNull(pathPrefix, "pathPrefix required");

    // Check if module has an application class
    Pom pom = projectOperations.getPomFromModuleName(controllerPackage.getModule());
    Validate.isTrue(typeLocationService.hasModuleFeature(pom, ModuleFeatureName.APPLICATION),
        "Specified module must have a class annotated with @SpringBootApplication module. Please, "
            + "specify it with --package option, or focus it before running this command.");

    // Search all entities with associated repository
    Set<ClassOrInterfaceTypeDetails> repositoryTypes =
        typeLocationService
            .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_REPOSITORY_JPA);
    for (ClassOrInterfaceTypeDetails entityDetails : typeLocationService
        .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_ENTITY)) {
      JavaType entity = entityDetails.getType();

      // Ignore abstract classes
      if (entityDetails.isAbstract()) {
        continue;
      }

      // Seek the repositories of each entity
      boolean hasRepository = false;
      for (ClassOrInterfaceTypeDetails repository : repositoryTypes) {
        if (repository.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA).getAttribute("entity")
            .getValue().equals(entity)) {
          hasRepository = true;
          AnnotationMetadata findersAnnotation = repository.getAnnotation(RooJavaType.ROO_FINDERS);
          if (findersAnnotation != null && findersAnnotation.getAttribute("finders") != null) {
            List<String> entityFinders = new ArrayList<String>();
            List<?> values = (List<?>) findersAnnotation.getAttribute("finders").getValue();
            Iterator<?> it = values.iterator();

            while (it.hasNext()) {
              NestedAnnotationAttributeValue finder = (NestedAnnotationAttributeValue) it.next();
              if (finder.getValue() != null && finder.getValue().getAttribute("finder") != null) {
                entityFinders.add((String) finder.getValue().getAttribute("finder").getValue());
              }
            }
            this.createOrUpdateSearchControllerForEntity(entity, entityFinders, responseType,
                controllerPackage, pathPrefix);
          } else {
            LOGGER
                .log(
                    Level.INFO,
                    String
                        .format(
                            "Entity %s hasn't any finder associated and web "
                                + "finder generation won't have effects. Use 'finder add' command to create finders.",
                            entity.getSimpleTypeName()));
          }
        }
      }
      if (!hasRepository) {
        LOGGER
            .log(
                Level.INFO,
                String
                    .format(
                        "Entity %s hasn't any repository associated. Web "
                            + "finder generation won't have effects. Use 'repository jpa' command to create repositories.",
                        entity.getSimpleTypeName()));
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
}
