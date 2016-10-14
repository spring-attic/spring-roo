package org.springframework.roo.addon.layers.service.addon;

import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ENTITY;
import static org.springframework.roo.model.RooJavaType.ROO_SERVICE;
import static org.springframework.roo.model.RooJavaType.ROO_SERVICE_IMPL;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ImportMetadata;
import org.springframework.roo.classpath.details.ImportMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.DependencyScope;
import org.springframework.roo.project.DependencyType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Class that implements {@link ServiceOperations}.
 *
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @since 1.2.0
 */
@Component
@Service
public class ServiceOperationsImpl implements ServiceOperations {

  private static final Logger LOGGER = HandlerUtils.getLogger(ServiceOperationsImpl.class);

  @Reference
  private FileManager fileManager;
  @Reference
  private PathResolver pathResolver;
  @Reference
  private ProjectOperations projectOperations;
  @Reference
  private TypeManagementService typeManagementService;
  @Reference
  private TypeLocationService typeLocationService;

  @Override
  public boolean areServiceCommandsAvailable() {
    return projectOperations.isFocusedProjectAvailable();
  }

  @Override
  public void addAllServices(JavaPackage apiPackage, JavaPackage implPackage) {
    Validate.notNull(apiPackage.getModule(), "ApiPackage module is required");
    Validate.notNull(implPackage.getModule(), "ImplPackage module is required");

    // Getting all generated entities
    Set<ClassOrInterfaceTypeDetails> entities =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ROO_JPA_ENTITY);
    for (final ClassOrInterfaceTypeDetails domainType : entities) {

      // Ignore abstract entities
      if (!domainType.isAbstract()) {
        // Creating service interfaces for every entity
        JavaType interfaceType =
            new JavaType(String.format("%s.%sService", apiPackage.getFullyQualifiedPackageName(),
                domainType.getName().getSimpleTypeName()), apiPackage.getModule());

        // Creating service implementation for every entity
        JavaType implType =
            new JavaType(String.format("%s.%sServiceImpl", implPackage
                .getFullyQualifiedPackageName(), domainType.getName().getSimpleTypeName()),
                implPackage.getModule());

        // Delegates on individual service creator
        addService(domainType.getType(), interfaceType, implType);
      }
    }
  }

  @Override
  public void addService(final JavaType domainType, final JavaType interfaceType,
      final JavaType implType) {

    JavaType repositoryType = null;

    // Getting all repositories
    Set<ClassOrInterfaceTypeDetails> repositories =
        typeLocationService
            .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_REPOSITORY_JPA);

    // Find repository related to entity
    for (ClassOrInterfaceTypeDetails repository : repositories) {
      AnnotationAttributeValue<JavaType> entityAttr =
          repository.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA).getAttribute("entity");

      if (entityAttr != null && entityAttr.getValue().equals(domainType)) {
        repositoryType = repository.getType();
      }
    }

    if (repositoryType == null) {
      LOGGER
          .log(
              Level.INFO,
              String
                  .format(
                      "ERROR: Entity '%s' does not have any repository generated. Use 'repository' commands to generate a valid repository and then try again.",
                      domainType.getSimpleTypeName()));
      return;
    }

    addService(domainType, repositoryType, interfaceType, implType);
  }

  @Override
  public void addService(final JavaType domainType, JavaType repositoryType,
      JavaType interfaceType, JavaType implType) {
    Validate.notNull(domainType, "ERROR: Domain type required to be able to generate service.");
    if (projectOperations.isMultimoduleProject()) {
      Validate
          .notNull(repositoryType,
              "ERROR: You must specify a repository type to be able to generate service on multimodule projects.");
    }

    if (repositoryType == null) {
      Set<ClassOrInterfaceTypeDetails> repositories =
          typeLocationService
              .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_REPOSITORY_JPA);

      Iterator<ClassOrInterfaceTypeDetails> it = repositories.iterator();
      while (it.hasNext()) {
        ClassOrInterfaceTypeDetails repository = it.next();
        AnnotationAttributeValue<JavaType> entityAttr =
            repository.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA).getAttribute("entity");

        if (entityAttr != null && entityAttr.getValue().equals(domainType)) {
          repositoryType = repository.getType();
        }
      }

      if (repositoryType == null) {
        // Is necessary at least one repository to generate service
        LOGGER
            .log(
                Level.INFO,
                String
                    .format(
                        "ERROR: You must generate a repository to '%s' entity before to generate a new service.",
                        domainType.getFullyQualifiedTypeName()));
        return;
      }

    }

    if (interfaceType == null) {
      interfaceType =
          new JavaType(String.format("%s.%s%s", implType == null ? domainType.getPackage()
              : implType.getPackage(), domainType.getSimpleTypeName(), implType == null ? "Service"
              : implType.getSimpleTypeName().concat("Api")),
              implType == null ? domainType.getModule() : implType.getModule());
    }

    if (implType == null) {
      implType =
          new JavaType(String.format("%s.%sServiceImpl", interfaceType.getPackage(),
              domainType.getSimpleTypeName()), interfaceType.getModule());
    }



    // Check if current entity is related with the repository
    ClassOrInterfaceTypeDetails repository = typeLocationService.getTypeDetails(repositoryType);


    if (repository.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA) == null) {
      LOGGER
          .log(
              Level.INFO,
              String
                  .format(
                      "INFO: Repository '%s' is not annotated with @RooJpaRepository. Use 'repository' commands to generate a valid repository and then try again.",
                      repositoryType.getSimpleTypeName()));
      return;
    }

    // Generating service interface
    createServiceInterface(domainType, interfaceType);

    // Generating service implementation
    createServiceImplementation(interfaceType, implType, repository, domainType);
  }

  /**
   * Method that creates the service interface
   *
   * @param domainType
   * @param interfaceType
   */
  private void createServiceInterface(final JavaType domainType, final JavaType interfaceType) {

    Validate.notNull(interfaceType.getModule(), "JavaType %s does not have a module", domainType);

    // Checks if new service interface already exists.
    final String interfaceIdentifier =
        pathResolver.getCanonicalPath(interfaceType.getModule(), Path.SRC_MAIN_JAVA, interfaceType);
    if (fileManager.exists(interfaceIdentifier)) {
      return; // Type already exists - nothing to do
    }

    // Validate that user provides a valid entity
    Validate.notNull(domainType, "ERROR: Domain type required to generate service");
    ClassOrInterfaceTypeDetails entityDetails = typeLocationService.getTypeDetails(domainType);
    Validate.notNull(entityDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY),
        "ERROR: Provided entity should be annotated with @RooJpaEntity");

    // Generating @RooService annotation
    final AnnotationMetadataBuilder interfaceAnnotationMetadata =
        new AnnotationMetadataBuilder(ROO_SERVICE);
    interfaceAnnotationMetadata.addAttribute(new ClassAttributeValue(new JavaSymbolName("entity"),
        domainType));

    // Creating interface builder
    final String interfaceMid =
        PhysicalTypeIdentifier.createIdentifier(interfaceType,
            pathResolver.getPath(interfaceIdentifier));
    final ClassOrInterfaceTypeDetailsBuilder interfaceTypeBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(interfaceMid, PUBLIC, interfaceType,
            PhysicalTypeCategory.INTERFACE);
    // Adding @RooService annotation to current interface
    interfaceTypeBuilder.addAnnotation(interfaceAnnotationMetadata.build());

    // Write service interface on disk
    typeManagementService.createOrUpdateTypeOnDisk(interfaceTypeBuilder.build());

    // Add dependencies between modules
    projectOperations.addModuleDependency(interfaceType.getModule(), domainType.getModule());
  }

  /**
   * Method that creates the service implementation
   *
   * @param interfaceType
   * @param implType
   * @param domainType
   */
  private void createServiceImplementation(final JavaType interfaceType, JavaType implType,
      ClassOrInterfaceTypeDetails repository, JavaType domainType) {
    Validate.notNull(interfaceType,
        "ERROR: Interface should be provided to be able to generate its implementation");
    Validate.notNull(interfaceType.getModule(), "ERROR: Interface module is required");
    Validate.notNull(domainType, "ERROR: Domain type required to generate service");

    // Generating implementation JavaType if needed
    if (implType == null) {
      implType =
          new JavaType(String.format("%sImpl", interfaceType.getFullyQualifiedTypeName()),
              interfaceType.getModule());
    }

    Validate.notNull(implType.getModule(), "ERROR: Implementation module is required");

    // Checks if new service interface already exists.
    final String implIdentifier =
        pathResolver.getCanonicalPath(implType.getModule(), Path.SRC_MAIN_JAVA, implType);
    if (fileManager.exists(implIdentifier)) {
      return; // Type already exists - nothing to do
    }

    // Generating @RooServiceImpl annotation
    final AnnotationMetadataBuilder implAnnotationMetadata =
        new AnnotationMetadataBuilder(ROO_SERVICE_IMPL);
    implAnnotationMetadata.addAttribute(new ClassAttributeValue(new JavaSymbolName("service"),
        interfaceType));

    // Creating class builder
    final String implMid =
        PhysicalTypeIdentifier.createIdentifier(implType, pathResolver.getPath(implIdentifier));
    final ClassOrInterfaceTypeDetailsBuilder implTypeBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(implMid, PUBLIC, implType,
            PhysicalTypeCategory.CLASS);
    // Adding @RooService annotation to current interface
    implTypeBuilder.addAnnotation(implAnnotationMetadata.build());

    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(implTypeBuilder.build().getType(),
            pathResolver.getPath(implType.getModule(), Path.SRC_MAIN_JAVA));

    // Add necessary imports
    List<ImportMetadata> imports = new ArrayList<ImportMetadata>();
    imports.add(new ImportMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC, repository
        .getType().getPackage(), repository.getType(), false, false).build());
    implTypeBuilder.addImports(imports);

    // Write service implementation on disk
    typeManagementService.createOrUpdateTypeOnDisk(implTypeBuilder.build());

    // Add dependencies between modules
    projectOperations.addModuleDependency(implType.getModule(), interfaceType.getModule());
    projectOperations.addModuleDependency(implType.getModule(), repository.getName().getModule());
    projectOperations.addModuleDependency(implType.getModule(), domainType.getModule());

    // ROO-3799 Included dependency spring-tx if it's a multimodule project
    if (projectOperations.isMultimoduleProject()) {
      projectOperations.addDependency(implType.getModule(), new Dependency("org.springframework",
          "spring-tx", "", DependencyType.JAR, DependencyScope.COMPILE));
    }

  }
}
