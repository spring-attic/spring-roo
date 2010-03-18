package org.springframework.roo.addon.logging;

import org.springframework.roo.support.util.Assert;

/**
 * Provides information related to the log level configuration of the logger.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
public class LogLevel implements Comparable<LogLevel> {
	
	private String logLevel;
	
	// in order
	public static final LogLevel FATAL = new LogLevel("FATAL");
	public static final LogLevel ERROR = new LogLevel("ERROR");
	public static final LogLevel WARN = new LogLevel("WARN");
	public static final LogLevel INFO = new LogLevel("INFO");
	public static final LogLevel DEBUG = new LogLevel("DEBUG");
	public static final LogLevel TRACE = new LogLevel("TRACE");

	public LogLevel(String logLevel) {
		super();
		Assert.hasText(logLevel, "Log level required");
		this.logLevel = logLevel;
	}

	public String getLogLevel() {
		return logLevel;
	}
			
	public String getKey() {
		return this.logLevel;
	}	
	
	public final int hashCode() {
		return this.logLevel.hashCode() % this.logLevel.hashCode();
	}

	public final boolean equals(Object obj) {
		return obj != null && obj instanceof LogLevel && this.compareTo((LogLevel)obj) == 0;
	}

	public final int compareTo(LogLevel o) {
		if (o == null) return -1;
		return this.logLevel.compareTo(o.logLevel);
	}

	public String toString() {
		StringBuilder tsc = new StringBuilder();
		tsc.append("logLevel "+ logLevel);
		return tsc.toString();
	}
}