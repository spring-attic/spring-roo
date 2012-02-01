package org.springframework.roo.addon.jpa;

import static org.springframework.roo.model.GoogleJavaType.GAE_DATASTORE_KEY;
import static org.springframework.roo.model.JavaType.LONG_OBJECT;
import static org.springframework.roo.model.RooJavaType.ROO_EQUALS;
import static org.springframework.roo.model.RooJavaType.ROO_JAVA_BEAN;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ACTIVE_RECORD;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ENTITY;
import static org.springframework.roo.model.RooJavaType.ROO_SERIALIZABLE;
import static org.springframework.roo.model.RooJavaType.ROO_TO_STRING;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.jpa.entity.RooJpaEntity;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.addon.test.IntegrationTestOperations;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.operations.InheritanceType;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.converters.StaticFieldConverter;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Commands for the JPA add-on to be used by the ROO shell.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.0
 */
@Component
@Service
public class JpaCommands implements CommandMarker {

    private static Logger LOGGER = HandlerUtils.getLogger(JpaCommands.class);
    private static final AnnotationMetadataBuilder ROO_EQUALS_BUILDER = new AnnotationMetadataBuilder(
            ROO_EQUALS);
    private static final AnnotationMetadataBuilder ROO_JAVA_BEAN_BUILDER = new AnnotationMetadataBuilder(
            ROO_JAVA_BEAN);
    private static final AnnotationMetadataBuilder ROO_SERIALIZABLE_BUILDER = new AnnotationMetadataBuilder(
            ROO_SERIALIZABLE);
    private static final AnnotationMetadataBuilder ROO_TO_STRING_BUILDER = new AnnotationMetadataBuilder(
            ROO_TO_STRING);

    @Reference private IntegrationTestOperations integrationTestOperations;
    @Reference private JpaOperations jpaOperations;
    @Reference private ProjectOperations projectOperations;
    @Reference private PropFileOperations propFileOperations;
    @Reference private StaticFieldConverter staticFieldConverter;

    protected void activate(final ComponentContext context) {
        staticFieldConverter.add(JdbcDatabase.class);
        staticFieldConverter.add(OrmProvider.class);
    }

    @CliCommand(value = "embeddable", help = "Creates a new Java class source file with the JPA @Embeddable annotation in SRC_MAIN_JAVA")
    public void createEmbeddableClass(
            @CliOption(key = "class", optionContext = "update,project", mandatory = true, help = "The name of the class to create") final JavaType name,
            @CliOption(key = "serializable", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Whether the generated class should implement java.io.Serializable") final boolean serializable,
            @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords) {

        if (!permitReservedWords) {
            ReservedWords.verifyReservedWordsNotPresent(name);
        }

        jpaOperations.newEmbeddableClass(name, serializable);
    }

    @CliCommand(value = "database properties list", help = "Shows database configuration details")
    public SortedSet<String> databaseProperties() {
        return jpaOperations.getDatabaseProperties();
    }

    @CliCommand(value = "database properties remove", help = "Removes a particular database property")
    public void databaseRemove(
            @CliOption(key = { "", "key" }, mandatory = true, help = "The property key that should be removed") final String key) {

        propFileOperations.removeProperty(Path.SPRING_CONFIG_ROOT
                .getModulePathId(projectOperations.getFocusedModuleName()),
                "database.properties", key);
    }

    @CliCommand(value = "database properties set", help = "Changes a particular database property")
    public void databaseSet(
            @CliOption(key = "key", mandatory = true, help = "The property key that should be changed") final String key,
            @CliOption(key = "value", mandatory = true, help = "The new vale for this property key") final String value) {

        propFileOperations.changeProperty(Path.SPRING_CONFIG_ROOT
                .getModulePathId(projectOperations.getFocusedModuleName()),
                "database.properties", key, value);
    }

    protected void deactivate(final ComponentContext context) {
        staticFieldConverter.remove(JdbcDatabase.class);
        staticFieldConverter.remove(OrmProvider.class);
    }

    /**
     * Returns a builder for the entity-related annotation to be added to a
     * newly created JPA entity
     * 
     * @param table
     * @param schema
     * @param catalog
     * @param identifierField
     * @param identifierColumn
     * @param identifierType
     * @param versionField
     * @param versionColumn
     * @param versionType
     * @param inheritanceType
     * @param mappedSuperclass
     * @param persistenceUnit
     * @param transactionManager
     * @param entityName
     * @param sequenceName
     * @param activeRecord whether to generate active record CRUD methods for
     *            the entity
     * @return a non-<code>null</code> builder
     */
    private AnnotationMetadataBuilder getEntityAnnotationBuilder(
            final String table, final String schema, final String catalog,
            final String identifierField, final String identifierColumn,
            final JavaType identifierType, final String versionField,
            final String versionColumn, final JavaType versionType,
            final InheritanceType inheritanceType,
            final boolean mappedSuperclass, final String persistenceUnit,
            final String transactionManager, final String entityName,
            final String sequenceName, final boolean activeRecord) {
        final AnnotationMetadataBuilder entityAnnotationBuilder = new AnnotationMetadataBuilder(
                getEntityAnnotationType(activeRecord));

        // Attributes that apply to all JPA entities (active record or not)
        if (catalog != null) {
            entityAnnotationBuilder.addStringAttribute("catalog", catalog);
        }
        if (entityName != null) {
            entityAnnotationBuilder
                    .addStringAttribute("entityName", entityName);
        }
        if (sequenceName != null) {
            entityAnnotationBuilder.addStringAttribute("sequenceName",
                    sequenceName);
        }
        if (identifierColumn != null) {
            entityAnnotationBuilder.addStringAttribute("identifierColumn",
                    identifierColumn);
        }
        if (identifierField != null) {
            entityAnnotationBuilder.addStringAttribute("identifierField",
                    identifierField);
        }
        if (!LONG_OBJECT.equals(identifierType)) {
            entityAnnotationBuilder.addClassAttribute("identifierType",
                    identifierType);
        }
        if (inheritanceType != null) {
            entityAnnotationBuilder.addStringAttribute("inheritanceType",
                    inheritanceType.name());
        }
        if (mappedSuperclass) {
            entityAnnotationBuilder.addBooleanAttribute("mappedSuperclass",
                    mappedSuperclass);
        }
        if (schema != null) {
            entityAnnotationBuilder.addStringAttribute("schema", schema);
        }
        if (table != null) {
            entityAnnotationBuilder.addStringAttribute("table", table);
        }
        if (versionColumn != null
                && !RooJpaEntity.VERSION_COLUMN_DEFAULT.equals(versionColumn)) {
            entityAnnotationBuilder.addStringAttribute("versionColumn",
                    versionColumn);
        }
        if (versionField != null
                && !RooJpaEntity.VERSION_FIELD_DEFAULT.equals(versionField)) {
            entityAnnotationBuilder.addStringAttribute("versionField",
                    versionField);
        }
        if (!JavaType.INT_OBJECT.equals(versionType)) {
            entityAnnotationBuilder.addClassAttribute("versionType",
                    versionType);
        }

        // Attributes that only apply to entities with CRUD active record
        // methods
        if (activeRecord) {
            if (persistenceUnit != null) {
                entityAnnotationBuilder.addStringAttribute("persistenceUnit",
                        persistenceUnit);
            }
            if (transactionManager != null) {
                entityAnnotationBuilder.addStringAttribute(
                        "transactionManager", transactionManager);
            }
        }

        return entityAnnotationBuilder;
    }

    /**
     * Returns the type of annotation to put on the entity
     * 
     * @param activeRecord whether the entity is to have CRUD active record
     *            methods generated
     * @return a non-<code>null</code> type
     */
    private JavaType getEntityAnnotationType(final boolean activeRecord) {
        return activeRecord ? ROO_JPA_ACTIVE_RECORD : ROO_JPA_ENTITY;
    }

    @CliAvailabilityIndicator({ "database properties list",
            "database properties remove", "database properties set" })
    public boolean hasDatabaseProperties() {
        return isJpaSetupAvailable() && jpaOperations.hasDatabaseProperties();
    }

    @CliCommand(value = "jpa setup", help = "Install or updates a JPA persistence provider in your project")
    public void installJpa(
            @CliOption(key = "provider", mandatory = true, help = "The persistence provider to support") final OrmProvider ormProvider,
            @CliOption(key = "database", mandatory = true, help = "The database to support") final JdbcDatabase jdbcDatabase,
            @CliOption(key = "applicationId", mandatory = false, unspecifiedDefaultValue = "the project's name", help = "The Google App Engine application identifier to use") final String applicationId,
            @CliOption(key = "jndiDataSource", mandatory = false, help = "The JNDI datasource to use") final String jndi,
            @CliOption(key = "hostName", mandatory = false, help = "The host name to use") final String hostName,
            @CliOption(key = "databaseName", mandatory = false, help = "The database name to use") final String databaseName,
            @CliOption(key = "userName", mandatory = false, help = "The username to use") final String userName,
            @CliOption(key = "password", mandatory = false, help = "The password to use") final String password,
            @CliOption(key = "transactionManager", mandatory = false, help = "The transaction manager name") final String transactionManager,
            @CliOption(key = "persistenceUnit", mandatory = false, help = "The persistence unit name to be used in the persistence.xml file") final String persistenceUnit) {

        if (jdbcDatabase == JdbcDatabase.GOOGLE_APP_ENGINE
                && ormProvider != OrmProvider.DATANUCLEUS) {
            LOGGER.warning("Provider must be " + OrmProvider.DATANUCLEUS.name()
                    + " for the Google App Engine");
            return;
        }

        if (jdbcDatabase == JdbcDatabase.DATABASE_DOT_COM
                && ormProvider != OrmProvider.DATANUCLEUS) {
            LOGGER.warning("Provider must be " + OrmProvider.DATANUCLEUS.name()
                    + " for Database.com");
            return;
        }

        if (jdbcDatabase == JdbcDatabase.FIREBIRD && !isJdk6OrHigher()) {
            LOGGER.warning("JDK must be 1.6 or higher to use Firebird");
            return;
        }

        jpaOperations.configureJpa(ormProvider, jdbcDatabase, jndi,
                applicationId, hostName, databaseName, userName, password,
                transactionManager, persistenceUnit,
                projectOperations.getFocusedModuleName());
    }

    @Deprecated
    @CliCommand(value = "persistence setup", help = "Install or updates a JPA persistence provider in your project - deprecated, use 'jpa setup' instead")
    public void installPersistence(
            @CliOption(key = "provider", mandatory = true, help = "The persistence provider to support") final OrmProvider ormProvider,
            @CliOption(key = "database", mandatory = true, help = "The database to support") final JdbcDatabase jdbcDatabase,
            @CliOption(key = "applicationId", mandatory = false, unspecifiedDefaultValue = "the project's name", help = "The Google App Engine application identifier to use") final String applicationId,
            @CliOption(key = "jndiDataSource", mandatory = false, help = "The JNDI datasource to use") final String jndi,
            @CliOption(key = "hostName", mandatory = false, help = "The host name to use") final String hostName,
            @CliOption(key = "databaseName", mandatory = false, help = "The database name to use") final String databaseName,
            @CliOption(key = "userName", mandatory = false, help = "The username to use") final String userName,
            @CliOption(key = "password", mandatory = false, help = "The password to use") final String password,
            @CliOption(key = "transactionManager", mandatory = false, help = "The transaction manager name") final String transactionManager,
            @CliOption(key = "persistenceUnit", mandatory = false, help = "The persistence unit name to be used in the persistence.xml file") final String persistenceUnit) {

        installJpa(ormProvider, jdbcDatabase, applicationId, jndi, hostName,
                databaseName, userName, password, transactionManager,
                persistenceUnit);
    }

    private boolean isJdk6OrHigher() {
        final String ver = System.getProperty("java.version");
        return ver.indexOf("1.6.") > -1 || ver.indexOf("1.7.") > -1;
    }

    @CliAvailabilityIndicator({ "jpa setup", "persistence setup" })
    public boolean isJpaSetupAvailable() {
        return jpaOperations.isJpaInstallationPossible();
    }

    @CliAvailabilityIndicator({ "entity jpa", "embeddable" })
    public boolean isPersistentClassAvailable() {
        return jpaOperations.isPersistentClassAvailable();
    }

    @CliCommand(value = "entity jpa", help = "Creates a new JPA persistent entity in SRC_MAIN_JAVA")
    public void newPersistenceClassJpa(
            @CliOption(key = "class", optionContext = "update,project", mandatory = true, help = "Name of the entity to create") final JavaType name,
            @CliOption(key = "extends", mandatory = false, unspecifiedDefaultValue = "java.lang.Object", help = "The superclass (defaults to java.lang.Object)") final JavaType superclass,
            @CliOption(key = "abstract", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Whether the generated class should be marked as abstract") final boolean createAbstract,
            @CliOption(key = "testAutomatically", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Create automatic integration tests for this entity") final boolean testAutomatically,
            @CliOption(key = "table", mandatory = false, help = "The JPA table name to use for this entity") final String table,
            @CliOption(key = "schema", mandatory = false, help = "The JPA table schema name to use for this entity") final String schema,
            @CliOption(key = "catalog", mandatory = false, help = "The JPA table catalog name to use for this entity") final String catalog,
            @CliOption(key = "identifierField", mandatory = false, help = "The JPA identifier field name to use for this entity") final String identifierField,
            @CliOption(key = "identifierColumn", mandatory = false, help = "The JPA identifier field column to use for this entity") final String identifierColumn,
            @CliOption(key = "identifierType", mandatory = false, optionContext = "java-lang,project", unspecifiedDefaultValue = "java.lang.Long", specifiedDefaultValue = "java.lang.Long", help = "The data type that will be used for the JPA identifier field (defaults to java.lang.Long)") final JavaType identifierType,
            @CliOption(key = "versionField", mandatory = false, help = "The JPA version field name to use for this entity") final String versionField,
            @CliOption(key = "versionColumn", mandatory = false, help = "The JPA version field column to use for this entity") final String versionColumn,
            @CliOption(key = "versionType", mandatory = false, optionContext = "java-lang,project", unspecifiedDefaultValue = "java.lang.Integer", help = "The data type that will be used for the JPA version field (defaults to java.lang.Integer)") final JavaType versionType,
            @CliOption(key = "inheritanceType", mandatory = false, help = "The JPA @Inheritance value (apply to base class)") final InheritanceType inheritanceType,
            @CliOption(key = "mappedSuperclass", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Apply @MappedSuperclass for this entity") final boolean mappedSuperclass,
            @CliOption(key = "equals", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Whether the generated class should implement equals and hashCode methods") final boolean equals,
            @CliOption(key = "serializable", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Whether the generated class should implement java.io.Serializable") final boolean serializable,
            @CliOption(key = "persistenceUnit", mandatory = false, help = "The persistence unit name to be used in the persistence.xml file") final String persistenceUnit,
            @CliOption(key = "transactionManager", mandatory = false, help = "The transaction manager name") final String transactionManager,
            @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
            @CliOption(key = "entityName", mandatory = false, help = "The name used to refer to the entity in queries") final String entityName,
            @CliOption(key = "sequenceName", mandatory = false, help = "The name of the sequence for incrementing sequence-driven primary keys") final String sequenceName,
            @CliOption(key = "activeRecord", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "true", help = "Generate CRUD active record methods for this entity") final boolean activeRecord) {
        Validate.isTrue(!identifierType.isPrimitive(),
                "Identifier type cannot be a primitive");

        if (!permitReservedWords) {
            ReservedWords.verifyReservedWordsNotPresent(name);
        }
        if (testAutomatically && createAbstract) {
            // We can't test an abstract class
            throw new IllegalArgumentException(
                    "Automatic tests cannot be created for an abstract entity; remove the --testAutomatically or --abstract option");
        }

        // Reject attempts to name the entity "Test", due to possible clashes
        // with data on demand (see ROO-50)
        // We will allow this to happen, though if the user insists on it via
        // --permitReservedWords (see ROO-666)
        if (!BeanInfoUtils.isEntityReasonablyNamed(name)) {
            if (permitReservedWords && testAutomatically) {
                throw new IllegalArgumentException(
                        "Entity name cannot contain 'Test' or 'TestCase' as you are requesting tests; remove --testAutomatically or rename the proposed entity");
            }
            if (!permitReservedWords) {
                throw new IllegalArgumentException(
                        "Entity name rejected as conflicts with test execution defaults; please remove 'Test' and/or 'TestCase'");
            }
        }

        // Create entity's annotations
        final List<AnnotationMetadataBuilder> annotationBuilder = new ArrayList<AnnotationMetadataBuilder>();
        annotationBuilder.add(ROO_JAVA_BEAN_BUILDER);
        annotationBuilder.add(ROO_TO_STRING_BUILDER);
        annotationBuilder.add(getEntityAnnotationBuilder(table, schema,
                catalog, identifierField, identifierColumn, identifierType,
                versionField, versionColumn, versionType, inheritanceType,
                mappedSuperclass, persistenceUnit, transactionManager,
                entityName, sequenceName, activeRecord));
        if (equals) {
            annotationBuilder.add(ROO_EQUALS_BUILDER);
        }
        if (serializable) {
            annotationBuilder.add(ROO_SERIALIZABLE_BUILDER);
        }

        // Produce the entity itself
        jpaOperations.newEntity(name, createAbstract, superclass,
                annotationBuilder);

        // Create entity identifier class if required
        if (!(identifierType.getPackage().getFullyQualifiedPackageName()
                .startsWith("java.") || identifierType
                .equals(GAE_DATASTORE_KEY))) {
            jpaOperations.newIdentifier(identifierType, identifierField,
                    identifierColumn);
        }

        if (testAutomatically) {
            integrationTestOperations.newIntegrationTest(name);
        }
    }
}