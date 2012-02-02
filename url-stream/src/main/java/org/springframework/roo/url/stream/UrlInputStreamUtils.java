package org.springframework.roo.url.stream;

import static org.apache.commons.io.IOUtils.LINE_SEPARATOR;

/**
 * Represents utility members for implementation of
 * {@link UrlInputStreamService}s.
 * 
 * @author Ben Alex
 * @since 1.1.1
 */
public final class UrlInputStreamUtils {

    public static final String SETUP_UAA_REQUIRED = LINE_SEPARATOR
            + "At this time you have not authorized Spring Roo to download resources from"
            + LINE_SEPARATOR
            + "VMware domains. Some Spring Roo features are therefore unavailable. Please"
            + LINE_SEPARATOR
            + "type 'download status' and press ENTER for further information."
            + LINE_SEPARATOR;

    private UrlInputStreamUtils() {
    }
}
