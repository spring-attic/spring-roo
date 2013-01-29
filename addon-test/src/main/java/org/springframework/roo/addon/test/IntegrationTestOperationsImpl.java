package org.springframework.roo.addon.test;

import static org.springframework.roo.model.RooJavaType.ROO_INTEGRATION_TEST;
import static org.springframework.roo.model.SpringJavaType.MOCK_STATIC_ENTITY_METHODS;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dod.DataOnDemandOperations;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;

/**
 * Provides convenience methods that can be used to create mock tests.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class IntegrationTestOperationsImpl implements IntegrationTestOperations {

    private static final JavaType JUNIT_4 = new JavaType(
            "org.junit.runners.JUnit4");
    private static final JavaType RUN_WITH = new JavaType(
            "org.junit.runner.RunWith");
    private static final JavaType TEST = new JavaType("org.junit.Test");

    @Reference private DataOnDemandOperations dataOnDemandOperations;
    @Reference private MemberDetailsScanner memberDetailsScanner;
    @Reference private MetadataService metadataService;
    @Reference private ProjectOperations projectOperations;
    @Reference private TypeLocationService typeLocationService;
    @Reference private TypeManagementService typeManagementService;

    /**
     * @param entity the entity to lookup required
     * @return the type details (never null; throws an exception if it cannot be
     *         obtained or parsed)
     */
    private ClassOrInterfaceTypeDetails getEntity(final JavaType entity) {
        final ClassOrInterfaceTypeDetails cid = typeLocationService
                .getTypeDetails(entity);
        Validate.notNull(cid,
                "Java source code details unavailable for type %s", cid);
        return cid;
    }

    public boolean isIntegrationTestInstallationPossible() {
        return projectOperations.isFocusedProjectAvailable()
                && projectOperations.isFeatureInstalledInFocusedModule(
                        FeatureNames.JPA, FeatureNames.MONGO);
    }

    public void newIntegrationTest(final JavaType entity) {
        newIntegrationTest(entity, true);
    }

    public void newIntegrationTest(final JavaType entity,
            final boolean transactional) {
        Validate.notNull(entity,
                "Entity to produce an integration test for is required");

        // Verify the requested entity actually exists as a class and is not
        // abstract
        final ClassOrInterfaceTypeDetails cid = getEntity(entity);
        Validate.isTrue(!Modifier.isAbstract(cid.getModifier()),
                "Type %s is abstract", entity.getFullyQualifiedTypeName());

        final LogicalPath path = PhysicalTypeIdentifier.getPath(cid
                .getDeclaredByMetadataId());
        dataOnDemandOperations.newDod(entity,
                new JavaType(entity.getFullyQualifiedTypeName()
                        + "DataOnDemand"));

        final JavaType name = new JavaType(entity + "IntegrationTest");
        final String declaredByMetadataId = PhysicalTypeIdentifier
                .createIdentifier(name,
                        Path.SRC_TEST_JAVA.getModulePathId(path.getModule()));

        if (metadataService.get(declaredByMetadataId) != null) {
            // The file already exists
            return;
        }

        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        final List<AnnotationAttributeValue<?>> config = new ArrayList<AnnotationAttributeValue<?>>();
        config.add(new ClassAttributeValue(new JavaSymbolName("entity"), entity));
        if (!transactional) {
            config.add(new BooleanAttributeValue(new JavaSymbolName(
                    "transactional"), false));
        }
        annotations.add(new AnnotationMetadataBuilder(ROO_INTEGRATION_TEST,
                config));

        final List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();
        final List<AnnotationMetadataBuilder> methodAnnotations = new ArrayList<AnnotationMetadataBuilder>();
        methodAnnotations.add(new AnnotationMetadataBuilder(TEST));
        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName(
                        "testMarkerMethod"), JavaType.VOID_PRIMITIVE,
                new InvocableMemberBodyBuilder());
        methodBuilder.setAnnotations(methodAnnotations);
        methods.add(methodBuilder);

        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                declaredByMetadataId, Modifier.PUBLIC, name,
                PhysicalTypeCategory.CLASS);
        cidBuilder.setAnnotations(annotations);
        cidBuilder.setDeclaredMethods(methods);

        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
    }

    /**
     * Creates a mock test for the entity. Silently returns if the mock test
     * file already exists.
     * 
     * @param entity to produce a mock test for (required)
     */
    public void newMockTest(final JavaType entity) {
        Validate.notNull(entity,
                "Entity to produce a mock test for is required");

        final JavaType name = new JavaType(entity + "Test");
        final String declaredByMetadataId = PhysicalTypeIdentifier
                .createIdentifier(name, Path.SRC_TEST_JAVA
                        .getModulePathId(projectOperations
                                .getFocusedModuleName()));

        if (metadataService.get(declaredByMetadataId) != null) {
            // The file already exists
            return;
        }

        // Determine if the mocking infrastructure needs installing
        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        final List<AnnotationAttributeValue<?>> config = new ArrayList<AnnotationAttributeValue<?>>();
        config.add(new ClassAttributeValue(new JavaSymbolName("value"), JUNIT_4));
        annotations.add(new AnnotationMetadataBuilder(RUN_WITH, config));
        annotations.add(new AnnotationMetadataBuilder(
                MOCK_STATIC_ENTITY_METHODS));

        final List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();
        final List<AnnotationMetadataBuilder> methodAnnotations = new ArrayList<AnnotationMetadataBuilder>();
        methodAnnotations.add(new AnnotationMetadataBuilder(TEST));

        // Get the entity so we can hopefully make a demo method that will be
        // usable
        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

        final ClassOrInterfaceTypeDetails cid = typeLocationService
                .getTypeDetails(entity);
        if (cid != null) {
            final MemberDetails memberDetails = memberDetailsScanner
                    .getMemberDetails(
                            IntegrationTestOperationsImpl.class.getName(), cid);
            final List<MethodMetadata> countMethods = memberDetails
                    .getMethodsWithTag(CustomDataKeys.COUNT_ALL_METHOD);
            if (countMethods.size() == 1) {
                final String countMethod = entity.getSimpleTypeName() + "."
                        + countMethods.get(0).getMethodName().getSymbolName()
                        + "()";
                bodyBuilder.appendFormalLine("int expectedCount = 13;");
                bodyBuilder.appendFormalLine(countMethod + ";");
                bodyBuilder
                        .appendFormalLine("org.springframework.mock.staticmock.AnnotationDrivenStaticEntityMockingControl.expectReturn(expectedCount);");
                bodyBuilder
                        .appendFormalLine("org.springframework.mock.staticmock.AnnotationDrivenStaticEntityMockingControl.playback();");
                bodyBuilder
                        .appendFormalLine("org.junit.Assert.assertEquals(expectedCount, "
                                + countMethod + ");");
            }
        }

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName(
                        "testMethod"), JavaType.VOID_PRIMITIVE, bodyBuilder);
        methodBuilder.setAnnotations(methodAnnotations);
        methods.add(methodBuilder);

        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                declaredByMetadataId, Modifier.PUBLIC, name,
                PhysicalTypeCategory.CLASS);
        cidBuilder.setAnnotations(annotations);
        cidBuilder.setDeclaredMethods(methods);

        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
    }

    public void newTestStub(final JavaType javaType) {
        Validate.notNull(javaType,
                "Class to produce a test stub for is required");

        final JavaType name = new JavaType(javaType + "Test");
        final String declaredByMetadataId = PhysicalTypeIdentifier
                .createIdentifier(name, Path.SRC_TEST_JAVA
                        .getModulePathId(projectOperations
                                .getFocusedModuleName()));

        if (metadataService.get(declaredByMetadataId) != null) {
            // The file already exists
            return;
        }

        // Determine if the test infrastructure needs installing
        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        final List<AnnotationAttributeValue<?>> config = new ArrayList<AnnotationAttributeValue<?>>();
        config.add(new ClassAttributeValue(new JavaSymbolName("value"), JUNIT_4));
        annotations.add(new AnnotationMetadataBuilder(RUN_WITH, config));

        final List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();
        final List<AnnotationMetadataBuilder> methodAnnotations = new ArrayList<AnnotationMetadataBuilder>();
        methodAnnotations.add(new AnnotationMetadataBuilder(TEST));

        // Get the class so we can hopefully make a demo method that will be
        // usable
        final ClassOrInterfaceTypeDetails governorTypeDetails = typeLocationService
                .getTypeDetails(javaType);
        final MemberDetails memberDetails = memberDetailsScanner
                .getMemberDetails(this.getClass().getName(),
                        governorTypeDetails);
        for (final MemberHoldingTypeDetails typeDetails : memberDetails
                .getDetails()) {
            if (!(typeDetails.getCustomData().keySet()
                    .contains(CustomDataKeys.PERSISTENT_TYPE) || typeDetails
                    .getDeclaredByMetadataId()
                    .startsWith(
                            "MID:org.springframework.roo.addon.tostring.ToStringMetadata"))) {
                for (final MethodMetadata method : typeDetails
                        .getDeclaredMethods()) {
                    // Check if public, non-abstract method
                    if (Modifier.isPublic(method.getModifier())
                            && !Modifier.isAbstract(method.getModifier())) {
                        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
                        bodyBuilder
                                .appendFormalLine("org.junit.Assert.assertTrue(true);");

                        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                                declaredByMetadataId, Modifier.PUBLIC,
                                method.getMethodName(),
                                JavaType.VOID_PRIMITIVE, bodyBuilder);
                        methodBuilder.setAnnotations(methodAnnotations);
                        methods.add(methodBuilder);
                    }
                }
            }
        }

        // Only create test class if there are test methods present
        if (!methods.isEmpty()) {
            final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                    declaredByMetadataId, Modifier.PUBLIC, name,
                    PhysicalTypeCategory.CLASS);

            // Create instance of entity to test
            final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(
                    declaredByMetadataId);
            fieldBuilder.setModifier(Modifier.PRIVATE);
            fieldBuilder.setFieldName(new JavaSymbolName(StringUtils
                    .uncapitalize(javaType.getSimpleTypeName())));
            fieldBuilder.setFieldType(javaType);
            fieldBuilder.setFieldInitializer("new "
                    + javaType.getFullyQualifiedTypeName() + "()");
            final List<FieldMetadataBuilder> fields = new ArrayList<FieldMetadataBuilder>();
            fields.add(fieldBuilder);
            cidBuilder.setDeclaredFields(fields);

            cidBuilder.setDeclaredMethods(methods);

            typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
        }
    }
}