package org.springframework.roo.addon.jpa.addon;

import static org.springframework.roo.model.JavaType.OBJECT;
import static org.springframework.roo.model.JpaJavaType.EMBEDDABLE;
import static org.springframework.roo.model.RooJavaType.ROO_EQUALS;
import static org.springframework.roo.model.RooJavaType.ROO_IDENTIFIER;
import static org.springframework.roo.model.RooJavaType.ROO_JAVA_BEAN;
import static org.springframework.roo.model.RooJavaType.ROO_SERIALIZABLE;
import static org.springframework.roo.model.RooJavaType.ROO_TO_STRING;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
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
import org.springframework.roo.application.config.ApplicationConfigService;
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
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link JpaOperations}.
 * 
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @author Juan Carlos García
 * @since 1.0
 */
@Component
@Service
public class JpaOperationsImpl implements JpaOperations {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(JpaOperationsImpl.class);
	
	// ------------ OSGi component attributes ----------------
   	private BundleContext context;
   	
    protected void activate(final ComponentContext context) {
    	this.context = context.getBundleContext();
    }


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

    private static final String DATASOURCE_PREFIX = "spring.datasource";
    private static final String DATABASE_DRIVER = "driver-class-name";
    private static final String DATABASE_PASSWORD = "password";
    private static final String DATABASE_URL = "url";
    private static final String DATABASE_USERNAME = "username";
    private static final String JNDI_NAME = "jndi-name";
    static final String POM_XML = "pom.xml";

    private FileManager fileManager;
    private PathResolver pathResolver;
    private ProjectOperations projectOperations;
    private TypeLocationService typeLocationService;
    private TypeManagementService typeManagementService;
    private ApplicationConfigService applicationConfigService;

    public void configureJpa(final OrmProvider ormProvider,
            final JdbcDatabase jdbcDatabase, final String jndi,
            final String hostName, final String databaseName,
            final String userName, final String password,
            final String moduleName, final String profile,
            final boolean force) {
    	
    	if(projectOperations == null){
    		projectOperations = getProjectOperations();
    	}
    	Validate.notNull(projectOperations, "ProjectOperations is required");
    	
        Validate.notNull(ormProvider, "ORM provider required");
        Validate.notNull(jdbcDatabase, "JDBC database required");

       // Parse the configuration.xml file
       final Element configuration = XmlUtils.getConfiguration(getClass());
        
        // Get the first part of the XPath expressions for unwanted databases
        // and ORM providers
        final String databaseXPath = getDbXPath(getUnwantedDatabases(jdbcDatabase));
        final String providersXPath = getProviderXPath(getUnwantedOrmProviders(ormProvider));
        
        // Updating pom.xml including necessary properties, dependencies and Spring Boot starters
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
        
        // Update Spring Config File with spring.datasource.* domain properties
        updateApplicationProperties(ormProvider, jdbcDatabase, hostName,
                databaseName, userName, password, moduleName, jndi, profile, force);
        
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
    	
    	if(projectOperations == null){
    		projectOperations = getProjectOperations();
    	}
    	Validate.notNull(projectOperations, "ProjectOperations is required");
    	
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

    public SortedSet<String> getDatabaseProperties(String profile) {
    	
    	if(projectOperations == null){
    		projectOperations = getProjectOperations();
    	}
    	Validate.notNull(projectOperations, "ProjectOperations is required");
    	
        if (hasDatabaseProperties()) {
            return getApplicationConfigService().getPropertyKeys(DATASOURCE_PREFIX, true, profile);
        }
        return getPropertiesFromDataNucleusConfiguration();
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
    	
    	if(projectOperations == null){
    		projectOperations = getProjectOperations();
    	}
    	Validate.notNull(projectOperations, "ProjectOperations is required");
    	
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

    public String getName() {
        return FeatureNames.JPA;
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
    	
    	if(projectOperations == null){
    		projectOperations = getProjectOperations();
    	}
    	Validate.notNull(projectOperations, "ProjectOperations is required");
    	
        return projectOperations.getProjectName(moduleName);
    }

    private SortedSet<String> getPropertiesFromDataNucleusConfiguration() {
    	
    	if(fileManager == null){
    		fileManager = getFileManager();
    	}
    	Validate.notNull(fileManager, "FileManager is required");
    	
    	if(pathResolver == null){
    		pathResolver = getPathResolver();
    	}
    	Validate.notNull(pathResolver, "PathResolver is required");
    	
        /*final String persistenceXmlPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_RESOURCES, PERSISTENCE_XML);*/
    	final String persistenceXmlPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_RESOURCES, "");
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
        SortedSet<String> databaseProperties = getApplicationConfigService()
                .getPropertyKeys(DATASOURCE_PREFIX, false, null);
    	
        return !databaseProperties.isEmpty();
    }

    public boolean isInstalledInModule(final String moduleName) {
    	
    	if(fileManager == null){
    		fileManager = getFileManager();
    	}
    	Validate.notNull(fileManager, "FileManager is required");
    	
    	if(pathResolver == null){
    		pathResolver = getPathResolver();
    	}
    	Validate.notNull(pathResolver, "PathResolver is required");
    	
    	if(projectOperations == null){
    		projectOperations = getProjectOperations();
    	}
    	Validate.notNull(projectOperations, "ProjectOperations is required");
    	
        final LogicalPath resourcesPath = LogicalPath.getInstance(
                Path.SRC_MAIN_RESOURCES, moduleName);
        return isJpaInstallationPossible();
    }

    public boolean isJpaInstallationPossible() {
    	
    	if(projectOperations == null){
    		projectOperations = getProjectOperations();
    	}
    	Validate.notNull(projectOperations, "ProjectOperations is required");
    	
        return projectOperations.isFocusedProjectAvailable();
    }

    public boolean isPersistentClassAvailable() {
    	
    	if(projectOperations == null){
    		projectOperations = getProjectOperations();
    	}
    	Validate.notNull(projectOperations, "ProjectOperations is required");
    	
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
    	
    	if(fileManager == null){
    		fileManager = getFileManager();
    	}
    	Validate.notNull(fileManager, "FileManager is required");
    	
    	if(pathResolver == null){
    		pathResolver = getPathResolver();
    	}
    	Validate.notNull(pathResolver, "PathResolver is required");
    	
        final String appenginePath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/appengine-web.xml");
        final boolean appenginePathExists = fileManager.exists(appenginePath);

        final String loggingPropertiesPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/logging.properties");
        final boolean loggingPropertiesPathExists = fileManager
                .exists(loggingPropertiesPath);

        if (appenginePathExists) {
            fileManager.delete(appenginePath,
                    "database is " + jdbcDatabase.name());
        }
        if (loggingPropertiesPathExists) {
            fileManager.delete(loggingPropertiesPath, "database is "
                    + jdbcDatabase.name());
        }
    }

    public void newEmbeddableClass(final JavaType name,
            final boolean serializable) {
    	
    	if(pathResolver == null){
    		pathResolver = getPathResolver();
    	}
    	Validate.notNull(pathResolver, "PathResolver is required");
    	
    	if(typeManagementService == null){
    		typeManagementService = getTypeManagementService();
    	}
    	Validate.notNull(typeManagementService, "TypeManagementService is required");
    	
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
    	
    	if(pathResolver == null){
    		pathResolver = getPathResolver();
    	}
    	Validate.notNull(pathResolver, "PathResolver is required");
    	
    	if(typeLocationService == null){
    		typeLocationService = getTypeLocationService();
    	}
    	Validate.notNull(typeLocationService, "TypeLocationService is required");
    	
    	if(typeManagementService == null){
    		typeManagementService = getTypeManagementService();
    	}
    	Validate.notNull(typeManagementService, "TypeManagementService is required");
    	
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
    	
    	if(pathResolver == null){
    		pathResolver = getPathResolver();
    	}
    	Validate.notNull(pathResolver, "PathResolver is required");
    	
    	if(typeManagementService == null){
    		typeManagementService = getTypeManagementService();
    	}
    	Validate.notNull(typeManagementService, "TypeManagementService is required");
    	
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

    private void updateBuildPlugins(final Element configuration,
            final OrmProvider ormProvider, final JdbcDatabase jdbcDatabase,
            final String databaseXPath, final String providersXPath,
            final String moduleName) {
    	
    	if(projectOperations == null){
    		projectOperations = getProjectOperations();
    	}
    	Validate.notNull(projectOperations, "ProjectOperations is required");
    	
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

    }

    private void updateDatabaseDotComConfigProperties(
            final OrmProvider ormProvider, final JdbcDatabase jdbcDatabase,
            final String hostName, final String userName,
            final String password, final String persistenceUnit,
            final String moduleName) {
    	
    	if(fileManager == null){
    		fileManager = getFileManager();
    	}
    	Validate.notNull(fileManager, "FileManager is required");
    	
    	if(pathResolver == null){
    		pathResolver = getPathResolver();
    	}
    	Validate.notNull(pathResolver, "PathResolver is required");
    	
        final String configPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_RESOURCES, persistenceUnit + ".properties");
        final boolean configExists = fileManager.exists(configPath);

        if (configExists) {
            fileManager.delete(configPath,
                    "database is " + jdbcDatabase.name());
        }
    }

    private void updateApplicationProperties(final OrmProvider ormProvider,
            final JdbcDatabase jdbcDatabase, final String hostName,
            final String databaseName, String userName, final String password,
            final String moduleName, String jndi, String profile, boolean force) {
    	
        // Check if jndi is blank. If is blank, include database properties on 
        // application.properties file
        if(StringUtils.isBlank(jndi)){
            
            final String connectionString = getConnectionString(jdbcDatabase,
                    hostName, databaseName, moduleName);
            if (jdbcDatabase.getKey().equals("HYPERSONIC")
                    || jdbcDatabase == JdbcDatabase.H2_IN_MEMORY
                    || jdbcDatabase == JdbcDatabase.SYBASE) {
                userName = StringUtils.defaultIfEmpty(userName, "sa");
            }

            // Getting current properties
            final String driver = getApplicationConfigService().getProperty(DATASOURCE_PREFIX, DATABASE_DRIVER);
            final String url = getApplicationConfigService().getProperty(DATASOURCE_PREFIX, DATABASE_URL);
            final String uname = getApplicationConfigService().getProperty(DATASOURCE_PREFIX, DATABASE_USERNAME);
            final String pwd = getApplicationConfigService().getProperty(DATASOURCE_PREFIX, DATABASE_PASSWORD);

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
            
            // Write changes to Spring Config file
            Map<String, String> props = new HashMap<String, String>();
            props.put(DATABASE_URL, connectionString);
            props.put(DATABASE_DRIVER, jdbcDatabase.getDriverClassName());
            if(userName != null){
                props.put(DATABASE_USERNAME, StringUtils.stripToEmpty(userName));
            }
            if(password != null){
                props.put(DATABASE_PASSWORD, StringUtils.stripToEmpty(password));
            }
            
            getApplicationConfigService().addProperties(DATASOURCE_PREFIX, props, profile, force);

            // Remove jndi property
            getApplicationConfigService().removeProperty(DATASOURCE_PREFIX, JNDI_NAME, profile);
            
        }else{
            
            final String jndiProperty = getApplicationConfigService().getProperty(DATASOURCE_PREFIX, JNDI_NAME);
            
            boolean hasChanged = jndiProperty == null || 
                    !jndiProperty.equals(StringUtils.stripToEmpty(jndi));
            if (!hasChanged) {
                // No changes from existing database configuration so exit now
                return;
            }
            
            // Write changes to Spring Config file
            Map<String, String> props = new HashMap<String, String>();
            props.put(JNDI_NAME, jndi);
            
            getApplicationConfigService().addProperties(DATASOURCE_PREFIX, props, profile, force);
            
            // Remove old properties
            getApplicationConfigService().removeProperty(DATASOURCE_PREFIX, DATABASE_URL, profile);
            getApplicationConfigService().removeProperty(DATASOURCE_PREFIX, DATABASE_DRIVER, profile);
            getApplicationConfigService().removeProperty(DATASOURCE_PREFIX, DATABASE_USERNAME, profile);
            getApplicationConfigService().removeProperty(DATASOURCE_PREFIX, DATABASE_PASSWORD, profile);
            
        }
    }

    private void updateDataNucleusPlugin(final boolean addToPlugin) {
    	
    	if(fileManager == null){
    		fileManager = getFileManager();
    	}
    	Validate.notNull(fileManager, "FileManager is required");
    	
    	if(pathResolver == null){
    		pathResolver = getPathResolver();
    	}
    	Validate.notNull(pathResolver, "PathResolver is required");
    	
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
    	
    	if(projectOperations == null){
    		projectOperations = getProjectOperations();
    	}
    	Validate.notNull(projectOperations, "ProjectOperations is required");
    	
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

    private void updateFilters(final Element configuration,
            final OrmProvider ormProvider, final JdbcDatabase jdbcDatabase,
            final String databaseXPath, final String providersXPath,
            final String moduleName) {
    	
    	if(projectOperations == null){
    		projectOperations = getProjectOperations();
    	}
    	Validate.notNull(projectOperations, "ProjectOperations is required");
    	
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

    private void updateLog4j(final OrmProvider ormProvider) {
    	
    	if(fileManager == null){
    		fileManager = getFileManager();
    	}
    	Validate.notNull(fileManager, "FileManager is required");
    	
    	if(pathResolver == null){
    		pathResolver = getPathResolver();
    	}
    	Validate.notNull(pathResolver, "PathResolver is required");
    	
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

    private void updatePluginRepositories(final Element configuration,
            final OrmProvider ormProvider, final JdbcDatabase jdbcDatabase,
            final String moduleName) {
    	
    	if(projectOperations == null){
    		projectOperations = getProjectOperations();
    	}
    	Validate.notNull(projectOperations, "ProjectOperations is required");
    	
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
    	
    	if(projectOperations == null){
    		projectOperations = getProjectOperations();
    	}
    	Validate.notNull(projectOperations, "ProjectOperations is required");
    	
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
    	
    	if(projectOperations == null){
    		projectOperations = getProjectOperations();
    	}
    	Validate.notNull(projectOperations, "ProjectOperations is required");
    	
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
    	
    	if(projectOperations == null){
    		projectOperations = getProjectOperations();
    	}
    	Validate.notNull(projectOperations, "ProjectOperations is required");
    	
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
    	
    	if(fileManager == null){
    		fileManager = getFileManager();
    	}
    	Validate.notNull(fileManager, "FileManager is required");
    	
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
    
    
    public FileManager getFileManager(){
    	// Get all Services implement FileManager interface
		try {
			ServiceReference<?>[] references = this.context.getAllServiceReferences(FileManager.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (FileManager) this.context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load FileManager on JpaOperationsImpl.");
			return null;
		}
    }
    
    public PathResolver getPathResolver(){
    	// Get all Services implement PathResolver interface
		try {
			ServiceReference<?>[] references = this.context.getAllServiceReferences(PathResolver.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (PathResolver) this.context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load PathResolver on JpaOperationsImpl.");
			return null;
		}
    }
    
    public ProjectOperations getProjectOperations(){
    	// Get all Services implement ProjectOperations interface
		try {
			ServiceReference<?>[] references = this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (ProjectOperations) this.context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load ProjectOperations on JpaOperationsImpl.");
			return null;
		}
    }
    
    public ApplicationConfigService getApplicationConfigService(){
        if(applicationConfigService == null){
            // Get all Services implement ApplicationConfigService interface
            try {
                ServiceReference<?>[] references = this.context.getAllServiceReferences(ApplicationConfigService.class.getName(), null);
                
                for(ServiceReference<?> ref : references){
                    applicationConfigService = (ApplicationConfigService) this.context.getService(ref);
                }
                
                return null;
                
            } catch (InvalidSyntaxException e) {
                LOGGER.warning("Cannot load ApplicationConfigService on JpaOperationsImpl.");
                return null;
            }
        }else{
            return applicationConfigService;
        }

    }
    
    public TypeLocationService getTypeLocationService(){
    	// Get all Services implement TypeLocationService interface
		try {
			ServiceReference<?>[] references = this.context.getAllServiceReferences(TypeLocationService.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (TypeLocationService) this.context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load TypeLocationService on JpaOperationsImpl.");
			return null;
		}
    }
    
    public TypeManagementService getTypeManagementService(){
    	// Get all Services implement TypeManagementService interface
		try {
			ServiceReference<?>[] references = this.context.getAllServiceReferences(TypeManagementService.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (TypeManagementService) this.context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load TypeManagementService on JpaOperationsImpl.");
			return null;
		}
    }
}
