package org.springframework.roo.addon.roobot.client.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BundleVersion {

	private String uri;
	private String obrUrl;
	private String version;
	private String presentationName;
	private Long size;
	private String description;
	private Map<String, String> commands = new HashMap<String, String>();
	private String pgpKey;
	private String pgpDescriptions;
	private String rooVersion;
	
	public BundleVersion(String uri, String obrUrl, String version, String presentationName, Long size, String description, String pgpKey, String pgpDescriptions, String rooVersion, Map<String, String> commands) {
		super();
		this.uri = uri;
		this.obrUrl = obrUrl;
		this.version = version;
		this.presentationName = presentationName;
		this.size = size;
		this.description = description;
		this.pgpKey = pgpKey;
		this.pgpDescriptions = pgpDescriptions;
		this.commands = commands;
		this.rooVersion = rooVersion;
	}

	public String getUri() {
		return uri;
	}

	public String getObrUrl() {
		return obrUrl;
	}

	public String getVersion() {
		return version;
	}

	public String getPresentationName() {
		return presentationName;
	}

	public Long getSize() {
		return size;
	}
	
	public String getRooVersion() {
		return rooVersion;
	}

	public String getDescription() {
		return description;
	}

	public Map<String, String> getCommands() {
		return commands;
	}

	public String getPgpKey() {
		return pgpKey;
	}

	public String getPgpDescriptions() {
		return pgpDescriptions;
	}
	
	public String getSummary() {
		return presentationName + "; " + description + "; " + pgpDescriptions + "; " + commands.toString();
	}

	public static List<BundleVersion> orderByVersion(List<BundleVersion> versions) {
		Collections.sort(versions, new Comparator<BundleVersion> () {
			public int compare(BundleVersion o1, BundleVersion o2) {
				return o1.getVersion().compareToIgnoreCase(o1.getVersion());
			}
		});
		return Collections.unmodifiableList(versions);
	}
}
