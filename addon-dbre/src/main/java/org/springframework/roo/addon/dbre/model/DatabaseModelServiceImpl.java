package org.springframework.roo.addon.dbre.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.DbrePath;
import org.springframework.roo.addon.dbre.jdbc.ConnectionProvider;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link DatabaseModelService).
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class DatabaseModelServiceImpl implements DatabaseModelService {
	private static final String NAME = "name";
	private static final String LOCAL = "local";
	private static final String FOREIGN = "foreign";
	private static final String DESCRIPTION = "description";
	private static final String KEY_SEQUENCE = "keySequence";
	private static final String SEQUENCE_NUMBER = "sequenceNumber";
	@Reference private PropFileOperations propFileOperations;
	@Reference private PathResolver pathResolver;
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private ConnectionProvider connectionProvider;
	@Reference private TableModelService tableModelService;

	private enum IndexType {
		INDEX, UNIQUE
	};

	public Set<Schema> getDatabaseSchemas() {
		Connection connection = null;
		try {
			connection = getConnection();
			DatabaseSchemaIntrospector introspector = new DatabaseSchemaIntrospector(connection);
			return introspector.getSchemas();
		} catch (Exception e) {
			return Collections.emptySet();
		} finally {
			connectionProvider.closeConnection(connection);
		}
	}

	public String getDatabaseMetadata(Schema schema, JavaPackage javaPackage) {
		try {
			if (schema == null) {
				schema = getLastKnownSchema();
			}
			if (javaPackage == null) {
				javaPackage = getLastKnownJavaPackage();
			}

			Database database = getDatabase(schema, javaPackage);
			Assert.isTrue(database != null && database.hasTables(), "Schema " + schema.getName() + " either does not exist or does not contain any tables");
			OutputStream outputStream = new ByteArrayOutputStream();
			Document document = getDocument(database);
			XmlUtils.writeXml(outputStream, document);
			return outputStream.toString();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to retrieve database metadata", e);
		}
	}

	public void serializeDatabaseMetadata(Schema schema, JavaPackage javaPackage, File file) {
		try {
			if (schema == null) {
				schema = getLastKnownSchema();
			}
			if (javaPackage == null) {
				javaPackage = getLastKnownJavaPackage();
			}

			Database database = getDatabase(schema, javaPackage);
			Assert.isTrue(database != null && database.hasTables(), "Schema " + schema.getName() + " either does not exist or does not contain any tables");
			Document document = getDocument(database);

			OutputStream outputStream;
			if (file == null) {
				String path = getDbreXmlPath();
				MutableFile mutableFile = fileManager.exists(path) ? fileManager.updateFile(path) : fileManager.createFile(path);
				outputStream = mutableFile.getOutputStream();
			} else {
				outputStream = new FileOutputStream(file);
			}
			XmlUtils.writeXml(outputStream, document);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to write database metadata to file", e);
		}
	}

	public Database deserializeDatabaseMetadata() {
		Document document = getDocument();
		Element databaseElement = document.getDocumentElement();

		Set<Table> tables = new LinkedHashSet<Table>();
		List<Element> tableElements = XmlUtils.findElements("table", databaseElement);
		for (Element tableElement : tableElements) {
			Table table = new Table();
			table.setName(tableElement.getAttribute(NAME));
			if (StringUtils.hasText(tableElement.getAttribute(DESCRIPTION))) {
				table.setDescription(tableElement.getAttribute(DESCRIPTION));
			}

			List<Element> columnElements = XmlUtils.findElements("column", tableElement);
			for (Element columnElement : columnElements) {
				String name = columnElement.getAttribute(NAME);
				Column column = new Column(name);
				column.setDescription(columnElement.getAttribute(DESCRIPTION));
				column.setPrimaryKey(Boolean.parseBoolean(columnElement.getAttribute("primaryKey")));
				column.setJavaType(columnElement.getAttribute("javaType"));
				column.setRequired(Boolean.parseBoolean(columnElement.getAttribute("required")));
				column.setSize(Integer.parseInt(columnElement.getAttribute("size")));
				column.setType(ColumnType.valueOf(columnElement.getAttribute("type")));
				column.setJavaName(columnElement.getAttribute("javaName"));
				column.setOrdinalPosition(Integer.parseInt(columnElement.getAttribute("index")));
				table.addColumn(column);
			}

			List<Element> foreignKeyElements = XmlUtils.findElements("foreignKey", tableElement);
			for (Element foreignKeyElement : foreignKeyElements) {
				ForeignKey foreignKey = new ForeignKey(foreignKeyElement.getAttribute(NAME), foreignKeyElement.getAttribute("foreignTable"));
				foreignKey.setOnDelete(CascadeAction.getCascadeAction(foreignKeyElement.getAttribute("onDelete")));
				foreignKey.setOnUpdate(CascadeAction.getCascadeAction(foreignKeyElement.getAttribute("onUpdate")));
				foreignKey.setKeySequence(new Short(foreignKeyElement.getAttribute(KEY_SEQUENCE)));

				List<Element> referenceElements = XmlUtils.findElements("reference", foreignKeyElement);
				for (Element referenceElement : referenceElements) {
					org.springframework.roo.addon.dbre.model.Reference reference = new org.springframework.roo.addon.dbre.model.Reference();
					reference.setForeignColumnName(referenceElement.getAttribute(FOREIGN));
					reference.setLocalColumnName(referenceElement.getAttribute(LOCAL));
					reference.setSequenceNumber(new Short(referenceElement.getAttribute(SEQUENCE_NUMBER)));
					foreignKey.addReference(reference);
				}
				table.addForeignKey(foreignKey);
			}

			List<Element> exportedKeyElements = XmlUtils.findElements("exportedKey", tableElement);
			for (Element exportedKeyElement : exportedKeyElements) {
				ForeignKey exportedKey = new ForeignKey(exportedKeyElement.getAttribute(NAME), exportedKeyElement.getAttribute("foreignTable"));
				exportedKey.setOnDelete(CascadeAction.getCascadeAction(exportedKeyElement.getAttribute("onDelete")));
				exportedKey.setOnUpdate(CascadeAction.getCascadeAction(exportedKeyElement.getAttribute("onUpdate")));
				exportedKey.setKeySequence(new Short(exportedKeyElement.getAttribute(KEY_SEQUENCE)));

				List<Element> referenceElements = XmlUtils.findElements("reference", exportedKeyElement);
				for (Element referenceElement : referenceElements) {
					org.springframework.roo.addon.dbre.model.Reference reference = new org.springframework.roo.addon.dbre.model.Reference();
					reference.setForeignColumnName(referenceElement.getAttribute(FOREIGN));
					reference.setLocalColumnName(referenceElement.getAttribute(LOCAL));
					reference.setSequenceNumber(new Short(referenceElement.getAttribute(SEQUENCE_NUMBER)));
					exportedKey.addReference(reference);
				}
				table.addExportedKey(exportedKey);
			}

			addIndices(table, tableElement, IndexType.INDEX);
			addIndices(table, tableElement, IndexType.UNIQUE);

			tables.add(table);
		}

		String name = databaseElement.getAttribute(NAME);
		Schema schema = new Schema(databaseElement.getAttribute("schema"));
		JavaPackage javaPackage = new JavaPackage(databaseElement.getAttribute("package"));

		return new Database(name, schema, javaPackage, tables);
	}

	private Schema getLastKnownSchema() {
		try {
			Document document = getDocument();
			Element databaseElement = document.getDocumentElement();
			String schemaAttribute = databaseElement.getAttribute("schema");
			if (!StringUtils.hasText(schemaAttribute)) {
				throw new IllegalStateException("Must specify a database schema name. Use --schema option");
			}
			return new Schema(schemaAttribute);
		} catch (Exception e) {
			throw new IllegalStateException("Must specify a database schema name. Use --schema option", e);
		}
	}

	private JavaPackage getLastKnownJavaPackage() {
		try {
			Document document = getDocument();
			Element databaseElement = document.getDocumentElement();
			String packageAttribute = databaseElement.getAttribute("package");
			return StringUtils.hasText(packageAttribute) ? new JavaPackage(packageAttribute) : getTopLevelPackage();
		} catch (Exception e) {
			return getTopLevelPackage();
		}
	}

	private JavaPackage getTopLevelPackage() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		return projectMetadata.getTopLevelPackage();
	}

	private Document getDocument() {
		String dbreXmlPath = getDbreXmlPath();
		FileDetails fileDetails = fileManager.readFile(dbreXmlPath);
		try {
			InputStream is = new FileInputStream(fileDetails.getFile());
			DocumentBuilder builder = XmlUtils.getDocumentBuilder();
			builder.setErrorHandler(null);
			return builder.parse(is);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void addIndices(Table table, Element tableElement, IndexType indexType) {
		List<Element> elements = XmlUtils.findElements(indexType.name().toLowerCase(), tableElement);
		for (Element element : elements) {
			Index index = new Index(element.getAttribute(NAME));
			index.setUnique(indexType == IndexType.UNIQUE);
			index.setTable(table);
			List<Element> indexColumnElements = XmlUtils.findElements(indexType.name().toLowerCase() + "-column", element);
			for (Element indexColumnElement : indexColumnElements) {
				IndexColumn indexColumn = new IndexColumn(indexColumnElement.getAttribute(NAME));
				indexColumn.setOrdinalPosition(new Short(indexColumnElement.getAttribute(SEQUENCE_NUMBER)));
				index.addColumn(indexColumn);
			}
			table.addIndex(index);
		}
	}

	private Document getDocument(Database database) {
		Document document = XmlUtils.getDocumentBuilder().newDocument();

		Element databaseElement = document.createElement("database");
		databaseElement.setAttribute(NAME, database.getName());
		if (database.getSchema() != null) {
			databaseElement.setAttribute("schema", database.getSchema().getName());
		}
		if (database.getJavaPackage() != null) {
			databaseElement.setAttribute("package", database.getJavaPackage().getFullyQualifiedPackageName());
		}

		for (Table table : database.getTables()) {
			Element tableElement = document.createElement("table");
			tableElement.setAttribute(NAME, table.getName());
			if (StringUtils.hasText(table.getDescription())) {
				tableElement.setAttribute(DESCRIPTION, table.getDescription());
			}

			for (Column column : table.getColumns()) {
				Element columnElement = document.createElement("column");
				columnElement.setAttribute(NAME, column.getName());
				if (StringUtils.hasText(column.getDescription())) {
					columnElement.setAttribute(DESCRIPTION, column.getDescription());
				}
				columnElement.setAttribute("primaryKey", String.valueOf(column.isPrimaryKey()));
				columnElement.setAttribute("required", String.valueOf(column.isRequired()));
				columnElement.setAttribute("size", String.valueOf(column.getSize()));
				columnElement.setAttribute("type", column.getType().name());
				columnElement.setAttribute("javaName", tableModelService.suggestFieldName(column.getName()));
				columnElement.setAttribute("index", String.valueOf(column.getOrdinalPosition()));
				tableElement.appendChild(columnElement);
			}

			short keySequence = 0;
			for (ForeignKey foreignKey : table.getForeignKeys()) {
				Element foreignKeyElement = document.createElement("foreignKey");
				String foreignTableName = foreignKey.getForeignTableName();
				foreignKeyElement.setAttribute(NAME, foreignKey.getName());
				foreignKeyElement.setAttribute("foreignTable", foreignTableName);
				foreignKeyElement.setAttribute("onDelete", foreignKey.getOnDelete().getCode());
				foreignKeyElement.setAttribute("onUpdate", foreignKey.getOnUpdate().getCode());

				if (table.getForeignKeyCountByForeignTableName(foreignTableName) > 1) {
					keySequence++;
				}
				foreignKeyElement.setAttribute(KEY_SEQUENCE, String.valueOf(keySequence));

				for (org.springframework.roo.addon.dbre.model.Reference reference : foreignKey.getReferences()) {
					Element referenceElement = document.createElement("reference");
					referenceElement.setAttribute(FOREIGN, reference.getForeignColumnName());
					referenceElement.setAttribute(LOCAL, reference.getLocalColumnName());
					referenceElement.setAttribute(SEQUENCE_NUMBER, reference.getSequenceNumber().toString());
					foreignKeyElement.appendChild(referenceElement);
				}
				tableElement.appendChild(foreignKeyElement);
			}

			for (Index index : table.getIndices()) {
				Element indexElement = document.createElement(index.isUnique() ? IndexType.UNIQUE.name().toLowerCase() : IndexType.INDEX.name().toLowerCase());
				indexElement.setAttribute(NAME, index.getName());
				for (IndexColumn indexColumn : index.getColumns()) {
					Element indexColumnElement = document.createElement((index.isUnique() ? IndexType.UNIQUE.name().toLowerCase() : IndexType.INDEX.name().toLowerCase()) + "-column");
					indexColumnElement.setAttribute(NAME, indexColumn.getName());
					indexColumnElement.setAttribute(SEQUENCE_NUMBER, String.valueOf(indexColumn.getOrdinalPosition()));
					indexElement.appendChild(indexColumnElement);
				}
				tableElement.appendChild(indexElement);
			}

			keySequence = 0;
			for (ForeignKey exportedKey : table.getExportedKeys()) {
				Element exportedKeyElement = document.createElement("exportedKey");
				String foreignTableName = exportedKey.getForeignTableName();
				exportedKeyElement.setAttribute(NAME, exportedKey.getName());
				exportedKeyElement.setAttribute("foreignTable", foreignTableName);
				exportedKeyElement.setAttribute("onDelete", exportedKey.getOnDelete().getCode());
				exportedKeyElement.setAttribute("onUpdate", exportedKey.getOnUpdate().getCode());

				if (table.getExportedKeyCountByForeignTableName(foreignTableName) > 1) {
					keySequence++;
				}
				exportedKeyElement.setAttribute(KEY_SEQUENCE, String.valueOf(keySequence));

				for (org.springframework.roo.addon.dbre.model.Reference reference : exportedKey.getReferences()) {
					Element referenceElement = document.createElement("reference");
					referenceElement.setAttribute(FOREIGN, reference.getForeignColumnName());
					referenceElement.setAttribute(LOCAL, reference.getLocalColumnName());
					referenceElement.setAttribute(SEQUENCE_NUMBER, reference.getSequenceNumber().toString());
					exportedKeyElement.appendChild(referenceElement);
				}
				tableElement.appendChild(exportedKeyElement);
			}

			databaseElement.appendChild(tableElement);
		}

		document.appendChild(databaseElement);

		return document;
	}

	private Database getDatabase(Schema schema, JavaPackage javaPackage) throws SQLException {
		Connection connection = null;
		try {
			connection = getConnection();
			DatabaseSchemaIntrospector introspector = new DatabaseSchemaIntrospector(connection, schema);
			return introspector.getDatabase(javaPackage);
		} finally {
			connectionProvider.closeConnection(connection);
		}
	}

	private Connection getConnection() throws SQLException {
		if (fileManager.exists(pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "database.properties"))) {
			connectionProvider.configure(propFileOperations.getProperties(Path.SPRING_CONFIG_ROOT, "database.properties"));
		} else {
			connectionProvider.configure(getConnectionPropertiesFromDataNucleusConfiguration());
		}
		return connectionProvider.getConnection();
	}

	private Properties getConnectionPropertiesFromDataNucleusConfiguration() {
		String persistenceXmlPath = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml");
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
			String key = propertyElement.getAttribute(NAME);
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
		}
		return properties;
	}

	private String getDbreXmlPath() {
		return pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, DbrePath.DBRE_XML_FILE.getPath());
	}
}
