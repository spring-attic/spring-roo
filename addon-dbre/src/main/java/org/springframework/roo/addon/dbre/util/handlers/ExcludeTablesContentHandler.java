package org.springframework.roo.addon.dbre.util.handlers;

import java.util.Set;

import org.springframework.roo.support.util.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * {@link ContentHandler} implementation for finding the excludeTables attribute of the database
 * element in the .roo-dbre file and creating a set of excluded table names.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public final class ExcludeTablesContentHandler extends DefaultHandler {
	private Set<String> excludeTables;
	
	public ExcludeTablesContentHandler() {
		super();
	}

	public Set<String> getExcludeTables() {
		return excludeTables;
	}
	
	@Override 
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equals("database")) {
			excludeTables = StringUtils.commaDelimitedListToSet(attributes.getValue("excludeTables"));
		}
	}
}
