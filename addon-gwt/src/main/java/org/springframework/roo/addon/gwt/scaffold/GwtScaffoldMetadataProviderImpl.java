package org.springframework.roo.addon.gwt.scaffold;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
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
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

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

    // Fields
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

    protected void deactivate(final ComponentContext context) {
        metadataDependencyRegistry.deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
    }

    public MetadataItem get(final String metadataIdentificationString) {
        // Obtain the governor's information
        ClassOrInterfaceTypeDetails mirroredType = getGovernor(metadataIdentificationString);
        if (mirroredType == null
                || Modifier.isAbstract(mirroredType.getModifier())) {
            return null;
        }

        ClassOrInterfaceTypeDetails proxy = gwtTypeService
                .lookupProxyFromEntity(mirroredType);
        if (proxy == null || proxy.getDeclaredMethods().isEmpty()) {
            return null;
        }

        ClassOrInterfaceTypeDetails request = gwtTypeService
                .lookupRequestFromEntity(mirroredType);
        if (request == null) {
            return null;
        }

        if (!GwtUtils.getBooleanAnnotationValue(proxy,
                RooJavaType.ROO_GWT_PROXY, "scaffold", false)) {
            return null;
        }

        String moduleName = PhysicalTypeIdentifier.getPath(
                proxy.getDeclaredByMetadataId()).getModule();
        buildType(GwtType.APP_ENTITY_TYPES_PROCESSOR, moduleName);
        buildType(GwtType.APP_REQUEST_FACTORY, moduleName);
        buildType(GwtType.LIST_PLACE_RENDERER, moduleName);
        buildType(GwtType.MASTER_ACTIVITIES, moduleName);
        buildType(GwtType.LIST_PLACE_RENDERER, moduleName);
        buildType(GwtType.DETAILS_ACTIVITIES, moduleName);
        buildType(GwtType.MOBILE_ACTIVITIES, moduleName);

        GwtScaffoldMetadata gwtScaffoldMetadata = new GwtScaffoldMetadata(
                metadataIdentificationString);

        final JavaPackage topLevelPackage = projectOperations
                .getTopLevelPackage(moduleName);
        Map<JavaSymbolName, GwtProxyProperty> clientSideTypeMap = new LinkedHashMap<JavaSymbolName, GwtProxyProperty>();
        for (MethodMetadata proxyMethod : proxy.getDeclaredMethods()) {
            if (!proxyMethod.getMethodName().getSymbolName().startsWith("get")) {
                continue;
            }
            JavaSymbolName propertyName = new JavaSymbolName(
                    StringUtils.uncapitalize(BeanInfoUtils
                            .getPropertyNameForJavaBeanMethod(proxyMethod)
                            .getSymbolName()));
            JavaType propertyType = proxyMethod.getReturnType();
            ClassOrInterfaceTypeDetails ptmd = typeLocationService
                    .getTypeDetails(propertyType);
            if (propertyType.isCommonCollectionType()
                    && !propertyType.getParameters().isEmpty()) {
                ptmd = typeLocationService.getTypeDetails(propertyType
                        .getParameters().get(0));
            }

            FieldMetadata field = proxy.getDeclaredField(propertyName);
            List<AnnotationMetadata> annotations = field != null ? field
                    .getAnnotations() : Collections
                    .<AnnotationMetadata> emptyList();

            GwtProxyProperty gwtProxyProperty = new GwtProxyProperty(
                    topLevelPackage, ptmd, propertyType,
                    propertyName.getSymbolName(), annotations, proxyMethod
                            .getMethodName().getSymbolName());
            clientSideTypeMap.put(propertyName, gwtProxyProperty);
        }

        GwtTemplateDataHolder templateDataHolder = gwtTemplateService
                .getMirrorTemplateTypeDetails(mirroredType, clientSideTypeMap,
                        moduleName);
        Map<GwtType, List<ClassOrInterfaceTypeDetails>> typesToBeWritten = new LinkedHashMap<GwtType, List<ClassOrInterfaceTypeDetails>>();
        Map<String, String> xmlToBeWritten = new LinkedHashMap<String, String>();

        Map<GwtType, JavaType> mirrorTypeMap = GwtUtils.getMirrorTypeMap(
                mirroredType.getName(), topLevelPackage);
        mirrorTypeMap.put(GwtType.PROXY, proxy.getName());
        mirrorTypeMap.put(GwtType.REQUEST, request.getName());

        for (Map.Entry<GwtType, JavaType> entry : mirrorTypeMap.entrySet()) {
            GwtType gwtType = entry.getKey();
            JavaType javaType = entry.getValue();
            if (!gwtType.isMirrorType() || gwtType.equals(GwtType.PROXY)
                    || gwtType.equals(GwtType.REQUEST)) {
                continue;
            }
            gwtType.dynamicallyResolveFieldsToWatch(clientSideTypeMap);
            gwtType.dynamicallyResolveMethodsToWatch(proxy.getName(),
                    clientSideTypeMap, topLevelPackage);

            List<MemberHoldingTypeDetails> extendsTypes = gwtTypeService
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
                String destFile = targetDirectory + File.separatorChar
                        + javaType.getSimpleTypeName() + ".ui.xml";
                String contents = gwtTemplateService.buildUiXml(
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
        for (Map.Entry<GwtType, List<ClassOrInterfaceTypeDetails>> entry : typesToBeWritten
                .entrySet()) {
            gwtFileManager.write(typesToBeWritten.get(entry.getKey()), entry
                    .getKey().isOverwriteConcrete());
        }
        for (ClassOrInterfaceTypeDetails type : templateDataHolder
                .getTypeList()) {
            gwtFileManager.write(type, false);
        }
        for (Map.Entry<String, String> entry : xmlToBeWritten.entrySet()) {
            gwtFileManager.write(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : templateDataHolder.getXmlMap()
                .entrySet()) {
            gwtFileManager.write(entry.getKey(), entry.getValue());
        }

        return gwtScaffoldMetadata;
    }

    private ClassOrInterfaceTypeDetails getGovernor(
            final String metadataIdentificationString) {
        JavaType governorTypeName = GwtScaffoldMetadata
                .getJavaType(metadataIdentificationString);
        LogicalPath governorTypePath = GwtScaffoldMetadata
                .getPath(metadataIdentificationString);

        String physicalTypeId = PhysicalTypeIdentifier.createIdentifier(
                governorTypeName, governorTypePath);
        return typeLocationService.getTypeDetails(physicalTypeId);
    }

    private void buildType(final GwtType type, final String moduleName) {
        gwtTypeService.buildType(type, gwtTemplateService
                .getStaticTemplateTypeDetails(type, moduleName), moduleName);
    }

    public void notify(String upstreamDependency, String downstreamDependency) {
        if (MetadataIdentificationUtils
                .isIdentifyingClass(downstreamDependency)) {
            Assert.isTrue(
                    MetadataIdentificationUtils.getMetadataClass(
                            upstreamDependency).equals(
                            MetadataIdentificationUtils
                                    .getMetadataClass(PhysicalTypeIdentifier
                                            .getMetadataIdentiferType())),
                    "Expected class-level notifications only for PhysicalTypeIdentifier (not '"
                            + upstreamDependency + "')");
            ClassOrInterfaceTypeDetails cid = typeLocationService
                    .getTypeDetails(upstreamDependency);
            if (cid == null) {
                return;
            }

            if (cid.getAnnotation(RooJavaType.ROO_GWT_PROXY) != null) {
                ClassOrInterfaceTypeDetails entityType = gwtTypeService
                        .lookupEntityFromProxy(cid);
                if (entityType != null) {
                    upstreamDependency = entityType.getDeclaredByMetadataId();
                }
            }
            else if (cid.getAnnotation(RooJavaType.ROO_GWT_REQUEST) != null) {
                ClassOrInterfaceTypeDetails entityType = gwtTypeService
                        .lookupEntityFromRequest(cid);
                if (entityType != null) {
                    upstreamDependency = entityType.getDeclaredByMetadataId();
                }
            }
            else if (cid.getAnnotation(RooJavaType.ROO_GWT_LOCATOR) != null) {
                ClassOrInterfaceTypeDetails entityType = gwtTypeService
                        .lookupEntityFromLocator(cid);
                if (entityType != null) {
                    upstreamDependency = entityType.getDeclaredByMetadataId();
                }
            }

            // A physical Java type has changed, and determine what the
            // corresponding local metadata identification string would have
            // been
            JavaType typeName = PhysicalTypeIdentifier
                    .getJavaType(upstreamDependency);
            LogicalPath typePath = PhysicalTypeIdentifier
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
        Assert.isTrue(
                MetadataIdentificationUtils.getMetadataClass(
                        downstreamDependency).equals(
                        MetadataIdentificationUtils
                                .getMetadataClass(getProvidesType())),
                "Unexpected downstream notification for '"
                        + downstreamDependency
                        + "' to this provider (which uses '"
                        + getProvidesType() + "'");

        metadataService.evictAndGet(downstreamDependency);
    }

    private String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return GwtScaffoldMetadata.createIdentifier(javaType, path);
    }

    public String getProvidesType() {
        return GwtScaffoldMetadata.getMetadataIdentifierType();
    }
}
