package org.springframework.roo.addon.web.mvc.jsp.tiles;

import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldMetadata;
import org.springframework.roo.project.LogicalPath;

/**
 * Methods for manipulating Apache Tiles view configuration files.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public interface TilesOperations {

    String DEFAULT_TEMPLATE = "default";

    String PUBLIC_TEMPLATE = "public";

    /**
     * Adds a new view definition to the <code>views.xml</code> Tiles
     * configuration in the given folder
     * 
     * @param folderName the name of the folder under
     *            <code>/WEB-INF/views</code> (specified via the path attribute
     *            in {@link WebScaffoldMetadata}; can be blank to update the
     *            main views file, or if not, any leading slash is ignored
     * @param tilesViewName The simple name of the view (i.e. 'list', 'show',
     *            'update', etc) or, if views are nested in sub-folders the name
     *            should be 'owner/list', 'owner/show', etc.; any leading slash
     *            is ignored
     * @param tilesTemplateName The template name (i.e. 'admin', 'public')
     * @param viewLocation The location of the view in the Web application (i.e.
     *            "/WEB-INF/views/owner/list.jspx")
     */
    void addViewDefinition(String folderName, LogicalPath path,
            String tilesViewName, String tilesTemplateName, String viewLocation);

    /**
     * Removes a view definition from the <code>views.xml</code> Tiles
     * configuration in the given folder
     * 
     * @param name the simple name of the view to remove (i.e. 'list', 'show',
     *            'update', etc)
     * @param folderName the name of the folder under
     *            <code>/WEB-INF/views</code>; can be blank to update the main
     *            views file, or if not, any leading slash is ignored
     */
    void removeViewDefinition(String name, String folderName, LogicalPath path);
}