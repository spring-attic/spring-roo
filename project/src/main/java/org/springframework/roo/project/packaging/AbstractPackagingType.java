package org.springframework.roo.project.packaging;

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
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Convenient superclass for implementing {@link PackagingType}.
 * 
 * Uses the "Template Method" GoF pattern.
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component(componentAbstract = true)
public abstract class AbstractPackagingType implements PackagingType {

	// Constants
	protected static final Logger LOGGER = HandlerUtils.getLogger(PackagingType.class);
	private static final String JAVA_VERSION_PLACEHOLDER = "JAVA_VERSION";
	
	// Fields
	@Reference protected ApplicationContextOperations applicationContextOperations;
	@Reference protected FileManager fileManager;
	@Reference protected PathResolver pathResolver;
	@Reference protected ProjectOperations projectOperations;
	
	private final String name;
	private final String pomTemplate;
	
	/**
	 * Constructor
	 *
	 * @param name the name of this type of packaging as used in the POM (required)
	 * @param pomTemplate the path of this packaging type's POM template,
	 * relative to its own package, as per {@link Class#getResourceAsStream(String)};
	 * this template should contain a "parent" element with its own groupId,
	 * artifactId, and version elements; this parent element will be removed if
	 * not required
	 */
	protected AbstractPackagingType(String name, String pomTemplate) {
		Assert.hasText(name, "Name is required");
		Assert.hasText(pomTemplate, "POM template path is required");
		this.name = name;
		this.pomTemplate = pomTemplate;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void createArtifacts(final JavaPackage topLevelPackage, final String nullableProjectName, final String javaVersion, final GAV parentPom) {
		createPom(topLevelPackage, nullableProjectName, javaVersion, parentPom);
		fileManager.scan();	// TODO not sure why or if this is necessary; find out and document/remove it
		createOtherArtifacts();
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
	 * to make life easier for authors of new {@link PackagingType}s.
	 * 
	 * @param topLevelPackage the new project or module's top-level Java package (required)
	 * @param nullableProjectName the project name provided by the user (can be blank)
	 * @param javaVersion the Java version to substitute into the POM (required)
	 * @param parentPom the Maven coordinates of the parent POM (can be <code>null</code>)
	 */
	protected void createPom(final JavaPackage topLevelPackage, final String nullableProjectName, final String javaVersion, final GAV parentPom) {
		Assert.hasText(javaVersion, "Java version required");
		Assert.notNull(topLevelPackage, "Top level package required");
		
		// Read the POM template from the classpath
		final Document pom = XmlUtils.readXml(TemplateUtils.getTemplate(getClass(), this.pomTemplate));
		final Element root = pom.getDocumentElement();

		// name
		final String projectName = getProjectName(nullableProjectName, topLevelPackage);
		if (StringUtils.hasText(projectName)) {
			DomUtils.createChildIfNotExists("name", root, pom).setTextContent(projectName.trim());
		}
		
		// parent and groupId
		setGroupIdAndParent(getGroupId(topLevelPackage), parentPom, root, pom);
		
		// artifactId
		final String artifactId = getArtifactId(nullableProjectName, topLevelPackage);
		Assert.hasText(artifactId, "Maven artifactIds cannot be blank");
		DomUtils.createChildIfNotExists("artifactId", root, pom).setTextContent(artifactId.trim());
		
		// packaging
		DomUtils.createChildIfNotExists("packaging", root, pom).setTextContent(getName());
		
		// Java versions
		final List<Element> versionElements = XmlUtils.findElements("//*[.='" + JAVA_VERSION_PLACEHOLDER + "']", root);
		for (final Element versionElement : versionElements) {
			versionElement.setTextContent(javaVersion);
		}

		// Write the new POM to disk
		fileManager.createOrUpdateTextFileIfRequired(pathResolver.getIdentifier(Path.ROOT, "pom.xml"), XmlUtils.nodeToString(pom), true);
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
	 * @param topLevelPackage the project or module's top level Java package (required)
	 * 
	 * @return a blank name if none is required
	 */
	protected String getProjectName(final String nullableProjectName, final JavaPackage topLevelPackage) {
		return StringUtils.defaultIfEmpty(nullableProjectName, topLevelPackage.getLastElement());
	}
	
	/**
	 * Returns the text to be inserted into the POM's <code>&lt;artifactId&gt;</code> element.
	 * 
	 * This implementation simply delegates to {@link #getProjectName}.
	 * Subclasses can override this method to use a different strategy.
	 * 
	 * @param nullableProjectName the project name entered by the user (can be blank)
	 * @param topLevelPackage the project or module's top level Java package (required)
	 * 
	 * @return a non-blank artifactId
	 */
	protected String getArtifactId(final String nullableProjectName, final JavaPackage topLevelPackage) {
		return getProjectName(nullableProjectName, topLevelPackage);
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
	 * by {@link #createPom}). This implementation does nothing.
	 */
	protected void createOtherArtifacts() {}
	
	/**
	 * Returns the package-relative path to this {@link PackagingType}'s POM template.
	 * 
	 * @return a non-blank path
	 */
	String getPomTemplate() {
		return pomTemplate;
	}
}
