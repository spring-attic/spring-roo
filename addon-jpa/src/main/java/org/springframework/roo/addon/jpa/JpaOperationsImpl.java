package org.springframework.roo.addon.jpa;

import static org.springframework.roo.model.JavaType.OBJECT;
import static org.springframework.roo.model.JpaJavaType.EMBEDDABLE;
import static org.springframework.roo.model.RooJavaType.ROO_EQUALS;
import static org.springframework.roo.model.RooJavaType.ROO_IDENTIFIER;
import static org.springframework.roo.model.RooJavaType.ROO_JAVA_BEAN;
import static org.springframework.roo.model.RooJavaType.ROO_SERIALIZABLE;
import static org.springframework.roo.model.RooJavaType.ROO_TO_STRING;
import static org.springframework.roo.model.SpringJavaType.JPA_TRANSACTION_MANAGER;
import static org.springframework.roo.model.SpringJavaType.LOCAL_CONTAINER_ENTITY_MANAGER_FACTORY_BEAN;
import static org.springframework.roo.model.SpringJavaType.LOCAL_ENTITY_MANAGER_FACTORY_BEAN;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.DependencyScope;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Filter;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.Repository;
import org.springframework.roo.project.Resource;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link JpaOperations}.
 * 
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
@Component
@Service
public class JpaOperationsImpl implements JpaOperations {

    static class LinkedProperties extends Properties {
        private static final long serialVersionUID = -8828266911075836165L;
        private final Set<Object> keys = new LinkedHashSet<Object>();

        @Override
        public Enumeration<Object> keys() {
            return Collections.<Object> enumeration(keys);
        }

        @Override
        public Object put(final Object key, final Object value) {
            keys.add(key);
            return super.put(key, value);
        }
    }

    static final String APPLICATION_CONTEXT_XML = "applicationContext.xml";
    private static final String DATABASE_DRIVER = "database.driverClassName";
    private static final String DATABASE_PASSWORD = "database.password";
    private static final String DATABASE_PROPERTIES_FILE = "database.properties";
    private static final String DATABASE_URL = "database.url";
    private static final String DATABASE_USERNAME = "database.username";
    private static final String DEFAULT_PERSISTENCE_UNIT = "persistenceUnit";
    private static final String GAE_PERSISTENCE_UNIT_NAME = "transactions-optional";
    static final String JPA_DIALECTS_FILE = "jpa-dialects.properties";

    private static final Dependency JSTL_IMPL_DEPENDENCY = new Dependency(
            "org.glassfish.web", "jstl-impl", "1.2");
    private static final Logger LOGGER = HandlerUtils
            .getLogger(JpaOperationsImpl.class);
    private static final String PERSISTENCE_UNIT = "persistence-unit";
    static final String PERSISTENCE_XML = "META-INF/persistence.xml";

    static final String POM_XML = "pom.xml";

    @Reference FileManager fileManager;
    @Reference PathResolver pathResolver;
    @Reference ProjectOperations projectOperations;
    @Reference PropFileOperations propFileOperations;
    @Reference TypeLocationService typeLocationService;
    @Reference TypeManagementService typeManagementService;

    public void configureJpa(final OrmProvider ormProvider,
            final JdbcDatabase jdbcDatabase, final String jndi,
            final String applicationId, final String hostName,
            final String databaseName, final String userName,
            final String password, final String transactionManager,
            final String persistenceUnit, final String moduleName) {
        Validate.notNull(ormProvider, "ORM provider required");
        Validate.notNull(jdbcDatabase, "JDBC database required");

        // Parse the configuration.xml file
        final Element configuration = XmlUtils.getConfiguration(getClass());

        // Get the first part of the XPath expressions for unwanted databases
        // and ORM providers
        final String databaseXPath = getDbXPath(getUnwantedDatabases(jdbcDatabase));
        final String providersXPath = getProviderXPath(getUnwantedOrmProviders(ormProvider));

        if (jdbcDatabase != JdbcDatabase.GOOGLE_APP_ENGINE) {
            updateEclipsePlugin(false);
            updateDataNucleusPlugin(false);
            projectOperations.updateDependencyScope(moduleName,
                    JSTL_IMPL_DEPENDENCY, null);
        }

        updateApplicationContext(ormProvider, jdbcDatabase, jndi,
                transactionManager, persistenceUnit);
        updatePersistenceXml(ormProvider, jdbcDatabase, hostName, databaseName,
                userName, password, persistenceUnit, moduleName);
        manageGaeXml(ormProvider, jdbcDatabase, applicationId, moduleName);
        updateDatabaseDotComConfigProperties(ormProvider, jdbcDatabase,
                hostName, userName, password, StringUtils.defaultIfEmpty(
                        persistenceUnit, DEFAULT_PERSISTENCE_UNIT), moduleName);

        if (StringUtils.isBlank(jndi)) {
            updateDatabaseProperties(ormProvider, jdbcDatabase, hostName,
                    databaseName, userName, password, moduleName);
        }
        else {
            updateJndiProperties();
        }

        updateLog4j(ormProvider);
        updatePomProperties(configuration, ormProvider, jdbcDatabase,
                moduleName);
        updateDependencies(configuration, ormProvider, jdbcDatabase,
                databaseXPath, providersXPath, moduleName);
        updateRepositories(configuration, ormProvider, jdbcDatabase, moduleName);
        updatePluginRepositories(configuration, ormProvider, jdbcDatabase,
                moduleName);
        updateFilters(configuration, ormProvider, jdbcDatabase, databaseXPath,
                providersXPath, moduleName);
        updateResources(configuration, ormProvider, jdbcDatabase,
                databaseXPath, providersXPath, moduleName);
        updateBuildPlugins(configuration, ormProvider, jdbcDatabase,
                databaseXPath, providersXPath, moduleName);
    }

    private Element createPropertyElement(final String name,
            final String value, final Document document) {
        final Element property = document.createElement("property");
        property.setAttribute("name", name);
        property.setAttribute("value", value);
        return property;
    }

    private Element createRefElement(final String name, final String value,
            final Document document) {
        final Element property = document.createElement("property");
        property.setAttribute("name", name);
        property.setAttribute("ref", value);
        return property;
    }

    private String getConnectionString(final JdbcDatabase jdbcDatabase,
            String hostName, final String databaseName, final String moduleName) {
        String connectionString = jdbcDatabase.getConnectionString();
        if (connectionString.contains("TO_BE_CHANGED_BY_ADDON")) {
            connectionString = connectionString.replace(
                    "TO_BE_CHANGED_BY_ADDON", StringUtils
                            .isNotBlank(databaseName) ? databaseName
                            : projectOperations.getProjectName(moduleName));
        }
        else {
            if (StringUtils.isNotBlank(databaseName)) {
                // Oracle uses a different connection URL - see ROO-1203
                final String dbDelimiter = jdbcDatabase == JdbcDatabase.ORACLE ? ":"
                        : "/";
                connectionString += dbDelimiter + databaseName;
            }
        }
        if (StringUtils.isBlank(hostName)) {
            hostName = "localhost";
        }
        return connectionString.replace("HOST_NAME", hostName);
    }

    public SortedSet<String> getDatabaseProperties() {
        if (hasDatabaseProperties()) {
            return propFileOperations.getPropertyKeys(Path.SPRING_CONFIG_ROOT
                    .getModulePathId(projectOperations.getFocusedModuleName()),
                    DATABASE_PROPERTIES_FILE, true);
        }
        return getPropertiesFromDataNucleusConfiguration();
    }

    private String getDatabasePropertiesPath() {
        return getPropertiesPath(DATABASE_PROPERTIES_FILE);
    }

    private String getDbXPath(final List<JdbcDatabase> databases) {
        final StringBuilder builder = new StringBuilder(
                "/configuration/databases/database[");
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

    private List<Dependency> getDependencies(final String xPathExpression,
            final Element configuration, final String moduleName) {
        final List<Dependency> dependencies = new ArrayList<Dependency>();
        for (final Element dependencyElement : XmlUtils.findElements(
                xPathExpression + "/dependencies/dependency", configuration)) {
            final Dependency dependency = new Dependency(dependencyElement);
            if (dependency.getGroupId().equals("com.google.appengine")
                    && dependency.getArtifactId().equals(
                            "appengine-api-1.0-sdk")
                    && projectOperations
                            .isFeatureInstalledInFocusedModule(FeatureNames.GWT)) {
                continue;
            }
            dependencies.add(dependency);
        }
        return dependencies;
    }

    private List<Filter> getFilters(final String xPathExpression,
            final Element configuration) {
        final List<Filter> filters = new ArrayList<Filter>();
        for (final Element filterElement : XmlUtils.findElements(
                xPathExpression + "/filters/filter", configuration)) {
            filters.add(new Filter(filterElement));
        }
        return filters;
    }

    private String getJndiPropertiesPath() {
        return getPropertiesPath("jndi.properties");
    }

    public String getName() {
        return FeatureNames.JPA;
    }

    private String getPersistencePathOfFocussedModule() {
        return pathResolver.getFocusedIdentifier(Path.SRC_MAIN_RESOURCES,
                PERSISTENCE_XML);
    }

    private List<Plugin> getPlugins(final String xPathExpression,
            final Element configuration) {
        final List<Plugin> buildPlugins = new ArrayList<Plugin>();
        for (final Element pluginElement : XmlUtils.findElements(
                xPathExpression + "/plugins/plugin", configuration)) {
            buildPlugins.add(new Plugin(pluginElement));
        }
        return buildPlugins;
    }

    private String getProjectName(final String moduleName) {
        return projectOperations.getProjectName(moduleName);
    }

    private SortedSet<String> getPropertiesFromDataNucleusConfiguration() {
        final String persistenceXmlPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_RESOURCES, PERSISTENCE_XML);
        if (!fileManager.exists(persistenceXmlPath)) {
            throw new IllegalStateException("Failed to find "
                    + persistenceXmlPath);
        }

        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(persistenceXmlPath));
        final Element root = document.getDocumentElement();

        final List<Element> propertyElements = XmlUtils.findElements(
                "/persistence/persistence-unit/properties/property", root);
        Validate.notEmpty(propertyElements,
                "Failed to find property elements in %s", persistenceXmlPath);
        final SortedSet<String> properties = new TreeSet<String>();

        for (final Element propertyElement : propertyElements) {
            final String key = propertyElement.getAttribute("name");
            final String value = propertyElement.getAttribute("value");
            if ("datanucleus.ConnectionDriverName".equals(key)) {
                properties.add("datanucleus.ConnectionDriverName = " + value);
            }
            if ("datanucleus.ConnectionURL".equals(key)) {
                properties.add("datanucleus.ConnectionURL = " + value);
            }
            if ("datanucleus.ConnectionUserName".equals(key)) {
                properties.add("datanucleus.ConnectionUserName = " + value);
            }
            if ("datanucleus.ConnectionPassword".equals(key)) {
                properties.add("datanucleus.ConnectionPassword = " + value);
            }

            if (properties.size() == 4) {
                // All required properties have been found so ignore rest of
                // elements
                break;
            }
        }
        return properties;
    }

    private String getPropertiesPath(final String propertiesFile) {
        String path = pathResolver.getFocusedIdentifier(
                Path.SPRING_CONFIG_ROOT, propertiesFile);
        if (StringUtils.isBlank(path)) {
            final String tmpDir = System.getProperty("java.io.tmpdir");
            // For unit testing, as path will be null otherwise
            path = tmpDir
                    + (!tmpDir.endsWith(File.separator) ? File.separator : "")
                    + propertiesFile;
        }
        return path;
    }

    private String getProviderXPath(final List<OrmProvider> ormProviders) {
        final StringBuilder builder = new StringBuilder(
                "/configuration/ormProviders/provider[");
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

    private List<Resource> getResources(final String xPathExpression,
            final Element configuration) {
        final List<Resource> resources = new ArrayList<Resource>();
        for (final Element resourceElement : XmlUtils.findElements(
                xPathExpression + "/resources/resource", configuration)) {
            resources.add(new Resource(resourceElement));
        }
        return resources;
    }

    private List<JdbcDatabase> getUnwantedDatabases(
            final JdbcDatabase jdbcDatabase) {
        final List<JdbcDatabase> unwantedDatabases = new ArrayList<JdbcDatabase>();
        for (final JdbcDatabase database : JdbcDatabase.values()) {
            if (!database.getKey().equals(jdbcDatabase.getKey())
                    && !database.getDriverClassName().equals(
                            jdbcDatabase.getDriverClassName())) {
                unwantedDatabases.add(database);
            }
        }
        return unwantedDatabases;
    }

    private List<OrmProvider> getUnwantedOrmProviders(
            final OrmProvider ormProvider) {
        final List<OrmProvider> unwantedOrmProviders = new LinkedList<OrmProvider>(
                Arrays.asList(OrmProvider.values()));
        unwantedOrmProviders.remove(ormProvider);
        return unwantedOrmProviders;
    }

    public boolean hasDatabaseProperties() {
        return fileManager.exists(getDatabasePropertiesPath());
    }

    public boolean isInstalledInModule(final String moduleName) {
        final LogicalPath resourcesPath = LogicalPath.getInstance(
                Path.SRC_MAIN_RESOURCES, moduleName);
        return isJpaInstallationPossible()
                && fileManager.exists(projectOperations.getPathResolver()
                        .getIdentifier(resourcesPath, PERSISTENCE_XML));
    }

    public boolean isJpaInstallationPossible() {
        return projectOperations.isFocusedProjectAvailable();
    }

    public boolean isPersistentClassAvailable() {
        return isInstalledInModule(projectOperations.getFocusedModuleName());
    }

    private void manageGaeBuildCommand(final boolean addGaeSettingsToPlugin,
            final Document document, final Collection<String> changes) {
        final Element root = document.getDocumentElement();
        final Element additionalBuildcommandsElement = XmlUtils
                .findFirstElement(
                        "/project/build/plugins/plugin[artifactId = 'maven-eclipse-plugin']/configuration/additionalBuildcommands",
                        root);
        Validate.notNull(additionalBuildcommandsElement,
                "additionalBuildCommands element of the maven-eclipse-plugin required");
        final String gaeBuildCommandName = "com.google.appengine.eclipse.core.enhancerbuilder";
        Element gaeBuildCommandElement = XmlUtils.findFirstElement(
                "buildCommand[name = '" + gaeBuildCommandName + "']",
                additionalBuildcommandsElement);
        if (addGaeSettingsToPlugin && gaeBuildCommandElement == null) {
            final Element nameElement = document.createElement("name");
            nameElement.setTextContent(gaeBuildCommandName);
            gaeBuildCommandElement = document.createElement("buildCommand");
            gaeBuildCommandElement.appendChild(nameElement);
            additionalBuildcommandsElement.appendChild(gaeBuildCommandElement);
            changes.add("added GAE buildCommand to maven-eclipse-plugin");
        }
        else if (!addGaeSettingsToPlugin && gaeBuildCommandElement != null) {
            additionalBuildcommandsElement.removeChild(gaeBuildCommandElement);
            changes.add("removed GAE buildCommand from maven-eclipse-plugin");
        }
    }

    private void manageGaeProjectNature(final boolean addGaeSettingsToPlugin,
            final Document document, final Collection<String> changes) {
        final Element root = document.getDocumentElement();
        final Element additionalProjectnaturesElement = XmlUtils
                .findFirstElement(
                        "/project/build/plugins/plugin[artifactId = 'maven-eclipse-plugin']/configuration/additionalProjectnatures",
                        root);
        Validate.notNull(additionalProjectnaturesElement,
                "additionalProjectnatures element of the maven-eclipse-plugin required");
        final String gaeProjectnatureName = "com.google.appengine.eclipse.core.gaeNature";
        Element gaeProjectnatureElement = XmlUtils.findFirstElement(
                "projectnature[text() = '" + gaeProjectnatureName + "']",
                additionalProjectnaturesElement);
        if (addGaeSettingsToPlugin && gaeProjectnatureElement == null) {
            gaeProjectnatureElement = new XmlElementBuilder("projectnature",
                    document).setText(gaeProjectnatureName).build();
            additionalProjectnaturesElement
                    .appendChild(gaeProjectnatureElement);
            changes.add("added GAE projectnature to maven-eclipse-plugin");
        }
        else if (!addGaeSettingsToPlugin && gaeProjectnatureElement != null) {
            additionalProjectnaturesElement
                    .removeChild(gaeProjectnatureElement);
            changes.add("removed GAE projectnature from maven-eclipse-plugin");
        }
    }

    private void manageGaeXml(final OrmProvider ormProvider,
            final JdbcDatabase jdbcDatabase, final String applicationId,
            final String moduleName) {
        final String appenginePath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/appengine-web.xml");
        final boolean appenginePathExists = fileManager.exists(appenginePath);

        final String loggingPropertiesPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/logging.properties");
        final boolean loggingPropertiesPathExists = fileManager
                .exists(loggingPropertiesPath);

        if (jdbcDatabase != JdbcDatabase.GOOGLE_APP_ENGINE) {
            if (appenginePathExists) {
                fileManager.delete(appenginePath,
                        "database is " + jdbcDatabase.name());
            }
            if (loggingPropertiesPathExists) {
                fileManager.delete(loggingPropertiesPath, "database is "
                        + jdbcDatabase.name());
            }
            return;
        }

        final InputStream inputStream;
        if (appenginePathExists) {
            inputStream = fileManager.getInputStream(appenginePath);
        }
        else {
            inputStream = FileUtils.getInputStream(getClass(),
                    "appengine-web-template.xml");
        }
        final Document appengine = XmlUtils.readXml(inputStream);

        final Element root = appengine.getDocumentElement();
        final Element applicationElement = XmlUtils.findFirstElement(
                "/appengine-web-app/application", root);
        final String textContent = StringUtils.defaultIfEmpty(applicationId,
                getProjectName(moduleName));
        if (!textContent.equals(applicationElement.getTextContent())) {
            applicationElement.setTextContent(textContent);
            fileManager.createOrUpdateTextFileIfRequired(appenginePath,
                    XmlUtils.nodeToString(appengine), false);
            LOGGER.warning("Please update your database details in src/main/resources/META-INF/persistence.xml.");
        }

        if (!loggingPropertiesPathExists) {
            InputStream templateInputStream = null;
            OutputStream outputStream = null;
            try {
                templateInputStream = FileUtils.getInputStream(getClass(),
                        "logging.properties");
                outputStream = fileManager.createFile(loggingPropertiesPath)
                        .getOutputStream();
                IOUtils.copy(templateInputStream, outputStream);
            }
            catch (final IOException e) {
                throw new IllegalStateException(e);
            }
            finally {
                IOUtils.closeQuietly(templateInputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }
    }

    public void newEmbeddableClass(final JavaType name,
            final boolean serializable) {
        Validate.notNull(name, "Embeddable name required");

        final String declaredByMetadataId = PhysicalTypeIdentifier
                .createIdentifier(name,
                        pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA));

        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>(
                Arrays.asList(new AnnotationMetadataBuilder(ROO_JAVA_BEAN),
                        new AnnotationMetadataBuilder(ROO_TO_STRING),
                        new AnnotationMetadataBuilder(EMBEDDABLE)));

        if (serializable) {
            annotations.add(new AnnotationMetadataBuilder(ROO_SERIALIZABLE));
        }

        final int modifier = Modifier.PUBLIC;
        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                declaredByMetadataId, modifier, name,
                PhysicalTypeCategory.CLASS);
        cidBuilder.setAnnotations(annotations);

        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
    }

    public void newEntity(final JavaType name, final boolean createAbstract,
            final JavaType superclass, final JavaType implementsType,
            final List<AnnotationMetadataBuilder> annotations) {
        Validate.notNull(name, "Entity name required");
        Validate.isTrue(
                !JdkJavaType.isPartOfJavaLang(name.getSimpleTypeName()),
                "Entity name '%s' must not be part of java.lang",
                name.getSimpleTypeName());

        int modifier = Modifier.PUBLIC;
        if (createAbstract) {
            modifier |= Modifier.ABSTRACT;
        }

        final String declaredByMetadataId = PhysicalTypeIdentifier
                .createIdentifier(name,
                        pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA));
        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                declaredByMetadataId, modifier, name,
                PhysicalTypeCategory.CLASS);

        if (!superclass.equals(OBJECT)) {
            final ClassOrInterfaceTypeDetails superclassClassOrInterfaceTypeDetails = typeLocationService
                    .getTypeDetails(superclass);
            if (superclassClassOrInterfaceTypeDetails != null) {
                cidBuilder
                        .setSuperclass(new ClassOrInterfaceTypeDetailsBuilder(
                                superclassClassOrInterfaceTypeDetails));
            }
        }

        cidBuilder.setExtendsTypes(Arrays.asList(superclass));

        if (implementsType != null) {
            final Set<JavaType> implementsTypes = new LinkedHashSet<JavaType>();
            final ClassOrInterfaceTypeDetails typeDetails = typeLocationService
                    .getTypeDetails(declaredByMetadataId);
            if (typeDetails != null) {
                implementsTypes.addAll(typeDetails.getImplementsTypes());
            }
            implementsTypes.add(implementsType);
            cidBuilder.setImplementsTypes(implementsTypes);
        }

        cidBuilder.setAnnotations(annotations);

        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
    }

    public void newIdentifier(final JavaType identifierType,
            final String identifierField, final String identifierColumn) {
        Validate.notNull(identifierType, "Identifier type required");

        final String declaredByMetadataId = PhysicalTypeIdentifier
                .createIdentifier(identifierType,
                        pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA));
        final List<AnnotationMetadataBuilder> identifierAnnotations = Arrays
                .asList(new AnnotationMetadataBuilder(ROO_TO_STRING),
                        new AnnotationMetadataBuilder(ROO_EQUALS),
                        new AnnotationMetadataBuilder(ROO_IDENTIFIER));
        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                declaredByMetadataId, Modifier.PUBLIC | Modifier.FINAL,
                identifierType, PhysicalTypeCategory.CLASS);
        cidBuilder.setAnnotations(identifierAnnotations);

        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
    }

    private Properties readProperties(final String path, final boolean exists,
            final String templateFilename) {
        final Properties props = new LinkedProperties();
        InputStream inputStream = null;
        try {
            if (exists) {
                inputStream = fileManager.getInputStream(path);
            }
            else {
                inputStream = FileUtils.getInputStream(getClass(),
                        templateFilename);
            }
            props.load(inputStream);
        }
        catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
        return props;
    }

    private void updateApplicationContext(final OrmProvider ormProvider,
            final JdbcDatabase jdbcDatabase, final String jndi,
            String transactionManager, final String persistenceUnit) {
        final String contextPath = projectOperations.getPathResolver()
                .getFocusedIdentifier(Path.SPRING_CONFIG_ROOT,
                        APPLICATION_CONTEXT_XML);
        final Document appCtx = XmlUtils.readXml(fileManager
                .getInputStream(contextPath));
        final Element root = appCtx.getDocumentElement();

        // Checking for existence of configurations, if found abort
        Element dataSource = XmlUtils.findFirstElement(
                "/beans/bean[@id = 'dataSource']", root);
        Element dataSourceJndi = XmlUtils.findFirstElement(
                "/beans/jndi-lookup[@id = 'dataSource']", root);

        if (ormProvider == OrmProvider.DATANUCLEUS) {
            if (dataSource != null) {
                root.removeChild(dataSource);
            }
            if (dataSourceJndi != null) {
                root.removeChild(dataSourceJndi);
            }
        }
        else if (StringUtils.isBlank(jndi) && dataSource == null) {
            dataSource = appCtx.createElement("bean");
            dataSource.setAttribute("class",
                    "org.apache.commons.dbcp.BasicDataSource");
            dataSource.setAttribute("destroy-method", "close");
            dataSource.setAttribute("id", "dataSource");
            dataSource.appendChild(createPropertyElement("driverClassName",
                    "${database.driverClassName}", appCtx));
            dataSource.appendChild(createPropertyElement("url",
                    "${database.url}", appCtx));
            dataSource.appendChild(createPropertyElement("username",
                    "${database.username}", appCtx));
            dataSource.appendChild(createPropertyElement("password",
                    "${database.password}", appCtx));
            dataSource.appendChild(createPropertyElement("testOnBorrow",
                    "true", appCtx));
            dataSource.appendChild(createPropertyElement("testOnReturn",
                    "true", appCtx));
            dataSource.appendChild(createPropertyElement("testWhileIdle",
                    "true", appCtx));
            dataSource.appendChild(createPropertyElement(
                    "timeBetweenEvictionRunsMillis", "1800000", appCtx));
            dataSource.appendChild(createPropertyElement(
                    "numTestsPerEvictionRun", "3", appCtx));
            dataSource.appendChild(createPropertyElement(
                    "minEvictableIdleTimeMillis", "1800000", appCtx));
            root.appendChild(dataSource);
            if (dataSourceJndi != null) {
                dataSourceJndi.getParentNode().removeChild(dataSourceJndi);
            }
        }
        else if (StringUtils.isNotBlank(jndi)) {
            if (dataSourceJndi == null) {
                dataSourceJndi = appCtx.createElement("jee:jndi-lookup");
                dataSourceJndi.setAttribute("id", "dataSource");
                root.appendChild(dataSourceJndi);
            }
            dataSourceJndi.setAttribute("jndi-name", jndi);
            if (dataSource != null) {
                dataSource.getParentNode().removeChild(dataSource);
            }
        }

        if (dataSource != null) {
            final Element validationQueryElement = XmlUtils.findFirstElement(
                    "property[@name = 'validationQuery']", dataSource);
            if (validationQueryElement != null) {
                dataSource.removeChild(validationQueryElement);
            }
            String validationQuery = "";
            switch (jdbcDatabase) {
            case ORACLE:
                validationQuery = "SELECT 1 FROM DUAL";
                break;
            case POSTGRES:
                validationQuery = "SELECT version();";
                break;
            case MYSQL:
                validationQuery = "SELECT 1";
                break;
            }
            if (StringUtils.isNotBlank(validationQuery)) {
                dataSource.appendChild(createPropertyElement("validationQuery",
                        validationQuery, appCtx));
            }
        }

        transactionManager = StringUtils.defaultIfEmpty(transactionManager,
                "transactionManager");
        Element transactionManagerElement = XmlUtils.findFirstElement(
                "/beans/bean[@id = '" + transactionManager + "']", root);
        if (transactionManagerElement == null) {
            transactionManagerElement = appCtx.createElement("bean");
            transactionManagerElement.setAttribute("id", transactionManager);
            transactionManagerElement.setAttribute("class",
                    JPA_TRANSACTION_MANAGER.getFullyQualifiedTypeName());
            transactionManagerElement.appendChild(createRefElement(
                    "entityManagerFactory", "entityManagerFactory", appCtx));
            root.appendChild(transactionManagerElement);
        }

        Element aspectJTxManager = XmlUtils.findFirstElement(
                "/beans/annotation-driven", root);
        if (aspectJTxManager == null) {
            aspectJTxManager = appCtx.createElement("tx:annotation-driven");
            aspectJTxManager.setAttribute("mode", "aspectj");
            aspectJTxManager.setAttribute("transaction-manager",
                    transactionManager);
            root.appendChild(aspectJTxManager);
        }
        else {
            aspectJTxManager.setAttribute("transaction-manager",
                    transactionManager);
        }

        Element entityManagerFactory = XmlUtils.findFirstElement(
                "/beans/bean[@id = 'entityManagerFactory']", root);
        if (entityManagerFactory != null) {
            root.removeChild(entityManagerFactory);
        }

        entityManagerFactory = appCtx.createElement("bean");
        entityManagerFactory.setAttribute("id", "entityManagerFactory");

        switch (jdbcDatabase) {
        case GOOGLE_APP_ENGINE:
            entityManagerFactory.setAttribute("class",
                    LOCAL_ENTITY_MANAGER_FACTORY_BEAN
                            .getFullyQualifiedTypeName());
            entityManagerFactory
                    .appendChild(createPropertyElement("persistenceUnitName",
                            StringUtils.defaultIfEmpty(persistenceUnit,
                                    GAE_PERSISTENCE_UNIT_NAME), appCtx));
            break;
        default:
            entityManagerFactory.setAttribute("class",
                    LOCAL_CONTAINER_ENTITY_MANAGER_FACTORY_BEAN
                            .getFullyQualifiedTypeName());
            entityManagerFactory
                    .appendChild(createPropertyElement("persistenceUnitName",
                            StringUtils.defaultIfEmpty(persistenceUnit,
                                    DEFAULT_PERSISTENCE_UNIT), appCtx));
            if (ormProvider != OrmProvider.DATANUCLEUS) {
                entityManagerFactory.appendChild(createRefElement("dataSource",
                        "dataSource", appCtx));
            }
            break;
        }

        root.appendChild(entityManagerFactory);

        DomUtils.removeTextNodes(root);

        fileManager.createOrUpdateTextFileIfRequired(contextPath,
                XmlUtils.nodeToString(appCtx), false);
    }

    private void updateBuildPlugins(final Element configuration,
            final OrmProvider ormProvider, final JdbcDatabase jdbcDatabase,
            final String databaseXPath, final String providersXPath,
            final String moduleName) {
        // Identify the required plugins
        final List<Plugin> requiredPlugins = new ArrayList<Plugin>();

        final List<Element> databasePlugins = XmlUtils.findElements(
                jdbcDatabase.getConfigPrefix() + "/plugins/plugin",
                configuration);
        for (final Element pluginElement : databasePlugins) {
            requiredPlugins.add(new Plugin(pluginElement));
        }

        final List<Element> ormPlugins = XmlUtils.findElements(
                ormProvider.getConfigPrefix() + "/plugins/plugin",
                configuration);
        for (final Element pluginElement : ormPlugins) {
            requiredPlugins.add(new Plugin(pluginElement));
        }

        // Identify any redundant plugins
        final List<Plugin> redundantPlugins = new ArrayList<Plugin>();
        redundantPlugins.addAll(getPlugins(databaseXPath, configuration));
        redundantPlugins.addAll(getPlugins(providersXPath, configuration));
        // Don't remove any that are still required
        redundantPlugins.removeAll(requiredPlugins);

        // Update the POM
        projectOperations.addBuildPlugins(moduleName, requiredPlugins);
        projectOperations.removeBuildPlugins(moduleName, redundantPlugins);

        if (jdbcDatabase == JdbcDatabase.GOOGLE_APP_ENGINE) {
            updateEclipsePlugin(true);
            updateDataNucleusPlugin(true);
            projectOperations.updateDependencyScope(moduleName,
                    JSTL_IMPL_DEPENDENCY, DependencyScope.PROVIDED);
        }
    }

    private void updateDatabaseDotComConfigProperties(
            final OrmProvider ormProvider, final JdbcDatabase jdbcDatabase,
            final String hostName, final String userName,
            final String password, final String persistenceUnit,
            final String moduleName) {
        final String configPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_RESOURCES, persistenceUnit + ".properties");
        final boolean configExists = fileManager.exists(configPath);

        if (jdbcDatabase != JdbcDatabase.DATABASE_DOT_COM) {
            if (configExists) {
                fileManager.delete(configPath,
                        "database is " + jdbcDatabase.name());
            }
            return;
        }

        final String connectionString = getConnectionString(jdbcDatabase,
                hostName, null /* databaseName */, moduleName).replace(
                "USER_NAME",
                StringUtils.defaultIfEmpty(userName, "${userName}"))
                .replace("PASSWORD",
                        StringUtils.defaultIfEmpty(password, "${password}"));
        final Properties props = readProperties(configPath, configExists,
                "database-dot-com-template.properties");

        final boolean hasChanged = !props.get("url").equals(
                StringUtils.stripToEmpty(connectionString));
        if (!hasChanged) {
            return;
        }

        props.put("url", StringUtils.stripToEmpty(connectionString));

        writeProperties(configPath, configExists, props);

        LOGGER.warning("Please update your database details in src/main/resources/"
                + persistenceUnit + ".properties.");
    }

    private void updateDatabaseProperties(final OrmProvider ormProvider,
            final JdbcDatabase jdbcDatabase, final String hostName,
            final String databaseName, String userName, final String password,
            final String moduleName) {
        final String databasePath = getDatabasePropertiesPath();
        final boolean databaseExists = fileManager.exists(databasePath);

        if (ormProvider == OrmProvider.DATANUCLEUS) {
            if (databaseExists) {
                fileManager.delete(databasePath, "ORM provider is "
                        + ormProvider.name());
            }
            return;
        }

        final String jndiPath = getJndiPropertiesPath();
        if (fileManager.exists(jndiPath)) {
            fileManager.delete(jndiPath, "JNDI is not used");
        }

        final Properties props = readProperties(databasePath, databaseExists,
                "database-template.properties");

        final String connectionString = getConnectionString(jdbcDatabase,
                hostName, databaseName, moduleName);
        if (jdbcDatabase.getKey().equals("HYPERSONIC")
                || jdbcDatabase == JdbcDatabase.H2_IN_MEMORY
                || jdbcDatabase == JdbcDatabase.SYBASE) {
            userName = StringUtils.defaultIfEmpty(userName, "sa");
        }

        final String driver = props.getProperty(DATABASE_DRIVER);
        final String url = props.getProperty(DATABASE_URL);
        final String uname = props.getProperty(DATABASE_USERNAME);
        final String pwd = props.getProperty(DATABASE_PASSWORD);

        boolean hasChanged = driver == null
                || !driver.equals(jdbcDatabase.getDriverClassName());
        hasChanged |= url == null || !url.equals(connectionString);
        hasChanged |= uname == null
                || !uname.equals(StringUtils.stripToEmpty(userName));
        hasChanged |= pwd == null
                || !pwd.equals(StringUtils.stripToEmpty(password));
        if (!hasChanged) {
            // No changes from existing database configuration so exit now
            return;
        }

        // Write changes to database.properties file
        props.put(DATABASE_URL, connectionString);
        props.put(DATABASE_DRIVER, jdbcDatabase.getDriverClassName());
        props.put(DATABASE_USERNAME, StringUtils.stripToEmpty(userName));
        props.put(DATABASE_PASSWORD, StringUtils.stripToEmpty(password));

        writeProperties(databasePath, databaseExists, props);

        // Log message to console
        switch (jdbcDatabase) {
        case ORACLE:
        case DB2_EXPRESS_C:
        case DB2_400:
            LOGGER.warning("The "
                    + jdbcDatabase.name()
                    + " JDBC driver is not available in public Maven repositories. Please adjust the pom.xml dependency to suit your needs");
            break;
        case POSTGRES:
        case DERBY_EMBEDDED:
        case DERBY_CLIENT:
        case MSSQL:
        case SYBASE:
        case MYSQL:
            LOGGER.warning("Please update your database details in src/main/resources/META-INF/spring/database.properties.");
            break;
        }
    }

    private void updateDataNucleusPlugin(final boolean addToPlugin) {
        final String pom = pathResolver
                .getFocusedIdentifier(Path.ROOT, POM_XML);
        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(pom));
        final Element root = document.getDocumentElement();

        // Manage mappingExcludes
        final Element configurationElement = XmlUtils
                .findFirstElement(
                        "/project/build/plugins/plugin[artifactId = 'maven-datanucleus-plugin']/configuration",
                        root);
        if (configurationElement == null) {
            return;
        }

        String descriptionOfChange = "";
        Element mappingExcludesElement = XmlUtils.findFirstElement(
                "mappingExcludes", configurationElement);
        if (addToPlugin && mappingExcludesElement == null) {
            mappingExcludesElement = new XmlElementBuilder("mappingExcludes",
                    document)
                    .setText(
                            "**/CustomRequestFactoryServlet.class, **/GaeAuthFilter.class")
                    .build();
            configurationElement.appendChild(mappingExcludesElement);
            descriptionOfChange = "added GAEAuthFilter mappingExcludes to maven-datanuclueus-plugin";
        }
        else if (!addToPlugin && mappingExcludesElement != null) {
            configurationElement.removeChild(mappingExcludesElement);
            descriptionOfChange = "removed GAEAuthFilter mappingExcludes from maven-datanuclueus-plugin";
        }

        fileManager.createOrUpdateTextFileIfRequired(pom,
                XmlUtils.nodeToString(document), descriptionOfChange, false);
    }

    /**
     * Updates the POM with the dependencies required for the given database and
     * ORM provider, removing any other persistence-related dependencies
     * 
     * @param configuration
     * @param ormProvider
     * @param jdbcDatabase
     * @param databaseXPath
     * @param providersXPath
     */
    private void updateDependencies(final Element configuration,
            final OrmProvider ormProvider, final JdbcDatabase jdbcDatabase,
            final String databaseXPath, final String providersXPath,
            final String moduleName) {
        final List<Dependency> requiredDependencies = new ArrayList<Dependency>();

        final List<Element> databaseDependencies = XmlUtils.findElements(
                jdbcDatabase.getConfigPrefix() + "/dependencies/dependency",
                configuration);
        for (final Element dependencyElement : databaseDependencies) {
            requiredDependencies.add(new Dependency(dependencyElement));
        }

        final List<Element> ormDependencies = XmlUtils.findElements(
                ormProvider.getConfigPrefix() + "/dependencies/dependency",
                configuration);
        for (final Element dependencyElement : ormDependencies) {
            requiredDependencies.add(new Dependency(dependencyElement));
        }

        // Hard coded to JPA & Hibernate Validator for now
        final List<Element> jpaDependencies = XmlUtils
                .findElements(
                        "/configuration/persistence/provider[@id = 'JPA']/dependencies/dependency",
                        configuration);
        for (final Element dependencyElement : jpaDependencies) {
            requiredDependencies.add(new Dependency(dependencyElement));
        }

        final List<Element> springDependencies = XmlUtils.findElements(
                "/configuration/spring/dependencies/dependency", configuration);
        for (final Element dependencyElement : springDependencies) {
            requiredDependencies.add(new Dependency(dependencyElement));
        }

        // Remove redundant dependencies
        final List<Dependency> redundantDependencies = new ArrayList<Dependency>();
        redundantDependencies.addAll(getDependencies(databaseXPath,
                configuration, moduleName));
        redundantDependencies.addAll(getDependencies(providersXPath,
                configuration, moduleName));
        // Don't remove any we actually need
        redundantDependencies.removeAll(requiredDependencies);

        // Update the POM
        projectOperations.addDependencies(moduleName, requiredDependencies);
        projectOperations.removeDependencies(moduleName, redundantDependencies);
    }

    private void updateEclipsePlugin(final boolean addGaeSettingsToPlugin) {
        final String pom = pathResolver
                .getFocusedIdentifier(Path.ROOT, POM_XML);
        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(pom));
        final Collection<String> changes = new ArrayList<String>();

        manageGaeBuildCommand(addGaeSettingsToPlugin, document, changes);
        manageGaeProjectNature(addGaeSettingsToPlugin, document, changes);

        if (!changes.isEmpty()) {
            final String changesMessage = StringUtils.join(changes, "; ");
            fileManager.createOrUpdateTextFileIfRequired(pom,
                    XmlUtils.nodeToString(document), changesMessage, false);
        }
    }

    private void updateFilters(final Element configuration,
            final OrmProvider ormProvider, final JdbcDatabase jdbcDatabase,
            final String databaseXPath, final String providersXPath,
            final String moduleName) {
        // Remove redundant filters
        final List<Filter> redundantFilters = new ArrayList<Filter>();
        redundantFilters.addAll(getFilters(databaseXPath, configuration));
        redundantFilters.addAll(getFilters(providersXPath, configuration));
        for (final Filter filter : redundantFilters) {
            projectOperations.removeFilter(moduleName, filter);
        }

        // Add required filters
        final List<Filter> filters = new ArrayList<Filter>();

        final List<Element> databaseFilters = XmlUtils.findElements(
                jdbcDatabase.getConfigPrefix() + "/filters/filter",
                configuration);
        for (final Element filterElement : databaseFilters) {
            filters.add(new Filter(filterElement));
        }

        final List<Element> ormFilters = XmlUtils.findElements(
                ormProvider.getConfigPrefix() + "/filters/filter",
                configuration);
        for (final Element filterElement : ormFilters) {
            filters.add(new Filter(filterElement));
        }

        for (final Filter filter : filters) {
            projectOperations.addFilter(moduleName, filter);
        }
    }

    private void updateJndiProperties() {
        final String databasePath = getDatabasePropertiesPath();
        if (fileManager.exists(databasePath)) {
            fileManager.delete(databasePath, "JNDI is used");
        }

        final String jndiPath = getJndiPropertiesPath();
        if (fileManager.exists(jndiPath)) {
            return;
        }

        final Properties props = readProperties(jndiPath, false,
                "jndi-template.properties");
        writeProperties(jndiPath, false, props);
        LOGGER.warning("Please update your JNDI details in src/main/resources/META-INF/spring/jndi.properties.");
    }

    private void updateLog4j(final OrmProvider ormProvider) {
        final String log4jPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_RESOURCES, "log4j.properties");
        if (!fileManager.exists(log4jPath)) {
            return;
        }

        final MutableFile log4jMutableFile = fileManager.updateFile(log4jPath);
        final Properties props = new Properties();
        OutputStream outputStream = null;
        try {
            props.load(log4jMutableFile.getInputStream());
            final String dnKey = "log4j.category.DataNucleus";
            if (ormProvider == OrmProvider.DATANUCLEUS
                    && !props.containsKey(dnKey)) {
                outputStream = log4jMutableFile.getOutputStream();
                props.put(dnKey, "WARN");
                props.store(outputStream, "Updated at " + new Date());
            }
            else if (ormProvider != OrmProvider.DATANUCLEUS
                    && props.containsKey(dnKey)) {
                outputStream = log4jMutableFile.getOutputStream();
                props.remove(dnKey);
                props.store(outputStream, "Updated at " + new Date());
            }
        }
        catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        finally {
            IOUtils.closeQuietly(outputStream);
        }
    }

    private void updatePersistenceXml(final OrmProvider ormProvider,
            final JdbcDatabase jdbcDatabase, final String hostName,
            final String databaseName, String userName, final String password,
            final String persistenceUnit, final String moduleName) {
        final String persistencePath = getPersistencePathOfFocussedModule();
        final InputStream inputStream;
        if (fileManager.exists(persistencePath)) {
            // There's an existing persistence config file; read it
            inputStream = fileManager.getInputStream(persistencePath);
        }
        else {
            // Use the addon's template file
            inputStream = FileUtils.getInputStream(getClass(),
                    "persistence-template.xml");
        }

        final Document persistence = XmlUtils.readXml(inputStream);
        final Element root = persistence.getDocumentElement();
        final Element persistenceElement = XmlUtils.findFirstElement(
                "/persistence", root);
        Validate.notNull(persistenceElement, "No persistence element found");

        Element persistenceUnitElement;
        if (StringUtils.isNotBlank(persistenceUnit)) {
            persistenceUnitElement = XmlUtils
                    .findFirstElement(PERSISTENCE_UNIT + "[@name = '"
                            + persistenceUnit + "']", persistenceElement);
        }
        else {
            persistenceUnitElement = XmlUtils
                    .findFirstElement(
                            PERSISTENCE_UNIT
                                    + "[@name = '"
                                    + (jdbcDatabase == JdbcDatabase.GOOGLE_APP_ENGINE ? GAE_PERSISTENCE_UNIT_NAME
                                            : DEFAULT_PERSISTENCE_UNIT) + "']",
                            persistenceElement);
        }

        if (persistenceUnitElement != null) {
            while (persistenceUnitElement.getFirstChild() != null) {
                persistenceUnitElement.removeChild(persistenceUnitElement
                        .getFirstChild());
            }
        }
        else {
            persistenceUnitElement = persistence
                    .createElement(PERSISTENCE_UNIT);
            persistenceElement.appendChild(persistenceUnitElement);
        }

        // Add provider element
        final Element provider = persistence.createElement("provider");
        switch (jdbcDatabase) {
        case GOOGLE_APP_ENGINE:
            persistenceUnitElement
                    .setAttribute("name", StringUtils.defaultIfEmpty(
                            persistenceUnit, GAE_PERSISTENCE_UNIT_NAME));
            persistenceUnitElement.removeAttribute("transaction-type");
            provider.setTextContent(ormProvider.getAdapter());
            break;
        case DATABASE_DOT_COM:
            persistenceUnitElement.setAttribute("name", StringUtils
                    .defaultIfEmpty(persistenceUnit, DEFAULT_PERSISTENCE_UNIT));
            persistenceUnitElement.removeAttribute("transaction-type");
            provider.setTextContent("com.force.sdk.jpa.PersistenceProviderImpl");
            break;
        default:
            persistenceUnitElement.setAttribute("name", StringUtils
                    .defaultIfEmpty(persistenceUnit, DEFAULT_PERSISTENCE_UNIT));
            persistenceUnitElement.setAttribute("transaction-type",
                    "RESOURCE_LOCAL");
            provider.setTextContent(ormProvider.getAdapter());
            break;
        }
        persistenceUnitElement.appendChild(provider);

        // Add properties
        final Properties dialects = propFileOperations.loadProperties(
                JPA_DIALECTS_FILE, getClass());
        final Element properties = persistence.createElement("properties");
        final boolean isDbreProject = fileManager.exists(pathResolver
                .getFocusedIdentifier(Path.SRC_MAIN_RESOURCES, "dbre.xml"));
        final boolean isDbreProjectOrDB2400 = isDbreProject
                || jdbcDatabase == JdbcDatabase.DB2_400;

        switch (ormProvider) {
        case HIBERNATE:
            final String dialectKey = ormProvider.name() + "."
                    + jdbcDatabase.name();
            properties.appendChild(createPropertyElement("hibernate.dialect",
                    dialects.getProperty(dialectKey), persistence));
            properties
                    .appendChild(persistence
                            .createComment(" value=\"create\" to build a new database on each run; value=\"update\" to modify an existing database; value=\"create-drop\" means the same as \"create\" but also drops tables when Hibernate closes; value=\"validate\" makes no changes to the database ")); // ROO-627
            properties
                    .appendChild(createPropertyElement(
                            "hibernate.hbm2ddl.auto",
                            isDbreProjectOrDB2400 ? "validate" : "create",
                            persistence));
            properties.appendChild(createPropertyElement(
                    "hibernate.ejb.naming_strategy",
                    "org.hibernate.cfg.ImprovedNamingStrategy", persistence));
            properties.appendChild(createPropertyElement(
                    "hibernate.connection.charSet", "UTF-8", persistence));
            properties
                    .appendChild(persistence
                            .createComment(" Uncomment the following two properties for JBoss only "));
            properties
                    .appendChild(persistence
                            .createComment(" property name=\"hibernate.validator.apply_to_ddl\" value=\"false\" /"));
            properties
                    .appendChild(persistence
                            .createComment(" property name=\"hibernate.validator.autoregister_listeners\" value=\"false\" /"));
            break;
        case OPENJPA:
            properties.appendChild(createPropertyElement(
                    "openjpa.jdbc.DBDictionary",
                    dialects.getProperty(ormProvider.name() + "."
                            + jdbcDatabase.name()), persistence));
            properties
                    .appendChild(persistence
                            .createComment(" value=\"buildSchema\" to runtime forward map the DDL SQL; value=\"validate\" makes no changes to the database ")); // ROO-627
            properties.appendChild(createPropertyElement(
                    "openjpa.jdbc.SynchronizeMappings",
                    isDbreProjectOrDB2400 ? "validate" : "buildSchema",
                    persistence));
            properties.appendChild(createPropertyElement(
                    "openjpa.RuntimeUnenhancedClasses", "supported",
                    persistence));
            break;
        case ECLIPSELINK:
            properties.appendChild(createPropertyElement(
                    "eclipselink.target-database",
                    dialects.getProperty(ormProvider.name() + "."
                            + jdbcDatabase.name()), persistence));
            properties
                    .appendChild(persistence
                            .createComment(" value=\"drop-and-create-tables\" to build a new database on each run; value=\"create-tables\" creates new tables if needed; value=\"none\" makes no changes to the database ")); // ROO-627
            properties.appendChild(createPropertyElement(
                    "eclipselink.ddl-generation",
                    isDbreProjectOrDB2400 ? "none" : "drop-and-create-tables",
                    persistence));
            properties.appendChild(createPropertyElement(
                    "eclipselink.ddl-generation.output-mode", "database",
                    persistence));
            properties.appendChild(createPropertyElement("eclipselink.weaving",
                    "static", persistence));
            break;
        case DATANUCLEUS:
            String connectionString = getConnectionString(jdbcDatabase,
                    hostName, databaseName, moduleName);
            switch (jdbcDatabase) {
            case GOOGLE_APP_ENGINE:
                properties
                        .appendChild(createPropertyElement(
                                "datanucleus.NontransactionalRead", "true",
                                persistence));
                properties.appendChild(createPropertyElement(
                        "datanucleus.NontransactionalWrite", "true",
                        persistence));
                properties.appendChild(createPropertyElement(
                        "datanucleus.autoCreateSchema", "false", persistence));
                break;
            case DATABASE_DOT_COM:
                properties.appendChild(createPropertyElement(
                        "datanucleus.storeManagerType", "force", persistence));
                properties.appendChild(createPropertyElement(
                        "datanucleus.Optimistic", "false", persistence));
                properties.appendChild(createPropertyElement(
                        "datanucleus.datastoreTransactionDelayOperations",
                        "true", persistence));
                properties.appendChild(createPropertyElement(
                        "datanucleus.autoCreateSchema",
                        Boolean.toString(!isDbreProject), persistence));
                break;
            default:
                properties.appendChild(createPropertyElement(
                        "datanucleus.ConnectionDriverName",
                        jdbcDatabase.getDriverClassName(), persistence));
                properties.appendChild(createPropertyElement(
                        "datanucleus.autoCreateSchema",
                        Boolean.toString(!isDbreProject), persistence));
                connectionString = connectionString.replace(
                        "TO_BE_CHANGED_BY_ADDON",
                        projectOperations.getProjectName(moduleName));
                if (jdbcDatabase.getKey().equals("HYPERSONIC")
                        || jdbcDatabase == JdbcDatabase.H2_IN_MEMORY
                        || jdbcDatabase == JdbcDatabase.SYBASE) {
                    userName = StringUtils.defaultIfEmpty(userName, "sa");
                }
                properties.appendChild(createPropertyElement(
                        "datanucleus.storeManagerType", "rdbms", persistence));
            }

            if (jdbcDatabase != JdbcDatabase.DATABASE_DOT_COM) {
                // These are specified in the connection properties file
                properties.appendChild(createPropertyElement(
                        "datanucleus.ConnectionURL", connectionString,
                        persistence));
                properties
                        .appendChild(createPropertyElement(
                                "datanucleus.ConnectionUserName", userName,
                                persistence));
                properties
                        .appendChild(createPropertyElement(
                                "datanucleus.ConnectionPassword", password,
                                persistence));
            }

            properties.appendChild(createPropertyElement(
                    "datanucleus.autoCreateTables",
                    Boolean.toString(!isDbreProject), persistence));
            properties.appendChild(createPropertyElement(
                    "datanucleus.autoCreateColumns", "false", persistence));
            properties.appendChild(createPropertyElement(
                    "datanucleus.autoCreateConstraints", "false", persistence));
            properties.appendChild(createPropertyElement(
                    "datanucleus.validateTables", "false", persistence));
            properties.appendChild(createPropertyElement(
                    "datanucleus.validateConstraints", "false", persistence));
            properties
                    .appendChild(createPropertyElement(
                            "datanucleus.jpa.addClassTransformer", "false",
                            persistence));
            break;
        }

        persistenceUnitElement.appendChild(properties);

        fileManager.createOrUpdateTextFileIfRequired(persistencePath,
                XmlUtils.nodeToString(persistence), false);

        if (jdbcDatabase != JdbcDatabase.GOOGLE_APP_ENGINE
                && ormProvider == OrmProvider.DATANUCLEUS) {
            LOGGER.warning("Please update your database details in src/main/resources/META-INF/persistence.xml.");
        }
    }

    private void updatePluginRepositories(final Element configuration,
            final OrmProvider ormProvider, final JdbcDatabase jdbcDatabase,
            final String moduleName) {
        final List<Repository> pluginRepositories = new ArrayList<Repository>();

        final List<Element> databasePluginRepositories = XmlUtils
                .findElements(jdbcDatabase.getConfigPrefix()
                        + "/pluginRepositories/pluginRepository", configuration);
        for (final Element pluginRepositoryElement : databasePluginRepositories) {
            pluginRepositories.add(new Repository(pluginRepositoryElement));
        }

        final List<Element> ormPluginRepositories = XmlUtils
                .findElements(ormProvider.getConfigPrefix()
                        + "/pluginRepositories/pluginRepository", configuration);
        for (final Element pluginRepositoryElement : ormPluginRepositories) {
            pluginRepositories.add(new Repository(pluginRepositoryElement));
        }

        // Add all new plugin repositories to pom.xml
        projectOperations.addPluginRepositories(moduleName, pluginRepositories);
    }

    private void updatePomProperties(final Element configuration,
            final OrmProvider ormProvider, final JdbcDatabase jdbcDatabase,
            final String moduleName) {
        final List<Element> databaseProperties = XmlUtils
                .findElements(jdbcDatabase.getConfigPrefix() + "/properties/*",
                        configuration);
        for (final Element property : databaseProperties) {
            projectOperations.addProperty(moduleName, new Property(property));
        }

        final List<Element> providerProperties = XmlUtils.findElements(
                ormProvider.getConfigPrefix() + "/properties/*", configuration);
        for (final Element property : providerProperties) {
            projectOperations.addProperty(moduleName, new Property(property));
        }
    }

    private void updateRepositories(final Element configuration,
            final OrmProvider ormProvider, final JdbcDatabase jdbcDatabase,
            final String moduleName) {
        final List<Repository> repositories = new ArrayList<Repository>();

        final List<Element> databaseRepositories = XmlUtils.findElements(
                jdbcDatabase.getConfigPrefix() + "/repositories/repository",
                configuration);
        for (final Element repositoryElement : databaseRepositories) {
            repositories.add(new Repository(repositoryElement));
        }

        final List<Element> ormRepositories = XmlUtils.findElements(
                ormProvider.getConfigPrefix() + "/repositories/repository",
                configuration);
        for (final Element repositoryElement : ormRepositories) {
            repositories.add(new Repository(repositoryElement));
        }

        final List<Element> jpaRepositories = XmlUtils
                .findElements(
                        "/configuration/persistence/provider[@id='JPA']/repositories/repository",
                        configuration);
        for (final Element repositoryElement : jpaRepositories) {
            repositories.add(new Repository(repositoryElement));
        }

        // Add all new repositories to pom.xml
        projectOperations.addRepositories(moduleName, repositories);
    }

    private void updateResources(final Element configuration,
            final OrmProvider ormProvider, final JdbcDatabase jdbcDatabase,
            final String databaseXPath, final String providersXPath,
            final String moduleName) {
        // Remove redundant resources
        final List<Resource> redundantResources = new ArrayList<Resource>();
        redundantResources.addAll(getResources(databaseXPath, configuration));
        redundantResources.addAll(getResources(providersXPath, configuration));
        for (final Resource resource : redundantResources) {
            projectOperations.removeResource(moduleName, resource);
        }

        // Add required resources
        final List<Resource> resources = new ArrayList<Resource>();

        final List<Element> databaseResources = XmlUtils.findElements(
                jdbcDatabase.getConfigPrefix() + "/resources/resource",
                configuration);
        for (final Element resourceElement : databaseResources) {
            resources.add(new Resource(resourceElement));
        }

        final List<Element> ormResources = XmlUtils.findElements(
                ormProvider.getConfigPrefix() + "/resources/resource",
                configuration);
        for (final Element resourceElement : ormResources) {
            resources.add(new Resource(resourceElement));
        }

        for (final Resource resource : resources) {
            projectOperations.addResource(moduleName, resource);
        }
    }

    private void writeProperties(final String path, final boolean exists,
            final Properties props) {
        OutputStream outputStream = null;
        try {
            final MutableFile mutableFile = exists ? fileManager
                    .updateFile(path) : fileManager.createFile(path);
            outputStream = mutableFile == null ? new FileOutputStream(path)
                    : mutableFile.getOutputStream();
            props.store(outputStream, "Updated at " + new Date());
        }
        catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        finally {
            IOUtils.closeQuietly(outputStream);
        }
    }
}
