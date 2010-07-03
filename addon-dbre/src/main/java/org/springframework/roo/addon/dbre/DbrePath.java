package org.springframework.roo.addon.dbre;

/**
 * Path artifacts for dbre classes.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public enum DbrePath {
	DBRE_XML_FILE("/META-INF/spring/dbre.xml");
	
	private String path;

	private DbrePath(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}
}
