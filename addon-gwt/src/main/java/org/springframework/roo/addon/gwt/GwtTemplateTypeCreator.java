package org.springframework.roo.addon.gwt;

import hapax.*;
import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.MutablePhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GwtTemplateTypeCreator {

	private final MutablePhysicalTypeMetadataProvider physicalTypeMetadataProvider;
	private final MetadataService metadataService;
	private final ProjectMetadata projectMetadata;
	private final BeanInfoMetadata beanInfoMetadata;
	private final EntityMetadata entityMetadata;
	private final ClassOrInterfaceTypeDetails governorTypeDetails;
	private final Map<GwtType, JavaType> mirrorTypeMap;
	private final Map<JavaSymbolName, GwtProxyProperty> clientSideTypeMap;

	public GwtTemplateTypeCreator(MutablePhysicalTypeMetadataProvider physicalTypeMetadataProvider,
	                              MetadataService metadataService,
	                              ProjectMetadata projectMetadata,
	                              BeanInfoMetadata beanInfoMetadata,
	                              EntityMetadata entityMetadata,
	                              ClassOrInterfaceTypeDetails governorTypeDetails,
	                              Map<GwtType, JavaType> mirrorTypeMap,
	                              Map<JavaSymbolName, GwtProxyProperty> clientSideTypeMap) {

		this.physicalTypeMetadataProvider = physicalTypeMetadataProvider;
		this.metadataService = metadataService;
		this.projectMetadata = projectMetadata;
		this.beanInfoMetadata = beanInfoMetadata;
		this.entityMetadata = entityMetadata;
		this.governorTypeDetails = governorTypeDetails;
		this.mirrorTypeMap = mirrorTypeMap;
		this.clientSideTypeMap = clientSideTypeMap;
	}


	public Map<GwtType, ClassOrInterfaceTypeDetails> getTemplateTypeDetails() {
		Map<GwtType, ClassOrInterfaceTypeDetails> templateTypeDetailsMap = new HashMap<GwtType, ClassOrInterfaceTypeDetails>();
		for (GwtType gwtType : GwtType.getMirrorTypes()) {
			if (gwtType.getTemplate() == null) {
				continue;
			}
			TemplateDataDictionary dataDictionary = buildDataDictionary(gwtType);
			gwtType.dynamicallyResolveFieldsToWatch(clientSideTypeMap);
			gwtType.dynamicallyResolveMethodsToWatch(mirrorTypeMap.get(GwtType.PROXY), clientSideTypeMap, projectMetadata);
			templateTypeDetailsMap.put(gwtType, getTemplateDetails(dataDictionary, gwtType.getTemplate(), mirrorTypeMap.get(gwtType)));
		}
		return templateTypeDetailsMap;
	}

	public Map<GwtType, String> getXmlTemplates() {
		Map<GwtType, String> xmlTemplates = new HashMap<GwtType, String>();
		for (GwtType gwtType : GwtType.getMirrorTypes()) {
			if (gwtType.isCreateUiXml()) {
				TemplateDataDictionary dataDictionary = buildDataDictionary(gwtType);
				xmlTemplates.put(gwtType, getTemplateContents(gwtType.getTemplate() + "UiXml", gwtType, dataDictionary));
			}
		}

		return xmlTemplates;
	}

	private String getTemplateContents(String templateName, GwtType destType, TemplateDataDictionary dataDictionary) {

		try {
			if (dataDictionary == null) {
				dataDictionary = buildDataDictionary(destType);
			}
			TemplateLoader templateLoader = TemplateResourceLoader.create();
			Template template;
			template = templateLoader.getTemplate(templateName);
			return template.renderToString(dataDictionary);
		} catch (TemplateException e) {
			throw new IllegalStateException(e);
		}

	}

	private ClassOrInterfaceTypeDetails getTemplateDetails(TemplateDataDictionary dataDictionary, String templateFile, JavaType templateType) {

		try {
			TemplateLoader templateLoader = TemplateResourceLoader.create();
			Template template = templateLoader.getTemplate(templateFile);
			String templateContents = template.renderToString(dataDictionary);

			String templateId = PhysicalTypeIdentifier.createIdentifier(templateType, Path.SRC_MAIN_JAVA);

			return physicalTypeMetadataProvider.parse(templateContents, templateId, templateType);

		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private TemplateDataDictionary buildDataDictionary(GwtType type) {

		JavaType proxyType = mirrorTypeMap.get(GwtType.PROXY);
		JavaType javaType = mirrorTypeMap.get(type);

		TemplateDataDictionary dataDictionary = TemplateDictionary.create();

		for (GwtType reference : type.getReferences()) {
			addReference(dataDictionary, reference);
		}

		GwtUtils.addImport(dataDictionary, proxyType.getFullyQualifiedTypeName());

		dataDictionary.setVariable("className", javaType.getSimpleTypeName());
		dataDictionary.setVariable("packageName", javaType.getPackage().getFullyQualifiedPackageName());
		dataDictionary.setVariable("placePackage", GwtPath.SCAFFOLD_PLACE.packageName(projectMetadata));
		dataDictionary.setVariable("scaffoldUiPackage", GwtPath.SCAFFOLD_UI.packageName(projectMetadata));
		dataDictionary.setVariable("sharedScaffoldPackage", GwtPath.SHARED_SCAFFOLD.packageName(projectMetadata));
		dataDictionary.setVariable("uiPackage", GwtPath.MANAGED_UI.packageName(projectMetadata));
		dataDictionary.setVariable("name", governorTypeDetails.getName().getSimpleTypeName());
		dataDictionary.setVariable("pluralName", entityMetadata.getPlural());
		dataDictionary.setVariable("nameUncapitalized", StringUtils.uncapitalize(governorTypeDetails.getName().getSimpleTypeName()));
		dataDictionary.setVariable("proxy", proxyType.getSimpleTypeName());
		dataDictionary.setVariable("pluralName", entityMetadata.getPlural());
		dataDictionary.setVariable("proxyRenderer", GwtProxyProperty.getProxyRendererType(projectMetadata, proxyType));
		String proxyFields = null;
		GwtProxyProperty primaryProp = null;
		GwtProxyProperty secondaryProp = null;
		GwtProxyProperty dateProp = null;
		Set<String> importSet = new HashSet<String>();
		for (GwtProxyProperty property : clientSideTypeMap.values()) {

			// Determine if this is the primary property.
			if (primaryProp == null) {
				// Choose the first available field.
				primaryProp = property;
			} else if (property.isString() && !primaryProp.isString()) {
				// Favor String properties over other types.
				secondaryProp = primaryProp;
				primaryProp = property;
			} else if (secondaryProp == null) {
				// Choose the next available property.
				secondaryProp = property;
			} else if (property.isString() && !secondaryProp.isString()) {
				// Favor String properties over other types.
				secondaryProp = property;
			}

			// Determine if this is the first date property.
			if (dateProp == null && property.isDate()) {
				dateProp = property;
			}

			if (property.isProxy() || property.isCollectionOfProxy()) {
				if (proxyFields != null) {
					proxyFields += ", ";
				} else {
					proxyFields = "";
				}
				proxyFields += "\"" + property.getName() + "\"";
			}

			dataDictionary.addSection("fields").setVariable("field", property.getName());
			if (!isReadOnly(property.getName()))
				dataDictionary.addSection("editViewProps").setVariable("prop", property.forEditView());

			TemplateDataDictionary propertiesSection = dataDictionary.addSection("properties");
			propertiesSection.setVariable("prop", property.getName());
			propertiesSection.setVariable("propId", proxyType.getSimpleTypeName() + "_" + property.getName());
			propertiesSection.setVariable("propGetter", property.getGetter());
			propertiesSection.setVariable("propType", property.getType());
			propertiesSection.setVariable("propFormatter", property.getFormatter());
			propertiesSection.setVariable("propRenderer", property.getRenderer());
			propertiesSection.setVariable("propReadable", property.getReadableName());

			if (!isReadOnly(property.getName())) {
				TemplateDataDictionary editableSection = dataDictionary.addSection("editableProperties");
				editableSection.setVariable("prop", property.getName());
				editableSection.setVariable("propId", proxyType.getSimpleTypeName() + "_" + property.getName());
				editableSection.setVariable("propGetter", property.getGetter());
				editableSection.setVariable("propType", property.getType());
				editableSection.setVariable("propFormatter", property.getFormatter());
				editableSection.setVariable("propRenderer", property.getRenderer());
				editableSection.setVariable("propBinder", property.getBinder());
				editableSection.setVariable("propReadable", property.getReadableName());
			}

			dataDictionary.setVariable("proxyRendererType", GwtType.EDIT_RENDERER.getPath().packageName(projectMetadata) + "." + proxyType.getSimpleTypeName() + "Renderer");

			if (property.isProxy() || property.isEnum() || property.isCollectionOfProxy()) {
				TemplateDataDictionary section = dataDictionary.addSection(property.isEnum() ? "setEnumValuePickers" : "setProxyValuePickers");
				section.setVariable("setValuePicker", property.getSetValuePickerMethod());
				section.setVariable("setValuePickerName", property.getSetValuePickerMethodName());
				section.setVariable("valueType", property.getValueType().getSimpleTypeName());
				section.setVariable("rendererType", property.getProxyRendererType());
				if (property.isProxy() || property.isCollectionOfProxy()) {
					String propTypeName = StringUtils.uncapitalize(property.isCollectionOfProxy() ? property.getPropertyType().getParameters().get(0).getSimpleTypeName() : property.getPropertyType().getSimpleTypeName());
					propTypeName = propTypeName.substring(0, propTypeName.indexOf("Proxy"));
					section.setVariable("requestInterface", propTypeName + "Request");
					section.setVariable("findMethod", "find" + StringUtils.capitalize(propTypeName) + "Entries(0, 50)");
				}
				GwtUtils.maybeAddImport(dataDictionary, importSet, property.getPropertyType());
				if (property.isCollectionOfProxy()) {
					GwtUtils.maybeAddImport(dataDictionary, importSet,
							property.getPropertyType().getParameters().get(0));
					GwtUtils.maybeAddImport(dataDictionary, importSet, property.getSetEditorType());
				}

			}

		}

		dataDictionary.setVariable("proxyFields", proxyFields);

		// Add a section for the mobile properties.
		if (primaryProp != null) {
			dataDictionary.setVariable("primaryProp", primaryProp.getName());
			dataDictionary.setVariable("primaryPropGetter", primaryProp.getGetter());
			dataDictionary.setVariable("primaryPropBuilder", primaryProp.forMobileListView("primaryRenderer"));
			TemplateDataDictionary section = dataDictionary.addSection("mobileProperties");
			section.setVariable("prop", primaryProp.getName());
			section.setVariable("propGetter", primaryProp.getGetter());
			section.setVariable("propType", primaryProp.getType());
			section.setVariable("propRenderer", primaryProp.getRenderer());
			section.setVariable("propRendererName", "primaryRenderer");
		} else {
			dataDictionary.setVariable("primaryProp", "id");
			dataDictionary.setVariable("primaryPropGetter", "getId");
			dataDictionary.setVariable("primaryPropBuilder", "");
		}
		if (secondaryProp != null) {
			dataDictionary.setVariable("secondaryPropBuilder", secondaryProp.forMobileListView("secondaryRenderer"));
			TemplateDataDictionary section = dataDictionary.addSection("mobileProperties");
			section.setVariable("prop", secondaryProp.getName());
			section.setVariable("propGetter", secondaryProp.getGetter());
			section.setVariable("propType", secondaryProp.getType());
			section.setVariable("propRenderer", secondaryProp.getRenderer());
			section.setVariable("propRendererName", "secondaryRenderer");
		} else {
			dataDictionary.setVariable("secondaryPropBuilder", "");
		}
		if (dateProp != null) {
			dataDictionary.setVariable("datePropBuilder", dateProp.forMobileListView("dateRenderer"));
			TemplateDataDictionary section = dataDictionary.addSection("mobileProperties");
			section.setVariable("prop", dateProp.getName());
			section.setVariable("propGetter", dateProp.getGetter());
			section.setVariable("propType", dateProp.getType());
			section.setVariable("propRenderer", dateProp.getRenderer());
			section.setVariable("propRendererName", "dateRenderer");
		} else {
			dataDictionary.setVariable("datePropBuilder", "");
		}
		return dataDictionary;
	}

	private void addReference(TemplateDataDictionary dataDictionary, GwtType type) {
		GwtUtils.addImport(dataDictionary, mirrorTypeMap.get(type).getFullyQualifiedTypeName());
		dataDictionary.setVariable(type.getName(), mirrorTypeMap.get(type).getSimpleTypeName());
	}

	private boolean isReadOnly(String name) {
		FieldMetadata versionField = entityMetadata.getVersionField();
		FieldMetadata idField = entityMetadata.getIdentifierField();
		Assert.notNull(versionField, "Version unavailable for " + governorTypeDetails.getName() + " - required for GWT support");
		Assert.notNull(idField, "Id unavailable for " + governorTypeDetails.getName() + " - required for GWT support");
		JavaSymbolName versionPropertyName = versionField.getFieldName();
		JavaSymbolName idPropertyName = idField.getFieldName();
		return name.equals(idPropertyName.getSymbolName()) || name.equals(versionPropertyName.getSymbolName());
	}

}
