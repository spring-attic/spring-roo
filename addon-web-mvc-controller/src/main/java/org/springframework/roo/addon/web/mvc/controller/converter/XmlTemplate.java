package org.springframework.roo.addon.web.mvc.controller.converter;

import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides a method for making updates to an XML file. It relies on {@link FileManager} and 
 * {@link XmlUtils} to do update the XML and provides a convenient callback mechanism that
 * provides access to the {@link Document} and the root {@link Element}. 
 * 
 * @author Rossen Stoyanchev
 */
public class XmlTemplate {

	private FileManager fileManager;

	public XmlTemplate(FileManager fileManager) {
		this.fileManager = fileManager;
	}

	/**
	 * Wraps common code for updating an XML file. Callers provide a callback and focus on 
	 * the actual changes without having to know the rest of the details.
	 * 
	 * @param resolvedPathIdentifier the path to the XML file to update.
	 * @param rootElementCallback A callback with the logic that needs to be applied.
	 * 
	 * @return true if it was updated
	 */
	public boolean update(String resolvedPathIdentifier, DomElementCallback rootElementCallback) {
		MutableFile mutableFile = fileManager.updateFile(resolvedPathIdentifier);
		Document document = null;
		try {
			document = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		Element root = document.getDocumentElement();
		if (rootElementCallback.doWithElement(document, root)) {
			XmlUtils.writeXml(mutableFile.getOutputStream(), document);
			return true;
		} else {
			return false;
		}
	}

	public interface DomElementCallback {

		/**
		 * Use this method to provide logic for updating a {@link Document}.
		 * 
		 * @param document the document
		 * @param rootElement the root element of the document.
		 * @return true if any changes were made that require saving, false otherwise
		 */
		boolean doWithElement(Document document, Element rootElement);
	
	}

}
