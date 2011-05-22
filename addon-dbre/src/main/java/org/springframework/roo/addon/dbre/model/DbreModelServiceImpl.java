package org.springframework.roo.addon.dbre.model;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.jdbc.ConnectionProvider;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;
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
	@Reference private ConnectionProvider connectionProvider;
	@Reference private FileManager fileManager;
	@Reference private ProjectOperations projectOperations;
	@Reference private PropFileOperations propFileOperations;
	private Schema lastSchema;
	private Map<Schema, Database> cachedIntrospections = new HashMap<Schema, Database>();

	public boolean supportsSchema(boolean displayAddOns) throws RuntimeException {
		Connection connection = null;
		try {
			connection = getConnection(displayAddOns);
			DatabaseMetaData databaseMetaData = connection.getMetaData();
			String schemaTerm = databaseMetaData.getSchemaTerm();
			return StringUtils.hasText(schemaTerm) && schemaTerm.equalsIgnoreCase("schema");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} finally {
			connectionProvider.closeConnection(connection);
		}
	}

	public Set<Schema> getSchemas(boolean displayAddOns) {
		Connection connection = null;
		try {
			connection = getConnection(displayAddOns);
			SchemaIntrospector introspector = new SchemaIntrospector(connection);
			return introspector.getSchemas();
		} catch (Exception e) {
			return Collections.emptySet();
		} finally {
			connectionProvider.closeConnection(connection);
		}
	}

	public Database getDatabase(boolean evictCache) {
		if (!evictCache && cachedIntrospections.containsKey(lastSchema)) {
			return cachedIntrospections.get(lastSchema);
		}
		if (evictCache && cachedIntrospections.containsKey(lastSchema)) {
			cachedIntrospections.remove(lastSchema);
		}
		String dbreXmlPath = getDbreXmlPath();
		if (!StringUtils.hasText(dbreXmlPath) || !fileManager.exists(dbreXmlPath)) {
			return null;
		}

		Database database = null;
		InputStream inputStream = null;
		try {
			inputStream = fileManager.getInputStream(dbreXmlPath);
			database = DatabaseXmlUtils.readDatabase(inputStream);
			cacheDatabase(database);
			return database;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException ignored) {}
			}
		}
	}

	public void writeDatabase(Database database) {
		Document document = DatabaseXmlUtils.getDatabaseDocument(database);
		fileManager.createOrUpdateTextFileIfRequired(getDbreXmlPath(), XmlUtils.nodeToString(document), true);
	}
	
	public String getDbreXmlPath() {
		return projectOperations.isProjectAvailable() ? projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "dbre.xml") : null;
	}
	
	public String getNoSchemaString() {
		return "no-schema-required";
	}

	public Database refreshDatabase(Schema schema, boolean view, Set<String> includeTables, Set<String> excludeTables) {
		Assert.notNull(schema, "Schema required");

		Connection connection = null;
		try {
			connection = getConnection(true);
			DatabaseIntrospector introspector = new DatabaseIntrospector(connection, schema, view, includeTables, excludeTables);
			Database database = introspector.createDatabase();
			cacheDatabase(database);
			return database;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} finally {
			connectionProvider.closeConnection(connection);
		}
	}
	
	private void cacheDatabase(Database database) {
		if (database != null) {
			lastSchema = database.getSchema();
			cachedIntrospections.put(lastSchema, database);
		}
	}
	
	private Connection getConnection(boolean displayAddOns) {
		if (fileManager.exists(projectOperations.getPathResolver().getIdentifier(Path.SPRING_CONFIG_ROOT, "database.properties"))) {
			Map<String, String> connectionProperties = propFileOperations.getProperties(Path.SPRING_CONFIG_ROOT, "database.properties");
			return connectionProvider.getConnection(connectionProperties, displayAddOns);
		}
		
		Properties connectionProperties = getConnectionPropertiesFromDataNucleusConfiguration();
		return connectionProvider.getConnection(connectionProperties, displayAddOns);
	}

	private Properties getConnectionPropertiesFromDataNucleusConfiguration() {
		String persistenceXmlPath = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml");
		if (!fileManager.exists(persistenceXmlPath)) {
			throw new IllegalStateException("Failed to find " + persistenceXmlPath);
		}

		FileDetails fileDetails = fileManager.readFile(persistenceXmlPath);
		Document document = null;
		try {
			InputStream is = new BufferedInputStream(new FileInputStream(fileDetails.getFile()));
			DocumentBuilder builder = XmlUtils.getDocumentBuilder();
			builder.setErrorHandler(null);
			document = builder.parse(is);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		List<Element> propertyElements = XmlUtils.findElements("/persistence/persistence-unit/properties/property", document.getDocumentElement());
		Assert.notEmpty(propertyElements, "Failed to find property elements in " + persistenceXmlPath);
		Properties properties = new Properties();

		for (Element propertyElement : propertyElements) {
			String key = propertyElement.getAttribute("name");
			String value = propertyElement.getAttribute("value");
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
				// All required properties have been found so ignore rest of elements
				break;
			}
		}
		return properties;
	}
}
