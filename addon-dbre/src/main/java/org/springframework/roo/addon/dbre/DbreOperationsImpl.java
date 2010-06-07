package org.springframework.roo.addon.dbre;

import java.io.InputStream;
import java.sql.Connection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.db.connection.ConnectionProvider;
import org.springframework.roo.addon.dbre.db.connection.ConnectionProviderImpl;
import org.springframework.roo.addon.dbre.db.metadata.Column;
import org.springframework.roo.addon.dbre.db.metadata.DatabaseModel;
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
		DatabaseModel databaseModel = new DatabaseModel(connection);
		if (StringUtils.hasLength(table)) {
			Table t = databaseModel.getTable(null, null, table);
			logger.log(t != null ? Level.INFO : Level.WARNING, t != null ? t.toString() : "Table " + table + " does not exist");
		} else {
			String databaseMetaData = databaseModel.toString();
			logger.log(StringUtils.hasText(databaseMetaData) ? Level.INFO : Level.WARNING, StringUtils.hasText(databaseMetaData) ? databaseMetaData : "Database metadata unavailable");
		}
		provider.closeConnection(connection);
	}

	public void reverseEngineer() {
		Map<String, String> map = propFileOperations.getProperties(Path.SPRING_CONFIG_ROOT, "database.properties");
		ConnectionProvider provider = new ConnectionProviderImpl(map);
		Connection connection = provider.getConnection();
		DatabaseModel databaseModel = new DatabaseModel(connection);
		updateDbreXml(databaseModel);
		provider.closeConnection(connection);
	}

	private void updateDbreXml(DatabaseModel databaseModel) {
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
		
		Set<String> tableData = new HashSet<String>();
		
		// Add or update tables in xml file
		for (Table table : databaseModel.getTables(null, null)) {
			String tableId = table.getId();
			tableData.add(tableId);
			String tableName = table.getTable();
			String tableXPath = getTableXPath(tableName);
			Element tableElement = XmlUtils.findFirstElement(tableXPath, databaseElement);
			if (tableElement == null) {
				// Create new table in xml
				tableElement = dbre.createElement("table");
			}
			tableElement.setAttribute("id", tableId);
			tableElement.setAttribute("name", tableName);
			tableElement.setAttribute("catalog", table.getCatalog());
			tableElement.setAttribute("schema", table.getSchema());
			tableElement.setAttribute("tableType", table.getTableType());
			
			// Iterate through columns
			for (Column column : table.getColumns()) {	
				String columnId = table.getId() + "." + column.getId();
				tableData.add(columnId);

				String columnName = column.getName();
				Element columnElement = XmlUtils.findFirstElement(tableXPath + "/column[@name = '" + columnName + "']", databaseElement);
				if (columnElement == null) {
					// Create new column for table in xml
					columnElement = dbre.createElement("column");
				}

				columnElement.setAttribute("id", columnId);
				columnElement.setAttribute("name", columnName);
				columnElement.setAttribute("typeName", column.getTypeName());
				columnElement.setAttribute("dataType", String.valueOf(column.getDataType()));
				columnElement.setAttribute("columnSize", String.valueOf(column.getColumnSize()));
				columnElement.setAttribute("decimalDigits", String.valueOf(column.getDecimalDigits()));
				columnElement.setAttribute("remarks", column.getRemarks());

				tableElement.appendChild(columnElement);
			}
			
			// Iterate through primary keys
			for (PrimaryKey primaryKey : table.getPrimaryKeys()) {
				String primaryKeyId = tableId + "." + primaryKey.getId();
				tableData.add(primaryKeyId);

				String primaryKeyName = primaryKey.getName();
				Element primaryKeyElement = XmlUtils.findFirstElement(tableXPath + "/primaryKey[@name = '" + primaryKeyName + "']", databaseElement);
				if (primaryKeyElement == null) {
					// Create new primary key for table in xml
					primaryKeyElement = dbre.createElement("primaryKey");
				}
				primaryKeyElement.setAttribute("id", primaryKeyId);
				primaryKeyElement.setAttribute("name", primaryKeyName);
				primaryKeyElement.setAttribute("columnName", primaryKey.getColumnName());
				primaryKeyElement.setAttribute("keySeq", String.valueOf(primaryKey.getKeySeq()));
				
				tableElement.appendChild(primaryKeyElement);
			}

			// Iterate through foreign keys
			for (ForeignKey foreignKey : table.getForeignKeys()) {
				String foreignKeyId = tableId + "." + foreignKey.getId();
				tableData.add(foreignKeyId);

				String foreignKeyName = foreignKey.getName();
				Element foreignKeyElement = XmlUtils.findFirstElement(tableXPath + "/foreignKey[@name = '" + foreignKeyName + "']", databaseElement);
				if (foreignKeyElement == null) {
					// Create new foreign key for table in xml
					foreignKeyElement = dbre.createElement("foreignKey");
				}
				foreignKeyElement.setAttribute("id", foreignKeyId);
				foreignKeyElement.setAttribute("name", foreignKeyName);
				foreignKeyElement.setAttribute("fkTable", foreignKey.getFkTable());
				
				tableElement.appendChild(foreignKeyElement);
			}
			
			// Iterate through indexes
			for (Index index : table.getIndexes()) {
				String indexId = tableId + "." + index.getId();
				tableData.add(indexId);

				String indexName = index.getName();
				Element indexElement = XmlUtils.findFirstElement(tableXPath + "/index[@name = '" + indexName + "']", databaseElement);
				if (indexElement == null) {
					// Create new primary key for table
					indexElement = dbre.createElement("index");
				}
				indexElement.setAttribute("id", indexId);
				indexElement.setAttribute("name", indexName);
				indexElement.setAttribute("columnName", index.getColumnName());
				indexElement.setAttribute("nonUnique", new Boolean(index.isNonUnique()).toString());
				indexElement.setAttribute("type", String.valueOf(index.getType()));
				
				tableElement.appendChild(indexElement);
			}

			databaseElement.appendChild(tableElement);
		}
		
		// Check for deleted tables and columns etc in schema and remove from xml 
		List<Element> xmlTables = XmlUtils.findElements("/database/table", databaseElement);
		for (Element tableElement : xmlTables) {
			if (!tableData.contains(tableElement.getAttribute("id"))) {
				databaseElement.removeChild(tableElement);
			} else {
				removeTableAttributes("column", tableData, tableElement);
				removeTableAttributes("primaryKey", tableData, tableElement);
				removeTableAttributes("foreignKey", tableData, tableElement);
				removeTableAttributes("index", tableData, tableElement);
			}
		}
		
		XmlUtils.removeTextNodes(databaseElement);
		XmlUtils.writeXml(dbreMutableFile.getOutputStream(), dbre);
	}

	private void removeTableAttributes(String tableAttribute, Set<String> tableData, Element tableElement) {
		NodeList nodeList = tableElement.getElementsByTagName(tableAttribute);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				if (!tableData.contains(element.getAttribute("id"))) {
					tableElement.removeChild(element);
				}
			}
		}
	}

	private String getTableXPath(String tableName) {
		return "/database/table[@name = '" + tableName + "']";
	}
}