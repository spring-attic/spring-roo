package org.springframework.roo.addon.logging;

/**
 * Interface to {@link LoggingOperationsImpl}.
 * 
 * @author Ben Alex
 *
 */
public interface LoggingOperations {

	public abstract boolean isConfigureLoggingAvailable();

	public abstract void configureLogging(LogLevel logLevel, LoggerPackage loggerPackage);

}