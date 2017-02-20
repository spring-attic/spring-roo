package org.springframework.roo.addon.layers.repository.jpa.addon.test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.layers.repository.jpa.annotations.test.RooRepositoryJpaIntegrationTest;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.Jsr303JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.model.SpringletsJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooRepositoryJpaIntegrationTest}.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class RepositoryJpaIntegrationTestMetadata extends
    AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = RepositoryJpaIntegrationTestMetadata.class
      .getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private static final JavaSymbolName EXPECTED_EXCEPTION_FIELD_NAME = new JavaSymbolName("thrown");
  private static final JavaSymbolName REPOSITORY_FIELD_NAME = new JavaSymbolName("repository");
  private static final JavaSymbolName DATA_ON_DEMAND_FIELD_NAME = new JavaSymbolName("dod");
  private static final JavaSymbolName BEFORE_METHOD_NAME = new JavaSymbolName(
      "checkDataOnDemandHasInitializedCorrectly");
  private static final JavaSymbolName COUNT_TEST_METHOD_NAME = new JavaSymbolName(
      "countShouldReturnExpectedValue");
  private static final JavaSymbolName PERSIST_SHOULD_GENERATE_ID_METHOD_NAME = new JavaSymbolName(
      "persistShouldGenerateIdValue");

  private static final JavaType RULE = new JavaType("org.junit.Rule");
  private static final JavaType BEFORE = new JavaType("org.junit.Before");
  private static final JavaType TEST = new JavaType("org.junit.Test");
  private static final JavaType RUN_WITH = new JavaType("org.junit.runner.RunWith");
  private static final JavaType EXPECTED_EXCEPTION = new JavaType(
      "org.junit.rules.ExpectedException");
  private static final JavaType ASSERT_THAT = new JavaType(
      "org.assertj.core.api.Assertions.assertThat");

  public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
    return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
  }

  public static JavaType getJavaType(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
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

  private final RepositoryJpaIntegrationTestAnnotationValues annotationValues;
  private final JavaType jpaDetachableRepositoryClass;
  private final JavaType entity;
  private final JavaSymbolName getRandomIdMethodName;
  private final JavaType identifierType;
  private final String entityVar;
  private final FieldMetadata repositoryField;
  private final FieldMetadata dodField;
  private final JavaSymbolName identifierAccessorMethodName;
  private final String getRandomMethodName;
  private final String entityPlural;
  private final JavaType defaultReturnType;
  private final boolean isReadOnly;

  /**
   * Constructor
   * 
   * @param identifier
   * @param aspectName
   * @param governorPhysicalTypeMetadata
   * @param annotationValues
   * @param jpaDetachableRepositoryClass 
   * @param identifierType the managed entity identifier type
   * @param identifierAccessorMethodName the managed entity identifier 
   *            accessor method name
   * @param entityPlural 
   * @param entity 
   * @param defaultReturnType the repository default return type for default 
   *            queries
   * @param isReadOnly whether the entity is read only
   */
  public RepositoryJpaIntegrationTestMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final RepositoryJpaIntegrationTestAnnotationValues annotationValues,
      final JavaType jpaDetachableRepositoryClass, final JavaType identifierType,
      final JavaSymbolName identifierAccessorMethodName, final String entityPlural,
      final JavaType entity, final JavaType defaultReturnType, final boolean isReadOnly) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.isTrue(isValid(identifier),
        "Metadata identification string '%s' does not appear to be a valid", identifier);
    Validate.notNull(annotationValues, "Annotation values required");

    this.annotationValues = annotationValues;
    this.jpaDetachableRepositoryClass = jpaDetachableRepositoryClass;
    this.entity = entity;
    this.getRandomIdMethodName =
        new JavaSymbolName(String.format("getRandom%sId", this.entity.getSimpleTypeName()));
    this.getRandomMethodName = String.format("getRandom%s", this.entity.getSimpleTypeName());
    this.identifierType = identifierType;
    this.entityVar = StringUtils.uncapitalize(this.entity.getSimpleTypeName());
    this.identifierAccessorMethodName = identifierAccessorMethodName;
    this.entityPlural = entityPlural;
    this.defaultReturnType = defaultReturnType;
    this.isReadOnly = isReadOnly;

    // Add @RunWith(SpringRunner.class)
    ensureGovernorIsAnnotated(getRunWithAnnotation());

    // Add @DataJpaTest
    ensureGovernorIsAnnotated(getDataJpaTestAnnotation());

    // Add @Import
    ensureGovernorIsAnnotated(getImportAnnotation());

    // Add fields
    ensureGovernorHasField(getExpectedExceptionField());
    this.repositoryField = getRepositoryField().build();
    ensureGovernorHasField(getRepositoryField());
    this.dodField = getDodField().build();
    ensureGovernorHasField(getDodField());

    // Add @Before method
    ensureGovernorHasMethod(new MethodMetadataBuilder(getBeforeMethod()));

    // Add count test method
    ensureGovernorHasMethod(new MethodMetadataBuilder(getCountTestMethod()));

    // Add find one test method
    ensureGovernorHasMethod(new MethodMetadataBuilder(getFindOneTestMethod()));

    // Add find all test method
    ensureGovernorHasMethod(new MethodMetadataBuilder(getFindAllTestMethod()));

    if (!this.isReadOnly) {

      // Add persist should generate id method
      ensureGovernorHasMethod(new MethodMetadataBuilder(getPersistGenerateIdTestMethod()));

      // Add delete should make entity unavailable method
      ensureGovernorHasMethod(new MethodMetadataBuilder(
          getDeleteShouldMakeEntityUnavailableTestMethod()));
    }


    // Add find all custom not filtered and not paged test method 
    ensureGovernorHasMethod(new MethodMetadataBuilder(
        getFindAllCustomNotFilteredNotPagedTestMethod()));

    // Add find all custom not filtered and paged test method 
    ensureGovernorHasMethod(new MethodMetadataBuilder(getFindAllCustomNotFilteredPagedTestMethod()));

    // Add getRandomId method
    ensureGovernorHasMethod(new MethodMetadataBuilder(getRandomIdMethod()));

    itdTypeDetails = builder.build();
  }

  /**
   * Builds a method annotated with `@Before` which checks if data-on-demand 
   * field has initialized correctly.
   * 
   * @return {@link MethodMetadata}
   */
  private MethodMetadata getBeforeMethod() {
    MethodMetadata method = getGovernorMethod(BEFORE_METHOD_NAME);
    if (method != null) {
      return method;
    }

    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // assertThat(dod.getRandomPet())
    bodyBuilder.appendFormalLine("%s(%s().%s())", getNameOfJavaType(ASSERT_THAT, true),
        getAccessorMethod(this.dodField).getMethodName(), this.getRandomMethodName);

    //      .as("Check data on demand for 'Pet' initializes correctly by getting a random Pet")
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(
        ".as(\"Check data on demand for '%1$s' initializes correctly by getting a random %1$s\")",
        getNameOfJavaType(this.entity));

    //      .isNotNull();
    bodyBuilder.appendFormalLine(".isNotNull();");
    bodyBuilder.reset();

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, BEFORE_METHOD_NAME,
            JavaType.VOID_PRIMITIVE, bodyBuilder);

    // Add @Before
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(BEFORE));

    return methodBuilder.build();
  }

  /**
   * Builds a method to test count method.
   * 
   * @return {@link MethodMetadata}
   */
  private MethodMetadata getCountTestMethod() {
    MethodMetadata method = getGovernorMethod(COUNT_TEST_METHOD_NAME);
    if (method != null) {
      return method;
    }

    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Verify
    bodyBuilder.appendFormalLine("// Verify");

    // assertThat(repository.count()).as("Check there are available 'Pet' entries").isGreaterThan(0);
    bodyBuilder.appendFormalLine(
        "%s(%s().count()).as(\"Check there are available '%s' entries\").isGreaterThan(0);",
        getNameOfJavaType(ASSERT_THAT), getAccessorMethod(this.repositoryField).getMethodName(),
        getNameOfJavaType(this.entity));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, COUNT_TEST_METHOD_NAME,
            JavaType.VOID_PRIMITIVE, bodyBuilder);

    // Add @Test
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(TEST));

    return methodBuilder.build();
  }

  /**
   * Builds and returns `@DataJpaTest` annotation
   * 
   * @return {@link AnnotationMetadataBuilder}
   */
  private AnnotationMetadataBuilder getDataJpaTestAnnotation() {
    AnnotationMetadataBuilder annotationBuilder =
        new AnnotationMetadataBuilder(SpringJavaType.DATA_JPA_TEST);
    return annotationBuilder;
  }

  /**
   * Builds a method to test the deletion of entity records.
   * 
   * @return {@link MethodMetadata}
   */
  private MethodMetadata getDeleteShouldMakeEntityUnavailableTestMethod() {
    JavaSymbolName methodName =
        new JavaSymbolName(String.format("deleteShouldMake%sUnavailable",
            getNameOfJavaType(this.entity)));

    // Check if method exists on governor
    MethodMetadata method = getGovernorMethod(methodName);
    if (method != null) {
      return method;
    }

    // Build method body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Setup
    bodyBuilder.appendFormalLine("// Setup");

    // Long id = getRandomPetId();
    bodyBuilder.appendFormalLine("%s id = %s();", getNameOfJavaType(this.identifierType),
        this.getRandomIdMethodName);

    // Pet pet = repository.findOne(id);
    bodyBuilder.appendFormalLine("%s %s = %s().findOne(id);", getNameOfJavaType(this.entity),
        this.entityVar, getAccessorMethod(this.repositoryField).getMethodName());

    // Exercise
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("// Exercise");

    // repository.delete(pet);
    bodyBuilder.appendFormalLine("%s().delete(%s);", getAccessorMethod(this.repositoryField)
        .getMethodName(), this.entityVar);

    // Verify
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("// Verify");

    // assertThat(repository.findOne(id))
    bodyBuilder.appendFormalLine("%s(%s().findOne(id))", getNameOfJavaType(ASSERT_THAT),
        getAccessorMethod(this.repositoryField).getMethodName());

    //  .as("Check the deleted 'Pet' %s is no longer available with 'findOne'", pet).isNull();
    bodyBuilder.appendFormalLine(
        ".as(\"Check the deleted '%s' %s is no longer available with 'findOne'\", %s).isNull();",
        getNameOfJavaType(this.entity), "%s", this.entityVar);

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
            bodyBuilder);

    // Add @Test
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(TEST));

    return methodBuilder.build();
  }

  /**
   * Builds and returns a `private` data-on-demand field. 
   * 
   * @return {@link FieldMetadataBuilder}
   */
  private FieldMetadataBuilder getDodField() {
    FieldMetadataBuilder fieldBuilder =
        new FieldMetadataBuilder(this.getId(), Modifier.PRIVATE, DATA_ON_DEMAND_FIELD_NAME,
            this.annotationValues.getDodClass(), null);

    // Add @Autowired
    fieldBuilder.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));

    return fieldBuilder;
  }

  /**
   * Builds and returns a <code>public</code> ExpectedException field. 
   * 
   * @return {@link FieldMetadataBuilder}
   */
  private FieldMetadataBuilder getExpectedExceptionField() {
    FieldMetadataBuilder fieldBuilder =
        new FieldMetadataBuilder(this.getId(), Modifier.PUBLIC, EXPECTED_EXCEPTION_FIELD_NAME,
            EXPECTED_EXCEPTION, String.format("%s.none()", getNameOfJavaType(EXPECTED_EXCEPTION)));

    // Add @Rule
    fieldBuilder.addAnnotation(new AnnotationMetadataBuilder(RULE));

    return fieldBuilder;
  }

  /**
   * Builds a method to test the find all custom method not filtered and paged.
   * 
   * @return {@link MethodMetadata}
   */
  private MethodMetadata getFindAllCustomNotFilteredPagedTestMethod() {
    JavaSymbolName methodName =
        new JavaSymbolName(String.format("findAllCustomNotFilteredPagedShouldReturnA%sPage",
            this.entityPlural));

    // Check if method exists on governor
    MethodMetadata method = getGovernorMethod(methodName);
    if (method != null) {
      return method;
    }

    // Build method body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Exercise
    bodyBuilder.appendFormalLine("// Exercise");

    // Page<Pet> all = repository.findAll((GlobalSearch) null, new PageRequest(0, 3));
    bodyBuilder.appendFormalLine("%s<%s> all = %s().findAll((%s) null, new %s(0, 3));",
        getNameOfJavaType(SpringJavaType.PAGE), getNameOfJavaType(this.defaultReturnType),
        getAccessorMethod(this.repositoryField).getMethodName(),
        getNameOfJavaType(SpringletsJavaType.SPRINGLETS_GLOBAL_SEARCH),
        getNameOfJavaType(SpringJavaType.PAGE_REQUEST));

    // Verify
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("// Verify");

    // assertThat(all.getNumberOfElements())
    bodyBuilder.appendFormalLine("%s(all.getNumberOfElements())", getNameOfJavaType(ASSERT_THAT));


    //  .as("Check result number is not greater than the page size").isLessThanOrEqualTo(3);
    bodyBuilder.indent();
    bodyBuilder
        .appendFormalLine(".as(\"Check result number is not greater than the page size\").isLessThanOrEqualTo(3);");
    bodyBuilder.indentRemove();

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
            bodyBuilder);

    // Add @Test
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(TEST));

    return methodBuilder.build();
  }

  /**
   * Builds a method to test the find all custom method not filtered and not paged.
   * 
   * @return {@link MethodMetadata}
   */
  private MethodMetadata getFindAllCustomNotFilteredNotPagedTestMethod() {
    JavaSymbolName methodName =
        new JavaSymbolName(String.format("findAllCustomNotFilteredNotPagedShouldReturnAll%s",
            this.entityPlural));

    // Check if method exists on governor
    MethodMetadata method = getGovernorMethod(methodName);
    if (method != null) {
      return method;
    }

    // Build method body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Exercise
    bodyBuilder.appendFormalLine("// Exercise");

    // Page<Pet> all = repository.findAll((GlobalSearch) null, new PageRequest(0, dod.getSize()));
    bodyBuilder.appendFormalLine(
        "%s<%s> all = %s().findAll((%s) null, new %s(0, %s().getSize()));",
        getNameOfJavaType(SpringJavaType.PAGE), getNameOfJavaType(this.defaultReturnType),
        getAccessorMethod(this.repositoryField).getMethodName(),
        getNameOfJavaType(SpringletsJavaType.SPRINGLETS_GLOBAL_SEARCH),
        getNameOfJavaType(SpringJavaType.PAGE_REQUEST), getAccessorMethod(this.dodField)
            .getMethodName());

    // Verify
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("// Verify");

    // assertThat(all.getNumberOfElements())
    bodyBuilder.appendFormalLine("%s(all.getNumberOfElements())", getNameOfJavaType(ASSERT_THAT));

    //  .as("Check 'findAll' with null 'GlobalSearch' and no pagination returns all entries")
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(
        ".as(\"Check 'findAll' with null '%s' and no pagination returns all entries\")",
        getNameOfJavaType(SpringletsJavaType.SPRINGLETS_GLOBAL_SEARCH));

    //  .isEqualTo(dod.getSize());
    bodyBuilder.appendFormalLine(".isEqualTo(%s().getSize());", getAccessorMethod(this.dodField)
        .getMethodName());
    bodyBuilder.reset();

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
            bodyBuilder);

    // Add @Test
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(TEST));

    return methodBuilder.build();
  }

  /**
   * Builds a method to test all one method.
   * 
   * @return {@link MethodMetadata}
   */
  private MethodMetadata getFindAllTestMethod() {

    // Set method name
    final JavaSymbolName methodName =
        new JavaSymbolName(String.format("findAllShouldReturnAll%s", this.entityPlural));

    // Check if method exists on governor
    MethodMetadata method = getGovernorMethod(methodName);
    if (method != null) {
      return method;
    }

    // Build method body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Setup
    bodyBuilder.appendFormalLine("// Setup");

    // assertThat(repository.count())
    bodyBuilder.appendFormalLine("%s(%s().count())", getNameOfJavaType(ASSERT_THAT),
        getAccessorMethod(this.repositoryField).getMethodName());

    //  .as("Check the number of entries is not too big (250 entries). "
    bodyBuilder.indent();
    bodyBuilder
        .appendFormalLine(".as(\"Check the number of entries is not too big (250 entries). \"");

    //      + "If it is, please review the tests so it doesn't take too long to run them")
    bodyBuilder.indent();
    bodyBuilder
        .appendFormalLine("+ \"If it is, please review the tests so it doesn't take too long to run them\")");
    bodyBuilder.indentRemove();

    //  .isLessThan(250);
    bodyBuilder.appendFormalLine(".isLessThan(250);");
    bodyBuilder.indentRemove();

    // Exercise
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("// Exercise");

    // List<Pet> result = repository.findAll();
    bodyBuilder.appendFormalLine("%s<%s> result = %s().findAll();",
        getNameOfJavaType(JavaType.LIST), getNameOfJavaType(this.entity),
        getAccessorMethod(this.repositoryField).getMethodName());

    // Verify
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("// Verify");

    // assertThat(result).as("Check 'findAll' returns a not null list of entries").isNotNull();
    bodyBuilder.appendFormalLine(
        "%s(result).as(\"Check 'findAll' returns a not null list of entries\").isNotNull();",
        getNameOfJavaType(ASSERT_THAT));

    // assertThat(result.size()).as("Check 'findAll' returns a not empty list of entries")
    bodyBuilder.appendFormalLine(
        "%s(result.size()).as(\"Check 'findAll' returns a not empty list of entries\")",
        getNameOfJavaType(ASSERT_THAT));

    // .isGreaterThan(0);
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(".isGreaterThan(0);");
    bodyBuilder.reset();

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
            bodyBuilder);

    // Add @Test
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(TEST));

    return methodBuilder.build();
  }

  /**
   * Builds a method to test find one method.
   * 
   * @return {@link MethodMetadata}
   */
  private MethodMetadata getFindOneTestMethod() {
    final JavaSymbolName methodName =
        new JavaSymbolName(String.format("findOneShouldReturnExisting%s",
            this.entity.getSimpleTypeName()));
    MethodMetadata method = getGovernorMethod(methodName);
    if (method != null) {
      return method;
    }

    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Setup
    bodyBuilder.appendFormalLine("// Setup");

    // Long id = getRandomPetId();
    bodyBuilder.appendFormalLine("%s id = %s();", getNameOfJavaType(this.identifierType),
        this.getRandomIdMethodName);

    // Exercise
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("// Exercise");

    // Pet pet = repository.findOne(id);
    bodyBuilder.appendFormalLine("%s %s = %s().findOne(id);", getNameOfJavaType(this.entity),
        entityVar, getAccessorMethod(this.repositoryField).getMethodName());

    // Verify
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("// Verify");

    // assertThat(pet).as("Check that findOne illegally returned null for id %s", id).isNotNull();
    bodyBuilder.appendFormalLine(
        "%s(%s).as(\"Check that findOne illegally returned null for id %s\", id).isNotNull();",
        getNameOfJavaType(ASSERT_THAT), this.entityVar, "%s");

    // assertThat(id).as("Check the identifier of the found 'Pet' is the same used to look for it")
    bodyBuilder.appendFormalLine(
        "%s(id).as(\"Check the identifier of the found '%s' is the same used to look for it\")",
        getNameOfJavaType(ASSERT_THAT), getNameOfJavaType(this.entity));

    //      .isEqualTo(pet.getId());
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(".isEqualTo(%s.%s());", this.entityVar,
        this.identifierAccessorMethodName);
    bodyBuilder.reset();

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
            bodyBuilder);

    // Add @Test
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(TEST));

    return methodBuilder.build();
  }

  /**
   * Builds and returns `@Import` annotation
   * 
   * @return {@link AnnotationMetadataBuilder}
   */
  private AnnotationMetadataBuilder getImportAnnotation() {
    AnnotationMetadataBuilder annotationBuilder =
        new AnnotationMetadataBuilder(SpringJavaType.ANNOTATION_IMPORT);

    // Create List of ClassAttributeValue
    List<ClassAttributeValue> typesToImport = new ArrayList<ClassAttributeValue>();
    typesToImport.add(new ClassAttributeValue(new JavaSymbolName("value"), this.annotationValues
        .getDodConfigurationClass()));
    typesToImport.add(new ClassAttributeValue(new JavaSymbolName("value"),
        this.jpaDetachableRepositoryClass));

    // Convert List to ArrayAttributeValue
    ArrayAttributeValue<ClassAttributeValue> importAttr =
        new ArrayAttributeValue<ClassAttributeValue>(new JavaSymbolName("value"), typesToImport);

    // Add annotation attribute
    annotationBuilder.addAttribute(importAttr);

    return annotationBuilder;
  }

  /**
   * Builds a method to test the generation of id when persisting entities.
   *  
   * @return {@link MethodMetadata}
   */
  private MethodMetadata getPersistGenerateIdTestMethod() {

    // Check if method exists on governor
    MethodMetadata method = getGovernorMethod(PERSIST_SHOULD_GENERATE_ID_METHOD_NAME);
    if (method != null) {
      return method;
    }

    // Build method body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Setup
    bodyBuilder.appendFormalLine("// Setup");

    // Exercise
    bodyBuilder.appendFormalLine("// Exercise");

    // Pet pet = dod.getNewRandomTransientPet();
    bodyBuilder.appendFormalLine("%1$s %2$s = %3$s().getNewRandomTransient%1$s();",
        getNameOfJavaType(this.entity), this.entityVar, getAccessorMethod(this.dodField)
            .getMethodName());

    // Verify
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("// Verify");

    // assertThat(pet).as("Check the Data on demand generated a new non null 'Pet'").isNotNull();
    bodyBuilder.appendFormalLine(
        "%s(%s).as(\"Check the Data on demand generated a new non null '%s'\").isNotNull();",
        getNameOfJavaType(ASSERT_THAT), this.entityVar, getNameOfJavaType(this.entity));

    // assertThat(pet.getId()).as("Check the Data on demand generated a new 'Pet' whose id is null")
    bodyBuilder.appendFormalLine(
        "%s(%s.%s()).as(\"Check the Data on demand generated a new '%s' whose id is null\")",
        getNameOfJavaType(ASSERT_THAT), this.entityVar, this.identifierAccessorMethodName,
        getNameOfJavaType(this.entity));

    //  .isNull();
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(".isNull();");
    bodyBuilder.indentRemove();

    // try {
    bodyBuilder.appendFormalLine("try {");

    //  pet = repository.saveAndFlush(pet);
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("%1$s = %2$s().saveAndFlush(%1$s);", this.entityVar,
        getAccessorMethod(this.repositoryField).getMethodName());

    // } catch (final ConstraintViolationException e) {
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("} catch (final %s e) {",
        getNameOfJavaType(Jsr303JavaType.CONSTRAINT_VIOLATION_EXCEPTION));

    // final StringBuilder msg = new StringBuilder();
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("final %1$s msg = new %1$s();",
        getNameOfJavaType(JdkJavaType.STRING_BUILDER));

    // for (Iterator<ConstraintViolation<?>> iter = e.getConstraintViolations().iterator(); iter
    bodyBuilder.appendFormalLine(
        "for (%s<%s<?>> iter = e.getConstraintViolations().iterator(); iter",
        getNameOfJavaType(JdkJavaType.ITERATOR),
        getNameOfJavaType(Jsr303JavaType.CONSTRAINT_VIOLATION));

    // .hasNext();) {
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(".hasNext();) {");

    // final ConstraintViolation<?> cv = iter.next();
    bodyBuilder.appendFormalLine("final %s<?> cv = iter.next();",
        getNameOfJavaType(Jsr303JavaType.CONSTRAINT_VIOLATION));

    // msg.append("[").append(cv.getRootBean().getClass().getName()).append(".")
    bodyBuilder
        .appendFormalLine("msg.append(\"[\").append(cv.getRootBean().getClass().getName()).append(\".\")");

    //  .append(cv.getPropertyPath()).append(": ").append(cv.getMessage())
    bodyBuilder.indent();
    bodyBuilder
        .appendFormalLine(".append(cv.getPropertyPath()).append(\": \").append(cv.getMessage())");

    // .append(" (invalid value = ").append(cv.getInvalidValue()).append(")").append("]");
    bodyBuilder
        .appendFormalLine(".append(\" (invalid value = \").append(cv.getInvalidValue()).append(\")\").append(\"]\");");

    // }
    bodyBuilder.indentRemove();
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // throw new IllegalStateException(msg.toString(), e);
    bodyBuilder.appendFormalLine("throw new %s(msg.toString(), e);",
        getNameOfJavaType(JdkJavaType.ILLEGAL_STATE_EXCEPTION));
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");

    // assertThat(pet.getId()).as("Check a 'Pet' (%s) id is not null after been persisted", pet)
    bodyBuilder
        .appendFormalLine(
            "%1$s(%2$s.%3$s()).as(\"Check a '%4$s' (%5$s) id is not null after been persisted\", %2$s)",
            getNameOfJavaType(ASSERT_THAT), this.entityVar, this.identifierAccessorMethodName,
            getNameOfJavaType(this.entity), "%s");

    //  .isNotNull();
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(".isNotNull();");
    bodyBuilder.reset();

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, PERSIST_SHOULD_GENERATE_ID_METHOD_NAME,
            JavaType.VOID_PRIMITIVE, bodyBuilder);

    // Add @Test
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(TEST));

    return methodBuilder.build();
  }

  /**
   * Builds a method to obtain the id value of a random generated entity 
   * instance generated with data-on-demand. 
   * 
   * @return {@link MethodMetadata}
   */
  private MethodMetadata getRandomIdMethod() {
    MethodMetadata method = getGovernorMethod(this.getRandomIdMethodName);
    if (method != null) {
      return method;
    }

    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Pet pet = dod.getRandomPet();
    bodyBuilder.appendFormalLine("%s %s = %s().%s();", getNameOfJavaType(this.entity),
        this.entityVar, getAccessorMethod(this.dodField).getMethodName(), this.getRandomMethodName);

    // Long id = pet.getId();
    bodyBuilder.appendFormalLine("%s id = %s.%s();", getNameOfJavaType(this.identifierType),
        this.entityVar, this.identifierAccessorMethodName);

    // assertThat(id).as("Check the Data on demand generated a 'Pet' with an identifier").isNotNull();
    bodyBuilder.appendFormalLine(
        "%s(id).as(\"Check the Data on demand generated a '%s' with an identifier\").isNotNull();",
        getNameOfJavaType(ASSERT_THAT), getNameOfJavaType(this.entity));

    // return id;
    bodyBuilder.appendFormalLine("return id;");

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PRIVATE, this.getRandomIdMethodName,
            this.identifierType, bodyBuilder);

    return methodBuilder.build();
  }

  /**
   * Builds and returns a `private` Roo repository field. 
   * 
   * @return {@link FieldMetadataBuilder}
   */
  private FieldMetadataBuilder getRepositoryField() {
    FieldMetadataBuilder fieldBuilder =
        new FieldMetadataBuilder(this.getId(), Modifier.PRIVATE, REPOSITORY_FIELD_NAME,
            this.annotationValues.getTargetClass(), null);

    // Add @Autowired
    fieldBuilder.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));

    return fieldBuilder;
  }

  /**
   * Builds and returns `@RunWith` annotation
   * 
   * @return {@link AnnotationMetadataBuilder}
   */
  private AnnotationMetadataBuilder getRunWithAnnotation() {
    AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(RUN_WITH);
    annotationBuilder.addClassAttribute("value", SpringJavaType.SPRING_RUNNER);
    return annotationBuilder;
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
