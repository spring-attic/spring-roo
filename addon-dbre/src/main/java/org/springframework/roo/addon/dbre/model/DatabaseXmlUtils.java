package org.springframework.roo.addon.dbre.model;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.EmptyStackException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Assists converting a {@link Database} to and from XML using DOM or SAX.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public abstract class DatabaseXmlUtils {
	public static final String NAME = "name";
	public static final String LOCAL = "local";
	public static final String FOREIGN = "foreign";
	public static final String FOREIGN_TABLE = "foreignTable";
	public static final String DESCRIPTION = "description";
	public static final String REFERENCE = "reference";
	public static final String ON_UPDATE = "onUpdate";
	public static final String ON_DELETE = "onDelete";

	public static enum IndexType {
		INDEX, UNIQUE
	}

	static Schema readSchemaFromInputStreamWithDom(InputStream inputStream) {
		Document document = getDocument(inputStream);
		Element databaseElement = document.getDocumentElement();
		return new Schema(databaseElement.getAttribute("schema"));
	}

	static Schema readSchemaFromInputStream(InputStream inputStream) {
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser parser = spf.newSAXParser();
			SchemaContentHandler contentHandler = new SchemaContentHandler();
			parser.parse(inputStream, contentHandler);
			return contentHandler.getSchema();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	static Database readDatabaseStructureFromInputStream(InputStream inputStream) {
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser parser = spf.newSAXParser();
			DatabaseContentHandler contentHandler = new DatabaseContentHandler();
			parser.parse(inputStream, contentHandler);
			return contentHandler.getDatabase();
		} catch (EmptyStackException e) {
			throw new IllegalStateException("Unable to read database from XML file", e);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	static Database readDatabaseStructureFromInputStreamWithDom(InputStream inputStream) {
		Document document = getDocument(inputStream);
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
				String type = columnElement.getAttribute("type");
				String[] dataTypeAndName = StringUtils.split(type, ",");
				int dataType = Integer.parseInt(dataTypeAndName[0]);
				String typeName = dataTypeAndName[1];

				int columnSize = Integer.parseInt(columnElement.getAttribute("size"));
				int scale = Integer.parseInt(columnElement.getAttribute("scale"));

				Column column = new Column(columnElement.getAttribute(NAME), dataType, typeName, columnSize, scale);
				column.setDescription(columnElement.getAttribute(DESCRIPTION));
				column.setPrimaryKey(Boolean.parseBoolean(columnElement.getAttribute("primaryKey")));
				column.setRequired(Boolean.parseBoolean(columnElement.getAttribute("required")));

				table.addColumn(column);
			}

			List<Element> foreignKeyElements = XmlUtils.findElements("foreign-key", tableElement);
			for (Element foreignKeyElement : foreignKeyElements) {
				ForeignKey foreignKey = new ForeignKey(foreignKeyElement.getAttribute(NAME), foreignKeyElement.getAttribute(FOREIGN_TABLE));
				foreignKey.setOnDelete(CascadeAction.getCascadeAction(foreignKeyElement.getAttribute(ON_DELETE)));
				foreignKey.setOnUpdate(CascadeAction.getCascadeAction(foreignKeyElement.getAttribute(ON_UPDATE)));

				List<Element> optionElements = XmlUtils.findElements("option", foreignKeyElement);
				for (Element optionElement : optionElements) {
					if (optionElement.getAttribute("key").equals("exported")) {
						foreignKey.setExported(Boolean.parseBoolean(optionElement.getAttribute("value")));
						break; // Don't process any more <option> elements
					}
				}

				List<Element> referenceElements = XmlUtils.findElements(REFERENCE, foreignKeyElement);
				for (Element referenceElement : referenceElements) {
					Reference reference = new Reference(referenceElement.getAttribute(LOCAL), referenceElement.getAttribute(FOREIGN));
					foreignKey.addReference(reference);
				}
				table.addImportedKey(foreignKey);
			}

			addIndices(table, tableElement, IndexType.INDEX);
			addIndices(table, tableElement, IndexType.UNIQUE);

			tables.add(table);
		}

		JavaPackage destinationPackage = null;
		if (StringUtils.hasText(databaseElement.getAttribute("package"))) {
			destinationPackage = new JavaPackage(databaseElement.getAttribute("package"));
		}

		Database database = new Database(databaseElement.getAttribute(NAME), tables);
		database.setDestinationPackage(destinationPackage);

		List<Element> optionElements = XmlUtils.findElements("option", databaseElement);
		for (Element optionElement : optionElements) {
			if (optionElement.getAttribute("key").equals("testAutomatically")) {
				database.setTestAutomatically(Boolean.parseBoolean(optionElement.getAttribute("value")));
			}
			if (optionElement.getAttribute("key").equals("includeNonPortableAttributes")) {
				database.setIncludeNonPortableAttributes(Boolean.parseBoolean(optionElement.getAttribute("value")));
			}
		}

		return database;
	}

	static void writeDatabaseStructureToOutputStream(Database database, OutputStream outputStream) {
		Document document = XmlUtils.getDocumentBuilder().newDocument();
		Comment comment = document.createComment("WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.");
		document.appendChild(comment);

		Element databaseElement = document.createElement("database");
		databaseElement.setAttribute(NAME, database.getName());

		if (database.getDestinationPackage() != null) {
			databaseElement.setAttribute("package", database.getDestinationPackage().getFullyQualifiedPackageName());
		}

		databaseElement.appendChild(createOptionElement("testAutomatically", String.valueOf(database.isTestAutomatically()), document));
		databaseElement.appendChild(createOptionElement("includeNonPortableAttributes", String.valueOf(database.isIncludeNonPortableAttributes()), document));

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
				columnElement.setAttribute("size", String.valueOf(column.getColumnSize()));
				columnElement.setAttribute("scale", String.valueOf(column.getScale()));
				columnElement.setAttribute("type", column.getDataType() + "," + column.getTypeName());

				tableElement.appendChild(columnElement);
			}

			addForeignKeyElements(table.getImportedKeys(), false, tableElement, document);
			addForeignKeyElements(table.getExportedKeys(), true, tableElement, document);

			for (Index index : table.getIndices()) {
				Element indexElement = document.createElement(index.isUnique() ? IndexType.UNIQUE.name().toLowerCase() : IndexType.INDEX.name().toLowerCase());
				indexElement.setAttribute(NAME, index.getName());
				for (IndexColumn indexColumn : index.getColumns()) {
					Element indexColumnElement = document.createElement((index.isUnique() ? IndexType.UNIQUE.name().toLowerCase() : IndexType.INDEX.name().toLowerCase()) + "-column");
					indexColumnElement.setAttribute(NAME, indexColumn.getName());
					indexElement.appendChild(indexColumnElement);
				}
				tableElement.appendChild(indexElement);
			}

			databaseElement.appendChild(tableElement);
		}

		document.appendChild(databaseElement);

		Transformer transformer = XmlUtils.createIndentingTransformer();
		transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://db.apache.org/torque/dtd/database_3_3.dtd");

		XmlUtils.writeXml(transformer, outputStream, document);
	}

	private static void addForeignKeyElements(Set<ForeignKey> foreignKeys, boolean exported, Element tableElement, Document document) {
		for (ForeignKey foreignKey : foreignKeys) {
			Element foreignKeyElement = document.createElement("foreign-key");
			String foreignTableName = foreignKey.getForeignTableName();
			foreignKeyElement.setAttribute(NAME, foreignKey.getName());
			foreignKeyElement.setAttribute(FOREIGN_TABLE, foreignTableName);
			foreignKeyElement.setAttribute(ON_DELETE, foreignKey.getOnDelete().getCode());
			foreignKeyElement.setAttribute(ON_UPDATE, foreignKey.getOnUpdate().getCode());
			foreignKeyElement.appendChild(createOptionElement("exported", String.valueOf(exported), document));

			for (Reference reference : foreignKey.getReferences()) {
				Element referenceElement = document.createElement(REFERENCE);
				referenceElement.setAttribute(FOREIGN, reference.getForeignColumnName());
				referenceElement.setAttribute(LOCAL, reference.getLocalColumnName());
				foreignKeyElement.appendChild(referenceElement);
			}
			tableElement.appendChild(foreignKeyElement);
		}
	}

	private static Element createOptionElement(String key, String value, Document document) {
		Element option = document.createElement("option");
		option.setAttribute("key", key);
		option.setAttribute("value", value);
		return option;
	}

	private static void addIndices(Table table, Element tableElement, IndexType indexType) {
		List<Element> elements = XmlUtils.findElements(indexType.name().toLowerCase(), tableElement);
		for (Element element : elements) {
			Index index = new Index(element.getAttribute(NAME));
			index.setUnique(indexType == IndexType.UNIQUE);
			List<Element> indexColumnElements = XmlUtils.findElements(indexType.name().toLowerCase() + "-column", element);
			for (Element indexColumnElement : indexColumnElements) {
				IndexColumn indexColumn = new IndexColumn(indexColumnElement.getAttribute(NAME));
				index.addColumn(indexColumn);
			}
			table.addIndex(index);
		}
	}

	private static Document getDocument(InputStream inputStream) {
		try {
			DocumentBuilder builder = XmlUtils.getDocumentBuilder();
			builder.setErrorHandler(null);
			return builder.parse(inputStream);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}