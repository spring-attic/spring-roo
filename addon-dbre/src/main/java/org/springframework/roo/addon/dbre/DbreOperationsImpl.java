package org.springframework.roo.addon.dbre;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.db.Column;
import org.springframework.roo.addon.dbre.db.DatabaseModel;
import org.springframework.roo.addon.dbre.db.ForeignKey;
import org.springframework.roo.addon.dbre.db.IdentifiableTable;
import org.springframework.roo.addon.dbre.db.Index;
import org.springframework.roo.addon.dbre.db.PrimaryKey;
import org.springframework.roo.addon.dbre.db.Table;
import org.springframework.roo.addon.dbre.jdbc.ConnectionProvider;
import org.springframework.roo.addon.dbre.jdbc.ConnectionProviderImpl;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
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

	public void displayDbMetadata(String table, String file) {
		Map<String, String> map = propFileOperations.getProperties(Path.SPRING_CONFIG_ROOT, "database.properties");
		ConnectionProvider provider = new ConnectionProviderImpl(map);
		Connection connection = provider.getConnection();
		try {
			DatabaseModel databaseModel = new DatabaseModel(connection);

			String dbMetadata;
			if (StringUtils.hasLength(table)) {
				Table t = databaseModel.getTable(new IdentifiableTable(null, null, table));
				dbMetadata = t != null ? t.toString() : null;
			} else {
				dbMetadata = databaseModel.toString();
			}

			if (StringUtils.hasText(dbMetadata)) {
				if (StringUtils.hasText(file)) {
					try {
						FileCopyUtils.copy(dbMetadata, new FileWriter(new File(file)));
						logger.info("Metadata written to file " + file);
					} catch (IOException e) {
						logger.warning("Unable to write metadata: " + e.getMessage());
					}
				} else {
					logger.info(dbMetadata);
				}
			} else {
				logger.warning("Database metadata unavailable " + (StringUtils.hasLength(table) ? " for table " + table : ""));
			}
		} finally {
			provider.closeConnection(connection);
		}
	}

	public void updateDbreXml(JavaPackage javaPackage) {
		Map<String, String> map = propFileOperations.getProperties(Path.SPRING_CONFIG_ROOT, "database.properties");
		ConnectionProvider provider = new ConnectionProviderImpl(map);
		Connection connection = provider.getConnection();
		try {
			updateDbreXml(javaPackage, new DatabaseModel(connection));
		} finally {
			provider.closeConnection(connection);
		}
	}

	private void updateDbreXml(JavaPackage javaPackage, DatabaseModel databaseModel) {
		String dbrePath = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, DbrePath.DBRE_XML_FILE.getPath());
		MutableFile dbreMutableFile = null;

		Document dbre;
		try {
			if (fileManager.exists(dbrePath)) {
				dbreMutableFile = fileManager.updateFile(dbrePath);
				dbre = XmlUtils.getDocumentBuilder().parse(dbreMutableFile.getInputStream());
			} else {
				dbreMutableFile = fileManager.createFile(dbrePath);
				InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), DbrePath.DBRE_XML_TEMPLATE.getPath());
				Assert.notNull(templateInputStream, "Could not acquire dbre xml template");
				dbre = XmlUtils.getDocumentBuilder().parse(templateInputStream);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element dbMetadataElement = dbre.getDocumentElement();
		
		// Calculate java package name to store new entities in
		String packageName;
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		String packageAttribute = dbMetadataElement.getAttribute("package");
		if (javaPackage != null ) {
			packageName = javaPackage.getFullyQualifiedPackageName();
		} else	if (packageAttribute != null) {
			packageName = packageAttribute;
		} else {
			packageName = projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName();
		}
		dbMetadataElement.setAttribute("package", packageName);
	
		Set<String> tableData = new HashSet<String>();
		
		// Add or update tables in xml file
		for (Table table : databaseModel.getTables()) {
			String tableId = table.getId();
			tableData.add(tableId);
			String tableName = table.getTable();
			String tableXPath = getSpecifiedTableXPath(tableName);
			Element tableElement = XmlUtils.findFirstElement(tableXPath, dbMetadataElement);
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
				Element columnElement = XmlUtils.findFirstElement(tableXPath + "/column[@name = '" + columnName + "']", dbMetadataElement);
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
				Element primaryKeyElement = XmlUtils.findFirstElement(tableXPath + "/primaryKey[@name = '" + primaryKeyName + "']", dbMetadataElement);
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
				Element foreignKeyElement = XmlUtils.findFirstElement(tableXPath + "/foreignKey[@name = '" + foreignKeyName + "']", dbMetadataElement);
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
				Element indexElement = XmlUtils.findFirstElement(tableXPath + "/index[@name = '" + indexName + "']", dbMetadataElement);
				if (indexElement == null) {
					// Create new index for table in xml
					indexElement = dbre.createElement("index");
				}
				indexElement.setAttribute("id", indexId);
				indexElement.setAttribute("name", indexName);
				indexElement.setAttribute("columnName", index.getColumnName());
				indexElement.setAttribute("nonUnique", new Boolean(index.isNonUnique()).toString());
				indexElement.setAttribute("type", String.valueOf(index.getType()));
				
				tableElement.appendChild(indexElement);
			}

			dbMetadataElement.appendChild(tableElement);
		}
		
		// Check for deleted tables and columns etc in schema and remove from xml 
		List<Element> xmlTables = XmlUtils.findElements(DbrePath.DBRE_TABLE_XPATH.getPath(), dbMetadataElement);
		for (Element tableElement : xmlTables) {
			if (!tableData.contains(tableElement.getAttribute("id"))) {
				dbMetadataElement.removeChild(tableElement);
			} else {
				removeTableAttributes("column", tableData, tableElement);
				removeTableAttributes("primaryKey", tableData, tableElement);
				removeTableAttributes("foreignKey", tableData, tableElement);
				removeTableAttributes("index", tableData, tableElement);
			}
		}
		
		XmlUtils.removeTextNodes(dbMetadataElement);
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

	private String getSpecifiedTableXPath(String tableName) {
		return DbrePath.DBRE_TABLE_XPATH.getPath() + "[@name = '" + tableName + "']";
	}
}