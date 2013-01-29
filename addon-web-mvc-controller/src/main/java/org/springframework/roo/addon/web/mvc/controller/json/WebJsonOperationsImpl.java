package org.springframework.roo.addon.web.mvc.controller.json;

import static org.springframework.roo.model.SpringJavaType.CHARACTER_ENCODING_FILTER;
import static org.springframework.roo.model.SpringJavaType.CONTEXT_LOADER_LISTENER;
import static org.springframework.roo.model.SpringJavaType.DISPATCHER_SERVLET;
import static org.springframework.roo.model.SpringJavaType.HIDDEN_HTTP_METHOD_FILTER;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.ProjectType;
import org.springframework.roo.support.util.WebXmlUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link WebJsonOperations}.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component
@Service
public class WebJsonOperationsImpl implements WebJsonOperations {

    @Reference private FileManager fileManager;
    @Reference private MetadataService metadataService;
    @Reference private WebMvcOperations mvcOperations;
    @Reference private PathResolver pathResolver;
    @Reference private ProjectOperations projectOperations;
    @Reference private TypeLocationService typeLocationService;
    @Reference private TypeManagementService typeManagementService;

    public void annotateAll(JavaPackage javaPackage) {
        if (javaPackage == null) {
            javaPackage = projectOperations
                    .getTopLevelPackage(projectOperations
                            .getFocusedModuleName());
        }
        for (final ClassOrInterfaceTypeDetails cod : typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JSON)) {
            if (Modifier.isAbstract(cod.getModifier())) {
                continue;
            }
            final JavaType jsonType = cod.getName();
            JavaType mvcType = null;
            for (final ClassOrInterfaceTypeDetails mvcCod : typeLocationService
                    .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_WEB_SCAFFOLD)) {
                // We know this physical type exists given type location service
                // just found it.
                final PhysicalTypeMetadata mvcMd = (PhysicalTypeMetadata) metadataService
                        .get(mvcCod.getDeclaredByMetadataId());
                final WebScaffoldAnnotationValues webScaffoldAnnotationValues = new WebScaffoldAnnotationValues(
                        mvcMd);
                if (webScaffoldAnnotationValues.isAnnotationFound()
                        && webScaffoldAnnotationValues.getFormBackingObject()
                                .equals(jsonType)) {
                    mvcType = mvcCod.getName();
                    break;
                }
            }
            if (mvcType == null) {
                createNewType(
                        new JavaType(javaPackage.getFullyQualifiedPackageName()
                                + "." + jsonType.getSimpleTypeName()
                                + "Controller"), jsonType);
            }
            else {
                appendToExistingType(mvcType, jsonType);
            }
        }
    }

    public void annotateType(final JavaType type, final JavaType jsonEntity) {
        Validate.notNull(type, "Target type required");
        Validate.notNull(jsonEntity, "Json entity required");
        final String id = typeLocationService.getPhysicalTypeIdentifier(type);
        if (id == null) {
            createNewType(type, jsonEntity);
        }
        else {
            appendToExistingType(type, jsonEntity);
        }
    }

    private void appendToExistingType(final JavaType type,
            final JavaType jsonEntity) {
        final ClassOrInterfaceTypeDetails cid = typeLocationService
                .getTypeDetails(type);
        if (cid == null) {
            throw new IllegalArgumentException("Cannot locate source for '"
                    + type.getFullyQualifiedTypeName() + "'");
        }

        if (MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(),
                RooJavaType.ROO_WEB_JSON) != null) {
            return;
        }

        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                cid);
        cidBuilder.addAnnotation(getAnnotation(jsonEntity));
        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
    }

    private void createNewType(final JavaType type, final JavaType jsonEntity) {
        final PluralMetadata pluralMetadata = (PluralMetadata) metadataService
                .get(PluralMetadata.createIdentifier(jsonEntity,
                        typeLocationService.getTypePath(jsonEntity)));
        if (pluralMetadata == null) {
            return;
        }

        final String declaredByMetadataId = PhysicalTypeIdentifier
                .createIdentifier(type,
                        pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA));
        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                declaredByMetadataId, Modifier.PUBLIC, type,
                PhysicalTypeCategory.CLASS);
        cidBuilder.addAnnotation(getAnnotation(jsonEntity));
        cidBuilder.addAnnotation(new AnnotationMetadataBuilder(
                SpringJavaType.CONTROLLER));
        final AnnotationMetadataBuilder requestMapping = new AnnotationMetadataBuilder(
                SpringJavaType.REQUEST_MAPPING);
        requestMapping.addAttribute(new StringAttributeValue(
                new JavaSymbolName("value"), "/"
                        + pluralMetadata.getPlural().toLowerCase()));
        cidBuilder.addAnnotation(requestMapping);
        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
    }

    private AnnotationMetadataBuilder getAnnotation(final JavaType type) {
        // Create annotation @RooWebJson(jsonObject = MyObject.class)
        final List<AnnotationAttributeValue<?>> rooJsonAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        rooJsonAttributes.add(new ClassAttributeValue(new JavaSymbolName(
                "jsonObject"), type));
        return new AnnotationMetadataBuilder(RooJavaType.ROO_WEB_JSON,
                rooJsonAttributes);
    }

    public boolean isWebJsonCommandAvailable() {
        return projectOperations
                .isFeatureInstalledInFocusedModule(FeatureNames.MVC)
                && !projectOperations
                        .isFeatureInstalledInFocusedModule(FeatureNames.JSF);
    }

    public boolean isWebJsonInstallationPossible() {
        return !projectOperations
                .isFeatureInstalledInFocusedModule(FeatureNames.MVC)
                && !projectOperations
                        .isFeatureInstalledInFocusedModule(FeatureNames.JSF);
    }

    public void setup() {
        mvcOperations.installMinimalWebArtifacts();

        // Verify that the web.xml already exists
        final String webXmlPath = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml");
        Validate.isTrue(fileManager.exists(webXmlPath), "'%s' does not exist",
                webXmlPath);

        final Document document = XmlUtils.readXml(fileManager
                .getInputStream(webXmlPath));

        WebXmlUtils.addContextParam(new WebXmlUtils.WebXmlParam(
                "contextConfigLocation",
                "classpath*:META-INF/spring/applicationContext*.xml"),
                document, null);
        WebXmlUtils.addFilter(WebMvcOperations.CHARACTER_ENCODING_FILTER_NAME,
                CHARACTER_ENCODING_FILTER.getFullyQualifiedTypeName(), "/*",
                document, null,
                new WebXmlUtils.WebXmlParam("encoding", "UTF-8"),
                new WebXmlUtils.WebXmlParam("forceEncoding", "true"));
        WebXmlUtils.addFilter(WebMvcOperations.HTTP_METHOD_FILTER_NAME,
                HIDDEN_HTTP_METHOD_FILTER.getFullyQualifiedTypeName(), "/*",
                document, null);
        WebXmlUtils
                .addListener(
                        CONTEXT_LOADER_LISTENER.getFullyQualifiedTypeName(),
                        document,
                        "Creates the Spring Container shared by all Servlets and Filters");
        WebXmlUtils.addServlet(projectOperations.getFocusedProjectName(),
                DISPATCHER_SERVLET.getFullyQualifiedTypeName(), "/", 1,
                document, "Handles Spring requests",
                new WebXmlUtils.WebXmlParam("contextConfigLocation",
                        "WEB-INF/spring/webmvc-config.xml"));

        fileManager.createOrUpdateTextFileIfRequired(webXmlPath,
                XmlUtils.nodeToString(document), false);

        updateConfiguration();
    }

    private void updateConfiguration() {
        final Element configuration = XmlUtils.getConfiguration(getClass());

        final List<Dependency> dependencies = new ArrayList<Dependency>();
        final List<Element> springDependencies = XmlUtils.findElements(
                "/configuration/springWebJson/dependencies/dependency",
                configuration);
        for (final Element dependencyElement : springDependencies) {
            dependencies.add(new Dependency(dependencyElement));
        }
        projectOperations.addDependencies(
                projectOperations.getFocusedModuleName(), dependencies);

        projectOperations.updateProjectType(
                projectOperations.getFocusedModuleName(), ProjectType.WAR);
    }
}
