package org.springframework.roo.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.ActiveProcessManager;
import org.springframework.roo.process.manager.ProcessManager;
import org.springframework.roo.project.packaging.PackagingProvider;
import org.springframework.roo.project.packaging.PackagingProviderRegistry;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.IOUtils;
import org.springframework.roo.support.util.ObjectUtils;
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
@Component(immediate = true)
@Service
public class MavenOperationsImpl extends AbstractProjectOperations implements
        MavenOperations {

    // Constants
    private static final Logger LOGGER = HandlerUtils
            .getLogger(MavenOperationsImpl.class);

    // Fields
    @Reference private PackagingProviderRegistry packagingProviderRegistry;
    @Reference private ProcessManager processManager;

    public boolean isCreateProjectAvailable() {
        return !isProjectAvailable(getFocusedModuleName());
    }

    public boolean isCreateModuleAvailable() {
        return true;
    }

    public String getProjectRoot() {
        return pathResolver.getRoot(Path.ROOT
                .getModulePathId(pomManagementService.getFocusedModuleName()));
    }

    public void createProject(final JavaPackage topLevelPackage,
            final String projectName, final Integer majorJavaVersion,
            final GAV parentPom,
            final PackagingProvider selectedPackagingProvider) {
        Assert.isTrue(isCreateProjectAvailable(),
                "Project creation is unavailable at this time");
        final PackagingProvider packagingProvider = getPackagingProvider(selectedPackagingProvider);
        packagingProvider.createArtifacts(topLevelPackage, projectName,
                getJavaVersion(majorJavaVersion), parentPom, "", this);
    }

    private PackagingProvider getPackagingProvider(
            final PackagingProvider selectedPackagingProvider) {
        return ObjectUtils.defaultIfNull(selectedPackagingProvider,
                packagingProviderRegistry.getDefaultPackagingProvider());
    }

    public void createModule(final JavaPackage topLevelPackage,
            final GAV parentPom, final String moduleName,
            final PackagingProvider selectedPackagingProvider,
            Integer majorJavaVersion) {
        Assert.isTrue(isCreateModuleAvailable(),
                "Cannot create modules at this time");
        final PackagingProvider packagingProvider = getPackagingProvider(selectedPackagingProvider);
        final String pathToNewPom = packagingProvider.createArtifacts(
                topLevelPackage, "", getJavaVersion(majorJavaVersion),
                parentPom, moduleName, this);
        updateParentModulePom(moduleName);
        setModule(pomManagementService.getPomFromPath(pathToNewPom));
    }

    private void updateParentModulePom(final String moduleName) {
        final String parentPomPath = pomManagementService.getFocusedModule()
                .getPath();
        final Document parentPomDocument = XmlUtils.readXml(fileManager
                .getInputStream(parentPomPath));
        final Element parentPomRoot = parentPomDocument.getDocumentElement();
        DomUtils.createChildIfNotExists("packaging", parentPomRoot,
                parentPomDocument).setTextContent("pom");
        addModuleDeclaration(moduleName, parentPomDocument, parentPomRoot);
        final String addModuleMessage = getDescriptionOfChange(ADDED,
                Collections.singleton(moduleName), "module", "modules");
        fileManager.createOrUpdateTextFileIfRequired(getFocusedModule()
                .getPath(), XmlUtils.nodeToString(parentPomDocument),
                addModuleMessage, false);
    }

    private void addModuleDeclaration(final String moduleName,
            final Document pomDocument, final Element root) {
        final Element modulesElement = createModulesElementIfNecessary(
                pomDocument, root);
        if (!isModuleAlreadyPresent(moduleName, modulesElement)) {
            modulesElement.appendChild(XmlUtils.createTextElement(pomDocument,
                    "module", moduleName));
        }
    }

    private Element createModulesElementIfNecessary(final Document pomDocument,
            final Element root) {
        Element modulesElement = XmlUtils.findFirstElement("/project/modules",
                root);
        if (modulesElement == null) {
            modulesElement = pomDocument.createElement("modules");
            final Element repositories = XmlUtils.findFirstElement(
                    "/project/repositories", root);
            root.insertBefore(modulesElement, repositories);
        }
        return modulesElement;
    }

    private boolean isModuleAlreadyPresent(final String moduleName,
            final Element modulesElement) {
        for (final Element element : XmlUtils.findElements("module",
                modulesElement)) {
            if (element.getTextContent().trim().equals(moduleName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the project's target Java version in POM format
     * 
     * @param majorJavaVersion the major version provided by the user; can be
     *            <code>null</code> to auto-detect it
     * @return a non-blank string
     */
    private String getJavaVersion(final Integer majorJavaVersion) {
        if (majorJavaVersion != null && majorJavaVersion >= 5
                && majorJavaVersion <= 7) {
            return String.valueOf(majorJavaVersion);
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
        Assert.isTrue(root.isDirectory() && root.exists(),
                "Project root does not currently exist as a directory ('"
                        + root.getCanonicalPath() + "')");

        final String cmd = (File.separatorChar == '\\' ? "mvn.bat " : "mvn ")
                + extra;
        final Process p = Runtime.getRuntime().exec(cmd, null, root);

        // Ensure separate threads are used for logging, as per ROO-652
        final LoggingInputStream input = new LoggingInputStream(
                p.getInputStream(), processManager);
        final LoggingInputStream errors = new LoggingInputStream(
                p.getErrorStream(), processManager);

        p.getOutputStream().close(); // Close OutputStream to avoid blocking by
                                     // Maven commands that expect input, as per
                                     // ROO-2034
        input.start();
        errors.start();

        try {
            if (p.waitFor() != 0) {
                LOGGER.warning("The command '" + cmd
                        + "' did not complete successfully");
            }
        }
        catch (final InterruptedException e) {
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
        public LoggingInputStream(final InputStream inputStream,
                final ProcessManager processManager) {
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
                    }
                    else if (line.startsWith("[WARNING]")) {
                        LOGGER.warning(line);
                    }
                    else {
                        LOGGER.info(line);
                    }
                }
            }
            catch (final IOException e) {
                if (e.getMessage().contains("No such file or directory") || // For
                                                                            // *nix/Mac
                        e.getMessage().contains("CreateProcess error=2")) { // For
                                                                            // Windows
                    LOGGER.severe("Could not locate Maven executable; please ensure mvn command is in your path");
                }
            }
            finally {
                IOUtils.closeQuietly(reader);
                ActiveProcessManager.clearActiveProcessManager();
            }
        }
    }
}
