package org.springframework.roo.project.providers.maven;

import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.packaging.PackagingProvider;
import org.springframework.roo.project.providers.ProjectManagerProvider;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * ProjectManager provider based on Maven.
 * 
 * This provider is only available on projects which uses Maven dependency
 * management.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */

@Component
@Service
public class MavenProjectManagerProvider implements ProjectManagerProvider {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(MavenProjectManagerProvider.class);
	
	private static String PROVIDER_NAME = "MAVEN";
    private static String PROVIDER_DESCRIPTION = "ProjectManager provider based on Maven dependency manager";

    // ------------ OSGi component attributes ----------------
   	private BundleContext context;
   	
   	private FileManager fileManager;
   	private PathResolver pathResolver;
   	
   	protected void activate(final ComponentContext context) {
    	this.context = context.getBundleContext();
    }
    
	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public boolean isActive() {
		// Checking if pom.xml file exists
		final String pomPath =  getPathResolver().getRoot().concat("/pom.xml");
		
		if(getFileManager().exists(pomPath)){
			return true;
		}

		return false;
	}

	@Override
	public String getName() {
		return PROVIDER_NAME;
	}

	@Override
	public String getDescription() {
		return PROVIDER_DESCRIPTION;
	}

	@Override
	public void createProject(JavaPackage topLevelPackage, String projectName,
            Integer majorJavaVersion,
            PackagingProvider packagingType){
		final String pomPath = createPom(topLevelPackage, projectName,
                getJavaVersion(majorJavaVersion), packagingType.getId());
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
     * <li>replaces all occurrences of {@link #JAVA_VERSION_PLACEHOLDER} with
     * the given Java version</li>
     * </ul>
     * This method makes as few assumptions about the POM template as possible,
     * to make life easier for anyone writing a {@link PackagingProvider}.
     * 
     * @param topLevelPackage the new project or module's top-level Java package
     *            (required)
     * @param projectName the project name provided by the user (can be blank)
     * @param javaVersion the Java version to substitute into the POM (required)
     * @param parentPom the Maven coordinates of the parent POM (can be
     *            <code>null</code>)
     * @param module the unqualified name of the Maven module to which the new
     *            POM belongs
     * @param projectService cannot be injected otherwise it's a circular
     *            dependency
     * @return the path of the newly created POM
     */
    protected String createPom(final JavaPackage topLevelPackage,
            final String projectName, final String javaVersion,
            final String packagingType) {
        Validate.notBlank(javaVersion, "Java version required");
        Validate.notNull(topLevelPackage, "Top level package required");

		// Load the pom template
		final InputStream templateInputStream = FileUtils.getInputStream(getClass(),
				String.format("%s-pom-template.xml", packagingType));

		final Document pom = XmlUtils.readXml(templateInputStream);
		final Element root = pom.getDocumentElement();
		
		Element groupIdElement = (Element) root.getElementsByTagName("groupId").item(0);
		groupIdElement.setTextContent(getGroupId(topLevelPackage));
		
		Element artifactIdElement = (Element) root.getElementsByTagName("artifactId").item(0);
		artifactIdElement.setTextContent(getProjectName(projectName, "", topLevelPackage));
		
		Element nameElement = (Element) root.getElementsByTagName("name").item(0);
		nameElement.setTextContent(getProjectName(projectName, "", topLevelPackage));
		
		final List<Element> versionElements = XmlUtils.findElements("//*[.='JAVA_VERSION']", root);
		for (final Element versionElement : versionElements) {
			versionElement.setTextContent(javaVersion);
		}

		final MutableFile pomMutableFile = getFileManager().createFile(getPathResolver().getRoot() + "/pom.xml");

		XmlUtils.writeXml(pomMutableFile.getOutputStream(), pom);
		
        return pomMutableFile.getCanonicalPath();
    }
    
    /**
     * Returns the text to be inserted into the POM's <code>&lt;name&gt;</code>
     * element. This implementation uses the given project name if not blank,
     * otherwise the last element of the given Java package. Subclasses can
     * override this method to use a different strategy.
     * 
     * @param nullableProjectName the project name entered by the user (can be
     *            blank)
     * @param module the name of the module being created (blank for the root
     *            module)
     * @param topLevelPackage the project or module's top level Java package
     *            (required)
     * @return a blank name if none is required
     */
    protected String getProjectName(final String nullableProjectName,
            final String module, final JavaPackage topLevelPackage) {
        String packageName = StringUtils.defaultIfEmpty(nullableProjectName,
                module);
        return StringUtils.defaultIfEmpty(packageName,
                topLevelPackage.getLastElement());
    }
    
    /**
     * Returns the groupId of the project or module being created. This
     * implementation simply uses the fully-qualified name of the given Java
     * package. Subclasses can override this method to use a different strategy.
     * 
     * @param topLevelPackage the new project or module's top-level Java package
     *            (required)
     * @return
     */
    protected String getGroupId(final JavaPackage topLevelPackage) {
        return topLevelPackage.getFullyQualifiedPackageName();
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
    
    public FileManager getFileManager(){
    	if(fileManager == null){
    		// Get all Services implement FileManager interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(FileManager.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				fileManager = (FileManager) this.context.getService(ref);
    				return fileManager;
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load FileManager on MavenProjectManagerProvider.");
    			return null;
    		}
    	}else{
    		return fileManager;
    	}
    }
    
    public PathResolver getPathResolver(){
    	if(pathResolver == null){
    		// Get all Services implement PathResolver interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(PathResolver.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				pathResolver = (PathResolver) this.context.getService(ref);
    				return pathResolver;
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load PathResolver on MavenProjectManagerProvider.");
    			return null;
    		}
    	}else{
    		return pathResolver;
    	}
    }
}
