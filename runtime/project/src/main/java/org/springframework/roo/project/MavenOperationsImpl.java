package org.springframework.roo.project;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.ProcessManager;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.project.packaging.PackagingProvider;
import org.springframework.roo.project.packaging.PackagingProviderRegistry;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Implementation of {@link MavenOperations}.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @author Juan Carlos García
 * @author Paula Navarro
 * @since 1.0
 */
@Component
@Service
public class MavenOperationsImpl extends AbstractProjectOperations implements MavenOperations {

  protected final static Logger LOGGER = HandlerUtils.getLogger(MavenOperationsImpl.class);

  private PackagingProviderRegistry packagingProviderRegistry;
  private ProcessManager processManager;
  private ProjectOperations projectOperations;

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
    public LoggingInputStream(final InputStream inputStream, final ProcessManager processManager) {
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
          } else if (line.startsWith("[WARNING]")) {
            LOGGER.warning(line);
          } else {
            LOGGER.info(line);
          }
        }
      } catch (final IOException e) {
        // 1st condition for *nix/Mac, 2nd condition for Windows
        if (e.getMessage().contains("No such file or directory")
            || e.getMessage().contains("CreateProcess error=2")) {
          LOGGER
              .severe("Could not locate Maven executable; please ensure mvn command is in your path");
        }
      } finally {
        IOUtils.closeQuietly(inputStream);
        ActiveProcessManager.clearActiveProcessManager();
      }
    }
  }

  private void addModuleDeclaration(final String moduleName, final Document pomDocument,
      final Element root) {
    final Element modulesElement = createModulesElementIfNecessary(pomDocument, root);
    if (!isModuleAlreadyPresent(moduleName, modulesElement)) {
      modulesElement.appendChild(XmlUtils.createTextElement(pomDocument, "module", moduleName));
    }
  }

  public void createModule(Pom parentPom, final String moduleName,
      final PackagingProvider selectedPackagingProvider, final String artifactId) {
    createModule(parentPom, moduleName, selectedPackagingProvider, artifactId, null);
  }


  private void createModule(Pom parentPom, final String moduleName,
      final PackagingProvider selectedPackagingProvider, final String artifactId,
      final String folder) {

    Validate.isTrue(isCreateModuleAvailable(), "Cannot create modules at this time");

    if (getProjectOperations().getPomFromModuleName(moduleName) != null) {
      throw new IllegalArgumentException(String.format("Module %s already exists", moduleName));
    }

    if (parentPom == null) {
      // Get current module as parent
      parentPom = getProjectOperations().getFocusedModule();
    } else {
      // Set changes into the parent module
      setModule(parentPom);
    }

    // Validate parent has POM packaging
    if (!parentPom.getPackaging().equals("pom")) {
      throw new IllegalArgumentException("ERROR: Parent module packaging is not POM");
    }

    final PackagingProvider packagingProvider = getPackagingProvider(selectedPackagingProvider);
    final String pathToNewPom =
        packagingProvider.createArtifacts(
            getProjectOperations().getTopLevelPackage(parentPom.getModuleName()), artifactId, "",
            new GAV(parentPom.getGroupId(), parentPom.getArtifactId(), parentPom.getVersion()),
            moduleName, this);

    updateParentModulePom(moduleName);
    setModule(pomManagementService.getPomFromPath(pathToNewPom));

    if (folder == null) {
      createFolder(getProjectOperations().getTopLevelPackage(moduleName), null);
    } else {
      createFolder(getProjectOperations().getTopLevelPackage(parentPom.getModuleName()), folder);
    }
  }

  private Element createModulesElementIfNecessary(final Document pomDocument, final Element root) {
    Element modulesElement = XmlUtils.findFirstElement("/project/modules", root);
    if (modulesElement == null) {
      modulesElement = pomDocument.createElement("modules");
      final Element repositories = XmlUtils.findFirstElement("/project/repositories", root);
      root.insertBefore(modulesElement, repositories);
    }
    return modulesElement;
  }

  public void createMultimoduleProject(final JavaPackage topLevelPackage, final String projectName,
      final Integer majorJavaVersion, final Multimodule multimodule) {
    Validate.isTrue(isCreateProjectAvailable(), "Project creation is unavailable at this time");
    Validate.notNull(multimodule, "Multimodule must not be null");

    final PackagingProvider parentPackagingProvider =
        getPackagingProvider(getPackagingProviderRegistry().getPackagingProvider("pom"));
    final PackagingProvider warPackagingProvider =
        getPackagingProvider(getPackagingProviderRegistry().getPackagingProvider("war"));
    final PackagingProvider jarPackagingProvider =
        getPackagingProvider(getPackagingProviderRegistry().getPackagingProvider("jar"));

    // Create parent pom
    parentPackagingProvider.createArtifacts(topLevelPackage, projectName,
        getJavaVersion(majorJavaVersion), null, "", this);


    Pom pom = getProjectOperations().getPomFromModuleName("");

    // If developer selects STANDARD multimodule project, is necessary to create first
    // the standard modules (model, repository, integration, service-api and service-impl
    if (multimodule == Multimodule.STANDARD) {
      createModule(pom, "model", jarPackagingProvider, "model");
      createModule(pom, "repository", jarPackagingProvider, "repository");

      // ROO-3762: Generate integration module by default
      createModule(pom, "integration", jarPackagingProvider, "integration");

      createModule(pom, "service-api", jarPackagingProvider, "service.api");
      createModule(pom, "service-impl", jarPackagingProvider, "service.impl");

      // Add dependencies between modules
      getProjectOperations().addDependency("repository", pom.getGroupId(), "model",
          "${project.version}");
      getProjectOperations().addDependency("integration", pom.getGroupId(), "model",
          "${project.version}");
      getProjectOperations().addDependency("service-api", pom.getGroupId(), "model",
          "${project.version}");
      getProjectOperations().addDependency("service-impl", pom.getGroupId(), "repository",
          "${project.version}");
      getProjectOperations().addDependency("service-impl", pom.getGroupId(), "service.api",
          "${project.version}");
      getProjectOperations().addDependency("service-impl", pom.getGroupId(), "model",
          "${project.version}");
      getProjectOperations().addDependency("service-impl", pom.getGroupId(), "integration",
          "${project.version}");
    }

    // In all cases, multimodule architectures have an application module where Spring Boot artifacts are created 
    createModule(pom, "application", warPackagingProvider, "application", "");

    installApplicationConfiguration("application");

    // ROO-3687: Generates necessary Spring Boot artifacts into application module.
    createSpringBootApplicationClass(topLevelPackage, projectName);
    //    createApplicationTestsClass(topLevelPackage, projectName);

    // ROO-3741: Including banner.txt on application module
    addBannerFile(getPomFromModuleName("application"));

    // Also, if STANDARD multimodule project has been selected, is necessary to include dependencies between
    // application module and the generated modules above
    if (multimodule == Multimodule.STANDARD) {
      getProjectOperations().addDependency("application", pom.getGroupId(), "service.impl",
          "${project.version}");
      getProjectOperations().addDependency("application", pom.getGroupId(), "service.api",
          "${project.version}");
      getProjectOperations().addDependency("application", pom.getGroupId(), "repository",
          "${project.version}");
      getProjectOperations().addDependency("application", pom.getGroupId(), "model",
          "${project.version}");
    }

  }

  private void installApplicationConfiguration(String moduleName) {

    // Add Spring Boot dependences
    final Element configuration = XmlUtils.getConfiguration(getClass());
    final List<Dependency> requiredDependencies = new ArrayList<Dependency>();

    final List<Element> dependencies =
        XmlUtils.findElements("/configuration/dependencies/dependency", configuration);
    for (final Element dependencyElement : dependencies) {
      requiredDependencies.add(new Dependency(dependencyElement));
    }
    getProjectOperations().addDependencies(moduleName, requiredDependencies);


    // Add Plugins
    List<Element> plugins = XmlUtils.findElements("/configuration/plugins/plugin", configuration);
    for (Element element : plugins) {
      Plugin plugin = new Plugin(element);
      getProjectOperations().addBuildPlugin(moduleName, plugin);
    }

  }

  public void createProject(final JavaPackage topLevelPackage, final String projectName,
      final Integer majorJavaVersion, final PackagingProvider selectedPackagingProvider) {
    Validate.isTrue(isCreateProjectAvailable(), "Project creation is unavailable at this time");
    final PackagingProvider packagingProvider = getPackagingProvider(selectedPackagingProvider);
    packagingProvider.createArtifacts(topLevelPackage, projectName,
        getJavaVersion(majorJavaVersion), null, "", this);

    // ROO-3687: Generates necessary Spring Boot artifacts
    createSpringBootApplicationClass(topLevelPackage, projectName);
    //    createApplicationTestsClass(topLevelPackage, projectName);

    // ROO-3741: Including banner.txt
    addBannerFile(getPomFromModuleName(""));
  }

  /**
   * This method creates a banner.txt file inside generated project that
   * will be displayed when the generated Spring Boot application starts.
   * 
   * @param Pom module where banner.txt should be generated
   */
  private void addBannerFile(Pom module) {

    LogicalPath resourcesPath =
        LogicalPath.getInstance(Path.SRC_MAIN_RESOURCES, module.getModuleName());

    String sourceAntPath = "banner/banner.txt";
    String targetDirectory = getPathResolver().getIdentifier(resourcesPath, "");

    if (!getFileManager().exists(targetDirectory)) {
      getFileManager().createDirectory(targetDirectory);
    }

    final String path = FileUtils.getPath(getClass(), sourceAntPath);
    final Iterable<URL> urls = OSGiUtils.findEntriesByPattern(context, path);
    Validate.notNull(urls, "Could not search bundles for resources for Ant Path '%s'", path);
    for (final URL url : urls) {
      final String fileName = url.getPath().substring(url.getPath().lastIndexOf("/") + 1);
      try {
        String contents = IOUtils.toString(url);
        getFileManager().createOrUpdateTextFileIfRequired(targetDirectory + fileName, contents,
            false);
      } catch (final Exception e) {
        throw new IllegalStateException(e);
      }

    }
  }

  /**
   * Creates topLevelPackage folder structure inside the focused module.
   * If folder is not null, adds this new folder inside topLevelPackage folders
   * 
   * @param topLevelPackage folder structure represented as a package (required)
   * @param folder the folder to add inside topLevelPackage (can be null)
   */
  private void createFolder(JavaPackage topLevelPackage, String folder) {

    Validate.notNull(topLevelPackage, "Cannot create topLevelPackage folders");
    String filename =
        topLevelPackage.getFullyQualifiedPackageName().replace('.', File.separatorChar);

    if (StringUtils.isNotBlank(folder)) {
      filename += File.separatorChar + folder;
    }
    final String physicalPath =
        getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_JAVA, filename);
    getFileManager().createDirectory(physicalPath);

  }


  public void createSpringBootApplicationClass(JavaPackage topLevelPackage, String projectName) {
    // Set projectName if null
    if (projectName == null) {
      projectName = topLevelPackage.getLastElement();
    }

    // Capitalize projectName and removing white spaces
    projectName = StringUtils.capitalize(projectName).replaceAll("\\s+", "");

    // Application class name
    String bootClass = projectName.concat("Application");

    final JavaType javaType =
        new JavaType(topLevelPackage.getFullyQualifiedPackageName().concat(".").concat(bootClass));
    final String physicalPath =
        getPathResolver().getFocusedCanonicalPath(Path.SRC_MAIN_JAVA, javaType);
    if (getFileManager().exists(physicalPath)) {
      throw new RuntimeException(
          "ERROR: You are trying to create two Java classes annotated with @SpringBootApplication");
    }

    InputStream inputStream = null;
    try {
      inputStream = FileUtils.getInputStream(getClass(), "SpringBootApplication-template._java");
      String input = IOUtils.toString(inputStream);
      // Replacing package
      input = input.replace("__PACKAGE__", topLevelPackage.getFullyQualifiedPackageName());
      input = input.replace("__PROJECT_NAME__", projectName);
      getFileManager().createOrUpdateTextFileIfRequired(physicalPath, input, false);
    } catch (final IOException e) {
      throw new IllegalStateException("Unable to create '" + physicalPath + "'", e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }

  }

  //  public void createApplicationTestsClass(JavaPackage topLevelPackage, String projectName) {
  //    // Set projectName if null
  //    if (projectName == null) {
  //      projectName = topLevelPackage.getLastElement();
  //    }
  //    // Uppercase projectName
  //    projectName =
  //        projectName.substring(0, 1).toUpperCase()
  //            .concat(projectName.substring(1, projectName.length()));
  //    String testClass = projectName.concat("ApplicationTests");
  //
  //    final JavaType javaType =
  //        new JavaType(topLevelPackage.getFullyQualifiedPackageName().concat(".").concat(testClass));
  //    final String physicalPath =
  //        getPathResolver().getFocusedCanonicalPath(Path.SRC_TEST_JAVA, javaType);
  //    if (getFileManager().exists(physicalPath)) {
  //      throw new RuntimeException(
  //          "ERROR: You are trying to create two Java classes annotated with @SpringApplicationConfiguration that will be used to execute JUnit tests");
  //    }
  //
  //    InputStream inputStream = null;
  //    try {
  //      inputStream = FileUtils.getInputStream(getClass(), "SpringApplicationTests-template._java");
  //      String input = IOUtils.toString(inputStream);
  //      // Replacing package
  //      input = input.replace("__PACKAGE__", topLevelPackage.getFullyQualifiedPackageName());
  //      input = input.replace("__PROJECT_NAME__", projectName);
  //      getFileManager().createOrUpdateTextFileIfRequired(physicalPath, input, false);
  //    } catch (final IOException e) {
  //      throw new IllegalStateException("Unable to create '" + physicalPath + "'", e);
  //    } finally {
  //      IOUtils.closeQuietly(inputStream);
  //    }
  //
  //  }

  public void executeMvnCommand(final String extra) throws IOException {

    if (processManager == null) {
      processManager = getProcessManager();
    }

    Validate.notNull(processManager, "ProcessManager is required");

    final File root = new File(getProjectRoot());
    Validate.isTrue(root.isDirectory() && root.exists(),
        "Project root does not currently exist as a directory ('%s')", root.getCanonicalPath());

    final String cmd = (File.separatorChar == '\\' ? "mvn.bat " : "mvn ") + extra;
    final Process p = Runtime.getRuntime().exec(cmd, null, root);

    // Ensure separate threads are used for logging, as per ROO-652
    final LoggingInputStream input = new LoggingInputStream(p.getInputStream(), processManager);
    final LoggingInputStream errors = new LoggingInputStream(p.getErrorStream(), processManager);

    // Close OutputStream to avoid blocking by Maven commands that expect
    // input, as per ROO-2034
    IOUtils.closeQuietly(p.getOutputStream());
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

  /**
   * Returns the project's target Java version in POM format
   * 
   * @param majorJavaVersion the major version provided by the user; can be
   *            <code>null</code> to auto-detect it
   * @return a non-blank string
   */
  private String getJavaVersion(final Integer majorJavaVersion) {
    if (majorJavaVersion != null && majorJavaVersion >= 6 && majorJavaVersion <= 8) {
      switch (majorJavaVersion) {
        case 6:
          return "1.6";
        case 7:
          return "1.7";
        case 8:
          return "1.8";
        default:
          break;
      }
    }
    // By default, Spring Roo projects will be generated on Java 1.8
    return "1.8";
  }

  private PackagingProvider getPackagingProvider(final PackagingProvider selectedPackagingProvider) {
    if (packagingProviderRegistry == null) {
      packagingProviderRegistry = getPackagingProviderRegistry();
    }
    Validate.notNull(packagingProviderRegistry, "PackagingProviderRegistry is required");
    return ObjectUtils.defaultIfNull(selectedPackagingProvider,
        packagingProviderRegistry.getDefaultPackagingProvider());
  }

  public String getProjectRoot() {
    return pathResolver.getRoot(Path.ROOT.getModulePathId(pomManagementService
        .getFocusedModuleName()));
  }

  public boolean isCreateModuleAvailable() {
    return true;
  }

  public boolean isCreateProjectAvailable() {
    return !isProjectAvailable(getFocusedModuleName());
  }

  private boolean isModuleAlreadyPresent(final String moduleName, final Element modulesElement) {
    for (final Element element : XmlUtils.findElements("module", modulesElement)) {
      if (element.getTextContent().trim().equals(moduleName)) {
        return true;
      }
    }
    return false;
  }

  private void updateParentModulePom(final String moduleName) {
    final String parentPomPath = pomManagementService.getFocusedModule().getPath();
    final Document parentPomDocument = XmlUtils.readXml(fileManager.getInputStream(parentPomPath));
    final Element parentPomRoot = parentPomDocument.getDocumentElement();
    DomUtils.createChildIfNotExists("packaging", parentPomRoot, parentPomDocument).setTextContent(
        "pom");
    addModuleDeclaration(moduleName, parentPomDocument, parentPomRoot);
    final String addModuleMessage =
        getDescriptionOfChange(ADDED, Collections.singleton(moduleName), "module", "modules");
    fileManager.createOrUpdateTextFileIfRequired(getFocusedModule().getPath(),
        XmlUtils.nodeToString(parentPomDocument), addModuleMessage, false);
  }

  public PackagingProviderRegistry getPackagingProviderRegistry() {
    // Get all Services implement UndoManager interface
    try {
      ServiceReference<?>[] references =
          this.context.getAllServiceReferences(PackagingProviderRegistry.class.getName(), null);

      for (ServiceReference<?> ref : references) {
        return (PackagingProviderRegistry) this.context.getService(ref);
      }

      return null;

    } catch (InvalidSyntaxException e) {
      LOGGER.warning("Cannot load PackagingProviderRegistry on MavenOperationsImpl.");
      return null;
    }
  }

  public ProcessManager getProcessManager() {
    // Get all Services implement ProcessManager interface
    try {
      ServiceReference<?>[] references =
          this.context.getAllServiceReferences(ProcessManager.class.getName(), null);

      for (ServiceReference<?> ref : references) {
        return (ProcessManager) this.context.getService(ref);
      }

      return null;

    } catch (InvalidSyntaxException e) {
      LOGGER.warning("Cannot load ProcessManager on MavenOperationsImpl.");
      return null;
    }
  }

  public FileManager getFileManager() {
    if (fileManager == null) {
      // Get all Services implement FileManager interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(FileManager.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          fileManager = (FileManager) this.context.getService(ref);
          return fileManager;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load FileManager on MavenOperationsImpl.");
        return null;
      }
    } else {
      return fileManager;
    }
  }


  public ProjectOperations getProjectOperations() {

    if (projectOperations == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          projectOperations = (ProjectOperations) this.context.getService(ref);
          return projectOperations;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on MavenOperationsImpl.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }
}
