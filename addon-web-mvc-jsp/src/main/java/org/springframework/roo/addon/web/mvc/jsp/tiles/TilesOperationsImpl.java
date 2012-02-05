package org.springframework.roo.addon.web.mvc.jsp.tiles;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.util.FileUtils;
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

    private static class TilesDtdResolver implements EntityResolver {
        public InputSource resolveEntity(final String publicId,
                final String systemId) {
            if (systemId
                    .equals("http://tiles.apache.org/dtds/tiles-config_2_1.dtd")) {
                return new InputSource(FileUtils.getInputStream(
                        TilesOperationsImpl.class, "tiles-config_2_1.dtd"));
            }
            // Use the default behaviour
            return null;
        }
    }

    @Reference private FileManager fileManager;
    @Reference private PathResolver pathResolver;

    public void addViewDefinition(final String folderName,
            final LogicalPath path, final String tilesViewName,
            final String tilesTemplateName, final String viewLocation) {
        Validate.notBlank(tilesViewName, "View name required");
        Validate.notBlank(tilesTemplateName, "Template name required");
        Validate.notBlank(viewLocation, "View location required");

        final String viewsDefinitionFile = getTilesConfigFile(folderName, path);

        final String unprefixedViewName = StringUtils.removeStart(
                tilesViewName, "/");
        final Element root = getViewsElement(viewsDefinitionFile);
        final Element existingDefinition = XmlUtils.findFirstElement(
                "/tiles-definitions/definition[@name = '" + unprefixedViewName
                        + "']", root);
        if (existingDefinition != null) {
            // A definition with this name does already exist - nothing to do
            return;
        }

        final Element newDefinition = root.getOwnerDocument().createElement(
                "definition");
        newDefinition.setAttribute("name", unprefixedViewName);
        newDefinition.setAttribute("extends", tilesTemplateName);

        final Element putAttribute = root.getOwnerDocument().createElement(
                "put-attribute");
        putAttribute.setAttribute("name", "body");
        putAttribute.setAttribute("value", viewLocation);

        newDefinition.appendChild(putAttribute);
        root.appendChild(newDefinition);

        writeToDiskIfNecessary(viewsDefinitionFile, root);
    }

    /**
     * Returns the canonical path of the "views.xml" Tiles configuration file in
     * the given folder.
     * 
     * @param folderName can be blank for the main views file; if not, any
     *            leading slash is ignored
     * @param path
     * @return a non-<code>null</code> path
     */
    private String getTilesConfigFile(final String folderName,
            final LogicalPath path) {
        final String subPath;
        if (StringUtils.isNotBlank(folderName) && !"/".equals(folderName)) {
            subPath = "/" + folderName;
        }
        else {
            subPath = "";
        }
        return pathResolver.getIdentifier(path, "WEB-INF/views" + subPath
                + "/views.xml");
    }

    /**
     * Returns the root element of the given Tiles configuration file
     * 
     * @param viewsDefinitionFile the canonical path of the file to load
     * @return the root of a new XML document if that file does not exist
     */
    private Element getViewsElement(final String viewsDefinitionFile) {
        final Document tilesView;
        if (fileManager.exists(viewsDefinitionFile)) {
            final DocumentBuilder builder = XmlUtils.getDocumentBuilder();
            builder.setEntityResolver(new TilesDtdResolver());
            try {
                tilesView = builder.parse(fileManager
                        .getInputStream(viewsDefinitionFile));
            }
            catch (final SAXException se) {
                throw new IllegalStateException("Unable to parse the tiles "
                        + viewsDefinitionFile + " file", se);
            }
            catch (final IOException ioe) {
                throw new IllegalStateException("Unable to read the tiles "
                        + viewsDefinitionFile + " file (reason: "
                        + ioe.getMessage() + ")", ioe);
            }
        }
        else {
            tilesView = XmlUtils.getDocumentBuilder().newDocument();
            tilesView.appendChild(tilesView.createElement("tiles-definitions"));
        }
        return tilesView.getDocumentElement();
    }

    public void removeViewDefinition(final String name,
            final String folderName, final LogicalPath path) {
        Validate.notBlank(name, "View name required");

        final String viewsDefinitionFile = getTilesConfigFile(folderName, path);

        final Element root = getViewsElement(viewsDefinitionFile);

        // Find menu item under this category if exists
        final Element element = XmlUtils.findFirstElement(
                "/tiles-definitions/definition[@name = '" + name + "']", root);
        if (element != null) {
            element.getParentNode().removeChild(element);
            writeToDiskIfNecessary(viewsDefinitionFile, root);
        }
    }

    /**
     * @param viewsDefinitionFile the canonical path of the file to update
     * @param body the element whose parent document is to be written
     * @return
     */
    private boolean writeToDiskIfNecessary(final String tilesDefinitionFile,
            final Element body) {
        // Build a string representation of the Tiles config file
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final Transformer transformer = XmlUtils.createIndentingTransformer();
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
                "http://tiles.apache.org/dtds/tiles-config_2_1.dtd");
        transformer
                .setOutputProperty(OutputKeys.DOCTYPE_PUBLIC,
                        "-//Apache Software Foundation//DTD Tiles Configuration 2.1//EN");
        XmlUtils.writeXml(transformer, byteArrayOutputStream,
                body.getOwnerDocument());
        final String viewContent = byteArrayOutputStream.toString();

        // If mutableFile becomes non-null, it means we need to use it to write
        // out the contents of jspContent to the file
        MutableFile mutableFile = null;
        if (fileManager.exists(tilesDefinitionFile)) {
            // First verify if the file has even changed
            final File file = new File(tilesDefinitionFile);
            String existing = null;
            try {
                existing = org.apache.commons.io.FileUtils
                        .readFileToString(file);
            }
            catch (final IOException ignored) {
            }

            if (!viewContent.equals(existing)) {
                mutableFile = fileManager.updateFile(tilesDefinitionFile);
            }
        }
        else {
            mutableFile = fileManager.createFile(tilesDefinitionFile);
            Validate.notNull(mutableFile,
                    "Could not create tiles view definition '"
                            + tilesDefinitionFile + "'");
        }

        if (mutableFile != null) {
            OutputStream outputStream = null;
            try {
                // We need to write the file out (it's a new file, or the
                // existing file has different contents)
                outputStream = mutableFile.getOutputStream();
                IOUtils.write(viewContent, outputStream);

                // Return and indicate we wrote out the file
                return true;
            }
            catch (final IOException ioe) {
                throw new IllegalStateException("Could not output '"
                        + mutableFile.getCanonicalPath() + "'", ioe);
            }
            finally {
                IOUtils.closeQuietly(outputStream);
            }
        }

        // A file existed, but it contained the same content, so we return false
        return false;
    }
}
