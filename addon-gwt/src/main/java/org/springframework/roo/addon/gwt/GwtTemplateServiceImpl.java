package org.springframework.roo.addon.gwt;

import static org.springframework.roo.model.JdkJavaType.ARRAY_LIST;
import static org.springframework.roo.model.JdkJavaType.HASH_SET;
import static org.springframework.roo.model.JdkJavaType.LIST;
import static org.springframework.roo.model.JdkJavaType.SET;
import hapax.Template;
import hapax.TemplateDataDictionary;
import hapax.TemplateDictionary;
import hapax.TemplateException;
import hapax.TemplateLoader;

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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.gwt.scaffold.GwtScaffoldMetadata;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeParsingService;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.ContextualPath;
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

	public GwtTemplateDataHolder getMirrorTemplateTypeDetails(final ClassOrInterfaceTypeDetails mirroredType, final Map<JavaSymbolName, GwtProxyProperty> clientSideTypeMap, final ProjectMetadata projectMetadata) {
		ClassOrInterfaceTypeDetails proxy = gwtTypeService.lookupProxyFromEntity(mirroredType);
		ClassOrInterfaceTypeDetails request = gwtTypeService.lookupRequestFromEntity(mirroredType);
		Map<GwtType, JavaType> mirrorTypeMap = GwtUtils.getMirrorTypeMap(projectOperations, mirroredType.getName());
		mirrorTypeMap.put(GwtType.PROXY, proxy.getName());
		mirrorTypeMap.put(GwtType.REQUEST, request.getName());

		Map<GwtType, ClassOrInterfaceTypeDetails> templateTypeDetailsMap = new HashMap<GwtType, ClassOrInterfaceTypeDetails>();
		Map<GwtType, String> xmlTemplates = new HashMap<GwtType, String>();
		for (GwtType gwtType : GwtType.getMirrorTypes()) {
			if (gwtType.getTemplate() == null) {
				continue;
			}
			TemplateDataDictionary dataDictionary = buildMirrorDataDictionary(gwtType, mirroredType, proxy, mirrorTypeMap, clientSideTypeMap, projectMetadata);
			gwtType.dynamicallyResolveFieldsToWatch(clientSideTypeMap);
			gwtType.dynamicallyResolveMethodsToWatch(mirroredType.getName(), clientSideTypeMap, projectOperations);
			templateTypeDetailsMap.put(gwtType, getTemplateDetails(dataDictionary, gwtType.getTemplate(), mirrorTypeMap.get(gwtType)));

			if (gwtType.isCreateUiXml()) {
				dataDictionary = buildMirrorDataDictionary(gwtType, mirroredType, proxy, mirrorTypeMap, clientSideTypeMap, projectMetadata);
				String contents = getTemplateContents(gwtType.getTemplate() + "UiXml", dataDictionary);
				xmlTemplates.put(gwtType, contents);
			}
		}
		String moduleName = PhysicalTypeIdentifier.getPath(proxy.getDeclaredByMetadataId()).getModule();
		JavaPackage topLevelPackage = projectOperations.getTopLevelPackage(moduleName);
		Map<String, String> xmlMap = new HashMap<String, String>();
		List<ClassOrInterfaceTypeDetails> typeDetails = new ArrayList<ClassOrInterfaceTypeDetails>();
		for (GwtProxyProperty proxyProperty : clientSideTypeMap.values()) {
			if (proxyProperty.isCollection() && !proxyProperty.isCollectionOfProxy()) {
				TemplateDataDictionary dataDictionary = TemplateDictionary.create();
				dataDictionary.setVariable("packageName", GwtPath.MANAGED_UI.packageName(topLevelPackage));
				dataDictionary.setVariable("scaffoldUiPackage", GwtPath.SCAFFOLD_UI.packageName(topLevelPackage));
				JavaType collectionTypeImpl = getCollectionImplementation(proxyProperty.getPropertyType());
				addImport(dataDictionary, collectionTypeImpl);
				addImport(dataDictionary, proxyProperty.getPropertyType());

				String collectionType = proxyProperty.getPropertyType().getSimpleTypeName();
				String boundCollectionType = proxyProperty.getPropertyType().getParameters().get(0).getSimpleTypeName();

				dataDictionary.setVariable("collectionType", collectionType);
				dataDictionary.setVariable("collectionTypeImpl", collectionTypeImpl.getSimpleTypeName());
				dataDictionary.setVariable("boundCollectionType", boundCollectionType);

				JavaType collectionEditorType = new JavaType(GwtPath.MANAGED_UI.packageName(topLevelPackage) + "." + boundCollectionType + collectionType + "Editor");
				typeDetails.add(getTemplateDetails(dataDictionary, "CollectionEditor", collectionEditorType));

				dataDictionary = TemplateDictionary.create();
				dataDictionary.setVariable("packageName", GwtPath.MANAGED_UI.packageName(topLevelPackage));
				dataDictionary.setVariable("scaffoldUiPackage", GwtPath.SCAFFOLD_UI.packageName(topLevelPackage));
				dataDictionary.setVariable("collectionType", collectionType);
				dataDictionary.setVariable("collectionTypeImpl", collectionTypeImpl.getSimpleTypeName());
				dataDictionary.setVariable("boundCollectionType", boundCollectionType);
				addImport(dataDictionary, proxyProperty.getPropertyType());

				String contents = getTemplateContents("CollectionEditor" + "UiXml", dataDictionary);
				xmlMap.put(GwtPath.MANAGED_UI.canonicalFileSystemPath(projectOperations) + "/" + boundCollectionType + collectionType + "Editor.ui.xml", contents);
			}
		}

		return new GwtTemplateDataHolder(templateTypeDetailsMap, xmlTemplates, typeDetails, xmlMap);
	}

	public List<ClassOrInterfaceTypeDetails> getStaticTemplateTypeDetails(final GwtType type, final ProjectMetadata projectMetadata) {
		List<ClassOrInterfaceTypeDetails> templateTypeDetails = new ArrayList<ClassOrInterfaceTypeDetails>();
		TemplateDataDictionary dataDictionary = buildDictionary(type, projectMetadata);
		templateTypeDetails.add(getTemplateDetails(dataDictionary, type.getTemplate(), getDestinationJavaType(type, projectMetadata)));
		return templateTypeDetails;
	}

	public String buildUiXml(final String templateContents, final String destFile, final List<MethodMetadata> proxyMethods) {
		try {
			DocumentBuilder builder = XmlUtils.getDocumentBuilder();
			builder.setEntityResolver(new EntityResolver() {
				public InputSource resolveEntity(final String publicId, final String systemId) throws SAXException, IOException {
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

			// Look for the element holder denoted by the 'debugId' attribute first
			Element existingHoldingElement = XmlUtils.findFirstElement("//*[@debugId='" + "boundElementHolder" + "']", existingDocument.getDocumentElement());
			Element templateHoldingElement = XmlUtils.findFirstElement("//*[@debugId='" + "boundElementHolder" + "']", templateDocument.getDocumentElement());

			// If holding element isn't found then the holding element is either not widget based or using the old convention of 'id' so look for the element holder with an 'id' attribute
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

	private String transformXml(final Document document) throws TransformerException {
		Transformer transformer = XmlUtils.createIndentingTransformer();
		StreamResult result = new StreamResult(new StringWriter());
		DOMSource source = new DOMSource(document);
		transformer.transform(source, result);
		return result.getWriter().toString();
	}

	private String getTemplateContents(final String templateName, final TemplateDataDictionary dataDictionary) {
		try {
			TemplateLoader templateLoader = TemplateResourceLoader.create();
			Template template = templateLoader.getTemplate(templateName);
			return template.renderToString(dataDictionary);
		} catch (TemplateException e) {
			throw new IllegalStateException(e);
		}
	}

	public ClassOrInterfaceTypeDetails getTemplateDetails(final TemplateDataDictionary dataDictionary, final String templateFile, final JavaType templateType) {
		String templateContents;
		try {
			TemplateLoader templateLoader = TemplateResourceLoader.create();
			Template template = templateLoader.getTemplate(templateFile);
			templateContents = template.renderToString(dataDictionary);
			ContextualPath contextualPath = projectOperations.getPathResolver().getFocusedPath(Path.SRC_MAIN_JAVA);
			String templateId = PhysicalTypeIdentifier.createIdentifier(templateType, contextualPath);
			return typeParsingService.getTypeFromString(templateContents, templateId, templateType);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private TemplateDataDictionary buildDictionary(final GwtType type, final ProjectMetadata projectMetadata) {
		Set<ClassOrInterfaceTypeDetails> proxies = typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_GWT_PROXY);
		TemplateDataDictionary dataDictionary = buildStandardDataDictionary(type, projectMetadata);
		switch (type) {
			case APP_ENTITY_TYPES_PROCESSOR:
				for (ClassOrInterfaceTypeDetails proxy : proxies) {
					if (!GwtUtils.scaffoldProxy(proxy)) {
						continue;
					}
					String proxySimpleName = proxy.getName().getSimpleTypeName();
					ClassOrInterfaceTypeDetails entity = gwtTypeService.lookupEntityFromProxy(proxy);
					if (entity != null) {
						String entitySimpleName = entity.getName().getSimpleTypeName();

						dataDictionary.addSection("proxys").setVariable("proxy", proxySimpleName);

						String entity1 = new StringBuilder("\t\tif (").append(proxySimpleName).append(".class.equals(clazz)) {\n\t\t\tprocessor.handle").append(entitySimpleName).append("((").append(proxySimpleName).append(") null);\n\t\t\treturn;\n\t\t}").toString();
						dataDictionary.addSection("entities1").setVariable("entity", entity1);

						String entity2 = new StringBuilder("\t\tif (proxy instanceof ").append(proxySimpleName).append(") {\n\t\t\tprocessor.handle").append(entitySimpleName).append("((").append(proxySimpleName).append(") proxy);\n\t\t\treturn;\n\t\t}").toString();
						dataDictionary.addSection("entities2").setVariable("entity", entity2);

						String entity3 = new StringBuilder("\tpublic abstract void handle").append(entitySimpleName).append("(").append(proxySimpleName).append(" proxy);").toString();
						dataDictionary.addSection("entities3").setVariable("entity", entity3);
						addImport(dataDictionary, proxy.getName().getFullyQualifiedTypeName());
					}
				}
				break;
			case MASTER_ACTIVITIES:
				for (ClassOrInterfaceTypeDetails proxy : proxies) {
					if (!GwtUtils.scaffoldProxy(proxy)) {
						continue;
					}
					String proxySimpleName = proxy.getName().getSimpleTypeName();
					ClassOrInterfaceTypeDetails entity = gwtTypeService.lookupEntityFromProxy(proxy);
					if (entity != null) {
						String entitySimpleName = entity.getName().getSimpleTypeName();
						TemplateDataDictionary section = dataDictionary.addSection("entities");
						section.setVariable("entitySimpleName", entitySimpleName);
						section.setVariable("entityFullPath", proxySimpleName);
						addImport(dataDictionary, entitySimpleName, GwtType.LIST_ACTIVITY, projectMetadata);
						addImport(dataDictionary, proxy.getName().getFullyQualifiedTypeName());
						addImport(dataDictionary, entitySimpleName, GwtType.LIST_VIEW, projectMetadata);
						addImport(dataDictionary, entitySimpleName, GwtType.MOBILE_LIST_VIEW, projectMetadata);
					}
				}
				break;
			case APP_REQUEST_FACTORY:
				for (ClassOrInterfaceTypeDetails proxy : proxies) {
					if (!GwtUtils.scaffoldProxy(proxy)) {
						continue;
					}
					ClassOrInterfaceTypeDetails entity = gwtTypeService.lookupEntityFromProxy(proxy);
					if (entity != null) {
						String entitySimpleName = entity.getName().getSimpleTypeName();
						ClassOrInterfaceTypeDetails request = gwtTypeService.lookupRequestFromProxy(proxy);
						if (request != null) {
							String requestExpression = new StringBuilder("\t").append(request.getName().getSimpleTypeName()).append(" ").append(StringUtils.uncapitalize(entitySimpleName)).append("Request();").toString();
							dataDictionary.addSection("entities").setVariable("entity", requestExpression);
							addImport(dataDictionary, request.getName().getFullyQualifiedTypeName());
						}
					}
					dataDictionary.setVariable("sharedScaffoldPackage", GwtPath.SHARED_SCAFFOLD.packageName(projectOperations.getTopLevelPackage(projectMetadata.getModuleName())));
				}

				if (projectOperations.isGaeEnabled(projectMetadata.getModuleName())) {
					dataDictionary.showSection("gae");
				}
				break;
			case LIST_PLACE_RENDERER:
				for (ClassOrInterfaceTypeDetails proxy : proxies) {
					if (!GwtUtils.scaffoldProxy(proxy)) {
						continue;
					}
					ClassOrInterfaceTypeDetails entity = gwtTypeService.lookupEntityFromProxy(proxy);
					if (entity != null) {
						String entitySimpleName = entity.getName().getSimpleTypeName();
						String proxySimpleName = proxy.getName().getSimpleTypeName();
						TemplateDataDictionary section = dataDictionary.addSection("entities");
						section.setVariable("entitySimpleName", entitySimpleName);
						section.setVariable("entityFullPath", proxySimpleName);
						addImport(dataDictionary, proxy.getName().getFullyQualifiedTypeName());
					}
				}
				break;
			case DETAILS_ACTIVITIES:
				for (ClassOrInterfaceTypeDetails proxy : proxies) {
					if (!GwtUtils.scaffoldProxy(proxy)) {
						continue;
					}
					ClassOrInterfaceTypeDetails entity = gwtTypeService.lookupEntityFromProxy(proxy);
					if (entity != null) {
						String proxySimpleName = proxy.getName().getSimpleTypeName();
						String entitySimpleName = entity.getName().getSimpleTypeName();
						String entityExpression = new StringBuilder("\t\t\tpublic void handle").append(entitySimpleName).append("(").append(proxySimpleName).append(" proxy) {\n").append("\t\t\t\tsetResult(new ").append(entitySimpleName).append("ActivitiesMapper(requests, placeController).getActivity(proxyPlace));\n\t\t\t}").toString();
						dataDictionary.addSection("entities").setVariable("entity", entityExpression);
						addImport(dataDictionary, proxy.getName().getFullyQualifiedTypeName());
						addImport(dataDictionary, GwtType.ACTIVITIES_MAPPER.getPath().packageName(projectOperations.getTopLevelPackage(projectMetadata.getModuleName())) + "." + entitySimpleName + GwtType.ACTIVITIES_MAPPER.getSuffix());
					}
				}
				break;
			case MOBILE_ACTIVITIES:
				// Do nothing
				break;
		}

		return dataDictionary;
	}

	private TemplateDataDictionary buildStandardDataDictionary(final GwtType type, final ProjectMetadata projectMetadata) {

		JavaType javaType = new JavaType(getFullyQualifiedTypeName(type, projectMetadata));
		TemplateDataDictionary dataDictionary = TemplateDictionary.create();
		for (GwtType reference : type.getReferences()) {
			addReference(dataDictionary, reference, projectMetadata);
		}
		dataDictionary.setVariable("className", javaType.getSimpleTypeName());
		dataDictionary.setVariable("packageName", javaType.getPackage().getFullyQualifiedPackageName());
		dataDictionary.setVariable("placePackage", GwtPath.SCAFFOLD_PLACE.packageName(projectOperations.getTopLevelPackage(projectMetadata.getModuleName())));
		dataDictionary.setVariable("sharedScaffoldPackage", GwtPath.SHARED_SCAFFOLD.packageName(projectOperations.getTopLevelPackage(projectMetadata.getModuleName())));
		dataDictionary.setVariable("sharedGaePackage", GwtPath.SHARED_GAE.packageName(projectOperations.getTopLevelPackage(projectMetadata.getModuleName())));
		return dataDictionary;
	}

	private void addImport(final TemplateDataDictionary dataDictionary, final String simpleName, final GwtType gwtType, final ProjectMetadata projectMetadata) {
		addImport(dataDictionary, gwtType.getPath().packageName(projectOperations.getTopLevelPackage(projectMetadata.getModuleName())) + "." + simpleName + gwtType.getSuffix());
	}

	private String getRequestMethodCall(final ClassOrInterfaceTypeDetails request, final MemberTypeAdditions memberTypeAdditions) {
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

	private TemplateDataDictionary buildMirrorDataDictionary(final GwtType type, final ClassOrInterfaceTypeDetails mirroredType, final ClassOrInterfaceTypeDetails proxy, final Map<GwtType, JavaType> mirrorTypeMap, final Map<JavaSymbolName, GwtProxyProperty> clientSideTypeMap, final ProjectMetadata projectMetadata) {
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

		ClassOrInterfaceTypeDetails request = gwtTypeService.lookupRequestFromProxy(proxy);

		MemberTypeAdditions countMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, CustomDataKeys.COUNT_ALL_METHOD.name(), entity, idType, LAYER_POSITION);
		final MethodParameter entityParameter = new MethodParameter(entity, "proxy");
		MemberTypeAdditions persistMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, CustomDataKeys.PERSIST_METHOD.name(), entity, idType, LAYER_POSITION, entityParameter);
		MemberTypeAdditions removeMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, CustomDataKeys.REMOVE_METHOD.name(), entity, idType, LAYER_POSITION, entityParameter);
		String persistMethodSignature = getRequestMethodCall(request, persistMethodAdditions);
		String removeMethodSignature = getRequestMethodCall(request, removeMethodAdditions);
		dataDictionary.setVariable("persistMethodSignature", persistMethodSignature);
		dataDictionary.setVariable("removeMethodSignature", removeMethodSignature);
		dataDictionary.setVariable("countEntitiesMethod", countMethodAdditions.getMethodName());

		for (GwtType reference : type.getReferences()) {
			addReference(dataDictionary, reference, mirrorTypeMap);
		}

		addImport(dataDictionary, proxyType.getFullyQualifiedTypeName());

		String pluralMetadataKey = PluralMetadata.createIdentifier(mirroredType.getName(), PhysicalTypeIdentifier.getPath(mirroredType.getDeclaredByMetadataId()));
		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(pluralMetadataKey);
		String plural = pluralMetadata.getPlural();

		String simpleTypeName = mirroredType.getName().getSimpleTypeName();

		dataDictionary.setVariable("className", javaType.getSimpleTypeName());
		dataDictionary.setVariable("packageName", javaType.getPackage().getFullyQualifiedPackageName());
		dataDictionary.setVariable("placePackage", GwtPath.SCAFFOLD_PLACE.packageName(projectOperations.getTopLevelPackage(projectMetadata.getModuleName())));
		dataDictionary.setVariable("scaffoldUiPackage", GwtPath.SCAFFOLD_UI.packageName(projectOperations.getTopLevelPackage(projectMetadata.getModuleName())));
		dataDictionary.setVariable("sharedScaffoldPackage", GwtPath.SHARED_SCAFFOLD.packageName(projectOperations.getTopLevelPackage(projectMetadata.getModuleName())));
		dataDictionary.setVariable("uiPackage", GwtPath.MANAGED_UI.packageName(projectOperations.getTopLevelPackage(projectMetadata.getModuleName())));
		dataDictionary.setVariable("name", simpleTypeName);
		dataDictionary.setVariable("pluralName", plural);
		dataDictionary.setVariable("nameUncapitalized", StringUtils.uncapitalize(simpleTypeName));
		dataDictionary.setVariable("proxy", proxyType.getSimpleTypeName());
		dataDictionary.setVariable("pluralName", plural);
		dataDictionary.setVariable("proxyRenderer", GwtProxyProperty.getProxyRendererType(projectOperations.getTopLevelPackage(projectMetadata.getModuleName()), proxyType));
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

	private void addReference(final TemplateDataDictionary dataDictionary, final GwtType type, final Map<GwtType, JavaType> mirrorTypeMap) {
		addImport(dataDictionary, mirrorTypeMap.get(type).getFullyQualifiedTypeName());
		dataDictionary.setVariable(type.getName(), mirrorTypeMap.get(type).getSimpleTypeName());
	}

	private JavaType getDestinationJavaType(final GwtType destType, final ProjectMetadata projectMetadata) {
		return new JavaType(getFullyQualifiedTypeName(destType, projectMetadata));
	}

	private void addReference(final TemplateDataDictionary dataDictionary, final GwtType type, final ProjectMetadata projectMetadata) {
		addImport(dataDictionary, getDestinationJavaType(type, projectMetadata).getFullyQualifiedTypeName());
		dataDictionary.setVariable(type.getName(), getDestinationJavaType(type, projectMetadata).getSimpleTypeName());
	}

	private boolean isReadOnly(final String name, final ClassOrInterfaceTypeDetails governorTypeDetails) {
		List<String> readOnly = new ArrayList<String>();
		ClassOrInterfaceTypeDetails proxy = gwtTypeService.lookupProxyFromEntity(governorTypeDetails);
		if (proxy != null) {
			readOnly.addAll(GwtUtils.getAnnotationValues(proxy, RooJavaType.ROO_GWT_PROXY, "readOnly"));
		}

		return readOnly.contains(name);
	}

	private void addImport(final TemplateDataDictionary dataDictionary, final String importDeclaration) {
		dataDictionary.addSection("imports").setVariable("import", importDeclaration);
	}

	private void maybeAddImport(final TemplateDataDictionary dataDictionary, final Set<String> importSet, final JavaType type) {
		if (!importSet.contains(type.getFullyQualifiedTypeName())) {
			addImport(dataDictionary, type.getFullyQualifiedTypeName());
			importSet.add(type.getFullyQualifiedTypeName());
		}
	}

	private void addImport(final TemplateDataDictionary dataDictionary, final JavaType type) {
		dataDictionary.addSection("imports").setVariable("import", type.getFullyQualifiedTypeName());
		for (JavaType param : type.getParameters()) {
			addImport(dataDictionary, param);
		}
	}

	private JavaType getCollectionImplementation(final JavaType javaType) {
		if (javaType.equals(SET)) {
			return new JavaType(HASH_SET.getFullyQualifiedTypeName(), javaType.getArray(), javaType.getDataType(), javaType.getArgName(), javaType.getParameters());
		}
		if (javaType.equals(LIST)) {
			return new JavaType(ARRAY_LIST.getFullyQualifiedTypeName(), javaType.getArray(), javaType.getDataType(), javaType.getArgName(), javaType.getParameters());
		}

		return javaType;
	}
	
	private String getFullyQualifiedTypeName(final GwtType gwtType, final ProjectMetadata projectMetadata) {
		return gwtType.getPath().packageName(projectOperations.getTopLevelPackage(projectMetadata.getModuleName())) + "." + gwtType.getTemplate();
	}
}
