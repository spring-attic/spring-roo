package org.springframework.roo.addon.dbre.model;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * {@link ContentHandler} for finding the schema attribute of the
 * <code>database</code> element in the DBRE XML file and creating a
 * {@link Schema} object.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class SchemaContentHandler extends DefaultHandler {

    private Schema schema;

    /**
     * Constructor for no schema
     */
    public SchemaContentHandler() {
    }

    /**
     * Returns the parsed schema
     * 
     * @return <code>null</code> if not parsed yet
     */
    public Schema getSchema() {
        return schema;
    }

    @Override
    public void startElement(final String uri, final String localName,
            final String qName, final Attributes attributes)
            throws SAXException {
        if (qName.equals("database")) {
            schema = new Schema(attributes.getValue("name"));
        }
    }
}
