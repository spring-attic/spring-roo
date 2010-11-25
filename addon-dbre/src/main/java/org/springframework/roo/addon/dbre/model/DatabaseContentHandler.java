package org.springframework.roo.addon.dbre.model;

import java.util.Stack;

import org.springframework.roo.addon.dbre.model.DatabaseXmlUtils.IndexType;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.support.util.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * {@link ContentHandler} implementation for converting the .roo-dbre xml file into a {@link Database} object.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public final class DatabaseContentHandler extends DefaultHandler {
	private Database database;
	private Stack<Object> stack;

	public DatabaseContentHandler() {
		super();
		stack = new Stack<Object>();
	}

	public Database getDatabase() {
		database.initialize();
		return database;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equals("database")) {
			stack.push(new Database());
			((Database) stack.peek()).setName(attributes.getValue(DatabaseXmlUtils.NAME));
			if (StringUtils.hasText(attributes.getValue("package"))) {
				((Database) stack.peek()).setDestinationPackage(new JavaPackage(attributes.getValue("package")));
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

		if (qName.equals("database")) {
			database = (Database) tmp;
		} else if (qName.equals("option")) {
			Option option = (Option) tmp;
			if (stack.peek() instanceof ForeignKey) {
				if (option.getKey().equals("exported")) {
					((ForeignKey) stack.peek()).setExported(new Boolean(option.getValue()));
				}
			} else if (stack.peek() instanceof Database) {
				if (option.getKey().equals(DatabaseXmlUtils.INCLUDED_TABLES)) {
					((Database) stack.peek()).setIncludeTables(option.getValue());
				}
			} else if (stack.peek() instanceof Database) {
				if (option.getKey().equals(DatabaseXmlUtils.EXCLUDED_TABLES)) {
					((Database) stack.peek()).setExcludeTables(option.getValue());
				}
			}
		} else if (qName.equals("table")) {
			((Database) stack.peek()).addTable((Table) tmp);
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
		} else {
			stack.push(tmp);
		}
	}

	private Table getTable(Attributes attributes) {
		Table table = new Table();
		table.setName(attributes.getValue(DatabaseXmlUtils.NAME));
		if (StringUtils.hasText(attributes.getValue(DatabaseXmlUtils.DESCRIPTION))) {
			table.setDescription(DatabaseXmlUtils.DESCRIPTION);
		}
		return table;
	}

	private Column getColumn(Attributes attributes) {
		Column column = new Column(attributes.getValue(DatabaseXmlUtils.NAME));
		column.setDescription(attributes.getValue(DatabaseXmlUtils.DESCRIPTION));
		column.setPrimaryKey(Boolean.parseBoolean(attributes.getValue("primaryKey")));
		column.setJavaType(attributes.getValue("javaType"));
		column.setRequired(Boolean.parseBoolean(attributes.getValue("required")));

		String size = attributes.getValue("size");
		if (size.contains(",")) {
			String[] precisionScale = StringUtils.split(size, ",");
			column.setPrecision(Integer.parseInt(precisionScale[0]));
			column.setScale(Integer.parseInt(precisionScale[1]));
		} else {
			column.setLength(Integer.parseInt(size));
		}

		column.setType(ColumnType.valueOf(attributes.getValue("type")));
		return column;
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
