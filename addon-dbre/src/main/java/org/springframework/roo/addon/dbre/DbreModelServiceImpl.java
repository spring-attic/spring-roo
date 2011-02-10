package org.springframework.roo.addon.dbre;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.dbre.jdbc.ConnectionProvider;
import org.springframework.roo.addon.dbre.model.Database;
import org.springframework.roo.addon.dbre.model.DatabaseIntrospector;
import org.springframework.roo.addon.dbre.model.DatabaseXmlUtils;
import org.springframework.roo.addon.dbre.model.Schema;
import org.springframework.roo.addon.dbre.model.SchemaIntrospector;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.event.ProcessManagerStatus;
import org.springframework.roo.process.manager.event.ProcessManagerStatusListener;
import org.springframework.roo.process.manager.event.ProcessManagerStatusProvider;
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
@Reference(name = "databaseListener", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = DatabaseListener.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
public class DbreModelServiceImpl implements DbreModelService, ProcessManagerStatusListener {
	@Reference private ConnectionProvider connectionProvider;
	@Reference private FileManager fileManager;
	@Reference private ProcessManagerStatusProvider processManagerStatusProvider;
	@Reference private ProjectOperations projectOperations;
	@Reference private PropFileOperations propFileOperations;
	private Map<Schema, Database> cachedIntrospections = new HashMap<Schema, Database>();
	private Schema lastSchema = null;
	private JavaPackage destinationPackage;
	private boolean view;
	private Set<String> includeTables;
	private Set<String> excludeTables;
	private Set<DatabaseListener> listeners = new HashSet<DatabaseListener>();
	private boolean startupCompleted = false;

	protected void activate(ComponentContext context) {
		processManagerStatusProvider.addProcessManagerStatusListener(this);
	}

	protected void deactivate(ComponentContext context) {
		processManagerStatusProvider.removeProcessManagerStatusListener(this);
	}

	public void onProcessManagerStatusChange(ProcessManagerStatus oldStatus, ProcessManagerStatus newStatus) {
		considerStartup(newStatus);
	}
	
	public void considerStartup(ProcessManagerStatus newStatus) {
		if (newStatus == ProcessManagerStatus.AVAILABLE && !startupCompleted) {
			// This is the first time we're starting, so tell listeners about the database
			startupCompleted = true;
			// Simply calling get will cause the currently-registered listeners to discover this database is available
			getDatabase(lastSchema, false, false);
		}
	}

	protected void bindDatabaseListener(DatabaseListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
			if (startupCompleted && lastSchema != null) {
				// This listener missed that startup event, so we should share it with them now they're online
				// We use "safe mode" to avoid telling everybody else about the database (they would already know separately)
				Database database = getDatabase(lastSchema, false, true);
				if (database != null) {
					try {
						listener.notifyDatabaseRefreshed(database);
					} catch (RuntimeException ignored) {}
				}
			}
		}
	}

	protected void unbindDatabaseListener(DatabaseListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

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

	public Set<Schema> getDatabaseSchemas(boolean displayAddOns) {
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

	public Schema getLastSchema() {
		// First see if they've done a command where the last schema has been set directly
		if (lastSchema != null) {
			return lastSchema;
		}
		// Otherwise fallback to compute it via a file load (note this does NOT change the last schema field, as a getter is side effect free)
		return deserializeSchemaMetadataIfPossible();
	}

	public Database getDatabase(Schema schema) {
		// Cached copy or DBRE XML file sourced version is OK
		return getDatabase(schema, false, false);
	}

	public Database refreshDatabase(Schema schema) {
		// Force it to grab it from the DB
		return getDatabase(schema, true, false);
	}

	public Database refreshDatabaseSafely(Schema schema) {
		return getDatabase(schema, true, true);
	}

	public void setDestinationPackage(JavaPackage destinationPackage) {
		this.destinationPackage = destinationPackage;
	}

	public void setView(boolean view) {
		this.view = view;
	}

	public void setIncludeTables(Set<String> includeTables) {
		this.includeTables = includeTables;
	}

	public void setExcludeTables(Set<String> excludeTables) {
		this.excludeTables = excludeTables;
	}

	private Database getDatabase(Schema schema, boolean forceReadFromDatabase, boolean safeMode) {
		if (safeMode) {
			Assert.notNull(schema, "Schema required");
		}
		if (schema == null) {
			// We're not in safe mode to be here, due to the earlier assertions
			Schema s = getLastSchema();
			if (s == null) {
				// Cannot determine which schema to give them
				return null;
			} else {
				return getDatabase(s, forceReadFromDatabase, safeMode);
			}
		}
		Assert.notNull(schema, "Schema required");

		// Store the schema so we know what was the last schema should a call ever happen with null as the schema
		if (!safeMode) {
			lastSchema = schema;
		}

		if (!forceReadFromDatabase) {
			if (cachedIntrospections.containsKey(schema)) {
				return cachedIntrospections.get(schema);
			}

			// Read the .roo-dbre xml file, and see if it is for this schema
			Database database = deserializeDatabaseMetadataIfPossible();
			if (database != null && database.getSchema().equals(schema)) {
				// The deserialized from disk database is the one we want, so cache it and get out of here....
				if (!safeMode) {
					cachedIntrospections.put(schema, database);
					publishToListeners(database);
				}
				return database;
			}
		}

		Connection connection = null;
		try {
			connection = getConnection(true);
			DatabaseIntrospector introspector = new DatabaseIntrospector(connection, schema, destinationPackage, view, includeTables, excludeTables);
			Database database = introspector.createDatabase();

			if (safeMode) {
				// Don't change anything else or notify anyone, so leave now
				return database;
			}

			// It worked, so store it for later
			cachedIntrospections.put(schema, database);

			// Also store it for next time they load Roo (in case they're on an aeroplane flying from Sydney to San Francisco and don't have the database around)
			// todo... detect using GPS coordinates whether they're on SYD -> SFO ;-)
			if (database != null && database.hasTables()) {
				serializeDatabaseMetadataToFile(database);
			}

			// Lastly, let's tell our listeners
			publishToListeners(database);

			return database;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} finally {
			connectionProvider.closeConnection(connection);
		}
	}

	private void publishToListeners(Database database) {
		synchronized (listeners) {
			for (DatabaseListener listener : listeners) {
				try {
					listener.notifyDatabaseRefreshed(database);
				} catch (RuntimeException ignoreAndMoveOn) {}
			}
		}
	}

	/**
	 * Writes the database information to XML.
	 * 
	 * <p>
	 * NOTE: the XML file can only store one database.
	 * 
	 * @param database to serialize to disk (required)
	 */
	private void serializeDatabaseMetadataToFile(Database database) {
		Assert.notNull(database, "Database required");
		String dbreXmlPath = getDbreXmlPath();
		OutputStream outputStream = new ByteArrayOutputStream();
		DatabaseXmlUtils.writeDatabaseStructureToOutputStream(database, outputStream);
		fileManager.createOrUpdateTextFileIfRequired(dbreXmlPath, outputStream.toString(), false);
	}

	/**
	 * Reads the database schema information from XML if possible.
	 * 
	 * <p>
	 * NOTE: the XML file can only store one database.
	 * 
	 * @return the database schema if it could be parsed, otherwise null if unavailable for any reason
	 */
	private Schema deserializeSchemaMetadataIfPossible() {
		String dbreXmlPath = getDbreXmlPath();
		if (!StringUtils.hasText(dbreXmlPath) || !fileManager.exists(dbreXmlPath)) {
			return null;
		}
		FileDetails fileDetails = fileManager.readFile(dbreXmlPath);
		try {
			InputStream inputStream = new FileInputStream(fileDetails.getFile());
			return DatabaseXmlUtils.readSchemaFromInputStream(inputStream);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Reads the database information from XML if possible.
	 * 
	 * <p>
	 * NOTE: the XML file can only store one database.
	 * 
	 * @return the database if it could be parsed, otherwise null if unavailable for any reason
	 */
	private Database deserializeDatabaseMetadataIfPossible() {
		String dbreXmlPath = getDbreXmlPath();
		if (!StringUtils.hasText(dbreXmlPath) || !fileManager.exists(dbreXmlPath)) {
			return null;
		}
		FileDetails fileDetails = fileManager.readFile(dbreXmlPath);
		try {
			InputStream inputStream = new FileInputStream(fileDetails.getFile());
			return DatabaseXmlUtils.readDatabaseStructureFromInputStream(inputStream);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private Connection getConnection(boolean displayAddOns) throws SQLException {
		if (fileManager.exists(projectOperations.getPathResolver().getIdentifier(Path.SPRING_CONFIG_ROOT, "database.properties"))) {
			Map<String, String> connectionProperties = propFileOperations.getProperties(Path.SPRING_CONFIG_ROOT, "database.properties");
			return connectionProvider.getConnection(connectionProperties, displayAddOns);
		} else {
			Properties connectionProperties = getConnectionPropertiesFromDataNucleusConfiguration();
			return connectionProvider.getConnection(connectionProperties, displayAddOns);
		}
	}

	private Properties getConnectionPropertiesFromDataNucleusConfiguration() {
		String persistenceXmlPath = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml");
		if (!fileManager.exists(persistenceXmlPath)) {
			throw new IllegalStateException("Failed to find " + persistenceXmlPath);
		}

		FileDetails fileDetails = fileManager.readFile(persistenceXmlPath);
		Document document = null;
		try {
			InputStream is = new FileInputStream(fileDetails.getFile());
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

	private String getDbreXmlPath() {
		return projectOperations.isProjectAvailable() ? projectOperations.getPathResolver().getIdentifier(Path.ROOT, DbreModelService.DBRE_FILE) : null;
	}
}
