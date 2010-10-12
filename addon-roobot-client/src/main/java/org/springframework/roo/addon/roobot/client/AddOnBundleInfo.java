package org.springframework.roo.addon.roobot.client;

import java.util.Date;
import java.util.Map;

/**
 * Thread safe container for addon related information.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
public class AddOnBundleInfo {

	private String bsn;
	private float ranking;
	private String name;
	private String description;
	private Date lastUpdated;
	private String version;
	private String pgpKey;
	private String signedBy;
	private long size;
	private String url;
	private Map<String, String> commands;
	
	public AddOnBundleInfo(String bsn, float ranking, String name,
			String description, Date lastUpdated, String version,
			String pgpKey, String signedBy, long size, String url,
			Map<String, String> commands) {
		super();
		this.bsn = bsn;
		this.ranking = ranking;
		this.name = name;
		this.description = description;
		this.lastUpdated = lastUpdated;
		this.version = version;
		this.pgpKey = pgpKey;
		this.signedBy = signedBy;
		this.size = size;
		this.url = url;
		this.commands = commands;
	}

	public String getBsn() {
		return bsn;
	}

	public float getRanking() {
		return ranking;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Date getLastUpdated() {
		return lastUpdated;
	}

	public String getVersion() {
		return version;
	}

	public String getPgpKey() {
		return pgpKey;
	}

	public String getSignedBy() {
		return signedBy;
	}

	public long getSize() {
		return size;
	}

	public String getUrl() {
		return url;
	}

	public Map<String, String> getCommands() {
		return commands;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bsn == null) ? 0 : bsn.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AddOnBundleInfo other = (AddOnBundleInfo) obj;
		if (bsn == null) {
			if (other.bsn != null)
				return false;
		} else if (!bsn.equals(other.bsn))
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	public String toString() {
		return "bsn=" + bsn + ", ranking=" + ranking + ", name="
				+ name + ", description=" + description + ", lastUpdated="
				+ lastUpdated + ", version=" + version + ", signedBy="
				+ signedBy + ", size=" + size + ", commands=" + commands.toString();
	}
}
