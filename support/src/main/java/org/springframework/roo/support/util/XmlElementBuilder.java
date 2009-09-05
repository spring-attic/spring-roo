package org.springframework.roo.support.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Very simple convenience Builder for XML {@code Element}s
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
public class XmlElementBuilder {	

	private Element element;

	/**
	 * Create a new Element instance.
	 * 
	 * @param name The name of the element (required, not empty)
	 * @param document The parent document (required)
	 */
	public XmlElementBuilder (String name, Document document) {	
		Assert.hasText(name, "Element name required.");
		Assert.notNull(document, "Owner document required");
		element = document.createElement(name);
	}

	/**
	 * Add an attribute to the current element.
	 * 
	 * @param qName The attribute name (required, not empty)
	 * @param value The value of the attribute (required)
	 * @return
	 */
	public XmlElementBuilder addAttribute(String qName, String value) {
		Assert.hasText(qName, "Attribute qName required.");
		Assert.notNull(value, "Attribute value required.");
		element.setAttribute(qName, value);
		return this;
	}

	/**
	 * Add a child element to the current element.
	 * 
	 * @param element The new element (required)
	 * @return The builder for the current element
	 */
	public XmlElementBuilder addChild(Element element) {
		Assert.notNull(element, "Element required.");
		this.element.appendChild(element);
		return this;
	}

	/**
	 * Add text contents to the current element. This will overwrite
	 * any previous text content.
	 * 
	 * @param element The text content (required, not empty)
	 * @return The builder for the current element
	 */
	public XmlElementBuilder setText(String text) {
		Assert.hasText(text, "Text content required.");
		element.setTextContent(text);
		return this;
	}
	
	/**
	 * Get the element instance.
	 * 
	 * @return The element.
	 */
	public Element build() {
		return element;
	}
}
