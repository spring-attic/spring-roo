package org.springframework.roo.addon.web.mvc.jsp.tiles;

import org.springframework.roo.addon.web.mvc.controller.scaffold.mvc.WebScaffoldMetadata;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public interface TilesOperations {
	
	String DEFAULT_TEMPLATE = "default";
	
	String PUBLIC_TEMPLATE = "public";

	/**
	 * Adds a new view definition to the views.xml tiles configuration
	 * 
	 * @param folderName The name of the folder under /WEB-INF/views (specified via the path attribute in {@link WebScaffoldMetadata}
	 * @param tilesViewName The simple name of the view (ie 'list', 'show', 'update', etc) or, if views are nested in sub-folders the name should be 'owner/list', 'owner/show', etc
	 * @param tilesTemplateName The template name (ie 'admin', 'public')
	 * @param viewLocation The location of the view in the Web application (ie "/WEB-INF/views/owner/list.jspx")
	 */
	void addViewDefinition(String folderName, String tilesViewName, String tilesTemplateName, String viewLocation);

	/**
	 * Adds a view definition from the views.xml tiles configuration
	 *
	 * @param name The simple name of the view (ie 'list', 'show', 'update', etc)
	 * @param folderName Th
	 */
	void removeViewDefinition(String name, String folderName);
}