package org.springframework.roo.addon.web.menu;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;

import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Generates the jsp menu and allows for management of menu items.
 *
 * @author Stefan Schmidt
 * @since 1.0
 */
@ScopeDevelopment
public class MenuOperations {
	
	private FileManager fileManager;
	
	private PathResolver pathResolver;
	
	private String menuFile;
	
	public MenuOperations(FileManager fileManager, PathResolver pathResolver){
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(pathResolver, "Path resolver required");

		this.fileManager = fileManager;
		this.pathResolver = pathResolver;
		
		menuFile = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/menu.jspx");
	}

	/**
	 * Allows for the addition of menu categories and menu items. If a category or menu item with the
	 * given identifier exists then it will <b>not</b> be overwritten or replaced.
	 * <p>
	 * Addons can determine their own category and menu item identifiers so that there are no clashes 
	 * with other addons. 
	 * <p>
	 * The recommended category identifier naming convention is <i>addon-name_intention_category</i> where 
	 * intention represents a further identifier to diffentiate between different categories provided
	 * by the same addon. Similarly, the recommended menu item identifier naming convention is
	 * <i>addon-name_intention_menu_item</i>.
	 *  
	 * 
	 * @param menuCategoryId the identifier for the menu category (required)
	 * @param menuCategoryLabel the category label (required)
	 * @param menuItemId the menu item identifier (required)
	 * @param menuItemLabel the menu item label (required)
	 * @param link the menu item link (required)
	 */
	public void addMenuItem(String menuCategoryId, JavaSymbolName menuCategoryLabel, String menuItemId, JavaSymbolName menuItemLabel, String messageCode, String link) {
		Assert.hasText(menuCategoryId, "Menu category label identifier required");
		Assert.notNull(menuCategoryLabel, "Menu category label required");
		Assert.hasText(menuItemId, "Menu item label identifier required");
		Assert.notNull(menuItemLabel, "Menu item object required");
		Assert.hasText(link, "Link required");
		
		menuItemId = menuItemId.replaceAll("/", "_");
		
		Document document;
		try {			
			document = XmlUtils.getDocumentBuilder().parse(getMenuFile());
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to parse menu.jspx" + (e.getMessage() == null || "".equals(e.getMessage()) ? "" : " (" + e.getMessage() + ")"));
		}
		
		//make the root element of the menu the one with the menu identifier allowing for different decorations of menu
		Element rootElement = XmlUtils.findRequiredElement("//ul[@id='roo_menu']", (Element) document.getFirstChild());
		
		//check for existence of menu category by looking for the indentifier provided
		Element categoryWrapper = XmlUtils.findFirstElement("//li[@id='" + menuCategoryId + "']", rootElement);
		Element categoryRoot = null;
			
		//if not exists, create new one
		if(categoryWrapper == null) {
			categoryWrapper = document.createElement("li");
			categoryWrapper.setAttribute("id", menuCategoryId);
			Element h2 = document.createElement("h2");
			if (null == getProperty(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages.properties", "menu.category." + menuCategoryLabel.getSymbolName().toLowerCase() + ".label")) {
				setProperty(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages.properties", "menu.category." + menuCategoryLabel.getSymbolName().toLowerCase() + ".label", menuCategoryLabel.getReadableSymbolName());
				setProperty(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_de.properties", "menu.category." + menuCategoryLabel.getSymbolName().toLowerCase() + ".label", menuCategoryLabel.getReadableSymbolName());
				setProperty(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_es.properties", "menu.category." + menuCategoryLabel.getSymbolName().toLowerCase() + ".label", menuCategoryLabel.getReadableSymbolName());
				setProperty(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_sv.properties", "menu.category." + menuCategoryLabel.getSymbolName().toLowerCase() + ".label", menuCategoryLabel.getReadableSymbolName());
				setProperty(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_it.properties", "menu.category." + menuCategoryLabel.getSymbolName().toLowerCase() + ".label", menuCategoryLabel.getReadableSymbolName());

			}
			Element categoryLabel = document.createElement("spring:message");
			categoryLabel.setAttribute("code", "menu.category." + menuCategoryLabel.getSymbolName().toLowerCase() + ".label");
			h2.appendChild(categoryLabel);
			categoryWrapper.appendChild(h2);
			categoryRoot = document.createElement("ul");
			categoryWrapper.appendChild(categoryRoot);
			rootElement.appendChild(categoryWrapper);
		} else {
			categoryRoot = XmlUtils.findRequiredElement("ul", categoryWrapper);
		}
		
		//check for existence of menu item by looking for the indentifier provided
		Element menuItem = XmlUtils.findFirstElement("//li[@id='" + menuItemId + "']", categoryWrapper);
		
		//if not exists, create one
		if(menuItem == null) {		
			menuItem = document.createElement("li");
			menuItem.setAttribute("id", menuItemId);
			Element url = document.createElement("spring:url");
			url.setAttribute("var", menuItemId + "_url");
			url.setAttribute("value", link);
			menuItem.appendChild(url);
			Element createLink = document.createElement("a");				
			createLink.setAttribute("href", "${" + menuItemId + "_url}");				
			Element message = document.createElement("spring:message");
			message.setAttribute("code", messageCode);
			if (menuItemLabel.getSymbolName().length() > 0) {
				if (null == getProperty(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages.properties", "label." + menuItemLabel.getSymbolName().toLowerCase())) {
					setProperty(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages.properties","label." + menuItemLabel.getSymbolName().toLowerCase(), menuItemLabel.getReadableSymbolName());
					setProperty(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_de.properties","label." + menuItemLabel.getSymbolName().toLowerCase(), menuItemLabel.getReadableSymbolName());
					setProperty(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_es.properties","label." + menuItemLabel.getSymbolName().toLowerCase(), menuItemLabel.getReadableSymbolName());
					setProperty(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_sv.properties","label." + menuItemLabel.getSymbolName().toLowerCase(), menuItemLabel.getReadableSymbolName());
					setProperty(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_it.properties","label." + menuItemLabel.getSymbolName().toLowerCase(), menuItemLabel.getReadableSymbolName());

				}
				Element entityLabel = document.createElement("spring:message");
				entityLabel.setAttribute("code", "label." + menuItemLabel.getSymbolName().toLowerCase());
				entityLabel.setAttribute("var", "label_" + menuItemLabel.getSymbolName().toLowerCase());
				createLink.appendChild(entityLabel);
				message.setAttribute("arguments", "${" + "label_" + menuItemLabel.getSymbolName().toLowerCase() + "}");
			}
			createLink.appendChild(message);
			menuItem.appendChild(createLink);
			categoryRoot.appendChild(menuItem);
		}
		
		writeToDiskIfNecessary(document.getChildNodes());
	}
	
	public void cleanUpMenuItems(String menuCategoryId, String menuItemIdPrefix, List<String> allowedMenuIds) {
		Assert.hasText(menuCategoryId, "Menu category identifier required");
		Assert.hasText(menuItemIdPrefix, "Menu item id prefix required (ie 'finder_')");
		Assert.notNull(allowedMenuIds, "List of allowed menu items required");
		
		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(getMenuFile());
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to parse menu.jspx", e);
		}
		
		//find any menu items under this category which have an id that starts with the menuItemIdPrefix
		List<Element> elements = XmlUtils.findElements("//li[@id='" + menuCategoryId + "']//li[starts-with(@id,'" + menuItemIdPrefix + "')]", document.getDocumentElement());
		if(elements.size()==0) {
			return;
		}
		
		for(Element element: elements) {
			if(!allowedMenuIds.contains(element.getAttribute("id"))) {
				element.getParentNode().removeChild(element);
			}
		}
		
		writeToDiskIfNecessary(document.getChildNodes());
	}
	
	public void cleanUpMenuItem(String menuCategoryId, String menuItemId) {
		Assert.hasText(menuCategoryId, "Menu category identifier required");
		Assert.hasText(menuItemId, "Menu item id required");
		
		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(getMenuFile());
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to parse menu.jsp", e);
		}
		
		//find menu item under this category if exists 
		Element element = XmlUtils.findFirstElement("//li[@id='" + menuCategoryId + "']//li[@id='" + menuItemId + "']", document.getDocumentElement());
		if(element==null) {
			return;
		}
		element.getParentNode().removeChild(element);
		
		writeToDiskIfNecessary(document.getChildNodes());
	}
	
	private InputStream getMenuFile() {			
		if (!fileManager.exists(menuFile)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "menu.jspx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/menu.jspx")).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}			
		}
		
		// We know the menu file already exists, as the logic earlier copied it from a template
		InputStream existingMenu;
		try {
			existingMenu = new FileInputStream(new File(menuFile));
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}		
		return existingMenu;
	}
	
	/** return indicates if disk was changed (ie updated or created) */
	private boolean writeToDiskIfNecessary(NodeList toWrite) {
		// Build a string representation of the JSP
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		XmlUtils.writeMalformedXml(XmlUtils.createIndentingTransformer(), byteArrayOutputStream, toWrite);
		String jspContent = byteArrayOutputStream.toString();
		
		// If mutableFile becomes non-null, it means we need to use it to write out the contents of jspContent to the file
		MutableFile mutableFile = null;
		if (fileManager.exists(menuFile)) {
			// First verify if the file has even changed
			File f = new File(menuFile);
			String existing = null;
			try {
				existing = FileCopyUtils.copyToString(new FileReader(f));
			} catch (IOException ignoreAndJustOverwriteIt) {}
			
			if (!jspContent.equals(existing)) {
				mutableFile = fileManager.updateFile(menuFile);
			}
			
		} else {
			mutableFile = fileManager.createFile(menuFile);
			Assert.notNull(mutableFile, "Could not create menu file '" + menuFile + "'");
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
	
	/**
	 * Changes the specified property, throwing an exception if the file does not exist.
	 * 
	 * @param key the property key to update (required)
	 * @param value the property value to set into the property key (required)
	 */
	private void setI18nMessageProperties(String key, String value) {
		Assert.hasText(key, "Key required");
		Assert.hasText(value, "Value required");
		
		//get hold of all messages*.properties files and add the supplied key and value
		String antPath = pathResolver.getRoot(Path.SRC_MAIN_WEBAPP) + "/WEB-INF/i18n/messages*.properties";

		SortedSet<FileDetails> entries = fileManager.findMatchingAntPath(antPath);
		MutableFile mutableFile = null;
		Properties props = new Properties();
		for (FileDetails file : entries) {
			try {
				if (fileManager.exists(file.getCanonicalPath())) {
					mutableFile = fileManager.updateFile(file.getCanonicalPath());
					props.load(mutableFile.getInputStream());
				} else {
					throw new IllegalStateException("Properties file not found");
				}
			} catch (IOException ioe) {
				throw new IllegalStateException(ioe);
			}
			props.setProperty(key, value);
			
			try {
				props.store(mutableFile.getOutputStream(), "Updated at " + new Date());
			} catch (IOException ioe) {
				throw new IllegalStateException(ioe);
			}
		}
	}
	
	 /**
     * Changes the specified property, throwing an exception if the file does not exist.
     * 
     * @param propertyFilePath the location of the property file (required)
     * @param propertyFilename the name of the property file within the specified path (required)
     * @param key the property key to update (required)
     * @param value the property value to set into the property key (required)
     */
    private void setProperty(Path propertyFilePath, String propertyFilename, String key, String value) {
            Assert.notNull(propertyFilePath, "Property file path required");
            Assert.hasText(propertyFilename, "Property filename required");
            Assert.hasText(key, "Key required");
            Assert.hasText(value, "Value required");
            
            String filePath = pathResolver.getIdentifier(propertyFilePath, propertyFilename);
            MutableFile mutableFile = null;
            Properties props = new Properties();
            
            try {
                    if (fileManager.exists(filePath)) {
                            mutableFile = fileManager.updateFile(filePath);
                            props.load(mutableFile.getInputStream());
                    } else {
                            throw new IllegalStateException("Properties file not found");
                    }
            } catch (IOException ioe) {
                    throw new IllegalStateException(ioe);
            }
            props.setProperty(key, value);
            
            try {
                    props.store(mutableFile.getOutputStream(), "Updated at " + new Date());
            } catch (IOException ioe) {
                    throw new IllegalStateException(ioe);
            }
    }

	/**
	 * Retrieves the specified property, returning null if the property or file does not exist.
	 * 
	 * @param propertyFilePath the location of the property file (required)
	 * @param propertyFilename the name of the property file within the specified path (required)
	 * @param key the property key to retrieve (required)
	 * @return the property value (may return null if the property file or requested property does not exist)
	 */
	private String getProperty(Path propertyFilePath, String propertyFilename, String key) {
		Assert.notNull(propertyFilePath, "Property file path required");
		Assert.hasText(propertyFilename, "Property filename required");
		Assert.hasText(key, "Key required");
		
		String filePath = pathResolver.getIdentifier(propertyFilePath, propertyFilename);
		MutableFile mutableFile = null;
		Properties props = new Properties();
		
		try {
			if (fileManager.exists(filePath)) {
				mutableFile = fileManager.updateFile(filePath);
				props.load(mutableFile.getInputStream());
			} else {
				return null;
			}
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
		
		return props.getProperty(key);
	}
}
