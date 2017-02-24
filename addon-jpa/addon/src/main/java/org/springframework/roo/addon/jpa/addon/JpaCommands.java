package org.springframework.roo.addon.jpa.addon;

import static org.springframework.roo.model.RooJavaType.ROO_EQUALS;
import static org.springframework.roo.model.RooJavaType.ROO_JAVA_BEAN;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ENTITY;
import static org.springframework.roo.model.RooJavaType.ROO_PLURAL;
import static org.springframework.roo.model.RooJavaType.ROO_SERIALIZABLE;
import static org.springframework.roo.model.RooJavaType.ROO_TO_STRING;
import static org.springframework.roo.shell.OptionContexts.APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE;
import static org.springframework.roo.shell.OptionContexts.INTERFACE;
import static org.springframework.roo.shell.OptionContexts.SUPERCLASS;
import static org.springframework.roo.shell.OptionContexts.UPDATELAST_PROJECT;
import static org.springframework.roo.shell.OptionContexts.UPDATE_PROJECT;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.jpa.addon.entity.IdentifierStrategy;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.operations.InheritanceType;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.settings.project.ProjectSettingsService;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionAutocompleteIndicator;
import org.springframework.roo.shell.CliOptionMandatoryIndicator;
import org.springframework.roo.shell.CliOptionVisibilityIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.shell.converters.StaticFieldConverter;
import org.springframework.roo.support.logging.HandlerUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Commands for the JPA add-on to be used by the ROO shell.
 *
 * @author Stefan Schmidt
 * @author Ben Alex
 * @author Alan Stewart
 * @author Juan Carlos García
 * @author Sergio Clares
 * @since 1.0
 */
@Component
@Service
public class JpaCommands implements CommandMarker {

  private static Logger LOGGER = HandlerUtils.getLogger(JpaCommands.class);

  // Project Settings
  private static final String SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME =
      "spring.roo.jpa.require.schema-object-name";

  // Annotations
  private static final AnnotationMetadataBuilder ROO_EQUALS_BUILDER =
      new AnnotationMetadataBuilder(ROO_EQUALS);

  private static final AnnotationMetadataBuilder ROO_SERIALIZABLE_BUILDER =
      new AnnotationMetadataBuilder(ROO_SERIALIZABLE);

  private static final AnnotationMetadataBuilder ROO_TO_STRING_BUILDER =
      new AnnotationMetadataBuilder(ROO_TO_STRING);

  private static final String IDENTIFIER_DEFAULT_TYPE = "java.lang.Long";

  private static final String VERSION_DEFAULT_TYPE = "java.lang.Integer";

  @Reference
  private JpaOperations jpaOperations;

  @Reference
  private ProjectOperations projectOperations;

  @Reference
  private PropFileOperations propFileOperations;

  @Reference
  private StaticFieldConverter staticFieldConverter;

  @Reference
  private TypeLocationService typeLocationService;

  @Reference
  private ProjectSettingsService projectSettings;

  @Reference
  private PathResolver pathResolver;

  @Reference
  private FileManager fileManager;

  protected void activate(final ComponentContext context) {
    staticFieldConverter.add(JdbcDatabase.class);
    staticFieldConverter.add(OrmProvider.class);
  }

  protected void deactivate(final ComponentContext context) {
    staticFieldConverter.remove(JdbcDatabase.class);
    staticFieldConverter.remove(OrmProvider.class);
  }

  @CliAvailabilityIndicator({"jpa setup"})
  public boolean isJpaSetupAvailable() {
    return jpaOperations.isJpaInstallationPossible();
  }

  @CliAvailabilityIndicator({"entity jpa", "embeddable"})
  public boolean isClassGenerationAvailable() {
    return jpaOperations.isJpaInstalled();
  }

  @CliCommand(
      value = "embeddable",
      help = "Creates a new Java class source file with the JPA `@Embeddable` annotation in the directory _src/main/java_ of the selected project module (if any).")
  public void createEmbeddableClass(
      @CliOption(key = "class", optionContext = UPDATE_PROJECT, mandatory = true,
          help = "The name of the embeddable class to create. If you consider it "
              + "necessary, you can also specify the package (base package can be "
              + "specified with `~`). Ex.: `--class ~.domain.MyEmbeddableClass`. "
              + "You can specify module as well, if necessary. "
              + "Ex.: `--class model:~.domain.MyEmbeddableClass`. When working "
              + "with a multi-module project, if module is not specified the class "
              + "will be created in the module which has the focus.") final JavaType name,
      @CliOption(key = "serializable", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether the generated class should implement `java.io.Serializable`. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean serializable,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean permitReservedWords) {

    if (!permitReservedWords) {
      ReservedWords.verifyReservedWordsNotPresent(name);
    }

    jpaOperations.newEmbeddableClass(name, serializable);
  }

  @CliOptionVisibilityIndicator(command = "jpa setup", params = {"jndiDataSource"},
      help = "`--jndiDataSource` parameter is not available if any of `--database`, "
          + "`--databaseName`, `--hostName`, `--password` or `--userName` are "
          + "present or if you are using an HYPERSONIC database.")
  public boolean isJndiVisible(ShellContext shellContext) {

    Map<String, String> params = shellContext.getParameters();

    // If user define databaseName, hostName, password or username parameters,
    // jndiDataSource
    // should not be visible.
    if (params.containsKey("database") || params.containsKey("databaseName")
        || params.containsKey("hostName") || params.containsKey("password")
        || params.containsKey("userName")) {
      return false;
    }

    return true;
  }

  @CliOptionVisibilityIndicator(command = "jpa setup", params = {"databaseName", "hostName",
      "password", "userName"},
      help = "Connection parameters are not available if jndiDatasource is "
          + "specified or if you are using an HYPERSONIC database.")
  public boolean areConnectionParamsVisible(ShellContext shellContext) {

    Map<String, String> params = shellContext.getParameters();

    // If parameter database is not defined, all parameters are not visible
    String database = params.get("database");
    if (database == null) {
      return false;
    }

    // If uses some memory databases or file databases, jndiDataSource parameter
    // should not be visible.
    if (database.startsWith("HYPERSONIC") || database.equals("H2_IN_MEMORY")) {
      return false;
    }

    // If user define jndiDatasource parameter, connection parameters should not
    // be visible
    if (params.containsKey("jndiDataSource")) {
      return false;
    }

    return true;
  }

  @CliOptionVisibilityIndicator(command = "jpa setup", params = "database",
      help = "'--database' option is not available if '--jndiDatasource' " + "is specified.")
  public boolean areProviderAndDatabaseVisible(ShellContext shellContext) {

    Map<String, String> params = shellContext.getParameters();

    // If user define jndiDatasource parameter, database should not be visible
    if (params.containsKey("jndiDataSource")) {
      return false;
    }

    return true;
  }

  @CliOptionVisibilityIndicator(command = "jpa setup", params = {"module"},
      help = "Module parameter is not available if there is only one application module")
  public boolean isModuleVisible(ShellContext shellContext) {
    if (typeLocationService.getModuleNames(ModuleFeatureName.APPLICATION).size() > 1) {
      return true;
    }
    return false;
  }

  @CliOptionMandatoryIndicator(params = "module", command = "jpa setup")
  public boolean isModuleRequired(ShellContext shellContext) {
    Pom module = projectOperations.getFocusedModule();
    if (!isModuleVisible(shellContext)
        || typeLocationService.hasModuleFeature(module, ModuleFeatureName.APPLICATION)) {
      return false;
    }
    return true;
  }

  @CliCommand(
      value = "jpa setup",
      help = "Installs or updates a JPA persistence provider in your project. User can execute this "
          + "command for diferent profiles with different persistence configurations.")
  public void installJpa(
      @CliOption(key = "provider", mandatory = true,
          help = "The persistence ORM provider to support. "
              + "Possible values are: `ECLIPSELINK` and `HIBERNATE`. "
              + "This option is available only if `--jndiDataSource` has not been specified. "
              + "This option is mandatory if `--jndiDataSource` has not been specified.") final OrmProvider ormProvider,
      @CliOption(
          key = "database",
          mandatory = false,
          help = "The database type to support."
              + "Possible values are: `DB2_400`, `DB2_EXPRESS_C`, `DERBY_CLIENT`, `DERBY_EMBEDDED`, "
              + "`FIREBIRD`, `H2_IN_MEMORY`, `HYPERSONIC_IN_MEMORY`, `HYPERSONIC_PERSISTENT`, `MSSQL`, "
              + "`MYSQL`, `ORACLE`, `POSTGRES` and `SYBASE`. "
              + "This option is mandatory if `--jndiDataSource` has not been specified. "
              + "This option is available only if `--jndiDataSource` has not been specified.") final JdbcDatabase jdbcDatabase,
      @CliOption(
          key = "module",
          mandatory = true,
          help = "The application module where to install the persistence. "
              + "This option is mandatory if the focus is not set in an application module, that is, a "
              + "module containing an `@SpringBootApplication` class. "
              + "This option is available only if there are more than one application module and none "
              + "of them is focused. "
              + "Default if option not present: the unique 'application' module, or focused 'application'"
              + " module.", unspecifiedDefaultValue = ".",
          optionContext = APPLICATION_FEATURE_INCLUDE_CURRENT_MODULE) Pom module,
      @CliOption(key = "jndiDataSource", mandatory = false, help = "The JNDI datasource to use. "
          + "This option is not available if any of `--provider`, `--database`, `--databaseName`, "
          + "`--hostName`, `--password` or `--userName` options are specified.") final String jndi,
      @CliOption(key = "hostName", mandatory = false, help = "The host name to use. "
          + "This option is available if `--database` has already been specified and its value is"
          + " not `HYPERSONIC` or `H2_IN_MEMORY` and `--jndiDatasource` has not been specified.") final String hostName,
      @CliOption(key = "databaseName", mandatory = false, help = "The database name to use. "
          + "This option is available if `--database` has already been specified and its value is"
          + " not `HYPERSONIC` or `H2_IN_MEMORY` and `--jndiDatasource` has not been specified.") final String databaseName,
      @CliOption(key = "userName", mandatory = false, help = "The username to use. "
          + "This option is available if `--database` has already been specified and its value is"
          + " not `HYPERSONIC` or `H2_IN_MEMORY` and `--jndiDatasource` has not been specified.") final String userName,
      @CliOption(key = "password", mandatory = false, help = "The password to use. "
          + "This option is available if `--database` has already been specified and its value is"
          + " not `HYPERSONIC` or `H2_IN_MEMORY` and `--jndiDatasource` has not been specified.") final String password,
      ShellContext shellContext) {

    if (jdbcDatabase != null && jdbcDatabase == JdbcDatabase.FIREBIRD && !isJdk6OrHigher()) {
      LOGGER.warning("JDK must be 1.6 or higher to use Firebird");
      return;
    }

    jpaOperations.configureJpa(ormProvider, jdbcDatabase, module, jndi, hostName, databaseName,
        userName, password, shellContext.getProfile(), shellContext.isForce());
  }

  /**
   * Indicator that checks if versionField param has been specified and makes
   * its associate params visible
   *
   * @param shellContext
   * @return true if versionField param has been specified.
   */
  @CliOptionVisibilityIndicator(
      command = "entity jpa",
      params = {"versionType", "versionColumn"},
      help = "Options --versionType and --versionColumn must be used with the --versionField option.")
  public boolean areVersionParamsVisibleForEntityJpa(ShellContext shellContext) {

    String versionFieldParam = shellContext.getParameters().get("versionField");

    if (versionFieldParam != null) {
      return true;
    }

    return false;
  }

  /**
   * ROO-3709: Indicator that checks if exists some project setting that makes
   * each of the following parameters mandatory: sequenceName, identifierColumn,
   * identifierStrategy, versionField, versionColumn, versionType and table.
   *
   * @param shellContext
   * @return true if exists property
   *         {@link #SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME} on project
   *         settings and its value is "true". If not, return false.
   */
  @CliOptionMandatoryIndicator(params = {"sequenceName", "identifierStrategy", "identifierColumn",
      "table", "versionField", "versionColumn", "versionType"}, command = "entity jpa")
  public boolean areSchemaObjectNamesRequired(ShellContext shellContext) {

    // Check if property 'spring.roo.jpa.require.schema-object-name' is defined
    // on
    // project settings
    String requiredSchemaObjectName =
        projectSettings.getProperty(SPRING_ROO_JPA_REQUIRE_SCHEMA_OBJECT_NAME);

    if (requiredSchemaObjectName != null && requiredSchemaObjectName.equals("true")) {
      return true;
    }

    return false;
  }

  /**
   * Indicator that provides all possible values for --class parameter The
   * provided results will not be validate. It will not include space on finish.
   *
   * @param shellContext
   * @return List with all possible values for --class parameter
   */
  @CliOptionAutocompleteIndicator(command = "entity jpa", param = "class",
      help = "Provided --class option should be a class annotated with @RooJpaEntity.",
      validate = false, includeSpaceOnFinish = false)
  public List<String> getClassPossibleValues(ShellContext shellContext) {
    List<String> allPossibleValues = new ArrayList<String>();

    // Add all modules to completions list
    if (projectOperations.isMultimoduleProject()) {
      Collection<String> modules = projectOperations.getModuleNames();
      for (String module : modules) {

        // Ignore root module
        if (StringUtils.isBlank(module)) {
          continue;
        }

        // Ignore module name if it is the focused module
        if (module.equals(projectOperations.getFocusedModule().getModuleName())) {
          List<JavaPackage> modulePackages = typeLocationService.getPackagesForModule(module);

          // Always add module top level package and project top level package
          modulePackages.add(projectOperations.getTopLevelPackage(module));
          for (JavaPackage javaPackage : modulePackages) {

            // Check if package name contains top level package to shorten it
            String currentPackageName =
                getPackageStringValue(module, javaPackage.getFullyQualifiedPackageName());

            // Add package to possible values
            if (!allPossibleValues.contains(currentPackageName.concat("."))) {
              allPossibleValues.add(currentPackageName.concat("."));
            }
          }
        } else {

          // It is not the focused module
          List<JavaPackage> modulePackages = typeLocationService.getPackagesForModule(module);

          // Always add module top level package and project top level package
          modulePackages.add(projectOperations.getTopLevelPackage(module));
          for (JavaPackage javaPackage : modulePackages) {

            // Check if package name contains top level package to shorten it
            String currentPackageName =
                getPackageStringValue(module, javaPackage.getFullyQualifiedPackageName());

            // Add package to possible values
            String valueToAdd =
                String.format("%s%s%s.", module, LogicalPath.MODULE_PATH_SEPARATOR,
                    currentPackageName);
            if (!allPossibleValues.contains(valueToAdd)) {
              allPossibleValues.add(valueToAdd);
            }
          }
        }
      }
    } else {

      // Check all JavaPackages in single module project
      for (JavaPackage javaPackage : typeLocationService.getPackagesForModule("")) {

        // Check if package name contains top level package to shorten it
        String currentPackageName =
            getPackageStringValue("", javaPackage.getFullyQualifiedPackageName());

        // Add package to possible values
        if (!allPossibleValues.contains(currentPackageName.concat("."))) {
          allPossibleValues.add(currentPackageName.concat("."));
        }
      }
    }

    return allPossibleValues;
  }

  @CliOptionAutocompleteIndicator(
      command = "entity jpa",
      param = "identifierType",
      help = "--identifierType option should be a wrapper of a primitive type or an embeddable class.")
  public List<String> getIdentifierTypePossibleValues(ShellContext shellContext) {
    String currentText = shellContext.getParameters().get("identifierType");
    List<String> allPossibleValues = new ArrayList<String>();

    // Add java-lang and java-number classes
    allPossibleValues.add(Number.class.getName());
    allPossibleValues.add(Short.class.getName());
    allPossibleValues.add(Byte.class.getName());
    allPossibleValues.add(Integer.class.getName());
    allPossibleValues.add(Long.class.getName());
    allPossibleValues.add(Float.class.getName());
    allPossibleValues.add(Double.class.getName());
    allPossibleValues.add(BigDecimal.class.getName());
    allPossibleValues.add(BigInteger.class.getName());

    // Getting all existing embeddable classes
    Set<ClassOrInterfaceTypeDetails> embeddableClassesInProject =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(JpaJavaType.EMBEDDABLE);
    for (ClassOrInterfaceTypeDetails embeddableClass : embeddableClassesInProject) {
      String name = replaceTopLevelPackageString(embeddableClass, currentText);
      if (!allPossibleValues.contains(name)) {
        allPossibleValues.add(name);
      }
    }

    return allPossibleValues;
  }

  @CliOptionAutocompleteIndicator(command = "entity jpa", param = "versionType",
      help = "--versionType option should be a wrapper of a primitive type.")
  public List<String> getVersionTypePossibleValues(ShellContext shellContext) {
    List<String> allPossibleValues = new ArrayList<String>();

    // Add java-lang and java-number classes
    allPossibleValues.add(Number.class.getName());
    allPossibleValues.add(Short.class.getName());
    allPossibleValues.add(Byte.class.getName());
    allPossibleValues.add(Integer.class.getName());
    allPossibleValues.add(Long.class.getName());
    allPossibleValues.add(Float.class.getName());
    allPossibleValues.add(Double.class.getName());
    allPossibleValues.add(BigDecimal.class.getName());
    allPossibleValues.add(BigInteger.class.getName());

    return allPossibleValues;
  }

  @CliCommand(
      value = "entity jpa",
      help = "Creates a new JPA persistent entity in the directory _src/main/java_ of the selected project module (if any) with `@RooEntity` annotation.")
  public void newPersistenceClassJpa(
      @CliOption(
          key = "class",
          optionContext = UPDATELAST_PROJECT,
          mandatory = true,
          help = "The name of the entity to create. If you consider it necessary, you can also "
              + "specify the package (base package can be specified with `~`). "
              + "Ex.: `--class ~.domain.MyEntity`. You can specify module as well, if necessary. "
              + "Ex.: `--class model:~.domain.MyEntity`. When working with a multi-module project, "
              + "if module is not specified the entity will be created in the module which has the focus.") final JavaType name,
      @CliOption(
          key = "table",
          mandatory = true,
          help = "The JPA table name to use for this entity. "
              + "This option is mandatory if `spring.roo.jpa.require.schema-object-name` configuration setting it’s `true`.") final String table,
      @CliOption(
          key = "identifierColumn",
          mandatory = true,
          help = "The JPA identifier field column to use for this entity. "
              + "This option is mandatory if `spring.roo.jpa.require.schema-object-name` configuration setting it’s `true`.") final String identifierColumn,
      @CliOption(
          key = "versionField",
          mandatory = true,
          help = "The JPA version field name to use for this entity. "
              + "This option is mandatory if `spring.roo.jpa.require.schema-object-name` configuration setting it’s `true`.") final String versionField,
      @CliOption(
          key = "versionColumn",
          mandatory = true,
          help = "The JPA version field column to use for this entity. "
              + "This option is available if 'versionField' option is set."
              + " This option is mandatory if `spring.roo.jpa.require.schema-object-name` configuration setting it’s `true`.") final String versionColumn,
      @CliOption(
          key = "versionType",
          mandatory = true,
          optionContext = "java-lang,project",
          unspecifiedDefaultValue = VERSION_DEFAULT_TYPE,
          help = "The data type that will be used for the JPA version field. "
              + "This option is available if 'versionField' option is set."
              + " This option is mandatory if `spring.roo.jpa.require.schema-object-name` configuration setting it’s `true`.") final JavaType versionType,
      @CliOption(
          key = "sequenceName",
          mandatory = true,
          help = "The name of the sequence for incrementing sequence-driven primary keys."
              + " This option is mandatory if `spring.roo.jpa.require.schema-object-name` configuration setting it’s `true`.") final String sequenceName,
      @CliOption(
          key = "identifierStrategy",
          mandatory = true,
          specifiedDefaultValue = "AUTO",
          help = "The generation value strategy to be used."
              + " This option is mandatory if `spring.roo.jpa.require.schema-object-name` configuration setting it’s `true`. "
              + "Default if option present: `AUTO`.") final IdentifierStrategy identifierStrategy,
      @CliOption(key = "extends", mandatory = false, unspecifiedDefaultValue = "java.lang.Object",
          optionContext = SUPERCLASS, help = "The fully qualified name of the superclass. "
              + "Default if option not present: `java.lang.Object`.") final JavaType superclass,
      @CliOption(key = "implements", mandatory = false, optionContext = INTERFACE,
          help = "The fully qualified name of the interface to implement.") final JavaType implementsType,
      @CliOption(key = "abstract", mandatory = false, specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Whether the generated class should be marked as abstract. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean createAbstract,
      @CliOption(key = "schema", mandatory = false,
          help = "The JPA table schema name to use for this entity.") final String schema,
      @CliOption(key = "catalog", mandatory = false,
          help = "The JPA table catalog name to use for this entity.") final String catalog,
      @CliOption(key = "identifierField", mandatory = false,
          help = "The JPA identifier field name to use for this entity.") final String identifierField,
      @CliOption(key = "identifierType", mandatory = false, optionContext = "java-lang,project",
          unspecifiedDefaultValue = IDENTIFIER_DEFAULT_TYPE,
          specifiedDefaultValue = "java.lang.Long",
          help = "The data type that will be used for the JPA identifier field. "
              + "Default: `java.lang.Long`.") final JavaType identifierType,
      @CliOption(key = "inheritanceType", mandatory = false,
          help = "The JPA @Inheritance value (apply to base class)") final InheritanceType inheritanceType,
      @CliOption(key = "mappedSuperclass", mandatory = false, specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false", help = "Apply @MappedSuperclass for this entity. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean mappedSuperclass,
      @CliOption(key = "serializable", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether the generated class should implement `java.io.Serializable`."
              + "Default if option present: `true`; default if option not present: `false`.") final boolean serializable,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean permitReservedWords,
      @CliOption(key = "entityName", mandatory = false,
          help = "The name used to refer to the entity in queries.") final String entityName,
      @CliOption(key = "readOnly", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Whether the generated entity should be used for read operations only. "
              + "Default if option present: `true`; default if option not present `false`.") final boolean readOnly,
      @CliOption(
          key = "plural",
          mandatory = false,
          help = "Specify the plural of this new entity. If not provided, a calculated plural will be used by default.") String plural,
      @CliOption(
          key = "entityFormatExpression",
          mandatory = false,
          help = "The SpEL expression used to format the entity when showing it in presentation layer e.g. "
              + "`{#fieldA} {#fieldB}`. It adds the `value` attribute to `io.springlets.format.EntityFormat` "
              + "annotation.") String formatExpression,
      @CliOption(
          key = "entityFormatMessage",
          mandatory = false,
          help = "The message key used to obtain a localized SpEL expression to format the entity when "
              + "showing it in presentation layer. It adds the `message` attribute to "
              + "`io.springlets.format.EntityFormat` annotation and creates a message in all message bundles "
              + "with the provided key. Message value should be  modified by developer. This kind of format "
              + "has more priority that 'expression' format added with `--entityFormatExpression`.") String formatMessage,
      ShellContext shellContext) {

    Validate.isTrue(!identifierType.isPrimitive(), "Identifier type cannot be a primitive");

    // Check if exists other entity with the same name
    final String entityFilePathIdentifier =
        pathResolver.getCanonicalPath(name.getModule(), Path.SRC_MAIN_JAVA, name);

    if (fileManager.exists(entityFilePathIdentifier) && shellContext.isForce()) {
      fileManager.delete(entityFilePathIdentifier);
    } else if (fileManager.exists(entityFilePathIdentifier) && !shellContext.isForce()) {
      throw new IllegalArgumentException(
          String
              .format(
                  "Entity '%s' already exists and cannot be created. Try to use a "
                      + "different entity name on --class parameter or use --force parameter to overwrite it.",
                  name));
    }

    // Check valid value for --extends
    if (superclass == null && shellContext.getParameters().get("extends").equals("")) {
      throw new IllegalArgumentException(
          "Option --extends must have a value when specified. Please, assign it a value.");
    }

    // ROO-3723: Add warning when using --extends with incompatible parameters
    if (superclass != null && !("java.lang.Object").equals(superclass.getFullyQualifiedTypeName())
        && !shellContext.isForce()) {
      this.checkExtendsOverride(identifierColumn, identifierField, identifierStrategy,
          identifierType, sequenceName, versionColumn, versionField, versionType);
    }

    // ROO-3764: Check reserved words only if doesn't permit reserved words
    // and table name has not been specified
    if (!permitReservedWords && StringUtils.isBlank(table)) {
      // Use try to catch exception and show custom message for this situation
      try {
        ReservedWords.verifyReservedWordsNotPresent(name);
      } catch (IllegalStateException exception) {
        LOGGER.log(Level.INFO,
            "ERROR: You are trying to use a reserved word as entity name. You have the following options:\n"
                + "1. Change provided entity name.\n"
                + "2. Specify a valid table name using --table parameter.\n"
                + "3. Use parameter --permitReservedWords to force use of reserved words.\n");
        return;
      }
    } else if (!permitReservedWords && StringUtils.isNotBlank(table)) {
      // If table name has been specified but doesn't permit reserved words,
      // check SQL reserved words on table name and JAVA reserved words on
      // entity name
      // Use try to catch exception and show custom message for this situation
      try {
        ReservedWords.verifyReservedSqlKeywordsNotPresent(table);
      } catch (IllegalStateException exception) {
        LOGGER.log(Level.INFO,
            "ERROR: You are trying to use a SQL reserved word as table name. You have the following options:\n"
                + "1. Specify a valid table name using --table parameter.\n"
                + "2. Use parameter --permitReservedWords to force use of reserved words.\n");
        return;
      }
      // Use try to catch exception and show custom message for this situation
      try {
        ReservedWords.verifyReservedJavaKeywordsNotPresent(name);
      } catch (IllegalStateException exception) {
        LOGGER
            .log(
                Level.INFO,
                "ERROR: You are trying to use a Java reserved word as entity name. You have the following options:\n"
                    + "1. Change provided entity name.\n"
                    + "2. Use parameter --permitReservedWords to force use of reserved words.\n");
        return;
      }
    }

    // Reject attempts to name the entity "Test", due to possible clashes
    // with data on demand (see ROO-50)
    // We will allow this to happen, though if the user insists on it via
    // --permitReservedWords (see ROO-666)
    if (!BeanInfoUtils.isEntityReasonablyNamed(name)) {
      if (!permitReservedWords) {
        throw new IllegalArgumentException(
            "Entity name rejected as conflicts with test execution defaults; please remove "
                + "'Test' and/or 'TestCase'");
      }
    }

    // Create entity's annotations
    final List<AnnotationMetadataBuilder> annotationBuilder =
        new ArrayList<AnnotationMetadataBuilder>();
    final AnnotationMetadataBuilder javaBeanAnnotationBuilder =
        new AnnotationMetadataBuilder(ROO_JAVA_BEAN);
    if (readOnly) {
      // ROO-3838: "ReadOnly" entities should not have setter methods.
      javaBeanAnnotationBuilder.addBooleanAttribute("settersByDefault", false);
    }
    annotationBuilder.add(javaBeanAnnotationBuilder);
    annotationBuilder.add(ROO_TO_STRING_BUILDER);
    annotationBuilder.add(getEntityAnnotationBuilder(table, schema, catalog, inheritanceType,
        mappedSuperclass, entityName, readOnly, formatExpression, formatMessage));

    // Add @RooEquals only if it's superclass is not an entity
    ClassOrInterfaceTypeDetails superclassCid = typeLocationService.getTypeDetails(superclass);
    if (superclassCid == null || superclassCid.getAnnotation(ROO_JPA_ENTITY) == null) {
      final AnnotationMetadataBuilder equalsAnnotationBuilder = ROO_EQUALS_BUILDER;
      equalsAnnotationBuilder.addBooleanAttribute("isJpaEntity", true);
      annotationBuilder.add(equalsAnnotationBuilder);
    }

    // Add @RooSerializable
    if (serializable) {
      annotationBuilder.add(ROO_SERIALIZABLE_BUILDER);
    }
    // ROO-3817: Including @RooPlural annotation if needed
    if (StringUtils.isNotEmpty(plural)) {
      AnnotationMetadataBuilder pluralAnnotation = new AnnotationMetadataBuilder(ROO_PLURAL);
      pluralAnnotation.addStringAttribute("value", plural);
      annotationBuilder.add(pluralAnnotation);
    }

    // Produce the entity itself
    jpaOperations.newEntity(name, createAbstract, superclass, implementsType, identifierField,
        identifierType, identifierColumn, sequenceName, identifierStrategy, versionField,
        versionType, versionColumn, inheritanceType, annotationBuilder);

    // Update entity identifier class if required (identifierClass should be
    // only an embeddable class)
    if (!(identifierType.getPackage().getFullyQualifiedPackageName().startsWith("java."))) {
      jpaOperations.updateEmbeddableToIdentifier(identifierType, identifierField, identifierColumn);
    }
  }

  /**
   * Returns a builder for the entity-related annotation to be added to a newly
   * created JPA entity
   *
   * @param table
   * @param schema
   * @param catalog
   * @param inheritanceType
   * @param mappedSuperclass
   * @param entityName
   * @param readOnly
   * @return a non-<code>null</code> builder
   */
  private AnnotationMetadataBuilder getEntityAnnotationBuilder(final String table,
      final String schema, final String catalog, final InheritanceType inheritanceType,
      final boolean mappedSuperclass, final String entityName, final boolean readOnly,
      final String formatExpression, final String formatMessage) {
    final AnnotationMetadataBuilder entityAnnotationBuilder =
        new AnnotationMetadataBuilder(ROO_JPA_ENTITY);

    // Attributes that apply to all JPA entities (active record or not)
    if (catalog != null) {
      entityAnnotationBuilder.addStringAttribute("catalog", catalog);
    }
    if (entityName != null) {
      entityAnnotationBuilder.addStringAttribute("entityName", entityName);
    }
    if (inheritanceType != null) {
      entityAnnotationBuilder.addStringAttribute("inheritanceType", inheritanceType.name());
    }
    if (mappedSuperclass) {
      entityAnnotationBuilder.addBooleanAttribute("mappedSuperclass", mappedSuperclass);
    }
    if (schema != null) {
      entityAnnotationBuilder.addStringAttribute("schema", schema);
    }
    if (table != null) {
      entityAnnotationBuilder.addStringAttribute("table", table);
    }

    // ROO-3868: New entity visualization support using a new format annotation
    if (StringUtils.isNotBlank(formatExpression)) {
      entityAnnotationBuilder.addStringAttribute("entityFormatExpression", formatExpression);
    }
    if (StringUtils.isNotBlank(formatMessage)) {
      entityAnnotationBuilder.addStringAttribute("entityFormatMessage", formatMessage);
    }

    // ROO-3708: Generate readOnly entities
    if (readOnly) {
      entityAnnotationBuilder.addBooleanAttribute("readOnly", true);
    }

    return entityAnnotationBuilder;
  }

  private boolean isJdk6OrHigher() {
    final String ver = System.getProperty("java.version");
    return ver.indexOf("1.6.") > -1 || ver.indexOf("1.7.") > -1;
  }

  /**
   * Returns a String with the JavaPackage name to show. It will return a full 
   * name if provided package doesn't contain the module top level JavaPackage. 
   * Otherwise, the package name will be shortened using `~`.
   * 
   * @param module the String with the module name.
   * @param javaPackage the JavaPackage to extract the String to return.
   * @return a String with the value to show for the java package.
   */
  private String getPackageStringValue(String module, String javaPackageName) {

    // Get project top level package from application class
    Set<JavaType> applicationTypes =
        typeLocationService.findTypesWithAnnotation(SpringJavaType.SPRING_BOOT_APPLICATION);
    Validate.isTrue(!applicationTypes.isEmpty(), "Couldn't find a main class "
        + "annotated with `@SpringBootApplication` in the project.");
    String topLevelPackage =
        projectOperations.getTopLevelPackage(module).getFullyQualifiedPackageName();

    // If package name contains top level package name, shorten it
    if (javaPackageName.contains(topLevelPackage)) {
      javaPackageName = javaPackageName.replace(topLevelPackage, "~");
    }
    return javaPackageName;
  }

  /**
   * Check if superclass of the extended entity which it's going to be created
   * will override any specified param and shows a message if so. If user uses
   * the --force global param it will be possible to execute the command for
   * creating the entity.
   *
   * @param identifierColumn
   * @param identifierField
   * @param identifierStrategy
   * @param identifierType
   * @param sequenceName
   * @param versionColumn
   * @param versionField
   * @param versionType
   */
  private void checkExtendsOverride(String identifierColumn, String identifierField,
      IdentifierStrategy identifierStrategy, JavaType identifierType, String sequenceName,
      String versionColumn, String versionField, JavaType versionType) {
    if (identifierColumn != null || identifierField != null || identifierStrategy != null
        || !IDENTIFIER_DEFAULT_TYPE.equals(identifierType.getFullyQualifiedTypeName())
        || sequenceName != null || versionColumn != null || versionField != null
        || !VERSION_DEFAULT_TYPE.equals(versionType.getFullyQualifiedTypeName())) {
      throw new IllegalArgumentException(
          "Identifier and version fields will be overwritten by superclass fields. Please, "
              + "use --force to execute the command anyway.");

    }
  }

  /**
   * Replaces a JavaType fullyQualifiedName for a shorter name using '~' for
   * TopLevelPackage
   *
   * @param cid ClassOrInterfaceTypeDetails of a JavaType
   * @param currentText String current text for option value
   * @return the String representing a JavaType with its name shortened
   */
  private String replaceTopLevelPackageString(ClassOrInterfaceTypeDetails cid, String currentText) {
    String javaTypeFullyQualilfiedName = cid.getType().getFullyQualifiedTypeName();
    String javaTypeString = "";
    String topLevelPackageString = "";

    // Add module value to topLevelPackage when necessary
    if (StringUtils.isNotBlank(cid.getType().getModule())
        && !cid.getType().getModule().equals(projectOperations.getFocusedModuleName())) {

      // Target module is not focused
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          projectOperations.getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else if (StringUtils.isNotBlank(cid.getType().getModule())
        && cid.getType().getModule().equals(projectOperations.getFocusedModuleName())
        && (currentText.startsWith(cid.getType().getModule()) || cid.getType().getModule()
            .startsWith(currentText)) && StringUtils.isNotBlank(currentText)) {

      // Target module is focused but user wrote it
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          projectOperations.getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else {

      // Not multimodule project
      topLevelPackageString =
          projectOperations.getFocusedTopLevelPackage().getFullyQualifiedPackageName();
    }

    // Autocomplete with abbreviate or full qualified mode
    String auxString =
        javaTypeString.concat(StringUtils.replace(javaTypeFullyQualilfiedName,
            topLevelPackageString, "~"));
    if ((StringUtils.isBlank(currentText) || auxString.startsWith(currentText))
        && StringUtils.contains(javaTypeFullyQualilfiedName, topLevelPackageString)) {

      // Value is for autocomplete only or user wrote abbreviate value
      javaTypeString = auxString;
    } else {

      // Value could be for autocomplete or for validation
      javaTypeString = String.format("%s%s", javaTypeString, javaTypeFullyQualilfiedName);
    }

    return javaTypeString;
  }

}
