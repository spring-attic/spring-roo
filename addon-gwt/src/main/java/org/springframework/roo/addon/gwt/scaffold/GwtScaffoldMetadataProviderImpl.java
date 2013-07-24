package org.springframework.roo.addon.gwt.scaffold;

import static org.springframework.roo.project.Path.SRC_MAIN_JAVA;

import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.gwt.GwtFileManager;
import org.springframework.roo.addon.gwt.GwtPath;
import org.springframework.roo.addon.gwt.GwtProxyProperty;
import org.springframework.roo.addon.gwt.GwtTemplateDataHolder;
import org.springframework.roo.addon.gwt.GwtTemplateService;
import org.springframework.roo.addon.gwt.GwtType;
import org.springframework.roo.addon.gwt.GwtTypeService;
import org.springframework.roo.addon.gwt.GwtUtils;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Monitors Java types and if necessary creates/updates/deletes the GWT files
 * maintained for each mirror-compatible object. You can find a list of
 * mirror-compatible objects in
 * {@link org.springframework.roo.addon.gwt.GwtType}.
 * <p/>
 * <p/>
 * For now only @RooJpaEntity instances will be mirror-compatible.
 * <p/>
 * <p/>
 * Like all Roo add-ons, this provider aims to expose potentially-useful
 * contents of the above files via {@link GwtScaffoldMetadata}. It also attempts
 * to avoiding writing to disk unless actually necessary.
 * <p/>
 * <p/>
 * A separate type monitors the creation/deletion of the aforementioned files to
 * maintain "global indexes".
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @author Ray Cromwell
 * @author Amit Manjhi
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class GwtScaffoldMetadataProviderImpl implements
        GwtScaffoldMetadataProvider {

    @Reference protected GwtFileManager gwtFileManager;
    @Reference protected GwtTemplateService gwtTemplateService;
    @Reference protected GwtTypeService gwtTypeService;
    @Reference protected MetadataDependencyRegistry metadataDependencyRegistry;
    @Reference protected MetadataService metadataService;
    @Reference protected ProjectOperations projectOperations;
    @Reference protected TypeLocationService typeLocationService;

    protected void activate(final ComponentContext context) {
        metadataDependencyRegistry.registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
    }

    private void buildType(final GwtType type, final String moduleName) {
        gwtTypeService.buildType(type, gwtTemplateService
                .getStaticTemplateTypeDetails(type, moduleName), moduleName);
    }

    private String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return GwtScaffoldMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        metadataDependencyRegistry.deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
    }

    @Override
    public MetadataItem get(final String metadataIdentificationString) {
        // Obtain the governor's information
        final ClassOrInterfaceTypeDetails mirroredType = getGovernor(metadataIdentificationString);
        if (mirroredType == null
                || Modifier.isAbstract(mirroredType.getModifier())) {
            return null;
        }

        final ClassOrInterfaceTypeDetails proxy = gwtTypeService
                .lookupProxyFromEntity(mirroredType);
        if (proxy == null || proxy.getDeclaredMethods().isEmpty()) {
            return null;
        }

        final ClassOrInterfaceTypeDetails request = gwtTypeService
                .lookupRequestFromEntity(mirroredType);
        if (request == null) {
            return null;
        }

        if (!GwtUtils.getBooleanAnnotationValue(proxy,
                RooJavaType.ROO_GWT_PROXY, "scaffold", false)) {
            return null;
        }

        final String moduleName = PhysicalTypeIdentifier.getPath(
                proxy.getDeclaredByMetadataId()).getModule();

        final JavaPackage topLevelPackage = projectOperations
                .getTopLevelPackage(moduleName);

        cleanUpLegacyProjects(GwtType.LIST_PLACE_RENDERER, topLevelPackage,
                "ApplicationListPlaceRenderer");

        buildType(GwtType.APP_ENTITY_TYPES_PROCESSOR, moduleName);
        buildType(GwtType.APP_REQUEST_FACTORY, moduleName);
        buildType(GwtType.LIST_PLACE_RENDERER, moduleName);
        buildType(GwtType.MASTER_ACTIVITIES, moduleName);
        buildType(GwtType.LIST_PLACE_RENDERER, moduleName);
        buildType(GwtType.DETAILS_ACTIVITIES, moduleName);
        buildType(GwtType.MOBILE_ACTIVITIES, moduleName);

        final GwtScaffoldMetadata gwtScaffoldMetadata = new GwtScaffoldMetadata(
                metadataIdentificationString);

        final Map<JavaSymbolName, GwtProxyProperty> clientSideTypeMap = new LinkedHashMap<JavaSymbolName, GwtProxyProperty>();
        for (final MethodMetadata proxyMethod : proxy.getDeclaredMethods()) {
            if (!proxyMethod.getMethodName().getSymbolName().startsWith("get")) {
                continue;
            }
            final JavaSymbolName propertyName = new JavaSymbolName(
                    StringUtils.uncapitalize(BeanInfoUtils
                            .getPropertyNameForJavaBeanMethod(proxyMethod)
                            .getSymbolName()));
            final JavaType propertyType = proxyMethod.getReturnType();
            ClassOrInterfaceTypeDetails ptmd = typeLocationService
                    .getTypeDetails(propertyType);
            if (propertyType.isCommonCollectionType()
                    && !propertyType.getParameters().isEmpty()) {
                ptmd = typeLocationService.getTypeDetails(propertyType
                        .getParameters().get(0));
            }

            final FieldMetadata field = proxy.getDeclaredField(propertyName);
            final List<AnnotationMetadata> annotations = field != null ? field
                    .getAnnotations() : Collections
                    .<AnnotationMetadata> emptyList();

            final GwtProxyProperty gwtProxyProperty = new GwtProxyProperty(
                    topLevelPackage, ptmd, propertyType,
                    propertyName.getSymbolName(), annotations, proxyMethod
                            .getMethodName().getSymbolName());
            clientSideTypeMap.put(propertyName, gwtProxyProperty);
        }

        final GwtTemplateDataHolder templateDataHolder = gwtTemplateService
                .getMirrorTemplateTypeDetails(mirroredType, clientSideTypeMap,
                        moduleName);
        final Map<GwtType, List<ClassOrInterfaceTypeDetails>> typesToBeWritten = new LinkedHashMap<GwtType, List<ClassOrInterfaceTypeDetails>>();
        final Map<String, String> xmlToBeWritten = new LinkedHashMap<String, String>();

        final Map<GwtType, JavaType> mirrorTypeMap = GwtUtils.getMirrorTypeMap(
                mirroredType.getName(), topLevelPackage);
        mirrorTypeMap.put(GwtType.PROXY, proxy.getName());
        mirrorTypeMap.put(GwtType.REQUEST, request.getName());

        for (final Map.Entry<GwtType, JavaType> entry : mirrorTypeMap
                .entrySet()) {
            final GwtType gwtType = entry.getKey();
            final JavaType javaType = entry.getValue();

            if (!gwtType.isMirrorType() || gwtType.equals(GwtType.PROXY)
                    || gwtType.equals(GwtType.REQUEST)) {
                continue;
            }

            cleanUpLegacyProjects(gwtType, topLevelPackage,
                    javaType.getSimpleTypeName());

            gwtType.dynamicallyResolveFieldsToWatch(clientSideTypeMap);
            gwtType.dynamicallyResolveMethodsToWatch(proxy.getName(),
                    clientSideTypeMap, topLevelPackage);

            final List<MemberHoldingTypeDetails> extendsTypes = gwtTypeService
                    .getExtendsTypes(templateDataHolder
                            .getTemplateTypeDetailsMap().get(gwtType));
            typesToBeWritten.put(gwtType, gwtTypeService
                    .buildType(gwtType, templateDataHolder
                            .getTemplateTypeDetailsMap().get(gwtType),
                            extendsTypes, moduleName));

            if (gwtType.isCreateUiXml()) {
                final GwtPath gwtPath = gwtType.getPath();
                final PathResolver pathResolver = projectOperations
                        .getPathResolver();
                final String webappPath = pathResolver.getIdentifier(
                        LogicalPath.getInstance(Path.SRC_MAIN_WEBAPP,
                                moduleName), moduleName);
                final String packagePath = pathResolver
                        .getIdentifier(LogicalPath.getInstance(
                                Path.SRC_MAIN_JAVA, moduleName), gwtPath
                                .getPackagePath(topLevelPackage));

                final String targetDirectory = gwtPath == GwtPath.WEB ? webappPath
                        : packagePath;
                final String destFile = targetDirectory + File.separatorChar
                        + javaType.getSimpleTypeName() + ".ui.xml";
                final String contents = gwtTemplateService.buildUiXml(
                        templateDataHolder.getXmlTemplates().get(gwtType),
                        destFile,
                        new ArrayList<MethodMetadata>(proxy
                                .getDeclaredMethods()));
                xmlToBeWritten.put(destFile, contents);
            }
        }

        // Our general strategy is to instantiate GwtScaffoldMetadata, which
        // offers a conceptual representation of what should go into the 4
        // key-specific types; after that we do comparisons and write to disk if
        // needed
        for (final Map.Entry<GwtType, List<ClassOrInterfaceTypeDetails>> entry : typesToBeWritten
                .entrySet()) {
            gwtFileManager.write(typesToBeWritten.get(entry.getKey()), entry
                    .getKey().isOverwriteConcrete());
        }
        for (final ClassOrInterfaceTypeDetails type : templateDataHolder
                .getTypeList()) {
            gwtFileManager.write(type, false);
        }
        for (final Map.Entry<String, String> entry : xmlToBeWritten.entrySet()) {
            gwtFileManager.write(entry.getKey(), entry.getValue());
        }
        for (final Map.Entry<String, String> entry : templateDataHolder
                .getXmlMap().entrySet()) {
            gwtFileManager.write(entry.getKey(), entry.getValue());
        }

        return gwtScaffoldMetadata;
    }

    private ClassOrInterfaceTypeDetails getGovernor(
            final String metadataIdentificationString) {
        final JavaType governorTypeName = GwtScaffoldMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath governorTypePath = GwtScaffoldMetadata
                .getPath(metadataIdentificationString);

        final String physicalTypeId = PhysicalTypeIdentifier.createIdentifier(
                governorTypeName, governorTypePath);
        return typeLocationService.getTypeDetails(physicalTypeId);
    }

    @Override
    public String getProvidesType() {
        return GwtScaffoldMetadata.getMetadataIdentifierType();
    }

    @Override
    public void notify(String upstreamDependency, String downstreamDependency) {
        if (MetadataIdentificationUtils
                .isIdentifyingClass(downstreamDependency)) {
            Validate.isTrue(
                    MetadataIdentificationUtils.getMetadataClass(
                            upstreamDependency).equals(
                            MetadataIdentificationUtils
                                    .getMetadataClass(PhysicalTypeIdentifier
                                            .getMetadataIdentiferType())),
                    "Expected class-level notifications only for PhysicalTypeIdentifier (not '%s')",
                    upstreamDependency);
            final ClassOrInterfaceTypeDetails cid = typeLocationService
                    .getTypeDetails(upstreamDependency);
            if (cid == null) {
                return;
            }

            if (cid.getAnnotation(RooJavaType.ROO_GWT_PROXY) != null) {
                final ClassOrInterfaceTypeDetails entityType = gwtTypeService
                        .lookupEntityFromProxy(cid);
                if (entityType != null) {
                    upstreamDependency = entityType.getDeclaredByMetadataId();
                }
            }
            else if (cid.getAnnotation(RooJavaType.ROO_GWT_REQUEST) != null) {
                final ClassOrInterfaceTypeDetails entityType = gwtTypeService
                        .lookupEntityFromRequest(cid);
                if (entityType != null) {
                    upstreamDependency = entityType.getDeclaredByMetadataId();
                }
            }
            else if (cid.getAnnotation(RooJavaType.ROO_GWT_LOCATOR) != null) {
                final ClassOrInterfaceTypeDetails entityType = gwtTypeService
                        .lookupEntityFromLocator(cid);
                if (entityType != null) {
                    upstreamDependency = entityType.getDeclaredByMetadataId();
                }
            }

            // A physical Java type has changed, and determine what the
            // corresponding local metadata identification string would have
            // been
            final JavaType typeName = PhysicalTypeIdentifier
                    .getJavaType(upstreamDependency);
            final LogicalPath typePath = PhysicalTypeIdentifier
                    .getPath(upstreamDependency);
            downstreamDependency = createLocalIdentifier(typeName, typePath);
        }

        // We only need to proceed if the downstream dependency relationship is
        // not already registered
        // (if it's already registered, the event will be delivered directly
        // later on)
        if (metadataDependencyRegistry.getDownstream(upstreamDependency)
                .contains(downstreamDependency)) {
            return;
        }

        // We should now have an instance-specific "downstream dependency" that
        // can be processed by this class
        Validate.isTrue(
                MetadataIdentificationUtils.getMetadataClass(
                        downstreamDependency).equals(
                        MetadataIdentificationUtils
                                .getMetadataClass(getProvidesType())),
                "Unexpected downstream notification for '%s' to this provider (which uses '%s')",
                downstreamDependency, getProvidesType());

        metadataService.evictAndGet(downstreamDependency);
    }

    private void cleanUpLegacyProjects(GwtType type,
            JavaPackage topLevelPackage, String simpleTypeName) {
        if (type.isCreateUiXml() || type == GwtType.MOBILE_LIST_VIEW
                || type == GwtType.LIST_PLACE_RENDERER
                || type == GwtType.LIST_PLACE_RENDERER) {
            String legacySimpleTypeName = simpleTypeName.replace("Desktop", "");

            String legacyClassName = GwtPath.MANAGED_UI
                    .packageName(topLevelPackage) + "." + legacySimpleTypeName;

            ClassOrInterfaceTypeDetails legacyView = typeLocationService
                    .getTypeDetails(new JavaType(legacyClassName));

            if (legacyView != null
                    && legacyView.getPhysicalTypeCategory() == PhysicalTypeCategory.CLASS) {
                String newClassName = type.getPath().packageName(
                        topLevelPackage)
                        + "." + simpleTypeName;

                JavaType newClass = new JavaType(newClassName);

                final String focusedModule = projectOperations
                        .getFocusedModuleName();
                final LogicalPath logicalPath = LogicalPath.getInstance(
                        SRC_MAIN_JAVA, focusedModule);

                ClassOrInterfaceTypeDetailsBuilder builder = new ClassOrInterfaceTypeDetailsBuilder(
                        PhysicalTypeIdentifier.createIdentifier(newClass,
                                logicalPath), legacyView);
                builder.setName(newClass);

                ClassOrInterfaceTypeDetails newView = builder.build();

                gwtFileManager.delete(legacyView);
                gwtFileManager.write(newView, false);

            }

            String legacyManagedClassName = GwtPath.MANAGED_UI
                    .packageName(topLevelPackage)
                    + "."
                    + legacySimpleTypeName
                    + "_Roo_Gwt";

            ClassOrInterfaceTypeDetails oldManagedDetailsView = typeLocationService
                    .getTypeDetails(new JavaType(legacyManagedClassName));

            if (oldManagedDetailsView != null) {
                gwtFileManager.delete(oldManagedDetailsView);
            }

            if (type.isCreateUiXml()) {
                String moduleName = projectOperations.getFocusedModuleName();
                final GwtPath targetPath = type.getPath();
                final GwtPath sourcePath = GwtPath.MANAGED_UI;
                final PathResolver pathResolver = projectOperations
                        .getPathResolver();
                final String sourceDirectory = pathResolver
                        .getIdentifier(LogicalPath.getInstance(
                                Path.SRC_MAIN_JAVA, moduleName), sourcePath
                                .getPackagePath(topLevelPackage));
                final String targetDirectory = pathResolver
                        .getIdentifier(LogicalPath.getInstance(
                                Path.SRC_MAIN_JAVA, moduleName), targetPath
                                .getPackagePath(topLevelPackage));

                final String sourceFile = sourceDirectory + File.separatorChar
                        + legacySimpleTypeName + ".ui.xml";
                final String destFile = targetDirectory + File.separatorChar
                        + simpleTypeName + ".ui.xml";

                if (gwtFileManager.fileExists(sourceFile)) {
                    FileReader fileReader;
                    try {
                        fileReader = new FileReader(sourceFile);
                        final DocumentBuilder docBuilder = XmlUtils
                                .getDocumentBuilder();
                        InputSource source = new InputSource();

                        source.setCharacterStream(fileReader);
                        final Document existingDocument = docBuilder
                                .parse(source);
                        existingDocument.setDocumentURI(destFile);
                        final Transformer transformer = XmlUtils
                                .createIndentingTransformer();
                        final DOMSource domSource = new DOMSource(
                                existingDocument);
                        final StreamResult result = new StreamResult(
                                new StringWriter());
                        transformer.transform(domSource, result);
                        String contents = result.getWriter().toString();
                        gwtFileManager.write(destFile, contents);
                    }
                    catch (Exception e) {
                        System.out.println(e.getMessage());
                    }

                    gwtFileManager.delete(sourceFile);
                }
            }
        }
    }
}
