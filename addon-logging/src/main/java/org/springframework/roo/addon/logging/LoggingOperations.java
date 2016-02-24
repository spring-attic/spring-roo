package org.springframework.roo.addon.logging;

/**
 * Provides logging configuration operations.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface LoggingOperations {

    void configureLogging(LogLevel logLevel, LoggerPackage loggerPackage);

    boolean isLoggingInstallationPossible();
}