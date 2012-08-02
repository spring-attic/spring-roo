package org.springframework.roo.addon.layers.service;

import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.classpath.PhysicalTypeCategory.CLASS;
import static org.springframework.roo.classpath.PhysicalTypeCategory.INTERFACE;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ACTIVE_RECORD;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ENTITY;
import static org.springframework.roo.model.RooJavaType.ROO_PERMISSION_EVALUATOR;
import static org.springframework.roo.model.RooJavaType.ROO_SERVICE;
import static org.springframework.roo.model.SpringJavaType.PERMISSION_EVALUATOR;
import static org.springframework.roo.project.Path.SRC_MAIN_JAVA;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.FileUtils;

/**
 * The {@link ServiceOperations} implementation.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component
@Service
public class ServiceOperationsImpl implements ServiceOperations {
    @Reference private FileManager fileManager;
    @Reference private PathResolver pathResolver;
    @Reference private ProjectOperations projectOperations;
    @Reference private TypeManagementService typeManagementService;
    @Reference private TypeLocationService typeLocationService;
    @Reference private MetadataService metadataService;

    private void createServiceClass(final JavaType interfaceType,
            final JavaType classType) {
        Validate.notNull(classType, "Class type required");
        final String classIdentifier = pathResolver.getFocusedCanonicalPath(
                Path.SRC_MAIN_JAVA, classType);
        if (fileManager.exists(classIdentifier)) {
            return; // Type already exists - nothing to do
        }
        final String classMid = PhysicalTypeIdentifier.createIdentifier(
                classType, pathResolver.getPath(classIdentifier));
        final ClassOrInterfaceTypeDetailsBuilder classTypeBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                classMid, PUBLIC, classType, CLASS);
        classTypeBuilder.addImplementsType(interfaceType);

        typeManagementService
                .createOrUpdateTypeOnDisk(classTypeBuilder.build());
    }

    private void createServiceInterface(final JavaType interfaceType,
            final JavaType domainType, boolean requireAuthentication,
            String role, boolean usePermissionEvaluator) {
        final String interfaceIdentifier = pathResolver
                .getFocusedCanonicalPath(Path.SRC_MAIN_JAVA, interfaceType);
        if (fileManager.exists(interfaceIdentifier)) {
            return; // Type already exists - nothing to do
        }
        Validate.notNull(domainType, "Domain type required");
        final AnnotationMetadataBuilder interfaceAnnotationMetadata = new AnnotationMetadataBuilder(
                ROO_SERVICE);
        interfaceAnnotationMetadata
                .addAttribute(new ArrayAttributeValue<ClassAttributeValue>(
                        new JavaSymbolName("domainTypes"), Arrays
                                .asList(new ClassAttributeValue(
                                        new JavaSymbolName("foo"), domainType))));
        interfaceAnnotationMetadata
                .addAttribute(new ArrayAttributeValue<StringAttributeValue>(
                        new JavaSymbolName("authorizedRole"), Arrays
                                .asList(new StringAttributeValue(
                                        new JavaSymbolName("bar"), role))));
        interfaceAnnotationMetadata.addBooleanAttribute(
                "requireAuthentication", requireAuthentication);
        interfaceAnnotationMetadata.addBooleanAttribute(
                "usePermissionEvaluator", usePermissionEvaluator);
        final String interfaceMid = PhysicalTypeIdentifier.createIdentifier(
                interfaceType, pathResolver.getPath(interfaceIdentifier));
        final ClassOrInterfaceTypeDetailsBuilder interfaceTypeBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                interfaceMid, PUBLIC, interfaceType, INTERFACE);
        interfaceTypeBuilder.addAnnotation(interfaceAnnotationMetadata.build());
        typeManagementService.createOrUpdateTypeOnDisk(interfaceTypeBuilder
                .build());
    }

    private void createPermissionEvaluator(
            final JavaPackage permissionEvaluatorPackage) {
        installPermissionEvaluatorTemplate(permissionEvaluatorPackage);
        final LogicalPath focusedSrcMainJava = LogicalPath.getInstance(
                SRC_MAIN_JAVA, projectOperations.getFocusedModuleName());
        JavaType permissionEvaluatorClass = new JavaType(
                permissionEvaluatorPackage.getFullyQualifiedPackageName()
                        + ".ServicePermissionEvaluator");
        final String identifier = pathResolver.getFocusedCanonicalPath(
                Path.SRC_MAIN_JAVA, permissionEvaluatorClass);
        if (fileManager.exists(identifier)) {
            return; // Type already exists - nothing to do
        }

        final AnnotationMetadataBuilder classAnnotationMetadata = new AnnotationMetadataBuilder(
                ROO_PERMISSION_EVALUATOR);
        final String classMid = PhysicalTypeIdentifier.createIdentifier(
                permissionEvaluatorClass, pathResolver.getPath(identifier));
        final ClassOrInterfaceTypeDetailsBuilder classBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                classMid, PUBLIC, permissionEvaluatorClass, CLASS);
        classBuilder.addAnnotation(classAnnotationMetadata.build());
        classBuilder.addImplementsType(PERMISSION_EVALUATOR);
        typeManagementService.createOrUpdateTypeOnDisk(classBuilder.build());

        metadataService
                .get(ServicePermissionEvaluatorMetadata.createIdentifier(
                        permissionEvaluatorClass, focusedSrcMainJava));
    }

    private void installPermissionEvaluatorTemplate(
            JavaPackage permissionEvaluatorPackage) {
        // Copy the template across
        final String destination = pathResolver.getFocusedIdentifier(
                Path.SPRING_CONFIG_ROOT,
                "applicationContext-security-permissionEvaluator.xml");
        if (!fileManager.exists(destination)) {
            try {
                InputStream inputStream = FileUtils
                        .getInputStream(getClass(),
                                "applicationContext-security-permissionEvaluator-template.xml");
                String content = IOUtils.toString(inputStream);
                content = content.replace("__PERMISSION_EVALUATOR_PACKAGE__",
                        permissionEvaluatorPackage
                                .getFullyQualifiedPackageName());

                fileManager.createOrUpdateTextFileIfRequired(destination,
                        content, true);
            }
            catch (final IOException ioe) {
                throw new IllegalStateException(ioe);
            }
        }
    }

    private boolean isPermissionEvaluatorInstalled() {
        Set<ClassOrInterfaceTypeDetails> types = typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(ROO_PERMISSION_EVALUATOR);
        return types.size() > 0;
    }

    @Override
    public boolean isServiceInstallationPossible() {
        return projectOperations.isFocusedProjectAvailable();
    }

    @Override
    public boolean isServicePermissionEvaluatorInstallationPossible() {
        return projectOperations.isFocusedProjectAvailable()
                && projectOperations.isFeatureInstalled(FeatureNames.SECURITY);
    }

    @Override
    public boolean isSecureServiceInstallationPossible() {
        return projectOperations.isFocusedProjectAvailable()
                && projectOperations.isFeatureInstalled(FeatureNames.SECURITY);
    }

    @Override
    public void setupService(final JavaType interfaceType,
            final JavaType classType, final JavaType domainType,
            boolean requireAuthentication, String role,
            boolean usePermissionEvaluator) {
        if (requireAuthentication || role.equals("") || usePermissionEvaluator) {
            Validate.isTrue(
                    projectOperations.isFeatureInstalled(FeatureNames.SECURITY),
                    "Security must first be setup before securing a method");
        }

        if (usePermissionEvaluator) {
            Validate.isTrue(isPermissionEvaluatorInstalled(),
                    "Permission evaluator must be installed (use permissionEvaluator command)");
        }

        Validate.notNull(interfaceType, "Interface type required");
        createServiceInterface(interfaceType, domainType,
                requireAuthentication, role, usePermissionEvaluator);
        createServiceClass(interfaceType, classType);
    }

    @Override
    public void setupAllServices(JavaPackage interfacePackage,
            JavaPackage classPackage, boolean requireAuthentication,
            String role, boolean usePermissionEvaluator) {
        for (final ClassOrInterfaceTypeDetails domainType : typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(ROO_JPA_ENTITY,
                        ROO_JPA_ACTIVE_RECORD)) {
            JavaType interfaceType = new JavaType(
                    interfacePackage.getFullyQualifiedPackageName() + "."
                            + domainType.getName().getSimpleTypeName()
                            + "Service");
            JavaType classType = new JavaType(
                    classPackage.getFullyQualifiedPackageName() + "."
                            + domainType.getName().getSimpleTypeName()
                            + "ServiceImpl");
            setupService(interfaceType, classType, domainType.getName(),
                    requireAuthentication, role, usePermissionEvaluator);
        }
    }

    @Override
    public void setupPermissionEvaluator(
            final JavaPackage permissionEvaluatorPackage) {
        Validate.isTrue(
                projectOperations.isFeatureInstalled(FeatureNames.SECURITY),
                "Security must first be setup before securing a method");
        Validate.notNull(permissionEvaluatorPackage, "Package required");
        createPermissionEvaluator(permissionEvaluatorPackage);
    }

}
