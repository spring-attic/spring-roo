package org.springframework.roo.addon.test;

import static org.springframework.roo.model.GoogleJavaType.GAE_LOCAL_SERVICE_TEST_HELPER;
import static org.springframework.roo.model.JdkJavaType.ITERATOR;
import static org.springframework.roo.model.JdkJavaType.LIST;
import static org.springframework.roo.model.Jsr303JavaType.CONSTRAINT_VIOLATION;
import static org.springframework.roo.model.Jsr303JavaType.CONSTRAINT_VIOLATION_EXCEPTION;
import static org.springframework.roo.model.SpringJavaType.AUTOWIRED;
import static org.springframework.roo.model.SpringJavaType.CONTEXT_CONFIGURATION;
import static org.springframework.roo.model.SpringJavaType.PROPAGATION;
import static org.springframework.roo.model.SpringJavaType.TRANSACTIONAL;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.dod.DataOnDemandMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooIntegrationTest}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class IntegrationTestMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {

    private static final JavaType AFTER_CLASS = new JavaType(
            "org.junit.AfterClass");
    private static final JavaType ASSERT = new JavaType("org.junit.Assert");

    private static final JavaType BEFORE_CLASS = new JavaType(
            "org.junit.BeforeClass");
    private static final String PROVIDES_TYPE_STRING = IntegrationTestMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);
    private static final JavaType RUN_WITH = new JavaType(
            "org.junit.runner.RunWith");
    private static final JavaType[] SETUP_PARAMETERS = {};
    private static final JavaType[] TEARDOWN_PARAMETERS = {};
    private static final JavaType TEST = new JavaType("org.junit.Test");

    public static String createIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return PhysicalTypeIdentifierNamingUtils.createIdentifier(
                PROVIDES_TYPE_STRING, javaType, path);
    }

    public static JavaType getJavaType(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getJavaType(
                PROVIDES_TYPE_STRING, metadataIdentificationString);
    }

    public static String getMetadataIdentiferType() {
        return PROVIDES_TYPE;
    }

    public static LogicalPath getPath(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }

    public static boolean isValid(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }

    private IntegrationTestAnnotationValues annotationValues;
    private DataOnDemandMetadata dataOnDemandMetadata;
    private boolean entityHasSuperclass;
    private boolean hasEmbeddedIdentifier;
    private boolean isGaeSupported = false;
    private String transactionManager;

    public IntegrationTestMetadata(final String identifier,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final IntegrationTestAnnotationValues annotationValues,
            final DataOnDemandMetadata dataOnDemandMetadata,
            final MethodMetadata identifierAccessorMethod,
            final MethodMetadata versionAccessorMethod,
            final MemberTypeAdditions countMethod,
            final MemberTypeAdditions findMethod,
            final MemberTypeAdditions findAllMethod,
            final MemberTypeAdditions findEntriesMethod,
            final MemberTypeAdditions flushMethod,
            final MemberTypeAdditions mergeMethod,
            final MemberTypeAdditions persistMethod,
            final MemberTypeAdditions removeMethod,
            final String transactionManager,
            final boolean hasEmbeddedIdentifier,
            final boolean entityHasSuperclass, final boolean isGaeEnabled) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.isTrue(
                isValid(identifier),
                "Metadata identification string '%s' does not appear to be a valid",
                identifier);
        Validate.notNull(annotationValues, "Annotation values required");
        Validate.notNull(dataOnDemandMetadata,
                "Data on demand metadata required");

        if (!isValid()) {
            return;
        }

        this.annotationValues = annotationValues;
        this.dataOnDemandMetadata = dataOnDemandMetadata;
        this.transactionManager = transactionManager;
        this.hasEmbeddedIdentifier = hasEmbeddedIdentifier;
        this.entityHasSuperclass = entityHasSuperclass;

        addRequiredIntegrationTestClassIntroductions(DataOnDemandMetadata
                .getJavaType(dataOnDemandMetadata.getId()));

        // Add GAE LocalServiceTestHelper instance and @BeforeClass/@AfterClass
        // methods if GAE is enabled
        if (isGaeEnabled) {
            isGaeSupported = true;
            addOptionalIntegrationTestClassIntroductions();
        }

        builder.addMethod(getCountMethodTest(countMethod));
        builder.addMethod(getFindMethodTest(findMethod,
                identifierAccessorMethod));
        builder.addMethod(getFindAllMethodTest(findAllMethod, countMethod));
        builder.addMethod(getFindEntriesMethodTest(countMethod,
                findEntriesMethod));
        if (flushMethod != null) {
            builder.addMethod(getFlushMethodTest(versionAccessorMethod,
                    identifierAccessorMethod, flushMethod, findMethod));
        }
        builder.addMethod(getMergeMethodTest(mergeMethod, findMethod,
                flushMethod, versionAccessorMethod, identifierAccessorMethod));
        builder.addMethod(getPersistMethodTest(persistMethod, flushMethod,
                identifierAccessorMethod));
        builder.addMethod(getRemoveMethodTest(removeMethod, findMethod,
                flushMethod, identifierAccessorMethod));

        itdTypeDetails = builder.build();
    }

    private void addOptionalIntegrationTestClassIntroductions() {
        // Add the GAE test helper field if the user did not define it on the
        // governor directly
        final JavaType helperType = GAE_LOCAL_SERVICE_TEST_HELPER;
        final FieldMetadata helperField = governorTypeDetails
                .getField(new JavaSymbolName("helper"));
        if (helperField != null) {
            Validate.isTrue(
                    helperField.getFieldType().getFullyQualifiedTypeName()
                            .equals(helperType.getFullyQualifiedTypeName()),
                    "Field 'helper' on '%s' must be of type '%s'",
                    destination.getFullyQualifiedTypeName(),
                    helperType.getFullyQualifiedTypeName());
        }
        else {
            // Add the field via the ITD
            final String initializer = "new LocalServiceTestHelper(new com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig())";
            final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(
                    getId(), Modifier.PRIVATE | Modifier.STATIC
                            | Modifier.FINAL, new JavaSymbolName("helper"),
                    helperType, initializer);
            builder.addField(fieldBuilder);
        }

        // Prepare setUp method signature
        final JavaSymbolName setUpMethodName = new JavaSymbolName("setUp");
        final MethodMetadata setUpMethod = getGovernorMethod(setUpMethodName,
                SETUP_PARAMETERS);
        if (setUpMethod != null) {
            Validate.notNull(
                    MemberFindingUtils.getAnnotationOfType(
                            setUpMethod.getAnnotations(), BEFORE_CLASS),
                    "Method 'setUp' on '%s' must be annotated with @BeforeClass",
                    destination.getFullyQualifiedTypeName());
        }
        else {
            // Add the method via the ITD
            final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
            annotations.add(new AnnotationMetadataBuilder(BEFORE_CLASS));

            final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
            bodyBuilder.appendFormalLine("helper.setUp();");

            final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                    getId(), Modifier.PUBLIC | Modifier.STATIC,
                    setUpMethodName, JavaType.VOID_PRIMITIVE,
                    AnnotatedJavaType.convertFromJavaTypes(SETUP_PARAMETERS),
                    new ArrayList<JavaSymbolName>(), bodyBuilder);
            methodBuilder.setAnnotations(annotations);
            builder.addMethod(methodBuilder);
        }

        // Prepare tearDown method signature
        final JavaSymbolName tearDownMethodName = new JavaSymbolName("tearDown");
        final MethodMetadata tearDownMethod = getGovernorMethod(
                tearDownMethodName, TEARDOWN_PARAMETERS);
        if (tearDownMethod != null) {
            Validate.notNull(
                    MemberFindingUtils.getAnnotationOfType(
                            tearDownMethod.getAnnotations(), AFTER_CLASS),
                    "Method 'tearDown' on '%s' must be annotated with @AfterClass",
                    destination.getFullyQualifiedTypeName());
        }
        else {
            // Add the method via the ITD
            final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
            annotations.add(new AnnotationMetadataBuilder(AFTER_CLASS));

            final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
            bodyBuilder.appendFormalLine("helper.tearDown();");

            final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                    getId(),
                    Modifier.PUBLIC | Modifier.STATIC,
                    tearDownMethodName,
                    JavaType.VOID_PRIMITIVE,
                    AnnotatedJavaType.convertFromJavaTypes(TEARDOWN_PARAMETERS),
                    new ArrayList<JavaSymbolName>(), bodyBuilder);
            methodBuilder.setAnnotations(annotations);
            builder.addMethod(methodBuilder);
        }
    }

    /**
     * Adds the JUnit and Spring type level annotations if needed
     */
    private void addRequiredIntegrationTestClassIntroductions(
            final JavaType dodGovernor) {
        // Add an @RunWith(SpringJunit4ClassRunner) annotation to the type, if
        // the user did not define it on the governor directly
        if (MemberFindingUtils.getAnnotationOfType(
                governorTypeDetails.getAnnotations(), RUN_WITH) == null) {
            final AnnotationMetadataBuilder runWithBuilder = new AnnotationMetadataBuilder(
                    RUN_WITH);
            runWithBuilder
                    .addClassAttribute("value",
                            "org.springframework.test.context.junit4.SpringJUnit4ClassRunner");
            builder.addAnnotation(runWithBuilder);
        }

        // Add an @ContextConfiguration("classpath:/applicationContext.xml")
        // annotation to the type, if the user did not define it on the governor
        // directly
        if (MemberFindingUtils.getAnnotationOfType(
                governorTypeDetails.getAnnotations(), CONTEXT_CONFIGURATION) == null) {
            final AnnotationMetadataBuilder contextConfigurationBuilder = new AnnotationMetadataBuilder(
                    CONTEXT_CONFIGURATION);
            contextConfigurationBuilder.addStringAttribute("locations",
                    "classpath*:/META-INF/spring/applicationContext*.xml");
            builder.addAnnotation(contextConfigurationBuilder);
        }

        // Add an @Transactional, if the user did not define it on the governor
        // directly
        if (annotationValues.isTransactional()
                && MemberFindingUtils.getAnnotationOfType(
                        governorTypeDetails.getAnnotations(), TRANSACTIONAL) == null) {
            final AnnotationMetadataBuilder transactionalBuilder = new AnnotationMetadataBuilder(
                    TRANSACTIONAL);
            if (StringUtils.isNotBlank(transactionManager)
                    && !"transactionManager".equals(transactionManager)) {
                transactionalBuilder.addStringAttribute("value",
                        transactionManager);
            }
            builder.addAnnotation(transactionalBuilder);
        }

        // Add the data on demand field if the user did not define it on the
        // governor directly
        final FieldMetadata field = governorTypeDetails
                .getField(new JavaSymbolName("dod"));
        if (field != null) {
            Validate.isTrue(field.getFieldType().equals(dodGovernor),
                    "Field 'dod' on '%s' must be of type '%s'",
                    destination.getFullyQualifiedTypeName(),
                    dodGovernor.getFullyQualifiedTypeName());
            Validate.notNull(
                    MemberFindingUtils.getAnnotationOfType(
                            field.getAnnotations(), AUTOWIRED),
                    "Field 'dod' on '%s' must be annotated with @Autowired",
                    destination.getFullyQualifiedTypeName());
        }
        else {
            // Add the field via the ITD
            final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
            annotations.add(new AnnotationMetadataBuilder(AUTOWIRED));
            final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(
                    getId(), 0, annotations, new JavaSymbolName("dod"),
                    dodGovernor);
            builder.addField(fieldBuilder);
        }

        builder.getImportRegistrationResolver().addImport(ASSERT);
    }

    /**
     * @return a test for the count method, if available and requested (may
     *         return null)
     */
    private MethodMetadataBuilder getCountMethodTest(
            final MemberTypeAdditions countMethod) {
        if (!annotationValues.isCount() || countMethod == null) {
            // User does not want this method
            return null;
        }

        // Prepare method signature
        final JavaSymbolName methodName = new JavaSymbolName("test"
                + StringUtils.capitalize(countMethod.getMethodName()));
        if (governorHasMethod(methodName)) {
            return null;
        }

        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(new AnnotationMetadataBuilder(TEST));

        final String entityName = annotationValues.getEntity()
                .getSimpleTypeName();

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder
                .appendFormalLine("Assert.assertNotNull(\"Data on demand for '"
                        + entityName
                        + "' failed to initialize correctly\", dod."
                        + dataOnDemandMetadata
                                .getRandomPersistentEntityMethod()
                                .getMethodName().getSymbolName() + "());");
        bodyBuilder.appendFormalLine("long count = "
                + countMethod.getMethodCall() + ";");
        bodyBuilder
                .appendFormalLine("Assert.assertTrue(\"Counter for '"
                        + entityName
                        + "' incorrectly reported there were no entries\", count > 0);");

        countMethod.copyAdditionsTo(builder, governorTypeDetails);

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
                bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
    }

    /**
     * @return a test for the find all method, if available and requested (may
     *         return null)
     */
    private MethodMetadataBuilder getFindAllMethodTest(
            final MemberTypeAdditions findAllMethod,
            final MemberTypeAdditions countMethod) {
        if (!annotationValues.isFindAll() || findAllMethod == null
                || countMethod == null) {
            // User does not want this method, or core dependencies are missing
            return null;
        }

        // Prepare method signature
        final JavaSymbolName methodName = new JavaSymbolName("test"
                + StringUtils.capitalize(findAllMethod.getMethodName()));
        if (governorHasMethod(methodName)) {
            return null;
        }

        builder.getImportRegistrationResolver().addImport(LIST);

        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(new AnnotationMetadataBuilder(TEST));

        final String entityName = annotationValues.getEntity()
                .getSimpleTypeName();

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder
                .appendFormalLine("Assert.assertNotNull(\"Data on demand for '"
                        + entityName
                        + "' failed to initialize correctly\", dod."
                        + dataOnDemandMetadata
                                .getRandomPersistentEntityMethod()
                                .getMethodName().getSymbolName() + "());");
        bodyBuilder.appendFormalLine("long count = "
                + countMethod.getMethodCall() + ";");
        bodyBuilder
                .appendFormalLine("Assert.assertTrue(\"Too expensive to perform a find all test for '"
                        + entityName
                        + "', as there are \" + count + \" entries; set the findAllMaximum to exceed this value or set findAll=false on the integration test annotation to disable the test\", count < "
                        + annotationValues.getFindAllMaximum() + ");");
        bodyBuilder.appendFormalLine("List<" + entityName + "> result = "
                + findAllMethod.getMethodCall() + ";");
        bodyBuilder
                .appendFormalLine("Assert.assertNotNull(\"Find all method for '"
                        + entityName + "' illegally returned null\", result);");
        bodyBuilder
                .appendFormalLine("Assert.assertTrue(\"Find all method for '"
                        + entityName
                        + "' failed to return any data\", result.size() > 0);");

        findAllMethod.copyAdditionsTo(builder, governorTypeDetails);
        countMethod.copyAdditionsTo(builder, governorTypeDetails);

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
                bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
    }

    /**
     * @return a test for the find entries method, if available and requested
     *         (may return null)
     */
    private MethodMetadataBuilder getFindEntriesMethodTest(
            final MemberTypeAdditions countMethod,
            final MemberTypeAdditions findEntriesMethod) {
        if (!annotationValues.isFindEntries() || countMethod == null
                || findEntriesMethod == null) {
            // User does not want this method, or core dependencies are missing
            return null;
        }

        // Prepare method signature
        final JavaSymbolName methodName = new JavaSymbolName("test"
                + StringUtils.capitalize(findEntriesMethod.getMethodName()));
        if (governorHasMethod(methodName)) {
            return null;
        }

        builder.getImportRegistrationResolver().addImport(LIST);

        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(new AnnotationMetadataBuilder(TEST));

        final String entityName = annotationValues.getEntity()
                .getSimpleTypeName();

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder
                .appendFormalLine("Assert.assertNotNull(\"Data on demand for '"
                        + entityName
                        + "' failed to initialize correctly\", dod."
                        + dataOnDemandMetadata
                                .getRandomPersistentEntityMethod()
                                .getMethodName().getSymbolName() + "());");
        bodyBuilder.appendFormalLine("long count = "
                + countMethod.getMethodCall() + ";");
        bodyBuilder.appendFormalLine("if (count > 20) count = 20;");
        bodyBuilder.appendFormalLine("int firstResult = 0;");
        bodyBuilder.appendFormalLine("int maxResults = (int) count;");
        bodyBuilder.appendFormalLine("List<" + entityName + "> result = "
                + findEntriesMethod.getMethodCall() + ";");
        bodyBuilder
                .appendFormalLine("Assert.assertNotNull(\"Find entries method for '"
                        + entityName + "' illegally returned null\", result);");
        bodyBuilder
                .appendFormalLine("Assert.assertEquals(\"Find entries method for '"
                        + entityName
                        + "' returned an incorrect number of entries\", count, result.size());");

        findEntriesMethod.copyAdditionsTo(builder, governorTypeDetails);
        countMethod.copyAdditionsTo(builder, governorTypeDetails);

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
                bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
    }

    /**
     * @return a test for the find (by ID) method, if available and requested
     *         (may return null)
     */
    private MethodMetadataBuilder getFindMethodTest(
            final MemberTypeAdditions findMethod,
            final MethodMetadata identifierAccessorMethod) {
        if (!annotationValues.isFind() || findMethod == null
                || identifierAccessorMethod == null) {
            // User does not want this method
            return null;
        }

        // Prepare method signature
        final JavaSymbolName methodName = new JavaSymbolName("test"
                + StringUtils.capitalize(findMethod.getMethodName()));
        if (governorHasMethod(methodName)) {
            return null;
        }

        builder.getImportRegistrationResolver().addImport(
                identifierAccessorMethod.getReturnType());

        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(new AnnotationMetadataBuilder(TEST));

        final String entityName = annotationValues.getEntity()
                .getSimpleTypeName();

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(entityName
                + " obj = dod."
                + dataOnDemandMetadata.getRandomPersistentEntityMethod()
                        .getMethodName().getSymbolName() + "();");
        bodyBuilder
                .appendFormalLine("Assert.assertNotNull(\"Data on demand for '"
                        + entityName
                        + "' failed to initialize correctly\", obj);");
        bodyBuilder.appendFormalLine(identifierAccessorMethod.getReturnType()
                .getSimpleTypeName()
                + " id = obj."
                + identifierAccessorMethod.getMethodName().getSymbolName()
                + "();");
        bodyBuilder
                .appendFormalLine("Assert.assertNotNull(\"Data on demand for '"
                        + entityName
                        + "' failed to provide an identifier\", id);");
        bodyBuilder.appendFormalLine("obj = " + findMethod.getMethodCall()
                + ";");
        bodyBuilder.appendFormalLine("Assert.assertNotNull(\"Find method for '"
                + entityName
                + "' illegally returned null for id '\" + id + \"'\", obj);");
        bodyBuilder.appendFormalLine("Assert.assertEquals(\"Find method for '"
                + entityName
                + "' returned the incorrect identifier\", id, obj."
                + identifierAccessorMethod.getMethodName().getSymbolName()
                + "());");

        findMethod.copyAdditionsTo(builder, governorTypeDetails);

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
                bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
    }

    /**
     * @return a test for the flush method, if available and requested (may
     *         return null)
     */
    private MethodMetadataBuilder getFlushMethodTest(
            final MethodMetadata versionAccessorMethod,
            final MethodMetadata identifierAccessorMethod,
            final MemberTypeAdditions flushMethod,
            final MemberTypeAdditions findMethod) {
        if (!annotationValues.isFlush() || versionAccessorMethod == null
                || identifierAccessorMethod == null || flushMethod == null
                || findMethod == null) {
            // User does not want this method, or core dependencies are missing
            return null;
        }

        // Prepare method signature
        final JavaSymbolName methodName = new JavaSymbolName("test"
                + StringUtils.capitalize(flushMethod.getMethodName()));
        if (governorHasMethod(methodName)) {
            return null;
        }

        final JavaType versionType = versionAccessorMethod.getReturnType();
        builder.getImportRegistrationResolver().addImports(
                identifierAccessorMethod.getReturnType(), versionType);

        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(new AnnotationMetadataBuilder(TEST));

        final String entityName = annotationValues.getEntity()
                .getSimpleTypeName();

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(entityName
                + " obj = dod."
                + dataOnDemandMetadata.getRandomPersistentEntityMethod()
                        .getMethodName().getSymbolName() + "();");
        bodyBuilder
                .appendFormalLine("Assert.assertNotNull(\"Data on demand for '"
                        + entityName
                        + "' failed to initialize correctly\", obj);");
        bodyBuilder.appendFormalLine(identifierAccessorMethod.getReturnType()
                .getSimpleTypeName()
                + " id = obj."
                + identifierAccessorMethod.getMethodName().getSymbolName()
                + "();");
        bodyBuilder
                .appendFormalLine("Assert.assertNotNull(\"Data on demand for '"
                        + entityName
                        + "' failed to provide an identifier\", id);");
        bodyBuilder.appendFormalLine("obj = " + findMethod.getMethodCall()
                + ";");
        bodyBuilder.appendFormalLine("Assert.assertNotNull(\"Find method for '"
                + entityName
                + "' illegally returned null for id '\" + id + \"'\", obj);");
        bodyBuilder.appendFormalLine("boolean modified =  dod."
                + dataOnDemandMetadata.getModifyMethod().getMethodName()
                        .getSymbolName() + "(obj);");

        bodyBuilder
                .appendFormalLine(versionAccessorMethod.getReturnType()
                        .getSimpleTypeName()
                        + " currentVersion = obj."
                        + versionAccessorMethod.getMethodName().getSymbolName()
                        + "();");
        bodyBuilder.appendFormalLine(flushMethod.getMethodCall() + ";");
        if (JdkJavaType.isDateField(versionType)) {
            bodyBuilder
                    .appendFormalLine("Assert.assertTrue(\"Version for '"
                            + entityName
                            + "' failed to increment on flush directive\", (currentVersion != null && obj."
                            + versionAccessorMethod.getMethodName()
                                    .getSymbolName()
                            + "().after(currentVersion)) || !modified);");
        }
        else {
            bodyBuilder
                    .appendFormalLine("Assert.assertTrue(\"Version for '"
                            + entityName
                            + "' failed to increment on flush directive\", (currentVersion != null && obj."
                            + versionAccessorMethod.getMethodName()
                                    .getSymbolName()
                            + "() > currentVersion) || !modified);");
        }
        flushMethod.copyAdditionsTo(builder, governorTypeDetails);
        findMethod.copyAdditionsTo(builder, governorTypeDetails);

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
                bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
    }

    /**
     * @return a test for the merge method, if available and requested (may
     *         return null)
     */
    private MethodMetadataBuilder getMergeMethodTest(
            final MemberTypeAdditions mergeMethod,
            final MemberTypeAdditions findMethod,
            final MemberTypeAdditions flushMethod,
            final MethodMetadata versionAccessorMethod,
            final MethodMetadata identifierAccessorMethod) {
        if (!annotationValues.isMerge() || mergeMethod == null
                || versionAccessorMethod == null || findMethod == null
                || identifierAccessorMethod == null) {
            // User does not want this method, or core dependencies are missing
            return null;
        }

        // Prepare method signature
        final JavaSymbolName methodName = new JavaSymbolName("test"
                + StringUtils.capitalize(mergeMethod.getMethodName())
                + "Update");
        if (governorHasMethod(methodName)) {
            return null;
        }

        final JavaType versionType = versionAccessorMethod.getReturnType();
        builder.getImportRegistrationResolver().addImports(
                identifierAccessorMethod.getReturnType(), versionType);

        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(new AnnotationMetadataBuilder(TEST));

        final String entityName = annotationValues.getEntity()
                .getSimpleTypeName();

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(entityName
                + " obj = dod."
                + dataOnDemandMetadata.getRandomPersistentEntityMethod()
                        .getMethodName().getSymbolName() + "();");
        bodyBuilder
                .appendFormalLine("Assert.assertNotNull(\"Data on demand for '"
                        + entityName
                        + "' failed to initialize correctly\", obj);");
        bodyBuilder.appendFormalLine(identifierAccessorMethod.getReturnType()
                .getSimpleTypeName()
                + " id = obj."
                + identifierAccessorMethod.getMethodName().getSymbolName()
                + "();");
        bodyBuilder
                .appendFormalLine("Assert.assertNotNull(\"Data on demand for '"
                        + entityName
                        + "' failed to provide an identifier\", id);");
        bodyBuilder.appendFormalLine("obj = " + findMethod.getMethodCall()
                + ";");
        bodyBuilder.appendFormalLine("boolean modified =  dod."
                + dataOnDemandMetadata.getModifyMethod().getMethodName()
                        .getSymbolName() + "(obj);");

        bodyBuilder
                .appendFormalLine(versionAccessorMethod.getReturnType()
                        .getSimpleTypeName()
                        + " currentVersion = obj."
                        + versionAccessorMethod.getMethodName().getSymbolName()
                        + "();");

        final String castStr = entityHasSuperclass ? "(" + entityName + ")"
                : "";
        bodyBuilder.appendFormalLine(entityName + " merged = " + castStr
                + mergeMethod.getMethodCall() + ";");

        if (flushMethod != null) {
            bodyBuilder.appendFormalLine(flushMethod.getMethodCall() + ";");
            flushMethod.copyAdditionsTo(builder, governorTypeDetails);
        }

        bodyBuilder
                .appendFormalLine("Assert.assertEquals(\"Identifier of merged object not the same as identifier of original object\", merged."
                        + identifierAccessorMethod.getMethodName()
                                .getSymbolName() + "(), id);");
        if (JdkJavaType.isDateField(versionType)) {
            bodyBuilder
                    .appendFormalLine("Assert.assertTrue(\"Version for '"
                            + entityName
                            + "' failed to increment on merge and flush directive\", (currentVersion != null && obj."
                            + versionAccessorMethod.getMethodName()
                                    .getSymbolName()
                            + "().after(currentVersion)) || !modified);");
        }
        else {
            bodyBuilder
                    .appendFormalLine("Assert.assertTrue(\"Version for '"
                            + entityName
                            + "' failed to increment on merge and flush directive\", (currentVersion != null && obj."
                            + versionAccessorMethod.getMethodName()
                                    .getSymbolName()
                            + "() > currentVersion) || !modified);");
        }
        mergeMethod.copyAdditionsTo(builder, governorTypeDetails);
        findMethod.copyAdditionsTo(builder, governorTypeDetails);

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
                bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
    }

    /**
     * @return a test for the persist method, if available and requested (may
     *         return null)
     */
    private MethodMetadataBuilder getPersistMethodTest(
            final MemberTypeAdditions persistMethod,
            final MemberTypeAdditions flushMethod,
            final MethodMetadata identifierAccessorMethod) {
        if (!annotationValues.isPersist() || persistMethod == null
                || identifierAccessorMethod == null) {
            // User does not want this method
            return null;
        }

        builder.getImportRegistrationResolver().addImports(ITERATOR,
                CONSTRAINT_VIOLATION_EXCEPTION, CONSTRAINT_VIOLATION);

        // Prepare method signature
        final JavaSymbolName methodName = new JavaSymbolName("test"
                + StringUtils.capitalize(persistMethod.getMethodName()));
        if (governorHasMethod(methodName)) {
            return null;
        }

        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(new AnnotationMetadataBuilder(TEST));

        final String entityName = annotationValues.getEntity()
                .getSimpleTypeName();

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder
                .appendFormalLine("Assert.assertNotNull(\"Data on demand for '"
                        + entityName
                        + "' failed to initialize correctly\", dod."
                        + dataOnDemandMetadata
                                .getRandomPersistentEntityMethod()
                                .getMethodName().getSymbolName() + "());");
        bodyBuilder.appendFormalLine(entityName
                + " obj = dod."
                + dataOnDemandMetadata.getNewTransientEntityMethod()
                        .getMethodName().getSymbolName()
                + "(Integer.MAX_VALUE);");
        bodyBuilder
                .appendFormalLine("Assert.assertNotNull(\"Data on demand for '"
                        + entityName
                        + "' failed to provide a new transient entity\", obj);");

        if (!hasEmbeddedIdentifier) {
            bodyBuilder.appendFormalLine("Assert.assertNull(\"Expected '"
                    + entityName + "' identifier to be null\", obj."
                    + identifierAccessorMethod.getMethodName().getSymbolName()
                    + "());");
        }

        bodyBuilder.appendFormalLine("try {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(persistMethod.getMethodCall() + ";");
        bodyBuilder.indentRemove();
        bodyBuilder
                .appendFormalLine("} catch (final ConstraintViolationException e) {");
        bodyBuilder.indent();
        bodyBuilder
                .appendFormalLine("final StringBuilder msg = new StringBuilder();");
        bodyBuilder
                .appendFormalLine("for (Iterator<ConstraintViolation<?>> iter = e.getConstraintViolations().iterator(); iter.hasNext();) {");
        bodyBuilder.indent();
        bodyBuilder
                .appendFormalLine("final ConstraintViolation<?> cv = iter.next();");
        bodyBuilder
                .appendFormalLine("msg.append(\"[\").append(cv.getRootBean().getClass().getName()).append(\".\").append(cv.getPropertyPath()).append(\": \").append(cv.getMessage()).append(\" (invalid value = \").append(cv.getInvalidValue()).append(\")\").append(\"]\");");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder
                .appendFormalLine("throw new IllegalStateException(msg.toString(), e);");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");

        if (flushMethod != null) {
            bodyBuilder.appendFormalLine(flushMethod.getMethodCall() + ";");
            flushMethod.copyAdditionsTo(builder, governorTypeDetails);
        }

        bodyBuilder.appendFormalLine("Assert.assertNotNull(\"Expected '"
                + entityName + "' identifier to no longer be null\", obj."
                + identifierAccessorMethod.getMethodName().getSymbolName()
                + "());");

        persistMethod.copyAdditionsTo(builder, governorTypeDetails);

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
                bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
    }

    /**
     * @return a test for the persist method, if available and requested (may
     *         return null)
     */
    private MethodMetadataBuilder getRemoveMethodTest(
            final MemberTypeAdditions removeMethod,
            final MemberTypeAdditions findMethod,
            final MemberTypeAdditions flushMethod,
            final MethodMetadata identifierAccessorMethod) {
        if (!annotationValues.isRemove() || removeMethod == null
                || findMethod == null || identifierAccessorMethod == null) {
            // User does not want this method or one of its core dependencies
            return null;
        }

        // Prepare method signature
        final JavaSymbolName methodName = new JavaSymbolName("test"
                + StringUtils.capitalize(removeMethod.getMethodName()));
        if (governorHasMethod(methodName)) {
            return null;
        }

        builder.getImportRegistrationResolver().addImport(
                identifierAccessorMethod.getReturnType());

        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(new AnnotationMetadataBuilder(TEST));
        if (isGaeSupported) {
            final AnnotationMetadataBuilder transactionalBuilder = new AnnotationMetadataBuilder(
                    TRANSACTIONAL);
            if (StringUtils.isNotBlank(transactionManager)
                    && !"transactionManager".equals(transactionManager)) {
                transactionalBuilder.addStringAttribute("value",
                        transactionManager);
            }
            transactionalBuilder
                    .addEnumAttribute("propagation", new EnumDetails(
                            PROPAGATION, new JavaSymbolName("SUPPORTS")));
            annotations.add(transactionalBuilder);
        }

        final String entityName = annotationValues.getEntity()
                .getSimpleTypeName();

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(entityName
                + " obj = dod."
                + dataOnDemandMetadata.getRandomPersistentEntityMethod()
                        .getMethodName().getSymbolName() + "();");
        bodyBuilder
                .appendFormalLine("Assert.assertNotNull(\"Data on demand for '"
                        + entityName
                        + "' failed to initialize correctly\", obj);");
        bodyBuilder.appendFormalLine(identifierAccessorMethod.getReturnType()
                .getSimpleTypeName()
                + " id = obj."
                + identifierAccessorMethod.getMethodName().getSymbolName()
                + "();");
        bodyBuilder
                .appendFormalLine("Assert.assertNotNull(\"Data on demand for '"
                        + entityName
                        + "' failed to provide an identifier\", id);");
        bodyBuilder.appendFormalLine("obj = " + findMethod.getMethodCall()
                + ";");
        bodyBuilder.appendFormalLine(removeMethod.getMethodCall() + ";");

        if (flushMethod != null) {
            bodyBuilder.appendFormalLine(flushMethod.getMethodCall() + ";");
            flushMethod.copyAdditionsTo(builder, governorTypeDetails);
        }

        bodyBuilder.appendFormalLine("Assert.assertNull(\"Failed to remove '"
                + entityName + "' with identifier '\" + id + \"'\", "
                + findMethod.getMethodCall() + ");");

        removeMethod.copyAdditionsTo(builder, governorTypeDetails);
        findMethod.copyAdditionsTo(builder, governorTypeDetails);

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
                bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("identifier", getId());
        builder.append("valid", valid);
        builder.append("aspectName", aspectName);
        builder.append("destinationType", destination);
        builder.append("governor", governorPhysicalTypeMetadata.getId());
        builder.append("itdTypeDetails", itdTypeDetails);
        return builder.toString();
    }
}