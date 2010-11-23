package org.springframework.roo.addon.dbre.model;

import java.util.Set;
import java.util.Stack;

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
	private Stack<Object> stack;
	
	public ExcludeTablesContentHandler() {
		super();
		stack = new Stack<Object>();		
	}

	public Set<String> getExcludeTables() {
		return excludeTables;
	}
	
	@Override 
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equals("database")) {
			stack.push(new Database());
		} else if (qName.equals("option") ) {
			stack.push(new Option(attributes.getValue("key"), attributes.getValue("value")));
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		Object tmp = stack.pop();

		if (qName.equals("option")) {
			Option option = (Option) tmp;
			if (stack.peek() instanceof Database) {
				if (option.getKey().equals("excludedTables")) {
					((Database) stack.peek()).setExcludeTables(option.getValue());
				}
			}
		} else {
			stack.push(tmp);
		}
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
