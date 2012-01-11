package org.springframework.roo.addon.web.flow;

import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides a method for making updates to an XML file. It relies on
 * {@link FileManager} and {@link XmlUtils} to do update the XML and provides a
 * convenient callback mechanism that provides access to the {@link Document}
 * and the root {@link Element}.
 * 
 * @author Rossen Stoyanchev
 */
public class XmlTemplate {

    public interface DomElementCallback {

        /**
         * Use this method to provide logic for updating a {@link Document}.
         * 
         * @param document the document
         * @param rootElement the root element of the document.
         * @return true if any changes were made that require saving, false
         *         otherwise
         */
        boolean doWithElement(Document document, Element rootElement);
    }

    private final FileManager fileManager;

    public XmlTemplate(final FileManager fileManager) {
        this.fileManager = fileManager;
    }

    /**
     * Wraps common code for updating an XML file. Callers provide a callback
     * and focus on the actual changes without having to know the rest of the
     * details.
     * 
     * @param resolvedPathIdentifier the path to the XML file to update.
     * @param rootElementCallback A callback with the logic that needs to be
     *            applied.
     */
    public void update(final String resolvedPathIdentifier,
            final DomElementCallback rootElementCallback) {
        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(resolvedPathIdentifier));
        final Element root = document.getDocumentElement();
        if (rootElementCallback.doWithElement(document, root)) {
            fileManager.createOrUpdateTextFileIfRequired(
                    resolvedPathIdentifier, XmlUtils.nodeToString(document),
                    false);
        }
    }
}
