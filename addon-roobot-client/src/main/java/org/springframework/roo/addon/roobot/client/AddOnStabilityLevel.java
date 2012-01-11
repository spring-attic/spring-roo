package org.springframework.roo.addon.roobot.client;

/**
 * Indication of stability level for add-ons / components.
 * 
 * @author Stefan Schmidt
 * @since 1.1.2
 */
public enum AddOnStabilityLevel {
    ANY(3), MILESTONE(2), RELEASE(0), RELEASE_CANDIDATE(1);

    public static AddOnStabilityLevel fromLevel(final int level) {
        if (level == ANY.getLevel()) {
            return ANY;
        }
        else if (level == RELEASE_CANDIDATE.getLevel()) {
            return RELEASE_CANDIDATE;
        }
        else if (level == MILESTONE.getLevel()) {
            return MILESTONE;
        }
        else {
            return RELEASE; // Default for all unknown inputs
        }
    }

    public static int getAddOnStabilityLevel(final String version) {
        if (version.endsWith(".RELEASE")) {
            return RELEASE.getLevel();
        }
        else if (version.matches("\\.RC\\d")) {
            return RELEASE_CANDIDATE.getLevel();
        }
        else if (version.matches("\\.M\\d")) {
            return MILESTONE.getLevel();
        }
        else {
            return ANY.getLevel();
        }
    }

    private int level;

    private AddOnStabilityLevel(final int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
