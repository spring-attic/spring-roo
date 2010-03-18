package org.springframework.roo.addon.logging;

import java.util.Arrays;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Provides information related to the configuration of the logger.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
public class LoggerPackage implements Comparable<LoggerPackage> {
	
	private String layer;
	private String [] packageNames;
	
	//configuration of root logger
	public static final LoggerPackage ROOT = new LoggerPackage("ROOT","rootLogger");
	
	public static final LoggerPackage PROJECT = new LoggerPackage("PROJECT", "TO_BE_CHANGED_BY_LISTENER");
	
	//logging for area of interest
	public static final LoggerPackage TRANSACTIONS = new LoggerPackage("TRANSACTIONS", "org.springframework.transactions");
	public static final LoggerPackage SECURITY = new LoggerPackage("SECURITY", "org.springframework.security");
	public static final LoggerPackage AOP = new LoggerPackage("AOP", "org.springframework.aop", "org.springframework.aspects");
	public static final LoggerPackage PERSISTENCE = new LoggerPackage("PERSISTENCE", "org.springframework.orm");
	public static final LoggerPackage ALL_SPRING = new LoggerPackage("ALL_SPRING", "org.springframework");
	public static final LoggerPackage WEB = new LoggerPackage("WEB", "org.springframework.web");
	
	public LoggerPackage(String layer, String ... packageNames) {
		Assert.hasText(layer, "Layer required");
		Assert.notNull(packageNames, "Package names are required");
		this.layer = layer;
		this.packageNames = packageNames;
	}

	public String getLayer() {
		return layer;
	}
	
	public String[] getPackageNames() {
		return packageNames;
	}

	public String getKey() {
		return layer;
	}
	
	public final int hashCode() {
		return this.layer.hashCode() % this.layer.hashCode();
	}

	public final boolean equals(Object obj) {
		return obj != null && obj instanceof LoggerPackage && this.compareTo((LoggerPackage)obj) == 0;
	}

	public final int compareTo(LoggerPackage o) {
		if (o == null) return -1;
		return this.layer.compareTo(o.layer);
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("layer", layer);
		tsc.append("package names", Arrays.asList(packageNames));
		return tsc.toString();
	}
}
