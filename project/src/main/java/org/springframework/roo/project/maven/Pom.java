package org.springframework.roo.project.maven;

import static org.springframework.roo.project.Path.ROOT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.DependencyScope;
import org.springframework.roo.project.DependencyType;
import org.springframework.roo.project.Filter;
import org.springframework.roo.project.GAV;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PhysicalPath;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.Repository;
import org.springframework.roo.project.Resource;
import org.springframework.roo.support.util.CollectionUtils;
import org.springframework.roo.support.util.FileUtils;

/**
 * A Maven project object model (POM).
 * 
 * @author James Tyrrell
 * @author Andrew Swan
 * @since 1.2.0
 */
public class Pom {

    static final String DEFAULT_PACKAGING = "jar"; // Maven behaviour

    private final Set<Plugin> buildPlugins = new LinkedHashSet<Plugin>();
    private final Set<Dependency> dependencies = new LinkedHashSet<Dependency>();
    private final Set<Filter> filters = new LinkedHashSet<Filter>();
    private final GAV gav;
    private final String moduleName;
    private final Set<Module> modules = new LinkedHashSet<Module>();
    private final String name;
    private final String packaging;
    private final Parent parent;
    private final String path;
    private final Map<Path, PhysicalPath> pathLocations = new LinkedHashMap<Path, PhysicalPath>();
    private final Set<Repository> pluginRepositories = new LinkedHashSet<Repository>();
    private final Set<Property> pomProperties = new LinkedHashSet<Property>();
    private final Set<Repository> repositories = new LinkedHashSet<Repository>();
    private final Set<Resource> resources = new LinkedHashSet<Resource>();
    private final String sourceDirectory; // TODO use pathCache instead
    private final String testSourceDirectory; // TODO use pathCache instead

    /**
     * Constructor
     * 
     * @param groupId the Maven groupId, explicit or inherited (required)
     * @param artifactId the Maven artifactId (required)
     * @param version the version of the artifact being built (required)
     * @param packaging the Maven packaging (required)
     * @param dependencies (can be <code>null</code> for none)
     * @param parent the POM's parent declaration (can be <code>null</code> for
     *            none)
     * @param modules the modules defined by this POM (only applies when
     *            packaging is "pom"; can be <code>null</code> for none)
     * @param pomProperties any properties defined in the POM (can be
     *            <code>null</code> for none)
     * @param name the Maven name of the artifact being built (can be blank)
     * @param repositories any repositories defined in the POM (can be
     *            <code>null</code> for none)
     * @param pluginRepositories any plugin repositories defined in the POM (can
     *            be <code>null</code> for none)
     * @param sourceDirectory the directory relative to the POM that contains
     *            production code (can be blank for the Maven default)
     * @param testSourceDirectory the directory relative to the POM that
     *            contains test code (can be blank for the Maven default)
     * @param filters any filters defined in the POM (can be <code>null</code>
     *            for none)
     * @param buildPlugins any plugins defined in the POM (can be
     *            <code>null</code> for none)
     * @param resources any build resources defined in the POM (can be
     *            <code>null</code> for none)
     * @param path the canonical path of this POM (required)
     * @param moduleName the Maven name of this module (blank for the project's
     *            root or only POM)
     * @param paths the {@link Path}s required for this module, in addition to
     *            the root (can be <code>null</code>)
     */
    public Pom(final String groupId, final String artifactId,
            final String version, final String packaging,
            final Collection<? extends Dependency> dependencies,
            final Parent parent, final Collection<? extends Module> modules,
            final Collection<? extends Property> pomProperties,
            final String name,
            final Collection<? extends Repository> repositories,
            final Collection<? extends Repository> pluginRepositories,
            final String sourceDirectory, final String testSourceDirectory,
            final Collection<? extends Filter> filters,
            final Collection<? extends Plugin> buildPlugins,
            final Collection<? extends Resource> resources, final String path,
            final String moduleName, final Collection<Path> paths) {
        Validate.notBlank(packaging, "Invalid packaging '%s'", packaging);
        Validate.notBlank(path, "Invalid path '%s'", path);

        //gav = new GAV(groupId, artifactId, version);
        this.moduleName = StringUtils.stripToEmpty(moduleName);
        this.name = StringUtils.stripToEmpty(name);
        this.packaging = packaging;
        this.parent = parent;

		if(version == null && parent.getVersion() != null) {
			gav = new GAV(groupId, artifactId, parent.getVersion());
		}else {
			gav = new GAV(groupId, artifactId, version);
		}

        this.path = path;
        this.sourceDirectory = StringUtils.defaultIfEmpty(sourceDirectory,
                Path.SRC_MAIN_JAVA.getDefaultLocation());
        this.testSourceDirectory = StringUtils.defaultIfEmpty(
                testSourceDirectory, Path.SRC_TEST_JAVA.getDefaultLocation());

        CollectionUtils.populate(this.buildPlugins, buildPlugins);
        CollectionUtils.populate(this.dependencies, dependencies);
        CollectionUtils.populate(this.filters, filters);
        CollectionUtils.populate(this.modules, modules);
        CollectionUtils.populate(this.pluginRepositories, pluginRepositories);
        CollectionUtils.populate(this.pomProperties, pomProperties);
        CollectionUtils.populate(this.repositories, repositories);
        CollectionUtils.populate(this.resources, resources);

        cachePhysicalPaths(paths);
    }

    /**
     * Returns this module as a Dependency with the given scope
     * 
     * @return a non-<code>null</code> instance
     */
    public Dependency asDependency(final DependencyScope scope) {
        return new Dependency(gav, DependencyType.valueOfTypeCode(packaging),
                scope);
    }

    private void cachePhysicalPaths(final Collection<Path> paths) {
        final Collection<Path> pathsToCache = CollectionUtils.populate(
                new HashSet<Path>(), paths);
        if (!pathsToCache.contains(ROOT)) {
            pathsToCache.add(ROOT);
        }
        for (final Path path : pathsToCache) {
            pathLocations.put(path, path.getModulePath(this));
        }
    }

    /**
     * Indicates whether it's valid to add the given {@link Dependency} to this
     * POM.
     * 
     * @param newDependency the {@link Dependency} to check (can be
     *            <code>null</code>)
     * @return see above
     * @since 1.2.1
     */
    public boolean canAddDependency(final Dependency newDependency) {
        return newDependency != null
                && !isDependencyRegistered(newDependency)
                && !Dependency.isHigherLevel(
                        newDependency.getType().toString(), packaging);
    }

    /**
     * Returns the ID of the artifact created by this module or project
     * 
     * @return a non-blank ID
     */
    public String getArtifactId() {
        return gav.getArtifactId();
    }

    /**
     * Returns any registered build plugins
     * 
     * @return a non-<code>null</code> collection
     */
    public Set<Plugin> getBuildPlugins() {
        return buildPlugins;
    }

    /**
     * Returns any build plugins with the same groupId and artifactId as the
     * given plugin. This is useful for upgrade cases.
     * 
     * @param plugin to locate (required; note the version number is ignored in
     *            comparisons)
     * @return any matching plugins (never returns null, but may return an empty
     *         {@link Set})
     */
    public Set<Plugin> getBuildPluginsExcludingVersion(final Plugin plugin) {
        Validate.notNull(plugin, "Plugin to locate is required");
        final Set<Plugin> result = new HashSet<Plugin>();
        for (final Plugin p : buildPlugins) {
            if (plugin.getArtifactId().equals(p.getArtifactId())
                    && plugin.getGroupId().equals(p.getGroupId())) {
                result.add(p);
            }
        }
        return result;
    }

    public Set<Dependency> getDependencies() {
        return dependencies;
    }

    /**
     * Locates any dependencies which match the presented dependency, excluding
     * the version number. This is useful for upgrade use cases, where it is
     * necessary to remove any dependencies with the same group id, artifact id,
     * and type as the dependency being upgraded to.
     * 
     * @param dependency to locate (can be <code>null</code>)
     * @return any matching dependencies (never returns null, but may return an
     *         empty {@link Set})
     */
    public Set<Dependency> getDependenciesExcludingVersion(
            final Dependency dependency) {
        final Set<Dependency> result = new HashSet<Dependency>();
        for (final Dependency d : dependencies) {
            if (dependency != null
                    && dependency.getArtifactId().equals(d.getArtifactId())
                    && dependency.getGroupId().equals(d.getGroupId())
                    && dependency.getType().equals(d.getType())) {
                result.add(d);
            }
        }
        return result;
    }

    /**
     * Returns the display name of this module of the user project
     * 
     * @return a non-blank name
     */
    public String getDisplayName() {
        return name;
    }

    public Set<Filter> getFilters() {
        return filters;
    }

    /**
     * Returns the ID of the organisation or group that owns this module or
     * project
     * 
     * @return a non-blank ID
     */
    public String getGroupId() {
        return gav.getGroupId();
    }

    /**
     * Returns the programmatic name of this module of the user project
     * 
     * @return an empty string for the root or only module
     */
    public String getModuleName() {
        return moduleName;
    }

    public Set<Module> getModules() {
        return modules;
    }

    /**
     * Returns the display name of this module of the user project
     * 
     * @return a non-blank name
     * @deprecated use {@link #getDisplayName()} instead
     */
    @Deprecated
    public String getName() {
        return getDisplayName();
    }

    public String getPackaging() {
        return packaging;
    }

    public Parent getParent() {
        return parent;
    }

    /**
     * Returns this descriptor's canonical path on the file system
     * 
     * @return a valid canonical path
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the canonical path of the given {@link Path} within this module,
     * plus a trailing separator if found
     * 
     * @param path the path for which to get the canonical location (required)
     * @return <code>null</code> if this module has no such path
     */
    public String getPathLocation(final Path path) {
        final PhysicalPath modulePath = getPhysicalPath(path);
        if (modulePath == null) {
            return null;
        }
        return FileUtils.ensureTrailingSeparator(modulePath.getLocationPath());
    }

    /**
     * Returns the {@link PhysicalPath} for the given {@link Path} of this
     * module
     * 
     * @param path the sub-path for which to return the {@link PhysicalPath}
     * @return <code>null</code> if this module has no such sub-path
     */
    public PhysicalPath getPhysicalPath(final Path path) {
        return pathLocations.get(path);
    }

    public List<PhysicalPath> getPhysicalPaths() {
        return new ArrayList<PhysicalPath>(pathLocations.values());
    }

    public Set<Repository> getPluginRepositories() {
        return pluginRepositories;
    }

    public Set<Property> getPomProperties() {
        return pomProperties;
    }

    /**
     * Locates any properties which match the presented property, excluding the
     * value. This is useful for upgrade use cases, where it is necessary to
     * locate any properties with the name so that they can be removed.
     * 
     * @param property to locate (required; note the value is ignored in
     *            comparisons)
     * @return any matching properties (never returns null, but may return an
     *         empty {@link Set})
     */
    public Set<Property> getPropertiesExcludingValue(final Property property) {
        Validate.notNull(property, "Property to locate is required");
        final Set<Property> result = new HashSet<Property>();
        for (final Property p : pomProperties) {
            if (property.getName().equals(p.getName())) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * Locates the first occurrence of a property for a given name and returns
     * it.
     * 
     * @param name the property name (required)
     * @return the property if found otherwise null
     */
    public Property getProperty(final String name) {
        Validate.notBlank(name, "Property name to locate is required");
        for (final Property p : pomProperties) {
            if (name.equals(p.getName())) {
                return p;
            }
        }
        return null;
    }

    public Set<Repository> getRepositories() {
        return repositories;
    }

    public Set<Resource> getResources() {
        return resources;
    }

    /**
     * Returns the canonical path of this module's root directory, plus a
     * trailing separator
     * 
     * @return a valid canonical path
     */
    public String getRoot() {
        return getPathLocation(Path.ROOT);
    }

    public String getSourceDirectory() {
        return sourceDirectory;
    }

    public String getTestSourceDirectory() {
        return testSourceDirectory;
    }

    /**
     * Returns the version number of this module or project
     * 
     * @return a non-blank version number
     */
    public String getVersion() {
        return gav.getVersion();
    }

    /**
     * Indicates whether this {@link Pom} has the given {@link Dependency},
     * ignoring the version number.
     * 
     * @param dependency the {@link Dependency} to check for (can be
     *            <code>null</code>)
     * @return <code>false</code> if a <code>null</code> dependency is given
     * @since 1.2.1
     */
    public boolean hasDependencyExcludingVersion(final Dependency dependency) {
        return !getDependenciesExcludingVersion(dependency).isEmpty();
    }

    /**
     * Indicates whether all of the given dependencies are registered, by
     * calling {@link #isDependencyRegistered(Dependency)} for each one,
     * ignoring any <code>null</code> elements.
     * 
     * @param dependencies the dependencies to check (can be <code>null</code>
     *            or contain <code>null</code> elements)
     * @return true if a <code>null</code> or empty collection is given
     */
    public boolean isAllDependenciesRegistered(
            final Collection<? extends Dependency> dependencies) {
        if (dependencies != null) {
            for (final Dependency dependency : dependencies) {
                if (dependency != null && !isDependencyRegistered(dependency)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Indicates whether all the given plugin repositories are registered, by
     * calling {@link #isPluginRepositoryRegistered(Repository)} for each one,
     * ignoring any <code>null</code> elements.
     * 
     * @param repositories the plugin repositories to check (can be
     *            <code>null</code>)
     * @return <code>true</code> if a <code>null</code> collection is given
     */
    public boolean isAllPluginRepositoriesRegistered(
            final Collection<? extends Repository> repositories) {
        if (repositories != null) {
            for (final Repository repository : repositories) {
                if (repository != null
                        && !isPluginRepositoryRegistered(repository)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Indicates whether all of the given plugins are registered, based on their
     * groupId, artifactId, and version.
     * 
     * @param plugins the plugins to check (required)
     * @return <code>false</code> if any of them are not registered
     */
    public boolean isAllPluginsRegistered(
            final Collection<? extends Plugin> plugins) {
        Validate.notNull(plugins, "Plugins to check is required");
        for (final Plugin plugin : plugins) {
            if (plugin != null && !isBuildPluginRegistered(plugin)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Indicates whether all the given repositories are registered. Equivalent
     * to calling {@link #isRepositoryRegistered(Repository)} for each one,
     * ignoring any <code>null</code> elements.
     * 
     * @param repositories the repositories to check (can be <code>null</code>)
     * @return true if a <code>null</code> collection is given
     */
    public boolean isAllRepositoriesRegistered(
            final Collection<? extends Repository> repositories) {
        if (repositories != null) {
            for (final Repository repository : repositories) {
                if (repository != null && !isRepositoryRegistered(repository)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Indicates whether any of the given dependencies are registered, by
     * calling {@link #isDependencyRegistered(Dependency)} for each one.
     * 
     * @param dependencies the dependencies to check (can be <code>null</code>)
     * @return see above
     */
    public boolean isAnyDependenciesRegistered(
            final Collection<? extends Dependency> dependencies) {
        if (dependencies != null) {
            for (final Dependency dependency : dependencies) {
                if (isDependencyRegistered(dependency)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Indicates whether any of the given plugins are registered, by calling
     * {@link #isBuildPluginRegistered(Plugin)} for each one.
     * 
     * @param plugins the plugins to check (required)
     * @return whether any of the plugins are currently registered or not
     */
    public boolean isAnyPluginsRegistered(
            final Collection<? extends Plugin> plugins) {
        Validate.notNull(plugins, "Plugins to check is required");
        for (final Plugin plugin : plugins) {
            if (isBuildPluginRegistered(plugin)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indicates whether the given build plugin is registered, based on its
     * groupId, artifactId, and version.
     * 
     * @param plugin to check (required)
     * @return whether the build plugin is currently registered or not
     * @deprecated use {@link #isPluginRegistered(GAV)} instead
     */
    @Deprecated
    public boolean isBuildPluginRegistered(final Plugin plugin) {
        return plugin != null && isPluginRegistered(plugin.getGAV());
    }

    /**
     * Indicates whether the given dependency is registered, by checking the
     * result of {@link Dependency#equals(Object)}.
     * 
     * @param dependency the dependency to check (can be <code>null</code>)
     * @return <code>false</code> if a <code>null</code> dependency is given
     */
    public boolean isDependencyRegistered(final Dependency dependency) {
        return dependency != null && dependencies.contains(dependency);
    }

    /**
     * Indicates whether the given filter is registered.
     * 
     * @param filter to check (required)
     * @return whether the filter is currently registered or not
     */
    public boolean isFilterRegistered(final Filter filter) {
        Validate.notNull(filter, "Filter to check is required");
        return filters.contains(filter);
    }

    /**
     * Indicates whether a plugin with the given coordinates is registered
     * 
     * @param coordinates the coordinates to match upon; can be
     *            <code>null</code>
     * @return false if <code>null</code> coordinates are given
     */
    public boolean isPluginRegistered(final GAV gav) {
        for (final Plugin existingPlugin : buildPlugins) {
            if (existingPlugin.getGAV().equals(gav)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indicates whether the given plugin repository is registered.
     * 
     * @param repository repository to check (can be <code>null</code>)
     * @return <code>false</code> if a <code>null</code> repository is given
     */
    public boolean isPluginRepositoryRegistered(final Repository repository) {
        return pluginRepositories.contains(repository);
    }

    /**
     * Indicates whether the given build property is registered.
     * 
     * @param property to check (required)
     * @return whether the property is currently registered or not
     */
    public boolean isPropertyRegistered(final Property property) {
        Validate.notNull(property, "Property to check is required");
        return pomProperties.contains(property);
    }

    /**
     * Indicates whether the given repository is registered.
     * 
     * @param repository to check (can be <code>null</code>)
     * @return <code>false</code> if a <code>null</code> repository is given
     */
    public boolean isRepositoryRegistered(final Repository repository) {
        return repositories.contains(repository);
    }

    /**
     * Indicates whether the given resource is registered.
     * 
     * @param resource to check (required)
     * @return whether the resource is currently registered or not
     */
    public boolean isResourceRegistered(final Resource resource) {
        Validate.notNull(resource, "Resource to check is required");
        return resources.contains(resource);
    }

    @Override
    public String toString() {
        // For debugging
        return gav + " at " + path;
    }
}
