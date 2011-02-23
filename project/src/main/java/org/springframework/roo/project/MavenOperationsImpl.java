package org.springframework.roo.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.ActiveProcessManager;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.process.manager.ProcessManager;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
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
	private static final Logger logger = HandlerUtils.getLogger(MavenOperationsImpl.class);
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
	
	public void createProject(JavaPackage topLevelPackage, String projectName, Integer majorJavaVersion) {
		Assert.isTrue(isCreateProjectAvailable(), "Project creation is unavailable at this time");
		Assert.notNull(topLevelPackage, "Top level package required");

		if (majorJavaVersion == null || (majorJavaVersion < 5 || majorJavaVersion > 7)) {
			// We need to detect the major Java version to use
			String ver = System.getProperty("java.version");
			if (ver.indexOf("1.7.") > -1) {
				majorJavaVersion = 7;
			} else if (ver.indexOf("1.6.") > -1) {
				majorJavaVersion = 6;
			} else {
				// To be running Roo they must be on Java 5 or above
				majorJavaVersion = 5;
			}
		}

		if (projectName == null) {
			String packageName = topLevelPackage.getFullyQualifiedPackageName();
			int lastIndex = packageName.lastIndexOf(".");
			if (lastIndex == -1) {
				projectName = packageName;
			} else {
				projectName = packageName.substring(lastIndex + 1);
			}
		}

		Document pom;
		try {
			pom = XmlUtils.getDocumentBuilder().parse(TemplateUtils.getTemplate(getClass(), "standard-project-template.xml"));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element root = pom.getDocumentElement();
		
		XmlUtils.findRequiredElement("/project/artifactId", root).setTextContent(projectName);
		XmlUtils.findRequiredElement("/project/groupId", root).setTextContent(topLevelPackage.getFullyQualifiedPackageName());
		XmlUtils.findRequiredElement("/project/name", root).setTextContent(projectName);

		List<Element> versionElements = XmlUtils.findElements("//*[.='JAVA_VERSION']", root);
		for (Element versionElement : versionElements) {
			versionElement.setTextContent("1." + majorJavaVersion);
		}

		MutableFile pomMutableFile = fileManager.createFile(pathResolver.getIdentifier(Path.ROOT, "pom.xml"));
		XmlUtils.writeXml(pomMutableFile.getOutputStream(), pom);

		// Java 5 needs the javax.annotation library (it's included in Java 6 and above), and the jaxb-api for Hibernate
		if (majorJavaVersion == 5) {
			List<Dependency> dependencies = new ArrayList<Dependency>();
			dependencies.add(new Dependency("javax.annotation", "jsr250-api", "1.0"));
			dependencies.add(new Dependency("javax.xml.bind", "jaxb-api", "2.1"));
			addDependencies(dependencies);
		}

		fileManager.scan();

		applicationContextOperations.createMiddleTierApplicationContext();

		try {
			FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "log4j.properties-template"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "log4j.properties")).getOutputStream());
		} catch (IOException e) {
			logger.warning("Unable to install log4j logging configuration");
		}
	}

	public void executeMvnCommand(String extra) throws IOException {
		File root = new File(getProjectRoot());
		Assert.isTrue(root.isDirectory() && root.exists(), "Project root does not currently exist as a directory ('" + root.getCanonicalPath() + "')");

		String cmd = (File.separatorChar == '\\' ? "mvn.bat " : "mvn ") + extra;
		Process p = Runtime.getRuntime().exec(cmd, null, root);

		// Ensure separate threads are used for logging, as per ROO-652
		LoggingInputStream input = new LoggingInputStream(p.getInputStream(), processManager);
		LoggingInputStream errors = new LoggingInputStream(p.getErrorStream(), processManager);

		p.getOutputStream().close(); // Close OutputStream to avoid blocking by Maven commands that expect input, as per ROO-2034
		input.start();
		errors.start();

		try {
			if (p.waitFor() != 0) {
				logger.warning("The command '" + cmd + "' did not complete successfully");
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private class LoggingInputStream extends Thread {
		private BufferedReader reader;
		private ProcessManager processManager;

		public LoggingInputStream(InputStream inputStream, ProcessManager processManager) {
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
			} catch (IOException e) {
				if (e.getMessage().contains("No such file or directory") || // For *nix/Mac
					e.getMessage().contains("CreateProcess error=2")) { // For Windows
					logger.severe("Could not locate Maven executable; please ensure mvn command is in your path");
				}
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException ignored) {
					}
				}
				ActiveProcessManager.clearActiveProcessManager();
			}
		}
	}	
}
