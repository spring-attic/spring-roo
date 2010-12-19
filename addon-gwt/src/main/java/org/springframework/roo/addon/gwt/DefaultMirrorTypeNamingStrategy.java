package org.springframework.roo.addon.gwt;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectMetadata;

/**
 * Provides a basic implementation of {@link MirrorTypeNamingStrategy}.
 *
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class DefaultMirrorTypeNamingStrategy implements MirrorTypeNamingStrategy {

    public JavaType convertGovernorTypeNameIntoKeyTypeName(MirrorType type, ProjectMetadata projectMetadata, JavaType governorTypeName) {
        String simple = governorTypeName.getSimpleTypeName();
        String destinationPackage = type.getPath().packageName(projectMetadata);
        return new JavaType(destinationPackage + "." + simple + type.getSuffix());
    }
}
