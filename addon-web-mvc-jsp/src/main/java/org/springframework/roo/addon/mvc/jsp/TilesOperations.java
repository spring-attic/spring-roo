package org.springframework.roo.addon.mvc.jsp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;

import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
	
	public TilesOperations(String simpleBeanName, FileManager fileManager, PathResolver pathResolver, String servletName) {
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(pathResolver, "Path resolver required");
		Assert.hasText(simpleBeanName, "Simple bean name required");
		
		this.fileManager = fileManager;
		this.pathResolver = pathResolver;
		
		Document tilesView;
		
		String viewFile = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/jsp/" + simpleBeanName + "/views.xml");
		if (!fileManager.exists(viewFile)) {			
			tilesView = XmlUtils.getDocumentBuilder().newDocument();
			tilesView.appendChild(tilesView.createElement("tiles-definitions"));			
			
			//register new tiles view configuration in TilesConfigurer within project servlet configuration
			String servletPath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/" + servletName);
			if (!fileManager.exists(servletPath)) {
				throw new IllegalStateException("Unable to find project servlet configuration under " + servletPath);
			}			
			MutableFile mutableServletDefinition = null;
			Document servletDefinition;
			try {
				mutableServletDefinition = fileManager.updateFile(servletPath);
				servletDefinition = XmlUtils.getDocumentBuilder().parse(mutableServletDefinition.getInputStream());
			} catch (Exception e) {
				throw new IllegalStateException("Unable to parse project servlet configuration");
			}
			Element tilesDefinitionList = XmlUtils.findRequiredElement("/beans/bean[@id='tilesConfigurer']/property/list", servletDefinition.getDocumentElement());
			if (null == XmlUtils.findFirstElement("//value[text()='/WEB-INF/jsp/" + simpleBeanName + "/views.xml']", tilesDefinitionList)){
				Element value = servletDefinition.createElement("value");
				value.setTextContent("/WEB-INF/jsp/" + simpleBeanName + "/views.xml");
				tilesDefinitionList.appendChild(value);
			}
			XmlUtils.writeXml(XmlUtils.createIndentingTransformer(), mutableServletDefinition.getOutputStream(), servletDefinition);
			
		} else {
			try {
				tilesView = XmlUtils.getDocumentBuilder().parse(viewFile);
			} catch (Exception e) {
				throw new IllegalStateException("Unable to parse the tiles " + viewFile + " file");
			}
		}
		
		root = tilesView.getDocumentElement(); 
		this.simpleBeanName = simpleBeanName;
	}

	/**
	 * Adds a new view definition to the views.xml tiles configuration
	 * 
	 * @param name The simple name of the view (ie 'list', 'show', 'update', etc)
	 * @param template The template name (ie 'admin', 'public')
	 */
	public void addViewDefinition(String name, String template) {
		Assert.hasText(name, "View name required");
		Assert.hasText(template, "Template name required");
		
		name = name.startsWith("/") ? name.replaceFirst("/", "") : name;
		
		Element definition = XmlUtils.findFirstElement("/tiles-definitions/definition[@name = '" + simpleBeanName + "/" + name + "']", root);
		
		if(definition != null) {
			//a definition with this name does already exist - nothing to do
			return;
		}
		
		definition = root.getOwnerDocument().createElement("definition");
		definition.setAttribute("name", simpleBeanName + "/" + name);
		definition.setAttribute("extends", template);
		
		Element putAttribute = root.getOwnerDocument().createElement("put-attribute");
		putAttribute.setAttribute("name", "body");
		putAttribute.setAttribute("value", "/WEB-INF/jsp/" + simpleBeanName + "/" + name + ".jspx");
		
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
		Element element = XmlUtils.findFirstElement("/tiles-definitions/definition[@name = '" + simpleBeanName + "/" + name + "']", root);
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
		String tilesDefinition = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/jsp/" + simpleBeanName + "/views.xml"); 
		
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
}
