package org.springframework.roo.addon.gwt;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hapax.Template;
import hapax.TemplateDataDictionary;
import hapax.TemplateDictionary;
import hapax.TemplateException;
import hapax.TemplateLoader;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.gwt.scaffold.GwtScaffoldMetadata;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeParsingService;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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

	// Constants
	private static final int LAYER_POSITION = LayerType.HIGHEST.getPosition();
	
	// Fields
	@Reference protected MetadataService metadataService;
	@Reference protected ProjectOperations projectOperations;
	@Reference protected TypeParsingService typeParsingService;
	@Reference protected TypeLocationService typeLocationService;
	@Reference protected GwtTypeService gwtTypeService;
	@Reference protected LayerService layerService;
	@Reference protected PersistenceMemberLocator persistenceMemberLocator;
	@Reference protected MemberDetailsScanner memberDetailsScanner;

	public GwtTemplateDataHolder getMirrorTemplateTypeDetails(ClassOrInterfaceTypeDetails mirroredType, Map<JavaSymbolName, GwtProxyProperty> clientSideTypeMap) {
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();
		ClassOrInterfaceTypeDetails proxy = gwtTypeService.lookupProxyFromEntity(mirroredType);
		ClassOrInterfaceTypeDetails request = gwtTypeService.lookupRequestFromEntity(mirroredType);
		Map<GwtType, JavaType> mirrorTypeMap = GwtUtils.getMirrorTypeMap(projectMetadata, mirroredType.getName());
		mirrorTypeMap.put(GwtType.PROXY, proxy.getName());
		mirrorTypeMap.put(GwtType.REQUEST, request.getName());

		Map<GwtType, ClassOrInterfaceTypeDetails> templateTypeDetailsMap = new HashMap<GwtType, ClassOrInterfaceTypeDetails>();
		Map<GwtType, String> xmlTemplates = new HashMap<GwtType, String>();
		for (GwtType gwtType : GwtType.getMirrorTypes()) {
			if (gwtType.getTemplate() == null) {
				continue;
			}
			TemplateDataDictionary dataDictionary = buildMirrorDataDictionary(gwtType, mirroredType, proxy, mirrorTypeMap, clientSideTypeMap);
			gwtType.dynamicallyResolveFieldsToWatch(clientSideTypeMap);
			gwtType.dynamicallyResolveMethodsToWatch(mirroredType.getName(), clientSideTypeMap, projectMetadata);
			templateTypeDetailsMap.put(gwtType, getTemplateDetails(dataDictionary, gwtType.getTemplate(), mirrorTypeMap.get(gwtType)));

			if (gwtType.isCreateUiXml()) {
				dataDictionary = buildMirrorDataDictionary(gwtType, mirroredType, proxy, mirrorTypeMap, clientSideTypeMap);
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

	public String buildUiXml(String templateContents, String destFile, List<MethodMetadata> proxyMethods) {
		try {
			DocumentBuilder builder = XmlUtils.getDocumentBuilder();
			builder.setEntityResolver(new EntityResolver() {
				public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
					if (systemId.equals("http://dl.google.com/gwt/DTD/xhtml.ent")) {
						return new InputSource(TemplateUtils.getTemplate(GwtScaffoldMetadata.class, "templates/xhtml.ent"));
					}

					// Use the default behaviour
					return null;
				}
			});

			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(templateContents));

			Document templateDocument = builder.parse(is);

			if (!new File(destFile).exists()) {
				return transformXml(templateDocument);
			}

			is = new InputSource();
			FileReader fileReader = new FileReader(destFile);
			is.setCharacterStream(fileReader);
			Document existingDocument = builder.parse(is);
			fileReader.close();

			//Look for the element holder denoted by the 'debugId' attribute first
			Element existingHoldingElement = XmlUtils.findFirstElement("//*[@debugId='" + "boundElementHolder" + "']", existingDocument.getDocumentElement());
			Element templateHoldingElement = XmlUtils.findFirstElement("//*[@debugId='" + "boundElementHolder" + "']", templateDocument.getDocumentElement());

			//If holding element isn't found then the holding element is either not widget based or using the old convention of 'id' so look for the element holder with an 'id' attribute
			if (existingHoldingElement == null) {
				existingHoldingElement = XmlUtils.findFirstElement("//*[@id='" + "boundElementHolder" + "']", existingDocument.getDocumentElement());
			}
			if (templateHoldingElement == null) {
				templateHoldingElement = XmlUtils.findFirstElement("//*[@id='" + "boundElementHolder" + "']", templateDocument.getDocumentElement());
			}

			if (existingHoldingElement != null) {
				HashMap<String, Element> templateElementMap = new LinkedHashMap<String, Element>();
				for (Element element : XmlUtils.findElements("//*[@id]", templateHoldingElement)) {
					templateElementMap.put(element.getAttribute("id"), element);
				}

				HashMap<String, Element> existingElementMap = new LinkedHashMap<String, Element>();
				for (Element element : XmlUtils.findElements("//*[@id]", existingHoldingElement)) {
					existingElementMap.put(element.getAttribute("id"), element);
				}

				if (existingElementMap.keySet().containsAll(templateElementMap.values())) {
					return transformXml(existingDocument);
				}

				ArrayList<Element> elementsToAdd = new ArrayList<Element>();
				for (Map.Entry<String, Element> entry : templateElementMap.entrySet()) {
					if (!existingElementMap.keySet().contains(entry.getKey())) {
						elementsToAdd.add(entry.getValue());
					}
				}

				ArrayList<Element> elementsToRemove = new ArrayList<Element>();
				for (Map.Entry<String, Element> entry : existingElementMap.entrySet()) {
					if (!templateElementMap.keySet().contains(entry.getKey())) {
						elementsToRemove.add(entry.getValue());
					}
				}

				for (Element element : elementsToAdd) {
					Node importedNode = existingDocument.importNode(element, true);
					existingHoldingElement.appendChild(importedNode);
				}

				for (Element element : elementsToRemove) {
					existingHoldingElement.removeChild(element);
				}

				if (elementsToAdd.size() > 0) {
					List<Element> sortedElements = new ArrayList<Element>();
					for (MethodMetadata method : proxyMethods) {
						String propertyName = StringUtils.uncapitalize(BeanInfoUtils.getPropertyNameForJavaBeanMethod(method).getSymbolName());
						Element element = XmlUtils.findFirstElement("//*[@id='" + propertyName + "']", existingHoldingElement);
						if (element != null) {
							sortedElements.add(element);
						}
					}
					for (Element el : sortedElements) {
						if (el.getParentNode() != null && el.getParentNode().equals(existingHoldingElement)) {
							existingHoldingElement.removeChild(el);
						}
					}

					for (Element el : sortedElements) {
						existingHoldingElement.appendChild(el);
					}
				}

				return transformXml(existingDocument);
			}

			return transformXml(templateDocument);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private String transformXml(Document document) throws TransformerException {
		Transformer transformer = XmlUtils.createIndentingTransformer();
		StreamResult result = new StreamResult(new StringWriter());
		DOMSource source = new DOMSource(document);
		transformer.transform(source, result);
		return result.getWriter().toString();
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
		String templateContents;
		try {
			TemplateLoader templateLoader = TemplateResourceLoader.create();
			Template template = templateLoader.getTemplate(templateFile);
			templateContents = template.renderToString(dataDictionary);
			String templateId = PhysicalTypeIdentifier.createIdentifier(templateType, Path.SRC_MAIN_JAVA);
			return typeParsingService.getTypeFromString(templateContents, templateId, templateType);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private TemplateDataDictionary buildDictionary(GwtType type) {
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();

		Set<ClassOrInterfaceTypeDetails> proxies = typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(GwtUtils.PROXY_ANNOTATIONS);
		TemplateDataDictionary dataDictionary = buildStandardDataDictionary(type);
		switch (type) {
			case APP_ENTITY_TYPES_PROCESSOR: 

				
				for (ClassOrInterfaceTypeDetails proxy : proxies) {
					String proxySimpleName = proxy.getName().getSimpleTypeName();
				   	String entitySimpleName = gwtTypeService.lookupEntityFromProxy(proxy).getName().getSimpleTypeName();

					dataDictionary.addSection("proxys").setVariable("proxy", proxySimpleName);

					String entity1 = new StringBuilder("\t\tif (").append(proxySimpleName).append(".class.equals(clazz)) {\n\t\t\tprocessor.handle").append(entitySimpleName).append("((").append(proxySimpleName).append(") null);\n\t\t\treturn;\n\t\t}").toString();
					dataDictionary.addSection("entities1").setVariable("entity", entity1);

					String entity2 = new StringBuilder("\t\tif (proxy instanceof ").append(proxySimpleName).append(") {\n\t\t\tprocessor.handle").append(entitySimpleName).append("((").append(proxySimpleName).append(") proxy);\n\t\t\treturn;\n\t\t}").toString();
					dataDictionary.addSection("entities2").setVariable("entity", entity2);

					String entity3 = new StringBuilder("\tpublic abstract void handle").append(entitySimpleName).append("(").append(proxySimpleName).append(" proxy);").toString();
					dataDictionary.addSection("entities3").setVariable("entity", entity3);
					addImport(dataDictionary, proxy.getName().getFullyQualifiedTypeName());
				}
				break;
			case MASTER_ACTIVITIES:
				for (ClassOrInterfaceTypeDetails proxy : proxies) {
					String proxySimpleName = proxy.getName().getSimpleTypeName();
					String entitySimpleName = gwtTypeService.lookupEntityFromProxy(proxy).getName().getSimpleTypeName();
					TemplateDataDictionary section = dataDictionary.addSection("entities");
					section.setVariable("entitySimpleName", entitySimpleName);
					section.setVariable("entityFullPath", proxySimpleName);
					addImport(dataDictionary, entitySimpleName, GwtType.LIST_ACTIVITY, projectMetadata);
					addImport(dataDictionary, proxy.getName().getFullyQualifiedTypeName());
					addImport(dataDictionary, entitySimpleName, GwtType.LIST_VIEW, projectMetadata);
					addImport(dataDictionary, entitySimpleName, GwtType.MOBILE_LIST_VIEW, projectMetadata);
				}
				break;
			case APP_REQUEST_FACTORY: 
				dataDictionary.setVariable("sharedScaffoldPackage", GwtPath.SHARED_SCAFFOLD.packageName(projectMetadata));

				for (ClassOrInterfaceTypeDetails proxy : proxies) {
					String entitySimpleName = gwtTypeService.lookupEntityFromProxy(proxy).getName().getSimpleTypeName();
					ClassOrInterfaceTypeDetails request = gwtTypeService.lookupRequestFromProxy(proxy);
					String entity = new StringBuilder("\t").append(request.getName().getSimpleTypeName()).append(" ").append(StringUtils.uncapitalize(entitySimpleName)).append("Request();").toString();
					dataDictionary.addSection("entities").setVariable("entity", entity);
					addImport(dataDictionary, request.getName().getFullyQualifiedTypeName());
				}

				if (projectMetadata.isGaeEnabled()) {
					dataDictionary.showSection("gae");
				}
				break;
			case LIST_PLACE_RENDERER:

				for (ClassOrInterfaceTypeDetails proxy : proxies) {
					String entitySimpleName = gwtTypeService.lookupEntityFromProxy(proxy).getName().getSimpleTypeName();
					String proxySimpleName = proxy.getName().getSimpleTypeName();
					TemplateDataDictionary section = dataDictionary.addSection("entities");
					section.setVariable("entitySimpleName", entitySimpleName);
					section.setVariable("entityFullPath", proxySimpleName);
					addImport(dataDictionary, proxy.getName().getFullyQualifiedTypeName());
				}
				break;
			case DETAILS_ACTIVITIES:

				for (ClassOrInterfaceTypeDetails proxy : proxies) {
					String proxySimpleName = proxy.getName().getSimpleTypeName();
					String entitySimpleName = gwtTypeService.lookupEntityFromProxy(proxy).getName().getSimpleTypeName();
					String entity = new StringBuilder("\t\t\tpublic void handle").append(entitySimpleName).append("(").append(proxySimpleName).append(" proxy) {\n").append("\t\t\t\tsetResult(new ").append(entitySimpleName).append("ActivitiesMapper(requests, placeController).getActivity(proxyPlace));\n\t\t\t}").toString();
					dataDictionary.addSection("entities").setVariable("entity", entity);
					addImport(dataDictionary, proxy.getName().getFullyQualifiedTypeName());
					addImport(dataDictionary, GwtType.ACTIVITIES_MAPPER.getPath().packageName(projectMetadata) + "." + entitySimpleName + GwtType.ACTIVITIES_MAPPER.getSuffix());
				}
				break;
			case MOBILE_ACTIVITIES:
				//Do nothing
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

	private String getRequestMethodCall(ClassOrInterfaceTypeDetails request, MemberTypeAdditions memberTypeAdditions) {
		String methodName = memberTypeAdditions.getMethodName();
		MethodMetadata requestMethod = MemberFindingUtils.getMethod(request, methodName);
		String requestMethodCall = memberTypeAdditions.getMethodName();
		if (requestMethod != null) {
			if (GwtUtils.INSTANCE_REQUEST.getFullyQualifiedTypeName().equals(requestMethod.getReturnType().getFullyQualifiedTypeName())) {
				requestMethodCall = requestMethodCall + "().using";
			}
		}
		return requestMethodCall;
	}

	private TemplateDataDictionary buildMirrorDataDictionary(GwtType type, ClassOrInterfaceTypeDetails mirroredType, ClassOrInterfaceTypeDetails proxy, Map<GwtType, JavaType> mirrorTypeMap, Map<JavaSymbolName, GwtProxyProperty> clientSideTypeMap) {
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();
		JavaType proxyType = proxy.getName();
		JavaType javaType = mirrorTypeMap.get(type);

		TemplateDataDictionary dataDictionary = TemplateDictionary.create();

		// Get my locator and
		JavaType entity = mirroredType.getName();
		String metadataIdentificationString = mirroredType.getDeclaredByMetadataId();
		final JavaType idType = persistenceMemberLocator.getIdentifierType(entity);
		if (idType == null) {
			return null;
		}

		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(getClass().getName(), mirroredType);
		ClassOrInterfaceTypeDetails request = gwtTypeService.lookupRequestFromProxy(proxy);

		MethodMetadata identifierAccessorMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.IDENTIFIER_ACCESSOR_METHOD);
		final MethodMetadata versionAccessorMethod = persistenceMemberLocator.getVersionAccessor(entity);
		MemberTypeAdditions countMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, PersistenceCustomDataKeys.COUNT_ALL_METHOD.name(), entity, idType, LAYER_POSITION);
		MemberTypeAdditions findMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, PersistenceCustomDataKeys.FIND_METHOD.name(), entity, idType, LAYER_POSITION, new MethodParameter(idType, "id"));
		MemberTypeAdditions findAllMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, PersistenceCustomDataKeys.FIND_ALL_METHOD.name(), entity, idType, LAYER_POSITION);
		final MethodParameter firstResultParameter = new MethodParameter(JavaType.INT_PRIMITIVE, "firstResult");
		final MethodParameter maxResultsParameter = new MethodParameter(JavaType.INT_PRIMITIVE, "maxResults");
		MemberTypeAdditions findEntriesMethod = layerService.getMemberTypeAdditions(metadataIdentificationString, PersistenceCustomDataKeys.FIND_ENTRIES_METHOD.name(), entity, idType, LAYER_POSITION, firstResultParameter, maxResultsParameter);
		final MethodParameter entityParameter = new MethodParameter(entity, "proxy");
		MemberTypeAdditions flushMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, PersistenceCustomDataKeys.FLUSH_METHOD.name(), entity, idType, LAYER_POSITION, entityParameter);
		MemberTypeAdditions mergeMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, PersistenceCustomDataKeys.MERGE_METHOD.name(), entity, idType, LAYER_POSITION, entityParameter);
		MemberTypeAdditions persistMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, PersistenceCustomDataKeys.PERSIST_METHOD.name(), entity, idType, LAYER_POSITION, entityParameter);
		MemberTypeAdditions removeMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, PersistenceCustomDataKeys.REMOVE_METHOD.name(), entity, idType, LAYER_POSITION, entityParameter);
		String persistMethodSignature = getRequestMethodCall(request, persistMethodAdditions);
		String removeMethodSignature = getRequestMethodCall(request, removeMethodAdditions);
		dataDictionary.setVariable("persistMethodSignature", persistMethodSignature);
		dataDictionary.setVariable("removeMethodSignature", removeMethodSignature);
		dataDictionary.setVariable("countEntitiesMethod", countMethodAdditions.getMethodName());

		for (GwtType reference : type.getReferences()) {
			addReference(dataDictionary, reference, mirrorTypeMap);
		}

		addImport(dataDictionary, proxyType.getFullyQualifiedTypeName());

		String pluralMetadataKey = PluralMetadata.createIdentifier(mirroredType.getName(), Path.SRC_MAIN_JAVA);
		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(pluralMetadataKey);
		String plural = pluralMetadata.getPlural();
		
		String simpleTypeName = mirroredType.getName().getSimpleTypeName();

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
			if (!isReadOnly(gwtProxyProperty.getName(), mirroredType)) {
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

			if (!isReadOnly(gwtProxyProperty.getName(), mirroredType)) {
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

	private boolean isReadOnly(String name, ClassOrInterfaceTypeDetails governorTypeDetails) {
		List<String> readOnly = new ArrayList<String>();
		ClassOrInterfaceTypeDetails proxy = gwtTypeService.lookupProxyFromEntity(governorTypeDetails);
		if (proxy != null) {
			readOnly.addAll(GwtUtils.getAnnotationValues(proxy, RooJavaType.ROO_GWT_PROXY, "readOnly"));
		}
		
		return readOnly.contains(name);
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
