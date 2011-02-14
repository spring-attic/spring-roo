package org.springframework.roo.addon.logging;

/**
 * Provides information related to the log level configuration of the logger.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
public enum LogLevel {
	FATAL,
	ERROR,
	WARN,
	INFO,
	DEBUG,
	TRACE;

	public String toString() {
		StringBuilder tsc = new StringBuilder();
		tsc.append("logLevel "+ name());
		return tsc.toString();
	}
}