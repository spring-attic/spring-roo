package org.springframework.roo.addon.logging;

import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.converters.StaticFieldConverter;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;

/**
 * Commands for the 'logging' add-on to be used by the ROO shell.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class LoggingCommands implements CommandMarker {
	
	private LoggingOperations loggingOperations;
	
	public LoggingCommands(StaticFieldConverter staticFieldConverter, LoggingOperations loggingOperations) {
		Assert.notNull(staticFieldConverter, "Static field converter required");
		Assert.notNull(loggingOperations, "Logging operations required");
		staticFieldConverter.add(LoggerPackage.class);
		staticFieldConverter.add(LogLevel.class);
		this.loggingOperations = loggingOperations;
	}
	
	/**
	 * @return true if the "configure logging" command is available at this moment
	 */
	@CliAvailabilityIndicator("configure logging")
	public boolean isConfigureLoggingAvailable() {
		return loggingOperations.isConfigureLoggingAvailable();
	}
	
	@CliCommand(value="configure logging", help="Configure logging in your project")
	public void configureLogging(@CliOption(key={"","level"}, mandatory=true, help="The log level to configure") LogLevel logLevel,
			@CliOption(key="package", mandatory=false, help="The package to append the logging level to (all by default)") LoggerPackage loggerAppender) {
		
		loggingOperations.configureLogging(logLevel, loggerAppender==null ? LoggerPackage.ROOT : loggerAppender);
	}
}