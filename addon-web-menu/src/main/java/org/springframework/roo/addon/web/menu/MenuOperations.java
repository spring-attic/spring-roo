package org.springframework.roo.addon.web.menu;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.List;

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
	
//	public void setMenuFile(String menuFile) {
//		this.menuFile = menuFile;
//	}

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
	public void addMenuItem(String menuCategoryId, String menuCategoryLabel, String menuItemId, String menuItemLabel, String messageCode, String link) {
		Assert.hasText(menuCategoryId, "Menu category label identifier required");
		Assert.hasText(menuCategoryLabel, "Menu category label required");
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
			h2.setTextContent(menuCategoryLabel);
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
			Element url = document.createElement("c:url");
			url.setAttribute("var", menuItemId + "_url");
			url.setAttribute("value", link);
			menuItem.appendChild(url);
			Element createLink = document.createElement("a");				
			createLink.setAttribute("href", "${" + menuItemId + "_url}");				
			Element message = document.createElement("spring:message");
			message.setAttribute("code", messageCode);
			if (menuItemLabel.length() > 0) {
				message.setAttribute("arguments", menuItemLabel);
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
}
