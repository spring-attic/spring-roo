package org.springframework.roo.addon.web.mvc.jsp.roundtrip;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.XmlRoundTripUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;

/**
 * Default implementation of {@link XmlRoundTripFileManager}.
 * 
 * @author James Tyrrell
 * @since 1.2.0
 */
@Component
@Service
public class DefaultXmlRoundTripFileManager implements XmlRoundTripFileManager {

    @Reference private FileManager fileManager;
    private final Map<String, String> fileContentsMap = new HashMap<String, String>();

    public void writeToDiskIfNecessary(final String filename,
            final Document proposed) {
        Validate.notNull(filename, "The file name is required");
        Validate.notNull(proposed, "The proposed document is required");
        if (fileManager.exists(filename)) {
            final String proposedContents = XmlUtils.nodeToString(proposed);
            try {
                final String contents = FileUtils.readFileToString(new File(
                        filename)) + proposedContents;
                final String contentsSha = DigestUtils.shaHex(contents);
                final String lastContents = fileContentsMap.get(filename);
                if (lastContents != null && contentsSha.equals(lastContents)) {
                    return;
                }
                fileContentsMap.put(filename, contentsSha);
            }
            catch (final IOException ignored) {
            }
            try {
                final Document original = XmlUtils.readXml(fileManager
                        .getInputStream(filename));
                if (XmlRoundTripUtils.compareDocuments(original, proposed)) {
                    DomUtils.removeTextNodes(original);
                    final String updateContents = XmlUtils
                            .nodeToString(original);
                    fileManager.createOrUpdateTextFileIfRequired(filename,
                            updateContents, false);
                }
            }
            catch (final Exception e) {
                throw new IllegalStateException("Failed to write " + filename
                        + " : " + e.getMessage());
            }
        }
        else {
            final String contents = XmlUtils.nodeToString(proposed);
            final String contentsSha = DigestUtils.shaHex(contents + contents);
            fileContentsMap.put(filename, contentsSha);
            fileManager.createOrUpdateTextFileIfRequired(filename, contents,
                    false);
        }
    }
}
