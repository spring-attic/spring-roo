package org.springframework.roo.addon.dbre;

/**
 * Various path artifacts for dbre classes.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public enum DbrePath {
	DBRE_XML_FILE("/META-INF/spring/dbre.xml"),
	DBRE_XML_TEMPLATE("dbre-template.xml"),
	DBRE_TABLE_XPATH("/dbMetadata/table");

	private String path;

	private DbrePath(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}
}
