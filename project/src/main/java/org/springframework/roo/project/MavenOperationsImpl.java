package org.springframework.roo.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.ActiveProcessManager;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.ProcessManager;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.IOUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link MavenOperations}.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.0
 */
@Component
@Service
public class MavenOperationsImpl extends AbstractProjectOperations implements MavenOperations {

	private static final Dependency JAXB_API = new Dependency("javax.xml.bind", "jaxb-api", "2.1");

	private static final Dependency JSR250_API = new Dependency("javax.annotation", "jsr250-api", "1.0");

	// Constants
	private static final Logger logger = HandlerUtils.getLogger(MavenOperationsImpl.class);
	
	// Fields
	@Reference private ApplicationContextOperations applicationContextOperations;
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private ProcessManager processManager;

	public boolean isCreateProjectAvailable() {
		return !isProjectAvailable();
	}
	
	public String getProjectRoot() {
		return pathResolver.getRoot(Path.ROOT);
	}
	
	public void createProject(final JavaPackage topLevelPackage, String projectName, Integer majorJavaVersion) {
		Assert.isTrue(isCreateProjectAvailable(), "Project creation is unavailable at this time");
		Assert.notNull(topLevelPackage, "Top level package required");

		if (majorJavaVersion == null || (majorJavaVersion < 5 || majorJavaVersion > 7)) {
			// We need to detect the major Java version to use
			final String ver = System.getProperty("java.version");
			if (ver.contains("1.7.")) {
				majorJavaVersion = 7;
			} else if (ver.contains("1.6.")) {
				majorJavaVersion = 6;
			} else {
				// To be running Roo they must be on Java 5 or above
				majorJavaVersion = 5;
			}
		}

		if (projectName == null) {
			final String packageName = topLevelPackage.getFullyQualifiedPackageName();
			final int lastIndex = packageName.lastIndexOf(".");
			if (lastIndex == -1) {
				projectName = packageName;
			} else {
				projectName = packageName.substring(lastIndex + 1);
			}
		}

		final Document pom = XmlUtils.readXml(TemplateUtils.getTemplate(getClass(), "standard-project-template.xml"));

		final Element root = pom.getDocumentElement();
		
		XmlUtils.findRequiredElement("/project/artifactId", root).setTextContent(projectName);
		XmlUtils.findRequiredElement("/project/groupId", root).setTextContent(topLevelPackage.getFullyQualifiedPackageName());
		XmlUtils.findRequiredElement("/project/name", root).setTextContent(projectName);

		final List<Element> versionElements = XmlUtils.findElements("//*[.='JAVA_VERSION']", root);
		for (final Element versionElement : versionElements) {
			versionElement.setTextContent("1." + majorJavaVersion);
		}

		fileManager.createOrUpdateTextFileIfRequired(pathResolver.getIdentifier(Path.ROOT, "pom.xml"), XmlUtils.nodeToString(pom), true);

		// Java 5 needs the javax.annotation library (it's included in Java 6 and above), and the jaxb-api for Hibernate
		if (majorJavaVersion == 5) {
			addDependencies(Arrays.asList(JSR250_API, JAXB_API));
		}

		fileManager.scan();

		applicationContextOperations.createMiddleTierApplicationContext();

		try {
			FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "log4j.properties-template"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "log4j.properties")).getOutputStream());
		} catch (final IOException e) {
			logger.warning("Unable to install log4j logging configuration");
		}
	}

	public void executeMvnCommand(final String extra) throws IOException {
		final File root = new File(getProjectRoot());
		Assert.isTrue(root.isDirectory() && root.exists(), "Project root does not currently exist as a directory ('" + root.getCanonicalPath() + "')");

		final String cmd = (File.separatorChar == '\\' ? "mvn.bat " : "mvn ") + extra;
		final Process p = Runtime.getRuntime().exec(cmd, null, root);

		// Ensure separate threads are used for logging, as per ROO-652
		final LoggingInputStream input = new LoggingInputStream(p.getInputStream(), processManager);
		final LoggingInputStream errors = new LoggingInputStream(p.getErrorStream(), processManager);

		p.getOutputStream().close(); // Close OutputStream to avoid blocking by Maven commands that expect input, as per ROO-2034
		input.start();
		errors.start();

		try {
			if (p.waitFor() != 0) {
				logger.warning("The command '" + cmd + "' did not complete successfully");
			}
		} catch (final InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private static class LoggingInputStream extends Thread {

		// Fields
		private final BufferedReader reader;
		private final ProcessManager processManager;

		/**
		 * Constructor
		 *
		 * @param inputStream
		 * @param processManager
		 */
		public LoggingInputStream(final InputStream inputStream, final ProcessManager processManager) {
			this.reader = new BufferedReader(new InputStreamReader(inputStream));
			this.processManager = processManager;
		}

		@Override
		public void run() {
			ActiveProcessManager.setActiveProcessManager(processManager);
			String line;
			try {
				while ((line = reader.readLine()) != null) {
					if (line.startsWith("[ERROR]")) {
						logger.severe(line);
					} else if (line.startsWith("[WARNING]")) {
						logger.warning(line);
					} else {
						logger.info(line);
					}
				}
			} catch (final IOException e) {
				if (e.getMessage().contains("No such file or directory") || // For *nix/Mac
					e.getMessage().contains("CreateProcess error=2")) { // For Windows
					logger.severe("Could not locate Maven executable; please ensure mvn command is in your path");
				}
			} finally {
				IOUtils.closeQuietly(reader);
				ActiveProcessManager.clearActiveProcessManager();
			}
		}
	}
}
