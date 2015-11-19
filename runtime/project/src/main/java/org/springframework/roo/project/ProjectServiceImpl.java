package org.springframework.roo.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.packaging.PackagingProvider;
import org.springframework.roo.project.providers.ProjectManagerProvider;
import org.springframework.roo.project.providers.ProjectManagerProviderId;
import org.springframework.roo.project.providers.maven.Pom;

/**
 * Provides common project operations. Should be subclassed by a
 * project-specific operations subclass.
 * 
 * @author Ben Alex
 * @author Adrian Colyer
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @author Juan Carlos García
 * @since 1.0
 */
@Component
@Service
@References(value = {
		@Reference(name = "feature", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = Feature.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE),
		@Reference(name = "provider", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = ProjectManagerProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
		}
)
public class ProjectServiceImpl implements ProjectService {

    private final Map<String, Feature> features = new HashMap<String, Feature>();
	private List<ProjectManagerProvider> providers = new ArrayList<ProjectManagerProvider>();
    private ProjectManagerProvider currentProvider = null;

	@Override
	public boolean isCreateProjectAvailable() {
		
		// Show error message if providers is empty
		if (providers.isEmpty()) {
			throw new RuntimeException("ERROR: Doesn't exists any ProjectManager provider."
					+ " You should implement a new one to be able to generate a new Spring Roo project.");
		}
		
		// Getting selected provider from all available providers.
        for (ProjectManagerProvider projectManagerProvider : providers) {
            if (projectManagerProvider.isActive()) {
            	currentProvider = projectManagerProvider;
                break;
            }
        }
        
        return currentProvider == null;
	}
	
	/**{@inheritDoc}*/
	@Override
	public void createProject(JavaPackage topLevelPackage, String projectName, Integer majorJavaVersion,
			PackagingProvider packagingType, ProjectManagerProviderId provider) {
		 Validate.isTrue(isCreateProjectAvailable(),
	                "Project creation is unavailable at this time");
		
		// Getting selected provider from all available providers.
        for (ProjectManagerProvider projectManagerProvider : providers) {
            if (provider.is(projectManagerProvider)) {
            	currentProvider = projectManagerProvider;
                break;
            }
        }
        
        if(currentProvider == null){
        	throw new RuntimeException(String.format("ERROR: Selected ProjectManager provider '%s' is not available", provider.getId()));
        }
        
        // Execute createProject operation using selected provider 
        currentProvider.createProject(topLevelPackage, projectName, majorJavaVersion, packagingType);
	}
	
	
	@Override
	public void addBuildPlugin(final String moduleName, final Plugin plugin) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Plugin modification prohibited at this time");
        Validate.notNull(plugin, "Plugin required");
        
        // Getting active project manager and adding build plugins
        getCurrentProjectManagerProvider().addBuildPlugin(moduleName, plugin);
    }

	@Override
    public void addBuildPlugins(final String moduleName,
            final Collection<? extends Plugin> newPlugins) {
        Validate.isTrue(isProjectAvailable(moduleName),
                "Plugin modification prohibited at this time");
        Validate.notNull(newPlugins, "Plugins required");
        
        // Getting active project manager and adding build plugins
        getCurrentProjectManagerProvider().addBuildPlugins(moduleName, newPlugins);
    }
	
    @Override
    public List<Dependency> addDependencies(final String moduleName,
            Collection<? extends Dependency> dependencies) {
    	// Getting active project manager and add dependencies
        return getCurrentProjectManagerProvider().addDependencies(moduleName, dependencies);
    }
	
    @Override
    public Dependency addDependency(final String moduleName, Dependency dependency) {
    	// Getting active project manager and add dependencies
        return getCurrentProjectManagerProvider().addDependency(moduleName, dependency);
    }
    
    @Override
    public Dependency addDependency(final String moduleName, String groupId,
            String artifactId, String version) {
    	// Getting active project manager and add dependencies
    	return getCurrentProjectManagerProvider().addDependency(moduleName, groupId, artifactId, version);
    }

    @Override
    public Dependency addDependency(final String moduleName, String groupId,
            String artifactId, String version, DependencyScope scope) {
    	// Getting active project manager and add dependencies
    	return getCurrentProjectManagerProvider().addDependency(moduleName, groupId, artifactId, version, scope);
    }

    @Override
    public Dependency addDependency(final String moduleName, String groupId,
            String artifactId, String version, DependencyScope scope,
            String classifier) {
    	// Getting active project manager and add dependencies
    	return getCurrentProjectManagerProvider().addDependency(moduleName, groupId, artifactId, version, scope, classifier);
    	
    }
    
    @Override
    public void addFilter(final String moduleName, Filter filter) {
    	// Getting active project manager and add filters
    	getCurrentProjectManagerProvider().addFilter(moduleName, filter);
    }
    
    @Override
    public void addModuleDependency(String moduleName) {
    	// Getting active project manager and add filters
    	getCurrentProjectManagerProvider().addModuleDependency(moduleName);
    }
    
    @Override
    public void addPluginRepositories(final String moduleName,
            final Collection<? extends Repository> repositories) {
    	// Getting active project manager and adding plugin repositories
    	getCurrentProjectManagerProvider().addPluginRepositories(moduleName, repositories);
    }
    
    @Override
    public void addPluginRepository(final String moduleName,
            final Repository repository) {
    	// Getting active project manager and adding plugin repositories
    	getCurrentProjectManagerProvider().addPluginRepository(moduleName, repository);
    }
    
    @Override
    public void addProperty(final String moduleName, final Property property) {
    	// Getting active project manager and adding property
    	getCurrentProjectManagerProvider().addProperty(moduleName, property);
    }
    
    @Override
    public void addRepositories(final String moduleName,
            final Collection<? extends Repository> repositories) {
    	// Getting active project manager and adding repositories
    	getCurrentProjectManagerProvider().addRepositories(moduleName, repositories);
    }
    
    @Override
    public void addRepository(final String moduleName,
            final Repository repository) {
    	// Getting active project manager and adding repository
    	getCurrentProjectManagerProvider().addRepository(moduleName, repository);
    }
    
    @Override
    public void addResource(final String moduleName, final Resource resource) {
    	// Getting active project manager and adding resource
    	getCurrentProjectManagerProvider().addResource(moduleName, resource);
    }
    
    
    @Override
    public Pom getFocusedModule() {
    	// Getting active project manager and get focused module
    	return getCurrentProjectManagerProvider().getFocusedModule();
    }

    @Override
    public String getFocusedModuleName() {
    	// Getting active project manager and get focused module name
    	return getCurrentProjectManagerProvider().getFocusedModuleName();
    }
    
    @Override
    public ProjectMetadata getFocusedProjectMetadata() {
    	// Getting active project manager and get focused project metadata
    	return getCurrentProjectManagerProvider().getFocusedProjectMetadata();
    }
    
    @Override
    public String getFocusedProjectName() {
    	// Getting active project manager and get focused project name
    	return getCurrentProjectManagerProvider().getFocusedProjectName();
    }
	
    @Override
    public JavaPackage getFocusedTopLevelPackage() {
    	// Getting active project manager and get focused topLevelPackage
    	return getCurrentProjectManagerProvider().getFocusedTopLevelPackage();
    }

    @Override
    public Pom getModuleForFileIdentifier(final String fileIdentifier) {
    	// Getting active project manager and get module for file identifier
    	return getCurrentProjectManagerProvider().getModuleForFileIdentifier(fileIdentifier);
    }

    @Override
    public Collection<String> getModuleNames() {
    	// Getting active project manager and get module for file identifier
    	return getCurrentProjectManagerProvider().getModuleNames();
    }
    
    @Override
    public PathResolver getPathResolver() {
    	// Getting active project manager and get path resolver
    	return getCurrentProjectManagerProvider().getPathResolver();
    }
    
	@Override
    public Pom getPomFromModuleName(String moduleName){
		// Getting active project manager and getting pom from module name
        return getCurrentProjectManagerProvider().getPomFromModuleName(moduleName);
	}

    @Override
    public Collection<Pom> getPoms() {
    	// Getting active project manager and get avalable poms
    	return getCurrentProjectManagerProvider().getPoms();
    }
    
    @Override
    public ProjectMetadata getProjectMetadata(String moduleName) {
    	// Getting active project manager and getting projectmetadata
        return getCurrentProjectManagerProvider().getProjectMetadata(moduleName);
    }
    
    @Override
    public String getProjectName(String moduleName) {
    	// Getting active project manager and getting projectName
        return getCurrentProjectManagerProvider().getProjectName(moduleName);
    }
    
    @Override
    public JavaPackage getTopLevelPackage(final String moduleName) {
    	// Getting active project manager and getting topLevelPackage
        return getCurrentProjectManagerProvider().getTopLevelPackage(moduleName);
    }
    
    @Override
    public boolean isFeatureInstalled(final String featureName) {
		if(getCurrentProjectManagerProvider() == null){
			return false;
		}
    	// Getting active project manager and check if feature is installed
        return getCurrentProjectManagerProvider().isFeatureInstalled(featureName, features);
    }
    
    @Override
    public boolean isFeatureInstalledInModule(String featureName,
            String moduleName) {
		if(getCurrentProjectManagerProvider() == null){
			return false;
		}
    	// Getting active project manager and check if feature is installed in module
        return getCurrentProjectManagerProvider().isFeatureInstalledInModule(featureName, moduleName, features);
    }
    
    @Override
    public boolean isFeatureInstalledInFocusedModule(
            final String... featureNames) {
		if(getCurrentProjectManagerProvider() == null){
			return false;
		}
    	// Getting active project manager and check if some features are installed in module
        return getCurrentProjectManagerProvider().isFeatureInstalledInFocusedModule(features, featureNames);
    }
    
	@Override
    public boolean isFocusedProjectAvailable() {
		if(getCurrentProjectManagerProvider() == null){
			return false;
		}
		// Getting active project manager and check if focused project available
        return getCurrentProjectManagerProvider().isFocusedProjectAvailable();
    }
	
	@Override
    public boolean isModuleCreationAllowed() {
		if(getCurrentProjectManagerProvider() == null){
			return true;
		}
		// Getting active project manager and check if is module creation allowed
        return getCurrentProjectManagerProvider().isModuleCreationAllowed();
    }
	
	@Override
    public boolean isModuleFocusAllowed() {
		if(getCurrentProjectManagerProvider() == null){
			return false;
		}
		// Getting active project manager and check if is module focus allowed
        return getCurrentProjectManagerProvider().isModuleFocusAllowed();
    }
	
    @Override
    public boolean isProjectAvailable(String moduleName) {
		if(getCurrentProjectManagerProvider() == null){
			return false;
		}
    	// Getting active project manager and checking if project is available
        return getCurrentProjectManagerProvider().isProjectAvailable(moduleName);
    }
    
    @Override
    public void removeBuildPlugin(final String moduleName, final Plugin plugin) {
    	// Getting active project manager and remove build plugin
        getCurrentProjectManagerProvider().removeBuildPlugin(moduleName, plugin);
    }
    
    @Override
    public void removeBuildPluginImmediately(final String moduleName,
            final Plugin plugin) {
    	// Getting active project manager and remove build plugin inmediately
        getCurrentProjectManagerProvider().removeBuildPluginImmediately(moduleName, plugin);
    }
    
    @Override
    public void removeBuildPlugins(final String moduleName,
            final Collection<? extends Plugin> plugins) {
    	// Getting active project manager and remove build plugins
        getCurrentProjectManagerProvider().removeBuildPlugins(moduleName, plugins);
    }
    
    @Override
    public void removeDependencies(final String moduleName,
            final Collection<? extends Dependency> dependenciesToRemove) {
    	// Getting active project manager and remove dependencies
        getCurrentProjectManagerProvider().removeDependencies(moduleName, dependenciesToRemove);
    }

    @Override
    public void removeDependency(final String moduleName,
            final Dependency dependency) {
    	// Getting active project manager and remove dependency
        getCurrentProjectManagerProvider().removeDependency(moduleName, dependency);
    }

 
    @Override
    public final void removeDependency(final String moduleName,
            final String groupId, final String artifactId, final String version) {
    	// Getting active project manager and remove dependency
        getCurrentProjectManagerProvider().removeDependency(moduleName, groupId, artifactId, version);
    }

    @Override
    public final void removeDependency(final String moduleName,
            final String groupId, final String artifactId,
            final String version, final String classifier) {
    	// Getting active project manager and remove dependency
        getCurrentProjectManagerProvider().removeDependency(moduleName, groupId, artifactId, version, classifier);
    }
    
    
    @Override
    public void removeFilter(final String moduleName, final Filter filter) {
    	// Getting active project manager and remove filter
        getCurrentProjectManagerProvider().removeFilter(moduleName, filter);
    }
    
    @Override
    public void removePluginRepository(final String moduleName,
            final Repository repository) {
    	// Getting active project manager and remove plugin repository
        getCurrentProjectManagerProvider().removePluginRepository(moduleName, repository);
    }
    
    @Override
    public void removeProperty(final String moduleName, final Property property) {
    	// Getting active project manager and remove property
        getCurrentProjectManagerProvider().removeProperty(moduleName, property);
    }
    
    @Override
    public void removeRepository(final String moduleName,
            final Repository repository) {
    	// Getting active project manager and removing repository
        getCurrentProjectManagerProvider().removeRepository(moduleName, repository);
    }
    
    @Override
    public void removeResource(final String moduleName, final Resource resource) {
    	// Getting active project manager and removing resource
        getCurrentProjectManagerProvider().removeResource(moduleName, resource);
    }
    
    @Override
    public void setModule(final Pom module) {
    	// Getting active project manager and setting module
        getCurrentProjectManagerProvider().setModule(module);
    }
    
    @Override
    public void updateBuildPlugin(final String moduleName, final Plugin plugin) {
    	// Getting active project manager and update build plugin
        getCurrentProjectManagerProvider().updateBuildPlugin(moduleName, plugin);
    }
    
    @Override
    public void updateDependencyScope(final String moduleName,
            final Dependency dependency, final DependencyScope dependencyScope) {
    	// Getting active project manager and update dependency scope
        getCurrentProjectManagerProvider().updateDependencyScope(moduleName, dependency, dependencyScope);
    }
    
    @Override
    public void updateProjectType(final String moduleName,
            final ProjectType projectType) {
    	// Getting active project manager and update project type
        getCurrentProjectManagerProvider().updateProjectType(moduleName, projectType);
    }
    
    @Override
    public Pom getPomFromPath(final String pomPath) {
    	// Getting active project manager and getting pom from specified Path
        return getCurrentProjectManagerProvider().getPomFromPath(pomPath);
    }
    
    @Override
    public Pom getRootPom() {
    	// Getting active project manager and getting root pom
        return getCurrentProjectManagerProvider().getRootPom();
    }
    
    @Override
    public ProjectManagerProviderId getProviderIdByName(String value) {
        if (providers.isEmpty()) {
            return null;
        }

        for (ProjectManagerProvider provider : providers) {
            if (provider.isAvailable()
                    && StringUtils.equals(value, provider.getName())) {
                return new ProjectManagerProviderId(provider);
            }
        }
        return null;
    }

    @Override
    public List<ProjectManagerProviderId> getProvidersId() {
        List<ProjectManagerProviderId> availables = new ArrayList<ProjectManagerProviderId>(
                providers.size());
        if (!providers.isEmpty()) {
            for (ProjectManagerProvider provider : providers) {
                if (provider.isAvailable()) {
                    availables.add(new ProjectManagerProviderId(provider));
                }
            }
        }
        return Collections.unmodifiableList(availables);
    }
    
    protected void bindFeature(final Feature feature) {
        if (feature != null) {
            features.put(feature.getName(), feature);
        }
    }
    
    protected void unbindFeature(final Feature feature) {
        if (feature != null) {
            features.remove(feature.getName());
        }
    }

    /**
	 * Bind providers list
	 * 
	 * @param provider
	 */
	protected void bindProvider(final ProjectManagerProvider provider) {
		providers.add(provider);
	}
	
    /**
     * Unbind providers list
     * 
     * @param provider
     */
    protected void unbindProvider(final ProjectManagerProvider provider) {
        providers.remove(provider);
        // Reset current provider
        currentProvider = null;
    }
    
    
    /**
     * Method that returns current ProjectManager provider
     * 
     * @return ProjectManagerProvider
     */
    private ProjectManagerProvider getCurrentProjectManagerProvider(){
    
    	// Check if is some project manager is saved
    	if(currentProvider != null){
    		return currentProvider;
    	}
    	
		// Show error message if providers is empty
		if (providers.isEmpty()) {
			throw new RuntimeException("ERROR: Doesn't exists any ProjectManager provider."
					+ " You should implement a new one to be able to generate a new Spring Roo project.");
		}
		
		// Getting selected provider from all available providers.
        for (ProjectManagerProvider projectManagerProvider : providers) {
            if (projectManagerProvider.isActive()) {
            	currentProvider = projectManagerProvider;
                break;
            }
        }
        
        return currentProvider;
    	
    }

}
