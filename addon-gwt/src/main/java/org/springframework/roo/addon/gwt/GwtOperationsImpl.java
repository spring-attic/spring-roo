package org.springframework.roo.addon.gwt;

import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.addon.gwt.GwtJavaType.ENTITY_PROXY;
import static org.springframework.roo.addon.gwt.GwtJavaType.OLD_ENTITY_PROXY;
import static org.springframework.roo.addon.gwt.GwtJavaType.OLD_REQUEST_CONTEXT;
import static org.springframework.roo.addon.gwt.GwtJavaType.PROXY_FOR_NAME;
import static org.springframework.roo.addon.gwt.GwtJavaType.REQUEST_CONTEXT;
import static org.springframework.roo.classpath.PhysicalTypeCategory.INTERFACE;
import static org.springframework.roo.model.RooJavaType.ROO_GWT_MIRRORED_FROM;
import static org.springframework.roo.model.RooJavaType.ROO_GWT_PROXY;
import static org.springframework.roo.model.RooJavaType.ROO_GWT_REQUEST;
import static org.springframework.roo.model.RooJavaType.ROO_GWT_UNMANAGED_REQUEST;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ACTIVE_RECORD;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ENTITY;
import static org.springframework.roo.project.Path.ROOT;
import static org.springframework.roo.project.Path.SRC_MAIN_JAVA;
import static org.springframework.roo.project.Path.SRC_MAIN_WEBAPP;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.gwt.request.GwtRequestMetadata;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.Repository;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.CollectionUtils;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.WebXmlUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link GwtOperations}.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @author Stefan Schmidt
 * @author Ray Cromwell
 * @author Amit Manjhi
 * @since 1.1
 */
@Component
@Service
public class GwtOperationsImpl implements GwtOperations {

    private static final String GWT_BUILD_COMMAND = "com.google.gwt.eclipse.core.gwtProjectValidator";
    private static final String GWT_PROJECT_NATURE = "com.google.gwt.eclipse.core.gwtNature";
    private static final String MAVEN_ECLIPSE_PLUGIN = "/project/build/plugins/plugin[artifactId = 'maven-eclipse-plugin']";
    private static final String OUTPUT_DIRECTORY = "${project.build.directory}/${project.build.finalName}/WEB-INF/classes";
    private static final JavaSymbolName VALUE = new JavaSymbolName("value");

    @Reference protected FileManager fileManager;
    @Reference protected GwtTemplateService gwtTemplateService;
    @Reference protected GwtTypeService gwtTypeService;
    @Reference protected MetadataService metadataService;
    @Reference protected PersistenceMemberLocator persistenceMemberLocator;
    @Reference protected ProjectOperations projectOperations;
    @Reference protected TypeLocationService typeLocationService;
    @Reference protected TypeManagementService typeManagementService;
    @Reference protected WebMvcOperations webMvcOperations;

    private Boolean wasGaeEnabled;
    private ComponentContext context;

    protected void activate(final ComponentContext context) {
        this.context = context;
    }

    private void addPackageToGwtXml(final JavaPackage sourcePackage) {
        String gwtConfig = gwtTypeService.getGwtModuleXml(projectOperations
                .getFocusedModuleName());
        gwtConfig = StringUtils.stripEnd(gwtConfig, File.separator);
        final String moduleRoot = projectOperations.getPathResolver()
                .getFocusedRoot(SRC_MAIN_JAVA);
        final String topLevelPackage = gwtConfig.replace(
                FileUtils.ensureTrailingSeparator(moduleRoot), "").replace(
                File.separator, ".");
        final String relativePackage = StringUtils.removeStart(
                sourcePackage.getFullyQualifiedPackageName(), topLevelPackage
                        + ".");
        gwtTypeService.addSourcePath(
                relativePackage.replace(".", PATH_DELIMITER),
                projectOperations.getFocusedModuleName());
    }

    private void copyDirectoryContents() {
        for (final GwtPath path : GwtPath.values()) {
            copyDirectoryContents(path);
        }
    }

    private void copyDirectoryContents(final GwtPath gwtPath) {
        final String sourceAntPath = gwtPath.getSourceAntPath();
        if (sourceAntPath.contains("gae")
                && !projectOperations
                        .isFeatureInstalledInFocusedModule(FeatureNames.GAE)) {
            return;
        }
        final String targetDirectory = gwtPath == GwtPath.WEB ? projectOperations
                .getPathResolver().getFocusedRoot(SRC_MAIN_WEBAPP)
                : projectOperations.getPathResolver().getFocusedIdentifier(
                        SRC_MAIN_JAVA,
                        gwtPath.getPackagePath(projectOperations
                                .getFocusedTopLevelPackage()));
        updateFile(sourceAntPath, targetDirectory, gwtPath.segmentPackage(),
                false);
    }

    private void createProxy(final ClassOrInterfaceTypeDetails entity,
            final JavaPackage destinationPackage) {
        final ClassOrInterfaceTypeDetails existingProxy = gwtTypeService
                .lookupProxyFromEntity(entity);
        if (existingProxy != null || entity.isAbstract()) {
            return;
        }

        final JavaType proxyType = new JavaType(
                destinationPackage.getFullyQualifiedPackageName() + "."
                        + entity.getName().getSimpleTypeName() + "Proxy");
        final String focusedModule = projectOperations.getFocusedModuleName();
        final LogicalPath proxyLogicalPath = LogicalPath.getInstance(
                SRC_MAIN_JAVA, focusedModule);
        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                PhysicalTypeIdentifier.createIdentifier(proxyType,
                        proxyLogicalPath));
        cidBuilder.setName(proxyType);
        cidBuilder.setExtendsTypes(Collections.singletonList(ENTITY_PROXY));
        cidBuilder.setPhysicalTypeCategory(INTERFACE);
        cidBuilder.setModifier(PUBLIC);
        final List<AnnotationAttributeValue<?>> attributeValues = new ArrayList<AnnotationAttributeValue<?>>();
        final StringAttributeValue stringAttributeValue = new StringAttributeValue(
                VALUE, entity.getName().getFullyQualifiedTypeName());
        attributeValues.add(stringAttributeValue);
        final String locator = projectOperations
                .getTopLevelPackage(focusedModule)
                + ".server.locator."
                + entity.getName().getSimpleTypeName() + "Locator";
        final StringAttributeValue locatorAttributeValue = new StringAttributeValue(
                new JavaSymbolName("locator"), locator);
        attributeValues.add(locatorAttributeValue);
        cidBuilder.updateTypeAnnotation(new AnnotationMetadataBuilder(
                PROXY_FOR_NAME, attributeValues));
        attributeValues.remove(locatorAttributeValue);
        final List<StringAttributeValue> readOnlyValues = new ArrayList<StringAttributeValue>();
        final FieldMetadata versionField = persistenceMemberLocator
                .getVersionField(entity.getName());
        if (versionField != null) {
            readOnlyValues.add(new StringAttributeValue(VALUE, versionField
                    .getFieldName().getSymbolName()));
        }
        final List<FieldMetadata> idFields = persistenceMemberLocator
                .getIdentifierFields(entity.getName());
        if (!CollectionUtils.isEmpty(idFields)) {
            readOnlyValues.add(new StringAttributeValue(VALUE, idFields.get(0)
                    .getFieldName().getSymbolName()));
        }
        final ArrayAttributeValue<StringAttributeValue> readOnlyAttribute = new ArrayAttributeValue<StringAttributeValue>(
                new JavaSymbolName("readOnly"), readOnlyValues);
        attributeValues.add(readOnlyAttribute);
        cidBuilder.updateTypeAnnotation(new AnnotationMetadataBuilder(
                ROO_GWT_PROXY, attributeValues));
        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
        addPackageToGwtXml(destinationPackage);
    }

    /**
     * Builds the given entity's managed RequestContext interface. Note that we
     * don't generate the entire interface here, only the @RooGwtRequest
     * annotation; we then invoke the metadata provider, which takes over and
     * generates the remaining code, namely the method declarations and the @ServiceName
     * annotation. This is analogous to how ITD-based addons work, e.g. adding a
     * trigger annotation and letting the metadata provider do the rest. This
     * allows for the metadata provider to correctly respond to project changes.
     * 
     * @param entity the entity for which to create the GWT request interface
     *            (required)
     * @param destinationPackage the package in which to create the request
     *            interface (required)
     */
    private void createRequestInterface(
            final ClassOrInterfaceTypeDetails entity,
            final JavaPackage destinationPackage) {
        final JavaType requestType = new JavaType(
                destinationPackage.getFullyQualifiedPackageName() + "."
                        + entity.getType().getSimpleTypeName()
                        + "Request_Roo_Gwt");
        final LogicalPath focusedSrcMainJava = LogicalPath.getInstance(
                SRC_MAIN_JAVA, projectOperations.getFocusedModuleName());
        final ClassOrInterfaceTypeDetailsBuilder requestBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                PhysicalTypeIdentifier.createIdentifier(requestType,
                        focusedSrcMainJava));
        requestBuilder.setName(requestType);
        requestBuilder.addExtendsTypes(REQUEST_CONTEXT);
        requestBuilder.setPhysicalTypeCategory(INTERFACE);
        requestBuilder.setModifier(PUBLIC);
        requestBuilder.addAnnotation(getRooGwtRequestAnnotation(entity));
        typeManagementService.createOrUpdateTypeOnDisk(requestBuilder.build());
        addPackageToGwtXml(destinationPackage);
        // Trigger the GwtRequestMetadataProvider to finish generating the code
        metadataService.get(GwtRequestMetadata.createIdentifier(requestType,
                focusedSrcMainJava));
    }

    /**
     * Builds the given entity's unmanaged RequestContext interface used for
     * adding custom methods. This interface extends the RequestContext
     * interface managed by Roo.
     * 
     * @param entity the entity for which to create the GWT request interface
     *            (required)
     * @param destinationPackage the package in which to create the request
     *            interface (required)
     */
    private void createUnmanagedRequestInterface(
            final ClassOrInterfaceTypeDetails entity,
            JavaPackage destinationPackage) {
        final ClassOrInterfaceTypeDetails managedRequest = gwtTypeService
                .lookupRequestFromEntity(entity);

        if (managedRequest == null)
            return;

        final JavaType unmanagedRequestType = new JavaType(
                destinationPackage.getFullyQualifiedPackageName() + "."
                        + entity.getType().getSimpleTypeName() + "Request");

        final LogicalPath focusedSrcMainJava = LogicalPath.getInstance(
                SRC_MAIN_JAVA, projectOperations.getFocusedModuleName());
        final ClassOrInterfaceTypeDetailsBuilder unmanagedRequestBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                PhysicalTypeIdentifier.createIdentifier(unmanagedRequestType,
                        focusedSrcMainJava));
        unmanagedRequestBuilder.setName(unmanagedRequestType);
        unmanagedRequestBuilder.addExtendsTypes(managedRequest.getType());
        unmanagedRequestBuilder.setPhysicalTypeCategory(INTERFACE);
        unmanagedRequestBuilder.setModifier(PUBLIC);
        unmanagedRequestBuilder
                .addAnnotation(getRooGwtUnmanagedRequestAnnotation(entity));
        unmanagedRequestBuilder.addAnnotation(managedRequest
                .getAnnotation(GwtJavaType.SERVICE_NAME));
        typeManagementService.createOrUpdateTypeOnDisk(unmanagedRequestBuilder
                .build());

    }

    private void createRequestInterfaceIfNecessary(
            final ClassOrInterfaceTypeDetails entity,
            final JavaPackage destinationPackage) {
        if (entity != null && !entity.isAbstract()
                && gwtTypeService.lookupRequestFromEntity(entity) == null) {
            createRequestInterface(entity, destinationPackage);

            createUnmanagedRequestInterface(entity, destinationPackage);
        }
    }

    private void createScaffold(final ClassOrInterfaceTypeDetails proxy) {
        final AnnotationMetadata annotationMetadata = GwtUtils
                .getFirstAnnotation(proxy, ROO_GWT_PROXY);
        if (annotationMetadata != null) {
            final AnnotationAttributeValue<Boolean> booleanAttributeValue = annotationMetadata
                    .getAttribute("scaffold");
            if (booleanAttributeValue == null
                    || !booleanAttributeValue.getValue()) {
                final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                        proxy);
                final AnnotationMetadataBuilder annotationMetadataBuilder = new AnnotationMetadataBuilder(
                        annotationMetadata);
                annotationMetadataBuilder.addBooleanAttribute("scaffold", true);
                for (final AnnotationMetadataBuilder existingAnnotation : cidBuilder
                        .getAnnotations()) {
                    if (existingAnnotation.getAnnotationType().equals(
                            annotationMetadata.getAnnotationType())) {
                        cidBuilder.getAnnotations().remove(existingAnnotation);
                        cidBuilder.getAnnotations().add(
                                annotationMetadataBuilder);
                        break;
                    }
                }
                typeManagementService.createOrUpdateTypeOnDisk(cidBuilder
                        .build());
            }
        }
    }

    private void deleteUntouchedSetupFiles(final String sourceAntPath,
            String targetDirectory) {
        if (!targetDirectory.endsWith(File.separator)) {
            targetDirectory += File.separator;
        }
        if (!fileManager.exists(targetDirectory)) {
            fileManager.createDirectory(targetDirectory);
        }

        final String path = FileUtils.getPath(getClass(), sourceAntPath);
        final Iterable<URL> uris = OSGiUtils.findEntriesByPattern(
                context.getBundleContext(), path);
        Validate.notNull(uris,
                "Could not search bundles for resources for Ant Path '%s'",
                path);

        for (final URL url : uris) {
            String fileName = url.getPath().substring(
                    url.getPath().lastIndexOf('/') + 1);
            fileName = fileName.replace("-template", "");
            final String targetFilename = targetDirectory + fileName;
            if (!fileManager.exists(targetFilename)) {
                continue;
            }
            try {
                String input = IOUtils.toString(url);
                input = processTemplate(input, null);
                final String existing = org.apache.commons.io.FileUtils
                        .readFileToString(new File(targetFilename));
                if (existing.equals(input)) {
                    // new File(targetFilename).delete();
                    fileManager.delete(targetFilename);
                }
            }
            catch (final IOException ignored) {
            }
        }
    }

    private CharSequence getGaeHookup() {
        final StringBuilder builder = new StringBuilder(
                "// AppEngine user authentication\n\n");
        builder.append("new GaeLoginWidgetDriver(requestFactory).setWidget(shell.getLoginWidget());\n\n");
        builder.append("new ReloadOnAuthenticationFailure().register(eventBus);\n\n");
        return builder.toString();
    }

    public String getName() {
        return FeatureNames.GWT;
    }

    private String getPomPath() {
        return projectOperations.getPathResolver().getFocusedIdentifier(ROOT,
                "pom.xml");
    }

    private AnnotationMetadata getRooGwtRequestAnnotation(
            final ClassOrInterfaceTypeDetails entity) {
        // The GwtRequestMetadataProvider doesn't need to know excluded methods
        // any more because it actively adds the required CRUD methods itself.
        final StringAttributeValue entityAttributeValue = new StringAttributeValue(
                VALUE, entity.getType().getFullyQualifiedTypeName());
        final List<AnnotationAttributeValue<?>> gwtRequestAttributeValues = new ArrayList<AnnotationAttributeValue<?>>();
        gwtRequestAttributeValues.add(entityAttributeValue);
        return new AnnotationMetadataBuilder(ROO_GWT_REQUEST,
                gwtRequestAttributeValues).build();
    }

    private AnnotationMetadata getRooGwtUnmanagedRequestAnnotation(
            final ClassOrInterfaceTypeDetails entity) {
        final StringAttributeValue entityAttributeValue = new StringAttributeValue(
                VALUE, entity.getType().getFullyQualifiedTypeName());
        final List<AnnotationAttributeValue<?>> gwtRequestAttributeValues = new ArrayList<AnnotationAttributeValue<?>>();
        gwtRequestAttributeValues.add(entityAttributeValue);
        return new AnnotationMetadataBuilder(ROO_GWT_UNMANAGED_REQUEST,
                gwtRequestAttributeValues).build();
    }

    public boolean isGwtInstallationPossible() {
        return projectOperations.isFocusedProjectAvailable()
                && !projectOperations
                        .isFeatureInstalledInFocusedModule(FeatureNames.JSF);
    }

    public boolean isInstalledInModule(final String moduleName) {
        final Pom pom = projectOperations.getPomFromModuleName(moduleName);
        if (pom == null) {
            return false;
        }
        for (final Plugin buildPlugin : pom.getBuildPlugins()) {
            if ("gwt-maven-plugin".equals(buildPlugin.getArtifactId())) {
                return true;
            }
        }
        return false;
    }

    public boolean isScaffoldAvailable() {
        return isGwtInstallationPossible()
                && isInstalledInModule(projectOperations.getFocusedModuleName());
    }

    private String processTemplate(String input, String segmentPackage) {
        if (segmentPackage == null) {
            segmentPackage = "";
        }
        final String topLevelPackage = projectOperations.getTopLevelPackage(
                projectOperations.getFocusedModuleName())
                .getFullyQualifiedPackageName();
        input = input.replace("__TOP_LEVEL_PACKAGE__", topLevelPackage);
        input = input.replace("__SEGMENT_PACKAGE__", segmentPackage);
        input = input.replace("__PROJECT_NAME__", projectOperations
                .getProjectName(projectOperations.getFocusedModuleName()));

        if (projectOperations
                .isFeatureInstalledInFocusedModule(FeatureNames.GAE)) {
            input = input.replace("__GAE_IMPORT__", "import " + topLevelPackage
                    + ".client.scaffold.gae.*;\n");
            input = input.replace("__GAE_HOOKUP__", getGaeHookup());
            input = input.replace("__GAE_REQUEST_TRANSPORT__",
                    ", new GaeAuthRequestTransport(eventBus)");
        }
        else {
            input = input.replace("__GAE_IMPORT__", "");
            input = input.replace("__GAE_HOOKUP__", "");
            input = input.replace("__GAE_REQUEST_TRANSPORT__", "");
        }
        return input;
    }

    public void proxyAll(final JavaPackage proxyPackage) {
        for (final ClassOrInterfaceTypeDetails entity : typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(ROO_JPA_ENTITY,
                        ROO_JPA_ACTIVE_RECORD)) {
            createProxy(entity, proxyPackage);
        }
        copyDirectoryContents(GwtPath.LOCATOR);
    }

    public void proxyAndRequestAll(final JavaPackage proxyAndRequestPackage) {
        proxyAll(proxyAndRequestPackage);
        requestAll(proxyAndRequestPackage);
    }

    public void proxyAndRequestType(final JavaPackage proxyAndRequestPackage,
            final JavaType type) {
        proxyType(proxyAndRequestPackage, type);
        requestType(proxyAndRequestPackage, type);
    }

    public void proxyType(final JavaPackage proxyPackage, final JavaType type) {
        final ClassOrInterfaceTypeDetails entity = typeLocationService
                .getTypeDetails(type);
        if (entity != null) {
            createProxy(entity, proxyPackage);
        }
        copyDirectoryContents(GwtPath.LOCATOR);
    }

    private void removeIfFound(final String xpath, final Element webXmlRoot) {
        for (Element toRemove : XmlUtils.findElements(xpath, webXmlRoot)) {
            if (toRemove != null) {
                toRemove.getParentNode().removeChild(toRemove);
                toRemove = null;
            }
        }
    }

    public void requestAll(final JavaPackage proxyPackage) {
        for (final ClassOrInterfaceTypeDetails entity : typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(ROO_JPA_ENTITY,
                        ROO_JPA_ACTIVE_RECORD)) {
            createRequestInterfaceIfNecessary(entity, proxyPackage);
        }
    }

    public void requestType(final JavaPackage requestPackage,
            final JavaType type) {
        createRequestInterfaceIfNecessary(
                typeLocationService.getTypeDetails(type), requestPackage);
    }

    public void scaffoldAll(final JavaPackage proxyPackage,
            final JavaPackage requestPackage) {
        updateScaffoldBoilerPlate();
        proxyAll(proxyPackage);
        requestAll(requestPackage);
        for (final ClassOrInterfaceTypeDetails proxy : typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(ROO_GWT_PROXY)) {
            final ClassOrInterfaceTypeDetails request = gwtTypeService
                    .lookupRequestFromProxy(proxy);
            if (request == null) {
                throw new IllegalStateException(
                        "In order to scaffold, an entity must have a request");
            }
            createScaffold(proxy);
        }
    }

    public void scaffoldType(final JavaPackage proxyPackage,
            final JavaPackage requestPackage, final JavaType type) {
        proxyType(proxyPackage, type);
        requestType(requestPackage, type);
        final ClassOrInterfaceTypeDetails entity = typeLocationService
                .getTypeDetails(type);
        if (entity != null && !entity.isAbstract()) {
            final ClassOrInterfaceTypeDetails proxy = gwtTypeService
                    .lookupProxyFromEntity(entity);
            final ClassOrInterfaceTypeDetails request = gwtTypeService
                    .lookupRequestFromEntity(entity);
            if (proxy == null || request == null) {
                throw new IllegalStateException(
                        "In order to scaffold, an entity must have an associated proxy and request");
            }
            updateScaffoldBoilerPlate();
            createScaffold(proxy);
        }
    }

    public void setup() {
        // Install web pieces if not already installed
        if (!fileManager.exists(projectOperations.getPathResolver()
                .getFocusedIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/web.xml"))) {
            webMvcOperations.installAllWebMvcArtifacts();
        }

        final String topPackageName = projectOperations.getTopLevelPackage(
                projectOperations.getFocusedModuleName())
                .getFullyQualifiedPackageName();
        final Set<FileDetails> gwtConfigs = fileManager
                .findMatchingAntPath(projectOperations.getPathResolver()
                        .getFocusedRoot(SRC_MAIN_JAVA)
                        + File.separatorChar
                        + topPackageName.replace('.', File.separatorChar)
                        + File.separator + "*.gwt.xml");
        final boolean gwtAlreadySetup = !gwtConfigs.isEmpty();

        if (!gwtAlreadySetup) {
            String sourceAntPath = "setup/*";
            final String targetDirectory = projectOperations.getPathResolver()
                    .getFocusedIdentifier(SRC_MAIN_JAVA,
                            topPackageName.replace('.', File.separatorChar));
            updateFile(sourceAntPath, targetDirectory, "", false);

            sourceAntPath = "setup/client/*";
            updateFile(sourceAntPath, targetDirectory + "/client", "", false);
        }

        for (final ClassOrInterfaceTypeDetails proxyOrRequest : typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(ROO_GWT_MIRRORED_FROM)) {
            final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                    proxyOrRequest);
            if (proxyOrRequest.extendsType(ENTITY_PROXY)
                    || proxyOrRequest.extendsType(OLD_ENTITY_PROXY)) {
                final AnnotationMetadata annotationMetadata = MemberFindingUtils
                        .getAnnotationOfType(proxyOrRequest.getAnnotations(),
                                ROO_GWT_MIRRORED_FROM);
                if (annotationMetadata != null) {
                    final AnnotationMetadataBuilder annotationMetadataBuilder = new AnnotationMetadataBuilder(
                            annotationMetadata);
                    annotationMetadataBuilder.setAnnotationType(ROO_GWT_PROXY);
                    cidBuilder.removeAnnotation(ROO_GWT_MIRRORED_FROM);
                    cidBuilder.addAnnotation(annotationMetadataBuilder);
                    typeManagementService.createOrUpdateTypeOnDisk(cidBuilder
                            .build());
                }
            }
            else if (proxyOrRequest.extendsType(REQUEST_CONTEXT)
                    || proxyOrRequest.extendsType(OLD_REQUEST_CONTEXT)) {
                final AnnotationMetadata annotationMetadata = MemberFindingUtils
                        .getAnnotationOfType(proxyOrRequest.getAnnotations(),
                                ROO_GWT_MIRRORED_FROM);
                if (annotationMetadata != null) {
                    final AnnotationMetadataBuilder annotationMetadataBuilder = new AnnotationMetadataBuilder(
                            annotationMetadata);
                    annotationMetadataBuilder
                            .setAnnotationType(ROO_GWT_REQUEST);
                    cidBuilder.removeAnnotation(ROO_GWT_MIRRORED_FROM);
                    cidBuilder.addAnnotation(annotationMetadataBuilder);
                    typeManagementService.createOrUpdateTypeOnDisk(cidBuilder
                            .build());
                }
            }
        }

        // Add GWT natures and builder names to maven eclipse plugin
        updateEclipsePlugin();

        // Add outputDirectory to build element of pom
        updateBuildOutputDirectory();

        final Element configuration = XmlUtils.getConfiguration(getClass());

        // Add properties
        updateProperties(configuration,
                projectOperations
                        .isFeatureInstalledInFocusedModule(FeatureNames.GAE));

        // Add POM repositories
        updateRepositories(configuration);

        // Add dependencies
        updateDependencies(configuration);

        // Update web.xml
        updateWebXml();

        // Update gwt-maven-plugin and others
        updateBuildPlugins(projectOperations
                .isFeatureInstalledInFocusedModule(FeatureNames.GAE));
    }

    /**
     * Sets the POM's output directory to {@value #OUTPUT_DIRECTORY}, if it's
     * not already set to something else.
     */
    private void updateBuildOutputDirectory() {
        // Read the POM
        final String pom = getPomPath();
        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(pom));
        final Element root = document.getDocumentElement();

        Element outputDirectoryElement = XmlUtils.findFirstElement(
                "/project/build/outputDirectory", root);
        if (outputDirectoryElement == null) {
            // Create it
            final Element buildElement = XmlUtils.findRequiredElement(
                    "/project/build", root);
            outputDirectoryElement = DomUtils.createChildElement(
                    "outputDirectory", buildElement, document);
        }
        outputDirectoryElement.setTextContent(OUTPUT_DIRECTORY);

        fileManager.createOrUpdateTextFileIfRequired(pom,
                XmlUtils.nodeToString(document), false);
    }

    private void updateBuildPlugins(final boolean isGaeEnabled) {
        // Update the POM
        final List<Plugin> plugins = new ArrayList<Plugin>();
        final String xPathExpression = "/configuration/"
                + (isGaeEnabled ? "gae" : "gwt") + "/plugins/plugin";
        final List<Element> pluginElements = XmlUtils.findElements(
                xPathExpression, XmlUtils.getConfiguration(getClass()));
        for (final Element pluginElement : pluginElements) {
            plugins.add(new Plugin(pluginElement));
        }
        projectOperations.addBuildPlugins(
                projectOperations.getFocusedModuleName(), plugins);
    }

    private void updateDependencies(final Element configuration) {
        final List<Dependency> dependencies = new ArrayList<Dependency>();
        final List<Element> gwtDependencies = XmlUtils.findElements(
                "/configuration/gwt/dependencies/dependency", configuration);
        for (final Element dependencyElement : gwtDependencies) {
            dependencies.add(new Dependency(dependencyElement));
        }
        projectOperations.addDependencies(
                projectOperations.getFocusedModuleName(), dependencies);
    }

    private void updateProperties(final Element configuration,
            final boolean isGaeEnabled) {
        // Update the POM
        final String xPathExpression = "/configuration/"
                + (isGaeEnabled ? "gae" : "gwt") + "/properties/*";
        final List<Element> propertyElements = XmlUtils.findElements(
                xPathExpression, configuration);
        for (final Element property : propertyElements) {
            projectOperations.addProperty(projectOperations
                    .getFocusedModuleName(), new Property(property));
        }
    }

    /**
     * Updates the Eclipse plugin in the POM with the necessary GWT details
     */
    private void updateEclipsePlugin() {
        // Load the POM
        final String pom = getPomPath();
        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(pom));
        final Element root = document.getDocumentElement();

        // Add the GWT "buildCommand"
        final Element additionalBuildCommandsElement = XmlUtils
                .findFirstElement(MAVEN_ECLIPSE_PLUGIN
                        + "/configuration/additionalBuildcommands", root);
        Validate.notNull(additionalBuildCommandsElement,
                "additionalBuildcommands element of the maven-eclipse-plugin required");
        Element gwtBuildCommandElement = XmlUtils.findFirstElement(
                "buildCommand[name = '" + GWT_BUILD_COMMAND + "']",
                additionalBuildCommandsElement);
        if (gwtBuildCommandElement == null) {
            gwtBuildCommandElement = DomUtils.createChildElement(
                    "buildCommand", additionalBuildCommandsElement, document);
            final Element nameElement = DomUtils.createChildElement("name",
                    gwtBuildCommandElement, document);
            nameElement.setTextContent(GWT_BUILD_COMMAND);
        }

        // Add the GWT "projectnature"
        final Element additionalProjectNaturesElement = XmlUtils
                .findFirstElement(MAVEN_ECLIPSE_PLUGIN
                        + "/configuration/additionalProjectnatures", root);
        Validate.notNull(additionalProjectNaturesElement,
                "additionalProjectnatures element of the maven-eclipse-plugin required");
        Element gwtProjectNatureElement = null;
        List<Element> gwtProjectNatureElements = XmlUtils.findElements(
                "projectnature", additionalProjectNaturesElement);
        for (Element element : gwtProjectNatureElements) {
            if (GWT_PROJECT_NATURE.equals(element.getTextContent())) {
                gwtProjectNatureElement = element;
                break;
            }
        }
        if (gwtProjectNatureElement == null) {
            gwtProjectNatureElement = new XmlElementBuilder("projectnature",
                    document).setText(GWT_PROJECT_NATURE).build();
            additionalProjectNaturesElement
                    .appendChild(gwtProjectNatureElement);
        }

        fileManager.createOrUpdateTextFileIfRequired(pom,
                XmlUtils.nodeToString(document), false);
    }

    private void updateFile(final String sourceAntPath, String targetDirectory,
            final String segmentPackage, final boolean overwrite) {
        if (!targetDirectory.endsWith(File.separator)) {
            targetDirectory += File.separator;
        }
        if (!fileManager.exists(targetDirectory)) {
            fileManager.createDirectory(targetDirectory);
        }

        final String path = FileUtils.getPath(getClass(), sourceAntPath);
        final Iterable<URL> urls = OSGiUtils.findEntriesByPattern(
                context.getBundleContext(), path);
        Validate.notNull(urls,
                "Could not search bundles for resources for Ant Path '%s'",
                path);

        for (final URL url : urls) {
            String fileName = url.getPath().substring(
                    url.getPath().lastIndexOf('/') + 1);
            fileName = fileName.replace("-template", "");
            final String targetFilename = targetDirectory + fileName;

            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                if (fileManager.exists(targetFilename) && !overwrite) {
                    continue;
                }
                if (targetFilename.endsWith("png")) {
                    inputStream = url.openStream();
                    outputStream = fileManager.createFile(targetFilename)
                            .getOutputStream();
                    IOUtils.copy(inputStream, outputStream);
                }
                else {
                    // Read template and insert the user's package
                    String input = IOUtils.toString(url);
                    input = processTemplate(input, segmentPackage);

                    // Output the file for the user
                    fileManager.createOrUpdateTextFileIfRequired(
                            targetFilename, input, true);
                }
            }
            catch (final IOException e) {
                throw new IllegalStateException("Unable to create '"
                        + targetFilename + "'", e);
            }
            finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }
    }

    public void updateGaeConfiguration() {
        final boolean isGaeEnabled = projectOperations
                .isFeatureInstalledInFocusedModule(FeatureNames.GAE);
        final boolean hasGaeStateChanged = wasGaeEnabled == null
                || isGaeEnabled != wasGaeEnabled;
        if (!isInstalledInModule(projectOperations.getFocusedModuleName())
                || !hasGaeStateChanged) {
            return;
        }

        wasGaeEnabled = isGaeEnabled;

        // Update the GaeHelper type
        updateGaeHelper();

        gwtTypeService.buildType(GwtType.APP_REQUEST_FACTORY,
                gwtTemplateService.getStaticTemplateTypeDetails(
                        GwtType.APP_REQUEST_FACTORY, projectOperations
                                .getFocusedProjectMetadata().getModuleName()),
                projectOperations.getFocusedModuleName());

        // Ensure the gwt-maven-plugin appropriate to a GAE enabled or disabled
        // environment is updated
        updateBuildPlugins(isGaeEnabled);

        // If there is a class that could possibly import from the appengine
        // sdk, denoted here as having Gae in the type name,
        // then we need to add the appengine-api-1.0-sdk dependency to the
        // pom.xml file
        final String rootPath = projectOperations.getPathResolver()
                .getFocusedRoot(ROOT);
        final Set<FileDetails> files = fileManager.findMatchingAntPath(rootPath
                + "/**/*Gae*.java");
        if (!files.isEmpty()) {
            final Element configuration = XmlUtils.getConfiguration(getClass());
            final Element gaeDependency = XmlUtils
                    .findFirstElement(
                            "/configuration/gae/dependencies/dependency",
                            configuration);
            projectOperations.addDependency(projectOperations
                    .getFocusedModuleName(), new Dependency(gaeDependency));
        }

        // Copy across any missing files, only if GAE state has changed and is
        // now enabled
        if (isGaeEnabled) {
            copyDirectoryContents();
        }
    }

    private void updateGaeHelper() {
        final String sourceAntPath = "module/client/scaffold/gae/GaeHelper-template.java";
        final String segmentPackage = "client.scaffold.gae";
        final String targetDirectory = projectOperations.getPathResolver()
                .getFocusedIdentifier(
                        SRC_MAIN_JAVA,
                        projectOperations
                                .getTopLevelPackage(
                                        projectOperations
                                                .getFocusedModuleName())
                                .getFullyQualifiedPackageName()
                                .replace('.', File.separatorChar)
                                + File.separator
                                + "client"
                                + File.separator
                                + "scaffold" + File.separator + "gae");
        updateFile(sourceAntPath, targetDirectory, segmentPackage, true);
    }

    private void updateRepositories(final Element configuration) {
        final List<Repository> repositories = new ArrayList<Repository>();

        final List<Element> gwtRepositories = XmlUtils.findElements(
                "/configuration/gwt/repositories/repository", configuration);
        for (final Element repositoryElement : gwtRepositories) {
            repositories.add(new Repository(repositoryElement));
        }
        projectOperations.addRepositories(
                projectOperations.getFocusedModuleName(), repositories);

        repositories.clear();
        final List<Element> gwtPluginRepositories = XmlUtils.findElements(
                "/configuration/gwt/pluginRepositories/pluginRepository",
                configuration);
        for (final Element repositoryElement : gwtPluginRepositories) {
            repositories.add(new Repository(repositoryElement));
        }
        projectOperations.addPluginRepositories(
                projectOperations.getFocusedModuleName(), repositories);
    }

    private void updateScaffoldBoilerPlate() {
        final String targetDirectory = projectOperations.getPathResolver()
                .getFocusedIdentifier(
                        SRC_MAIN_JAVA,
                        projectOperations
                                .getTopLevelPackage(
                                        projectOperations
                                                .getFocusedModuleName())
                                .getFullyQualifiedPackageName()
                                .replace('.', File.separatorChar));
        deleteUntouchedSetupFiles("setup/*", targetDirectory);
        deleteUntouchedSetupFiles("setup/client/*", targetDirectory + "/client");
        copyDirectoryContents();
        updateGaeHelper();
    }

    private void updateWebXml() {
        final String webXmlpath = projectOperations.getPathResolver()
                .getFocusedIdentifier(SRC_MAIN_WEBAPP, "WEB-INF/web.xml");
        final Document webXml = XmlUtils.readXml(fileManager
                .getInputStream(webXmlpath));
        final Element root = webXml.getDocumentElement();

        WebXmlUtils.addServlet(
                "requestFactory",
                projectOperations.getTopLevelPackage(projectOperations
                        .getFocusedModuleName())
                        + ".server.CustomRequestFactoryServlet", "/gwtRequest",
                null, webXml, null);
        if (projectOperations
                .isFeatureInstalledInFocusedModule(FeatureNames.GAE)) {
            WebXmlUtils
                    .addFilter(
                            "GaeAuthFilter",
                            GwtPath.SERVER_GAE.packageName(projectOperations
                                    .getTopLevelPackage(projectOperations
                                            .getFocusedModuleName()))
                                    + ".GaeAuthFilter",
                            "/gwtRequest/*",
                            webXml,
                            "This filter makes GAE authentication services visible to a RequestFactory client.");
            final String displayName = "Redirect to the login page if needed before showing any html pages";
            final WebXmlUtils.WebResourceCollection webResourceCollection = new WebXmlUtils.WebResourceCollection(
                    "Login required", null,
                    Collections.singletonList("*.html"),
                    new ArrayList<String>());
            final ArrayList<String> roleNames = new ArrayList<String>();
            roleNames.add("*");
            final String userDataConstraint = null;
            WebXmlUtils.addSecurityConstraint(displayName,
                    Collections.singletonList(webResourceCollection),
                    roleNames, userDataConstraint, webXml, null);
        }
        else {
            final Element filter = XmlUtils.findFirstElement(
                    "/web-app/filter[filter-name = 'GaeAuthFilter']", root);
            if (filter != null) {
                filter.getParentNode().removeChild(filter);
            }
            final Element filterMapping = XmlUtils.findFirstElement(
                    "/web-app/filter-mapping[filter-name = 'GaeAuthFilter']",
                    root);
            if (filterMapping != null) {
                filterMapping.getParentNode().removeChild(filterMapping);
            }
            final Element securityConstraint = XmlUtils.findFirstElement(
                    "security-constraint", root);
            if (securityConstraint != null) {
                securityConstraint.getParentNode().removeChild(
                        securityConstraint);
            }
        }

        removeIfFound("/web-app/error-page", root);

        fileManager.createOrUpdateTextFileIfRequired(webXmlpath,
                XmlUtils.nodeToString(webXml), false);
    }
}
