package org.springframework.roo.addon.layers.service.addon;

import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ENTITY;
import static org.springframework.roo.model.RooJavaType.ROO_SERVICE;
import static org.springframework.roo.model.RooJavaType.ROO_SERVICE_IMPL;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
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
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.ImportMetadata;
import org.springframework.roo.classpath.details.ImportMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.process.manager.FileManager;
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

      // Creating service interfaces for every entity
      JavaType interfaceType =
          new JavaType(String.format("%s.%sService", apiPackage.getFullyQualifiedPackageName(),
              domainType.getName().getSimpleTypeName()), apiPackage.getModule());

      // Creating service implementation for every entity
      JavaType implType =
          new JavaType(
              String.format("%s.%sServiceImpl", implPackage.getFullyQualifiedPackageName(),
                  domainType.getName().getSimpleTypeName()), implPackage.getModule());

      // Delegates on individual service creator
      addService(domainType.getType(), interfaceType, implType);
    }
  }

  @Override
  public void addService(final JavaType domainType, final JavaType interfaceType,
      final JavaType implType) {
    Validate.notNull(interfaceType,
        "ERROR: Interface type required to be able to generate service.");
    Validate.notNull(domainType, "ERROR: Domain type required to be able to generate service.");

    // Check if current entity has valid repository
    Set<ClassOrInterfaceTypeDetails> repositories =
        typeLocationService
            .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_REPOSITORY_JPA);

    ClassOrInterfaceTypeDetails repositoryDetails = null;
    for (ClassOrInterfaceTypeDetails repository : repositories) {
      AnnotationAttributeValue<JavaType> entityAttr =
          repository.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA).getAttribute("entity");

      if (entityAttr != null && entityAttr.getValue().equals(domainType)) {
        repositoryDetails = repository;
        break;
      }
    }

    if (repositoryDetails == null) {
      LOGGER
          .log(
              Level.INFO,
              String
                  .format(
                      "INFO: Service '%s' will not be generated because you didn't generate a @RooJpaRepository for entity '%s'. Use 'repository' commands to generate a valid repository and then try again.",
                      interfaceType.getSimpleTypeName(), domainType.getSimpleTypeName()));
      return;
    }

    // Generating service interface
    createServiceInterface(domainType, interfaceType);

    // Generating service implementation
    createServiceImplementation(interfaceType, implType, repositoryDetails);
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
  }

  /**
   * Method that creates the service implementation
   * 
   * @param interfaceType
   * @param implType
   */
  private void createServiceImplementation(final JavaType interfaceType, JavaType implType,
      ClassOrInterfaceTypeDetails repository) {
    Validate.notNull(interfaceType,
        "ERROR: Interface should be provided to be able to generate its implementation");
    Validate.notNull(interfaceType.getModule(), "ERROR: Interface module is required");

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

    // Add constructor
    implTypeBuilder.addConstructor(getServiceConstructor(declaredByMetadataId, repository));

    // Add necessary imports
    List<ImportMetadata> imports = new ArrayList<ImportMetadata>();
    imports.add(new ImportMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC, repository
        .getType().getPackage(), repository.getType(), false, false).build());
    implTypeBuilder.addImports(imports);

    // Write service implementation on disk
    typeManagementService.createOrUpdateTypeOnDisk(implTypeBuilder.build());
  }

  /**
   * Method that generates Service implementation constructor. If exists a
   * repository, it will be included as constructor parameter
   * 
   * @param declaredByMetadataId
   * @param repository
   * @return
   */
  private ConstructorMetadataBuilder getServiceConstructor(String declaredByMetadataId,
      ClassOrInterfaceTypeDetails repository) {

    ConstructorMetadataBuilder constructorBuilder =
        new ConstructorMetadataBuilder(declaredByMetadataId);
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Append repository parameter if needed
    if (repository != null) {
      constructorBuilder.addParameter("repository", repository.getType());
      bodyBuilder.appendFormalLine("this.repository = repository;");
    }

    constructorBuilder.setBodyBuilder(bodyBuilder);

    // Adding @Autowired annotation
    constructorBuilder.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));

    return constructorBuilder;
  }

}
