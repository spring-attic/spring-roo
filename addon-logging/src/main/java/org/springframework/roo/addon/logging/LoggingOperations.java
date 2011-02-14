package org.springframework.roo.addon.logging;

/**
 * Provides logging configuration operations.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface LoggingOperations {

	boolean isConfigureLoggingAvailable();

	void configureLogging(LogLevel logLevel, LoggerPackage loggerPackage);
}