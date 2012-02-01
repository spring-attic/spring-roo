package org.springframework.roo.addon.roobot.client.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.Validate;

public class Bundle {

    public static List<Bundle> orderByRanking(final List<Bundle> bundles) {
        Collections.sort(bundles, new Comparator<Bundle>() {
            public int compare(final Bundle o1, final Bundle o2) {
                if (o1.getRanking() == o2.getRanking()) {
                    return 0;
                }
                else if (o1.getRanking() < o2.getRanking()) {
                    return 1;
                }
                else {
                    return -1;
                }
            }
        });
        return Collections.unmodifiableList(bundles);
    }

    public static List<Bundle> orderBySearchRelevance(final List<Bundle> bundles) {
        Collections.sort(bundles, new Comparator<Bundle>() {
            public int compare(final Bundle o1, final Bundle o2) {
                if (o1.getSearchRelevance() < o2.getSearchRelevance()) {
                    return -1;
                }
                else if (o1.getSearchRelevance() > o2.getSearchRelevance()) {
                    return 1;
                }
                else {
                    if (o1.getRanking() == o2.getRanking()) {
                        return 0;
                    }
                    else if (o1.getRanking() < o2.getRanking()) {
                        return 1;
                    }
                    else {
                        return -1;
                    }
                }
            }
        });
        return Collections.unmodifiableList(bundles);
    }

    private List<Comment> comments;
    private float ranking;
    private float searchRelevance;

    private String symbolicName;

    private List<BundleVersion> versions;

    public Bundle(final String symbolicName, final float ranking,
            final List<Comment> inComments) {
        super();
        this.symbolicName = symbolicName;
        this.ranking = ranking;
        Collections.sort(inComments, new Comparator<Comment>() {
            public int compare(final Comment o1, final Comment o2) {
                return o1.getDate().compareTo(o2.getDate());
            }
        });
        comments = inComments;
        versions = new ArrayList<BundleVersion>();
    }

    public void addComment(final Comment comment) {
        comments.add(comment);
    }

    public void addVersion(final BundleVersion bundleVersion) {
        versions.add(bundleVersion);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Bundle other = (Bundle) obj;
        if (symbolicName == null) {
            if (other.symbolicName != null) {
                return false;
            }
        }
        else if (!symbolicName.equals(other.symbolicName)) {
            return false;
        }
        return true;
    }

    public BundleVersion getBundleVersion(final String bundleKey) {
        Validate.notBlank(bundleKey, "Bundle key required");
        if (bundleKey.contains(";")) {
            final String[] split = bundleKey.split(";");
            Validate.isTrue(split.length == 2,
                    "Incorrect bundle identifier presented");
            final String remains = split[1];
            for (final BundleVersion version : versions) {
                if (version.getVersion().equalsIgnoreCase(remains)) {
                    return version;
                }
            }
            throw new IllegalStateException("Unable to find bundle with key "
                    + bundleKey);
        }
        return getLatestVersion();
    }

    public List<Comment> getComments() {
        return comments;
    }

    public BundleVersion getLatestVersion() {
        final List<BundleVersion> versions = BundleVersion
                .orderByVersion(getVersions());
        if (versions.size() > 0) {
            return versions.get(versions.size() - 1);
        }
        return null;
    }

    public float getRanking() {
        return ranking;
    }

    public float getSearchRelevance() {
        return searchRelevance;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public List<BundleVersion> getVersions() {
        return versions;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + (symbolicName == null ? 0 : symbolicName.hashCode());
        return result;
    }

    public void setSearchRelevance(final float searchRelevance) {
        this.searchRelevance = searchRelevance;
    }
}
