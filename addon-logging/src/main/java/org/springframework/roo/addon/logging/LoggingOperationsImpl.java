package org.springframework.roo.addon.logging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Properties;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.IOUtils;

/**
 * Implementation of {@link LoggingOperations}.
 *
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component
@Service
public class LoggingOperationsImpl implements LoggingOperations {

	// Fields
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private ProjectOperations projectOperations;

	public boolean isConfigureLoggingAvailable() {
		return projectOperations.isFocusedProjectAvailable();
	}

	public void configureLogging(final LogLevel logLevel, final LoggerPackage loggerPackage) {
		Assert.notNull(logLevel, "LogLevel required");
		Assert.notNull(loggerPackage, "LoggerPackage required");

		setupProperties(logLevel, loggerPackage);
	}

	private void setupProperties(final LogLevel logLevel, final LoggerPackage loggerPackage) {
		String filePath = pathResolver.getFocusedIdentifier(Path.SRC_MAIN_RESOURCES, "log4j.properties");
		MutableFile log4jMutableFile = null;
		Properties props = new Properties();

		InputStream inputStream = null;
		try {
			if (fileManager.exists(filePath)) {
				log4jMutableFile = fileManager.updateFile(filePath);
				inputStream = log4jMutableFile.getInputStream();
				props.load(inputStream);
			} else {
				log4jMutableFile = fileManager.createFile(filePath);
				inputStream = FileUtils.getInputStream(getClass(), "log4j-template.properties");
				Assert.notNull(inputStream, "Could not acquire log4j configuration template");
				props.load(inputStream);
			}
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}

		JavaPackage topLevelPackage = projectOperations.getTopLevelPackage(projectOperations.getFocusedModuleName());
		final String logStr = "log4j.logger.";

		switch (loggerPackage) {
			case ROOT:
				props.remove("log4j.rootLogger");
				props.setProperty("log4j.rootLogger", logLevel.name() + ", stdout");
				break;
			case PROJECT:
				props.remove(logStr + topLevelPackage.getFullyQualifiedPackageName());
				props.setProperty(logStr + topLevelPackage.getFullyQualifiedPackageName(), logLevel.name());
				break;
			default:
				for (String packageName : loggerPackage.getPackageNames()) {
					props.remove(logStr + packageName);
					props.setProperty(logStr + packageName, logLevel.name());
				}
				break;
		}

		OutputStream outputStream = null;
		try {
			outputStream = log4jMutableFile.getOutputStream();
			props.store(outputStream, "Updated at " + new Date());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		} finally {
			IOUtils.closeQuietly(outputStream);
		}
	}
}
