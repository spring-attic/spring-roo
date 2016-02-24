package org.springframework.roo.project.maven;

import static org.springframework.roo.project.maven.Pom.DEFAULT_PACKAGING;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Filter;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.Repository;
import org.springframework.roo.project.Resource;
import org.springframework.roo.project.packaging.PackagingProvider;
import org.springframework.roo.project.packaging.PackagingProviderRegistry;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

@Component
@Service
public class PomFactoryImpl implements PomFactory {

    private static final String ARTIFACT_ID_XPATH = "/project/artifactId";
    private static final String DEFAULT_RELATIVE_PATH = "../pom.xml";
    private static final String DEPENDENCY_XPATH = "/project/dependencies/dependency";
    private static final String FILTER_XPATH = "/project/build/filters/filter";
    private static final String GROUP_ID_XPATH = "/project/groupId";
    private static final String MODULE_XPATH = "/project/modules/module";
    private static final String NAME_XPATH = "/project/name";
    private static final String PACKAGING_PROVIDER_PROPERTY_XPATH = "/project/properties/roo.packaging.provider";
    private static final String PACKAGING_XPATH = "/project/packaging";
    private static final String PARENT_GROUP_ID_XPATH = "/project/parent/groupId";
    private static final String PARENT_XPATH = "/project/parent";
    private static final String PLUGIN_REPOSITORY_XPATH = "/project/pluginRepositories/pluginRepository";
    private static final String PLUGIN_XPATH = "/project/build/plugins/plugin";
    private static final String PROPERTY_XPATH = "/project/properties/*";
    private static final String REPOSITORY_XPATH = "/project/repositories/repository";
    private static final String RESOURCE_XPATH = "/project/build/resources/resource";
    private static final String SOURCE_DIRECTORY_XPATH = "/project/build/sourceDirectory";
    private static final String TEST_SOURCE_DIRECTORY_XPATH = "/project/build/testSourceDirectory";
    private static final String VERSION_XPATH = "/project/version";

    @Reference PackagingProviderRegistry packagingProviderRegistry;

    /**
     * Returns the groupId defined in the given POM
     * 
     * @param root the POM's root element (required)
     * @return a non-blank groupId
     */
    private String getGroupId(final Element root) {
        final String projectGroupId = XmlUtils.getTextContent(GROUP_ID_XPATH,
                root);
        if (StringUtils.isNotBlank(projectGroupId)) {
            return projectGroupId;
        }
        // Fall back to a group ID assumed to be the same as any possible
        // <parent> (ROO-1193)
        return XmlUtils.getTextContent(PARENT_GROUP_ID_XPATH, root);
    }

    public Pom getInstance(final Element root, final String pomPath,
            final String moduleName) {
        Validate.notBlank(pomPath, "POM's canonical path is required");
        final String artifactId = XmlUtils.getTextContent(ARTIFACT_ID_XPATH,
                root);
        final String groupId = getGroupId(root);
        final String name = XmlUtils.getTextContent(NAME_XPATH, root);
        final String packaging = XmlUtils.getTextContent(PACKAGING_XPATH, root,
                DEFAULT_PACKAGING);
        String version = XmlUtils.getTextContent(VERSION_XPATH, root);
        final String sourceDirectory = XmlUtils.getTextContent(
                SOURCE_DIRECTORY_XPATH, root);
        final String testSourceDirectory = XmlUtils.getTextContent(
                TEST_SOURCE_DIRECTORY_XPATH, root);
        final List<Dependency> dependencies = parseElements(Dependency.class,
                DEPENDENCY_XPATH, root);
        final List<Filter> filters = parseElements(Filter.class, FILTER_XPATH,
                root);
        final List<Module> modules = getModules(root, pomPath, packaging);
        final List<Plugin> plugins = parseElements(Plugin.class, PLUGIN_XPATH,
                root);
        final List<Property> pomProperties = parseElements(Property.class,
                PROPERTY_XPATH, root);
        final List<Repository> pluginRepositories = parseElements(
                Repository.class, PLUGIN_REPOSITORY_XPATH, root);
        final List<Repository> repositories = parseElements(Repository.class,
                REPOSITORY_XPATH, root);
        final List<Resource> resources = parseElements(Resource.class,
                RESOURCE_XPATH, root);
		final String projectParentVersion = XmlUtils.getTextContent("/project/parent/version", root);
        final Parent parent = getParent(pomPath, root);
		if(version == null) {
			version = projectParentVersion;
		}
        final Collection<Path> paths = getPaths(root, packaging);
        return new Pom(groupId, artifactId, version, packaging, dependencies,
                parent, modules, pomProperties, name, repositories,
                pluginRepositories, sourceDirectory, testSourceDirectory,
                filters, plugins, resources, pomPath, moduleName, paths);
    }

    private List<Module> getModules(final Element root, final String pomPath,
            final String packaging) {
        if (!"pom".equalsIgnoreCase(packaging)) {
            return null;
        }
        final List<Module> modules = new ArrayList<Module>();
        for (final Element module : XmlUtils.findElements(MODULE_XPATH, root)) {
            final String moduleName = module.getTextContent();
            if (StringUtils.isNotBlank(moduleName)) {
                final String modulePath = resolveRelativePath(pomPath,
                        moduleName);
                modules.add(new Module(moduleName, modulePath));
            }
        }
        return modules;
    }

    private Parent getParent(final String pomPath, final Element root) {
        final Element parentElement = XmlUtils.findFirstElement(PARENT_XPATH,
                root);
        if (parentElement == null) {
            return null;
        }
        final String relativePath = XmlUtils.getTextContent("/relativePath",
                parentElement, DEFAULT_RELATIVE_PATH);
        final String parentPomPath = resolveRelativePath(pomPath, relativePath);
        return new ParentBuilder(parentElement, parentPomPath).build();
    }

    private Collection<Path> getPaths(final Element root, final String packaging) {
        final String packagingProviderId = XmlUtils.getTextContent(
                PACKAGING_PROVIDER_PROPERTY_XPATH, root, packaging);
        final PackagingProvider packagingProvider = packagingProviderRegistry
                .getPackagingProvider(packagingProviderId);
        Validate.notNull(packagingProvider,
                "No PackagingProvider found with the ID '%s'",
                packagingProviderId);
        return packagingProvider.getPaths();
    }

    /**
     * Parses any elements matching the given XPath expression into instances of
     * the given type.
     * 
     * @param <T> the type of object to parse
     * @param type the type of object to parse; must have a constructor that
     *            accepts an {@link Element} as its sole argument
     * @param xPath the XPath expression to apply (required)
     * @param root the root of the XML document being searched (required)
     * @return a non-<code>null</code> list
     */
    private <T> List<T> parseElements(final Class<T> type, final String xPath,
            final Element root) {
        final List<T> results = new ArrayList<T>();
        for (final Element element : XmlUtils.findElements(xPath, root)) {
            try {
                results.add(type.getConstructor(Element.class).newInstance(
                        element));
            }
            catch (final RuntimeException e) {
                throw e;
            }
            catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
        return results;
    }

    private String resolveRelativePath(String relativeTo,
            final String relativePath) {
        relativeTo = StringUtils.removeEnd(relativeTo, File.separator);
        while (new File(relativeTo).isFile()) {
            relativeTo = relativeTo.substring(0,
                    relativeTo.lastIndexOf(File.separator));
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
            sb.append(File.separator);
        }

        while (backCount > 0) {
            relativeTo = relativeTo.substring(0,
                    relativeTo.lastIndexOf(File.separatorChar));
            backCount--;
        }
        String path = relativeTo + File.separator + sb.toString();
        if (new File(path).isDirectory()) {
            path = path + "pom.xml";
        }
        return path;
    }
}
