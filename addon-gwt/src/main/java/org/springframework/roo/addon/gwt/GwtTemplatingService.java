package org.springframework.roo.addon.gwt;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaSymbolName;

import java.util.List;
import java.util.Map;

public interface GwtTemplatingService {

	GwtTemplateDataHolder getMirrorTemplateTypeDetails(ClassOrInterfaceTypeDetails governorTypeDetails, Map<JavaSymbolName, GwtProxyProperty> clientSideTypeMap);

	List<ClassOrInterfaceTypeDetails> getStaticTemplateTypeDetails(GwtType type);

}
