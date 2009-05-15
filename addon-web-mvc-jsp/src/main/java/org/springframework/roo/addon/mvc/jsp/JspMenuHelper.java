package org.springframework.roo.addon.mvc.jsp;

import java.io.InputStream;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Helper class which generates the jsp menu.
 *
 * @author Stefan Schmidt
 * @since 1.0
 */
public class JspMenuHelper {
	
	public JspMenuHelper(){	}
	
	public Document addMenuItem(String entityName, String pluralName, String projectName, InputStream existingMenu) {
		Assert.notNull(entityName, "Entity name required");
		Assert.notNull(pluralName, "Plural name required");
		Assert.notNull(projectName, "Project name required");
		Assert.notNull(existingMenu, "Menu file required");
		
		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(existingMenu);
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to parse menu.jsp", e);
		}
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		
		Element rootElement = null;
		try {
			XPathExpression expr = xpath.compile("//ul[@id='roo_menu']");
			rootElement = (Element) expr.evaluate(document, XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			throw new IllegalArgumentException("Unable to xpath expression for menu.jsp", e);
		}	
		
		Element liWrapper = null;
		Element ulSubMenu = null;
		try {
			XPathExpression expr = xpath.compile("//li[@id='roo_" + entityName.toLowerCase() + "_menu_item']");
			liWrapper = (Element) expr.evaluate(document, XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			throw new IllegalArgumentException("Unable to xpath expression for menu.jsp", e);
		}		
		
		if(liWrapper == null) {
			liWrapper = document.createElement("li");
			liWrapper.setAttribute("id", "roo_" + entityName.toLowerCase() + "_menu_item");
			Element h2 = document.createElement("h2");
			h2.setTextContent(entityName);
			liWrapper.appendChild(h2);
			ulSubMenu = document.createElement("ul");
			liWrapper.appendChild(ulSubMenu);
			rootElement.appendChild(liWrapper);
			
			Element ilSubMenuItem = document.createElement("li");
			
			Element createLink = document.createElement("a");				
			createLink.setAttribute("href", "/" + projectName + "/" + entityName.toLowerCase() +  "/form");				
			createLink.setTextContent("Create New " + entityName);		
			ilSubMenuItem.appendChild(createLink);
			ulSubMenu.appendChild(ilSubMenuItem);
			
			Element ilSubMenuItem2 = document.createElement("li");
			Element listLink = document.createElement("a");				
			listLink.setAttribute("href", "/" + projectName + "/" + entityName.toLowerCase());				
			listLink.setTextContent("List all " + pluralName);		
			ilSubMenuItem2.appendChild(listLink);		
			ulSubMenu.appendChild(ilSubMenuItem2);	
		} 
		
		return document;
	}
}
