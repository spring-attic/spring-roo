package org.springframework.roo.addon.gwt;

import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectMetadata;

public interface GwtTypeNamingStrategy {

	/**
	 * Returns the type name that should be created for a particular mirror type for a particular governor.
	 *
	 * @param type             the mirror type to create (required)
	 * @param projectMetadata  the project to create the mirror in (required)
	 * @param governorTypeName the governor this mirror is for (required)
	 * @return the type (never null)
	 */
	JavaType convertGovernorTypeNameIntoKeyTypeName(GwtType type, ProjectMetadata projectMetadata, JavaType governorTypeName);
}
