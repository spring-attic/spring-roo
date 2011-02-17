package org.springframework.roo.addon.web.mvc.jsp.menu;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlRoundTripUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Generates the jsp menu and allows for management of menu items.
 *
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component
@Service
public class MenuOperationsImpl implements MenuOperations {
	@Reference private FileManager fileManager;
	@Reference private ProjectOperations projectOperations;
	@Reference private PropFileOperations propFileOperations;

	public void addMenuItem(JavaSymbolName menuCategoryName, JavaSymbolName menuItemId, String globalMessageCode, String link, String idPrefix) {
		addMenuItem(menuCategoryName, menuItemId, "", globalMessageCode, link, idPrefix, false);
	}
	
	public void addMenuItem(JavaSymbolName menuCategoryName, JavaSymbolName menuItemId, String menuItemLabel, String globalMessageCode, String link, String idPrefix) {
		addMenuItem(menuCategoryName, menuItemId, menuItemLabel, globalMessageCode, link, idPrefix, true);
	}
	
	private void addMenuItem(JavaSymbolName menuCategoryName, JavaSymbolName menuItemId, String menuItemLabel, String globalMessageCode, String link, String idPrefix, boolean writeProps) {
		Assert.notNull(menuCategoryName, "Menu category name required");
		Assert.notNull(menuItemId, "Menu item name required");
		Assert.hasText(link, "Link required");
		
		Map<String, String> properties = new HashMap<String, String>();
		
		if (idPrefix == null || idPrefix.length() == 0) {
			idPrefix = DEFAULT_MENU_ITEM_PREFIX;
		}
		
		Document document;
		try {		
			document = XmlUtils.getDocumentBuilder().parse(getMenuFile());
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to parse menu.jspx" + (!StringUtils.hasText(e.getMessage()) ? "" : " (" + e.getMessage() + ")"), e);
		}
		
		// Make the root element of the menu the one with the menu identifier allowing for different decorations of menu
		Element rootElement = XmlUtils.findFirstElement("//*[@id='_menu']", (Element) document.getFirstChild());
		if (rootElement == null) {
			Element rootMenu = new XmlElementBuilder("menu:menu", document).addAttribute("id", "_menu").build();
			rootMenu.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(rootMenu));
			rootElement = (Element) document.getDocumentElement().appendChild(rootMenu);
		}
		
		// Check for existence of menu category by looking for the indentifier provided
		Element category = XmlUtils.findFirstElement("//*[@id='c_" + menuCategoryName.getSymbolName().toLowerCase() + "']", rootElement);
			
		// If not exists, create new one
		if(category == null) {
			category = (Element) rootElement.appendChild(new XmlElementBuilder("menu:category", document)
															.addAttribute("id", "c_" + menuCategoryName.getSymbolName().toLowerCase())
														.build());
			category.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(category));
			properties.put("menu_category_" + menuCategoryName.getSymbolName().toLowerCase() + "_label", menuCategoryName.getReadableSymbolName());
		}
		
		// Check for existence of menu item by looking for the indentifier provided
		Element menuItem = XmlUtils.findFirstElement("//*[@id='" + idPrefix + menuCategoryName.getSymbolName().toLowerCase() + "_" + menuItemId.getSymbolName().toLowerCase() + "']", rootElement);
		
		if (menuItem == null) {
			menuItem = new XmlElementBuilder("menu:item", document)
							.addAttribute("id", idPrefix + menuCategoryName.getSymbolName().toLowerCase() + "_" + menuItemId.getSymbolName().toLowerCase())
							.addAttribute("messageCode", globalMessageCode)
							.addAttribute("url", link)
						.build();
			menuItem.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(menuItem));
			category.appendChild(menuItem);	
		}
		if (writeProps) {
			properties.put("menu_item_" + menuCategoryName.getSymbolName().toLowerCase() + "_" + menuItemId.getSymbolName().toLowerCase() + "_label", menuItemLabel);
			propFileOperations.addProperties(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/application.properties", properties, true, false);
		}
		writeToDiskIfNecessary(document);
	}
	
	public void cleanUpFinderMenuItems(JavaSymbolName menuCategoryName, List<String> allowedFinderMenuIds) {
		Assert.notNull(menuCategoryName, "Menu category identifier required");
		Assert.notNull(allowedFinderMenuIds, "List of allowed menu items required");
		
		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(getMenuFile());
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to parse menu.jspx", e);
		}
		
		// Find any menu items under this category which have an id that starts with the menuItemIdPrefix
		List<Element> elements = XmlUtils.findElements("//category[@id='c_" +  menuCategoryName.getSymbolName().toLowerCase() + "']//item[starts-with(@id, '" + FINDER_MENU_ITEM_PREFIX + "')]", document.getDocumentElement());
		if(elements.size() == 0) {
			return;
		}
		for (Element element: elements) {
			if (!allowedFinderMenuIds.contains(element.getAttribute("id")) && ("?".equals(element.getAttribute("z")) || XmlRoundTripUtils.calculateUniqueKeyFor(element).equals(element.getAttribute("z")))) {
				element.getParentNode().removeChild(element);
			}
		}
		writeToDiskIfNecessary(document);
	}
	
	/**
	 * Attempts to locate a menu item and remove it. 
	 * 
	 * @param menuCategoryName the identifier for the menu category (required)
	 * @param menuItemName the menu item identifier (required)
	 * @param idPrefix the prefix to be used for this menu item (optional, MenuOperations.DEFAULT_MENU_ITEM_PREFIX is default)
	 */
	public void cleanUpMenuItem(JavaSymbolName menuCategoryName, JavaSymbolName menuItemName, String idPrefix) {
		Assert.notNull(menuCategoryName, "Menu category identifier required");
		Assert.notNull(menuItemName, "Menu item id required");
		
		if (idPrefix == null || idPrefix.length() == 0) {
			idPrefix = DEFAULT_MENU_ITEM_PREFIX;
		}
		
		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(getMenuFile());
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to parse menu.jsp", e);
		}
		
		// Find menu item under this category if exists 
		Element element = XmlUtils.findFirstElement("//category[@id='c_" + menuCategoryName.getSymbolName().toLowerCase() + "']//item[@id='" + idPrefix + menuCategoryName.getSymbolName().toLowerCase() + "_" + menuItemName.getSymbolName().toLowerCase() + "']", document.getDocumentElement());
		if(element==null) {
			return;
		}
		if ("?".equals(element.getAttribute("z")) || XmlRoundTripUtils.calculateUniqueKeyFor(element).equals(element.getAttribute("z"))) {
			element.getParentNode().removeChild(element);
		}
		
		writeToDiskIfNecessary(document);
	}
	
	private File getMenuFile() {
		String menuFileName = getMenuFileName();
		if (!fileManager.exists(menuFileName)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "menu.jspx"), fileManager.createFile(menuFileName).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC Menu addon.", e);
			}
		}
		
		PathResolver pathResolver = projectOperations.getPathResolver();
		
		if (!fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/menu/menu.tagx"))) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "menu.tagx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/menu/menu.tagx")).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC Menu addon.", e);
			}
		}
		
		if (!fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/menu/item.tagx"))) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "item.tagx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/menu/item.tagx")).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC Menu addon.", e);
			}
		}
		
		if (!fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/menu/category.tagx"))) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "category.tagx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/menu/category.tagx")).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC Menu addon.", e);
			}
		}
			
		return new File(menuFileName);
	}
	
	private String getMenuFileName() {
		return projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/menu.jspx");
	}
	
	/** return indicates if disk was changed (ie updated or created) */
	private boolean writeToDiskIfNecessary(Document proposed) {
		Document original = null;
		
		// If mutableFile becomes non-null, it means we need to use it to write out the contents of jspContent to the file
		MutableFile mutableFile = null;
		String menuFileName = getMenuFileName();
		if (fileManager.exists(menuFileName)) {	
			try {
				original = XmlUtils.getDocumentBuilder().parse(getMenuFile());
			} catch (Exception e) {
				new IllegalStateException("Could not parse file: " + menuFileName);
			} 
			Assert.notNull(original, "Unable to parse " + menuFileName);
			if (XmlRoundTripUtils.compareDocuments(original, proposed)) {
				mutableFile = fileManager.updateFile(menuFileName);
			}
		} else {
			original = proposed;
			mutableFile = fileManager.createFile(menuFileName);
			Assert.notNull(mutableFile, "Could not create JSP file '" + menuFileName + "'");
		}
		
		if (mutableFile != null) {
			try {
				// Build a string representation of the JSP
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				XmlUtils.writeXml(XmlUtils.createIndentingTransformer(), byteArrayOutputStream, original);
				String jspContent = byteArrayOutputStream.toString();
				byteArrayOutputStream.close();
				// We need to write the file out (it's a new file, or the existing file has different contents)
				FileCopyUtils.copy(jspContent, new OutputStreamWriter(mutableFile.getOutputStream()));
				// Return and indicate we wrote out the file
				return true;
			} catch (IOException ioe) {
				throw new IllegalStateException("Could not output '" + mutableFile.getCanonicalPath() + "'", ioe);
			}
		}
		
		// A file existed, but it contained the same content, so we return false
		return false;
	}
}