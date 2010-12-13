package org.springframework.roo.url.stream;


/**
 * Represents utility members for implementation of {@link UrlInputStreamService}s.
 * 
 * @author Ben Alex
 * @since 1.1.1
 *
 */
public abstract class UrlInputStreamUtils {
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	public static final String SETUP_UAA_REQUIRED = LINE_SEPARATOR +
		"At this time you have not authorized Spring Roo to download resources from" + LINE_SEPARATOR +
		"VMware domains. Some Spring Roo features are therefore unavailable. Please" + LINE_SEPARATOR +
		"type 'download status' and press ENTER for further information." + LINE_SEPARATOR;
	
}
