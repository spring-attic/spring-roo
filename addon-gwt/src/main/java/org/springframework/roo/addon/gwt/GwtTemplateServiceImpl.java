package org.springframework.roo.addon.gwt;

import hapax.Template;
import hapax.TemplateDataDictionary;
import hapax.TemplateDictionary;
import hapax.TemplateException;
import hapax.TemplateLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.MutablePhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Provides a basic implementation of {@link GwtTemplateService} which
 * is used to create {@link ClassOrInterfaceTypeDetails} objects from
 * source files created from templates. This class keeps all templating
 * concerns in one place.
 *
 * @author James Tyrrell
 * @since 1.1.2
 */
@Component
@Service
public class GwtTemplateServiceImpl implements GwtTemplateService {
	@Reference private FileManager fileManager;
	@Reference private MemberDetailsScanner memberDetailsScanner;
	@Reference private MetadataService metadataService;
	@Reference private MutablePhysicalTypeMetadataProvider physicalTypeMetadataProvider;
	@Reference private ProjectOperations projectOperations;

	public GwtTemplateDataHolder getMirrorTemplateTypeDetails(ClassOrInterfaceTypeDetails governorTypeDetails, Map<JavaSymbolName, GwtProxyProperty> clientSideTypeMap) {
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();
		Map<GwtType, JavaType> mirrorTypeMap = GwtUtils.getMirrorTypeMap(projectMetadata, governorTypeDetails.getName());

		Map<GwtType, ClassOrInterfaceTypeDetails> templateTypeDetailsMap = new HashMap<GwtType, ClassOrInterfaceTypeDetails>();
		Map<GwtType, String> xmlTemplates = new HashMap<GwtType, String>();
		for (GwtType gwtType : GwtType.getMirrorTypes()) {
			if (gwtType.getTemplate() == null) {
				continue;
			}
			TemplateDataDictionary dataDictionary = buildMirrorDataDictionary(gwtType, governorTypeDetails, mirrorTypeMap, clientSideTypeMap);
			gwtType.dynamicallyResolveFieldsToWatch(clientSideTypeMap);
			gwtType.dynamicallyResolveMethodsToWatch(mirrorTypeMap.get(GwtType.PROXY), clientSideTypeMap, projectMetadata);
			templateTypeDetailsMap.put(gwtType, getTemplateDetails(dataDictionary, gwtType.getTemplate(), mirrorTypeMap.get(gwtType)));

			if (gwtType.isCreateUiXml()) {
				dataDictionary = buildMirrorDataDictionary(gwtType, governorTypeDetails, mirrorTypeMap, clientSideTypeMap);
				String contents = getTemplateContents(gwtType.getTemplate() + "UiXml", dataDictionary);
				xmlTemplates.put(gwtType, contents);
			}
		}

		Map<String, String> xmlMap = new HashMap<String, String>();
		List<ClassOrInterfaceTypeDetails> typeDetails = new ArrayList<ClassOrInterfaceTypeDetails>();
		for (GwtProxyProperty proxyProperty : clientSideTypeMap.values()) {
			if (proxyProperty.isCollection() && !proxyProperty.isCollectionOfProxy()) {
				TemplateDataDictionary dataDictionary = TemplateDictionary.create();
				dataDictionary.setVariable("packageName", GwtPath.MANAGED_UI.packageName(projectMetadata));
				dataDictionary.setVariable("scaffoldUiPackage", GwtPath.SCAFFOLD_UI.packageName(projectMetadata));
				JavaType collectionTypeImpl = getCollectionImplementation(proxyProperty.getPropertyType());
				addImport(dataDictionary, collectionTypeImpl);
				addImport(dataDictionary, proxyProperty.getPropertyType());

				String collectionType = proxyProperty.getPropertyType().getSimpleTypeName();
				String boundCollectionType = proxyProperty.getPropertyType().getParameters().get(0).getSimpleTypeName();

				dataDictionary.setVariable("collectionType", collectionType);
				dataDictionary.setVariable("collectionTypeImpl", collectionTypeImpl.getSimpleTypeName());
				dataDictionary.setVariable("boundCollectionType", boundCollectionType);

				JavaType collectionEditorType = new JavaType(GwtPath.MANAGED_UI.packageName(projectMetadata) + "." + boundCollectionType + collectionType + "Editor");
				typeDetails.add(getTemplateDetails(dataDictionary, "CollectionEditor", collectionEditorType));

				dataDictionary = TemplateDictionary.create();
				dataDictionary.setVariable("packageName", GwtPath.MANAGED_UI.packageName(projectMetadata));
				dataDictionary.setVariable("scaffoldUiPackage", GwtPath.SCAFFOLD_UI.packageName(projectMetadata));
				dataDictionary.setVariable("collectionType", collectionType);
				dataDictionary.setVariable("collectionTypeImpl", collectionTypeImpl.getSimpleTypeName());
				dataDictionary.setVariable("boundCollectionType", boundCollectionType);
				addImport(dataDictionary, proxyProperty.getPropertyType());

				String contents = getTemplateContents("CollectionEditor" + "UiXml", dataDictionary);
				xmlMap.put(GwtPath.MANAGED_UI.canonicalFileSystemPath(projectMetadata) + "/" + boundCollectionType + collectionType + "Editor.ui.xml", contents);
			}
		}

		return new GwtTemplateDataHolder(templateTypeDetailsMap, xmlTemplates, typeDetails, xmlMap);
	}

	public List<ClassOrInterfaceTypeDetails> getStaticTemplateTypeDetails(GwtType type) {
		List<ClassOrInterfaceTypeDetails> templateTypeDetails = new ArrayList<ClassOrInterfaceTypeDetails>();
		TemplateDataDictionary dataDictionary = buildDictionary(type);
		templateTypeDetails.add(getTemplateDetails(dataDictionary, type.getTemplate(), getDestinationJavaType(type)));
		return templateTypeDetails;
	}

	private String getTemplateContents(String templateName, TemplateDataDictionary dataDictionary) {
		try {
			TemplateLoader templateLoader = TemplateResourceLoader.create();
			Template template = templateLoader.getTemplate(templateName);
			return template.renderToString(dataDictionary);
		} catch (TemplateException e) {
			throw new IllegalStateException(e);
		}
	}

	public ClassOrInterfaceTypeDetails getTemplateDetails(TemplateDataDictionary dataDictionary, String templateFile, JavaType templateType) {
		String templateContents = null;
		try {
			TemplateLoader templateLoader = TemplateResourceLoader.create();
			Template template = templateLoader.getTemplate(templateFile);
			templateContents = template.renderToString(dataDictionary);
			String templateId = PhysicalTypeIdentifier.createIdentifier(templateType, Path.SRC_MAIN_JAVA);
			return physicalTypeMetadataProvider.parse(templateContents, templateId, templateType);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private TemplateDataDictionary buildDictionary(GwtType type) {
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();
		TemplateDataDictionary dataDictionary = null;
		GwtType locate = GwtType.PROXY;
		String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
		
		switch (type) {
			case APP_ENTITY_TYPES_PROCESSOR: 
				dataDictionary = buildStandardDataDictionary(type);
				
				for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
					String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
					String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Proxy" suffix from filename

					dataDictionary.addSection("proxys").setVariable("proxy", fullPath);

					String entity1 = new StringBuilder("\t\tif (").append(fullPath).append(".class.equals(clazz)) {\n\t\t\tprocessor.handle").append(simpleName).append("((").append(fullPath).append(") null);\n\t\t\treturn;\n\t\t}").toString();
					dataDictionary.addSection("entities1").setVariable("entity", entity1);

					String entity2 = new StringBuilder("\t\tif (proxy instanceof ").append(fullPath).append(") {\n\t\t\tprocessor.handle").append(simpleName).append("((").append(fullPath).append(") proxy);\n\t\t\treturn;\n\t\t}").toString();
					dataDictionary.addSection("entities2").setVariable("entity", entity2);

					String entity3 = new StringBuilder("\tpublic abstract void handle").append(simpleName).append("(").append(fullPath).append(" proxy);").toString();
					dataDictionary.addSection("entities3").setVariable("entity", entity3);
				}
				break;
			case MASTER_ACTIVITIES: 
				dataDictionary = buildStandardDataDictionary(type);
				
				for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
					String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
					String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Proxy" suffix from filename
					TemplateDataDictionary section = dataDictionary.addSection("entities");
					section.setVariable("entitySimpleName", simpleName);
					section.setVariable("entityFullPath", fullPath);
					addImport(dataDictionary, simpleName, GwtType.LIST_ACTIVITY, projectMetadata);
					addImport(dataDictionary, simpleName, GwtType.PROXY, projectMetadata);
					addImport(dataDictionary, simpleName, GwtType.LIST_VIEW, projectMetadata);
					addImport(dataDictionary, simpleName, GwtType.MOBILE_LIST_VIEW, projectMetadata);
				}
				break;
			case APP_REQUEST_FACTORY: 
				dataDictionary = buildStandardDataDictionary(type);
				dataDictionary.setVariable("sharedScaffoldPackage", GwtPath.SHARED_SCAFFOLD.packageName(projectMetadata));

				for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
					String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
					String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Proxy" suffix from filename
					String entity = new StringBuilder("\t").append(simpleName).append("Request ").append(StringUtils.uncapitalize(simpleName)).append("Request();").toString();
					dataDictionary.addSection("entities").setVariable("entity", entity);
				}

				if (projectMetadata.isGaeEnabled()) {
					dataDictionary.showSection("gae");
				}
				break;
			case LIST_PLACE_RENDERER:
				dataDictionary = buildStandardDataDictionary(type);

				for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
					String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
					String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Proxy" suffix from filename
					TemplateDataDictionary section = dataDictionary.addSection("entities");
					section.setVariable("entitySimpleName", simpleName);
					section.setVariable("entityFullPath", fullPath);
					addImport(dataDictionary, GwtType.PROXY.getPath().packageName(projectMetadata) + "." + simpleName + GwtType.PROXY.getSuffix());
				}
				break;
			case DETAILS_ACTIVITIES:
				dataDictionary = buildStandardDataDictionary(type);

				for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
					String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
					String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Proxy" suffix from filename
					String entity = new StringBuilder("\t\t\tpublic void handle").append(simpleName).append("(").append(fullPath).append(" proxy) {\n").append("\t\t\t\tsetResult(new ").append(simpleName).append("ActivitiesMapper(requests, placeController).getActivity(proxyPlace));\n\t\t\t}").toString();
					dataDictionary.addSection("entities").setVariable("entity", entity);
					addImport(dataDictionary, GwtType.PROXY.getPath().packageName(projectMetadata) + "." + simpleName + GwtType.PROXY.getSuffix());
					addImport(dataDictionary, GwtType.ACTIVITIES_MAPPER.getPath().packageName(projectMetadata) + "." + simpleName + GwtType.ACTIVITIES_MAPPER.getSuffix());
				}
				break;
			case MOBILE_ACTIVITIES:
				dataDictionary = buildStandardDataDictionary(type);
				break;
		}
		
		return dataDictionary;
	}

	private TemplateDataDictionary buildStandardDataDictionary(GwtType type) {
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();

		JavaType javaType = new JavaType(getFullyQualifiedTypeName(type, projectMetadata));
		TemplateDataDictionary dataDictionary = TemplateDictionary.create();
		for (GwtType reference : type.getReferences()) {
			addReference(dataDictionary, reference);
		}
		dataDictionary.setVariable("className", javaType.getSimpleTypeName());
		dataDictionary.setVariable("packageName", javaType.getPackage().getFullyQualifiedPackageName());
		dataDictionary.setVariable("placePackage", GwtPath.SCAFFOLD_PLACE.packageName(projectMetadata));
		dataDictionary.setVariable("sharedScaffoldPackage", GwtPath.SHARED_SCAFFOLD.packageName(projectMetadata));
		dataDictionary.setVariable("sharedGaePackage", GwtPath.SHARED_GAE.packageName(projectMetadata));
		return dataDictionary;
	}

	private void addImport(TemplateDataDictionary dataDictionary, String simpleName, GwtType gwtType, ProjectMetadata projectMetadata) {
		addImport(dataDictionary, gwtType.getPath().packageName(projectMetadata) + "." + simpleName + gwtType.getSuffix());
	}

	private TemplateDataDictionary buildMirrorDataDictionary(GwtType type, ClassOrInterfaceTypeDetails governorTypeDetails, Map<GwtType, JavaType> mirrorTypeMap, Map<JavaSymbolName, GwtProxyProperty> clientSideTypeMap) {
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();
		JavaType proxyType = mirrorTypeMap.get(GwtType.PROXY);
		JavaType javaType = mirrorTypeMap.get(type);

		TemplateDataDictionary dataDictionary = TemplateDictionary.create();

		for (GwtType reference : type.getReferences()) {
			addReference(dataDictionary, reference, mirrorTypeMap);
		}

		addImport(dataDictionary, proxyType.getFullyQualifiedTypeName());

		String pluralMetadataKey = PluralMetadata.createIdentifier(governorTypeDetails.getName(), Path.SRC_MAIN_JAVA);
		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(pluralMetadataKey);
		String plural = pluralMetadata.getPlural();
		
		String simpleTypeName = governorTypeDetails.getName().getSimpleTypeName();

		dataDictionary.setVariable("className", javaType.getSimpleTypeName());
		dataDictionary.setVariable("packageName", javaType.getPackage().getFullyQualifiedPackageName());
		dataDictionary.setVariable("placePackage", GwtPath.SCAFFOLD_PLACE.packageName(projectMetadata));
		dataDictionary.setVariable("scaffoldUiPackage", GwtPath.SCAFFOLD_UI.packageName(projectMetadata));
		dataDictionary.setVariable("sharedScaffoldPackage", GwtPath.SHARED_SCAFFOLD.packageName(projectMetadata));
		dataDictionary.setVariable("uiPackage", GwtPath.MANAGED_UI.packageName(projectMetadata));
		dataDictionary.setVariable("name", simpleTypeName);
		dataDictionary.setVariable("pluralName", plural);
		dataDictionary.setVariable("nameUncapitalized", StringUtils.uncapitalize(simpleTypeName));
		dataDictionary.setVariable("proxy", proxyType.getSimpleTypeName());
		dataDictionary.setVariable("pluralName", plural);
		dataDictionary.setVariable("proxyRenderer", GwtProxyProperty.getProxyRendererType(projectMetadata, proxyType));
		String proxyFields = null;
		GwtProxyProperty primaryProperty = null;
		GwtProxyProperty secondaryProperty = null;
		GwtProxyProperty dateProperty = null;
		Set<String> importSet = new HashSet<String>();
		
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(getClass().getName(), governorTypeDetails);
		 
		for (GwtProxyProperty gwtProxyProperty : clientSideTypeMap.values()) {
			// Determine if this is the primary property.
			if (primaryProperty == null) {
				// Choose the first available field.
				primaryProperty = gwtProxyProperty;
			} else if (gwtProxyProperty.isString() && !primaryProperty.isString()) {
				// Favor String properties over other types.
				secondaryProperty = primaryProperty;
				primaryProperty = gwtProxyProperty;
			} else if (secondaryProperty == null) {
				// Choose the next available property.
				secondaryProperty = gwtProxyProperty;
			} else if (gwtProxyProperty.isString() && !secondaryProperty.isString()) {
				// Favor String properties over other types.
				secondaryProperty = gwtProxyProperty;
			}

			// Determine if this is the first date property.
			if (dateProperty == null && gwtProxyProperty.isDate()) {
				dateProperty = gwtProxyProperty;
			}

			if (gwtProxyProperty.isProxy() || gwtProxyProperty.isCollectionOfProxy()) {
				if (proxyFields != null) {
					proxyFields += ", ";
				} else {
					proxyFields = "";
				}
				proxyFields += "\"" + gwtProxyProperty.getName() + "\"";
			}

			dataDictionary.addSection("fields").setVariable("field", gwtProxyProperty.getName());
			if (!isReadOnly(gwtProxyProperty.getName(), governorTypeDetails, memberDetails)) {
				dataDictionary.addSection("editViewProps").setVariable("prop", gwtProxyProperty.forEditView());
			}

			TemplateDataDictionary propertiesSection = dataDictionary.addSection("properties");
			propertiesSection.setVariable("prop", gwtProxyProperty.getName());
			propertiesSection.setVariable("propId", proxyType.getSimpleTypeName() + "_" + gwtProxyProperty.getName());
			propertiesSection.setVariable("propGetter", gwtProxyProperty.getGetter());
			propertiesSection.setVariable("propType", gwtProxyProperty.getType());
			propertiesSection.setVariable("propFormatter", gwtProxyProperty.getFormatter());
			propertiesSection.setVariable("propRenderer", gwtProxyProperty.getRenderer());
			propertiesSection.setVariable("propReadable", gwtProxyProperty.getReadableName());

			if (!isReadOnly(gwtProxyProperty.getName(), governorTypeDetails, memberDetails)) {
				TemplateDataDictionary editableSection = dataDictionary.addSection("editableProperties");
				editableSection.setVariable("prop", gwtProxyProperty.getName());
				editableSection.setVariable("propId", proxyType.getSimpleTypeName() + "_" + gwtProxyProperty.getName());
				editableSection.setVariable("propGetter", gwtProxyProperty.getGetter());
				editableSection.setVariable("propType", gwtProxyProperty.getType());
				editableSection.setVariable("propFormatter", gwtProxyProperty.getFormatter());
				editableSection.setVariable("propRenderer", gwtProxyProperty.getRenderer());
				editableSection.setVariable("propBinder", gwtProxyProperty.getBinder());
				editableSection.setVariable("propReadable", gwtProxyProperty.getReadableName());
			}

			dataDictionary.setVariable("proxyRendererType", proxyType.getSimpleTypeName() + "Renderer");

			if (gwtProxyProperty.isProxy() || gwtProxyProperty.isEnum() || gwtProxyProperty.isCollectionOfProxy()) {
				TemplateDataDictionary section = dataDictionary.addSection(gwtProxyProperty.isEnum() ? "setEnumValuePickers" : "setProxyValuePickers");
				section.setVariable("setValuePicker", gwtProxyProperty.getSetValuePickerMethod());
				section.setVariable("setValuePickerName", gwtProxyProperty.getSetValuePickerMethodName());
				section.setVariable("valueType", gwtProxyProperty.getValueType().getSimpleTypeName());
				section.setVariable("rendererType", gwtProxyProperty.getProxyRendererType());
				if (gwtProxyProperty.isProxy() || gwtProxyProperty.isCollectionOfProxy()) {
					String propTypeName = StringUtils.uncapitalize(gwtProxyProperty.isCollectionOfProxy() ? gwtProxyProperty.getPropertyType().getParameters().get(0).getSimpleTypeName() : gwtProxyProperty.getPropertyType().getSimpleTypeName());
					propTypeName = propTypeName.substring(0, propTypeName.indexOf("Proxy"));
					section.setVariable("requestInterface", propTypeName + "Request");
					section.setVariable("findMethod", "find" + StringUtils.capitalize(propTypeName) + "Entries(0, 50)");
				}
				maybeAddImport(dataDictionary, importSet, gwtProxyProperty.getPropertyType());
				maybeAddImport(dataDictionary, importSet, gwtProxyProperty.getValueType());
				if (gwtProxyProperty.isCollectionOfProxy()) {
					maybeAddImport(dataDictionary, importSet, gwtProxyProperty.getPropertyType().getParameters().get(0));
					maybeAddImport(dataDictionary, importSet, gwtProxyProperty.getSetEditorType());
				}
			}
		}

		dataDictionary.setVariable("proxyFields", proxyFields);

		// Add a section for the mobile properties.
		if (primaryProperty != null) {
			dataDictionary.setVariable("primaryProp", primaryProperty.getName());
			dataDictionary.setVariable("primaryPropGetter", primaryProperty.getGetter());
			dataDictionary.setVariable("primaryPropBuilder", primaryProperty.forMobileListView("primaryRenderer"));
			TemplateDataDictionary section = dataDictionary.addSection("mobileProperties");
			section.setVariable("prop", primaryProperty.getName());
			section.setVariable("propGetter", primaryProperty.getGetter());
			section.setVariable("propType", primaryProperty.getType());
			section.setVariable("propRenderer", primaryProperty.getRenderer());
			section.setVariable("propRendererName", "primaryRenderer");
		} else {
			dataDictionary.setVariable("primaryProp", "id");
			dataDictionary.setVariable("primaryPropGetter", "getId");
			dataDictionary.setVariable("primaryPropBuilder", "");
		}
		if (secondaryProperty != null) {
			dataDictionary.setVariable("secondaryPropBuilder", secondaryProperty.forMobileListView("secondaryRenderer"));
			TemplateDataDictionary section = dataDictionary.addSection("mobileProperties");
			section.setVariable("prop", secondaryProperty.getName());
			section.setVariable("propGetter", secondaryProperty.getGetter());
			section.setVariable("propType", secondaryProperty.getType());
			section.setVariable("propRenderer", secondaryProperty.getRenderer());
			section.setVariable("propRendererName", "secondaryRenderer");
		} else {
			dataDictionary.setVariable("secondaryPropBuilder", "");
		}
		if (dateProperty != null) {
			dataDictionary.setVariable("datePropBuilder", dateProperty.forMobileListView("dateRenderer"));
			TemplateDataDictionary section = dataDictionary.addSection("mobileProperties");
			section.setVariable("prop", dateProperty.getName());
			section.setVariable("propGetter", dateProperty.getGetter());
			section.setVariable("propType", dateProperty.getType());
			section.setVariable("propRenderer", dateProperty.getRenderer());
			section.setVariable("propRendererName", "dateRenderer");
		} else {
			dataDictionary.setVariable("datePropBuilder", "");
		}
		return dataDictionary;
	}

	private void addReference(TemplateDataDictionary dataDictionary, GwtType type, Map<GwtType, JavaType> mirrorTypeMap) {
		addImport(dataDictionary, mirrorTypeMap.get(type).getFullyQualifiedTypeName());
		dataDictionary.setVariable(type.getName(), mirrorTypeMap.get(type).getSimpleTypeName());
	}

	private JavaType getDestinationJavaType(GwtType destType) {
		return new JavaType(getFullyQualifiedTypeName(destType, projectOperations.getProjectMetadata()));
	}

	private void addReference(TemplateDataDictionary dataDictionary, GwtType type) {
		addImport(dataDictionary, getDestinationJavaType(type).getFullyQualifiedTypeName());
		dataDictionary.setVariable(type.getName(), getDestinationJavaType(type).getSimpleTypeName());
	}

	private boolean isReadOnly(String name, ClassOrInterfaceTypeDetails governorTypeDetails, MemberDetails memberDetails) {
		List<FieldMetadata> idFields = MemberFindingUtils.getFieldsWithTag(memberDetails, PersistenceCustomDataKeys.IDENTIFIER_FIELD);
		Assert.isTrue(!idFields.isEmpty(), "Id unavailable for '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "' - required for GWT support");
		FieldMetadata idField = idFields.get(0);
		JavaSymbolName idPropertyName = idField.getFieldName();
		
		List<FieldMetadata> versionFields = MemberFindingUtils.getFieldsWithTag(memberDetails, PersistenceCustomDataKeys.VERSION_FIELD);
		Assert.isTrue(!versionFields.isEmpty(), "Version unavailable for '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "' - required for GWT support");
		FieldMetadata versionField = versionFields.get(0);
		JavaSymbolName versionPropertyName = versionField.getFieldName();
		
		return name.equals(idPropertyName.getSymbolName()) || name.equals(versionPropertyName.getSymbolName());
	}

	private void addImport(TemplateDataDictionary dataDictionary, String importDeclaration) {
		dataDictionary.addSection("imports").setVariable("import", importDeclaration);
	}

	private void maybeAddImport(TemplateDataDictionary dataDictionary, Set<String> importSet, JavaType type) {
		if (!importSet.contains(type.getFullyQualifiedTypeName())) {
			addImport(dataDictionary, type.getFullyQualifiedTypeName());
			importSet.add(type.getFullyQualifiedTypeName());
		}
	}

	private void addImport(TemplateDataDictionary dataDictionary, JavaType type) {
		dataDictionary.addSection("imports").setVariable("import", type.getFullyQualifiedTypeName());
		for (JavaType param : type.getParameters()) {
			addImport(dataDictionary, param);
		}
	}

	private JavaType getCollectionImplementation(JavaType javaType) {
		if (javaType.getFullyQualifiedTypeName().equals("java.util.Set")) {
			return new JavaType("java.util.HashSet", javaType.getArray(), javaType.getDataType(), javaType.getArgName(), javaType.getParameters());
		}

		if (javaType.getFullyQualifiedTypeName().equals("java.util.List")) {
			return new JavaType("java.util.ArrayList", javaType.getArray(), javaType.getDataType(), javaType.getArgName(), javaType.getParameters());
		}

		return javaType;
	}
	
	private String getFullyQualifiedTypeName(GwtType gwtType, ProjectMetadata projectMetadata) {
		return gwtType.getPath().packageName(projectMetadata) + "." + gwtType.getTemplate();
	}
}
