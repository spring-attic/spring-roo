package org.springframework.roo.addon.dbre.model;

import java.io.InputStream;
import java.util.EmptyStackException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.roo.model.JavaPackage;
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

    public static enum IndexType {
        INDEX, UNIQUE
    }

    public static final String DESCRIPTION = "description";
    public static final String FOREIGN = "foreign";
    public static final String FOREIGN_TABLE = "foreignTable";
    public static final String LOCAL = "local";
    public static final String NAME = "name";
    public static final String ON_DELETE = "onDelete";
    public static final String ON_UPDATE = "onUpdate";

    public static final String REFERENCE = "reference";

    /**
     * Adds an <option key="foo" value="true"> element as a child of the given
     * parent element
     * 
     * @param document the XML document containing the parent and child
     *            (required)
     * @param parent the parent element to which to add a child (required)
     * @param key the option key/name (required)
     * @param value the option value
     */
    private static void addBooleanOptionElement(final Document document,
            final Element parent, final String key, final boolean value) {
        parent.appendChild(createOptionElement(key, String.valueOf(value),
                document));
    }

    private static void addForeignKeyElements(
            final Set<ForeignKey> foreignKeys, final boolean exported,
            final Element tableElement, final Document document) {
        for (final ForeignKey foreignKey : foreignKeys) {
            final Element foreignKeyElement = document
                    .createElement("foreign-key");
            foreignKeyElement.setAttribute(NAME, foreignKey.getName());
            foreignKeyElement.setAttribute(FOREIGN_TABLE,
                    foreignKey.getForeignTableName());
            foreignKeyElement.setAttribute(ON_DELETE, foreignKey.getOnDelete()
                    .getCode());
            foreignKeyElement.setAttribute(ON_UPDATE, foreignKey.getOnUpdate()
                    .getCode());

            final String foreignSchemaName = foreignKey.getForeignSchemaName();
            if (!DbreModelService.NO_SCHEMA_REQUIRED.equals(foreignSchemaName)) {
                foreignKeyElement.appendChild(createOptionElement(
                        "foreignSchemaName", foreignSchemaName, document));
            }

            foreignKeyElement.appendChild(createOptionElement("exported",
                    String.valueOf(exported), document));

            for (final Reference reference : foreignKey.getReferences()) {
                final Element referenceElement = document
                        .createElement(REFERENCE);
                referenceElement.setAttribute(FOREIGN,
                        reference.getForeignColumnName());
                referenceElement.setAttribute(LOCAL,
                        reference.getLocalColumnName());
                foreignKeyElement.appendChild(referenceElement);
            }
            tableElement.appendChild(foreignKeyElement);
        }
    }

    private static void addIndices(final Table table,
            final Element tableElement, final IndexType indexType) {
        final List<Element> elements = XmlUtils.findElements(indexType.name()
                .toLowerCase(), tableElement);
        for (final Element element : elements) {
            final Index index = new Index(element.getAttribute(NAME));
            index.setUnique(indexType == IndexType.UNIQUE);
            final List<Element> indexColumnElements = XmlUtils.findElements(
                    indexType.name().toLowerCase() + "-column", element);
            for (final Element indexColumnElement : indexColumnElements) {
                final IndexColumn indexColumn = new IndexColumn(
                        indexColumnElement.getAttribute(NAME));
                index.addColumn(indexColumn);
            }
            table.addIndex(index);
        }
    }

    private static Element createOptionElement(final String key,
            final String value, final Document document) {
        final Element option = document.createElement("option");
        option.setAttribute("key", key);
        option.setAttribute("value", value);
        return option;
    }

    public static Document getDatabaseDocument(final Database database) {
        final Document document = XmlUtils.getDocumentBuilder().newDocument();
        final Comment comment = document
                .createComment("WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.");
        document.appendChild(comment);

        final Element databaseElement = document.createElement("database");
        databaseElement.setAttribute(NAME, "deprecated");

        if (database.getDestinationPackage() != null) {
            databaseElement.setAttribute("package", database
                    .getDestinationPackage().getFullyQualifiedPackageName());
        }

        databaseElement.appendChild(createOptionElement("moduleName",
                database.getModuleName(), document));
        addBooleanOptionElement(document, databaseElement, "activeRecord",
                database.isActiveRecord());
        addBooleanOptionElement(document, databaseElement,
                "includeNonPortableAttributes",
                database.isIncludeNonPortableAttributes());
        addBooleanOptionElement(document, databaseElement,
                "disableVersionFields", database.isDisableVersionFields());
        addBooleanOptionElement(document, databaseElement,
                "disableGeneratedIdentifiers",
                database.isDisableGeneratedIdentifiers());
        addBooleanOptionElement(document, databaseElement, "testAutomatically",
                database.isTestAutomatically());

        for (final Table table : database.getTables()) {
            final Element tableElement = document.createElement("table");
            tableElement.setAttribute(NAME, table.getName());
            final String schemaName = table.getSchema().getName();
            if (!DbreModelService.NO_SCHEMA_REQUIRED.equals(schemaName)) {
                tableElement.setAttribute("alias", schemaName);
            }
            if (StringUtils.isNotBlank(table.getDescription())) {
                tableElement.setAttribute(DESCRIPTION, table.getDescription());
            }

            for (final Column column : table.getColumns()) {
                final Element columnElement = document.createElement("column");
                columnElement.setAttribute(NAME, column.getName());
                if (StringUtils.isNotBlank(column.getDescription())) {
                    columnElement.setAttribute(DESCRIPTION,
                            column.getDescription());
                }

                columnElement.setAttribute("primaryKey",
                        String.valueOf(column.isPrimaryKey()));
                columnElement.setAttribute("required",
                        String.valueOf(column.isRequired()));
                columnElement.setAttribute("size",
                        String.valueOf(column.getColumnSize()));
                columnElement.setAttribute("scale",
                        String.valueOf(column.getScale()));
                columnElement.setAttribute("type", column.getDataType() + ","
                        + column.getTypeName());

                tableElement.appendChild(columnElement);
            }

            addForeignKeyElements(table.getImportedKeys(), false, tableElement,
                    document);
            addForeignKeyElements(table.getExportedKeys(), true, tableElement,
                    document);

            for (final Index index : table.getIndices()) {
                final Element indexElement = document.createElement(index
                        .isUnique() ? IndexType.UNIQUE.name().toLowerCase()
                        : IndexType.INDEX.name().toLowerCase());
                indexElement.setAttribute(NAME, index.getName());
                for (final IndexColumn indexColumn : index.getColumns()) {
                    final Element indexColumnElement = document
                            .createElement((index.isUnique() ? IndexType.UNIQUE
                                    .name().toLowerCase() : IndexType.INDEX
                                    .name().toLowerCase())
                                    + "-column");
                    indexColumnElement
                            .setAttribute(NAME, indexColumn.getName());
                    indexElement.appendChild(indexColumnElement);
                }
                tableElement.appendChild(indexElement);
            }

            databaseElement.appendChild(tableElement);
        }

        document.appendChild(databaseElement);

        // ROO-2355: transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
        // "http://db.apache.org/torque/dtd/database_3_3.dtd");

        return document;
    }

    static Database readDatabase(final InputStream inputStream) {
        try {
            final SAXParserFactory spf = SAXParserFactory.newInstance();
            final SAXParser parser = spf.newSAXParser();
            final DatabaseContentHandler contentHandler = new DatabaseContentHandler();
            parser.parse(inputStream, contentHandler);
            return contentHandler.getDatabase();
        }
        catch (final EmptyStackException e) {
            throw new IllegalStateException(
                    "Unable to read database from XML file", e);
        }
        catch (final Exception e) {
            if (e.getMessage().contains("Invalid byte")) {
                throw new IllegalStateException(
                        "Invalid content in XML file. There may hidden characters in the file, such as the byte order mark (BOM). Try re-saving the xml in a text editor",
                        e);
            }
            throw new IllegalStateException(e);
        }
    }

    static Database readDatabaseWithDom(final InputStream inputStream) {
        final Document document = XmlUtils.readXml(inputStream);
        final Element databaseElement = document.getDocumentElement();

        final Set<Table> tables = new LinkedHashSet<Table>();

        final List<Element> tableElements = XmlUtils.findElements("table",
                databaseElement);
        for (final Element tableElement : tableElements) {
            final String alias = tableElement.getAttribute("alias");
            final String schemaName = StringUtils.defaultIfEmpty(alias,
                    databaseElement.getAttribute(NAME));
            final Table table = new Table(tableElement.getAttribute(NAME),
                    new Schema(schemaName));
            if (StringUtils.isNotBlank(tableElement.getAttribute(DESCRIPTION))) {
                table.setDescription(tableElement.getAttribute(DESCRIPTION));
            }

            final List<Element> columnElements = XmlUtils.findElements(
                    "column", tableElement);
            for (final Element columnElement : columnElements) {
                final String type = columnElement.getAttribute("type");
                final String[] dataTypeAndName = StringUtils.split(type, ",");
                final int dataType = Integer.parseInt(dataTypeAndName[0]);
                final String typeName = dataTypeAndName[1];

                final int columnSize = Integer.parseInt(columnElement
                        .getAttribute("size"));
                final int scale = Integer.parseInt(columnElement
                        .getAttribute("scale"));

                final Column column = new Column(
                        columnElement.getAttribute(NAME), dataType, typeName,
                        columnSize, scale);
                column.setDescription(columnElement.getAttribute(DESCRIPTION));
                column.setPrimaryKey(Boolean.parseBoolean(columnElement
                        .getAttribute("primaryKey")));
                column.setRequired(Boolean.parseBoolean(columnElement
                        .getAttribute("required")));

                table.addColumn(column);
            }

            final List<Element> foreignKeyElements = XmlUtils.findElements(
                    "foreign-key", tableElement);
            for (final Element foreignKeyElement : foreignKeyElements) {
                final ForeignKey foreignKey = new ForeignKey(
                        foreignKeyElement.getAttribute(NAME),
                        foreignKeyElement.getAttribute(FOREIGN_TABLE));
                foreignKey.setOnDelete(CascadeAction
                        .getCascadeAction(foreignKeyElement
                                .getAttribute(ON_DELETE)));
                foreignKey.setOnUpdate(CascadeAction
                        .getCascadeAction(foreignKeyElement
                                .getAttribute(ON_UPDATE)));

                final List<Element> optionElements = XmlUtils.findElements(
                        "option", foreignKeyElement);
                for (final Element optionElement : optionElements) {
                    if (optionElement.getAttribute("key").equals("exported")) {
                        foreignKey.setExported(Boolean
                                .parseBoolean(optionElement
                                        .getAttribute("value")));
                    }
                    if (optionElement.getAttribute("key").equals(
                            "foreignSchemaName")) {
                        foreignKey.setForeignSchemaName(optionElement
                                .getAttribute("value"));
                    }
                }

                final List<Element> referenceElements = XmlUtils.findElements(
                        REFERENCE, foreignKeyElement);
                for (final Element referenceElement : referenceElements) {
                    final Reference reference = new Reference(
                            referenceElement.getAttribute(LOCAL),
                            referenceElement.getAttribute(FOREIGN));
                    foreignKey.addReference(reference);
                }
                table.addImportedKey(foreignKey);
            }

            addIndices(table, tableElement, IndexType.INDEX);
            addIndices(table, tableElement, IndexType.UNIQUE);

            tables.add(table);
        }

        JavaPackage destinationPackage = null;
        if (StringUtils.isNotBlank(databaseElement.getAttribute("package"))) {
            destinationPackage = new JavaPackage(
                    databaseElement.getAttribute("package"));
        }

        final Database database = new Database(tables);
        database.setDestinationPackage(destinationPackage);

        final List<Element> optionElements = XmlUtils.findElements("option",
                databaseElement);
        for (final Element optionElement : optionElements) {
            if (optionElement.getAttribute("key").equals("moduleName")) {
                database.setModuleName(optionElement.getAttribute("value"));
            }
            if (optionElement.getAttribute("key").equals("activeRecord")) {
                database.setActiveRecord(Boolean.parseBoolean(optionElement
                        .getAttribute("value")));
            }
            if (optionElement.getAttribute("key").equals("testAutomatically")) {
                database.setTestAutomatically(Boolean
                        .parseBoolean(optionElement.getAttribute("value")));
            }
            if (optionElement.getAttribute("key").equals(
                    "includeNonPortableAttributes")) {
                database.setIncludeNonPortableAttributes(Boolean
                        .parseBoolean(optionElement.getAttribute("value")));
            }
            if (optionElement.getAttribute("key")
                    .equals("disableVersionFields")) {
                database.setDisableVersionFields(Boolean
                        .parseBoolean(optionElement.getAttribute("value")));
            }
            if (optionElement.getAttribute("key").equals(
                    "disableGeneratedIdentifiers")) {
                database.setDisableGeneratedIdentifiers(Boolean
                        .parseBoolean(optionElement.getAttribute("value")));
            }
        }

        return database;
    }
}