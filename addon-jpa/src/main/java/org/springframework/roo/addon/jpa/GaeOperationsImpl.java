package org.springframework.roo.addon.jpa;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;

/**
 * Implementation of {@link GaeOperations}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component
@Service
public class GaeOperationsImpl implements GaeOperations {

    @Reference private ProjectOperations projectOperations;

    public String getName() {
        return FeatureNames.GAE;
    }

    public boolean isInstalledInModule(final String moduleName) {
        final Pom pom = projectOperations.getPomFromModuleName(moduleName);
        if (pom == null) {
            return false;
        }
        for (final Plugin buildPlugin : pom.getBuildPlugins()) {
            if ("maven-gae-plugin".equals(buildPlugin.getArtifactId())) {
                return true;
            }
        }
        return false;
    }
}
