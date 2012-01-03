package org.springframework.roo.addon.web.mvc.controller;

import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSISTENT_TYPE;
import static org.springframework.roo.model.RooJavaType.ROO_WEB_SCAFFOLD;
import static org.springframework.roo.model.SpringJavaType.CONTROLLER;
import static org.springframework.roo.model.SpringJavaType.REQUEST_MAPPING;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Implementation of {@link ControllerOperations}.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component
@Service
public class ControllerOperationsImpl implements ControllerOperations {

    // Constants
    private static final JavaSymbolName PATH = new JavaSymbolName("path");
    private static final JavaSymbolName VALUE = new JavaSymbolName("value");
    private static final Logger LOG = HandlerUtils
            .getLogger(ControllerOperationsImpl.class);

    // Fields
    @Reference private MetadataDependencyRegistry dependencyRegistry;
    @Reference private MetadataService metadataService;
    @Reference private PathResolver pathResolver;
    @Reference private ProjectOperations projectOperations;
    @Reference private TypeLocationService typeLocationService;
    @Reference private TypeManagementService typeManagementService;
    @Reference private WebMvcOperations webMvcOperations;

    public boolean isControllerInstallationPossible() {
        return projectOperations.isFocusedProjectAvailable()
                && projectOperations
                        .isFeatureInstalledInFocusedModule(FeatureNames.MVC)
                && !projectOperations
                        .isFeatureInstalledInFocusedModule(FeatureNames.JSF);
    }

    public boolean isNewControllerAvailable() {
        return projectOperations.isFocusedProjectAvailable();
    }

    public void setup() {
        webMvcOperations.installAllWebMvcArtifacts();
    }

    public void generateAll(final JavaPackage javaPackage) {
        for (ClassOrInterfaceTypeDetails entityDetails : typeLocationService
                .findClassesOrInterfaceDetailsWithTag(PERSISTENT_TYPE)) {
            if (Modifier.isAbstract(entityDetails.getModifier())) {
                continue;
            }

            final JavaType entityType = entityDetails.getType();
            final LogicalPath entityPath = PhysicalTypeIdentifier
                    .getPath(entityDetails.getDeclaredByMetadataId());

            // Check to see if this persistent type has a web scaffold metadata
            // listening to it
            String downstreamWebScaffoldMetadataId = WebScaffoldMetadata
                    .createIdentifier(entityType, entityPath);
            if (dependencyRegistry.getDownstream(
                    entityDetails.getDeclaredByMetadataId()).contains(
                    downstreamWebScaffoldMetadataId)) {
                // There is already a controller for this entity
                continue;
            }

            // To get here, there is no listening controller, so add one
            final PluralMetadata pluralMetadata = (PluralMetadata) metadataService
                    .get(PluralMetadata
                            .createIdentifier(entityType, entityPath));
            if (pluralMetadata != null) {
                final JavaType controller = new JavaType(
                        javaPackage.getFullyQualifiedPackageName() + "."
                                + entityType.getSimpleTypeName() + "Controller");
                createAutomaticController(controller, entityType,
                        new HashSet<String>(), pluralMetadata.getPlural()
                                .toLowerCase());
            }
        }
    }

    public void createAutomaticController(final JavaType controller,
            final JavaType entity, final Set<String> disallowedOperations,
            final String path) {
        Assert.notNull(controller, "Controller Java Type required");
        Assert.notNull(entity, "Entity Java Type required");
        Assert.notNull(disallowedOperations,
                "Set of disallowed operations required");
        Assert.hasText(path, "Controller base path required");

        // Look for an existing controller mapped to this path
        final ClassOrInterfaceTypeDetails existingController = getExistingController(path);

        webMvcOperations.installConversionService(controller.getPackage());

        List<AnnotationMetadataBuilder> annotations = null;

        ClassOrInterfaceTypeDetailsBuilder cidBuilder = null;
        if (existingController == null) {
            LogicalPath controllerPath = pathResolver
                    .getFocusedPath(Path.SRC_MAIN_JAVA);
            String resourceIdentifier = typeLocationService
                    .getPhysicalTypeCanonicalPath(controller, controllerPath);
            String declaredByMetadataId = PhysicalTypeIdentifier
                    .createIdentifier(controller,
                            pathResolver.getPath(resourceIdentifier));

            // Create annotation @RequestMapping("/myobject/**")
            List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
            requestMappingAttributes.add(new StringAttributeValue(VALUE, "/"
                    + path));
            annotations = new ArrayList<AnnotationMetadataBuilder>();
            annotations.add(new AnnotationMetadataBuilder(REQUEST_MAPPING,
                    requestMappingAttributes));

            // Create annotation @Controller
            List<AnnotationAttributeValue<?>> controllerAttributes = new ArrayList<AnnotationAttributeValue<?>>();
            annotations.add(new AnnotationMetadataBuilder(CONTROLLER,
                    controllerAttributes));

            // Create annotation @RooWebScaffold(path = "/test",
            // formBackingObject = MyObject.class)
            annotations.add(getRooWebScaffoldAnnotation(entity,
                    disallowedOperations, path, PATH));
            cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                    declaredByMetadataId, Modifier.PUBLIC, controller,
                    PhysicalTypeCategory.CLASS);
        }
        else {
            cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                    existingController);
            annotations = cidBuilder.getAnnotations();
            if (MemberFindingUtils.getAnnotationOfType(
                    existingController.getAnnotations(), ROO_WEB_SCAFFOLD) == null) {
                annotations.add(getRooWebScaffoldAnnotation(entity,
                        disallowedOperations, path, PATH));
            }
        }
        cidBuilder.setAnnotations(annotations);
        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
    }

    /**
     * Looks for an existing controller mapped to the given path
     * 
     * @param path (required)
     * @return <code>null</code> if there is no such controller
     */
    private ClassOrInterfaceTypeDetails getExistingController(final String path) {
        for (final ClassOrInterfaceTypeDetails cid : typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(REQUEST_MAPPING)) {
            final StringAttributeValue mappingAttribute = (StringAttributeValue) MemberFindingUtils
                    .getAnnotationOfType(cid.getAnnotations(), REQUEST_MAPPING)
                    .getAttribute(VALUE);
            if (mappingAttribute != null) {
                final String mapping = mappingAttribute.getValue();
                if (StringUtils.hasText(mapping)
                        && mapping.equalsIgnoreCase("/" + path)) {
                    LOG.info("Introducing into existing controller '"
                            + cid.getName().getFullyQualifiedTypeName()
                            + "' mapped to '/" + path);
                    return cid;
                }
            }
        }
        return null;
    }

    private AnnotationMetadataBuilder getRooWebScaffoldAnnotation(
            final JavaType entity, final Set<String> disallowedOperations,
            final String path, final JavaSymbolName pathName) {
        List<AnnotationAttributeValue<?>> rooWebScaffoldAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        rooWebScaffoldAttributes.add(new StringAttributeValue(pathName, path));
        rooWebScaffoldAttributes.add(new ClassAttributeValue(
                new JavaSymbolName("formBackingObject"), entity));
        for (String operation : disallowedOperations) {
            rooWebScaffoldAttributes.add(new BooleanAttributeValue(
                    new JavaSymbolName(operation), false));
        }
        return new AnnotationMetadataBuilder(ROO_WEB_SCAFFOLD,
                rooWebScaffoldAttributes);
    }
}
