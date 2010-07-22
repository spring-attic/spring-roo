package org.springframework.roo.addon.web.mvc.controller;

import javax.xml.parsers.DocumentBuilder;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * Offers a Document instance which uses an internal DTD to allow offline XML validation.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
@Component
@Service
public class UrlRewriteOperationsImpl implements UrlRewriteOperations {

	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	
	public Document getUrlRewriteDocument() {
		Document urlrewriteXmlDoc;
		DocumentBuilder builder = XmlUtils.getDocumentBuilder();
		builder.setEntityResolver(new UrlRewriteDtdResolver());
		try {
			urlrewriteXmlDoc = builder.parse(fileManager.getInputStream(getUrlRewriteLocation()));
		} catch (Exception e) {
			throw new IllegalStateException("Could not parse Url Rewrite document", e);
		}
		Assert.notNull(urlrewriteXmlDoc, "Could not obtain Url Rewrite document");
		return urlrewriteXmlDoc;
	}
	
	public void writeUrlRewriteDocument(Document urlRewriteDoc) {	
		MutableFile mutableUrlrewriteXml = null;
		try {
			mutableUrlrewriteXml = fileManager.updateFile(getUrlRewriteLocation());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}		
		XmlUtils.writeXml(mutableUrlrewriteXml.getOutputStream(), urlRewriteDoc);
	}
	
	private String getUrlRewriteLocation() {
		String urlrewriteXml = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/urlrewrite.xml");
		Assert.isTrue(fileManager.exists(urlrewriteXml), "urlrewrite.xml not found; cannot continue");
		return urlrewriteXml;
	}

	private class UrlRewriteDtdResolver implements EntityResolver {		
		public InputSource resolveEntity (String publicId, String systemId) {
			if (systemId.equals("http://tuckey.org/res/dtds/urlrewrite3.1.dtd")) {				
				return new InputSource(TemplateUtils.getTemplate(UrlRewriteOperationsImpl.class, "urlrewrite3.1.dtd"));
			} else {
				// use the default behaviour
				return null;
			}
		}
	}
}
