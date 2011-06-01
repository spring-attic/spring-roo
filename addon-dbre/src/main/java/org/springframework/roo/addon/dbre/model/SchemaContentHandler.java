package org.springframework.roo.addon.dbre.model;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * {@link ContentHandler} implementation for finding the schema attribute of the database
 * element in the DBRE XML file and creating a {@link Schema} object.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public final class SchemaContentHandler extends DefaultHandler {
	private Schema schema;
	
	public SchemaContentHandler() {
		super();
	}

	public Schema getSchema() {
		return schema;
	}
	
	@Override 
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equals("database")) {
			schema = new Schema(attributes.getValue("name"));
		}
	}
}
