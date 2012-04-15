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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
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
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
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

/**
 * Implementation of {@link ControllerOperations}.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component
@Service
public class ControllerOperationsImpl implements ControllerOperations {

    private static final Logger LOGGER = HandlerUtils
            .getLogger(ControllerOperationsImpl.class);
    private static final JavaSymbolName PATH = new JavaSymbolName("path");
    private static final JavaSymbolName VALUE = new JavaSymbolName("value");

    @Reference private MetadataDependencyRegistry dependencyRegistry;
    @Reference private MetadataService metadataService;
    @Reference private PathResolver pathResolver;
    @Reference private ProjectOperations projectOperations;
    @Reference private TypeLocationService typeLocationService;
    @Reference private TypeManagementService typeManagementService;
    @Reference private WebMvcOperations webMvcOperations;

    public void createAutomaticController(final JavaType controller,
            final JavaType entity, final Set<String> disallowedOperations,
            final String path) {
        Validate.notNull(controller, "Controller Java Type required");
        Validate.notNull(entity, "Entity Java Type required");
        Validate.notNull(disallowedOperations,
                "Set of disallowed operations required");
        Validate.notBlank(path, "Controller base path required");

        // Look for an existing controller mapped to this path
        final ClassOrInterfaceTypeDetails existingController = getExistingController(path);

        webMvcOperations.installConversionService(controller.getPackage());

        List<AnnotationMetadataBuilder> annotations = null;

        ClassOrInterfaceTypeDetailsBuilder cidBuilder = null;
        if (existingController == null) {
            final LogicalPath controllerPath = pathResolver
                    .getFocusedPath(Path.SRC_MAIN_JAVA);
            final String resourceIdentifier = typeLocationService
                    .getPhysicalTypeCanonicalPath(controller, controllerPath);
            final String declaredByMetadataId = PhysicalTypeIdentifier
                    .createIdentifier(controller,
                            pathResolver.getPath(resourceIdentifier));

            // Create annotation @RequestMapping("/myobject/**")
            final List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
            requestMappingAttributes.add(new StringAttributeValue(VALUE, "/"
                    + path));
            annotations = new ArrayList<AnnotationMetadataBuilder>();
            annotations.add(new AnnotationMetadataBuilder(REQUEST_MAPPING,
                    requestMappingAttributes));

            // Create annotation @Controller
            final List<AnnotationAttributeValue<?>> controllerAttributes = new ArrayList<AnnotationAttributeValue<?>>();
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

    public void generateAll(final JavaPackage javaPackage) {
        for (final ClassOrInterfaceTypeDetails entityDetails : typeLocationService
                .findClassesOrInterfaceDetailsWithTag(PERSISTENT_TYPE)) {
            if (Modifier.isAbstract(entityDetails.getModifier())) {
                continue;
            }

            final JavaType entityType = entityDetails.getType();
            final LogicalPath entityPath = PhysicalTypeIdentifier
                    .getPath(entityDetails.getDeclaredByMetadataId());

            // Check to see if this persistent type has a web scaffold metadata
            // listening to it
            final String downstreamWebScaffoldMetadataId = WebScaffoldMetadata
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

    /**
     * Looks for an existing controller mapped to the given path
     * 
     * @param path (required)
     * @return <code>null</code> if there is no such controller
     */
    private ClassOrInterfaceTypeDetails getExistingController(final String path) {
        for (final ClassOrInterfaceTypeDetails cid : typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(REQUEST_MAPPING)) {
            final AnnotationAttributeValue<?> attribute = MemberFindingUtils
                    .getAnnotationOfType(cid.getAnnotations(), REQUEST_MAPPING)
                    .getAttribute(VALUE);
            if (attribute instanceof ArrayAttributeValue) {
                final ArrayAttributeValue<?> mappingAttribute = (ArrayAttributeValue<?>) attribute;
                if (mappingAttribute.getValue().size() > 1) {
                    LOGGER.warning("Skipping controller '"
                            + cid.getName().getFullyQualifiedTypeName()
                            + "' as it contains more than one path");
                    continue;
                }
                else if (mappingAttribute.getValue().size() == 1) {
                    final StringAttributeValue attr = (StringAttributeValue) mappingAttribute
                            .getValue().get(0);
                    final String mapping = attr.getValue();
                    if (StringUtils.isNotBlank(mapping)
                            && mapping.equalsIgnoreCase("/" + path)) {
                        return cid;
                    }
                }
            }
            else if (attribute instanceof StringAttributeValue) {
                final StringAttributeValue mappingAttribute = (StringAttributeValue) attribute;
                if (mappingAttribute != null) {
                    final String mapping = mappingAttribute.getValue();
                    if (StringUtils.isNotBlank(mapping)
                            && mapping.equalsIgnoreCase("/" + path)) {
                        return cid;
                    }
                }
            }
        }
        return null;
    }

    private AnnotationMetadataBuilder getRooWebScaffoldAnnotation(
            final JavaType entity, final Set<String> disallowedOperations,
            final String path, final JavaSymbolName pathName) {
        final List<AnnotationAttributeValue<?>> rooWebScaffoldAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        rooWebScaffoldAttributes.add(new StringAttributeValue(pathName, path));
        rooWebScaffoldAttributes.add(new ClassAttributeValue(
                new JavaSymbolName("formBackingObject"), entity));
        for (final String operation : disallowedOperations) {
            rooWebScaffoldAttributes.add(new BooleanAttributeValue(
                    new JavaSymbolName(operation), false));
        }
        return new AnnotationMetadataBuilder(ROO_WEB_SCAFFOLD,
                rooWebScaffoldAttributes);
    }
}
