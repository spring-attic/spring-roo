package org.springframework.roo.addon.gwt;

import java.util.List;
import java.util.Map;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.project.ProjectMetadata;

/**
 * Interface for {@link GwtTemplateServiceImpl}.
 *
 * @author James Tyrrell
 * @since 1.1.2
 */
public interface GwtTemplateService {

	GwtTemplateDataHolder getMirrorTemplateTypeDetails(ClassOrInterfaceTypeDetails governorTypeDetails, Map<JavaSymbolName, GwtProxyProperty> clientSideTypeMap, ProjectMetadata projectMetadata);

	List<ClassOrInterfaceTypeDetails> getStaticTemplateTypeDetails(GwtType type, ProjectMetadata projectMetadata);

	String buildUiXml(String templateContents, String destFile, List<MethodMetadata> proxyMethods);
}
