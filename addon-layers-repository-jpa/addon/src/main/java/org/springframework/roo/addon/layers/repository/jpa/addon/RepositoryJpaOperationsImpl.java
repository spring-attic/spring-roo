package org.springframework.roo.addon.layers.repository.jpa.addon;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.jpa.addon.JpaOperations;
import org.springframework.roo.addon.jpa.addon.JpaOperationsImpl;
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
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Repository;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@link RepositoryJpaOperations} implementation.
 * 
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @author Sergio Clares
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
  private JpaOperationsImpl jpaOperations;

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
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_JPA_ENTITY);
    Iterator<ClassOrInterfaceTypeDetails> it = entities.iterator();
    while (it.hasNext()) {
      ClassOrInterfaceTypeDetails entity = it.next();

      // Ignore abstract classes
      if (!entity.isAbstract()) {
        // Generating new interface type using entity
        JavaType interfaceType =
            new JavaType(repositoriesPackage.getFullyQualifiedPackageName().concat(".")
                .concat(entity.getType().getSimpleTypeName()).concat("Repository"),
                repositoriesPackage.getModule());

        // Delegate on simple add repository method
        addRepository(interfaceType, entity.getType(), null);
      }
    }

  }

  @Override
  public void addRepository(JavaType interfaceType, final JavaType domainType,
      JavaType defaultReturnType) {
    Validate.notNull(domainType, "ERROR: You must specify a valid Entity. ");

    if (getProjectOperations().isMultimoduleProject()) {
      Validate.notNull(interfaceType,
          "ERROR: You must specify an interface repository type on multimodule projects.");
      Validate.notNull(interfaceType.getModule(),
          "ERROR: interfaceType module is required on multimodule projects.");
    } else if (interfaceType == null) {
      interfaceType =
          new JavaType(String.format("%s.%sRepository", domainType.getPackage(),
              domainType.getSimpleTypeName()), "");
    }

    // Check if entity provided type is annotated with @RooJpaEntity
    ClassOrInterfaceTypeDetails entityDetails = getTypeLocationService().getTypeDetails(domainType);
    AnnotationMetadata entityAnnotation = entityDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY);

    // Show an error indicating that entity should be annotated with
    // @RooJpaEntity
    Validate.notNull(entityAnnotation,
        "ERROR: Provided entity should be annotated with @RooJpaEntity");

    if (defaultReturnType != null) {

      ClassOrInterfaceTypeDetails defaultReturnTypeDetails =
          getTypeLocationService().getTypeDetails(defaultReturnType);
      AnnotationMetadata defaultReturnTypeAnnotation =
          defaultReturnTypeDetails.getAnnotation(RooJavaType.ROO_ENTITY_PROJECTION);

      // Show an error indicating that defaultReturnType should be annotated with
      // @RooEntityProjection
      Validate.notNull(defaultReturnTypeAnnotation,
          "ERROR: Provided defaultSearchResult should be annotated with @RooDTO");
    } else {
      defaultReturnType = domainType;
    }

    // Check if the new interface to be created already exists
    final String interfaceIdentifier =
        getPathResolver().getCanonicalPath(interfaceType.getModule(), Path.SRC_MAIN_JAVA,
            interfaceType);

    if (getFileManager().exists(interfaceIdentifier)) {
      // Type already exists - return.
      LOGGER.log(
          Level.INFO,
          String.format("INFO: The repository '%s' already exists.",
              interfaceType.getSimpleTypeName()));
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
        entityDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY).getAttribute("readOnly");

    boolean readOnly = readOnlyAttr != null && readOnlyAttr.getValue() ? true : false;

    // If is readOnly entity, generates common ReadOnlyRepository interface
    if (readOnly) {
      generateReadOnlyRepository(interfaceType.getPackage());
    }

    // Generates repository interface
    addRepositoryInterface(interfaceType, domainType, entityDetails, interfaceIdentifier);

    // Generate QueryDslRepositorySupportExt
    generateQueryDslRepositorySupportExt(interfaceType.getPackage());

    // By default, generate RepositoryCustom interface and its
    // implementation that allow developers to include its dynamic queries
    // using QueryDSL
    addRepositoryCustom(domainType, interfaceType, interfaceType.getPackage(), defaultReturnType);

    // Add dependencies between modules
    getProjectOperations().addModuleDependency(interfaceType.getModule(), domainType.getModule());

    // Add dependencies and plugins
    generateConfiguration(interfaceType, domainType);

  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void generateConfiguration(JavaType interfaceType, JavaType domainType) {

    final Element configuration = XmlUtils.getConfiguration(getClass());

    // Add querydsl dependency
    final List<Element> dependencies;
    final List<Element> plugins;

    if (getProjectOperations().isMultimoduleProject()) {
      dependencies =
          XmlUtils
              .findElements("/configuration/multimodule/dependencies/dependency", configuration);
      plugins = XmlUtils.findElements("/configuration/multimodule/plugins/plugin", configuration);

      // Add database test dependency
      getJpaOperationsImpl().addDatabaseDependencyWithTestScope(interfaceType.getModule(), null,
          null);

    } else {
      dependencies =
          XmlUtils.findElements("/configuration/monomodule/dependencies/dependency", configuration);
      plugins = XmlUtils.findElements("/configuration/monomodule/plugins/plugin", configuration);
    }

    for (final Element dependencyElement : dependencies) {
      getProjectOperations().addDependency(interfaceType.getModule(),
          new Dependency(dependencyElement));
    }

    // Add querydsl plugin
    Plugin queryDslPlugin = null;

    for (final Element pluginElement : plugins) {
      Plugin plugin = new Plugin(pluginElement);
      if (plugin.getArtifactId().equals("querydsl-maven-plugin")) {
        queryDslPlugin = plugin;
      }
      getProjectOperations().addBuildPlugin(interfaceType.getModule(), plugin);
    }

    if (getProjectOperations().isMultimoduleProject()) {

      if (queryDslPlugin == null) {
        throw new RuntimeException("Error: Missing QueryDSL plugin");
      }

      // Add entity package to find Q classes.
      Set<String> packages = new HashSet();
      for (ClassOrInterfaceTypeDetails cid : getTypeLocationService()
          .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_REPOSITORY_JPA)) {
        if (cid.getType().getModule().equals(interfaceType.getModule())) {

          JavaType relatedEntity =
              (JavaType) cid.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA).getAttribute("entity")
                  .getValue();
          String module =
              getTypeLocationService().getTypeDetails(relatedEntity).getType().getModule();

          if (!packages.contains(module)) {
            packages.add(module);

            getProjectOperations().addPackageToPluginExecution(interfaceType.getModule(),
                queryDslPlugin, "generate-qtypes",
                getProjectOperations().getTopLevelPackage(module).getFullyQualifiedPackageName());
          }
        }
      }
      getProjectOperations().addPackageToPluginExecution(
          interfaceType.getModule(),
          queryDslPlugin,
          "generate-qtypes",
          getProjectOperations().getTopLevelPackage(domainType.getModule())
              .getFullyQualifiedPackageName());
    } else {

      // Add querydsl processor repository
      List<Element> repositories =
          XmlUtils.findElements("/configuration/monomodule/repositories/repository", configuration);

      for (final Element repositoryElement : repositories) {
        getProjectOperations().addRepository(interfaceType.getModule(),
            new Repository(repositoryElement));
      }
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
        new AnnotationMetadataBuilder(RooJavaType.ROO_REPOSITORY_JPA);
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
        getTypeLocationService().findTypesWithAnnotation(RooJavaType.ROO_READ_ONLY_REPOSITORY);

    if (!readOnlyRepositories.isEmpty()) {
      Iterator<JavaType> it = readOnlyRepositories.iterator();
      while (it.hasNext()) {
        return it.next();
      }
    }

    final JavaType javaType =
        new JavaType(String.format("%s.ReadOnlyRepository", repositoryPackage),
            repositoryPackage.getModule());
    final String physicalPath =
        getPathResolver().getCanonicalPath(javaType.getModule(), Path.SRC_MAIN_JAVA, javaType);

    // Including ReadOnlyRepository interface
    InputStream inputStream = null;
    try {
      // Use defined template
      inputStream = FileUtils.getInputStream(getClass(), "ReadOnlyRepository-template._java");
      String input = IOUtils.toString(inputStream);
      // Replacing package
      input = input.replace("__PACKAGE__", repositoryPackage.getFullyQualifiedPackageName());

      // Creating ReadOnlyRepository interface
      getFileManager().createOrUpdateTextFileIfRequired(physicalPath, input, true);
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
    JavaType implType =
        new JavaType(repository.getFullyQualifiedTypeName().concat("Impl"), repository.getModule());

    // Check if new class exists yet
    final String implIdentifier =
        getPathResolver().getCanonicalPath(implType.getModule(), Path.SRC_MAIN_JAVA, implType);

    if (getFileManager().exists(implIdentifier)) {
      // Type already exists - return
      return implType;
    }

    // Check if already exists some class annotated with
    // @RooJpaRepositoryCustomImpl
    // that implements the same repositoryCustom interface.
    Set<JavaType> repositoriesCustomImpl =
        getTypeLocationService()
            .findTypesWithAnnotation(RooJavaType.ROO_REPOSITORY_JPA_CUSTOM_IMPL);

    if (!repositoriesCustomImpl.isEmpty()) {
      Iterator<JavaType> it = repositoriesCustomImpl.iterator();
      while (it.hasNext()) {
        JavaType repositoryCustom = it.next();
        ClassOrInterfaceTypeDetails repositoryDetails =
            getTypeLocationService().getTypeDetails(repositoryCustom);
        AnnotationMetadata annotation =
            repositoryDetails.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA_CUSTOM_IMPL);
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

      // Creates QEntity to be able to use QueryDsl
      JavaType qEntity =
          new JavaType(String.format("%s.Q%s", entity.getPackage(), entity.getSimpleTypeName()),
              entity.getModule());

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
   * @param defaultReturnType 
   * 
   * @return JavaType with new RepositoryCustom interface.
   */
  private JavaType addRepositoryCustom(JavaType domainType, JavaType repositoryType,
      JavaPackage repositoryPackage, JavaType defaultReturnType) {

    // Getting RepositoryCustom interface JavaTYpe
    JavaType interfaceType =
        new JavaType(repositoryPackage.getFullyQualifiedPackageName().concat(".")
            .concat(repositoryType.getSimpleTypeName()).concat("Custom"),
            repositoryType.getModule());

    // Check if new interface exists yet
    final String interfaceIdentifier =
        getPathResolver().getCanonicalPath(interfaceType.getModule(), Path.SRC_MAIN_JAVA,
            interfaceType);

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
        new AnnotationMetadataBuilder(RooJavaType.ROO_REPOSITORY_JPA_CUSTOM);
    repositoryCustomAnnotationMetadata.addAttribute(new ClassAttributeValue(new JavaSymbolName(
        "entity"), domainType));

    repositoryCustomAnnotationMetadata.addAttribute(new ClassAttributeValue(new JavaSymbolName(
        "defaultReturnType"), defaultReturnType));

    // Add dependencies between modules
    getProjectOperations().addModuleDependency(interfaceType.getModule(),
        defaultReturnType.getModule());

    interfaceBuilder.addAnnotation(repositoryCustomAnnotationMetadata);

    // Save RepositoryCustom interface and its implementation on disk
    getTypeManagementService().createOrUpdateTypeOnDisk(interfaceBuilder.build());

    generateRepositoryCustomImpl(interfaceType, repositoryType, domainType);

    return interfaceType;

  }

  /**
   * Method that generates QueryDslRepositorySupportExt on current package. 
   * If it already exists in this or other package, it will not be generated.
   * 
   * @param repositoryPackage Package where QueryDslRepositorySupportExt should 
   *            be generated
   * @return JavaType with existing or new QueryDslRepositorySupportExt
   */
  private JavaType generateQueryDslRepositorySupportExt(JavaPackage repositoryPackage) {

    // Create JavaType in repositoryPackage
    final JavaType javaType =
        new JavaType(String.format("%s.QueryDslRepositorySupportExt", repositoryPackage),
            repositoryPackage.getModule());
    final String physicalPath =
        getPathResolver().getCanonicalPath(javaType.getModule(), Path.SRC_MAIN_JAVA, javaType);

    // Find GlobalSearch and get fully qualified name
    Set<JavaType> globalSearchTypes =
        getTypeLocationService().findTypesWithAnnotation(RooJavaType.ROO_GLOBAL_SEARCH);
    JavaType globalSearch = null;
    if (!globalSearchTypes.isEmpty()) {
      for (JavaType type : globalSearchTypes) {
        globalSearch = type;
      }
    }
    Validate.notNull(globalSearch,
        "The project must have a GlobalSearch class to work properly with repositories.");

    // Including ReadOnlyRepository interface
    InputStream inputStream = null;
    try {
      // Use defined template
      inputStream =
          FileUtils.getInputStream(getClass(), "QueryDslRepositorySupportExt-template._java");
      String input = IOUtils.toString(inputStream);
      // Replacing package
      input = input.replace("__PACKAGE__", repositoryPackage.getFullyQualifiedPackageName());
      // Replacing GlobalSerach import
      input = input.replace("__GLOBAL_SEARCH_IMPORT__", globalSearch.getFullyQualifiedTypeName());

      // Creating ReadOnlyRepository interface
      getFileManager().createOrUpdateTextFileIfRequired(physicalPath, input, true);
    } catch (final IOException e) {
      throw new IllegalStateException(String.format("Unable to create '%s'", physicalPath), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }

    return javaType;

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

  /**
   * Method to get JpaOperations Service implementation
   * 
   * @return
   */
  public JpaOperationsImpl getJpaOperationsImpl() {
    if (jpaOperations == null) {
      // Get all Services implement JpaOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(JpaOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          jpaOperations = (JpaOperationsImpl) this.context.getService(ref);
          return jpaOperations;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load JpaOperations on ProjectConfigurationController.");
        return null;
      }
    } else {
      return jpaOperations;
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
