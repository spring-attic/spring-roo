package org.springframework.roo.addon.gwt;

import java.util.List;
import java.util.Map;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;

/**
 * Holder for types and xml files created via {@link GwtTemplateService}.
 *
 * @author James Tyrrell
 * @since 1.1.2
 */
public class GwtTemplateDataHolder {
	private final Map<GwtType, ClassOrInterfaceTypeDetails> templateTypeDetailsMap;
	private final Map<GwtType, String> xmlTemplates;
	private final List<ClassOrInterfaceTypeDetails> typeList;
	private final Map<String, String> xmlMap;

	public GwtTemplateDataHolder(Map<GwtType, ClassOrInterfaceTypeDetails> templateTypeDetailsMap, Map<GwtType, String> xmlTemplates, List<ClassOrInterfaceTypeDetails> typeList, Map<String, String> xmlMap) {
		this.templateTypeDetailsMap = templateTypeDetailsMap;
		this.xmlTemplates = xmlTemplates;
		this.typeList = typeList;
		this.xmlMap = xmlMap;
	}

	public Map<GwtType, ClassOrInterfaceTypeDetails> getTemplateTypeDetailsMap() {
		return templateTypeDetailsMap;
	}

	public Map<GwtType, String> getXmlTemplates() {
		return xmlTemplates;
	}

	public List<ClassOrInterfaceTypeDetails> getTypeList() {
		return typeList;
	}

	public Map<String, String> getXmlMap() {
		return xmlMap;
	}
}
