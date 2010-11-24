package org.springframework.roo.url.stream;

import java.net.URL;

/**
 * Represents utility members for implementation of {@link UrlInputStreamService}s.
 * 
 * @author Ben Alex
 * @since 1.1.1
 *
 */
public abstract class UrlInputStreamUtils {
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	private static final String[] VMWARE_DOMAINS = {"vmware.com", "springsource.com", "springsource.org", "springide.org"};
	
	public static final String SETUP_UAA_REQUIRED = LINE_SEPARATOR +
		"At this time you have not authorized Spring Roo to download resources from" + LINE_SEPARATOR +
		"VMware domains. Some Spring Roo features are therefore unavailable. Please" + LINE_SEPARATOR +
		"type 'uaa status' and press ENTER for further information." + LINE_SEPARATOR;
	
	public static boolean isVMwareDomain(URL url) {
		for (String domain : VMWARE_DOMAINS) {
			if (url.getHost().endsWith(domain)) {
				return true;
			}
		}
		return false;
	}
	
}
