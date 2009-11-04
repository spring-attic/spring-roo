package org.springframework.roo.addon.mvc.jsp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;

import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * Provides operations to manage tiles view definitions.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
public class TilesOperations {
	
	private Element root;
	private FileManager fileManager;
	private PathResolver pathResolver;
	private String simpleBeanName;
	public static final String DEFAULT_TEMPLATE = "default";
	public static final String PUBLIC_TEMPLATE = "public";
	
	/**
	 * Creates configuration object to setup artifacts for the Tiles layout engine.
	 * 
	 * @param folderName The name of the folder where the view artifacts for a specific add-on or domain object are stored (located under WEB-INF/views)
	 * @param fileManager The file manager to be used for virtual filesystem operations
	 * @param pathResolver The path resolver
	 * @param webMvcConfigName The configuration where the TilesConfigurer resides
	 */
	public TilesOperations(String folderName, FileManager fileManager, PathResolver pathResolver, String webMvcConfigName) {
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(pathResolver, "Path resolver required");
		Assert.hasText(folderName, "Simple bean name required");
		Assert.hasText(webMvcConfigName, "Web MVC config filename rquired");
		
		this.fileManager = fileManager;
		this.pathResolver = pathResolver;
		
		folderName = (folderName.length() > 0 && !folderName.startsWith("/")) ? "/" + folderName : folderName;
		
		Document tilesView;
		
		String viewFile = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/views" + folderName + "/views.xml");
		if (!fileManager.exists(viewFile)) {			
			tilesView = XmlUtils.getDocumentBuilder().newDocument();
			tilesView.appendChild(tilesView.createElement("tiles-definitions"));			
		} else {
			try {
				DocumentBuilder builder = XmlUtils.getDocumentBuilder();
				builder.setEntityResolver(new TilesDtdResolver());
				tilesView = builder.parse(viewFile);
			} catch (Exception e) {
				throw new IllegalStateException("Unable to parse the tiles " + viewFile + " file");
			}
		}
		
		root = tilesView.getDocumentElement(); 
		this.simpleBeanName = folderName;
	}

	/**
	 * Adds a new view definition to the views.xml tiles configuration
	 * 
	 * @param tilesViewName The simple name of the view (ie 'list', 'show', 'update', etc) or, if views are nested in sub-folders the name should be 'owner/list', 'owner/show', etc
	 * @param tilesTemplateName The template name (ie 'admin', 'public')
	 * @param templateLocation The location of the template in the Web application (ie "/WEB-INF/views/owner/list.jspx")
	 */
	public void addViewDefinition(String tilesViewName, String tilesTemplateName, String viewLocation) {
		Assert.hasText(tilesViewName, "View name required");
		Assert.hasText(tilesTemplateName, "Template name required");
		Assert.hasText(viewLocation, "View location required");
		
		tilesViewName = tilesViewName.startsWith("/") ? tilesViewName.replaceFirst("/", "") : tilesViewName;
		
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
	}
	
	/**
	 * Adds a view definition from the views.xml tiles configuration
	 * 
	 * @param name The simple name of the view (ie 'list', 'show', 'update', etc)
	 */
	public void removeViewDefinition(String name) {
		Assert.hasText(name, "View name required");
		
		//find menu item under this category if exists 
		Element element = XmlUtils.findFirstElement("/tiles-definitions/definition[@name = '" + name + "']", root);
		if(element==null) {
			return;
		}
		element.getParentNode().removeChild(element);
	}
	
	/**
	 * This method will attempt to save the tiles view definition but only if actual changes 
	 * to the file have been detected.
	 * 
	 * @return indicates if disk was changed (ie updated or created)
	 */
	public boolean writeToDiskIfNecessary() {
		// Build a string representation of the JSP
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		Transformer transformer = XmlUtils.createIndentingTransformer();
		transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://tiles.apache.org/dtds/tiles-config_2_0.dtd");
		transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//Apache Software Foundation//DTD Tiles Configuration 2.0//EN");
		XmlUtils.writeXml(transformer, byteArrayOutputStream, root.getOwnerDocument());
		String jspContent = byteArrayOutputStream.toString();
		String tilesDefinition = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/" + simpleBeanName + "/views.xml"); 
		
		// If mutableFile becomes non-null, it means we need to use it to write out the contents of jspContent to the file
		MutableFile mutableFile = null;
		if (fileManager.exists(tilesDefinition)) {
			// First verify if the file has even changed
			File f = new File(tilesDefinition);
			String existing = null;
			try {
				existing = FileCopyUtils.copyToString(new FileReader(f));
			} catch (IOException ignoreAndJustOverwriteIt) {}
			
			if (!jspContent.equals(existing)) {
				mutableFile = fileManager.updateFile(tilesDefinition);
			}			
		} else {
			mutableFile = fileManager.createFile(tilesDefinition);
			Assert.notNull(mutableFile, "Could not create tiles view definition '" + tilesDefinition + "'");
		}
		
		try {
			if (mutableFile != null) {
				// We need to write the file out (it's a new file, or the existing file has different contents)
				FileCopyUtils.copy(jspContent, new OutputStreamWriter(mutableFile.getOutputStream()));
				// Return and indicate we wrote out the file
				return true;
			}
		} catch (IOException ioe) {
			throw new IllegalStateException("Could not output '" + mutableFile.getCanonicalPath() + "'", ioe);
		}
		
		// A file existed, but it contained the same content, so we return false
		return false;
	}
	
	private class TilesDtdResolver implements EntityResolver {		
		public InputSource resolveEntity (String publicId, String systemId) {
			if (systemId.equals("http://tiles.apache.org/dtds/tiles-config_2_0.dtd")) {				
				return new InputSource(TemplateUtils.getTemplate(TilesOperations.class, "layout/tiles-config_2_0.dtd"));
			} else {
				// use the default behaviour
				return null;
			}
		}
	}
}
