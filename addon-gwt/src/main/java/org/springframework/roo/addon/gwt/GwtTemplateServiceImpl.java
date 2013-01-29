package org.springframework.roo.addon.gwt;

import static org.springframework.roo.addon.gwt.GwtJavaType.INSTANCE_REQUEST;
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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
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
import org.springframework.roo.classpath.details.FieldMetadata;
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
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Provides a basic implementation of {@link GwtTemplateService} which is used
 * to create {@link ClassOrInterfaceTypeDetails} objects from source files
 * created from templates. This class keeps all templating concerns in one
 * place.
 * 
 * @author James Tyrrell
 * @since 1.1.2
 */
@Component
@Service
public class GwtTemplateServiceImpl implements GwtTemplateService {

    private static final int LAYER_POSITION = LayerType.HIGHEST.getPosition();

    @Reference GwtTypeService gwtTypeService;
    @Reference LayerService layerService;
    @Reference MetadataService metadataService;
    @Reference PersistenceMemberLocator persistenceMemberLocator;
    @Reference ProjectOperations projectOperations;
    @Reference TypeLocationService typeLocationService;
    @Reference TypeParsingService typeParsingService;

    @Override
    public String buildUiXml(final String templateContents,
            final String destFile, final List<MethodMetadata> proxyMethods) {
        FileReader fileReader = null;
        try {
            final DocumentBuilder builder = XmlUtils.getDocumentBuilder();
            builder.setEntityResolver(new EntityResolver() {
                @Override
                public InputSource resolveEntity(final String publicId,
                        final String systemId) throws SAXException, IOException {
                    if (systemId
                            .equals("http://dl.google.com/gwt/DTD/xhtml.ent")) {
                        return new InputSource(FileUtils.getInputStream(
                                GwtScaffoldMetadata.class,
                                "templates/xhtml.ent"));
                    }

                    // Use the default behaviour
                    return null;
                }
            });

            InputSource source = new InputSource();
            source.setCharacterStream(new StringReader(templateContents));

            final Document templateDocument = builder.parse(source);

            if (!new File(destFile).exists()) {
                return transformXml(templateDocument);
            }

            source = new InputSource();
            fileReader = new FileReader(destFile);
            source.setCharacterStream(fileReader);
            final Document existingDocument = builder.parse(source);

            // Look for the element holder denoted by the 'debugId' attribute
            // first
            Element existingHoldingElement = XmlUtils.findFirstElement(
                    "//*[@debugId='" + "boundElementHolder" + "']",
                    existingDocument.getDocumentElement());
            Element templateHoldingElement = XmlUtils.findFirstElement(
                    "//*[@debugId='" + "boundElementHolder" + "']",
                    templateDocument.getDocumentElement());

            // If holding element isn't found then the holding element is either
            // not widget based or using the old convention of 'id' so look for
            // the element holder with an 'id' attribute
            if (existingHoldingElement == null) {
                existingHoldingElement = XmlUtils.findFirstElement("//*[@id='"
                        + "boundElementHolder" + "']",
                        existingDocument.getDocumentElement());
            }
            if (templateHoldingElement == null) {
                templateHoldingElement = XmlUtils.findFirstElement("//*[@id='"
                        + "boundElementHolder" + "']",
                        templateDocument.getDocumentElement());
            }

            if (existingHoldingElement != null) {
                final Map<String, Element> templateElementMap = new LinkedHashMap<String, Element>();
                for (final Element element : XmlUtils.findElements("//*[@id]",
                        templateHoldingElement)) {
                    templateElementMap.put(element.getAttribute("id"), element);
                }

                final Map<String, Element> existingElementMap = new LinkedHashMap<String, Element>();
                for (final Element element : XmlUtils.findElements("//*[@id]",
                        existingHoldingElement)) {
                    existingElementMap.put(element.getAttribute("id"), element);
                }

                if (existingElementMap.keySet().containsAll(
                        templateElementMap.values())) {
                    return transformXml(existingDocument);
                }

                final List<Element> elementsToAdd = new ArrayList<Element>();
                for (final Map.Entry<String, Element> entry : templateElementMap
                        .entrySet()) {
                    if (!existingElementMap.keySet().contains(entry.getKey())) {
                        elementsToAdd.add(entry.getValue());
                    }
                }

                final List<Element> elementsToRemove = new ArrayList<Element>();
                for (final Map.Entry<String, Element> entry : existingElementMap
                        .entrySet()) {
                    if (!templateElementMap.keySet().contains(entry.getKey())) {
                        elementsToRemove.add(entry.getValue());
                    }
                }

                for (final Element element : elementsToAdd) {
                    final Node importedNode = existingDocument.importNode(
                            element, true);
                    existingHoldingElement.appendChild(importedNode);
                }

                for (final Element element : elementsToRemove) {
                    existingHoldingElement.removeChild(element);
                }

                if (elementsToAdd.size() > 0) {
                    final List<Element> sortedElements = new ArrayList<Element>();
                    for (final MethodMetadata method : proxyMethods) {
                        final String propertyName = StringUtils
                                .uncapitalize(BeanInfoUtils
                                        .getPropertyNameForJavaBeanMethod(
                                                method).getSymbolName());
                        final Element element = XmlUtils.findFirstElement(
                                "//*[@id='" + propertyName + "']",
                                existingHoldingElement);
                        if (element != null) {
                            sortedElements.add(element);
                        }
                    }
                    for (final Element el : sortedElements) {
                        if (el.getParentNode() != null
                                && el.getParentNode().equals(
                                        existingHoldingElement)) {
                            existingHoldingElement.removeChild(el);
                        }
                    }
                    for (final Element el : sortedElements) {
                        existingHoldingElement.appendChild(el);
                    }
                }

                return transformXml(existingDocument);
            }

            return transformXml(templateDocument);
        }
        catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        finally {
            IOUtils.closeQuietly(fileReader);
        }
    }

    @Override
    public GwtTemplateDataHolder getMirrorTemplateTypeDetails(
            final ClassOrInterfaceTypeDetails mirroredType,
            final Map<JavaSymbolName, GwtProxyProperty> clientSideTypeMap,
            final String moduleName) {
        final ClassOrInterfaceTypeDetails proxy = gwtTypeService
                .lookupProxyFromEntity(mirroredType);
        final ClassOrInterfaceTypeDetails request = gwtTypeService
                .lookupRequestFromEntity(mirroredType);
        final JavaPackage topLevelPackage = projectOperations
                .getTopLevelPackage(moduleName);
        final Map<GwtType, JavaType> mirrorTypeMap = GwtUtils.getMirrorTypeMap(
                mirroredType.getName(), topLevelPackage);
        mirrorTypeMap.put(GwtType.PROXY, proxy.getName());
        mirrorTypeMap.put(GwtType.REQUEST, request.getName());

        final Map<GwtType, ClassOrInterfaceTypeDetails> templateTypeDetailsMap = new LinkedHashMap<GwtType, ClassOrInterfaceTypeDetails>();
        final Map<GwtType, String> xmlTemplates = new LinkedHashMap<GwtType, String>();
        for (final GwtType gwtType : GwtType.getMirrorTypes()) {
            if (gwtType.getTemplate() == null) {
                continue;
            }
            TemplateDataDictionary dataDictionary = buildMirrorDataDictionary(
                    gwtType, mirroredType, proxy, mirrorTypeMap,
                    clientSideTypeMap, moduleName);
            gwtType.dynamicallyResolveFieldsToWatch(clientSideTypeMap);
            gwtType.dynamicallyResolveMethodsToWatch(mirroredType.getName(),
                    clientSideTypeMap, topLevelPackage);
            templateTypeDetailsMap.put(
                    gwtType,
                    getTemplateDetails(dataDictionary, gwtType.getTemplate(),
                            mirrorTypeMap.get(gwtType), moduleName));

            if (gwtType.isCreateUiXml()) {
                dataDictionary = buildMirrorDataDictionary(gwtType,
                        mirroredType, proxy, mirrorTypeMap, clientSideTypeMap,
                        moduleName);
                final String contents = getTemplateContents(
                        gwtType.getTemplate() + "UiXml", dataDictionary);
                xmlTemplates.put(gwtType, contents);
            }
        }

        final Map<String, String> xmlMap = new LinkedHashMap<String, String>();
        final List<ClassOrInterfaceTypeDetails> typeDetails = new ArrayList<ClassOrInterfaceTypeDetails>();
        for (final GwtProxyProperty proxyProperty : clientSideTypeMap.values()) {
            if (!proxyProperty.isCollection()
                    || proxyProperty.isCollectionOfProxy()) {
                continue;
            }

            TemplateDataDictionary dataDictionary = TemplateDictionary.create();
            dataDictionary.setVariable("packageName",
                    GwtPath.MANAGED_UI.packageName(topLevelPackage));
            dataDictionary.setVariable("scaffoldUiPackage",
                    GwtPath.SCAFFOLD_UI.packageName(topLevelPackage));
            final JavaType collectionTypeImpl = getCollectionImplementation(proxyProperty
                    .getPropertyType());
            addImport(dataDictionary, collectionTypeImpl);
            addImport(dataDictionary, proxyProperty.getPropertyType());

            final String collectionType = proxyProperty.getPropertyType()
                    .getSimpleTypeName();
            final String boundCollectionType = proxyProperty.getPropertyType()
                    .getParameters().get(0).getSimpleTypeName();

            dataDictionary.setVariable("collectionType", collectionType);
            dataDictionary.setVariable("collectionTypeImpl",
                    collectionTypeImpl.getSimpleTypeName());
            dataDictionary.setVariable("boundCollectionType",
                    boundCollectionType);

            final JavaType collectionEditorType = new JavaType(
                    GwtPath.MANAGED_UI.packageName(topLevelPackage) + "."
                            + boundCollectionType + collectionType + "Editor");
            typeDetails.add(getTemplateDetails(dataDictionary,
                    "CollectionEditor", collectionEditorType, moduleName));

            dataDictionary = TemplateDictionary.create();
            dataDictionary.setVariable("packageName",
                    GwtPath.MANAGED_UI.packageName(topLevelPackage));
            dataDictionary.setVariable("scaffoldUiPackage",
                    GwtPath.SCAFFOLD_UI.packageName(topLevelPackage));
            dataDictionary.setVariable("collectionType", collectionType);
            dataDictionary.setVariable("collectionTypeImpl",
                    collectionTypeImpl.getSimpleTypeName());
            dataDictionary.setVariable("boundCollectionType",
                    boundCollectionType);
            addImport(dataDictionary, proxyProperty.getPropertyType());

            final String contents = getTemplateContents("CollectionEditor"
                    + "UiXml", dataDictionary);
            final String packagePath = projectOperations.getPathResolver()
                    .getFocusedIdentifier(Path.SRC_MAIN_JAVA,
                            GwtPath.MANAGED_UI.getPackagePath(topLevelPackage));
            xmlMap.put(packagePath + "/" + boundCollectionType + collectionType
                    + "Editor.ui.xml", contents);
        }

        return new GwtTemplateDataHolder(templateTypeDetailsMap, xmlTemplates,
                typeDetails, xmlMap);
    }

    @Override
    public List<ClassOrInterfaceTypeDetails> getStaticTemplateTypeDetails(
            final GwtType type, final String moduleName) {
        final List<ClassOrInterfaceTypeDetails> templateTypeDetails = new ArrayList<ClassOrInterfaceTypeDetails>();
        final TemplateDataDictionary dataDictionary = buildDictionary(type,
                moduleName);
        templateTypeDetails.add(getTemplateDetails(dataDictionary,
                type.getTemplate(), getDestinationJavaType(type, moduleName),
                moduleName));
        return templateTypeDetails;
    }

    public ClassOrInterfaceTypeDetails getTemplateDetails(
            final TemplateDataDictionary dataDictionary,
            final String templateFile, final JavaType templateType,
            final String moduleName) {
        try {
            final TemplateLoader templateLoader = TemplateResourceLoader
                    .create();
            final Template template = templateLoader.getTemplate(templateFile);
            Validate.notNull(template, "Template required for '%s'",
                    templateFile);
            final String templateContents = template
                    .renderToString(dataDictionary);
            final String templateId = PhysicalTypeIdentifier.createIdentifier(
                    templateType,
                    LogicalPath.getInstance(Path.SRC_MAIN_JAVA, moduleName));
            return typeParsingService.getTypeFromString(templateContents,
                    templateId, templateType);
        }
        catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void addImport(final TemplateDataDictionary dataDictionary,
            final JavaType type) {
        dataDictionary.addSection("imports").setVariable("import",
                type.getFullyQualifiedTypeName());
        for (final JavaType param : type.getParameters()) {
            addImport(dataDictionary, param.getFullyQualifiedTypeName());
        }
    }

    private void addImport(final TemplateDataDictionary dataDictionary,
            final String importDeclaration) {
        dataDictionary.addSection("imports").setVariable("import",
                importDeclaration);
    }

    private void addImport(final TemplateDataDictionary dataDictionary,
            final String simpleName, final GwtType gwtType,
            final String moduleName) {
        addImport(
                dataDictionary,
                gwtType.getPath().packageName(
                        projectOperations.getTopLevelPackage(moduleName))
                        + "." + simpleName + gwtType.getSuffix());
    }

    private void addReference(final TemplateDataDictionary dataDictionary,
            final GwtType type, final Map<GwtType, JavaType> mirrorTypeMap) {
        addImport(dataDictionary, mirrorTypeMap.get(type)
                .getFullyQualifiedTypeName());
        dataDictionary.setVariable(type.getName(), mirrorTypeMap.get(type)
                .getSimpleTypeName());
    }

    private void addReference(final TemplateDataDictionary dataDictionary,
            final GwtType type, final String moduleName) {
        addImport(dataDictionary, getDestinationJavaType(type, moduleName)
                .getFullyQualifiedTypeName());
        dataDictionary.setVariable(type.getName(),
                getDestinationJavaType(type, moduleName).getSimpleTypeName());
    }

    private TemplateDataDictionary buildDictionary(final GwtType type,
            final String moduleName) {
        final Set<ClassOrInterfaceTypeDetails> proxies = typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_GWT_PROXY);
        final TemplateDataDictionary dataDictionary = buildStandardDataDictionary(
                type, moduleName);
        switch (type) {
        case APP_ENTITY_TYPES_PROCESSOR:
            for (final ClassOrInterfaceTypeDetails proxy : proxies) {
                if (!GwtUtils.scaffoldProxy(proxy)) {
                    continue;
                }
                final String proxySimpleName = proxy.getName()
                        .getSimpleTypeName();
                final ClassOrInterfaceTypeDetails entity = gwtTypeService
                        .lookupEntityFromProxy(proxy);
                if (entity != null) {
                    final String entitySimpleName = entity.getName()
                            .getSimpleTypeName();

                    dataDictionary.addSection("proxys").setVariable("proxy",
                            proxySimpleName);

                    final String entity1 = new StringBuilder("\t\tif (")
                            .append(proxySimpleName)
                            .append(".class.equals(clazz)) {\n\t\t\tprocessor.handle")
                            .append(entitySimpleName).append("((")
                            .append(proxySimpleName)
                            .append(") null);\n\t\t\treturn;\n\t\t}")
                            .toString();
                    dataDictionary.addSection("entities1").setVariable(
                            "entity", entity1);

                    final String entity2 = new StringBuilder(
                            "\t\tif (proxy instanceof ")
                            .append(proxySimpleName)
                            .append(") {\n\t\t\tprocessor.handle")
                            .append(entitySimpleName).append("((")
                            .append(proxySimpleName)
                            .append(") proxy);\n\t\t\treturn;\n\t\t}")
                            .toString();
                    dataDictionary.addSection("entities2").setVariable(
                            "entity", entity2);

                    final String entity3 = new StringBuilder(
                            "\tpublic abstract void handle")
                            .append(entitySimpleName).append("(")
                            .append(proxySimpleName).append(" proxy);")
                            .toString();
                    dataDictionary.addSection("entities3").setVariable(
                            "entity", entity3);
                    addImport(dataDictionary, proxy.getName()
                            .getFullyQualifiedTypeName());
                }
            }
            break;
        case MASTER_ACTIVITIES:
            for (final ClassOrInterfaceTypeDetails proxy : proxies) {
                if (!GwtUtils.scaffoldProxy(proxy)) {
                    continue;
                }
                final String proxySimpleName = proxy.getName()
                        .getSimpleTypeName();
                final ClassOrInterfaceTypeDetails entity = gwtTypeService
                        .lookupEntityFromProxy(proxy);
                if (entity != null
                        && !Modifier.isAbstract(entity.getModifier())) {
                    final String entitySimpleName = entity.getName()
                            .getSimpleTypeName();
                    final TemplateDataDictionary section = dataDictionary
                            .addSection("entities");
                    section.setVariable("entitySimpleName", entitySimpleName);
                    section.setVariable("entityFullPath", proxySimpleName);
                    addImport(dataDictionary, entitySimpleName,
                            GwtType.LIST_ACTIVITY, moduleName);
                    addImport(dataDictionary, proxy.getName()
                            .getFullyQualifiedTypeName());
                    addImport(dataDictionary, entitySimpleName,
                            GwtType.LIST_VIEW, moduleName);
                    addImport(dataDictionary, entitySimpleName,
                            GwtType.MOBILE_LIST_VIEW, moduleName);
                }
            }
            break;
        case APP_REQUEST_FACTORY:
            for (final ClassOrInterfaceTypeDetails proxy : proxies) {
                if (!GwtUtils.scaffoldProxy(proxy)) {
                    continue;
                }
                final ClassOrInterfaceTypeDetails entity = gwtTypeService
                        .lookupEntityFromProxy(proxy);
                if (entity != null
                        && !Modifier.isAbstract(entity.getModifier())) {
                    final String entitySimpleName = entity.getName()
                            .getSimpleTypeName();
                    ClassOrInterfaceTypeDetails request = gwtTypeService
                            .lookupUnmanagedRequestFromProxy(proxy);
                    if (request == null) {
                        request = gwtTypeService.lookupRequestFromProxy(proxy);
                    }
                    if (request != null) {
                        final String requestExpression = new StringBuilder("\t")
                                .append(request.getName().getSimpleTypeName())
                                .append(" ")
                                .append(StringUtils
                                        .uncapitalize(entitySimpleName))
                                .append("Request();").toString();
                        dataDictionary.addSection("entities").setVariable(
                                "entity", requestExpression);
                        addImport(dataDictionary, request.getName()
                                .getFullyQualifiedTypeName());
                    }
                }
                dataDictionary.setVariable("sharedScaffoldPackage",
                        GwtPath.SHARED_SCAFFOLD.packageName(projectOperations
                                .getTopLevelPackage(moduleName)));
            }

            if (projectOperations.isFeatureInstalled(FeatureNames.GAE)) {
                dataDictionary.showSection("gae");
            }
            break;
        case LIST_PLACE_RENDERER:
            for (final ClassOrInterfaceTypeDetails proxy : proxies) {
                if (!GwtUtils.scaffoldProxy(proxy)) {
                    continue;
                }
                final ClassOrInterfaceTypeDetails entity = gwtTypeService
                        .lookupEntityFromProxy(proxy);
                if (entity != null) {
                    final String entitySimpleName = entity.getName()
                            .getSimpleTypeName();
                    final String proxySimpleName = proxy.getName()
                            .getSimpleTypeName();
                    final TemplateDataDictionary section = dataDictionary
                            .addSection("entities");
                    section.setVariable("entitySimpleName", entitySimpleName);
                    section.setVariable("entityFullPath", proxySimpleName);
                    addImport(dataDictionary, proxy.getName()
                            .getFullyQualifiedTypeName());
                }
            }
            break;
        case DETAILS_ACTIVITIES:
            for (final ClassOrInterfaceTypeDetails proxy : proxies) {
                if (!GwtUtils.scaffoldProxy(proxy)) {
                    continue;
                }
                final ClassOrInterfaceTypeDetails entity = gwtTypeService
                        .lookupEntityFromProxy(proxy);
                if (entity != null) {
                    final String proxySimpleName = proxy.getName()
                            .getSimpleTypeName();
                    final String entitySimpleName = entity.getName()
                            .getSimpleTypeName();
                    final String entityExpression = new StringBuilder(
                            "\t\t\tpublic void handle")
                            .append(entitySimpleName)
                            .append("(")
                            .append(proxySimpleName)
                            .append(" proxy) {\n")
                            .append("\t\t\t\tsetResult(new ")
                            .append(entitySimpleName)
                            .append("ActivitiesMapper(requests, placeController).getActivity(proxyPlace));\n\t\t\t}")
                            .toString();
                    dataDictionary.addSection("entities").setVariable("entity",
                            entityExpression);
                    addImport(dataDictionary, proxy.getName()
                            .getFullyQualifiedTypeName());
                    addImport(
                            dataDictionary,
                            GwtType.ACTIVITIES_MAPPER.getPath().packageName(
                                    projectOperations
                                            .getTopLevelPackage(moduleName))
                                    + "."
                                    + entitySimpleName
                                    + GwtType.ACTIVITIES_MAPPER.getSuffix());
                }
            }
            break;
        case MOBILE_ACTIVITIES:
            // Do nothing
            break;
        }

        return dataDictionary;
    }

    private TemplateDataDictionary buildMirrorDataDictionary(
            final GwtType type, final ClassOrInterfaceTypeDetails mirroredType,
            final ClassOrInterfaceTypeDetails proxy,
            final Map<GwtType, JavaType> mirrorTypeMap,
            final Map<JavaSymbolName, GwtProxyProperty> clientSideTypeMap,
            final String moduleName) {
        final JavaType proxyType = proxy.getName();
        final JavaType javaType = mirrorTypeMap.get(type);

        final TemplateDataDictionary dataDictionary = TemplateDictionary
                .create();

        // Get my locator and
        final JavaType entity = mirroredType.getName();
        final String entityName = entity.getFullyQualifiedTypeName();
        final String metadataIdentificationString = mirroredType
                .getDeclaredByMetadataId();
        final JavaType idType = persistenceMemberLocator
                .getIdentifierType(entity);
        Validate.notNull(idType,
                "Identifier type is not available for entity '%s'", entityName);

        final MethodParameter entityParameter = new MethodParameter(entity,
                "proxy");
        final ClassOrInterfaceTypeDetails request = gwtTypeService
                .lookupRequestFromProxy(proxy);

        final MemberTypeAdditions persistMethodAdditions = layerService
                .getMemberTypeAdditions(metadataIdentificationString,
                        CustomDataKeys.PERSIST_METHOD.name(), entity, idType,
                        LAYER_POSITION, entityParameter);
        Validate.notNull(persistMethodAdditions,
                "Persist method is not available for entity '%s'", entityName);
        final String persistMethodSignature = getRequestMethodCall(request,
                persistMethodAdditions);
        dataDictionary.setVariable("persistMethodSignature",
                persistMethodSignature);

        final MemberTypeAdditions removeMethodAdditions = layerService
                .getMemberTypeAdditions(metadataIdentificationString,
                        CustomDataKeys.REMOVE_METHOD.name(), entity, idType,
                        LAYER_POSITION, entityParameter);
        Validate.notNull(removeMethodAdditions,
                "Remove method is not available for entity '%s'", entityName);
        final String removeMethodSignature = getRequestMethodCall(request,
                removeMethodAdditions);
        dataDictionary.setVariable("removeMethodSignature",
                removeMethodSignature);

        final MemberTypeAdditions countMethodAdditions = layerService
                .getMemberTypeAdditions(metadataIdentificationString,
                        CustomDataKeys.COUNT_ALL_METHOD.name(), entity, idType,
                        LAYER_POSITION);
        Validate.notNull(countMethodAdditions,
                "Count method is not available for entity '%s'", entityName);
        dataDictionary.setVariable("countEntitiesMethod",
                countMethodAdditions.getMethodName());

        for (final GwtType reference : type.getReferences()) {
            addReference(dataDictionary, reference, mirrorTypeMap);
        }

        addImport(dataDictionary, proxyType.getFullyQualifiedTypeName());

        final String pluralMetadataKey = PluralMetadata.createIdentifier(
                mirroredType.getName(), PhysicalTypeIdentifier
                        .getPath(mirroredType.getDeclaredByMetadataId()));
        final PluralMetadata pluralMetadata = (PluralMetadata) metadataService
                .get(pluralMetadataKey);
        final String plural = pluralMetadata.getPlural();

        final String simpleTypeName = mirroredType.getName()
                .getSimpleTypeName();
        final JavaPackage topLevelPackage = projectOperations
                .getTopLevelPackage(moduleName);
        dataDictionary.setVariable("className", javaType.getSimpleTypeName());
        dataDictionary.setVariable("packageName", javaType.getPackage()
                .getFullyQualifiedPackageName());
        dataDictionary.setVariable("placePackage",
                GwtPath.SCAFFOLD_PLACE.packageName(topLevelPackage));
        dataDictionary.setVariable("scaffoldUiPackage",
                GwtPath.SCAFFOLD_UI.packageName(topLevelPackage));
        dataDictionary.setVariable("sharedScaffoldPackage",
                GwtPath.SHARED_SCAFFOLD.packageName(topLevelPackage));
        dataDictionary.setVariable("uiPackage",
                GwtPath.MANAGED_UI.packageName(topLevelPackage));
        dataDictionary.setVariable("name", simpleTypeName);
        dataDictionary.setVariable("pluralName", plural);
        dataDictionary.setVariable("nameUncapitalized",
                StringUtils.uncapitalize(simpleTypeName));
        dataDictionary.setVariable("proxy", proxyType.getSimpleTypeName());
        dataDictionary.setVariable("pluralName", plural);
        dataDictionary.setVariable("proxyRenderer", GwtProxyProperty
                .getProxyRendererType(topLevelPackage, proxyType));

        String proxyFields = null;
        GwtProxyProperty primaryProperty = null;
        GwtProxyProperty secondaryProperty = null;
        GwtProxyProperty dateProperty = null;
        final Set<String> importSet = new HashSet<String>();

        final List<String> omittedFields = new ArrayList<String>();

        // Adds names of fields in edit view to ommittedFields list
        if (type == GwtType.EDIT_VIEW) {
            try {
                final String className = GwtPath.MANAGED_UI
                        .packageName(topLevelPackage)
                        + "."
                        + simpleTypeName
                        + GwtType.EDIT_VIEW.getTemplate();

                final ClassOrInterfaceTypeDetails details = typeLocationService
                        .getTypeDetails(new JavaType(className));
                if (details != null) {
                    for (final FieldMetadata field : details.getDeclaredFields()) {
                        final JavaSymbolName fieldName = field.getFieldName();
                        final String name = fieldName.toString();
                        omittedFields.add(name);
                    }
                }
            }
            catch (final Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        // Adds names of fields in mobile edit view to ommittedFields list
        if (type == GwtType.MOBILE_EDIT_VIEW) {
            try {
                final String className = GwtPath.MANAGED_UI
                        .packageName(topLevelPackage)
                        + "."
                        + simpleTypeName
                        + GwtType.MOBILE_EDIT_VIEW.getTemplate();

                final ClassOrInterfaceTypeDetails details = typeLocationService
                        .getTypeDetails(new JavaType(className));
                if (details != null) {
                    for (final FieldMetadata field : details.getDeclaredFields()) {
                        final JavaSymbolName fieldName = field.getFieldName();
                        final String name = fieldName.toString();
                        omittedFields.add(name);
                    }
                }
            }
            catch (final Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        for (final GwtProxyProperty gwtProxyProperty : clientSideTypeMap
                .values()) {
            // Determine if this is the primary property.
            if (primaryProperty == null) {
                // Choose the first available field.
                primaryProperty = gwtProxyProperty;
            }
            else if (gwtProxyProperty.isString() && !primaryProperty.isString()) {
                // Favor String properties over other types.
                secondaryProperty = primaryProperty;
                primaryProperty = gwtProxyProperty;
            }
            else if (secondaryProperty == null) {
                // Choose the next available property.
                secondaryProperty = gwtProxyProperty;
            }
            else if (gwtProxyProperty.isString()
                    && !secondaryProperty.isString()) {
                // Favor String properties over other types.
                secondaryProperty = gwtProxyProperty;
            }

            // Determine if this is the first date property.
            if (dateProperty == null && gwtProxyProperty.isDate()) {
                dateProperty = gwtProxyProperty;
            }

            if (gwtProxyProperty.isProxy()
                    || gwtProxyProperty.isCollectionOfProxy()) {
                if (proxyFields != null) {
                    proxyFields += ", ";
                }
                else {
                    proxyFields = "";
                }
                proxyFields += "\"" + gwtProxyProperty.getName() + "\"";
            }

            dataDictionary.addSection("fields").setVariable("field",
                    gwtProxyProperty.getName());
            if (!isReadOnly(gwtProxyProperty.getName(), mirroredType)) {
                // if the property is in the omittedFields list, do not add it
                if (!omittedFields.contains(gwtProxyProperty.getName())) {
                    dataDictionary.addSection("editViewProps").setVariable(
                            "prop", gwtProxyProperty.forEditView());
                }
            }

            final TemplateDataDictionary propertiesSection = dataDictionary
                    .addSection("properties");
            propertiesSection.setVariable("prop", gwtProxyProperty.getName());
            propertiesSection.setVariable(
                    "propId",
                    proxyType.getSimpleTypeName() + "_"
                            + gwtProxyProperty.getName());
            propertiesSection.setVariable("propGetter",
                    gwtProxyProperty.getGetter());
            propertiesSection.setVariable("propType",
                    gwtProxyProperty.getType());
            propertiesSection.setVariable("propFormatter",
                    gwtProxyProperty.getFormatter());
            propertiesSection.setVariable("propRenderer",
                    gwtProxyProperty.getRenderer());
            propertiesSection.setVariable("propReadable",
                    gwtProxyProperty.getReadableName());

            if (!isReadOnly(gwtProxyProperty.getName(), mirroredType)) {
                final TemplateDataDictionary editableSection = dataDictionary
                        .addSection("editableProperties");
                editableSection.setVariable("prop", gwtProxyProperty.getName());
                editableSection.setVariable(
                        "propId",
                        proxyType.getSimpleTypeName() + "_"
                                + gwtProxyProperty.getName());
                editableSection.setVariable("propGetter",
                        gwtProxyProperty.getGetter());
                editableSection.setVariable("propType",
                        gwtProxyProperty.getType());
                editableSection.setVariable("propFormatter",
                        gwtProxyProperty.getFormatter());
                editableSection.setVariable("propRenderer",
                        gwtProxyProperty.getRenderer());
                editableSection.setVariable("propBinder",
                        gwtProxyProperty.getBinder());
                editableSection.setVariable("propReadable",
                        gwtProxyProperty.getReadableName());
            }

            dataDictionary.setVariable("proxyRendererType",
                    proxyType.getSimpleTypeName() + "Renderer");

            if (gwtProxyProperty.isProxy() || gwtProxyProperty.isEnum()
                    || gwtProxyProperty.isCollectionOfProxy()) {
                final TemplateDataDictionary section = dataDictionary
                        .addSection(gwtProxyProperty.isEnum() ? "setEnumValuePickers"
                                : "setProxyValuePickers");
                section.setVariable("setValuePicker",
                        gwtProxyProperty.getSetValuePickerMethod());
                section.setVariable("setValuePickerName",
                        gwtProxyProperty.getSetValuePickerMethodName());
                section.setVariable("valueType", gwtProxyProperty
                        .getValueType().getSimpleTypeName());
                section.setVariable("rendererType",
                        gwtProxyProperty.getProxyRendererType());
                if (gwtProxyProperty.isProxy()
                        || gwtProxyProperty.isCollectionOfProxy()) {
                    String propTypeName = StringUtils
                            .uncapitalize(gwtProxyProperty
                                    .isCollectionOfProxy() ? gwtProxyProperty
                                    .getPropertyType().getParameters().get(0)
                                    .getSimpleTypeName() : gwtProxyProperty
                                    .getPropertyType().getSimpleTypeName());
                    propTypeName = propTypeName.substring(0,
                            propTypeName.indexOf("Proxy"));
                    section.setVariable("requestInterface", propTypeName
                            + "Request");
                    section.setVariable("findMethod",
                            "find" + StringUtils.capitalize(propTypeName)
                                    + "Entries(0, 50)");
                }
                maybeAddImport(dataDictionary, importSet,
                        gwtProxyProperty.getPropertyType());
                maybeAddImport(dataDictionary, importSet,
                        gwtProxyProperty.getValueType());
                if (gwtProxyProperty.isCollectionOfProxy()) {
                    maybeAddImport(dataDictionary, importSet, gwtProxyProperty
                            .getPropertyType().getParameters().get(0));
                    maybeAddImport(dataDictionary, importSet,
                            gwtProxyProperty.getSetEditorType());
                }
            }
        }

        dataDictionary.setVariable("proxyFields", proxyFields);

        // Add a section for the mobile properties.
        if (primaryProperty != null) {
            dataDictionary
                    .setVariable("primaryProp", primaryProperty.getName());
            dataDictionary.setVariable("primaryPropGetter",
                    primaryProperty.getGetter());
            dataDictionary.setVariable("primaryPropBuilder",
                    primaryProperty.forMobileListView("primaryRenderer"));
            final TemplateDataDictionary section = dataDictionary
                    .addSection("mobileProperties");
            section.setVariable("prop", primaryProperty.getName());
            section.setVariable("propGetter", primaryProperty.getGetter());
            section.setVariable("propType", primaryProperty.getType());
            section.setVariable("propRenderer", primaryProperty.getRenderer());
            section.setVariable("propRendererName", "primaryRenderer");
        }
        else {
            dataDictionary.setVariable("primaryProp", "id");
            dataDictionary.setVariable("primaryPropGetter", "getId");
            dataDictionary.setVariable("primaryPropBuilder", "");
        }
        if (secondaryProperty != null) {
            dataDictionary.setVariable("secondaryPropBuilder",
                    secondaryProperty.forMobileListView("secondaryRenderer"));
            final TemplateDataDictionary section = dataDictionary
                    .addSection("mobileProperties");
            section.setVariable("prop", secondaryProperty.getName());
            section.setVariable("propGetter", secondaryProperty.getGetter());
            section.setVariable("propType", secondaryProperty.getType());
            section.setVariable("propRenderer", secondaryProperty.getRenderer());
            section.setVariable("propRendererName", "secondaryRenderer");
        }
        else {
            dataDictionary.setVariable("secondaryPropBuilder", "");
        }
        if (dateProperty != null) {
            dataDictionary.setVariable("datePropBuilder",
                    dateProperty.forMobileListView("dateRenderer"));
            final TemplateDataDictionary section = dataDictionary
                    .addSection("mobileProperties");
            section.setVariable("prop", dateProperty.getName());
            section.setVariable("propGetter", dateProperty.getGetter());
            section.setVariable("propType", dateProperty.getType());
            section.setVariable("propRenderer", dateProperty.getRenderer());
            section.setVariable("propRendererName", "dateRenderer");
        }
        else {
            dataDictionary.setVariable("datePropBuilder", "");
        }
        return dataDictionary;
    }

    private TemplateDataDictionary buildStandardDataDictionary(
            final GwtType type, final String moduleName) {
        final JavaType javaType = new JavaType(getFullyQualifiedTypeName(type,
                moduleName));
        final TemplateDataDictionary dataDictionary = TemplateDictionary
                .create();
        for (final GwtType reference : type.getReferences()) {
            addReference(dataDictionary, reference, moduleName);
        }
        dataDictionary.setVariable("className", javaType.getSimpleTypeName());
        dataDictionary.setVariable("packageName", javaType.getPackage()
                .getFullyQualifiedPackageName());
        dataDictionary.setVariable("placePackage", GwtPath.SCAFFOLD_PLACE
                .packageName(projectOperations.getTopLevelPackage(moduleName)));
        dataDictionary.setVariable("sharedScaffoldPackage",
                GwtPath.SHARED_SCAFFOLD.packageName(projectOperations
                        .getTopLevelPackage(moduleName)));
        dataDictionary.setVariable("sharedGaePackage", GwtPath.SHARED_GAE
                .packageName(projectOperations.getTopLevelPackage(moduleName)));
        return dataDictionary;
    }

    private JavaType getCollectionImplementation(final JavaType javaType) {
        if (isSameBaseType(javaType, SET)) {
            return new JavaType(HASH_SET.getFullyQualifiedTypeName(),
                    javaType.getArray(), javaType.getDataType(),
                    javaType.getArgName(), javaType.getParameters());
        }
        if (isSameBaseType(javaType, LIST)) {
            return new JavaType(ARRAY_LIST.getFullyQualifiedTypeName(),
                    javaType.getArray(), javaType.getDataType(),
                    javaType.getArgName(), javaType.getParameters());
        }
        return javaType;
    }

    private JavaType getDestinationJavaType(final GwtType destType,
            final String moduleName) {
        return new JavaType(getFullyQualifiedTypeName(destType, moduleName));
    }

    private String getFullyQualifiedTypeName(final GwtType gwtType,
            final String moduleName) {
        return gwtType.getPath().packageName(
                projectOperations.getTopLevelPackage(moduleName))
                + "." + gwtType.getTemplate();
    }

    private String getRequestMethodCall(
            final ClassOrInterfaceTypeDetails request,
            final MemberTypeAdditions memberTypeAdditions) {
        final String methodName = memberTypeAdditions.getMethodName();
        final MethodMetadata requestMethod = MemberFindingUtils.getMethod(
                request, methodName);
        String requestMethodCall = memberTypeAdditions.getMethodName();
        if (requestMethod != null) {
            if (INSTANCE_REQUEST.getFullyQualifiedTypeName().equals(
                    requestMethod.getReturnType().getFullyQualifiedTypeName())) {
                requestMethodCall = requestMethodCall + "().using";
            }
        }
        return requestMethodCall;
    }

    private String getTemplateContents(final String templateName,
            final TemplateDataDictionary dataDictionary) {
        try {
            final TemplateLoader templateLoader = TemplateResourceLoader
                    .create();
            final Template template = templateLoader.getTemplate(templateName);
            return template.renderToString(dataDictionary);
        }
        catch (final TemplateException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean isReadOnly(final String name,
            final ClassOrInterfaceTypeDetails governorTypeDetails) {
        final List<String> readOnly = new ArrayList<String>();
        final ClassOrInterfaceTypeDetails proxy = gwtTypeService
                .lookupProxyFromEntity(governorTypeDetails);
        if (proxy != null) {
            readOnly.addAll(GwtUtils.getAnnotationValues(proxy,
                    RooJavaType.ROO_GWT_PROXY, "readOnly"));
        }

        return readOnly.contains(name);
    }

    private boolean isSameBaseType(final JavaType type1, final JavaType type2) {
        return type1.getFullyQualifiedTypeName().equals(
                type2.getFullyQualifiedTypeName());
    }

    private void maybeAddImport(final TemplateDataDictionary dataDictionary,
            final Set<String> importSet, final JavaType type) {
        if (!importSet.contains(type.getFullyQualifiedTypeName())) {
            addImport(dataDictionary, type.getFullyQualifiedTypeName());
            importSet.add(type.getFullyQualifiedTypeName());
        }
    }

    private String transformXml(final Document document)
            throws TransformerException {
        final Transformer transformer = XmlUtils.createIndentingTransformer();
        final DOMSource source = new DOMSource(document);
        final StreamResult result = new StreamResult(new StringWriter());
        transformer.transform(source, result);
        return result.getWriter().toString();
    }
}
