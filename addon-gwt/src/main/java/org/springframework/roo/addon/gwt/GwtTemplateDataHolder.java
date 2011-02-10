package org.springframework.roo.addon.gwt;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;

import java.util.Map;

/**
 * Holder for types and xml files created via {@link GwtTemplatingService}.
 *
 * @author James Tyrrell
 * @since 1.1.2
 */

public class GwtTemplateDataHolder {

	private final Map<GwtType, ClassOrInterfaceTypeDetails> templateTypeDetailsMap;
	private Map<GwtType, ClassOrInterfaceTypeDetails> abstractTypeDetailsMap;
	private final Map<GwtType, String> xmlTemplates;

	public GwtTemplateDataHolder(Map<GwtType, ClassOrInterfaceTypeDetails> templateTypeDetailsMap, Map<GwtType, String> xmlTemplates) {
		this.templateTypeDetailsMap = templateTypeDetailsMap;
		this.xmlTemplates = xmlTemplates;
	}

	public GwtTemplateDataHolder(Map<GwtType, ClassOrInterfaceTypeDetails> templateTypeDetailsMap,
	                             Map<GwtType, ClassOrInterfaceTypeDetails> abstractTypeDetailsMap,
	                             Map<GwtType, String> xmlTemplates) {
		this.templateTypeDetailsMap = templateTypeDetailsMap;
		this.abstractTypeDetailsMap = abstractTypeDetailsMap;
		this.xmlTemplates = xmlTemplates;
	}

	public Map<GwtType, ClassOrInterfaceTypeDetails> getTemplateTypeDetailsMap() {
		return templateTypeDetailsMap;
	}

	public Map<GwtType, String> getXmlTemplates() {
		return xmlTemplates;
	}

	public Map<GwtType, ClassOrInterfaceTypeDetails> getAbstractTypeDetailsMap() {
		return abstractTypeDetailsMap;
	}
}
