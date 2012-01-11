package org.springframework.roo.addon.logging;

/**
 * Provides information related to the log level configuration of the LOGGER.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
public enum LogLevel {
    DEBUG, ERROR, FATAL, INFO, TRACE, WARN;

    @Override
    public String toString() {
        final StringBuilder tsc = new StringBuilder();
        tsc.append("logLevel " + name());
        return tsc.toString();
    }
}