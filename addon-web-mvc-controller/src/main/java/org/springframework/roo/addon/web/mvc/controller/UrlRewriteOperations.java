package org.springframework.roo.addon.web.mvc.controller;

import org.w3c.dom.Document;

/**
 * Offers a Document instance which uses an internal DTD to allow offline XML validation.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
public interface UrlRewriteOperations {

	/**
	 * Obtain a preconfigured document which uses a custom DTD resolver for offline validation.
	 * 
	 * @return the URl rewrite document
	 */
	public Document getUrlRewriteDocument();
	
	/**
	 * Persist Url Rewrite document.
	 * 
	 * @param urlRewriteDoc The document
	 */
	public void writeUrlRewriteDocument(Document urlRewriteDoc);
}
