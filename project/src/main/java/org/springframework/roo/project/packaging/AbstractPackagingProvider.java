package org.springframework.roo.project.packaging;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.ApplicationContextOperations;
import org.springframework.roo.project.GAV;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Convenient superclass for core or third-party addons to implement a {@link PackagingProvider}.
 *
 * Uses the "Template Method" GoF pattern.
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component(componentAbstract = true)
public abstract class AbstractPackagingProvider implements PackagingProvider {

	// Constants
	protected static final Logger LOGGER = HandlerUtils.getLogger(PackagingProvider.class);
	private static final String JAVA_VERSION_PLACEHOLDER = "JAVA_VERSION";

	// Fields
	@Reference protected ApplicationContextOperations applicationContextOperations;
	@Reference protected FileManager fileManager;
	@Reference protected PathResolver pathResolver;

	private final String id;
	private final String name;
	private final String pomTemplate;

	/**
	 * Constructor
	 *
	 * @param id the unique ID of this packaging type, see {@link PackagingProvider#getId()}
	 * @param name the name of this type of packaging as used in the POM (required)
	 * @param pomTemplate the path of this packaging type's POM template,
	 * relative to its own package, as per {@link Class#getResourceAsStream(String)};
	 * this template should contain a "parent" element with its own groupId,
	 * artifactId, and version elements; this parent element will be removed if
	 * not required
	 */
	protected AbstractPackagingProvider(final String id, final String name, final String pomTemplate) {
		Assert.hasText(id, "ID is required");
		Assert.hasText(name, "Name is required");
		Assert.hasText(pomTemplate, "POM template path is required");
		this.id = id;
		this.name = name;
		this.pomTemplate = pomTemplate;
	}
	
	public String getId() {
		return id;
	}

	public String createArtifacts(final JavaPackage topLevelPackage, final String nullableProjectName, final String javaVersion, final GAV parentPom, final String module, final ProjectOperations projectOperations) {
		final String pomPath = createPom(topLevelPackage, nullableProjectName, javaVersion, parentPom, module, projectOperations);
		fileManager.scan();	// TODO not sure why or if this is necessary; find out and document/remove it
		createOtherArtifacts(topLevelPackage, module);
		return pomPath;
	}

	/**
	 * Creates the Maven POM using the subclass' POM template as follows:
	 * <ul>
	 * <li>sets the parent POM to the given parent (if any)</li>
	 * <li>sets the groupId to the result of {@link #getGroupId}, omitting this
	 * element if it's the same as the parent's groupId (as per Maven best
	 * practice)</li>
	 * <li>sets the artifactId to the result of {@link #getArtifactId}</li>
	 * <li>sets the packaging to the result of {@link #getName()}</li>
	 * <li>sets the project name to the result of {@link #getProjectName}</li>
	 * <li>replaces all occurrences of {@link #JAVA_VERSION_PLACEHOLDER}
	 *   with the given Java version</li>
	 * </ul>
	 *
	 * This method makes as few assumptions about the POM template as possible,
	 * to make life easier for authors of new {@link PackagingProvider}s.
	 *
	 * @param topLevelPackage the new project or module's top-level Java package (required)
	 * @param projectName the project name provided by the user (can be blank)
	 * @param javaVersion the Java version to substitute into the POM (required)
	 * @param parentPom the Maven coordinates of the parent POM (can be <code>null</code>)
	 * @return the path of the newly created POM
	 */
	protected String createPom(final JavaPackage topLevelPackage, final String projectName, final String javaVersion, final GAV parentPom, final String module, final ProjectOperations projectOperations) {
		Assert.hasText(javaVersion, "Java version required");
		Assert.notNull(topLevelPackage, "Top level package required");

		// Read the POM template from the classpath
		final Document pom = XmlUtils.readXml(FileUtils.getInputStream(getClass(), this.pomTemplate));
		final Element root = pom.getDocumentElement();

		// name
		final String mavenName = getProjectName(projectName, module, topLevelPackage);
		if (StringUtils.hasText(mavenName)) {
			// If the user wants this element in the traditional place, ensure
			// the template already contains it
			DomUtils.createChildIfNotExists("name", root, pom).setTextContent(mavenName.trim());
		} else {
			DomUtils.removeElements("name", root);
		}

		// parent and groupId
		setGroupIdAndParent(getGroupId(topLevelPackage), parentPom, root, pom);

		// artifactId
		final String artifactId = getArtifactId(projectName, module, topLevelPackage);
		Assert.hasText(artifactId, "Maven artifactIds cannot be blank");
		DomUtils.createChildIfNotExists("artifactId", root, pom).setTextContent(artifactId.trim());

		// packaging
		DomUtils.createChildIfNotExists("packaging", root, pom).setTextContent(this.name);

		// Java versions
		final List<Element> versionElements = XmlUtils.findElements("//*[.='" + JAVA_VERSION_PLACEHOLDER + "']", root);
		for (final Element versionElement : versionElements) {
			versionElement.setTextContent(javaVersion);
		}

		// Write the new POM to disk
		final String pomPath = pathResolver.getIdentifier(Path.ROOT.contextualize(module), "pom.xml");
		fileManager.createOrUpdateTextFileIfRequired(pomPath, XmlUtils.nodeToString(pom), true);
		return pomPath;
	}

	/**
	 * Returns the groupId of the project or module being created.
	 *
	 * This implementation simply uses the fully-qualified name of the given
	 * Java package. Subclasses can override this method to use a different
	 * strategy.
	 *
	 * @param topLevelPackage the new project or module's top-level Java package (required)
	 * @return
	 */
	protected String getGroupId(final JavaPackage topLevelPackage) {
		return topLevelPackage.getFullyQualifiedPackageName();
	}

	/**
	 * Returns the text to be inserted into the POM's <code>&lt;name&gt;</code> element.
	 *
	 * This implementation uses the given project name if not blank, otherwise
	 * the last element of the given Java package. Subclasses can override this
	 * method to use a different strategy.
	 *
	 * @param nullableProjectName the project name entered by the user (can be blank)
	 * @param module the name of the module being created (blank for the root module)
	 * @param topLevelPackage the project or module's top level Java package (required)
	 *
	 * @return a blank name if none is required
	 */
	protected String getProjectName(final String nullableProjectName, final String module, final JavaPackage topLevelPackage) {
		return StringUtils.defaultIfEmpty(nullableProjectName, module, topLevelPackage.getLastElement());
	}

	/**
	 * Returns the text to be inserted into the POM's <code>&lt;artifactId&gt;</code> element.
	 *
	 * This implementation simply delegates to {@link #getProjectName}.
	 * Subclasses can override this method to use a different strategy.
	 *
	 * @param nullableProjectName the project name entered by the user (can be blank)
	 * @param module the name of the module being created (blank for the root module)
	 * @param topLevelPackage the project or module's top level Java package (required)
	 *
	 * @return a non-blank artifactId
	 */
	protected String getArtifactId(final String nullableProjectName, final String module, final JavaPackage topLevelPackage) {
		return getProjectName(nullableProjectName, module, topLevelPackage);
	}

	/**
	 * Sets the Maven groupIds of the parent and/or project as necessary
	 *
	 * @param projectGroupId the project's groupId (required)
	 * @param parentPom the Maven coordinates of the parent POM (can be <code>null</code>)
	 * @param root the root element of the POM document (required)
	 * @param pom the POM document (required)
	 */
	protected void setGroupIdAndParent(final String projectGroupId, final GAV parentPom, final Element root, final Document pom) {
		final Element parentPomElement = DomUtils.createChildIfNotExists("parent", root, pom);
		final Element projectGroupIdElement = DomUtils.createChildIfNotExists("groupId", root, pom);
		if (parentPom == null) {
			// No parent POM was specified; remove the parent element
			root.removeChild(parentPomElement);
			DomUtils.removeTextNodes(root);
			projectGroupIdElement.setTextContent(projectGroupId);
		} else {
			// Parent groupId, artifactId, and version
			DomUtils.createChildIfNotExists("groupId", parentPomElement, pom).setTextContent(parentPom.getGroupId());
			DomUtils.createChildIfNotExists("artifactId", parentPomElement, pom).setTextContent(parentPom.getArtifactId());
			DomUtils.createChildIfNotExists("version", parentPomElement, pom).setTextContent(parentPom.getVersion());

			// Project groupId (if necessary)
			if (projectGroupId.equals(parentPom.getGroupId())) {
				// Maven best practice is to inherit the groupId from the parent
				root.removeChild(projectGroupIdElement);
				DomUtils.removeTextNodes(root);
			} else {
				// Project has its own groupId => needs to be explicit
				projectGroupIdElement.setTextContent(projectGroupId);
			}
		}
	}

	/**
	 * Subclasses can override this method to create any other required files
	 * or directories (apart from the POM, which has previously been generated
	 * by {@link #createPom}).
	 * <p>
	 * This implementation sets up the Log4j configuration file for the root module.
	 * @param topLevelPackage 
	 * 
	 * @param module 
	 */
	protected void createOtherArtifacts(final JavaPackage topLevelPackage, final String module) {
		if (StringUtils.isBlank(module)) {
			setUpLog4jConfiguration();
		}
	}
	
	private void setUpLog4jConfiguration() {
		final String log4jConfigFile = pathResolver.getFocusedIdentifier(Path.SRC_MAIN_RESOURCES, "log4j.properties");
		final InputStream template = FileUtils.getInputStream(getClass(), "log4j.properties-template");
		try {
			FileCopyUtils.copy(template, fileManager.createFile(log4jConfigFile).getOutputStream());
		} catch (final IOException e) {
			LOGGER.warning("Unable to install log4j logging configuration");
		}
	}

	/**
	 * Returns the package-relative path to this {@link PackagingProvider}'s POM template.
	 *
	 * @return a non-blank path
	 */
	String getPomTemplate() {
		return pomTemplate;
	}
}
