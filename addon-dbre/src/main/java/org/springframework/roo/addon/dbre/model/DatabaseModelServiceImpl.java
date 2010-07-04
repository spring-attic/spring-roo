package org.springframework.roo.addon.dbre.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
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
		Connection connection = getConnection();
		try {
			DatabaseModelReader modelReader = new DatabaseModelReader(connection);
			return modelReader.getSchemas();
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to get database schemas: " + e.getMessage());
		} finally {
			connectionProvider.closeConnection(connection);
		}
	}

	public void displayDatabaseMetadata(String catalog, Schema schema, JavaPackage javaPackage) {
		Database database = getDatabase(catalog, schema, javaPackage);
		OutputStream outputStream = new ByteArrayOutputStream();
		XmlUtils.writeXml(outputStream, getDocument(database));
		logger.info(outputStream.toString());
	}

	public void serializeDatabaseMetadata(String catalog, Schema schema, JavaPackage javaPackage, File file) {
		try {
			Database database = getDatabase(catalog, schema, javaPackage);
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
			throw new IllegalStateException("Failed to write database metadata to file: " + e.getMessage());
		}
	}

	public Database deserializeDatabaseMetadata() {
		Document document = null;

		String dbreXmlPath = getDbreXmlPath();
		if (!fileManager.exists(dbreXmlPath)) {
			throw new IllegalStateException(dbreXmlPath + " does not exist");
		}
		
		MutableFile mutableFile = fileManager.updateFile(dbreXmlPath);
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
				foreignKey.setOnDelete(new Short(foreignKeyElement.getAttribute("onDelete")));
				foreignKey.setOnUpdate(new Short(foreignKeyElement.getAttribute("onUpdate")));
				
				List<Element> referenceElements = XmlUtils.findElements("reference", foreignKeyElement);
				for (Element referenceElement : referenceElements) {
					org.springframework.roo.addon.dbre.model.Reference reference = new org.springframework.roo.addon.dbre.model.Reference();
					reference.setForeignColumnName(referenceElement.getAttribute("foreign"));
					reference.setLocalColumnName(referenceElement.getAttribute("local"));
					foreignKey.addReference(reference);
				}
				table.addForeignKey(foreignKey);
			}
			
			tables.add(table);
		}
		
		database.addTables(tables);
		return database;
	}

	private Document getDocument(Database database) {
		if (database == null || database.getTables().isEmpty()) {
			throw new IllegalStateException("Schema does not exist or the database does not contain any tables");
		}
		
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
				foreignKeyElement.setAttribute("onDelete", String.valueOf(foreignKey.getOnDelete()));
				foreignKeyElement.setAttribute("onUpdate", String.valueOf(foreignKey.getOnUpdate()));
				for (org.springframework.roo.addon.dbre.model.Reference reference : foreignKey.getReferences()) {
					Element referenceElement = document.createElement("reference");
					referenceElement.setAttribute("foreign", reference.getForeignColumnName());
					referenceElement.setAttribute("local", reference.getLocalColumnName());
					foreignKeyElement.appendChild(referenceElement);
				}
				tableElement.appendChild(foreignKeyElement);
			}

			databaseElement.appendChild(tableElement);
		}

		document.appendChild(databaseElement);
		
		return document;		
	}
	
	private Database getDatabase(String catalog, Schema schema, JavaPackage javaPackage) {
		Connection connection = getConnection();
		try {
			DatabaseModelReader modelReader = new DatabaseModelReader(connection);
			modelReader.setCatalog(StringUtils.hasText(catalog) ? catalog : modelReader.getConnection().getCatalog());
			modelReader.setSchema(schema);
			return modelReader.getDatabase(javaPackage);
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to get database model: " + e.getMessage());
		} finally {
			connectionProvider.closeConnection(connection);
		}
	}
	
	private Connection getConnection() {
		connectionProvider.configure(propFileOperations.getProperties(Path.SPRING_CONFIG_ROOT, "database.properties"));
		try {
			return connectionProvider.getConnection();
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to get database connection: " + e.getMessage());
		}		
	}
	
	private String getDbreXmlPath() {
		return pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, DbrePath.DBRE_XML_FILE.getPath());
	}
}
