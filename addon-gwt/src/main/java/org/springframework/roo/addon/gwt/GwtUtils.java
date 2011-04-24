package org.springframework.roo.addon.gwt;

import java.util.HashMap;
import java.util.Map;

import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectMetadata;

/**
 * Utility methods used in the GWT Add-On.
 *
 * @author James Tyrrell
 * @since 1.1.2
 */
public class GwtUtils {

	private GwtUtils() {
	}

	public static Map<GwtType, JavaType> getMirrorTypeMap(ProjectMetadata projectMetadata, JavaType governorType) {
		Map<GwtType, JavaType> mirrorTypeMap = new HashMap<GwtType, JavaType>();
		for (GwtType mirrorType : GwtType.values()) {
			mirrorTypeMap.put(mirrorType, convertGovernorTypeNameIntoKeyTypeName(governorType, mirrorType, projectMetadata));
		}
		return mirrorTypeMap;
	}

	public static JavaType convertGovernorTypeNameIntoKeyTypeName(JavaType governorType, GwtType type, ProjectMetadata projectMetadata) {
		String destinationPackage = type.getPath().packageName(projectMetadata);
		String typeName;
		if (type.isMirrorType()) {
			String simple = governorType.getSimpleTypeName();
			typeName = destinationPackage + "." + simple + type.getSuffix();
		} else {
			typeName = destinationPackage + "." + type.getTemplate();
		}
		return new JavaType(typeName);
	}
}
