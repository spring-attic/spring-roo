package org.springframework.roo.addon.dbre.model;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.jdbc.ConnectionProvider;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link DbreModelService}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class DbreModelServiceImpl implements DbreModelService {

    private final Set<Database> cachedIntrospections = new HashSet<Database>();
    @Reference private ConnectionProvider connectionProvider;
    @Reference private FileManager fileManager;
    private Database lastDatabase;

    @Reference private ProjectOperations projectOperations;
    @Reference private PropFileOperations propFileOperations;

    private void cacheDatabase(final Database database) {
        if (database != null) {
            lastDatabase = database;
            cachedIntrospections.add(database);
        }
    }

    private Connection getConnection(final boolean displayAddOns) {
        final String dbProps = "database.properties";
        final String jndiDataSource = getJndiDataSourceName();
        if (StringUtils.isNotBlank(jndiDataSource)) {
            final Map<String, String> props = propFileOperations.getProperties(
                    Path.SPRING_CONFIG_ROOT.getModulePathId(projectOperations
                            .getFocusedModuleName()), "jndi.properties");
            return connectionProvider.getConnectionViaJndiDataSource(
                    jndiDataSource, props, displayAddOns);
        }
        else if (fileManager.exists(projectOperations.getPathResolver()
                .getFocusedIdentifier(Path.SPRING_CONFIG_ROOT, dbProps))) {
            final Map<String, String> props = propFileOperations.getProperties(
                    Path.SPRING_CONFIG_ROOT.getModulePathId(projectOperations
                            .getFocusedModuleName()), dbProps);
            return connectionProvider.getConnection(props, displayAddOns);
        }

        final Properties connectionProperties = getConnectionPropertiesFromDataNucleusConfiguration();
        return connectionProvider.getConnection(connectionProperties,
                displayAddOns);
    }

    private Properties getConnectionPropertiesFromDataNucleusConfiguration() {
        final String persistenceXmlPath = projectOperations.getPathResolver()
                .getFocusedIdentifier(Path.SRC_MAIN_RESOURCES,
                        "META-INF/persistence.xml");
        if (!fileManager.exists(persistenceXmlPath)) {
            throw new IllegalStateException("Failed to find "
                    + persistenceXmlPath);
        }

        final FileDetails fileDetails = fileManager
                .readFile(persistenceXmlPath);
        Document document = null;
        try {
            final InputStream is = new BufferedInputStream(new FileInputStream(
                    fileDetails.getFile()));
            final DocumentBuilder builder = XmlUtils.getDocumentBuilder();
            builder.setErrorHandler(null);
            document = builder.parse(is);
        }
        catch (final Exception e) {
            throw new IllegalStateException(e);
        }

        final List<Element> propertyElements = XmlUtils.findElements(
                "/persistence/persistence-unit/properties/property",
                document.getDocumentElement());
        Validate.notEmpty(propertyElements,
                "Failed to find property elements in " + persistenceXmlPath);
        final Properties properties = new Properties();

        for (final Element propertyElement : propertyElements) {
            final String key = propertyElement.getAttribute("name");
            final String value = propertyElement.getAttribute("value");
            if ("datanucleus.ConnectionDriverName".equals(key)) {
                properties.put("database.driverClassName", value);
            }
            if ("datanucleus.ConnectionURL".equals(key)) {
                properties.put("database.url", value);
            }
            if ("datanucleus.ConnectionUserName".equals(key)) {
                properties.put("database.username", value);
            }
            if ("datanucleus.ConnectionPassword".equals(key)) {
                properties.put("database.password", value);
            }

            if (properties.size() == 4) {
                // All required properties have been found so ignore rest of
                // elements
                break;
            }
        }
        return properties;
    }

    public Database getDatabase(final boolean evictCache) {
        if (!evictCache && cachedIntrospections.contains(lastDatabase)) {
            for (final Database database : cachedIntrospections) {
                if (database.equals(lastDatabase)) {
                    return lastDatabase;
                }
            }
        }
        if (evictCache && cachedIntrospections.contains(lastDatabase)) {
            cachedIntrospections.remove(lastDatabase);
        }

        final String dbreXmlPath = getDbreXmlPath();
        if (StringUtils.isBlank(dbreXmlPath)
                || !fileManager.exists(dbreXmlPath)) {
            return null;
        }

        Database database = null;
        InputStream inputStream = null;
        try {
            inputStream = fileManager.getInputStream(dbreXmlPath);
            database = DatabaseXmlUtils.readDatabase(inputStream);
            cacheDatabase(database);
            return database;
        }
        catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private String getDbreXmlPath() {
        for (final String moduleName : projectOperations.getModuleNames()) {
            final LogicalPath logicalPath = LogicalPath.getInstance(
                    Path.SRC_MAIN_RESOURCES, moduleName);
            final String dbreXmlPath = projectOperations.getPathResolver()
                    .getIdentifier(logicalPath, DBRE_XML);
            if (fileManager.exists(dbreXmlPath)) {
                return dbreXmlPath;
            }
        }
        return projectOperations.getPathResolver().getFocusedIdentifier(
                Path.SRC_MAIN_RESOURCES, DBRE_XML);
    }

    private String getJndiDataSourceName() {
        final String contextPath = projectOperations.getPathResolver()
                .getFocusedIdentifier(Path.SPRING_CONFIG_ROOT,
                        "applicationContext.xml");
        final Document appCtx = XmlUtils.readXml(fileManager
                .getInputStream(contextPath));
        final Element root = appCtx.getDocumentElement();
        final Element dataSourceJndi = XmlUtils.findFirstElement(
                "/beans/jndi-lookup[@id = 'dataSource']", root);
        return dataSourceJndi != null ? dataSourceJndi
                .getAttribute("jndi-name") : null;
    }

    public Set<Schema> getSchemas(final boolean displayAddOns) {
        Connection connection = null;
        try {
            connection = getConnection(displayAddOns);
            final SchemaIntrospector introspector = new SchemaIntrospector(
                    connection);
            return introspector.getSchemas();
        }
        catch (final Exception e) {
            return Collections.emptySet();
        }
        finally {
            connectionProvider.closeConnection(connection);
        }
    }

    public Database refreshDatabase(final Set<Schema> schemas,
            final boolean view, final Set<String> includeTables,
            final Set<String> excludeTables) {
        Validate.notNull(schemas, "Schemas required");

        Connection connection = null;
        try {
            connection = getConnection(true);
            final DatabaseIntrospector introspector = new DatabaseIntrospector(
                    connection, schemas, view, includeTables, excludeTables);
            final Database database = introspector.createDatabase();
            cacheDatabase(database);
            return database;
        }
        catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        finally {
            connectionProvider.closeConnection(connection);
        }
    }

    public boolean supportsSchema(final boolean displayAddOns)
            throws RuntimeException {
        Connection connection = null;
        try {
            connection = getConnection(displayAddOns);
            final DatabaseMetaData databaseMetaData = connection.getMetaData();
            final String schemaTerm = databaseMetaData.getSchemaTerm();
            return StringUtils.isNotBlank(schemaTerm)
                    && schemaTerm.equalsIgnoreCase("schema");
        }
        catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        finally {
            connectionProvider.closeConnection(connection);
        }
    }

    public void writeDatabase(final Database database) {
        final Document document = DatabaseXmlUtils
                .getDatabaseDocument(database);
        fileManager.createOrUpdateTextFileIfRequired(getDbreXmlPath(),
                XmlUtils.nodeToString(document), true);
    }
}
