package org.springframework.roo.addon.dbre.model;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.roo.addon.dbre.model.DatabaseXmlUtils.IndexType;
import org.springframework.roo.model.JavaPackage;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * {@link ContentHandler} implementation for converting the DBRE XML file into a
 * {@link Database} object.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class DatabaseContentHandler extends DefaultHandler {

    private static class Option extends Pair<String, String> {

        private static final long serialVersionUID = 3471455277824528758L;
        private final String key;
        private final String value;

        public Option(final String key, final String value) {
            this.key = key;
            this.value = value;

        }

        @Override
        public String getLeft() {
            return key;
        }

        @Override
        public String getRight() {
            return value;
        }

        public String setValue(final String value) {
            throw new UnsupportedOperationException();
        }
    }

    private boolean activeRecord;
    private Database database;
    private JavaPackage destinationPackage;
    private boolean includeNonPortableAttributes;
    private String moduleName;
    private final Stack<Object> stack = new Stack<Object>();
    private final Set<Table> tables = new LinkedHashSet<Table>();

    private boolean testAutomatically;

    /**
     * Constructor
     */
    public DatabaseContentHandler() {
        super();
    }

    @Override
    public void endElement(final String uri, final String localName,
            final String qName) throws SAXException {
        final Object tmp = stack.pop();

        if (qName.equals("option")) {
            final Option option = (Option) tmp;
            if (stack.peek() instanceof ForeignKey) {
                if (option.getKey().equals("exported")) {
                    ((ForeignKey) stack.peek()).setExported(Boolean
                            .parseBoolean(option.getValue()));
                }
                if (option.getKey().equals("foreignSchemaName")) {
                    ((ForeignKey) stack.peek()).setForeignSchemaName(option
                            .getValue());
                }
            }
            if (option.getKey().equals("moduleName")) {
                moduleName = option.getValue();
            }
            if (option.getKey().equals("includeNonPortableAttributes")) {
                includeNonPortableAttributes = Boolean.parseBoolean(option
                        .getValue());
            }
            if (option.getKey().equals("activeRecord")) {
                activeRecord = Boolean.parseBoolean(option.getValue());
            }
            if (option.getKey().equals("testAutomatically")) {
                testAutomatically = Boolean.parseBoolean(option.getValue());
            }
        }
        else if (qName.equals("table")) {
            tables.add((Table) tmp);
        }
        else if (qName.equals("column")) {
            ((Table) stack.peek()).addColumn((Column) tmp);
        }
        else if (qName.equals("foreign-key")) {
            final ForeignKey foreignKey = (ForeignKey) tmp;
            final Table table = (Table) stack.peek();
            if (foreignKey.isExported()) {
                table.addExportedKey(foreignKey);
            }
            else {
                table.addImportedKey(foreignKey);
            }
        }
        else if (qName.equals("reference")) {
            ((ForeignKey) stack.peek()).addReference((Reference) tmp);
        }
        else if (qName.equals("unique") || qName.equals("index")) {
            ((Table) stack.peek()).addIndex((Index) tmp);
        }
        else if (qName.equals("unique-column") || qName.equals("index-column")) {
            ((Index) stack.peek()).addColumn((IndexColumn) tmp);
        }
        else if (qName.equals("database")) {
            database = new Database(tables);
            database.setModuleName(moduleName);
            database.setDestinationPackage(destinationPackage);
            database.setIncludeNonPortableAttributes(includeNonPortableAttributes);
            database.setActiveRecord(activeRecord);
            database.setTestAutomatically(testAutomatically);
        }
        else {
            stack.push(tmp);
        }
    }

    private Column getColumn(final Attributes attributes) {
        final String type = attributes.getValue("type");
        final String[] dataTypeAndName = StringUtils.split(type, ",");
        Validate.notNull(
                dataTypeAndName,
                "The 'type' attribute of the column element must contain a comma separated value pair, eg, type=\"12,varchar\"."
                        + getErrorMessage());
        final int dataType = Integer.parseInt(dataTypeAndName[0]);
        final String typeName = dataTypeAndName[1];

        int columnSize;
        int scale = 0;
        final String size = attributes.getValue("size");
        if (size.contains(",")) {
            final String[] precisionScale = StringUtils.split(size, ",");
            columnSize = Integer.parseInt(precisionScale[0]);
            scale = Integer.parseInt(precisionScale[1]);
        }
        else {
            columnSize = Integer.parseInt(size);
        }

        if (StringUtils.isNotBlank(attributes.getValue("scale"))) {
            scale = Integer.parseInt(attributes.getValue("scale"));
        }

        final Column column = new Column(
                attributes.getValue(DatabaseXmlUtils.NAME), dataType, typeName,
                columnSize, scale);
        column.setDescription(attributes.getValue(DatabaseXmlUtils.DESCRIPTION));
        column.setPrimaryKey(Boolean.parseBoolean(attributes
                .getValue("primaryKey")));
        column.setRequired(Boolean.parseBoolean(attributes.getValue("required")));

        return column;
    }

    public Database getDatabase() {
        return database;
    }

    private String getErrorMessage() {
        return "Your DBRE XML file may be not be in the current format. Delete the file and execute the database reverse engineer command again.";
    }

    private ForeignKey getForeignKey(final Attributes attributes) {
        final ForeignKey foreignKey = new ForeignKey(
                attributes.getValue(DatabaseXmlUtils.NAME),
                attributes.getValue(DatabaseXmlUtils.FOREIGN_TABLE));
        foreignKey.setOnDelete(CascadeAction.getCascadeAction(attributes
                .getValue(DatabaseXmlUtils.ON_DELETE)));
        foreignKey.setOnUpdate(CascadeAction.getCascadeAction(attributes
                .getValue(DatabaseXmlUtils.ON_UPDATE)));
        return foreignKey;
    }

    private Index getIndex(final Attributes attributes,
            final IndexType indexType) {
        final Index index = new Index(
                attributes.getValue(DatabaseXmlUtils.NAME));
        index.setUnique(indexType == IndexType.UNIQUE);
        return index;
    }

    private IndexColumn getIndexColumn(final Attributes attributes) {
        return new IndexColumn(attributes.getValue(DatabaseXmlUtils.NAME));
    }

    private Reference getReference(final Attributes attributes) {
        return new Reference(attributes.getValue(DatabaseXmlUtils.LOCAL),
                attributes.getValue(DatabaseXmlUtils.FOREIGN));
    }

    private Table getTable(final Attributes attributes) {
        final Table table = new Table(
                attributes.getValue(DatabaseXmlUtils.NAME), new Schema(
                        attributes.getValue("alias")));
        if (StringUtils.isNotBlank(attributes
                .getValue(DatabaseXmlUtils.DESCRIPTION))) {
            table.setDescription(DatabaseXmlUtils.DESCRIPTION);
        }
        return table;
    }

    @Override
    public void startElement(final String uri, final String localName,
            final String qName, final Attributes attributes)
            throws SAXException {
        if (qName.equals("database")) {
            stack.push(new Object());
            if (StringUtils.isNotBlank(attributes.getValue("package"))) {
                destinationPackage = new JavaPackage(
                        attributes.getValue("package"));
            }
        }
        else if (qName.equals("option")) {
            stack.push(new Option(attributes.getValue("key"), attributes
                    .getValue("value")));
        }
        else if (qName.equals("table")) {
            stack.push(getTable(attributes));
        }
        else if (qName.equals("column")) {
            stack.push(getColumn(attributes));
        }
        else if (qName.equals("foreign-key")) {
            stack.push(getForeignKey(attributes));
        }
        else if (qName.equals("reference")) {
            stack.push(getReference(attributes));
        }
        else if (qName.equals("unique")) {
            stack.push(getIndex(attributes, IndexType.UNIQUE));
        }
        else if (qName.equals("index")) {
            stack.push(getIndex(attributes, IndexType.INDEX));
        }
        else if (qName.equals("unique-column") || qName.equals("index-column")) {
            stack.push(getIndexColumn(attributes));
        }
    }
}
