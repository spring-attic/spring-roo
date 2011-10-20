package org.springframework.roo.addon.gwt;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Interface for {@link GwtTypeServiceImpl}.
 *
 * @author James Tyrrell
 * @since 1.1.2
 */
public interface GwtTypeService {

	JavaType getGwtSideLeafType(JavaType returnType, JavaType governorType, boolean requestType, boolean convertPrimitive);

	List<MemberHoldingTypeDetails> getExtendsTypes(ClassOrInterfaceTypeDetails childType);

	List<ClassOrInterfaceTypeDetails> buildType(GwtType destType, ClassOrInterfaceTypeDetails templateClass, List<MemberHoldingTypeDetails> extendsTypes);

	void buildType(GwtType destType, List<ClassOrInterfaceTypeDetails> templateTypeDetails, String moduleName);

	List<MethodMetadata> getRequestMethods(ClassOrInterfaceTypeDetails governorTypeDetails);

	Map<JavaSymbolName, MethodMetadata> getProxyMethods(ClassOrInterfaceTypeDetails governorTypeDetails);

	boolean isDomainObject(JavaType type);

	boolean isMethodReturnTypesInSourcePath(MethodMetadata method, MemberHoldingTypeDetails memberHoldingTypeDetail, Set<String> sourcePaths);

	Set<String> getSourcePaths(String moduleName);

	ClassOrInterfaceTypeDetails lookupRequestFromProxy(ClassOrInterfaceTypeDetails proxy);

	ClassOrInterfaceTypeDetails lookupProxyFromRequest(ClassOrInterfaceTypeDetails request);

	ClassOrInterfaceTypeDetails lookupRequestFromEntity(ClassOrInterfaceTypeDetails entity);

	ClassOrInterfaceTypeDetails lookupProxyFromEntity(ClassOrInterfaceTypeDetails entity);

	ClassOrInterfaceTypeDetails lookupEntityFromProxy(ClassOrInterfaceTypeDetails proxy);

	ClassOrInterfaceTypeDetails lookupEntityFromRequest(ClassOrInterfaceTypeDetails request);

	ClassOrInterfaceTypeDetails lookupEntityFromLocator(ClassOrInterfaceTypeDetails request);

	ClassOrInterfaceTypeDetails lookupTargetServiceFromRequest(ClassOrInterfaceTypeDetails request);
}
