package org.springframework.roo.addon.gwt;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectMetadata;

/**
 * Provides a basic implementation of {@link GwtTypeNamingStrategy}.
 *
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class DefaultGwtTypeNamingStrategy implements GwtTypeNamingStrategy {

	public JavaType convertGovernorTypeNameIntoKeyTypeName(GwtType type, ProjectMetadata projectMetadata, JavaType governorTypeName) {
		String destinationPackage = type.getPath().packageName(projectMetadata);
		String typeName;
		if (type.isMirrorType()) {
			String simple = governorTypeName.getSimpleTypeName();
			typeName = destinationPackage + "." + simple + type.getSuffix();
		} else {
			typeName = destinationPackage + "." + type.getTemplate();
		}
		return new JavaType(typeName);
	}
}
