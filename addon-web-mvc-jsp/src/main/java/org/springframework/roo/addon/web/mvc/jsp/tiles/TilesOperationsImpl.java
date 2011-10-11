package org.springframework.roo.addon.web.mvc.jsp.tiles;

import java.io.ByteArrayOutputStream;
import java.io.File;
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
import org.springframework.roo.support.util.StringUtils;
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

	// Fields
	@Reference private FileManager fileManager;
	@Reference private ProjectOperations projectOperations;

	public void addViewDefinition(String folderName, String tilesViewName, final String tilesTemplateName, final String viewLocation) {
		Assert.hasText(tilesViewName, "View name required");
		Assert.hasText(tilesTemplateName, "Template name required");
		Assert.hasText(viewLocation, "View location required");

		folderName = (folderName.length() > 0 && !folderName.startsWith("/")) ? "/" + folderName : folderName;
		tilesViewName = tilesViewName.startsWith("/") ? tilesViewName.replaceFirst("/", "") : tilesViewName;
		Element root = getRootElement(folderName);
		Element definition = XmlUtils.findFirstElement("/tiles-definitions/definition[@name = '" + tilesViewName + "']", root);
		if (definition != null) {
			// A definition with this name does already exist - nothing to do
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

	public void removeViewDefinition(final String name, final String folderName) {
		Assert.hasText(name, "View name required");

		Element root = getRootElement(folderName);

		// Find menu item under this category if exists
		Element element = XmlUtils.findFirstElement("/tiles-definitions/definition[@name = '" + name + "']", root);
		if (element != null) {
			element.getParentNode().removeChild(element);
			writeToDiskIfNecessary(folderName, root);
		}
	}

	/**
	 * 
	 * @param folderName should not start with a slash
	 * @param body the element whose parent document is to be written
	 * @return
	 */
	private boolean writeToDiskIfNecessary(final String folderName, final Element body) {
		// Build a string representation of the Tiles config file
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		Transformer transformer = XmlUtils.createIndentingTransformer();
		transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://tiles.apache.org/dtds/tiles-config_2_1.dtd");
		transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//Apache Software Foundation//DTD Tiles Configuration 2.1//EN");
		XmlUtils.writeXml(transformer, byteArrayOutputStream, body.getOwnerDocument());
		String viewContent = byteArrayOutputStream.toString();
		String tilesDefinition = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/views/" + folderName + "/views.xml");

		// If mutableFile becomes non-null, it means we need to use it to write out the contents of jspContent to the file
		MutableFile mutableFile = null;
		if (fileManager.exists(tilesDefinition)) {
			// First verify if the file has even changed
			File f = new File(tilesDefinition);
			String existing = null;
			try {
				existing = FileCopyUtils.copyToString(f);
			} catch (IOException ignored) {}

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

	/**
	 * Returns the root element of the <code>views.xml</code> file in the given
	 * folder
	 * 
	 * @param folderName the sub-directory of <code>WEB-INF/views</code> from
	 * which to load the file (leading slash is optional)
	 * @return the root of a new XML document if that file does not exist
	 */
	private Element getRootElement(final String folderName) {
		final Document tilesView;
		final String prefixedFolder = StringUtils.prefix(folderName, "/");
		final String viewsFile = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/views" + prefixedFolder + "/views.xml");
		if (fileManager.exists(viewsFile)) {
			final DocumentBuilder builder = XmlUtils.getDocumentBuilder();
			builder.setEntityResolver(new TilesDtdResolver());
			try {
				tilesView = builder.parse(fileManager.getInputStream(viewsFile));
			} catch (SAXException se) {
				throw new IllegalStateException("Unable to parse the tiles " + viewsFile + " file", se);
			} catch (IOException ioe) {
				throw new IllegalStateException("Unable to read the tiles " + viewsFile + " file (reason: " + ioe.getMessage() + ")", ioe);
			}
		} else {
			tilesView = XmlUtils.getDocumentBuilder().newDocument();
			tilesView.appendChild(tilesView.createElement("tiles-definitions"));
		}
		return tilesView.getDocumentElement();
	}

	private static class TilesDtdResolver implements EntityResolver {
		public InputSource resolveEntity(final String publicId, final String systemId) {
			if (systemId.equals("http://tiles.apache.org/dtds/tiles-config_2_1.dtd")) {
				return new InputSource(TemplateUtils.getTemplate(TilesOperationsImpl.class, "tiles-config_2_1.dtd"));
			}
			// Use the default behaviour
			return null;
		}
	}
}
