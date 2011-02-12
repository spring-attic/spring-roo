package org.springframework.roo.addon.gwt;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

import java.util.List;
import java.util.Map;

public interface GwtTemplatingService {

	GwtTemplateDataHolder getMirrorTemplateTypeDetails(ClassOrInterfaceTypeDetails governorTypeDetails);

	Map<JavaSymbolName, GwtProxyProperty> getClientSideTypeMap(List<MemberHoldingTypeDetails> memberHoldingTypeDetails, Map<JavaType, JavaType> gwtClientTypeMap);

	List<ClassOrInterfaceTypeDetails> getStaticTemplateTypeDetails(GwtType type);

	public Map<JavaType, JavaType> getClientTypeMap(ClassOrInterfaceTypeDetails governorTypeDetails);
}
