package org.springframework.roo.addon.web.mvc.jsp.tiles;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Provides operations to manage tiles view definitions.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component
@Service
public class TilesOperationsImpl implements TilesOperations {
	@Reference private FileManager fileManager;
	@Reference private ProjectOperations projectOperations;
	
	/* (non-Javadoc)
	 * @see org.springframework.roo.addon.mvc.jsp.TilesOperationsI#addViewDefinition(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public void addViewDefinition(String folderName, String tilesViewName, String tilesTemplateName, String viewLocation) {
		Assert.hasText(tilesViewName, "View name required");
		Assert.hasText(tilesTemplateName, "Template name required");
		Assert.hasText(viewLocation, "View location required");
		
		folderName = (folderName.length() > 0 && !folderName.startsWith("/")) ? "/" + folderName : folderName;
		
		tilesViewName = tilesViewName.startsWith("/") ? tilesViewName.replaceFirst("/", "") : tilesViewName;
		
		Element root = getRootElement(folderName);
		
		Element definition = XmlUtils.findFirstElement("/tiles-definitions/definition[@name = '" + tilesViewName + "']", root);
		
		if(definition != null) {
			//a definition with this name does already exist - nothing to do
			return;
		}
		
		definition = root.getOwnerDocument().createElement("definition");
		definition.setAttribute("name", tilesViewName);
		definition.setAttribute("extends", tilesTemplateName);
		
		Element putAttribute = root.getOwnerDocument().createElement("put-attribute");
		putAttribute.setAttribute("name", "body");
		putAttribute.setAttribute("value", viewLocation);
	
		definition.appendChild(putAttribute);
		root.appendChild(definition);
		
		writeToDiskIfNecessary(folderName, root);
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.roo.addon.mvc.jsp.TilesOperationsI#removeViewDefinition(java.lang.String)
	 */
	public void removeViewDefinition(String name, String folderName) {
		Assert.hasText(name, "View name required");
		
		Element root = getRootElement(folderName);
		
		//find menu item under this category if exists 
		Element element = XmlUtils.findFirstElement("/tiles-definitions/definition[@name = '" + name + "']", root);
		if(element==null) {
			return;
		}
		element.getParentNode().removeChild(element);
		
		writeToDiskIfNecessary(folderName, root);
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.roo.addon.mvc.jsp.TilesOperationsI#writeToDiskIfNecessary()
	 */
	private boolean writeToDiskIfNecessary(String folderName, Element body) {
		// Build a string representation of the JSP
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		Transformer transformer = XmlUtils.createIndentingTransformer();
		transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://tiles.apache.org/dtds/tiles-config_2_1.dtd");
		transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//Apache Software Foundation//DTD Tiles Configuration 2.1//EN");
		XmlUtils.writeXml(transformer, byteArrayOutputStream, body.getOwnerDocument());
		String viewContent = byteArrayOutputStream.toString();
		String tilesDefinition = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/" + folderName + "/views.xml"); 
		
		// If mutableFile becomes non-null, it means we need to use it to write out the contents of jspContent to the file
		MutableFile mutableFile = null;
		if (fileManager.exists(tilesDefinition)) {
			// First verify if the file has even changed
			File f = new File(tilesDefinition);
			String existing = null;
			try {
				existing = FileCopyUtils.copyToString(new FileReader(f));
			} catch (IOException ignoreAndJustOverwriteIt) {}
			
			if (!viewContent.equals(existing)) {
				mutableFile = fileManager.updateFile(tilesDefinition);
			}			
		} else {
			mutableFile = fileManager.createFile(tilesDefinition);
			Assert.notNull(mutableFile, "Could not create tiles view definition '" + tilesDefinition + "'");
		}
		
		if (mutableFile != null) {
			try {
				// We need to write the file out (it's a new file, or the existing file has different contents)
				FileCopyUtils.copy(viewContent, new OutputStreamWriter(mutableFile.getOutputStream()));
				// Return and indicate we wrote out the file
				return true;
			} catch (IOException ioe) {
				throw new IllegalStateException("Could not output '" + mutableFile.getCanonicalPath() + "'", ioe);
			}
		}
		
		// A file existed, but it contained the same content, so we return false
		return false;
	}
	
	private Element getRootElement(String folderName) {
		Document tilesView;	
		String viewFile = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/views" + folderName + "/views.xml");
		if (!fileManager.exists(viewFile)) {			
			tilesView = XmlUtils.getDocumentBuilder().newDocument();
			tilesView.appendChild(tilesView.createElement("tiles-definitions"));			
		} else {
			DocumentBuilder builder = XmlUtils.getDocumentBuilder();
			builder.setEntityResolver(new TilesDtdResolver());
			try {
				tilesView = builder.parse(new File(viewFile));
			} catch (SAXException se) {
				throw new IllegalStateException("Unable to parse the tiles " + viewFile + " file", se);
			} catch (IOException ioe) {
				throw new IllegalStateException("Unable to read the tiles " + viewFile + " file (reason: " + ioe.getMessage() + ")", ioe);
			}
		}
		return tilesView.getDocumentElement();	
	}
	
	private class TilesDtdResolver implements EntityResolver {		
		public InputSource resolveEntity (String publicId, String systemId) {
			if (systemId.equals("http://tiles.apache.org/dtds/tiles-config_2_1.dtd")) {				
				return new InputSource(TemplateUtils.getTemplate(TilesOperationsImpl.class, "tiles-config_2_1.dtd"));
			} else {
				// Use the default behaviour
				return null;
			}
		}
	}
}
