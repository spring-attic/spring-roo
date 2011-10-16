package org.springframework.roo.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.ActiveProcessManager;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.ProcessManager;
import org.springframework.roo.project.packaging.PackagingProvider;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.IOUtils;
import org.springframework.roo.support.util.StringUtils;

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

	// Constants
	private static final Logger LOGGER = HandlerUtils.getLogger(MavenOperationsImpl.class);

	// Fields
	@Reference private FileManager fileManager;
	@Reference private ProcessManager processManager;

	public boolean isCreateProjectAvailable() {
		return !isProjectAvailable();
	}

	public String getProjectRoot() {
		return pathResolver.getRoot(Path.ROOT);
	}

	public void createProject(final JavaPackage topLevelPackage, final String projectName, final Integer majorJavaVersion, final GAV parentPom, final PackagingProvider packagingType) {
		Assert.isTrue(isCreateProjectAvailable(), "Project creation is unavailable at this time");
		final String javaVersion = getJavaVersion(majorJavaVersion);
		packagingType.createArtifacts(topLevelPackage, projectName, javaVersion, parentPom);
	}

	/**
	 * Returns the project's target Java version in POM format
	 *
	 * @param majorJavaVersion the major version provided by the user; can be
	 * <code>null</code> to auto-detect it
	 * @return a non-blank string
	 */
	private final String getJavaVersion(final Integer majorJavaVersion) {
		if (majorJavaVersion != null && majorJavaVersion >= 5 && majorJavaVersion <= 7) {
			return "1." + majorJavaVersion;
		}

		// No valid version given; detect the major Java version to use
		final String ver = System.getProperty("java.version");
		if (ver.contains("1.7.")) {
			return "1.6"; // This is a workaround for ROO-2824
		}
		if (ver.contains("1.6.")) {
			return "1.6";
		}
		// To be running Roo they must be on Java 5 or above
		return "1.5";
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
				LOGGER.warning("The command '" + cmd + "' did not complete successfully");
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
						LOGGER.severe(line);
					} else if (line.startsWith("[WARNING]")) {
						LOGGER.warning(line);
					} else {
						LOGGER.info(line);
					}
				}
			} catch (final IOException e) {
				if (e.getMessage().contains("No such file or directory") || // For *nix/Mac
					e.getMessage().contains("CreateProcess error=2")) { // For Windows
					LOGGER.severe("Could not locate Maven executable; please ensure mvn command is in your path");
				}
			} finally {
				IOUtils.closeQuietly(reader);
				ActiveProcessManager.clearActiveProcessManager();
			}
		}
	}

	public boolean isCreateModuleAvailable() {
		// TODO Waiting on JTT's work for ROO-120 to find out if the currently focussed module is POM-packaged
		return false;
	}

	public void createModule(final JavaPackage topLevelPackage, final String name, final GAV parent, final PackagingProvider packagingType) {
		Assert.isTrue(isCreateModuleAvailable(), "Cannot create modules at this time");
		final String moduleName = StringUtils.defaultIfEmpty(name, topLevelPackage.getLastElement());
		final GAV module = new GAV(topLevelPackage.getFullyQualifiedPackageName(), moduleName, parent.getVersion());
		// TODO create or update "modules" element of parent module's POM
		// Create the new module's directory, named by its artifactId (Maven standard practice)
		fileManager.createDirectory(moduleName);
		// Focus the new module so that artifacts created below go to the correct path(s)
		focus(module);
		packagingType.createArtifacts(topLevelPackage, name, "${java.version}", parent);
	}

	public void focus(final GAV module) {
		Assert.notNull(module, "Specify the module to focus on");
		throw new UnsupportedOperationException("Module focussing not implemented yet");	// TODO by JTT for ROO-120
	}
}
