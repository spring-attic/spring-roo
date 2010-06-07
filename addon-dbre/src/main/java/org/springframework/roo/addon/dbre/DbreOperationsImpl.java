package org.springframework.roo.addon.dbre;

import java.io.InputStream;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.db.connection.ConnectionProvider;
import org.springframework.roo.addon.dbre.db.connection.ConnectionProviderImpl;
import org.springframework.roo.addon.dbre.db.metadata.Column;
import org.springframework.roo.addon.dbre.db.metadata.DbMetadata;
import org.springframework.roo.addon.dbre.db.metadata.ForeignKey;
import org.springframework.roo.addon.dbre.db.metadata.Index;
import org.springframework.roo.addon.dbre.db.metadata.PrimaryKey;
import org.springframework.roo.addon.dbre.db.metadata.Table;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Provides database reverse engineering configuration operations.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class DbreOperationsImpl implements DbreOperations {
	private static final Logger logger = HandlerUtils.getLogger(DbreOperationsImpl.class);
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private MetadataService metadataService;
	@Reference private PropFileOperations propFileOperations;

	public boolean isDbreAvailable() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null && fileManager.exists(pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "database.properties"));
	}

	public void displayMetadata(String table) {
		Map<String, String> map = propFileOperations.getProperties(Path.SPRING_CONFIG_ROOT, "database.properties");
		ConnectionProvider provider = new ConnectionProviderImpl(map);
		Connection connection = provider.getConnection();
		DbMetadata dbMetadata = new DbMetadata(connection);
		if (StringUtils.hasLength(table)) {
			Table t = dbMetadata.getTable(null, null, table);
			logger.log(t != null ? Level.INFO : Level.WARNING, t != null ? t.toString() : "Table " + table + " does not exist");
		} else {
			String databaseMetaData = dbMetadata.toString();
			logger.log(StringUtils.hasText(databaseMetaData) ? Level.INFO : Level.WARNING, StringUtils.hasText(databaseMetaData) ? databaseMetaData : "Database metadata unavailable");
		}
		provider.closeConnection(connection);
	}

	public void reverseEngineer() {
		Map<String, String> map = propFileOperations.getProperties(Path.SPRING_CONFIG_ROOT, "database.properties");
		ConnectionProvider provider = new ConnectionProviderImpl(map);
		Connection connection = provider.getConnection();
		DbMetadata dbMetadata = new DbMetadata(connection);
		updateDbreXml(dbMetadata);
		provider.closeConnection(connection);
	}

	private void updateDbreXml(DbMetadata dbMetadata) {
		String dbrePath = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/dbre.xml");
		MutableFile dbreMutableFile = null;

		Document dbre;
		try {
			if (fileManager.exists(dbrePath)) {
				dbreMutableFile = fileManager.updateFile(dbrePath);
				dbre = XmlUtils.getDocumentBuilder().parse(dbreMutableFile.getInputStream());
			} else {
				dbreMutableFile = fileManager.createFile(dbrePath);
				InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "dbre-template.xml");
				Assert.notNull(templateInputStream, "Could not acquire dbre.xml template");
				dbre = XmlUtils.getDocumentBuilder().parse(templateInputStream);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element databaseElement = dbre.getDocumentElement();

		// Reset isManaged attribute in tables and columns etc to false
		List<Element> tables = XmlUtils.findElements("/database/table", databaseElement);
		for (Element tableElement : tables) {
			tableElement.setAttribute("isManaged", Boolean.FALSE.toString());
			resetIsManagedAttribute(tableElement, "column");
			resetIsManagedAttribute(tableElement, "primaryKey");
			resetIsManagedAttribute(tableElement, "foreignKey");
			resetIsManagedAttribute(tableElement, "index");
		}

		// Add or update tables in xml file
		for (Table table : dbMetadata.getTables(null, null)) {
			String tableName = table.getTable();
			String tableXPath = getTableXPath(tableName);
			Element tableElement = XmlUtils.findFirstElement(tableXPath, databaseElement);
			if (tableElement == null) {
				// Create new table in xml
				tableElement = dbre.createElement("table");
			}
			tableElement.setAttribute("name", tableName);
			tableElement.setAttribute("catalog", table.getCatalog());
			tableElement.setAttribute("schema", table.getSchema());
			tableElement.setAttribute("tableType", table.getTableType());
			tableElement.setAttribute("isManaged", Boolean.TRUE.toString());
			
			// Iterate through columns
			for (Column column : table.getColumns()) {
				String columnName = column.getName();
				Element columnElement = XmlUtils.findFirstElement(tableXPath + "/column[@name = '" + columnName + "']", databaseElement);
				if (columnElement == null) {
					// Create new column for table in xml
					columnElement = dbre.createElement("column");
				}

				columnElement.setAttribute("name", columnName);
				columnElement.setAttribute("typeName", column.getTypeName());
				columnElement.setAttribute("dataType", String.valueOf(column.getDataType()));
				columnElement.setAttribute("columnSize", String.valueOf(column.getColumnSize()));
				columnElement.setAttribute("decimalDigits", String.valueOf(column.getDecimalDigits()));
				columnElement.setAttribute("remarks", column.getRemarks());
				columnElement.setAttribute("isManaged", Boolean.TRUE.toString());

				tableElement.appendChild(columnElement);
			}
			
			// Iterate through primary keys
			for (PrimaryKey primaryKey : table.getPrimaryKeys()) {
				String primaryKeyName = primaryKey.getName();
				Element primaryKeyElement = XmlUtils.findFirstElement(tableXPath + "/primaryKey[@name = '" + primaryKeyName + "']", databaseElement);
				if (primaryKeyElement == null) {
					// Create new primary key for table in xml
					primaryKeyElement = dbre.createElement("primaryKey");
				}
				primaryKeyElement.setAttribute("name", primaryKeyName);
				primaryKeyElement.setAttribute("columnName", primaryKey.getColumnName());
				primaryKeyElement.setAttribute("keySeq", String.valueOf(primaryKey.getKeySeq()));
				primaryKeyElement.setAttribute("isManaged", Boolean.TRUE.toString());
				
				tableElement.appendChild(primaryKeyElement);
			}

			// Iterate through foreign keys
			for (ForeignKey foreignKey : table.getForeignKeys()) {
				String foreignKeyName = foreignKey.getName();
				Element foreignKeyElement = XmlUtils.findFirstElement(tableXPath + "/foreignKey[@name = '" + foreignKeyName + "']", databaseElement);
				if (foreignKeyElement == null) {
					// Create new foreign key for table in xml
					foreignKeyElement = dbre.createElement("foreignKey");
				}
				foreignKeyElement.setAttribute("name", foreignKeyName);
				foreignKeyElement.setAttribute("fkTable", foreignKey.getFkTable());
				foreignKeyElement.setAttribute("isManaged", Boolean.TRUE.toString());
				
				tableElement.appendChild(foreignKeyElement);
			}
			
			// Iterate through indexes
			for (Index index : table.getIndexes()) {
				String indexName = index.getName();
				Element indexElement = XmlUtils.findFirstElement(tableXPath + "/index[@name = '" + indexName + "']", databaseElement);
				if (indexElement == null) {
					// Create new primary key for table
					indexElement = dbre.createElement("index");
				}
				indexElement.setAttribute("name", indexName);
				indexElement.setAttribute("columnName", index.getColumnName());
				indexElement.setAttribute("nonUnique", new Boolean(index.isNonUnique()).toString());
				indexElement.setAttribute("type", String.valueOf(index.getType()));
				indexElement.setAttribute("isManaged", Boolean.TRUE.toString());
				
				tableElement.appendChild(indexElement);
			}

			databaseElement.appendChild(tableElement);
		}
		
		// Check for deleted tables and columns etc in schema and remove from xml 
		for (Element tableElement : tables) {
			if (!Boolean.parseBoolean(tableElement.getAttribute("isManaged"))) {
				databaseElement.removeChild(tableElement);
			} else {
				removeTableAttributes(tableElement, "column");
				removeTableAttributes(tableElement, "primaryKey");
				removeTableAttributes(tableElement, "foreignKey");
				removeTableAttributes(tableElement, "index");
			}
		}
		
		XmlUtils.removeTextNodes(databaseElement);
		XmlUtils.writeXml(dbreMutableFile.getOutputStream(), dbre);
	}

	private void resetIsManagedAttribute(Element tableElement, String tableAttribute) {
		NodeList nodeList = tableElement.getElementsByTagName(tableAttribute);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				element.setAttribute("isManaged", Boolean.FALSE.toString());
			}
		}
	}
	
	private void removeTableAttributes(Element tableElement, String tableAttribute) {
		NodeList nodeList = tableElement.getElementsByTagName(tableAttribute);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				if (!Boolean.parseBoolean(element.getAttribute("isManaged"))) {
					tableElement.removeChild(element);
				}
			}
		}
	}

	private String getTableXPath(String tableName) {
		return "/database/table[@name = '" + tableName + "']";
	}
}