package org.springframework.roo.addon.roobot.client.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.roo.support.util.Assert;

public class Bundle {
    private String symbolicName;
    private float ranking;
    private float searchRelevance;
    private List<BundleVersion> versions;
    private List<Comment> comments;
    
	public Bundle(String symbolicName, float ranking, List<Comment> inComments) {
		super();
		this.symbolicName = symbolicName;
		this.ranking = ranking;
		Collections.sort(inComments, new Comparator<Comment>() {
			public int compare(Comment o1, Comment o2) {
				return o1.getDate().compareTo(o2.getDate());
			}
		});
		this.comments = inComments;
		versions = new ArrayList<BundleVersion>();
	}

	public float getSearchRelevance() {
		return searchRelevance;
	}

	public void setSearchRelevance(float searchRelevance) {
		this.searchRelevance = searchRelevance;
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	public float getRanking() {
		return ranking;
	}

	public List<BundleVersion> getVersions() {
		return versions;
	}

	public List<Comment> getComments() {
		return comments;
	}
    
    public void addComment(Comment comment) {
    	comments.add(comment);
    }
    
    public void addVersion(BundleVersion bundleVersion) {
    	versions.add(bundleVersion);    
    }
    
    public BundleVersion getLatestVersion() {
    	List<BundleVersion> versions = BundleVersion.orderByVersion(getVersions());
    	if (versions.size() > 0) {
    		return versions.get(versions.size()-1);
    	}
    	return null;
    }
    
	public BundleVersion getBundleVersion(String bundleKey) {
		Assert.hasText(bundleKey, "Bundle key required");
		if (bundleKey.contains(";")) {
			String[] split = bundleKey.split(";");
			Assert.isTrue(split.length == 2, "Incorrect bundle identifier presented");
			String remains = split[1];
			for (BundleVersion version: versions) {
				if (version.getVersion().equalsIgnoreCase(remains)) {
					return version;
				}
			}
			throw new IllegalStateException("Unable to find bundle with key " + bundleKey);
		}
		return getLatestVersion();
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((symbolicName == null) ? 0 : symbolicName.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Bundle other = (Bundle) obj;
		if (symbolicName == null) {
			if (other.symbolicName != null)
				return false;
		} else if (!symbolicName.equals(other.symbolicName))
			return false;
		return true;
	}
	
	public static List<Bundle> orderByRanking(List<Bundle> bundles) {
		Collections.sort(bundles, new Comparator<Bundle>() {
			public int compare(Bundle o1, Bundle o2) {
				if (o1.getRanking() == o2.getRanking()) return 0;
				else if (o1.getRanking() < o2.getRanking()) return 1;
				else return -1;
			}});
		return Collections.unmodifiableList(bundles);
	}
	
	public static List<Bundle> orderBySearchRelevance(List<Bundle> bundles) {
		Collections.sort(bundles, new Comparator<Bundle>() {
			public int compare(Bundle o1, Bundle o2) {
				if (o1.getSearchRelevance() < o2.getSearchRelevance()) return -1;
				else if (o1.getSearchRelevance() > o2.getSearchRelevance()) return 1;
				// Order by ranking if search relevance is equal
				else {
					if (o1.getRanking() == o2.getRanking()) return 0;
					else if (o1.getRanking() < o2.getRanking()) return 1;
					else return -1;	
				}
			}});
		return Collections.unmodifiableList(bundles);
	}
}
