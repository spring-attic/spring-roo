package org.springframework.roo.addon.web.mvc.jsp.roundtrip;

import org.w3c.dom.Document;

/**
 * Used to write XML documents that are round tripped, i.e. contain Z attributed
 * elements, to disk. The class shouldn't be used for general XML documents.
 * 
 * @author James Tyrrell
 * @since 1.2.0
 */
public interface XmlRoundTripFileManager {

    /**
     * Updates or creates an XML file at the passed in location based on the
     * passed in proposed contains. If the file specified doesn't exist the
     * proposed document is interpreted as a String and written to disk. Should
     * the document exist it is parsed, compared with the proposed and if
     * required updated accordingly. The file output is cached using the
     * proposed and the original as a key, this improves performance
     * significantly.
     * 
     * @param filename the path of the file to written or updated (required)
     * @param proposed the proposed contents of the file
     */
    void writeToDiskIfNecessary(String filename, Document proposed);
}
