package org.springframework.roo.addon.logging;

import java.util.Arrays;

import org.springframework.roo.support.style.ToStringCreator;

/**
 * Provides information related to the configuration of the logger.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
public enum LoggerPackage {
	ROOT,
	PROJECT,
	TRANSACTIONS("org.springframework.transactions"),
	SECURITY("org.springframework.security"),
	AOP("org.springframework.aop", "org.springframework.aspects"),
	PERSISTENCE("org.springframework.orm"),
	ALL_SPRING("org.springframework"),
	WEB("org.springframework.web");

	private String[] packageNames;
	
	private LoggerPackage(String... packageNames) {
		this.packageNames = packageNames;
	}
	
	public String[] getPackageNames() {
		return packageNames;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("layer", name());
		tsc.append("package names", Arrays.asList(packageNames));
		return tsc.toString();
	}
}
