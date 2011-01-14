package org.springframework.roo.addon.roobot.client;

/**
 * Indication of stability level for add-ons / components.
 * 
 * @author Stefan Schmidt
 * @since 1.1.2
 */
public enum AddOnStabilityLevel {
	
	RELEASE				(0), 
	RELEASE_CANDIDATE	(1), 
	MILESTONE			(2), 
	ANY					(3);
	
	private int level;
	
	private AddOnStabilityLevel(int level) {
		this.level = level;
	}
	
	public int getLevel() {
		return level;
	}
	
	public static AddOnStabilityLevel fromLevel(int level) {
		if (level == ANY.getLevel()) {
			return ANY;
		} else if (level == RELEASE_CANDIDATE.getLevel()) {
			return RELEASE_CANDIDATE;
		} else if (level == MILESTONE.getLevel()) {
			return MILESTONE;
		} else {
			return RELEASE; //default for all unknown inputs
		}
	}

	public static int getAddOnStabilityLevel (String version) {
		if (version.endsWith(".RELEASE")) {
			return RELEASE.getLevel();
		} else if (version.matches("\\.RC\\d")) {
			return RELEASE_CANDIDATE.getLevel();
		} else if (version.matches("\\.M\\d")) {
			return MILESTONE.getLevel();
		} else {
			return ANY.getLevel();
		}
	}
}
