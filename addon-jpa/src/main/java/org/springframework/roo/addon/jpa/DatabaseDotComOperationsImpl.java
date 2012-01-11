package org.springframework.roo.addon.jpa;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;

/**
 * Implementation of {@link DatabaseDotComOperations}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component
@Service
public class DatabaseDotComOperationsImpl implements DatabaseDotComOperations {

    @Reference private ProjectOperations projectOperations;

    public String getName() {
        return FeatureNames.DATABASE_DOT_COM;
    }

    public boolean isInstalledInModule(final String moduleName) {
        final Pom pom = projectOperations.getPomFromModuleName(moduleName);
        if (pom == null) {
            return false;
        }
        for (final Plugin buildPlugin : pom.getBuildPlugins()) {
            if ("com.force.sdk".equals(buildPlugin.getArtifactId())) {
                return true;
            }
        }
        return false;
    }
}
