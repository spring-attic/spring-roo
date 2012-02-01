package org.springframework.roo.project;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.project.maven.Pom;

/**
 * Automatically upgrades a Spring Roo annotation JAR to the current version of
 * Roo. If the annotation JAR is equal to or newer than the version of Roo
 * running, the upgrade service makes no changes.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component
public class AutomaticProjectUpgradeService implements
        MetadataNotificationListener {

    private static class VersionInfo implements Comparable<VersionInfo> {

        private Integer major = 0;
        private Integer minor = 0;
        private Integer patch = 0;
        private String qualifier = "";

        public int compareTo(final VersionInfo v) {
            if (v == null) {
                throw new NullPointerException("VersionInfo is null");
            }
            int result = major.compareTo(v.major);
            if (result != 0) {
                return result;
            }
            result = minor.compareTo(v.minor);
            if (result != 0) {
                return result;
            }
            result = patch.compareTo(v.patch);
            if (result != 0) {
                return result;
            }
            result = qualifier.compareTo(v.qualifier);
            if (result != 0) {
                return result;
            }
            return 0;
        }

        @Override
        public String toString() {
            return major + "." + minor + "." + patch + "." + qualifier;
        }
    }

    private static final String MY_BUNDLE_SYMBOLIC_NAME = AutomaticProjectUpgradeService.class
            .getPackage().getName();

    private static final String SPRING_VERSION = "3.1.0.RELEASE";
    private VersionInfo bundleVersionInfo;
    @Reference private MetadataDependencyRegistry metadataDependencyRegistry;
    @Reference private ProjectOperations projectOperations;

    protected void activate(final ComponentContext componentContext) {
        metadataDependencyRegistry.addNotificationListener(this);
        for (final Bundle b : componentContext.getBundleContext().getBundles()) {
            if (!MY_BUNDLE_SYMBOLIC_NAME.equals(b.getSymbolicName())) {
                continue;
            }
            final Object v = b.getHeaders().get("Bundle-Version");
            if (v != null) {
                final String version = v.toString();
                bundleVersionInfo = extractVersionInfoFromString(version);
            }
            break;
        }
    }

    protected void deactivate(final ComponentContext componentContext) {
        metadataDependencyRegistry.removeNotificationListener(this);
    }

    /**
     * Extracts the version information from the string. Never throws an
     * exception.
     * 
     * @param version to extract from (can be null or empty)
     * @return the version information or null if it was not in a normal form
     */
    private VersionInfo extractVersionInfoFromString(final String version) {
        if (StringUtils.isBlank(version)) {
            return null;
        }

        final String[] ver = version.split("\\.");
        try {
            if (ver.length == 4) {
                final VersionInfo result = new VersionInfo();
                result.major = new Integer(ver[0]);
                result.minor = new Integer(ver[1]);
                result.patch = new Integer(ver[2]);
                result.qualifier = ver[3];
                return result;
            }
        }
        catch (final RuntimeException e) {
            return null;
        }
        return null;
    }

    public void notify(final String upstreamDependency,
            final String downstreamDependency) {
        if (bundleVersionInfo != null
                && ProjectMetadata.isValid(upstreamDependency)) {
            final String moduleName = ProjectMetadata
                    .getModuleName(upstreamDependency);
            // Project Metadata available.
            if (!projectOperations.isProjectAvailable(moduleName)) {
                return;
            }

            for (final Pom pom : projectOperations.getPoms()) {
                final Set<Property> rooVersionResults = pom
                        .getPropertiesExcludingValue(new Property("roo.version"));
                for (final Property existingProperty : rooVersionResults) {
                    final VersionInfo rooVersion = extractVersionInfoFromString(existingProperty
                            .getValue());
                    if (rooVersion != null) {
                        if (rooVersion.compareTo(bundleVersionInfo) < 0) {
                            final Property newProperty = new Property(
                                    existingProperty.getName(),
                                    bundleVersionInfo.toString());
                            projectOperations.addProperty(moduleName,
                                    newProperty);
                            break;
                        }
                    }
                }

                final Set<Property> springVersionResults = pom
                        .getPropertiesExcludingValue(new Property(
                                "spring.version"));
                for (final Property existingProperty : springVersionResults) {
                    final VersionInfo springVersion = extractVersionInfoFromString(existingProperty
                            .getValue());
                    if (springVersion != null) {
                        final VersionInfo latestSpringVersion = extractVersionInfoFromString(SPRING_VERSION);
                        if (springVersion.compareTo(latestSpringVersion) < 0) {
                            final Property newProperty = new Property(
                                    existingProperty.getName(),
                                    latestSpringVersion.toString());
                            projectOperations.addProperty(moduleName,
                                    newProperty);
                            break;
                        }
                    }
                }
            }
        }
    }
}
