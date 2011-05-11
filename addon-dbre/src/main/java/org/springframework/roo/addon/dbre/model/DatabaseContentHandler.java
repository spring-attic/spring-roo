package org.springframework.roo.addon.dbre.model;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;

import org.springframework.roo.addon.dbre.model.DatabaseXmlUtils.IndexType;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * {@link ContentHandler} implementation for converting the DBRE xml file into a {@link Database} object.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public final class DatabaseContentHandler extends DefaultHandler {
	private Stack<Object> stack = new Stack<Object>();
	private Database database;
	private String name;
	private Set<Table> tables = new LinkedHashSet<Table>();
	private JavaPackage destinationPackage;
	private boolean testAutomatically;
	private boolean includeNonPortableAttributes;

	public DatabaseContentHandler() {
		super();
	}

	public Database getDatabase() {
		return database;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equals("database")) {
			stack.push(new Object()); 
			name = attributes.getValue(DatabaseXmlUtils.NAME);
			if (StringUtils.hasText(attributes.getValue("package"))) {
				destinationPackage = new JavaPackage(attributes.getValue("package"));
			}
		} else if (qName.equals("option")) {
			stack.push(new Option(attributes.getValue("key"), attributes.getValue("value")));
		} else if (qName.equals("table")) {
			stack.push(getTable(attributes));
		} else if (qName.equals("column")) {
			stack.push(getColumn(attributes));
		} else if (qName.equals("foreign-key")) {
			stack.push(getForeignKey(attributes));
		} else if (qName.equals("reference")) {
			stack.push(getReference(attributes));
		} else if (qName.equals("unique")) {
			stack.push(getIndex(attributes, IndexType.UNIQUE));
		} else if (qName.equals("index")) {
			stack.push(getIndex(attributes, IndexType.INDEX));
		} else if (qName.equals("unique-column") || qName.equals("index-column")) {
			stack.push(getIndexColumn(attributes));
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		Object tmp = stack.pop();

		if (qName.equals("option")) {
			Option option = (Option) tmp;
			if (stack.peek() instanceof ForeignKey && option.getKey().equals("exported")) {
				((ForeignKey) stack.peek()).setExported(Boolean.parseBoolean(option.getValue()));
			}
			if (option.getKey().equals("testAutomatically")) {
				testAutomatically = Boolean.parseBoolean(option.getValue());
			}
			if (option.getKey().equals("includeNonPortableAttributes")) {
				includeNonPortableAttributes = Boolean.parseBoolean(option.getValue());
			}
		} else if (qName.equals("table")) {
			tables.add((Table) tmp);
		} else if (qName.equals("column")) {
			((Table) stack.peek()).addColumn((Column) tmp);
		} else if (qName.equals("foreign-key")) {
			ForeignKey foreignKey = (ForeignKey) tmp;
			Table table = (Table) stack.peek();
			if (foreignKey.isExported()) {
				table.addExportedKey(foreignKey);
			} else {
				table.addImportedKey(foreignKey);
			}
		} else if (qName.equals("reference")) {
			((ForeignKey) stack.peek()).addReference((Reference) tmp);
		} else if (qName.equals("unique") || qName.equals("index")) {
			((Table) stack.peek()).addIndex((Index) tmp);
		} else if (qName.equals("unique-column") || qName.equals("index-column")) {
			((Index) stack.peek()).addColumn((IndexColumn) tmp);
		} else if (qName.equals("database")) {
			database = new Database(name, tables);
			database.setDestinationPackage(destinationPackage);
			database.setTestAutomatically(testAutomatically);
			database.setIncludeNonPortableAttributes(includeNonPortableAttributes);
		} else {
			stack.push(tmp);
		}
	}

	private Table getTable(Attributes attributes) {
		Table table = new Table();
		table.setName(attributes.getValue(DatabaseXmlUtils.NAME));
		table.setSchema(new Schema(name));
		if (StringUtils.hasText(attributes.getValue(DatabaseXmlUtils.DESCRIPTION))) {
			table.setDescription(DatabaseXmlUtils.DESCRIPTION);
		}
		return table;
	}

	private Column getColumn(Attributes attributes) {
		String type = attributes.getValue("type");
		String[] dataTypeAndName = StringUtils.split(type, ",");
		Assert.notNull(dataTypeAndName, "The 'type' attribute of the column element must contain a comma separated value pair, eg, type=\"12,varchar\"." + getErrorMessage());
		int dataType = Integer.parseInt(dataTypeAndName[0]);
		String typeName = dataTypeAndName[1];
		
		int columnSize;
		int scale = 0;
		String size = attributes.getValue("size");
		if (size.contains(",")) {
			String[] precisionScale = StringUtils.split(size, ",");
			columnSize = Integer.parseInt(precisionScale[0]);
			scale = Integer.parseInt(precisionScale[1]);
		} else {
			columnSize = Integer.parseInt(size);
		}
		
		if (StringUtils.hasText(attributes.getValue("scale"))) {
			scale = Integer.parseInt(attributes.getValue("scale"));
		}

		Column column = new Column(attributes.getValue(DatabaseXmlUtils.NAME), dataType, typeName, columnSize, scale);
		column.setDescription(attributes.getValue(DatabaseXmlUtils.DESCRIPTION));
		column.setPrimaryKey(Boolean.parseBoolean(attributes.getValue("primaryKey")));
		column.setRequired(Boolean.parseBoolean(attributes.getValue("required")));

		return column;
	}

	private String getErrorMessage() {
		return "Your DBRE XML file may be not be in the current format. Delete the file and execute the database reverse engineer command again.";
	}

	private ForeignKey getForeignKey(Attributes attributes) {
		ForeignKey foreignKey = new ForeignKey(attributes.getValue(DatabaseXmlUtils.NAME), attributes.getValue(DatabaseXmlUtils.FOREIGN_TABLE));
		foreignKey.setOnDelete(CascadeAction.getCascadeAction(attributes.getValue(DatabaseXmlUtils.ON_DELETE)));
		foreignKey.setOnUpdate(CascadeAction.getCascadeAction(attributes.getValue(DatabaseXmlUtils.ON_UPDATE)));
		return foreignKey;
	}

	private Reference getReference(Attributes attributes) {
		return new Reference(attributes.getValue(DatabaseXmlUtils.LOCAL), attributes.getValue(DatabaseXmlUtils.FOREIGN));
	}

	private Index getIndex(Attributes attributes, IndexType indexType) {
		Index index = new Index(attributes.getValue(DatabaseXmlUtils.NAME));
		index.setUnique(indexType == IndexType.UNIQUE);
		return index;
	}

	private IndexColumn getIndexColumn(Attributes attributes) {
		return new IndexColumn(attributes.getValue(DatabaseXmlUtils.NAME));
	}

	private static class Option {
		private String key;
		private String value;

		public Option(String key, String value) {
			this.key = key;
			this.value = value;
		}

		public String getKey() {
			return key;
		}

		public String getValue() {
			return value;
		}
	}
}
