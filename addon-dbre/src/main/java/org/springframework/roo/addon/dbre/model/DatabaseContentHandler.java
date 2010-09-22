package org.springframework.roo.addon.dbre.model;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.roo.addon.dbre.model.DatabaseXmlUtils.IndexType;
import org.springframework.roo.support.util.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * {@link ContentHandler} implementation for converting the dbre.xml file into a {@link Database} object.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public final class DatabaseContentHandler extends DefaultHandler {
	private Database database;
	private String name;
	private Schema schema;
	private Set<Table> tables;
	private Table table;
	private Column column;
	private ForeignKey foreignKey;
	private Reference reference;
	private Index index;
	private IndexColumn indexColumn;
	private Set<Sequence> sequences;

	DatabaseContentHandler() {
		super();
	}

	public Database getDatabase() {
		return database;
	}

	@Override 
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equals("database")) {
			name = attributes.getValue(DatabaseXmlUtils.NAME);
			schema = new Schema(attributes.getValue("schema"));
			tables = new LinkedHashSet<Table>();
			sequences = new LinkedHashSet<Sequence>();
		} else if (qName.equals("table")) {
			populateTable(attributes);
		} else if (qName.equals("column")) {
			populateColumn(attributes);
		} else if (qName.equals("foreignKey") || qName.equals("exportedKey")) {
			populateForeignKey(attributes);
		} else if (qName.equals("reference")) {
			populateReference(attributes);
		} else if (qName.equals("unique")) {
			populateIndex(attributes, IndexType.UNIQUE);
		} else if (qName.equals("index")) {
			populateIndex(attributes, IndexType.INDEX);
		} else if (qName.equals("unique-column") || qName.equals("index-column")) {
			populateIndexColumn(attributes);
		} else if (qName.equals("sequence")) {
			sequences.add(new Sequence(attributes.getValue(DatabaseXmlUtils.NAME)));
		}
	}

	@Override 
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equals("database")) {
			database = new Database(name, schema, tables);
			database.setSequences(sequences);
		} else if (qName.equals("table")) {
			tables.add(table);
		} else if (qName.equals("column")) {
			table.addColumn(column);
		} else if (qName.equals("exportedKey")) {
			table.addExportedKey(foreignKey);
		} else if (qName.equals("foreignKey")) {
			table.addForeignKey(foreignKey);
		} else if (qName.equals("reference")) {
			foreignKey.addReference(reference);
		} else if (qName.equals("unique") || qName.equals("index")) {
			table.addIndex(index);
		} else if (qName.equals("unique-column") || qName.equals("index-column")) {
			index.addColumn(indexColumn);
		}
	}

	@Override 
	public void endDocument() throws SAXException {
		super.endDocument();
		tables = new LinkedHashSet<Table>();
		sequences = new LinkedHashSet<Sequence>();		
	}

	private void populateTable(Attributes attributes) {
		table = new Table();
		table.setName(attributes.getValue(DatabaseXmlUtils.NAME));
		if (StringUtils.hasText(attributes.getValue(DatabaseXmlUtils.DESCRIPTION))) {
			table.setDescription(DatabaseXmlUtils.DESCRIPTION);
		}
	}

	private void populateColumn(Attributes attributes) {
		column = new Column(attributes.getValue(DatabaseXmlUtils.NAME));
		column.setDescription(attributes.getValue(DatabaseXmlUtils.DESCRIPTION));
		column.setPrimaryKey(Boolean.parseBoolean(attributes.getValue("primaryKey")));
		column.setJavaType(attributes.getValue("javaType"));
		column.setRequired(Boolean.parseBoolean(attributes.getValue("required")));
		column.setSize(Integer.parseInt(attributes.getValue("size")));
		column.setType(ColumnType.valueOf(attributes.getValue("type")));
		column.setOrdinalPosition(Integer.parseInt(attributes.getValue("index")));
	}

	private void populateForeignKey(Attributes attributes) {
		foreignKey = new ForeignKey(attributes.getValue(DatabaseXmlUtils.NAME), attributes.getValue(DatabaseXmlUtils.FOREIGN_TABLE));
		foreignKey.setOnDelete(CascadeAction.getCascadeAction(attributes.getValue(DatabaseXmlUtils.ON_DELETE)));
		foreignKey.setOnUpdate(CascadeAction.getCascadeAction(attributes.getValue(DatabaseXmlUtils.ON_UPDATE)));
	}

	private void populateReference(Attributes attributes) {
		reference = new Reference();
		reference.setForeignColumnName(attributes.getValue(DatabaseXmlUtils.FOREIGN));
		reference.setLocalColumnName(attributes.getValue(DatabaseXmlUtils.LOCAL));
		reference.setSequenceNumber(new Short(attributes.getValue(DatabaseXmlUtils.SEQUENCE_NUMBER)));
	}

	private void populateIndex(Attributes attributes, IndexType indexType) {
		index = new Index(attributes.getValue(DatabaseXmlUtils.NAME));
		index.setUnique(indexType == IndexType.UNIQUE);
	}

	private void populateIndexColumn(Attributes attributes) {
		indexColumn = new IndexColumn(attributes.getValue(DatabaseXmlUtils.NAME));
		indexColumn.setOrdinalPosition(new Short(attributes.getValue(DatabaseXmlUtils.SEQUENCE_NUMBER)));
	}
}
