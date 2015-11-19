package org.springframework.roo.project;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.ActiveProcessManager;
import org.springframework.roo.process.manager.ProcessManager;
import org.springframework.roo.project.packaging.PackagingProvider;
import org.springframework.roo.project.packaging.PackagingProviderRegistry;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.FileUtils;
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
public class MavenOperationsImpl extends AbstractProjectOperations implements
        MavenOperations {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(MavenOperationsImpl.class);
	
	private PackagingProviderRegistry packagingProviderRegistry;
    private ProcessManager processManager;
	
	// ------------ OSGi component attributes ----------------
   	private BundleContext context;
   	
   	protected void activate(final ComponentContext context) {
    	this.context = context.getBundleContext();
    }

    private static class LoggingInputStream extends Thread {
        private InputStream inputStream;
        private final ProcessManager processManager;

        /**
         * Constructor
         * 
         * @param inputStream
         * @param processManager
         */
        public LoggingInputStream(final InputStream inputStream,
                final ProcessManager processManager) {
            this.inputStream = inputStream;
            this.processManager = processManager;
        }

        @Override
        public void run() {
            ActiveProcessManager.setActiveProcessManager(processManager);
            try {
                for (String line : IOUtils.readLines(inputStream)) {
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
                // 1st condition for *nix/Mac, 2nd condition for Windows
                if (e.getMessage().contains("No such file or directory")
                        || e.getMessage().contains("CreateProcess error=2")) {
                    LOGGER.severe("Could not locate Maven executable; please ensure mvn command is in your path");
                }
            }
            finally {
                IOUtils.closeQuietly(inputStream);
                ActiveProcessManager.clearActiveProcessManager();
            }
        }
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

    public void createModule(final JavaPackage topLevelPackage,
            final GAV parentPom, final String moduleName,
            final PackagingProvider selectedPackagingProvider,
            final Integer majorJavaVersion, final String artifactId) {
        Validate.isTrue(isCreateModuleAvailable(),
                "Cannot create modules at this time");
        final PackagingProvider packagingProvider = getPackagingProvider(selectedPackagingProvider);
        final String pathToNewPom = packagingProvider.createArtifacts(
                topLevelPackage, artifactId, getJavaVersion(majorJavaVersion),
                parentPom, moduleName, this);
        updateParentModulePom(moduleName);
        setModule(pomManagementService.getPomFromPath(pathToNewPom));
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

    public void createProject(final JavaPackage topLevelPackage,
            final String projectName, final Integer majorJavaVersion,
            final GAV parentPom,
            final PackagingProvider selectedPackagingProvider) {
        Validate.isTrue(isCreateProjectAvailable(),
                "Project creation is unavailable at this time");
        final PackagingProvider packagingProvider = getPackagingProvider(selectedPackagingProvider);
        packagingProvider.createArtifacts(topLevelPackage, projectName,
                getJavaVersion(majorJavaVersion), parentPom, "", this);
        
    	// ROO-3687: Generates @SpringBootApplication Java class
        createSpringBootApplicationClass(topLevelPackage, projectName);
        
        // ROO-3687: Generates application.properties file that will be used by Spring Boot
        createApplicationPropertiesFile();
        
        // ROO-3687: Generates @SpringApplicationConfiguration file that will be
        // used by JUnit Tests
        createApplicationTestsClass(topLevelPackage, projectName);
    }

    /**
     * Method that creates Java class annotated with @SpringApplicationConfiguration 
     * that will be used by JUnit Tests
     * 
     * @param topLevelPackage
     * @param projectName
     */
    private void createApplicationTestsClass(JavaPackage topLevelPackage,
            String projectName) {
     // Set projectName if null
        if (projectName == null) {
            projectName = topLevelPackage.getLastElement();
        }
        // Uppercase projectName
        projectName = projectName.substring(0, 1).toUpperCase()
                .concat(projectName.substring(1, projectName.length()));
        String testClass = projectName.concat("ApplicationTests");

        final JavaType javaType = new JavaType(topLevelPackage
                .getFullyQualifiedPackageName().concat(".").concat(testClass));
        final String physicalPath = pathResolver
                .getFocusedCanonicalPath(Path.SRC_TEST_JAVA, javaType);
        if (fileManager.exists(physicalPath)) {
            throw new RuntimeException(
                    "ERROR: You are trying to create two Java classes annotated with @SpringApplicationConfiguration that will be used to execute JUnit tests");
        }

        InputStream inputStream = null;
        try {
            inputStream = FileUtils.getInputStream(getClass(),
                    "SpringApplicationTests-template._java");
            String input = IOUtils.toString(inputStream);
            // Replacing package
            input = input.replace("__PACKAGE__",
                    topLevelPackage.getFullyQualifiedPackageName());
            input = input.replace("__PROJECT_NAME__", projectName);
            fileManager.createOrUpdateTextFileIfRequired(physicalPath, input,
                    false);
        }
        catch (final IOException e) {
            throw new IllegalStateException(
                    "Unable to create '" + physicalPath + "'", e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * Method that creates application.properties file that will be used by Spring Boot
     * 
     */
    private void createApplicationPropertiesFile() {
        LogicalPath resourcesPath = Path.SRC_MAIN_RESOURCES
                .getModulePathId("");
        
        if(!fileManager.exists(getPathResolver().getIdentifier(resourcesPath,
                "application.properties"))){
            fileManager.createFile(getPathResolver().getIdentifier(resourcesPath,
                    "application.properties"));
        }
    }

    /**
     * Method that creates Java class annotated with @SpringBootApplication
     * 
     * @param topLevelPackage
     * @param projectName
     */
    private void createSpringBootApplicationClass(JavaPackage topLevelPackage, String projectName) {
        // Set projectName if null
        if (projectName == null) {
            projectName = topLevelPackage.getLastElement();
        }
        // Uppercase projectName
        projectName = projectName.substring(0, 1).toUpperCase()
                .concat(projectName.substring(1, projectName.length()));
        String bootClass = projectName.concat("Application");

        final JavaType javaType = new JavaType(topLevelPackage
                .getFullyQualifiedPackageName().concat(".").concat(bootClass));
        final String physicalPath = pathResolver
                .getFocusedCanonicalPath(Path.SRC_MAIN_JAVA, javaType);
        if (fileManager.exists(physicalPath)) {
            throw new RuntimeException(
                    "ERROR: You are trying to create two Java classes annotated with @SpringBootApplication");
        }

        InputStream inputStream = null;
        try {
            inputStream = FileUtils.getInputStream(getClass(),
                    "SpringBootApplication-template._java");
            String input = IOUtils.toString(inputStream);
            // Replacing package
            input = input.replace("__PACKAGE__",
                    topLevelPackage.getFullyQualifiedPackageName());
            input = input.replace("__PROJECT_NAME__", projectName);
            fileManager.createOrUpdateTextFileIfRequired(physicalPath, input,
                    false);
        }
        catch (final IOException e) {
            throw new IllegalStateException(
                    "Unable to create '" + physicalPath + "'", e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }

	}

	public void executeMvnCommand(final String extra) throws IOException {
    	
    	if(processManager == null){
    		processManager = getProcessManager();
    	}
    	
    	Validate.notNull(processManager, "ProcessManager is required");
    	
        final File root = new File(getProjectRoot());
        Validate.isTrue(root.isDirectory() && root.exists(),
                "Project root does not currently exist as a directory ('%s')",
                root.getCanonicalPath());

        final String cmd = (File.separatorChar == '\\' ? "mvn.bat " : "mvn ")
                + extra;
        final Process p = Runtime.getRuntime().exec(cmd, null, root);

        // Ensure separate threads are used for logging, as per ROO-652
        final LoggingInputStream input = new LoggingInputStream(
                p.getInputStream(), processManager);
        final LoggingInputStream errors = new LoggingInputStream(
                p.getErrorStream(), processManager);

        // Close OutputStream to avoid blocking by Maven commands that expect
        // input, as per ROO-2034
        IOUtils.closeQuietly(p.getOutputStream());
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

    /**
     * Returns the project's target Java version in POM format
     * 
     * @param majorJavaVersion the major version provided by the user; can be
     *            <code>null</code> to auto-detect it
     * @return a non-blank string
     */
    private String getJavaVersion(final Integer majorJavaVersion) {
        if (majorJavaVersion != null && majorJavaVersion >= 6
                && majorJavaVersion <= 7) {
            return String.valueOf(majorJavaVersion);
        }
        // To be running Roo they must be on Java 6 or above
        return "1.6";
    }

    private PackagingProvider getPackagingProvider(
            final PackagingProvider selectedPackagingProvider) {
    	if(packagingProviderRegistry == null){
    		packagingProviderRegistry = getPackagingProviderRegistry();
    	}
    	Validate.notNull(packagingProviderRegistry, "PackagingProviderRegistry is required");
        return ObjectUtils.defaultIfNull(selectedPackagingProvider,
                packagingProviderRegistry.getDefaultPackagingProvider());
    }

    public String getProjectRoot() {
        return pathResolver.getRoot(Path.ROOT
                .getModulePathId(pomManagementService.getFocusedModuleName()));
    }

    public boolean isCreateModuleAvailable() {
        return true;
    }

    public boolean isCreateProjectAvailable() {
        return !isProjectAvailable(getFocusedModuleName());
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
    
    public PackagingProviderRegistry getPackagingProviderRegistry(){
    	// Get all Services implement UndoManager interface
		try {
			ServiceReference<?>[] references = this.context.getAllServiceReferences(PackagingProviderRegistry.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (PackagingProviderRegistry) this.context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load PackagingProviderRegistry on MavenOperationsImpl.");
			return null;
		}
    }
    
    public ProcessManager getProcessManager(){
    	// Get all Services implement ProcessManager interface
		try {
			ServiceReference<?>[] references = this.context.getAllServiceReferences(ProcessManager.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (ProcessManager) this.context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load ProcessManager on MavenOperationsImpl.");
			return null;
		}
    }
    
}
