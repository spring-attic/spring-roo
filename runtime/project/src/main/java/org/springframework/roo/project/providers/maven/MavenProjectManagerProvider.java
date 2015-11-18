package org.springframework.roo.project.providers.maven;

import static org.springframework.roo.project.DependencyScope.COMPILE;
import static org.springframework.roo.support.util.AnsiEscapeCode.FG_CYAN;
import static org.springframework.roo.support.util.AnsiEscapeCode.decorate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.monitor.FileMonitorService;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.DependencyScope;
import org.springframework.roo.project.DependencyType;
import org.springframework.roo.project.Feature;
import org.springframework.roo.project.Filter;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectType;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.Repository;
import org.springframework.roo.project.Resource;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.project.maven.PomFactory;
import org.springframework.roo.project.packaging.PackagingProvider;
import org.springframework.roo.project.providers.ProjectManagerProvider;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.CollectionUtils;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
	
    static final String ADDED = "added";
    static final String CHANGED = "changed";
    static final String REMOVED = "removed";
    static final String SKIPPED = "skipped";
    static final String UPDATED = "updated";
    
    private static final String SEPARATOR = File.separator;
    private static final String DEFAULT_POM_NAME = "pom.xml";
    private static final String DEFAULT_RELATIVE_PATH = ".." + SEPARATOR
            + DEFAULT_POM_NAME;
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(MavenProjectManagerProvider.class);
	
	private static String PROVIDER_NAME = "MAVEN";
    private static String PROVIDER_DESCRIPTION = "ProjectManager provider based on Maven dependency manager";

    // ------------ OSGi component attributes ----------------
   	private BundleContext context;
   	
   	private FileManager fileManager;
   	private PathResolver pathResolver;
    private MetadataService metadataService;
    private Shell shell;
    private FileMonitorService fileMonitorService;
    private PomFactory pomFactory;
    private MetadataDependencyRegistry metadataDependencyRegistry;
    
    private final Map<String, Pom> pomMap = new HashMap<String, Pom>();
    private final Set<String> toBeParsed = new HashSet<String>();
    private String projectRootDirectory;
   	
   	protected void activate(final ComponentContext context) {
    	this.context = context.getBundleContext();
    	final File projectDirectory = new File(StringUtils.defaultIfEmpty(
                OSGiUtils.getRooWorkingDirectory(context),
                FileUtils.CURRENT_DIRECTORY));
    	projectRootDirectory = FileUtils.getCanonicalPath(projectDirectory);
    }
    
	@Override
	public void createProject(JavaPackage topLevelPackage, String projectName,
            Integer majorJavaVersion,
            PackagingProvider packagingType){
		final String pomPath = createPom(topLevelPackage, projectName,
                getJavaVersion(majorJavaVersion), packagingType.getId());
    }
	
	@Override
	public void addBuildPlugin(String moduleName, Plugin plugin) {
		addBuildPlugins(moduleName, Collections.singletonList(plugin));
	}
	
	@Override
    public void addBuildPlugins(final String moduleName,
            final Collection<? extends Plugin> newPlugins) {
        if (CollectionUtils.isEmpty(newPlugins)) {
            return;
        }
        final Pom pom = getPomFromModuleName(moduleName);
        Validate.notNull(pom,
                "The pom is not available, so plugin addition cannot be performed");

        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(pom.getPath()));
        final Element root = document.getDocumentElement();
        final Element pluginsElement = DomUtils.createChildIfNotExists(
                "/project/build/plugins", root, document);
        final List<Element> existingPluginElements = XmlUtils.findElements(
                "plugin", pluginsElement);

        final List<String> addedPlugins = new ArrayList<String>();
        final List<String> removedPlugins = new ArrayList<String>();
        for (final Plugin newPlugin : newPlugins) {
            if (newPlugin != null) {

                // Look for any existing instances of this plugin
                boolean inserted = false;
                for (final Element existingPluginElement : existingPluginElements) {
                    final Plugin existingPlugin = new Plugin(
                            existingPluginElement);
                    if (existingPlugin.hasSameCoordinates(newPlugin)) {
                        // It's the same artifact, but might have a different
                        // version, exclusions, etc.
                        if (!inserted) {
                            // We haven't added the new one yet; do so now
                            pluginsElement.insertBefore(
                                    newPlugin.getElement(document),
                                    existingPluginElement);
                            inserted = true;
                            if (!newPlugin.getVersion().equals(
                                    existingPlugin.getVersion())) {
                                // It's a genuine version change => mention the
                                // old and new versions in the message
                                addedPlugins.add(newPlugin
                                        .getSimpleDescription());
                                removedPlugins.add(existingPlugin
                                        .getSimpleDescription());
                            }
                        }
                        // Either way, we remove the previous one in case it was
                        // different in any way
                        pluginsElement.removeChild(existingPluginElement);
                    }
                    // Keep looping in case it's present more than once
                }
                if (!inserted) {
                    // We didn't encounter any existing dependencies with the
                    // same coordinates; add it now
                    pluginsElement.appendChild(newPlugin.getElement(document));
                    addedPlugins.add(newPlugin.getSimpleDescription());
                }
            }
        }

        if (!newPlugins.isEmpty()) {
            final String message = getPomPluginsUpdateMessage(addedPlugins,
                    removedPlugins);
            fileManager.createOrUpdateTextFileIfRequired(pom.getPath(),
                    XmlUtils.nodeToString(document), message, false);
        }
    }
	

	
	@Override
	public List<Dependency> addDependencies(final String moduleName,
            final Collection<? extends Dependency> newDependencies) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Dependency modification prohibited; no such module '%s'",
                moduleName);
        final Pom pom = getPomFromModuleName(moduleName);
        Validate.notNull(pom,
                "The pom is not available, so dependencies cannot be added");

        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(pom.getPath()));
        final Element dependenciesElement = DomUtils.createChildIfNotExists(
                "dependencies", document.getDocumentElement(), document);
        final List<Element> existingDependencyElements = XmlUtils.findElements(
                "dependency", dependenciesElement);

        final List<Dependency> finalDependencies = new ArrayList<Dependency>();
        final List<String> addedDependencies = new ArrayList<String>();
        final List<String> removedDependencies = new ArrayList<String>();
        final List<String> skippedDependencies = new ArrayList<String>();
        for (final Dependency newDependency : newDependencies) {
        	// ROO-3465: Prevent version changes adding checkVersion to false
        	// when check if is possible to add the new dependency
            if (pom.canAddDependency(newDependency, false)) {
                // Look for any existing instances of this dependency
                boolean inserted = false;
                for (final Element existingDependencyElement : existingDependencyElements) {
                    final Dependency existingDependency = new Dependency(
                            existingDependencyElement);
                    if (existingDependency.hasSameCoordinates(newDependency)) {
                        // It's the same artifact, but might have a different
                        // version, exclusions, etc.
                        if (!inserted) {
                            // We haven't added the new one yet; do so now
                        	// ROO-3685: Check if current dependency has version when is added again
                        	Element newDependencyElement = removeVersionIfBlank(
                        			newDependency.getElement(document));
                            dependenciesElement.insertBefore(
                            		newDependencyElement,
                                    existingDependencyElement);
                            inserted = true;
                            if (!newDependency.getVersion().equals(
                                    existingDependency.getVersion())) {
                                // It's a genuine version change => mention the
                                // old and new versions in the message
                            	finalDependencies.add(newDependency);
                                addedDependencies.add(newDependency
                                        .getSimpleDescription());
                                removedDependencies.add(existingDependency
                                        .getSimpleDescription());
                            }
                        }
                        // Either way, we remove the previous one in case it was
                        // different in any way
                        dependenciesElement
                                .removeChild(existingDependencyElement);
                    }
                    // Keep looping in case it's present more than once
                }
                if (!inserted) {
					// We didn't encounter any existing dependencies with the
					// same coordinates; add it now

					// ROO-3660: Check if current dependency has version. If
					// not, remove version attribute
					Element newDependencyElement = removeVersionIfBlank(
							newDependency.getElement(document));
					dependenciesElement.appendChild(newDependencyElement);
					finalDependencies.add(newDependency);
					addedDependencies.add(newDependency.getSimpleDescription());
                }
            }
            else {
                skippedDependencies.add(newDependency.getSimpleDescription());
                finalDependencies.add(newDependency);
            }
        }
        if (!newDependencies.isEmpty() || !skippedDependencies.isEmpty()) {
            final String message = getPomDependenciesUpdateMessage(
                    addedDependencies, removedDependencies, skippedDependencies);
            fileManager.createOrUpdateTextFileIfRequired(pom.getPath(),
                    XmlUtils.nodeToString(document), message, false);
        }
        
        return finalDependencies;
    }
	
	@Override
	public Dependency addDependency(final String moduleName,
            final Dependency dependency) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Dependency modification prohibited at this time");
        Validate.notNull(dependency, "Dependency required");
        return addDependencies(moduleName, Collections.singletonList(dependency)).get(0);
    }

	@Override
    public final Dependency addDependency(final String moduleName,
            final String groupId, final String artifactId, final String version) {
        return addDependency(moduleName, groupId, artifactId, version, COMPILE);
    }

	@Override
    public final Dependency addDependency(final String moduleName,
            final String groupId, final String artifactId,
            final String version, final DependencyScope scope) {
        return addDependency(moduleName, groupId, artifactId, version, scope, "");
    }

	@Override
    public final Dependency addDependency(final String moduleName,
            final String groupId, final String artifactId,
            final String version, DependencyScope scope, final String classifier) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Dependency modification prohibited at this time");
        Validate.notNull(groupId, "Group ID required");
        Validate.notNull(artifactId, "Artifact ID required");
        Validate.notBlank(version, "Version required");
        if (scope == null) {
            scope = COMPILE;
        }
        final Dependency dependency = new Dependency(groupId, artifactId,
                version, DependencyType.JAR, scope, classifier);
        return addDependency(moduleName, dependency);
    }
	
	@Override
	public void addFilter(final String moduleName, final Filter filter) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Filter modification prohibited at this time");
        Validate.notNull(filter, "Filter required");
        final Pom pom = getPomFromModuleName(moduleName);
        Validate.notNull(pom,
                "The pom is not available, so filter addition cannot be performed");
        if (filter == null || pom.isFilterRegistered(filter)) {
            return;
        }

        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(pom.getPath()));
        final Element root = document.getDocumentElement();
        final String descriptionOfChange;
        final Element buildElement = XmlUtils.findFirstElement(
                "/project/build", root);
        final Element existingFilter = XmlUtils.findFirstElement(
                "filters/filter['" + filter.getValue() + "']", buildElement);
        if (existingFilter == null) {
            // No such filter; add it
            final Element filtersElement = DomUtils.createChildIfNotExists(
                    "filters", buildElement, document);
            filtersElement.appendChild(XmlUtils.createTextElement(document,
                    "filter", filter.getValue()));
            descriptionOfChange = highlight(ADDED + " filter") + " '"
                    + filter.getValue() + "'";
        }
        else {
            existingFilter.setTextContent(filter.getValue());
            descriptionOfChange = highlight(UPDATED + " filter") + " '"
                    + filter.getValue() + "'";
        }

        fileManager.createOrUpdateTextFileIfRequired(pom.getPath(),
                XmlUtils.nodeToString(document), descriptionOfChange, false);
    }
	
	@Override
	public void addModuleDependency(final String moduleToDependUpon) {
        if (StringUtils.isBlank(moduleToDependUpon)) {
            return; // No need to ever add a dependency upon the root POM
        }
        final Pom focusedModule = getFocusedModule();
        if (focusedModule != null
                && StringUtils.isNotBlank(focusedModule.getModuleName())
                && !moduleToDependUpon.equals(focusedModule.getModuleName())) {
            final ProjectMetadata dependencyProject = getProjectMetadata(moduleToDependUpon);
            if (dependencyProject != null) {
                final Pom dependencyPom = dependencyProject.getPom();
                if (!dependencyPom.getPath().equals(focusedModule.getPath())) {
                    final Dependency dependency = dependencyPom
                            .asDependency(COMPILE);
                    if (!focusedModule
                            .hasDependencyExcludingVersion(dependency)) {
                        addDependency(focusedModule.getModuleName(), dependency);
                        detectCircularDependency(focusedModule, dependencyPom);
                    }
                }
            }
        }
    }
	
	@Override
    public Pom getFocusedModule() {
        final ProjectMetadata focusedProjectMetadata = getFocusedProjectMetadata();
        if (focusedProjectMetadata == null) {
            return null;
        }
        return focusedProjectMetadata.getPom();
    }
	
    @Override
    public void addPluginRepositories(final String moduleName,
            final Collection<? extends Repository> repositories) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Plugin repository modification prohibited at this time");
        Validate.notNull(repositories, "Plugin repositories required");
        addRepositories(moduleName, repositories, "pluginRepositories",
                "pluginRepository");
    }
    
    @Override
    public void addPluginRepository(final String moduleName,
            final Repository repository) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Plugin repository modification prohibited at this time");
        Validate.notNull(repository, "Repository required");
        addRepository(moduleName, repository, "pluginRepositories",
                "pluginRepository");
    }
    
    @Override
    public void addProperty(final String moduleName, final Property property) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Property modification prohibited at this time");
        Validate.notNull(property, "Property to add required");
        final Pom pom = getPomFromModuleName(moduleName);
        Validate.notNull(pom,
                "The pom is not available, so property addition cannot be performed");
        if (pom.isPropertyRegistered(property)) {
            return;
        }

        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(pom.getPath()));
        final Element root = document.getDocumentElement();
        final String descriptionOfChange;
        final Element existing = XmlUtils.findFirstElement(
                "/project/properties/" + property.getName(), root);
        if (existing == null) {
            // No existing property of this name; add it
            final Element properties = DomUtils.createChildIfNotExists(
                    "properties", document.getDocumentElement(), document);
            properties.appendChild(XmlUtils.createTextElement(document,
                    property.getName(), property.getValue()));
            descriptionOfChange = highlight(ADDED + " property") + " '"
                    + property.getName() + "' = '" + property.getValue() + "'";
        }
        else {
            // A property of this name exists; update it
            existing.setTextContent(property.getValue());
            descriptionOfChange = highlight(UPDATED + " property") + " '"
                    + property.getName() + "' to '" + property.getValue() + "'";
        }

        fileManager.createOrUpdateTextFileIfRequired(pom.getPath(),
                XmlUtils.nodeToString(document), descriptionOfChange, false);
    }
    
    @Override
    public void addRepositories(final String moduleName,
            final Collection<? extends Repository> repositories) {
        addRepositories(moduleName, repositories, "repositories", "repository");
    }
    
    @Override
    public void addRepository(final String moduleName,
            final Repository repository) {
        addRepository(moduleName, repository, "repositories", "repository");
    }
    
    @Override
    public void addResource(final String moduleName, final Resource resource) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Resource modification prohibited at this time");
        Validate.notNull(resource, "Resource to add required");
        final Pom pom = getPomFromModuleName(moduleName);
        Validate.notNull(pom,
                "The pom is not available, so resource addition cannot be performed");
        if (pom.isResourceRegistered(resource)) {
            return;
        }

        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(pom.getPath()));
        final Element buildElement = XmlUtils.findFirstElement(
                "/project/build", document.getDocumentElement());
        final Element resourcesElement = DomUtils.createChildIfNotExists(
                "resources", buildElement, document);
        resourcesElement.appendChild(resource.getElement(document));
        final String descriptionOfChange = highlight(ADDED + " resource") + " "
                + resource.getSimpleDescription();

        fileManager.createOrUpdateTextFileIfRequired(pom.getPath(),
                XmlUtils.nodeToString(document), descriptionOfChange, false);
    }
    
    @Override
    public String getFocusedModuleName() {
    	if (getFocusedModule() == null) {
            return "";
        }
        return getFocusedModule().getModuleName();
    }
    
    @Override
    public ProjectMetadata getFocusedProjectMetadata() {
        return getProjectMetadata(getFocusedModuleName());
    }
    
    @Override
    public String getFocusedProjectName() {
        return getProjectName(getFocusedModuleName());
    }
    
    @Override
    public JavaPackage getFocusedTopLevelPackage() {
        return getTopLevelPackage(getFocusedModuleName());
    }

    @Override
    public Pom getModuleForFileIdentifier(final String fileIdentifier) {
    	 updatePomCache();
         String startingPoint = FileUtils.getFirstDirectory(fileIdentifier);
         String pomPath = FileUtils.ensureTrailingSeparator(startingPoint)
                 + DEFAULT_POM_NAME;
         File pom = new File(pomPath);
         while (!pom.exists()) {
             if (startingPoint.equals(SEPARATOR)) {
                 break;
             }
             startingPoint = StringUtils.removeEnd(startingPoint, SEPARATOR);
             
             if (startingPoint.lastIndexOf(SEPARATOR) < 0) {
             	break;
             }
             startingPoint = startingPoint.substring(0,
                     startingPoint.lastIndexOf(SEPARATOR));
             startingPoint = StringUtils.removeEnd(startingPoint, SEPARATOR);

             pomPath = FileUtils.ensureTrailingSeparator(startingPoint)
                     + DEFAULT_POM_NAME;
             pom = new File(pomPath);
         }
         return getPomFromPath(pomPath);
    }
    
    @Override
    public Collection<String> getModuleNames() {
        final Set<String> moduleNames = new HashSet<String>();
        for (final Pom module : pomMap.values()) {
            moduleNames.add(module.getModuleName());
        }
        return moduleNames;
    }
    
    @Override
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
    
	@Override
    public final Pom getPomFromModuleName(final String moduleName) {
        final ProjectMetadata projectMetadata = getProjectMetadata(moduleName);
        return projectMetadata == null ? null : projectMetadata.getPom();
    }

    @Override
    public Collection<Pom> getPoms() {
    	updatePomCache();
        return new ArrayList<Pom>(pomMap.values());
    }
    
	@Override
	public final ProjectMetadata getProjectMetadata(final String moduleName) {
		return (ProjectMetadata) getMetadataService().get(ProjectMetadata
				.getProjectIdentifier(moduleName));
	}
	
	@Override
    public String getProjectName(final String moduleName) {
        final Pom pom = getPomFromModuleName(moduleName);
        Validate.notNull(pom, "A pom with module name '%s' could not be found",
                moduleName);
        return pom.getDisplayName();
    }
	
    @Override
    public JavaPackage getTopLevelPackage(final String moduleName) {
        final Pom pom = getPomFromModuleName(moduleName);
        if (pom != null) {
            return new JavaPackage(pom.getGroupId());
        }
        return null;
    }
    
    @Override
    public boolean isFeatureInstalled(final String featureName, final Map<String, Feature> features) {
        final Feature feature = features.get(featureName);
        if (feature == null) {
            return false;
        }
        for (final String moduleName : getModuleNames()) {
            if (feature.isInstalledInModule(moduleName)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean isFeatureInstalledInModule(String featureName,
            String moduleName, final Map<String, Feature> features) {
        final Feature feature = features.get(featureName);
        if (feature == null) {
            return false;
        }
        if (feature.isInstalledInModule(moduleName)) {
            return true;
        }
        return false;
    }
    
    
    @Override
    public boolean isFeatureInstalledInFocusedModule(Map<String, Feature> features,
            final String... featureNames) {
        for (final String featureName : featureNames) {
            final Feature feature = features.get(featureName);
            if (feature != null
                    && feature.isInstalledInModule(getFocusedModuleName())) {
                return true;
            }
        }
        return false;
    }
    
	@Override
    public boolean isFocusedProjectAvailable() {
        return isProjectAvailable(getFocusedModuleName());
    }
	
	@Override
    public boolean isModuleCreationAllowed() {
        return isProjectAvailable("");
    }
	
	@Override
    public boolean isModuleFocusAllowed() {
        return getModuleNames().size() > 1;
    }
    
	@Override
    public final boolean isProjectAvailable(final String moduleName) {
        return getProjectMetadata(moduleName) != null;
    }
	
    @Override
    public void removeBuildPlugin(final String moduleName, final Plugin plugin) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Plugin modification prohibited at this time");
        Validate.notNull(plugin, "Plugin required");
        removeBuildPlugins(moduleName, Collections.singletonList(plugin));
    }
    
    @Override
    public void removeBuildPluginImmediately(final String moduleName,
            final Plugin plugin) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Plugin modification prohibited at this time");
        Validate.notNull(plugin, "Plugin required");
        removeBuildPlugins(moduleName, Collections.singletonList(plugin), true);
    }
    
    @Override
    public void removeBuildPlugins(final String moduleName,
            final Collection<? extends Plugin> plugins) {
        removeBuildPlugins(moduleName, plugins, false);
    }
    
    @Override
    public void removeDependencies(final String moduleName,
            final Collection<? extends Dependency> dependenciesToRemove) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Dependency modification prohibited at this time");
        Validate.notNull(dependenciesToRemove, "Dependencies required");
        if (CollectionUtils.isEmpty(dependenciesToRemove)) {
            return;
        }
        final Pom pom = getPomFromModuleName(moduleName);
        Validate.notNull(pom,
                "The pom is not available, so dependency removal cannot be performed");
        if (!pom.isAnyDependenciesRegistered(dependenciesToRemove)) {
            return;
        }

        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(pom.getPath()));
        final Element root = document.getDocumentElement();
        final Element dependenciesElement = XmlUtils.findFirstElement(
                "/project/dependencies", root);
        if (dependenciesElement == null) {
            return;
        }

        final List<Element> existingDependencyElements = XmlUtils.findElements(
                "dependency", dependenciesElement);
        final List<String> removedDependencies = new ArrayList<String>();
        for (final Dependency dependencyToRemove : dependenciesToRemove) {
            if (pom.isDependencyRegistered(dependencyToRemove, false)) {
                for (final Iterator<Element> iter = existingDependencyElements
                        .iterator(); iter.hasNext();) {
                    final Element candidate = iter.next();
                    final Dependency candidateDependency = new Dependency(
                            candidate);
                    if (candidateDependency.equals(dependencyToRemove)) {
                        // It's the same dependency; remove it
                        dependenciesElement.removeChild(candidate);
                        // Ensure we don't try to remove it again for another
                        // Dependency
                        iter.remove();
                        removedDependencies.add(candidateDependency
                                .getSimpleDescription());
                    }
                    // Keep looping in case it's in the POM more than once
                }
            }
        }
        if (removedDependencies.isEmpty()) {
            return;
        }
        DomUtils.removeTextNodes(dependenciesElement);
        final String message = getDescriptionOfChange(REMOVED,
                removedDependencies, "dependency", "dependencies");

        fileManager.createOrUpdateTextFileIfRequired(pom.getPath(),
                XmlUtils.nodeToString(document), message, false);
    }

    @Override
    public void removeDependency(final String moduleName,
            final Dependency dependency) {
        removeDependency(moduleName, dependency, "/project/dependencies",
                "/project/dependencies/dependency");
    }

 
    @Override
    public final void removeDependency(final String moduleName,
            final String groupId, final String artifactId, final String version) {
        removeDependency(moduleName, groupId, artifactId, version, "");
    }

    @Override
    public final void removeDependency(final String moduleName,
            final String groupId, final String artifactId,
            final String version, final String classifier) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Dependency modification prohibited at this time");
        Validate.notNull(groupId, "Group ID required");
        Validate.notNull(artifactId, "Artifact ID required");
        Validate.notBlank(version, "Version required");
        final Dependency dependency = new Dependency(groupId, artifactId,
                version, DependencyType.JAR, COMPILE, classifier);
        removeDependency(moduleName, dependency);
    }
    
    @Override
    public void removeFilter(final String moduleName, final Filter filter) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Filter modification prohibited at this time");
        Validate.notNull(filter, "Filter required");
        final Pom pom = getPomFromModuleName(moduleName);
        Validate.notNull(pom,
                "The pom is not available, so filter removal cannot be performed");
        if (filter == null || !pom.isFilterRegistered(filter)) {
            return;
        }

        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(pom.getPath()));
        final Element root = document.getDocumentElement();

        final Element filtersElement = XmlUtils.findFirstElement(
                "/project/build/filters", root);
        if (filtersElement == null) {
            return;
        }

        String descriptionOfChange = "";
        for (final Element candidate : XmlUtils.findElements("filter",
                filtersElement)) {
            if (filter.equals(new Filter(candidate))) {
                filtersElement.removeChild(candidate);
                descriptionOfChange = highlight(REMOVED + " filter") + " '"
                        + filter.getValue() + "'";
                // We will not break the loop (even though we could
                // theoretically), just in case it was in the POM more than once
            }
        }

        final List<Element> filterElements = XmlUtils.findElements("filter",
                filtersElement);
        if (filterElements.isEmpty()) {
            filtersElement.getParentNode().removeChild(filtersElement);
        }

        DomUtils.removeTextNodes(root);

        fileManager.createOrUpdateTextFileIfRequired(pom.getPath(),
                XmlUtils.nodeToString(document), descriptionOfChange, false);
    }
    
    
    @Override
    public void removePluginRepository(final String moduleName,
            final Repository repository) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Plugin repository modification prohibited at this time");
        Validate.notNull(repository, "Repository required");
        removeRepository(moduleName, repository,
                "/project/pluginRepositories/pluginRepository");
    }
    
    @Override
    public void removeProperty(final String moduleName, final Property property) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Property modification prohibited at this time");
        Validate.notNull(property, "Property to remove required");
        final Pom pom = getPomFromModuleName(moduleName);
        Validate.notNull(pom,
                "The pom is not available, so property removal cannot be performed");
        if (!pom.isPropertyRegistered(property)) {
            return;
        }

        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(pom.getPath()));
        final Element root = document.getDocumentElement();
        final Element propertiesElement = XmlUtils.findFirstElement(
                "/project/properties", root);
        String descriptionOfChange = "";
        for (final Element candidate : XmlUtils.findElements(
                "/project/properties/*", document.getDocumentElement())) {
            if (property.equals(new Property(candidate))) {
                propertiesElement.removeChild(candidate);
                descriptionOfChange = highlight(REMOVED + " property") + " "
                        + property.getName();
                // Stay in the loop just in case it was in the POM more than
                // once
            }
        }

        DomUtils.removeTextNodes(propertiesElement);

        fileManager.createOrUpdateTextFileIfRequired(pom.getPath(),
                XmlUtils.nodeToString(document), descriptionOfChange, false);
    }
    
    
    @Override
    public void removeRepository(final String moduleName,
            final Repository repository) {
        removeRepository(moduleName, repository,
                "/project/repositories/repository");
    }
    
    @Override
    public void removeResource(final String moduleName, final Resource resource) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Resource modification prohibited at this time");
        Validate.notNull(resource, "Resource required");
        final Pom pom = getPomFromModuleName(moduleName);
        Validate.notNull(pom,
                "The pom is not available, so resource removal cannot be performed");
        if (!pom.isResourceRegistered(resource)) {
            return;
        }

        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(pom.getPath()));
        final Element root = document.getDocumentElement();
        final Element resourcesElement = XmlUtils.findFirstElement(
                "/project/build/resources", root);
        if (resourcesElement == null) {
            return;
        }
        String descriptionOfChange = "";
        for (final Element candidate : XmlUtils.findElements(
                "resource[directory = '" + resource.getDirectory() + "']",
                resourcesElement)) {
            if (resource.equals(new Resource(candidate))) {
                resourcesElement.removeChild(candidate);
                descriptionOfChange = highlight(REMOVED + " resource") + " "
                        + resource.getSimpleDescription();
                // Stay in the loop just in case it was in the POM more than
                // once
            }
        }

        final List<Element> resourceElements = XmlUtils.findElements(
                "resource", resourcesElement);
        if (resourceElements.isEmpty()) {
            resourcesElement.getParentNode().removeChild(resourcesElement);
        }

        DomUtils.removeTextNodes(root);

        fileManager.createOrUpdateTextFileIfRequired(pom.getPath(),
                XmlUtils.nodeToString(document), descriptionOfChange, false);
    }
    
    @Override
    public void setModule(final Pom module) {
        // Update window title with project name
        getShell().flash(Level.FINE,
                "Spring Roo: " + getTopLevelPackage(module.getModuleName()),
                Shell.WINDOW_TITLE_SLOT);
        getShell().setPromptPath(module.getModuleName());
    }
    
    @Override
    public void updateBuildPlugin(final String moduleName, final Plugin plugin) {
        final Pom pom = getPomFromModuleName(moduleName);
        Validate.notNull(pom,
                "The pom is not available, so plugins cannot be modified at this time");
        Validate.notNull(plugin, "Plugin required");
        for (final Plugin existingPlugin : pom.getBuildPlugins()) {
            if (existingPlugin.equals(plugin)) {
                // Already exists, so just quit
                return;
            }
        }

        // Delete any existing plugin with a different version
        removeBuildPlugin(moduleName, plugin);

        // Add the plugin
        addBuildPlugin(moduleName, plugin);
    }
    
    
    @Override
    public void updateDependencyScope(final String moduleName,
            final Dependency dependency, final DependencyScope dependencyScope) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Dependency modification prohibited at this time");
        Validate.notNull(dependency, "Dependency to update required");
        final Pom pom = getPomFromModuleName(moduleName);
        Validate.notNull(pom,
                "The pom is not available, so updating a dependency cannot be performed");
        if (!pom.isDependencyRegistered(dependency, false)) {
            return;
        }

        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(pom.getPath()));
        final Element root = document.getDocumentElement();
        final Element dependencyElement = XmlUtils.findFirstElement(
                "/project/dependencies/dependency[groupId = '"
                        + dependency.getGroupId() + "' and artifactId = '"
                        + dependency.getArtifactId() + "' and version = '"
                        + dependency.getVersion() + "']", root);
        if (dependencyElement == null) {
            return;
        }

        final Element scopeElement = XmlUtils.findFirstElement("scope",
                dependencyElement);
        final String descriptionOfChange;
        if (scopeElement == null) {
            if (dependencyScope != null) {
                dependencyElement.appendChild(new XmlElementBuilder("scope",
                        document).setText(dependencyScope.name().toLowerCase())
                        .build());
                descriptionOfChange = highlight(ADDED + " scope") + " "
                        + dependencyScope.name().toLowerCase()
                        + " to dependency " + dependency.getSimpleDescription();
            }
            else {
                descriptionOfChange = null;
            }
        }
        else {
            if (dependencyScope != null) {
                scopeElement.setTextContent(dependencyScope.name()
                        .toLowerCase());
                descriptionOfChange = highlight(CHANGED + " scope") + " to "
                        + dependencyScope.name().toLowerCase()
                        + " in dependency " + dependency.getSimpleDescription();
            }
            else {
                dependencyElement.removeChild(scopeElement);
                descriptionOfChange = highlight(REMOVED + " scope")
                        + " from dependency "
                        + dependency.getSimpleDescription();
            }
        }

        if (descriptionOfChange != null) {
            fileManager
                    .createOrUpdateTextFileIfRequired(pom.getPath(),
                            XmlUtils.nodeToString(document),
                            descriptionOfChange, false);
        }
    }
    
    @Override
    public void updateProjectType(final String moduleName,
            final ProjectType projectType) {
        Validate.notNull(projectType, "Project type required");
        final Pom pom = getPomFromModuleName(moduleName);
        Validate.notNull(pom,
                "The pom is not available, so the project type cannot be changed");

        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(pom.getPath()));
        final Element packaging = DomUtils.createChildIfNotExists("packaging",
                document.getDocumentElement(), document);
        if (packaging.getTextContent().equals(projectType.getType())) {
            return;
        }

        packaging.setTextContent(projectType.getType());
        final String descriptionOfChange = highlight(UPDATED + " project type")
                + " to " + projectType.getType();

        fileManager.createOrUpdateTextFileIfRequired(pom.getPath(),
                XmlUtils.nodeToString(document), descriptionOfChange, false);
    }
    
    @Override
    public Pom getPomFromPath(final String pomPath) {
        updatePomCache();
        return pomMap.get(pomPath);
    }
    
    @Override
    public Pom getRootPom() {
        updatePomCache();
        return pomMap.get(projectRootDirectory + SEPARATOR + DEFAULT_POM_NAME);
    }
    
    private void updatePomCache() {
        findUnparsedPoms();
        final Collection<Pom> newPoms = parseUnparsedPoms();
        if (!newPoms.isEmpty()) {
            sortPomMap();
        }
        updateProjectMetadataForModules(newPoms);
    }
    
    private void findUnparsedPoms() {
        for (final String change : getFileMonitorService().getDirtyFiles(getClass()
                .getName())) {
            if (change.endsWith(DEFAULT_POM_NAME)) {
                toBeParsed.add(change);
            }
        }
    }
    
    private Set<Pom> parseUnparsedPoms() {
        final Map<String, String> pomModuleMap = new HashMap<String, String>();
        final Set<Pom> newPoms = new HashSet<Pom>();
        for (final Iterator<String> iter = toBeParsed.iterator(); iter
                .hasNext();) {
            final String pathToChangedPom = iter.next();
            if (new File(pathToChangedPom).exists()) {
                String pomContents = "";
                try {
                    pomContents = org.apache.commons.io.FileUtils
                            .readFileToString(new File(pathToChangedPom));
                }
                catch (IOException ignored) {
                }
                if (StringUtils.isNotBlank(pomContents)) {
                    final Element rootElement = XmlUtils
                            .stringToElement(pomContents);
                    resolvePoms(rootElement, pathToChangedPom, pomModuleMap);
                    final String moduleName = getModuleName(FileUtils
                            .getFirstDirectory(pathToChangedPom));
                    final Pom pom = getPomFactory().getInstance(rootElement,
                            pathToChangedPom, moduleName);
                    Validate.notNull(pom,
                            "POM is null for module '%s' and path '%s'",
                            moduleName, pathToChangedPom);
                    pomMap.put(pathToChangedPom, pom);
                    newPoms.add(pom);
                    iter.remove();
                }
            }
        }
        return newPoms;
    }
    
    private String getModuleName(final String pomDirectory) {
        final String normalisedRootPath = FileUtils
                .ensureTrailingSeparator(projectRootDirectory);
        final String normalisedPomDirectory = FileUtils
                .ensureTrailingSeparator(pomDirectory);
        final String moduleName = StringUtils.removeStart(
                normalisedPomDirectory, normalisedRootPath);
        return StringUtils.stripEnd(moduleName, SEPARATOR);
    }
    
    private void resolvePoms(final Element pomRoot, final String pomPath,
            final Map<String, String> pomSet) {
        pomSet.put(pomPath, pomSet.get(pomPath)); // ensures this key exists

        final Element parentElement = XmlUtils.findFirstElement(
                "/project/parent", pomRoot);
        if (parentElement != null) {
            resolveParentPom(pomPath, pomSet, parentElement);
        }

        resolveChildModulePoms(pomRoot, pomPath, pomSet);
    }
    
    private void resolveChildModulePoms(final Element pomRoot,
            final String pomPath, final Map<String, String> pomSet) {
        for (final Element module : XmlUtils.findElements(
                "/project/modules/module", pomRoot)) {
            final String moduleName = module.getTextContent();
            if (StringUtils.isNotBlank(moduleName)) {
                final String modulePath = resolveRelativePath(pomPath,
                        moduleName);
                final boolean alreadyDiscovered = pomSet
                        .containsKey(modulePath);
                pomSet.put(modulePath, moduleName);
                if (!alreadyDiscovered) {
                    final Document pomDocument = XmlUtils.readXml(getFileManager()
                            .getInputStream(modulePath));
                    final Element root = pomDocument.getDocumentElement();
                    resolvePoms(root, modulePath, pomSet);
                }
            }
        }
    }
    
    private void resolveParentPom(final String pomPath,
            final Map<String, String> pomSet, final Element parentElement) {
        final String relativePath = XmlUtils.getTextContent("/relativePath",
                parentElement, DEFAULT_RELATIVE_PATH);
        final String parentPomPath = resolveRelativePath(pomPath, relativePath);
        final boolean alreadyDiscovered = pomSet.containsKey(parentPomPath);
        if (!alreadyDiscovered) {
            pomSet.put(parentPomPath, pomSet.get(parentPomPath));
            if (new File(parentPomPath).isFile()) {
                final Document pomDocument = XmlUtils.readXml(getFileManager()
                        .getInputStream(parentPomPath));
                final Element root = pomDocument.getDocumentElement();
                resolvePoms(root, parentPomPath, pomSet);
            }
        }
    }
    
    private String resolveRelativePath(String relativeTo,
            final String relativePath) {
        if (relativeTo.endsWith(SEPARATOR)) {
            relativeTo = relativeTo.substring(0, relativeTo.length() - 1);
        }
        while (new File(relativeTo).isFile()) {
            relativeTo = relativeTo.substring(0,
                    relativeTo.lastIndexOf(SEPARATOR));
        }
        final String[] relativePathSegments = relativePath.split(FileUtils
                .getFileSeparatorAsRegex());

        int backCount = 0;
        for (final String relativePathSegment : relativePathSegments) {
            if (relativePathSegment.equals("..")) {
                backCount++;
            }
            else {
                break;
            }
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = backCount; i < relativePathSegments.length; i++) {
            sb.append(relativePathSegments[i]);
            sb.append(SEPARATOR);
        }

        while (backCount > 0) {
            relativeTo = relativeTo.substring(0,
                    relativeTo.lastIndexOf(SEPARATOR));
            backCount--;
        }
        String path = relativeTo + SEPARATOR + sb.toString();
        if (new File(path).isDirectory()) {
            path = path + DEFAULT_POM_NAME;
        }
        if (path.endsWith(SEPARATOR)) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
    
    private void sortPomMap() {
        final List<String> sortedPomPaths = new ArrayList<String>(
                pomMap.keySet());
        final Map<String, Pom> sortedPomMap = new LinkedHashMap<String, Pom>();
        for (final String pomPath : sortedPomPaths) {
            sortedPomMap.put(pomPath, pomMap.get(pomPath));
        }
        pomMap.clear();
        pomMap.putAll(sortedPomMap);
    }
    
    private void updateProjectMetadataForModules(final Iterable<Pom> newPoms) {
        for (final Pom pom : newPoms) {
            final String projectMetadataId = ProjectMetadata
                    .getProjectIdentifier(pom.getModuleName());
            getMetadataService().evictAndGet(projectMetadataId);
            getMetadataDependencyRegistry().notifyDownstream(projectMetadataId);
        }
    }
    
    /**
     * Removes an element identified by the given dependency, whenever it occurs
     * at the given path
     * 
     * @param moduleName the name of the module to remove the dependency from
     * @param dependency the dependency to remove
     * @param containingPath the path to the dependencies element
     * @param path the path to the individual dependency elements
     */
    private void removeDependency(final String moduleName,
            final Dependency dependency, final String containingPath,
            final String path) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Dependency modification prohibited at this time");
        Validate.notNull(dependency, "Dependency to remove required");
        final Pom pom = getPomFromModuleName(moduleName);
        Validate.notNull(pom,
                "The pom is not available, so dependency removal cannot be performed");
        if (!pom.isDependencyRegistered(dependency, false)) {
            return;
        }

        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(pom.getPath()));
        final Element root = document.getDocumentElement();

        String descriptionOfChange = "";
        final Element dependenciesElement = XmlUtils.findFirstElement(
                containingPath, root);
        for (final Element candidate : XmlUtils.findElements(path, root)) {
            if (dependency.equals(new Dependency(candidate))) {
                dependenciesElement.removeChild(candidate);
                descriptionOfChange = highlight(REMOVED + " dependency") + " "
                        + dependency.getSimpleDescription();
                // Stay in the loop, just in case it was in the POM more than
                // once
            }
        }

        DomUtils.removeTextNodes(dependenciesElement);

        fileManager.createOrUpdateTextFileIfRequired(pom.getPath(),
                XmlUtils.nodeToString(document), descriptionOfChange, false);
    }
    
    private void addRepositories(final String moduleName,
            final Collection<? extends Repository> repositories,
            final String containingPath, final String path) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Repository modification prohibited at this time");
        Validate.notNull(repositories, "Repositories required");

        if (CollectionUtils.isEmpty(repositories)) {
            return;
        }
        final Pom pom = getPomFromModuleName(moduleName);
        Validate.notNull(pom,
                "The pom is not available, so repository addition cannot be performed");
        if ("pluginRepository".equals(path)) {
            if (pom.isAllPluginRepositoriesRegistered(repositories)) {
                return;
            }
        }
        else if (pom.isAllRepositoriesRegistered(repositories)) {
            return;
        }

        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(pom.getPath()));
        final Element repositoriesElement = DomUtils.createChildIfNotExists(
                containingPath, document.getDocumentElement(), document);

        final List<String> addedRepositories = new ArrayList<String>();
        for (final Repository repository : repositories) {
            if ("pluginRepository".equals(path)) {
                if (pom.isPluginRepositoryRegistered(repository)) {
                    continue;
                }
            }
            else {
                if (pom.isRepositoryRegistered(repository)) {
                    continue;
                }
            }
            if (repository != null) {
                repositoriesElement.appendChild(repository.getElement(document,
                        path));
                addedRepositories.add(repository.getUrl());
            }
        }
        final String message = getDescriptionOfChange(ADDED, addedRepositories,
                path, containingPath);

        fileManager.createOrUpdateTextFileIfRequired(pom.getPath(),
                XmlUtils.nodeToString(document), message, false);
    }
    
    private void addRepository(final String moduleName,
            final Repository repository, final String containingPath,
            final String path) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Repository modification prohibited at this time");
        Validate.notNull(repository, "Repository required");
        addRepositories(moduleName, Collections.singletonList(repository),
                containingPath, path);
    }

    private void detectCircularDependency(final Pom module1, final Pom module2) {
        if (module1.isDependencyRegistered(module2.asDependency(COMPILE), false)
                && module2
                        .isDependencyRegistered(module1.asDependency(COMPILE), false)) {
            throw new IllegalStateException("Circular dependency detected, '"
                    + module1.getModuleName() + "' depends on '"
                    + module2.getModuleName() + "' and vice versa");
        }
    }

    private void removeBuildPlugins(final String moduleName,
            final Collection<? extends Plugin> plugins,
            final boolean writeImmediately) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Plugin modification prohibited at this time");
        Validate.notNull(plugins, "Plugins required");
        if (CollectionUtils.isEmpty(plugins)) {
            return;
        }
        final Pom pom = getPomFromModuleName(moduleName);
        Validate.notNull(pom,
                "The pom is not available, so plugin removal cannot be performed");
        if (!pom.isAnyPluginsRegistered(plugins)) {
            return;
        }

        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(pom.getPath()));
        final Element root = document.getDocumentElement();
        final Element pluginsElement = XmlUtils.findFirstElement(
                "/project/build/plugins", root);
        if (pluginsElement == null) {
            return;
        }

        final List<String> removedPlugins = new ArrayList<String>();
        for (final Plugin plugin : plugins) {
            // Can't filter the XPath on groupId, as it's optional in the POM
            // for Apache-owned plugins
            for (final Element candidate : XmlUtils.findElements(
                    "plugin[artifactId = '" + plugin.getArtifactId()
                            + "' and version = '" + plugin.getVersion() + "']",
                    pluginsElement)) {
                final Plugin candidatePlugin = new Plugin(candidate);
                if (candidatePlugin.getGroupId().equals(plugin.getGroupId())) {
                    // This element has the same groupId, artifactId, and
                    // version as the plugin to be removed; remove it
                    pluginsElement.removeChild(candidate);
                    removedPlugins.add(candidatePlugin.getSimpleDescription());
                    // Keep looping in case this plugin is in the POM more than
                    // once (unlikely)
                }
            }
        }
        if (removedPlugins.isEmpty()) {
            return;
        }
        DomUtils.removeTextNodes(pluginsElement);
        final String message = getDescriptionOfChange(REMOVED, removedPlugins,
                "plugin", "plugins");

        fileManager.createOrUpdateTextFileIfRequired(pom.getPath(),
                XmlUtils.nodeToString(document), message, writeImmediately);
    }
    
    private void removeRepository(final String moduleName,
            final Repository repository, final String path) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Repository modification prohibited at this time");
        Validate.notNull(repository, "Repository required");
        final Pom pom = getPomFromModuleName(moduleName);
        Validate.notNull(pom,
                "The pom is not available, so repository removal cannot be performed");
        if ("pluginRepository".equals(path)) {
            if (!pom.isPluginRepositoryRegistered(repository)) {
                return;
            }
        }
        else {
            if (!pom.isRepositoryRegistered(repository)) {
                return;
            }
        }

        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(pom.getPath()));
        final Element root = document.getDocumentElement();

        String descriptionOfChange = "";
        for (final Element candidate : XmlUtils.findElements(path, root)) {
            if (repository.equals(new Repository(candidate))) {
                candidate.getParentNode().removeChild(candidate);
                descriptionOfChange = highlight(REMOVED + " repository") + " "
                        + repository.getUrl();
                // We stay in the loop just in case it was in the POM more than
                // once
            }
        }

        fileManager.createOrUpdateTextFileIfRequired(pom.getPath(),
                XmlUtils.nodeToString(document), descriptionOfChange, false);
    }
    
    
    
	
	
    private String getPomDependenciesUpdateMessage(
            final Collection<String> addedDependencies,
            final Collection<String> removedDependencies,
            final Collection<String> skippedDependencies) {
        final List<String> changes = new ArrayList<String>();
        changes.add(getDescriptionOfChange(ADDED, addedDependencies,
                "dependency", "dependencies"));
        changes.add(getDescriptionOfChange(REMOVED, removedDependencies,
                "dependency", "dependencies"));
        changes.add(getDescriptionOfChange(SKIPPED, skippedDependencies,
                "dependency", "dependencies"));
        for (final Iterator<String> iter = changes.iterator(); iter.hasNext();) {
            if (StringUtils.isBlank(iter.next())) {
                iter.remove();
            }
        }
        return StringUtils.join(changes, "; ");
    }
	
    private String getPomPluginsUpdateMessage(
            final Collection<String> addedPlugins,
            final Collection<String> removedPlugins) {
        final List<String> changes = new ArrayList<String>();
        changes.add(getDescriptionOfChange(ADDED, addedPlugins, "plugin",
                "plugins"));
        changes.add(getDescriptionOfChange(REMOVED, removedPlugins, "plugin",
                "plugins"));
        for (final Iterator<String> iter = changes.iterator(); iter.hasNext();) {
            if (StringUtils.isBlank(iter.next())) {
                iter.remove();
            }
        }
        return StringUtils.join(changes, "; ");
    }
    
    
    /**
     * Generates a message about the addition of the given items to the POM
     * 
     * @param action the past tense of the action that was performed
     * @param items the items that were acted upon (required, can be empty)
     * @param singular the singular of this type of item (required)
     * @param plural the plural of this type of item (required)
     * @return a non-<code>null</code> message
     * @since 1.2.0
     */
    static String getDescriptionOfChange(final String action,
            final Collection<String> items, final String singular,
            final String plural) {
        if (items.isEmpty()) {
            return "";
        }
        return highlight(action + " " + (items.size() == 1 ? singular : plural))
                + " " + StringUtils.join(items, ", ");
    }
    
    /**
     * Highlights the given text
     * 
     * @param text the text to highlight (can be blank)
     * @return the highlighted text
     */
    static String highlight(final String text) {
        return decorate(text, FG_CYAN);
    }
    
    /**
     * Method that removes version from dependency element if blank
     * 
     * @param dependency
     * @return Element that contains dependency without version if blank
     */
    private Element removeVersionIfBlank(Element dependency) {
    	NodeList dependencyAttributes = dependency.getChildNodes();
    	for (int i = 0; i < dependencyAttributes.getLength(); i++) {
    		Element dependencyAttribute = (Element) dependencyAttributes.item(i);
			if (dependencyAttribute != null && dependencyAttribute.getTagName().equals("version")
					&& (dependencyAttribute.getTextContent() == null
							|| "".equals(dependencyAttribute.getTextContent()))) {
				dependency.removeChild(dependencyAttributes.item(i));
				break;
			}
    	}
    	return dependency;
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
    
    
    
    // Typical provider methods
    
	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public boolean isActive() {
		// Checking if pom.xml file exists
		if(getFileManager().exists(DEFAULT_RELATIVE_PATH)){
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
    
    
    // OSGi Services dynamic getters
    
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
    
    public MetadataService getMetadataService(){
    	if(metadataService == null){
    		// Get all Services implement MetadataService interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(MetadataService.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				metadataService = (MetadataService) this.context.getService(ref);
    				return metadataService;
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load MetadataService on MavenProjectManagerProvider.");
    			return null;
    		}
    	}else{
    		return metadataService;
    	}
    }
    
    public Shell getShell(){
    	if(shell == null){
    		// Get all Services implement Shell interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(Shell.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				shell = (Shell) this.context.getService(ref);
    				return shell;
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load Shell on MavenProjectManagerProvider.");
    			return null;
    		}
    	}else{
    		return shell;
    	}
    }
    
	public FileMonitorService getFileMonitorService() {
		if (fileMonitorService == null) {
			// Get all Services implement FileMonitorService interface
			try {
				ServiceReference<?>[] references = context
						.getAllServiceReferences(
								FileMonitorService.class.getName(), null);
				
				for (ServiceReference<?> ref : references) {
					fileMonitorService = (FileMonitorService) context.getService(ref);
					return fileMonitorService;
				}
				
				return null;
				
			} catch (InvalidSyntaxException e) {
				LOGGER.warning("Cannot load FileMonitorService on MavenProjectManagerProvider.");
				return null;
			}
		} else {
			return fileMonitorService;
		}
	}
	
	public PomFactory getPomFactory() {
		if (pomFactory == null) {
			// Get all Services implement PomFactory interface
			try {
				ServiceReference<?>[] references = context
						.getAllServiceReferences(
								PomFactory.class.getName(), null);
				
				for (ServiceReference<?> ref : references) {
					pomFactory = (PomFactory) context.getService(ref);
					return pomFactory;
				}
				
				return null;
				
			} catch (InvalidSyntaxException e) {
				LOGGER.warning("Cannot load PomFactory on MavenProjectManagerProvider.");
				return null;
			}
		} else {
			return pomFactory;
		}
	}
	
	public MetadataDependencyRegistry getMetadataDependencyRegistry() {
		if (metadataDependencyRegistry == null) {
			// Get all Services implement MetadataDependencyRegistry interface
			try {
				ServiceReference<?>[] references = context
						.getAllServiceReferences(
								MetadataDependencyRegistry.class.getName(), null);
				
				for (ServiceReference<?> ref : references) {
					metadataDependencyRegistry = (MetadataDependencyRegistry) context.getService(ref);
					return metadataDependencyRegistry;
				}
				
				return null;
				
			} catch (InvalidSyntaxException e) {
				LOGGER.warning("Cannot load MetadataDependencyRegistry on MavenProjectManagerProvider.");
				return null;
			}
		} else {
			return metadataDependencyRegistry;
		}
	}
}
