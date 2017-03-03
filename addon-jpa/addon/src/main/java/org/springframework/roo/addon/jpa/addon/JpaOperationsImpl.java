package org.springframework.roo.addon.jpa.addon;

import static org.springframework.roo.model.JavaType.OBJECT;
import static org.springframework.roo.model.JpaJavaType.COLUMN;
import static org.springframework.roo.model.JpaJavaType.EMBEDDABLE;
import static org.springframework.roo.model.JpaJavaType.EMBEDDED_ID;
import static org.springframework.roo.model.JpaJavaType.GENERATED_VALUE;
import static org.springframework.roo.model.JpaJavaType.GENERATION_TYPE;
import static org.springframework.roo.model.JpaJavaType.ID;
import static org.springframework.roo.model.JpaJavaType.SEQUENCE_GENERATOR;
import static org.springframework.roo.model.JpaJavaType.VERSION;
import static org.springframework.roo.model.RooJavaType.ROO_EQUALS;
import static org.springframework.roo.model.RooJavaType.ROO_IDENTIFIER;
import static org.springframework.roo.model.RooJavaType.ROO_JAVA_BEAN;
import static org.springframework.roo.model.RooJavaType.ROO_SERIALIZABLE;
import static org.springframework.roo.model.RooJavaType.ROO_TO_STRING;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.jpa.addon.entity.IdentifierStrategy;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.jpa.annotations.entity.JpaRelationType;
import org.springframework.roo.application.config.ApplicationConfigService;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.operations.InheritanceType;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of {@link JpaOperations}.
 *
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @author Juan Carlos García
 * @author Paula Navarro
 * @author Jose Manuel Vivó
 * @author Sergio Clares
 * @since 1.0
 */
@Component
@Service
public class JpaOperationsImpl implements JpaOperations {

  protected final static Logger LOGGER = HandlerUtils.getLogger(JpaOperationsImpl.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private static final String DATASOURCE_PREFIX = "spring.datasource";
  private static final String DATABASE_DRIVER = "driver-class-name";
  private static final String DATABASE_PASSWORD = "password";
  private static final String DATABASE_URL = "url";
  private static final String DATABASE_USERNAME = "username";
  private static final String JNDI_NAME = "jndi-name";
  private static final String HIBERNATE_NAMING_STRATEGY = "spring.jpa.hibernate.naming.strategy";
  private static final String HIBERNATE_NAMING_STRATEGY_VALUE =
      "org.hibernate.cfg.ImprovedNamingStrategy";
  static final String POM_XML = "pom.xml";

  private ServiceInstaceManager serviceManager = new ServiceInstaceManager();

  private static final Property SPRINGLETS_VERSION_PROPERTY = new Property("springlets.version",
      "1.2.0.RC1");
  private static final Dependency SPRINGLETS_DATA_JPA_STARTER = new Dependency("io.springlets",
      "springlets-data-jpa", "${springlets.version}");
  private static final Dependency SPRINGLETS_DATA_COMMONS_STARTER = new Dependency("io.springlets",
      "springlets-data-commons", "${springlets.version}");
  private static final Dependency SPRINGLETS_CONTEXT_DEPENDENCY = new Dependency("io.springlets",
      "springlets-context", "${springlets.version}");

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    this.serviceManager.activate(this.context);
  }

  @Override
  public void configureJpa(final OrmProvider ormProvider, final JdbcDatabase jdbcDatabase,
      final Pom module, final String jndi, final String hostName, final String databaseName,
      final String userName, final String password, final String profile, final boolean force) {

    Validate.notNull(module, "Module required");
    Validate.notNull(ormProvider, "ORM provider required");
    if (StringUtils.isBlank(jndi)) {
      Validate.notNull(jdbcDatabase, "JDBC database or JNDI data source required");
    }

    // Parse the configuration.xml file
    final Element configuration = XmlUtils.getConfiguration(getClass());

    // Get the first part of the XPath expressions for unwanted databases
    // and ORM providers
    if (jdbcDatabase != null) {
      final String databaseXPath = getDbXPath(getUnwantedDatabases(jdbcDatabase));
      final String providersXPath = getProviderXPath(getUnwantedOrmProviders(ormProvider));
      final String startersXPath = getStarterXPath(getUnwantedOrmProviders(ormProvider));

      // Updating pom.xml including necessary properties, dependencies and Spring Boot starters
      updateDependencies(module, configuration, ormProvider, jdbcDatabase, startersXPath,
          providersXPath, databaseXPath, profile);
    }

    // Update Spring Config File with spring.datasource.* domain properties
    updateApplicationProperties(module.getModuleName(), ormProvider, jdbcDatabase, hostName,
        databaseName, userName, password, jndi, profile, force);

  }

  @Override
  public boolean isJpaInstallationPossible() {
    return getProjectOperations().isFocusedProjectAvailable();
  }

  @Override
  public void newEmbeddableClass(final JavaType name, final boolean serializable) {

    Validate.notNull(name, "Embeddable name required");

    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(name,
            getPathResolver().getFocusedPath(Path.SRC_MAIN_JAVA));

    final List<AnnotationMetadataBuilder> annotations =
        new ArrayList<AnnotationMetadataBuilder>(Arrays.asList(new AnnotationMetadataBuilder(
            ROO_JAVA_BEAN), new AnnotationMetadataBuilder(ROO_TO_STRING),
            new AnnotationMetadataBuilder(EMBEDDABLE)));

    if (serializable) {
      annotations.add(new AnnotationMetadataBuilder(ROO_SERIALIZABLE));
    }

    final int modifier = Modifier.PUBLIC;
    final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, modifier, name,
            PhysicalTypeCategory.CLASS);
    cidBuilder.setAnnotations(annotations);

    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());

    getProjectOperations().addDependency(name.getModule(),
        new Dependency("org.springframework.boot", "spring-boot-starter-data-jpa", null));
  }

  @Override
  public void newEntity(final JavaType name, final boolean createAbstract,
      final JavaType superclass, final JavaType implementsType, final String identifierField,
      final JavaType identifierType, final String identifierColumn, final String sequenceName,
      final IdentifierStrategy identifierStrategy, final String versionField,
      final JavaType versionType, final String versionColumn,
      final InheritanceType inheritanceType, final List<AnnotationMetadataBuilder> annotations) {

    Validate.notNull(name, "Entity name required");
    Validate.isTrue(!JdkJavaType.isPartOfJavaLang(name.getSimpleTypeName()),
        "Entity name '%s' must not be part of java.lang", name.getSimpleTypeName());

    getProjectOperations().setModule(getProjectOperations().getPomFromModuleName(name.getModule()));

    // Add springlets-context dependency
    getProjectOperations().addDependency(name.getModule(), SPRINGLETS_CONTEXT_DEPENDENCY);
    getProjectOperations().addProperty("", SPRINGLETS_VERSION_PROPERTY);

    int modifier = Modifier.PUBLIC;
    if (createAbstract) {
      modifier |= Modifier.ABSTRACT;
    }

    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(name,
            getPathResolver().getFocusedPath(Path.SRC_MAIN_JAVA));
    final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, modifier, name,
            PhysicalTypeCategory.CLASS);

    if (!superclass.equals(OBJECT)) {
      final ClassOrInterfaceTypeDetails superclassClassOrInterfaceTypeDetails =
          getTypeLocationService().getTypeDetails(superclass);
      if (superclassClassOrInterfaceTypeDetails != null) {
        cidBuilder.setSuperclass(new ClassOrInterfaceTypeDetailsBuilder(
            superclassClassOrInterfaceTypeDetails));

        //Add dependency with superclass module
        getProjectOperations().addModuleDependency(superclass.getModule());
      }
    }

    cidBuilder.setExtendsTypes(Arrays.asList(superclass));

    if (implementsType != null) {
      final Set<JavaType> implementsTypes = new LinkedHashSet<JavaType>();
      final ClassOrInterfaceTypeDetails typeDetails =
          getTypeLocationService().getTypeDetails(declaredByMetadataId);
      if (typeDetails != null) {
        implementsTypes.addAll(typeDetails.getImplementsTypes());
      }
      implementsTypes.add(implementsType);
      cidBuilder.setImplementsTypes(implementsTypes);

      //Add dependency with implementsType modules
      getProjectOperations().addModuleDependency(implementsType.getModule());
    }

    // Set annotations to new entity
    cidBuilder.setAnnotations(annotations);

    // Write entity on disk
    ClassOrInterfaceTypeDetails entityDetails = cidBuilder.build();
    getTypeManagementService().createOrUpdateTypeOnDisk(entityDetails);

    // If a parent is defined, it must provide the identifier field.
    // Adding identifier and version fields
    if (superclass.equals(OBJECT)) {
      getTypeManagementService().addField(
          getIdentifierField(name, identifierField, identifierType, identifierColumn, sequenceName,
              identifierStrategy, inheritanceType), true);
      getTypeManagementService().addField(
          getVersionField(name, versionField, versionType, versionColumn), true);
    }

    // Add persistence dependencies to entity module if necessary
    // Don't need to add them if spring-boot-starter-data-jpa is present, often in single module project
    if (!getProjectOperations().getFocusedModule().hasDependencyExcludingVersion(
        new Dependency("org.springframework.boot", "spring-boot-starter-data-jpa", null))) {
      List<Dependency> dependencies = new ArrayList<Dependency>();
      dependencies.add(new Dependency("org.springframework", "spring-aspects", null));
      dependencies.add(new Dependency("org.springframework", "spring-context", null));
      dependencies.add(new Dependency("org.springframework.data", "spring-data-jpa", null));
      dependencies.add(new Dependency("org.springframework.data", "spring-data-commons", null));
      dependencies.add(new Dependency("org.eclipse.persistence", "javax.persistence", null));
      getProjectOperations().addDependencies(getProjectOperations().getFocusedModuleName(),
          dependencies);
    }
  }

  @Override
  public void updateEmbeddableToIdentifier(final JavaType identifierType,
      final String identifierField, final String identifierColumn) {

    Validate.notNull(identifierType, "Identifier type required");

    // Get details from existing JavaType
    ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(getTypeLocationService().getTypeDetails(
            identifierType));

    // Create @RooIdentifier with getters and setters
    AnnotationMetadataBuilder rooIdentifier = new AnnotationMetadataBuilder(ROO_IDENTIFIER);
    rooIdentifier.addBooleanAttribute("settersByDefault", true);

    final List<AnnotationMetadataBuilder> identifierAnnotations =
        Arrays.asList(new AnnotationMetadataBuilder(ROO_TO_STRING), new AnnotationMetadataBuilder(
            ROO_EQUALS), rooIdentifier);
    cidBuilder.setAnnotations(identifierAnnotations);

    // Set implement Serializable
    List<JavaType> implementTypes = new ArrayList<JavaType>();
    implementTypes.add(JdkJavaType.SERIALIZABLE);
    cidBuilder.setImplementsTypes(implementTypes);

    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
  }

  @Override
  public SortedSet<String> getDatabaseProperties(String profile) {
    return getApplicationConfigService().getPropertyKeys(DATASOURCE_PREFIX, true, profile);
  }

  @Override
  public boolean isJpaInstalled() {
    return getProjectOperations().isFeatureInstalled(FeatureNames.JPA);
  }


  private String getConnectionString(final JdbcDatabase jdbcDatabase, String hostName,
      final String databaseName) {

    String connectionString = jdbcDatabase.getConnectionString();
    if (connectionString.contains("TO_BE_CHANGED_BY_ADDON")) {
      connectionString =
          connectionString.replace("TO_BE_CHANGED_BY_ADDON",
              StringUtils.isNotBlank(databaseName) ? databaseName : getProjectOperations()
                  .getProjectName(""));
    } else {
      if (StringUtils.isNotBlank(databaseName)) {
        // Oracle uses a different connection URL - see ROO-1203
        final String dbDelimiter = jdbcDatabase == JdbcDatabase.ORACLE ? ":" : "/";
        connectionString += dbDelimiter + databaseName;
      }
    }
    if (StringUtils.isBlank(hostName)) {
      hostName = "localhost";
    }
    return connectionString.replace("HOST_NAME", hostName);
  }

  private String getDbXPath(final List<JdbcDatabase> databases) {
    final StringBuilder builder = new StringBuilder("/configuration/databases/database[");
    for (int i = 0; i < databases.size(); i++) {
      if (i > 0) {
        builder.append(" or ");
      }
      builder.append("@id = '");
      builder.append(databases.get(i).getKey());
      builder.append("'");
    }
    builder.append("]");
    return builder.toString();
  }

  private String getProviderXPath(final List<OrmProvider> ormProviders) {
    final StringBuilder builder = new StringBuilder("/configuration/ormProviders/provider[");
    for (int i = 0; i < ormProviders.size(); i++) {
      if (i > 0) {
        builder.append(" or ");
      }
      builder.append("@id = '");
      builder.append(ormProviders.get(i).name());
      builder.append("'");
    }
    builder.append("]");
    return builder.toString();
  }

  private String getStarterXPath(final List<OrmProvider> ormProviders) {
    final StringBuilder builder = new StringBuilder("/configuration/starter/provider[");
    for (int i = 0; i < ormProviders.size(); i++) {
      if (i > 0) {
        builder.append(" or ");
      }
      builder.append("@id = '");
      builder.append(ormProviders.get(i).name());
      builder.append("'");
    }
    builder.append("]");
    return builder.toString();
  }

  private List<JdbcDatabase> getUnwantedDatabases(final JdbcDatabase jdbcDatabase) {
    final List<JdbcDatabase> unwantedDatabases = new ArrayList<JdbcDatabase>();
    for (final JdbcDatabase database : JdbcDatabase.values()) {
      if (!database.getKey().equals(jdbcDatabase.getKey())
          && !database.getDriverClassName().equals(jdbcDatabase.getDriverClassName())) {
        unwantedDatabases.add(database);
      }
    }
    return unwantedDatabases;
  }

  private List<OrmProvider> getUnwantedOrmProviders(final OrmProvider ormProvider) {
    final List<OrmProvider> unwantedOrmProviders =
        new LinkedList<OrmProvider>(Arrays.asList(OrmProvider.values()));
    unwantedOrmProviders.remove(ormProvider);
    return unwantedOrmProviders;
  }

  public boolean hasDatabaseProperties() {
    SortedSet<String> databaseProperties =
        getApplicationConfigService().getPropertyKeys(DATASOURCE_PREFIX, false, null);

    return !databaseProperties.isEmpty();
  }

  private void updateApplicationProperties(final String moduleName, final OrmProvider ormProvider,
      final JdbcDatabase jdbcDatabase, final String hostName, final String databaseName,
      String userName, final String password, String jndi, String profile, boolean force) {

    // Check if jndi is blank. If is blank, include database properties on
    // application.properties file
    if (StringUtils.isBlank(jndi)) {

      final String connectionString = getConnectionString(jdbcDatabase, hostName, databaseName);

      // Getting current properties
      final String driver =
          getApplicationConfigService().getProperty(moduleName, DATASOURCE_PREFIX, DATABASE_DRIVER,
              profile);
      final String url =
          getApplicationConfigService().getProperty(moduleName, DATASOURCE_PREFIX, DATABASE_URL,
              profile);
      final String uname =
          getApplicationConfigService().getProperty(moduleName, DATASOURCE_PREFIX,
              DATABASE_USERNAME, profile);
      final String pwd =
          getApplicationConfigService().getProperty(moduleName, DATASOURCE_PREFIX,
              DATABASE_PASSWORD, profile);

      boolean hasChanged = !jdbcDatabase.getDriverClassName().equals(driver);
      hasChanged |= !connectionString.equals(url);
      hasChanged |= !StringUtils.stripToEmpty(userName).equals(uname);
      hasChanged |= !StringUtils.stripToEmpty(password).equals(pwd);
      if (!hasChanged) {
        LOGGER.log(Level.INFO, "INFO: No changes are needed.");
        return;
      }

      // Write changes to Spring Config file
      Map<String, String> props = new HashMap<String, String>();
      props.put(DATABASE_URL, connectionString);
      props.put(DATABASE_DRIVER, jdbcDatabase.getDriverClassName());
      if (userName != null) {
        props.put(DATABASE_USERNAME, StringUtils.stripToEmpty(userName));
      } else {
        getApplicationConfigService().removeProperty(moduleName, DATASOURCE_PREFIX,
            DATABASE_USERNAME, profile);
      }
      if (password != null) {
        props.put(DATABASE_PASSWORD, StringUtils.stripToEmpty(password));
      } else {
        getApplicationConfigService().removeProperty(moduleName, DATASOURCE_PREFIX,
            DATABASE_PASSWORD, profile);
      }

      getApplicationConfigService().addProperties(moduleName, DATASOURCE_PREFIX, props, profile,
          force);

      // Remove jndi property
      getApplicationConfigService().removeProperty(moduleName, DATASOURCE_PREFIX, JNDI_NAME,
          profile);

    } else {

      final String jndiProperty =
          getApplicationConfigService().getProperty(moduleName, DATASOURCE_PREFIX, JNDI_NAME);

      boolean hasChanged =
          jndiProperty == null || !jndiProperty.equals(StringUtils.stripToEmpty(jndi));
      if (!hasChanged) {
        // No changes from existing database configuration so exit now
        return;
      }

      // Write changes to Spring Config file defined in profile
      Map<String, String> props = new HashMap<String, String>();
      props.put(JNDI_NAME, jndi);
      getApplicationConfigService().addProperties(moduleName, DATASOURCE_PREFIX, props, profile,
          force);

      // Remove old properties if existing
      getApplicationConfigService().removeProperty(moduleName, DATASOURCE_PREFIX, DATABASE_URL,
          profile);
      getApplicationConfigService().removeProperty(moduleName, DATASOURCE_PREFIX, DATABASE_DRIVER,
          profile);
      getApplicationConfigService().removeProperty(moduleName, DATASOURCE_PREFIX,
          DATABASE_USERNAME, profile);
      getApplicationConfigService().removeProperty(moduleName, DATASOURCE_PREFIX,
          DATABASE_PASSWORD, profile);
    }

    // Add Hibernate naming strategy property
    if (ormProvider.toString().equals(OrmProvider.HIBERNATE.toString())) {
      getApplicationConfigService().addProperty(moduleName, HIBERNATE_NAMING_STRATEGY,
          HIBERNATE_NAMING_STRATEGY_VALUE, profile, force);
    }

    // Add dev properties
    getApplicationConfigService().addProperty(moduleName, "spring.jpa.show-sql", "true", "dev",
        true);
    getApplicationConfigService().addProperty(moduleName,
        "spring.jpa.properties.hibernate.format_sql", "true", "dev", true);
    getApplicationConfigService().addProperty(moduleName,
        "spring.jpa.properties.hibernate.generate_statistics", "true", "dev", true);
    getApplicationConfigService().addProperty(moduleName, "logging.level.org.hibernate.stat",
        "DEBUG", "dev", true);
    getApplicationConfigService().addProperty(moduleName,
        "logging.level.com.querydsl.jpa.impl.JPAQuery", "DEBUG", "dev", true);
    getApplicationConfigService().addProperty(moduleName, "logging.pattern.level",
        "%5p - QP:%X{querydsl.parameters} -", "dev", true);
  }

  /**
   * Updates the POM with the dependencies required for the given database and
   * ORM provider, removing any other persistence-related dependencies
   *
   * @param configuration
   * @param ormProvider
   * @param jdbcDatabase
   * @param startersXPath
   * @param profile
   */
  private void updateDependencies(final Pom module, final Element configuration,
      final OrmProvider ormProvider, final JdbcDatabase jdbcDatabase, final String startersXPath,
      final String providersXPath, final String databaseXPath, String profile) {

    final List<Dependency> requiredDependencies = new ArrayList<Dependency>();

    final List<Element> starterDependencies =
        XmlUtils.findElements("/configuration/starter/provider[@id = '" + ormProvider.name()
            + "']/dependencies/dependency", configuration);
    for (final Element dependencyElement : starterDependencies) {
      requiredDependencies.add(new Dependency(dependencyElement));
    }

    // Add database dependencies
    final List<Element> databaseDependencies =
        XmlUtils.findElements(jdbcDatabase.getConfigPrefix() + "/dependencies/dependency",
            configuration);
    for (final Element dependencyElement : databaseDependencies) {
      requiredDependencies.add(new Dependency(dependencyElement));
    }
    if (jdbcDatabase.toString().equals(JdbcDatabase.ORACLE.toString())) {
      LOGGER
          .warning("Oracle drivers aren't in Maven public repositories!! You should include them manually in your local Maven repository.");
    }

    final List<Element> ormDependencies =
        XmlUtils.findElements(ormProvider.getConfigPrefix() + "/dependencies/dependency",
            configuration);
    for (final Element dependencyElement : ormDependencies) {
      requiredDependencies.add(new Dependency(dependencyElement));
    }

    // Hard coded to JPA & Hibernate Validator for now
    final List<Element> jpaDependencies =
        XmlUtils.findElements(
            "/configuration/persistence/provider[@id = 'JPA']/dependencies/dependency",
            configuration);
    for (final Element dependencyElement : jpaDependencies) {
      requiredDependencies.add(new Dependency(dependencyElement));
    }

    final List<Element> springDependencies =
        XmlUtils.findElements("/configuration/spring/dependencies/dependency", configuration);
    for (final Element dependencyElement : springDependencies) {
      requiredDependencies.add(new Dependency(dependencyElement));
    }

    final List<Element> commonDependencies =
        XmlUtils.findElements("/configuration/common/dependencies/dependency", configuration);
    for (final Element dependencyElement : commonDependencies) {
      requiredDependencies.add(new Dependency(dependencyElement));
    }
    // Add properties
    List<Element> properties = XmlUtils.findElements("/configuration/properties/*", configuration);
    for (Element property : properties) {
      getProjectOperations().addProperty("", new Property(property));
    }
    // Add dependencies used by other profiles, excluding the current profile
    List<String> profiles =
        getApplicationConfigService().getApplicationProfiles(module.getModuleName());
    profiles.remove(profile);

    for (String applicationProfile : profiles) {

      // Extract database
      final String driver =
          getApplicationConfigService().getProperty(module.getModuleName(), DATASOURCE_PREFIX,
              DATABASE_DRIVER, applicationProfile);

      for (JdbcDatabase database : JdbcDatabase.values()) {
        if (database.getDriverClassName().equals(driver)) {
          for (final Element dependencyElement : XmlUtils.findElements(database.getConfigPrefix()
              + "/dependencies/dependency", configuration)) {
            requiredDependencies.add(new Dependency(dependencyElement));
          }
          break;
        }
      }
    }

    // Remove redundant dependencies
    final List<Dependency> redundantDependencies = new ArrayList<Dependency>();
    redundantDependencies.addAll(getDependencies(databaseXPath, configuration));
    redundantDependencies.addAll(getDependencies(startersXPath, configuration));
    redundantDependencies.addAll(getDependencies(providersXPath, configuration));

    // Don't remove any we actually need
    redundantDependencies.removeAll(requiredDependencies);

    // Update the POM
    getProjectOperations().removeDependencies(module.getModuleName(), redundantDependencies);
    getProjectOperations().addDependencies(module.getModuleName(), requiredDependencies);

    // Add database test dependency to repository module if it is multimodule project
    // and some repository has been already added
    if (getProjectOperations().isMultimoduleProject()) {
      Set<JavaType> repositoryTypes =
          getTypeLocationService().findTypesWithAnnotation(RooJavaType.ROO_REPOSITORY_JPA);
      if (!repositoryTypes.isEmpty()) {
        Iterator<JavaType> repositoryIterator = repositoryTypes.iterator();
        while (repositoryIterator.hasNext()) {
          JavaType repositoryType = repositoryIterator.next();
          String moduleName = repositoryType.getModule();

          // Remove redundant dependencies from modules with repository classes
          getProjectOperations().removeDependencies(moduleName, redundantDependencies);

          // Add new database dependencies
          addDatabaseDependencyWithTestScope(moduleName, profile, jdbcDatabase.getConfigPrefix());
        }

      }
    }

    // Include Springlets Starter project dependencies and properties
    getProjectOperations().addProperty("", SPRINGLETS_VERSION_PROPERTY);

    // If current project is a multimodule project, include dependencies
    // first
    // on dependencyManagement and then on current module
    getProjectOperations().addDependency(module.getModuleName(), SPRINGLETS_DATA_JPA_STARTER);
    getProjectOperations().addDependency(module.getModuleName(), SPRINGLETS_DATA_COMMONS_STARTER);

  }

  /**
   * Add datasource dependency for testing purposes in a module with
   * repository classes. This method can be called when installing/changing
   * persistence database or when adding repositories to the project.
   *
   * @param repositoryModuleName
   *            the module name where the dependency should be added.
   * @param profile
   *            the profile used to obtain the datasource property from spring
   *            config file.
   * @param databaseConfigPrefix
   *            the database prefix used to find the right dependency in the
   *            configuration file. It could be null if called from repository
   *            commands.
   */
  public void addDatabaseDependencyWithTestScope(String repositoryModuleName, String profile,
      String databaseConfigPrefix) {

    // Get configuration Element from configuration.xml
    final Element configuration = XmlUtils.getConfiguration(getClass());

    // If databaseConfigPrefix is null, get prefix from properties file
    if (databaseConfigPrefix == null) {

      // Get application module where properties file should be located
      List<Pom> modules =
          (List<Pom>) getTypeLocationService().getModules(ModuleFeatureName.APPLICATION);
      if (modules.size() == 0) {
        throw new RuntimeException(String.format("ERROR: Not found a module with %s feature",
            ModuleFeatureName.APPLICATION));
      }

      if (profile == null) {

        // Add the database dependency of each profile
        List<String> profiles =
            getApplicationConfigService().getApplicationProfiles(modules.get(0).getModuleName());

        for (String applicationProfile : profiles) {

          // // Find the driver name to obtain the right dependency to add
          final String driver =
              getApplicationConfigService().getProperty(modules.get(0).getModuleName(),
                  DATASOURCE_PREFIX, DATABASE_DRIVER, applicationProfile);

          for (JdbcDatabase database : JdbcDatabase.values()) {
            if (database.getDriverClassName().equals(driver)) {
              databaseConfigPrefix = database.getConfigPrefix();
              addTestScopedDependency(repositoryModuleName, databaseConfigPrefix, configuration);
            }
          }
        }
      } else {

        // Find the driver name to obtain the right dependency to add
        String driver =
            getApplicationConfigService().getProperty(modules.get(0).getModuleName(),
                DATASOURCE_PREFIX, DATABASE_DRIVER, profile);

        // Find the prefix value from JdbcDatabase enum
        JdbcDatabase[] jdbcDatabaseValues = JdbcDatabase.values();
        for (JdbcDatabase database : jdbcDatabaseValues) {
          if (database.getDriverClassName().equals(driver)) {
            databaseConfigPrefix = database.getConfigPrefix();
            addTestScopedDependency(repositoryModuleName, databaseConfigPrefix, configuration);
          }
        }
      }
    } else {

      // No need to find the driver name to obtain database prefix
      addTestScopedDependency(repositoryModuleName, databaseConfigPrefix, configuration);
    }
  }

  /**
   * Gets database dependency from config file and adds it with test scope
   *
   * @param moduleName
   *            the module which dependency should be added
   * @param databaseConfigPrefix
   *            the prefix name for choosing the dependency to add
   * @param configuration
   *            the configuration file with the dependencies to copy from
   */
  private void addTestScopedDependency(String moduleName, String databaseConfigPrefix,
      final Element configuration) {
    final List<Element> databaseDependencies =
        XmlUtils.findElements(databaseConfigPrefix + "/dependencies/dependency", configuration);
    for (final Element dependencyElement : databaseDependencies) {

      // Change scope from provided to test
      NodeList childNodes = dependencyElement.getChildNodes();
      for (int i = 0; i < childNodes.getLength(); i++) {
        final Node node = childNodes.item(i);
        if (node != null && node.getNodeType() == Node.ELEMENT_NODE
            && node.getNodeName().equals("scope")) {
          node.setTextContent("test");
        }
      }

      // Add dependency
      getProjectOperations().addDependency(moduleName, new Dependency(dependencyElement));
    }
  }

  private List<Dependency> getDependencies(final String xPathExpression, final Element configuration) {

    final List<Dependency> dependencies = new ArrayList<Dependency>();
    for (final Element dependencyElement : XmlUtils.findElements(xPathExpression
        + "/dependencies/dependency", configuration)) {
      final Dependency dependency = new Dependency(dependencyElement);
      dependencies.add(dependency);
    }
    return dependencies;
  }

  @Override
  public void deleteEntity(JavaType entity) {
    final String entityFilePathIdentifier =
        getPathResolver().getCanonicalPath(entity.getModule(), Path.SRC_MAIN_JAVA, entity);

    if (getFileManager().exists(entityFilePathIdentifier)) {
      getFileManager().delete(entityFilePathIdentifier);
    }
  }

  @Override
  public Pair<FieldMetadata, RelationInfo> getFieldChildPartOfCompositionRelation(
      ClassOrInterfaceTypeDetails entityCdi) {
    JavaType domainType = entityCdi.getType();
    List<Pair<FieldMetadata, RelationInfo>> relations = getFieldChildPartOfRelation(entityCdi);
    if (relations.isEmpty()) {
      return null;
    }
    JpaEntityMetadata parent;
    JavaType parentType;
    RelationInfo info;
    List<Pair<FieldMetadata, RelationInfo>> compositionRelation =
        new ArrayList<Pair<FieldMetadata, RelationInfo>>();
    for (Pair<FieldMetadata, RelationInfo> field : relations) {
      if (field.getRight().type == JpaRelationType.COMPOSITION) {
        compositionRelation.add(field);
      }
    }
    Validate.isTrue(compositionRelation.size() <= 1,
        "Entity %s has more than one relations of composition as child part: ", domainType,
        StringUtils.join(getFieldNamesOfRelationList(compositionRelation), ","));
    if (compositionRelation.isEmpty()) {
      return null;
    }
    return compositionRelation.get(0);
  }


  private List<String> getFieldNamesOfRelationList(
      List<Pair<FieldMetadata, RelationInfo>> compositionRelation) {
    List<String> names = new ArrayList<String>(compositionRelation.size());
    for (Pair<FieldMetadata, RelationInfo> pair : compositionRelation) {
      names.add(pair.getLeft().getFieldName().getSymbolName());
    }
    return names;
  }

  @Override
  public List<Pair<FieldMetadata, RelationInfo>> getFieldChildPartOfRelation(JavaType entity) {
    ClassOrInterfaceTypeDetails entityDetails = getTypeLocationService().getTypeDetails(entity);
    return getFieldChildPartOfRelation(entityDetails);
  }

  @Override
  public List<Pair<FieldMetadata, RelationInfo>> getFieldChildPartOfRelation(
      ClassOrInterfaceTypeDetails entityCdi) {
    JavaType domainType = entityCdi.getType();
    JpaEntityMetadata entityMetadata = getJpaEntityMetadata(entityCdi);
    Validate.notNull(entityMetadata, "%s should be a Jpa Entity", domainType);
    Map<String, FieldMetadata> relations = entityMetadata.getRelationsAsChild();
    List<Pair<FieldMetadata, RelationInfo>> childRelations =
        new ArrayList<Pair<FieldMetadata, RelationInfo>>();
    JpaEntityMetadata parent;
    JavaType parentType;
    RelationInfo info;
    for (Entry<String, FieldMetadata> fieldEntry : relations.entrySet()) {
      parentType = fieldEntry.getValue().getFieldType().getBaseType();
      parent = getJpaEntityMetadata(parentType);
      Validate.notNull(parent,
          "Can't get information about Entity %s which is declared as parent in %s.%s field",
          parentType, domainType, fieldEntry.getKey());
      info = parent.getRelationInfosByMappedBy(domainType, fieldEntry.getKey());
      if (info != null) {
        childRelations.add(Pair.of(fieldEntry.getValue(), info));
      }
    }
    return childRelations;
  }

  @Override
  public Pair<FieldMetadata, RelationInfo> getFieldChildPartOfCompositionRelation(JavaType entity) {
    return getFieldChildPartOfCompositionRelation(getTypeLocationService().getTypeDetails(entity));
  }

  /**
   * This method generates the identifier field using the provided values.
   * 
   * @param entity
   * @param identifierField
   * @param identifierType
   * @param identifierColumn
   * @param sequenceName
   * @param identifierStrategy
   * @param inheritanceType
   * @return
   */
  private FieldMetadata getIdentifierField(final JavaType entity, String identifierField,
      final JavaType identifierType, final String identifierColumn, final String sequenceName,
      IdentifierStrategy identifierStrategy, InheritanceType inheritanceType) {

    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    final boolean hasIdClass = !(identifierType.isCoreType());
    final JavaType annotationType = hasIdClass ? EMBEDDED_ID : ID;
    annotations.add(new AnnotationMetadataBuilder(annotationType));

    if (StringUtils.isEmpty(identifierField)) {
      identifierField = "id";
    }

    // Compute the column name, as required
    if (!hasIdClass) {
      if (!"".equals(sequenceName)) {

        // ROO-3719: Add SEQUENCE as @GeneratedValue strategy
        // Check if provided identifierStrategy is valid
        boolean isValidIdentifierStrategy = false;
        if (identifierStrategy != null) {
          for (IdentifierStrategy identifierStrategyType : IdentifierStrategy.values()) {
            if (identifierStrategyType.name().equals(identifierStrategy.name())) {
              isValidIdentifierStrategy = true;
              break;
            }
          }
        }

        if (!isValidIdentifierStrategy) {
          identifierStrategy = IdentifierStrategy.AUTO;
        }

        // ROO-746: Use @GeneratedValue(strategy = GenerationType.TABLE)
        // If the root of the governor declares @Inheritance(strategy =
        // InheritanceType.TABLE_PER_CLASS)
        if (IdentifierStrategy.AUTO.name().equals(identifierStrategy.name())) {
          if (inheritanceType != null) {
            if ("TABLE_PER_CLASS".equals(inheritanceType.name())) {
              identifierStrategy = IdentifierStrategy.TABLE;
            }
          }
        }

        final AnnotationMetadataBuilder generatedValueBuilder =
            new AnnotationMetadataBuilder(GENERATED_VALUE);
        generatedValueBuilder.addEnumAttribute("strategy", new EnumDetails(GENERATION_TYPE,
            new JavaSymbolName(identifierStrategy.name())));

        if (StringUtils.isNotBlank(sequenceName)) {
          final String sequenceKey = StringUtils.uncapitalize(entity.getSimpleTypeName()) + "Gen";
          generatedValueBuilder.addStringAttribute("generator", sequenceKey);
          final AnnotationMetadataBuilder sequenceGeneratorBuilder =
              new AnnotationMetadataBuilder(SEQUENCE_GENERATOR);
          sequenceGeneratorBuilder.addStringAttribute("name", sequenceKey);
          sequenceGeneratorBuilder.addStringAttribute("sequenceName", sequenceName);
          annotations.add(sequenceGeneratorBuilder);
        }
        annotations.add(generatedValueBuilder);
      }

      // User has specified alternative columnName
      if (StringUtils.isNotBlank(identifierColumn)) {
        final AnnotationMetadataBuilder columnBuilder = new AnnotationMetadataBuilder(COLUMN);
        columnBuilder.addStringAttribute("name", identifierColumn);
        annotations.add(columnBuilder);
      }
    }

    FieldDetails identifierFieldDetails =
        new FieldDetails(getTypeLocationService().getPhysicalTypeIdentifier(entity),
            identifierType, new JavaSymbolName(identifierField));
    identifierFieldDetails.setModifiers(Modifier.PRIVATE);
    identifierFieldDetails.addAnnotations(annotations);

    return new FieldMetadataBuilder(identifierFieldDetails).build();
  }

  /**
   * This method generates the version field using the provided values
   * 
   * @param entity
   * @param versionField
   * @param versionType
   * @param versionColumn
   * @return
   */
  private FieldMetadata getVersionField(final JavaType entity, String versionField,
      final JavaType versionType, final String versionColumn) {

    if (StringUtils.isEmpty(versionField)) {
      versionField = "version";
    }

    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    annotations.add(new AnnotationMetadataBuilder(VERSION));

    if (StringUtils.isNotEmpty(versionColumn)) {
      final AnnotationMetadataBuilder columnBuilder = new AnnotationMetadataBuilder(COLUMN);
      columnBuilder.addStringAttribute("name", versionColumn);
      annotations.add(columnBuilder);
    }

    FieldDetails versionFieldDetails =
        new FieldDetails(getTypeLocationService().getPhysicalTypeIdentifier(entity), versionType,
            new JavaSymbolName(versionField));
    versionFieldDetails.setModifiers(Modifier.PRIVATE);
    versionFieldDetails.addAnnotations(annotations);

    return new FieldMetadataBuilder(versionFieldDetails).build();
  }


  /**
   * Gets JpaEntityMetadata by a javaType
   * @param domainType
   * @return
   */
  private JpaEntityMetadata getJpaEntityMetadata(JavaType domainType) {
    ClassOrInterfaceTypeDetails entityDetails = getTypeLocationService().getTypeDetails(domainType);
    return getJpaEntityMetadata(entityDetails);
  }

  /**
   * Gets JpaEntityMetadata by a javaType
   * @param domainType
   * @return
   */
  private JpaEntityMetadata getJpaEntityMetadata(ClassOrInterfaceTypeDetails domainTypeDetails) {
    final String entityMetadataId = JpaEntityMetadata.createIdentifier(domainTypeDetails);
    JpaEntityMetadata entityMetadata = getMetadataService().get(entityMetadataId);
    return entityMetadata;
  }


  private MetadataService getMetadataService() {
    return serviceManager.getServiceInstance(this, MetadataService.class);
  }

  private FileManager getFileManager() {
    return serviceManager.getServiceInstance(this, FileManager.class);
  }

  private PathResolver getPathResolver() {
    return serviceManager.getServiceInstance(this, PathResolver.class);
  }

  private ProjectOperations getProjectOperations() {
    return serviceManager.getServiceInstance(this, ProjectOperations.class);
  }

  private TypeLocationService getTypeLocationService() {
    return serviceManager.getServiceInstance(this, TypeLocationService.class);
  }

  private TypeManagementService getTypeManagementService() {
    return serviceManager.getServiceInstance(this, TypeManagementService.class);
  }

  private ApplicationConfigService getApplicationConfigService() {
    return serviceManager.getServiceInstance(this, ApplicationConfigService.class);
  }

  /**
   * FEATURE Methods
   */

  public boolean isInstalledInModule(final String moduleName) {

    Pom pom = getProjectOperations().getPomFromModuleName(moduleName);
    if (pom == null) {
      return false;
    }

    // Check if spring-boot-starter-data-jpa has been included
    Set<Dependency> dependencies = pom.getDependencies();
    Dependency starter =
        new Dependency("org.springframework.boot", "spring-boot-starter-data-jpa", "");

    boolean hasStarter = dependencies.contains(starter);

    // Check existing application profiles
    boolean existsSpringConfigProfileInModule = false;
    List<String> applicationProfiles =
        getApplicationConfigService().getApplicationProfiles(moduleName);
    for (String profile : applicationProfiles) {
      if (getApplicationConfigService().existsSpringConfigFile(moduleName, profile)) {
        existsSpringConfigProfileInModule = true;
        break;
      }
    }

    return existsSpringConfigProfileInModule && hasStarter;
  }

  public String getName() {
    return FeatureNames.JPA;
  }

}
