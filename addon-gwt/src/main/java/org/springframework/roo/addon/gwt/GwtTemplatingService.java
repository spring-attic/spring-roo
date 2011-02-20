package org.springframework.roo.addon.gwt;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;

import java.util.List;

public interface GwtTemplatingService {

	GwtTemplateDataHolder getMirrorTemplateTypeDetails(ClassOrInterfaceTypeDetails governorTypeDetails);

	List<ClassOrInterfaceTypeDetails> getStaticTemplateTypeDetails(GwtType type);

}
