package org.springframework.roo.addon.gwt;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectMetadata;

import java.util.List;

public interface GwtTypeService {

	JavaType getGwtSideLeafType(JavaType type, ProjectMetadata projectMetadata, JavaType governorType, boolean requestType);

	List<MemberHoldingTypeDetails> getExtendsTypes(ClassOrInterfaceTypeDetails childType);
}
