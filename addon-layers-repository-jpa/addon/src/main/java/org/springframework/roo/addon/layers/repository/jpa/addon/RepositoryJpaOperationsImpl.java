package org.springframework.roo.addon.layers.repository.jpa.addon;

import static java.lang.reflect.Modifier.PUBLIC;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.jpa.addon.JpaOperations;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Repository;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
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
 * @author Jose Manuel Vivó
 * @since 1.2.0
 */
@Component
@Service
public class RepositoryJpaOperationsImpl implements RepositoryJpaOperations {

  protected final static Logger LOGGER = HandlerUtils.getLogger(RepositoryJpaOperationsImpl.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    serviceInstaceManager.activate(this.context);
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
      if (entity.isAbstract()) {
        continue;
      }

      // Generating new interface type using entity
      JavaType interfaceType =
          new JavaType(repositoriesPackage.getFullyQualifiedPackageName().concat(".")
              .concat(entity.getType().getSimpleTypeName()).concat("Repository"),
              repositoriesPackage.getModule());

      // Delegate on simple add repository method
      addRepository(interfaceType, entity.getType(), null, false);
    }

  }

  @Override
  public void addRepository(JavaType interfaceType, final JavaType domainType,
      JavaType defaultReturnType, boolean failOnComposition) {
    Validate.notNull(domainType, "ERROR: You must specify a valid Entity. ");

    if (getProjectOperations().isMultimoduleProject()) {
      Validate.notNull(interfaceType,
          "ERROR: You must specify an interface repository type on multimodule projects.");
      Validate.notNull(interfaceType.getModule(),
          "ERROR: interfaceType module is required on multimodule projects.");
    } else if (interfaceType == null) {
      interfaceType =
          new JavaType(String.format("%s.repository.%sRepository", getProjectOperations()
              .getFocusedTopLevelPackage(), domainType.getSimpleTypeName()), "");
    }

    // Check if entity provided type is annotated with @RooJpaEntity
    ClassOrInterfaceTypeDetails entityDetails = getTypeLocationService().getTypeDetails(domainType);
    AnnotationMetadata entityAnnotation = entityDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY);

    // Show an error indicating that entity should be annotated with
    // @RooJpaEntity
    Validate.notNull(entityAnnotation,
        "ERROR: Provided entity should be annotated with @RooJpaEntity");

    if (!shouldGenerateRepository(entityDetails)) {
      if (failOnComposition) {
        throw new IllegalArgumentException(
            "%s is child part of a composition relation. Can't create repository (entity should be handle in parent part)");
      } else {
        // Nothing to do: silently exit
        return;
      }
    }

    if (defaultReturnType != null) {

      ClassOrInterfaceTypeDetails defaultReturnTypeDetails =
          getTypeLocationService().getTypeDetails(defaultReturnType);
      AnnotationMetadata defaultReturnTypeAnnotation =
          defaultReturnTypeDetails.getAnnotation(RooJavaType.ROO_ENTITY_PROJECTION);

      // Show an error indicating that defaultReturnType should be annotated with
      // @RooEntityProjection
      Validate.notNull(defaultReturnTypeAnnotation,
          "ERROR: Provided defaultReturnType should be annotated with @RooEntityProjection");
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

    // Add Springlets base repository class
    addRepositoryConfigurationClass();

    // Check if current entity is defined as "readOnly".
    AnnotationAttributeValue<Boolean> readOnlyAttr =
        entityDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY).getAttribute("readOnly");

    boolean readOnly = readOnlyAttr != null && readOnlyAttr.getValue() ? true : false;

    if (readOnly) {
      // If is readOnly entity, generates common ReadOnlyRepository interface
      generateReadOnlyRepository(interfaceType.getPackage());
    }

    // Generates repository interface
    addRepositoryInterface(interfaceType, domainType, entityDetails, interfaceIdentifier,
        defaultReturnType);

    // By default, generate RepositoryCustom interface and its
    // implementation that allow developers to include its dynamic queries
    // using QueryDSL
    addRepositoryCustom(domainType, interfaceType, interfaceType.getPackage());

    // Add dependencies between modules
    getProjectOperations().addModuleDependency(interfaceType.getModule(), domainType.getModule());

    // Add dependencies and plugins
    generateConfiguration(interfaceType, domainType);

  }

  /**
   * Checks for all the application modules in project and adds a repository 
   * configuration class, which uses the Springlets base repository class if 
   * none is already specified.
   *  
   */
  private void addRepositoryConfigurationClass() {
    Set<ClassOrInterfaceTypeDetails> applicationCids =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            SpringJavaType.SPRING_BOOT_APPLICATION);
    for (ClassOrInterfaceTypeDetails applicationCid : applicationCids) {

      // Obtain main application config class and its module
      Pom module =
          getProjectOperations().getPomFromModuleName(applicationCid.getType().getModule());

      // Create or update SpringDataJpaDetachableRepositoryConfiguration
      JavaType repositoryConfigurationClass =
          new JavaType(String.format("%s.config.SpringDataJpaDetachableRepositoryConfiguration",
              getTypeLocationService().getTopLevelPackageForModule(module)), module.getModuleName());

      Validate.notNull(repositoryConfigurationClass.getModule(),
          "ERROR: Module name is required to generate a valid JavaType");

      // Checks if new service interface already exists.
      final String repositoryConfigurationClassIdentifier =
          getPathResolver().getCanonicalPath(repositoryConfigurationClass.getModule(),
              Path.SRC_MAIN_JAVA, repositoryConfigurationClass);
      final String mid =
          PhysicalTypeIdentifier.createIdentifier(repositoryConfigurationClass, getPathResolver()
              .getPath(repositoryConfigurationClassIdentifier));
      if (!getFileManager().exists(repositoryConfigurationClassIdentifier)) {

        // Repository config class doesn't exist. Create class builder
        final ClassOrInterfaceTypeDetailsBuilder typeBuilder =
            new ClassOrInterfaceTypeDetailsBuilder(mid, PUBLIC, repositoryConfigurationClass,
                PhysicalTypeCategory.CLASS);

        // Add @RooJpaRepositoryConfiguration
        AnnotationMetadataBuilder repositoryCondigurationAnnotation =
            new AnnotationMetadataBuilder(RooJavaType.ROO_JPA_REPOSITORY_CONFIGURATION);
        typeBuilder.addAnnotation(repositoryCondigurationAnnotation);

        // Write new class disk
        getTypeManagementService().createOrUpdateTypeOnDisk(typeBuilder.build());
      }
    }
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
      JavaPackage repositoryPackage) {

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

    interfaceBuilder.addAnnotation(repositoryCustomAnnotationMetadata);

    // Save RepositoryCustom interface and its implementation on disk
    getTypeManagementService().createOrUpdateTypeOnDisk(interfaceBuilder.build());

    generateRepositoryCustomImpl(interfaceType, repositoryType, domainType);

    return interfaceType;

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
      ClassOrInterfaceTypeDetails entityDetails, String interfaceIdentifier,
      JavaType defaultReturnType) {
    // Generates @RooJpaRepository annotation with referenced entity value
    // and repository custom associated to this repository
    final AnnotationMetadataBuilder interfaceAnnotationMetadata =
        new AnnotationMetadataBuilder(RooJavaType.ROO_REPOSITORY_JPA);
    interfaceAnnotationMetadata.addAttribute(new ClassAttributeValue(new JavaSymbolName("entity"),
        domainType));
    if (defaultReturnType != null) {
      interfaceAnnotationMetadata.addAttribute(new ClassAttributeValue(new JavaSymbolName(
          "defaultReturnType"), defaultReturnType));

      // Add dependencies between modules
      getProjectOperations().addModuleDependency(interfaceType.getModule(),
          defaultReturnType.getModule());
    }
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
      getJpaOperations().addDatabaseDependencyWithTestScope(interfaceType.getModule(), null, null);

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

      // Replacing interface .class
      input = input.replace("__REPOSITORY_CUSTOM_INTERFACE__", interfaceType.getSimpleTypeName());

      // Replacing class name
      input = input.replaceAll("__REPOSITORY_CUSTOM_IMPL__", implType.getSimpleTypeName());

      // Replacing entity name
      input = input.replace("__ENTITY_NAME__", entity.getSimpleTypeName());

      // Creating RepositoryCustomImpl class
      getFileManager().createOrUpdateTextFileIfRequired(implIdentifier, input, false);
    } catch (final IOException e) {
      throw new IllegalStateException(String.format("Unable to create '%s'", implIdentifier), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }

    return implType;

  }

  private FileManager getFileManager() {
    return serviceInstaceManager.getServiceInstance(this, FileManager.class);
  }

  private PathResolver getPathResolver() {
    return serviceInstaceManager.getServiceInstance(this, PathResolver.class);
  }

  private ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);
  }

  public TypeManagementService getTypeManagementService() {
    return serviceInstaceManager.getServiceInstance(this, TypeManagementService.class);
  }

  public TypeLocationService getTypeLocationService() {
    return serviceInstaceManager.getServiceInstance(this, TypeLocationService.class);
  }

  public MemberDetailsScanner getMemberDetailsScanner() {
    return serviceInstaceManager.getServiceInstance(this, MemberDetailsScanner.class);
  }

  public MetadataService getMetadataService() {
    return serviceInstaceManager.getServiceInstance(this, MetadataService.class);
  }

  /**
   * Method to get JpaOperations Service implementation
   *
   * @return
   */
  public JpaOperations getJpaOperations() {
    return serviceInstaceManager.getServiceInstance(this, JpaOperations.class);
  }

  @Override
  public boolean shouldGenerateRepository(JavaType domainType) {
    ClassOrInterfaceTypeDetails entityDetails = getTypeLocationService().getTypeDetails(domainType);
    return shouldGenerateRepository(entityDetails);
  }

  private boolean shouldGenerateRepository(ClassOrInterfaceTypeDetails entity) {
    Pair<FieldMetadata, RelationInfo> compositionRelation =
        getJpaOperations().getFieldChildPartOfCompositionRelation(entity);
    if (compositionRelation == null) {
      return true;
    }
    return compositionRelation.getRight().cardinality != Cardinality.ONE_TO_ONE;
  }
}
