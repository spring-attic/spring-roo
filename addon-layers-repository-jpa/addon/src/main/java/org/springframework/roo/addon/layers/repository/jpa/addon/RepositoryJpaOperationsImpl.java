package org.springframework.roo.addon.layers.repository.jpa.addon;

import static org.springframework.roo.model.RooJavaType.ROO_JPA_ENTITY;
import static org.springframework.roo.model.RooJavaType.ROO_READ_ONLY_REPOSITORY;
import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_JPA;
import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_JPA_CUSTOM;
import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_JPA_CUSTOM_IMPL;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * The {@link RepositoryJpaOperations} implementation.
 * 
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @since 1.2.0
 */
@Component
@Service
public class RepositoryJpaOperationsImpl implements RepositoryJpaOperations {

  protected final static Logger LOGGER = HandlerUtils.getLogger(RepositoryJpaOperationsImpl.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private FileManager fileManager;
  private PathResolver pathResolver;
  private ProjectOperations projectOperations;
  private TypeManagementService typeManagementService;
  private TypeLocationService typeLocationService;
  private MemberDetailsScanner memberDetailsScanner;
  private MetadataService metadataService;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  @Override
  public boolean isRepositoryInstallationPossible() {
    return getProjectOperations().isFeatureInstalled(FeatureNames.JPA);
  }

  @Override
  public void generateAllRepositories(JavaPackage repositoriesPackage) {
    // Getting all project entities
    Set<ClassOrInterfaceTypeDetails> entities =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(ROO_JPA_ENTITY);
    Iterator<ClassOrInterfaceTypeDetails> it = entities.iterator();
    while (it.hasNext()) {
      ClassOrInterfaceTypeDetails entity = it.next();

      getProjectOperations().setModule(
          getProjectOperations().getPomFromModuleName(
              StringUtils.substringBetween(entity.getDeclaredByMetadataId(), "#", ":")));

      // Generating new interface type using entity
      JavaType interfaceType =
          new JavaType(repositoriesPackage.getFullyQualifiedPackageName().concat(".")
              .concat(entity.getType().getSimpleTypeName()).concat("Repository"),
              repositoriesPackage.getModule());

      // Delegate on simple add repository method
      addRepository(interfaceType, entity.getType());

    }

  }

  @Override
  public void addRepository(final JavaType interfaceType, final JavaType domainType) {
    Validate.notNull(interfaceType, "ERROR: You must specify an interface repository type.");
    Validate.notNull(domainType, "ERROR: You must specify a valid Entity. ");

    // Check if entity provided type is annotated with @RooJpaEntity
    ClassOrInterfaceTypeDetails entityDetails = getTypeLocationService().getTypeDetails(domainType);
    AnnotationMetadata entityAnnotation = entityDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY);

    // Show an error indicating that entity should be annotated with
    // @RooJpaEntity
    Validate.notNull(entityAnnotation,
        "ERROR: Provided entity should be annotated with @RooJpaEntity");

    // Set module where generate the artefacts
    getProjectOperations().setModule(
        getProjectOperations().getPomFromModuleName(interfaceType.getModule()));

    // Check if the new interface to be created exists yet
    final String interfaceIdentifier =
        getPathResolver().getFocusedCanonicalPath(Path.SRC_MAIN_JAVA, interfaceType);

    if (getFileManager().exists(interfaceIdentifier)) {
      // Type already exists - return.
      return;
    }

    // Check if already exists a repository that manage current entity
    // Only one repository per entity is allowed
    Set<ClassOrInterfaceTypeDetails> existingRepositories =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_REPOSITORY_JPA);

    for (ClassOrInterfaceTypeDetails existingRepository : existingRepositories) {
      AnnotationAttributeValue<Object> relatedEntity =
          existingRepository.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA).getAttribute("entity");
      if (relatedEntity.getValue().equals(domainType)) {
        LOGGER
            .log(
                Level.INFO,
                String
                    .format(
                        "INFO: Already exists a repository associated to the entity '%s'. Only one repository per entity is allowed.",
                        domainType.getSimpleTypeName()));
        return;
      }
    }

    // Check if current entity is defined as "readOnly".
    AnnotationAttributeValue<Boolean> readOnlyAttr =
        entityDetails.getAnnotation(ROO_JPA_ENTITY).getAttribute("readOnly");

    boolean readOnly = readOnlyAttr != null && readOnlyAttr.getValue() ? true : false;

    // If is readOnly entity, generates common ReadOnlyRepository interface
    if (readOnly) {
      generateReadOnlyRepository(interfaceType.getPackage());
    }

    // Generates repository interface
    addRepositoryInterface(interfaceType, domainType, entityDetails, interfaceIdentifier);

    // By default, generate RepositoryCustom interface and its
    // implementation that allow developers to include its dynamic queries
    // using QueryDSL
    addRepositoryCustom(domainType, interfaceType, interfaceType.getPackage());

    // Add querydsl dependency
    final Element configuration = XmlUtils.getConfiguration(getClass());
    final List<Element> dependencies =
        XmlUtils.findElements("/configuration/dependencies/dependency", configuration);
    for (final Element dependencyElement : dependencies) {
      getProjectOperations().addDependency(getProjectOperations().getFocusedModuleName(),
          new Dependency(dependencyElement));
    }

    // Add querydsl Plugin
    List<Element> elements = XmlUtils.findElements("/configuration/plugins/plugin", configuration);
    for (final Element element : elements) {
      getProjectOperations().addBuildPlugin(getProjectOperations().getFocusedModuleName(),
          new Plugin(element));
    }

  }

  /**
   * Method that generates the repository interface. This method takes in mind
   * if entity is defined as readOnly or not.
   * 
   * @param interfaceType
   * @param domainType
   * @param entityDetails
   * @param interfaceIdentifier
   */
  private void addRepositoryInterface(JavaType interfaceType, JavaType domainType,
      ClassOrInterfaceTypeDetails entityDetails, String interfaceIdentifier) {
    // Generates @RooJpaRepository annotation with referenced entity value
    // and repository custom associated to this repository
    final AnnotationMetadataBuilder interfaceAnnotationMetadata =
        new AnnotationMetadataBuilder(ROO_REPOSITORY_JPA);
    interfaceAnnotationMetadata.addAttribute(new ClassAttributeValue(new JavaSymbolName("entity"),
        domainType));
    // Generating interface
    final String interfaceMdId =
        PhysicalTypeIdentifier.createIdentifier(interfaceType,
            getPathResolver().getPath(interfaceIdentifier));
    final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(interfaceMdId, Modifier.PUBLIC, interfaceType,
            PhysicalTypeCategory.INTERFACE);

    // Annotate repository interface
    cidBuilder.addAnnotation(interfaceAnnotationMetadata.build());

    // Save new repository on disk
    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());

  }

  /**
   * Method that generates ReadOnlyRepository interface on current package. If
   * ReadOnlyRepository already exists in this or other package, will not be
   * generated.
   * 
   * @param repositoryPackage Package where ReadOnlyRepository should be
   *            generated
   * @return JavaType with existing or new ReadOnlyRepository
   */
  private JavaType generateReadOnlyRepository(JavaPackage repositoryPackage) {

    // First of all, check if already exists a @RooReadOnlyRepository
    // interface on current project
    Set<JavaType> readOnlyRepositories =
        getTypeLocationService().findTypesWithAnnotation(ROO_READ_ONLY_REPOSITORY);

    if (!readOnlyRepositories.isEmpty()) {
      Iterator<JavaType> it = readOnlyRepositories.iterator();
      while (it.hasNext()) {
        return it.next();
      }
    }

    final JavaType javaType =
        new JavaType(String.format("%s.ReadOnlyRepository", repositoryPackage));
    final String physicalPath =
        getPathResolver().getFocusedCanonicalPath(Path.SRC_MAIN_JAVA, javaType);

    // Including ReadOnlyRepository interface
    InputStream inputStream = null;
    try {
      // Use defined template
      inputStream = FileUtils.getInputStream(getClass(), "ReadOnlyRepository-template._java");
      String input = IOUtils.toString(inputStream);
      // Replacing package
      input = input.replace("__PACKAGE__", repositoryPackage.getFullyQualifiedPackageName());

      // Creating ReadOnlyRepository interface
      fileManager.createOrUpdateTextFileIfRequired(physicalPath, input, false);
    } catch (final IOException e) {
      throw new IllegalStateException(String.format("Unable to create '%s'", physicalPath), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }

    return javaType;

  }

  /**
   * Method that generates RepositoryCustom implementation on current package.
   * If this RepositoryCustom implementation already exists in this or other
   * package, will not be generated.
   * 
   * @param interfaceType
   * @param repository
   * @param entity
   * @return JavaType with existing or new RepositoryCustom implementation
   */
  private JavaType generateRepositoryCustomImpl(JavaType interfaceType, JavaType repository,
      JavaType entity) {

    // Getting RepositoryCustomImpl JavaType
    JavaType implType = new JavaType(repository.getFullyQualifiedTypeName().concat("Impl"));

    // Check if new class exists yet
    final String implIdentifier =
        getPathResolver().getFocusedCanonicalPath(Path.SRC_MAIN_JAVA, implType);

    if (getFileManager().exists(implIdentifier)) {
      // Type already exists - return
      return implType;
    }

    // Check if already exists some class annotated with
    // @RooJpaRepositoryCustomImpl
    // that implements the same repositoryCustom interface.
    Set<JavaType> repositoriesCustomImpl =
        getTypeLocationService().findTypesWithAnnotation(ROO_REPOSITORY_JPA_CUSTOM_IMPL);

    if (!repositoriesCustomImpl.isEmpty()) {
      Iterator<JavaType> it = repositoriesCustomImpl.iterator();
      while (it.hasNext()) {
        JavaType repositoryCustom = it.next();
        ClassOrInterfaceTypeDetails repositoryDetails =
            getTypeLocationService().getTypeDetails(repositoryCustom);
        AnnotationMetadata annotation =
            repositoryDetails.getAnnotation(ROO_REPOSITORY_JPA_CUSTOM_IMPL);
        AnnotationAttributeValue<JavaType> repositoryType = annotation.getAttribute("repository");
        if (repositoryType.getValue().equals(interfaceType)) {
          return repositoryType.getValue();
        }
      }
    }

    // If not, continue creating new RepositoryCustomImpl
    InputStream inputStream = null;
    try {
      // Use defined template
      inputStream = FileUtils.getInputStream(getClass(), "RepositoryCustomImpl-template._java");
      String input = IOUtils.toString(inputStream);
      // Replacing package
      input = input.replace("__PACKAGE__", implType.getPackage().getFullyQualifiedPackageName());

      // Replacing entity import
      input = input.replace("__ENTITY_IMPORT__", entity.getFullyQualifiedTypeName());

      // Replacing interface .class
      input = input.replace("__REPOSITORY_CUSTOM_INTERFACE__", interfaceType.getSimpleTypeName());

      // Replacing class name
      input = input.replaceAll("__REPOSITORY_CUSTOM_IMPL__", implType.getSimpleTypeName());

      // Replacing entity name
      input = input.replace("__ENTITY_NAME__", entity.getSimpleTypeName());

      // Creating RepositoryCustomImpl class
      fileManager.createOrUpdateTextFileIfRequired(implIdentifier, input, false);
    } catch (final IOException e) {
      throw new IllegalStateException(String.format("Unable to create '%s'", implIdentifier), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }

    return implType;

  }

  /**
   * Method that generates RepositoryCustom interface and its implementation
   * for an specific entity
   * 
   * @param domainType
   * @param repositoryType
   * @param repositoryPackage
   * 
   * @return JavaType with new RepositoryCustom interface.
   */
  private JavaType addRepositoryCustom(JavaType domainType, JavaType repositoryType,
      JavaPackage repositoryPackage) {

    // Getting RepositoryCustom interface JavaTYpe
    JavaType interfaceType =
        new JavaType(repositoryPackage.getFullyQualifiedPackageName().concat(".")
            .concat(repositoryType.getSimpleTypeName()).concat("Custom"));

    // Check if new interface exists yet
    final String interfaceIdentifier =
        getPathResolver().getFocusedCanonicalPath(Path.SRC_MAIN_JAVA, interfaceType);

    if (getFileManager().exists(interfaceIdentifier)) {
      // Type already exists - return
      return interfaceType;
    }

    final String interfaceMdId =
        PhysicalTypeIdentifier.createIdentifier(interfaceType,
            getPathResolver().getPath(interfaceIdentifier));
    final ClassOrInterfaceTypeDetailsBuilder interfaceBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(interfaceMdId, Modifier.PUBLIC, interfaceType,
            PhysicalTypeCategory.INTERFACE);

    // Generates @RooJpaRepositoryCustom annotation with referenced entity value
    final AnnotationMetadataBuilder repositoryCustomAnnotationMetadata =
        new AnnotationMetadataBuilder(ROO_REPOSITORY_JPA_CUSTOM);
    repositoryCustomAnnotationMetadata.addAttribute(new ClassAttributeValue(new JavaSymbolName(
        "entity"), domainType));

    interfaceBuilder.addAnnotation(repositoryCustomAnnotationMetadata);

    // Save RepositoryCustom interface and its implementation on disk
    getTypeManagementService().createOrUpdateTypeOnDisk(interfaceBuilder.build());

    generateRepositoryCustomImpl(interfaceType, repositoryType, domainType);

    return interfaceType;

  }

  public FileManager getFileManager() {
    if (fileManager == null) {
      // Get all Services implement FileManager interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(FileManager.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          fileManager = (FileManager) this.context.getService(ref);
          return fileManager;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load FileManager on RepositoryJpaOperationsImpl.");
        return null;
      }
    } else {
      return fileManager;
    }
  }

  public PathResolver getPathResolver() {
    if (pathResolver == null) {
      // Get all Services implement PathResolver interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(PathResolver.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          pathResolver = (PathResolver) this.context.getService(ref);
          return pathResolver;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load PathResolver on RepositoryJpaOperationsImpl.");
        return null;
      }
    } else {
      return pathResolver;
    }
  }

  public ProjectOperations getProjectOperations() {
    if (projectOperations == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          projectOperations = (ProjectOperations) this.context.getService(ref);
          return projectOperations;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on RepositoryJpaOperationsImpl.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }

  public TypeManagementService getTypeManagementService() {
    if (typeManagementService == null) {
      // Get all Services implement TypeManagementService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TypeManagementService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          typeManagementService = (TypeManagementService) this.context.getService(ref);
          return typeManagementService;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeManagementService on RepositoryJpaOperationsImpl.");
        return null;
      }
    } else {
      return typeManagementService;
    }
  }

  public TypeLocationService getTypeLocationService() {
    if (typeLocationService == null) {
      // Get all Services implement TypeLocationService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TypeLocationService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          typeLocationService = (TypeLocationService) this.context.getService(ref);
          return typeLocationService;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeLocationService on RepositoryJpaOperationsImpl.");
        return null;
      }
    } else {
      return typeLocationService;
    }
  }

  public MemberDetailsScanner getMemberDetailsScanner() {
    if (memberDetailsScanner == null) {
      // Get all Services implement MemberDetailsScanner interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(MemberDetailsScanner.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          memberDetailsScanner = (MemberDetailsScanner) this.context.getService(ref);
          return memberDetailsScanner;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load MemberDetailsScanner on RepositoryJpaOperationsImpl.");
        return null;
      }
    } else {
      return memberDetailsScanner;
    }
  }

  public MetadataService getMetadataService() {
    if (metadataService == null) {
      // Get all Services implement MetadataService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(MetadataService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          metadataService = (MetadataService) this.context.getService(ref);
          return metadataService;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load MetadataService on RepositoryJpaOperationsImpl.");
        return null;
      }
    } else {
      return metadataService;
    }
  }

  // Feature methods

  public String getName() {
    return FeatureNames.JPA;
  }

  public boolean isInstalledInModule(final String moduleName) {
    return getProjectOperations().isFeatureInstalled(FeatureNames.JPA);
  }
}
