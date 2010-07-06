package org.springframework.roo.addon.dbre.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.DbrePath;
import org.springframework.roo.addon.dbre.jdbc.ConnectionProvider;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.logging.HandlerUtils;
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
	private static final Logger logger = HandlerUtils.getLogger(DatabaseModelServiceImpl.class);
	@Reference private PropFileOperations propFileOperations;
	@Reference private PathResolver pathResolver;
	@Reference private FileManager fileManager;
	@Reference private ConnectionProvider connectionProvider;

	public Set<Schema> getDatabaseSchemas() {
		Connection connection = null;
		try {
			connection = getConnection();
			DatabaseModelReader modelReader = new DatabaseModelReader(connection);
			return modelReader.getSchemas();
		} catch (Exception e) {
			return Collections.emptySet();
		} finally {
			connectionProvider.closeConnection(connection);
		}
	}

	public void displayDatabaseMetadata(String catalog, Schema schema, JavaPackage javaPackage) {
		try {
			Database database = getDatabase(catalog, schema, javaPackage);
			if (database == null || database.getTables().isEmpty()) {
				logger.warning("Schema " + schema.getName() + " does not contain any tables");
				return;
			}

			OutputStream outputStream = new ByteArrayOutputStream();
			Document document = getDocument(database);
			XmlUtils.writeXml(outputStream, document);
			logger.info(outputStream.toString());
		} catch (Exception e) {
			logger.warning("Failed to retrieve database metadata: " + e.getMessage());
		}
	}

	public void serializeDatabaseMetadata(String catalog, Schema schema, JavaPackage javaPackage, File file) {
		try {
			Database database = getDatabase(catalog, schema, javaPackage);
			if (database == null || database.getTables().isEmpty()) {
				logger.warning("Schema " + schema.getName() + " does not contain any tables");
				return;
			}

			Document document = getDocument(database);

			if (file != null) {
				XmlUtils.writeXml(new FileOutputStream(file), document);
				logger.info("Database metadata written to file " + file.getAbsolutePath());
			} else {
				String dbreXmlPath = getDbreXmlPath();
				MutableFile mutableFile = fileManager.exists(dbreXmlPath) ? fileManager.updateFile(dbreXmlPath) : fileManager.createFile(dbreXmlPath);
				XmlUtils.writeXml(mutableFile.getOutputStream(), document);
			}
		} catch (Exception e) {
			logger.warning("Failed to write database metadata to file: " + e.getMessage());
		}
	}

	public Database deserializeDatabaseMetadata() {
		String dbreXmlPath = getDbreXmlPath();
		MutableFile mutableFile = fileManager.updateFile(dbreXmlPath);
		Document document = null;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception e) {
			throw new IllegalStateException("Unable to parse " + dbreXmlPath, e);
		}
		if (document == null || !document.hasChildNodes()) {
			throw new IllegalStateException(dbreXmlPath + " is empty");
		}

		Element databaseElement = document.getDocumentElement();

		Database database = new Database();
		database.setName(databaseElement.getAttribute("name"));
		database.setJavaPackage(new JavaPackage(databaseElement.getAttribute("package")));

		Set<Table> tables = new LinkedHashSet<Table>();
		List<Element> tableElements = XmlUtils.findElements("table", databaseElement);
		for (Element tableElement : tableElements) {
			Table table = new Table();
			table.setName(tableElement.getAttribute("name"));

			List<Element> columnElements = XmlUtils.findElements("column", tableElement);
			for (Element columnElement : columnElements) {
				Column column = new Column(columnElement.getAttribute("name"));
				column.setDescription(columnElement.getAttribute("description"));
				column.setPrimaryKey(Boolean.parseBoolean(columnElement.getAttribute("primaryKey")));
				column.setJavaType(new JavaType(columnElement.getAttribute("javaType")));
				column.setRequired(Boolean.parseBoolean(columnElement.getAttribute("required")));
				column.setSize(Integer.parseInt(columnElement.getAttribute("size")));
				column.setType(columnElement.getAttribute("type"));
				table.addColumn(column);
			}

			List<Element> foreignKeyElements = XmlUtils.findElements("foreignKey", tableElement);
			for (Element foreignKeyElement : foreignKeyElements) {
				ForeignKey foreignKey = new ForeignKey(foreignKeyElement.getAttribute("name"));
				foreignKey.setForeignTableName(foreignKeyElement.getAttribute("foreignTable"));
				foreignKey.setOnDelete(CascadeAction.getCascadeAction(foreignKeyElement.getAttribute("onDelete")));
				foreignKey.setOnUpdate(CascadeAction.getCascadeAction(foreignKeyElement.getAttribute("onUpdate")));

				List<Element> referenceElements = XmlUtils.findElements("reference", foreignKeyElement);
				for (Element referenceElement : referenceElements) {
					org.springframework.roo.addon.dbre.model.Reference reference = new org.springframework.roo.addon.dbre.model.Reference();
					reference.setForeignColumnName(referenceElement.getAttribute("foreign"));
					reference.setLocalColumnName(referenceElement.getAttribute("local"));
					foreignKey.addReference(reference);
				}
				table.addForeignKey(foreignKey);
			}

			addIndices(table, tableElement, "index");
			addIndices(table, tableElement, "unique");

			tables.add(table);
		}

		database.addTables(tables);

		return database;
	}

	private void addIndices(Table table, Element tableElement, String indexType) {
		List<Element> elements = XmlUtils.findElements(indexType, tableElement);
		for (Element element : elements) {
			Index index = new Index(element.getAttribute("name"));
			List<Element> indexColumnElements = XmlUtils.findElements(indexType + "-column", element);
			for (Element indexColumnElement : indexColumnElements) {
				IndexColumn indexColumn = new IndexColumn(indexColumnElement.getAttribute("name"));
				index.addColumn(indexColumn);
			}
			table.addIndex(index);
		}
	}

	private Document getDocument(Database database) {
		Document document = XmlUtils.getDocumentBuilder().newDocument();

		Element databaseElement = document.createElement("database");
		if (database.getName() != null) {
			databaseElement.setAttribute("name", database.getName());
		}
		if (database.getJavaPackage() != null) {
			databaseElement.setAttribute("package", database.getJavaPackage().getFullyQualifiedPackageName());
		}

		for (Table table : database.getTables()) {
			Element tableElement = document.createElement("table");
			tableElement.setAttribute("name", table.getName());
			if (StringUtils.hasText(table.getDescription())) {
				tableElement.setAttribute("description", table.getDescription());
			}

			for (Column column : table.getColumns()) {
				Element columnElement = document.createElement("column");
				columnElement.setAttribute("name", column.getName());
				if (StringUtils.hasText(column.getDescription())) {
					columnElement.setAttribute("description", column.getDescription());
				}
				columnElement.setAttribute("primaryKey", String.valueOf(column.isPrimaryKey()));
				columnElement.setAttribute("javaType", column.getJavaTypeFromTypeCode().getFullyQualifiedTypeName());
				columnElement.setAttribute("required", String.valueOf(column.isRequired()));
				columnElement.setAttribute("size", String.valueOf(column.getSize()));
				columnElement.setAttribute("type", column.getType());
				tableElement.appendChild(columnElement);
			}

			for (ForeignKey foreignKey : table.getForeignKeys()) {
				Element foreignKeyElement = document.createElement("foreignKey");
				foreignKeyElement.setAttribute("name", foreignKey.getName());
				foreignKeyElement.setAttribute("foreignTable", foreignKey.getForeignTableName());
				foreignKeyElement.setAttribute("onDelete", foreignKey.getOnDelete().getCode());
				foreignKeyElement.setAttribute("onUpdate", foreignKey.getOnUpdate().getCode());
				for (org.springframework.roo.addon.dbre.model.Reference reference : foreignKey.getReferences()) {
					Element referenceElement = document.createElement("reference");
					referenceElement.setAttribute("foreign", reference.getForeignColumnName());
					referenceElement.setAttribute("local", reference.getLocalColumnName());
					foreignKeyElement.appendChild(referenceElement);
				}
				tableElement.appendChild(foreignKeyElement);
			}

			for (Index index : table.getIndices()) {
				Element indexElement = document.createElement(index.isUnique() ? "unique" : "index");
				indexElement.setAttribute("name", index.getName());
				for (IndexColumn indexColumn : index.getColumns()) {
					Element indexColumnElement = document.createElement(index.isUnique() ? "unique-column" : "index-column");
					indexColumnElement.setAttribute("name", indexColumn.getName());
					indexElement.appendChild(indexColumnElement);
				}
				tableElement.appendChild(indexElement);
			}

			databaseElement.appendChild(tableElement);
		}

		document.appendChild(databaseElement);

		return document;
	}

	private Database getDatabase(String catalog, Schema schema, JavaPackage javaPackage) throws SQLException {
		Connection connection = null;
		try {
			connection = getConnection();
			DatabaseModelReader modelReader = new DatabaseModelReader(connection);
			modelReader.setCatalog(StringUtils.hasText(catalog) ? catalog : modelReader.getConnection().getCatalog());
			modelReader.setSchema(schema);
			return modelReader.getDatabase(javaPackage);
		} finally {
			connectionProvider.closeConnection(connection);
		}
	}

	private Connection getConnection() throws SQLException {
		connectionProvider.configure(propFileOperations.getProperties(Path.SPRING_CONFIG_ROOT, "database.properties"));
		return connectionProvider.getConnection();
	}

	private String getDbreXmlPath() {
		return pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, DbrePath.DBRE_XML_FILE.getPath());
	}
}
