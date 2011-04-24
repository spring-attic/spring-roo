package org.springframework.roo.addon.gwt;

import java.util.List;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectMetadata;

/**
 * Interface for {@link GwtTypeServiceImpl}.
 *
 * @author James Tyrrell
 * @since 1.1.2
 */
public interface GwtTypeService {

	JavaType getGwtSideLeafType(JavaType type, ProjectMetadata projectMetadata, JavaType governorType, boolean requestType);

	List<MemberHoldingTypeDetails> getExtendsTypes(ClassOrInterfaceTypeDetails childType);

	List<ClassOrInterfaceTypeDetails> buildType(GwtType destType, ClassOrInterfaceTypeDetails templateClass, List<MemberHoldingTypeDetails> extendsTypes);

	void buildType(GwtType destType);

	List<MethodMetadata> getRequestMethods(ClassOrInterfaceTypeDetails governorTypeDetails);

	List<MethodMetadata> getProxyMethods(ClassOrInterfaceTypeDetails governorTypeDetails);

	boolean isDomainObject(JavaType type);

	boolean isMethodReturnTypesInSourcePath(MethodMetadata method, MemberHoldingTypeDetails memberHoldingTypeDetail, List<String> sourcePaths);

	List<String> getSourcePaths();

	boolean isGwtModuleXmlPresent();
}
