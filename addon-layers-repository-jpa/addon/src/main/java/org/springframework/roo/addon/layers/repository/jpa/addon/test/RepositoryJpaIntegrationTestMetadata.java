package org.springframework.roo.addon.layers.repository.jpa.addon.test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaMetadata;
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
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooRepositoryJpaIntegrationTest}.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class RepositoryJpaIntegrationTestMetadata extends
    AbstractItdTypeDetailsProvidingMetadataItem {

  // TODO: This class is under construction. It can contain several commented code

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

  private static final JavaType RULE = new JavaType("org.junit.Rule");
  private static final JavaType MOCKITO_RULE = new JavaType("org.mockito.junit.MockitoRule");
  private static final JavaType MOCKITO_JUNIT = new JavaType("org.mockito.junit.MockitoJUnit");
  private static final JavaType MOCK = new JavaType("org.mockito.Mock");
  private static final JavaType BEFORE = new JavaType("org.junit.Before");
  private static final JavaType AFTER = new JavaType("org.junit.After");
  private static final JavaType TEST = new JavaType("org.junit.Test");
  private static final JavaType IGNORE = new JavaType("org.junit.Ignore");
  private static final JavaType RUN_WITH = new JavaType("org.junit.runner.RunWith");
  private static final JavaType EXPECTED_EXCEPTION = new JavaType(
      "org.junit.rules.ExpectedException");
  private static final JavaType ASSERT_THAT = new JavaType(
      "org.assertj.core.api.Assertions.assertThat");

  private Map<JavaType, FieldMetadata> entityFactories = new TreeMap<JavaType, FieldMetadata>();

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
  private final RepositoryJpaMetadata repositoryMetadata;
  private final JavaSymbolName getRandomIdMethodName;
  private final JavaType identifierType;
  private final String entityVar;
  private final FieldMetadata repositoryField;
  private final FieldMetadata dodField;
  private final JavaSymbolName identifierAccessorMethodName;
  private final JavaSymbolName findOneTestMethodName;
  private final String getRandomMethodName;

  /**
   * Constructor
   * 
   * @param identifier
   * @param aspectName
   * @param governorPhysicalTypeMetadata
   * @param annotationValues
   * @param jpaDetachableRepositoryClass 
   * @param repositoryMetadata 
   * @param identifierType 
   * @param identifierAccessorMethodName 
   */
  public RepositoryJpaIntegrationTestMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final RepositoryJpaIntegrationTestAnnotationValues annotationValues,
      final JavaType jpaDetachableRepositoryClass, final RepositoryJpaMetadata repositoryMetadata,
      final JavaType identifierType, final JavaSymbolName identifierAccessorMethodName) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.isTrue(isValid(identifier),
        "Metadata identification string '%s' does not appear to be a valid", identifier);
    Validate.notNull(annotationValues, "Annotation values required");

    this.annotationValues = annotationValues;
    this.jpaDetachableRepositoryClass = jpaDetachableRepositoryClass;
    this.entity = repositoryMetadata.getEntity();
    this.repositoryMetadata = repositoryMetadata;
    this.getRandomIdMethodName =
        new JavaSymbolName(String.format("getRandom%sId", this.entity.getSimpleTypeName()));
    this.getRandomMethodName = String.format("getRandom%s", this.entity.getSimpleTypeName());
    this.identifierType = identifierType;
    this.entityVar = StringUtils.uncapitalize(this.entity.getSimpleTypeName());
    this.identifierAccessorMethodName = identifierAccessorMethodName;
    this.findOneTestMethodName =
        new JavaSymbolName(String.format("findOneShouldReturnExisting%s",
            this.entity.getSimpleTypeName()));

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

    // Add test count method
    ensureGovernorHasMethod(new MethodMetadataBuilder(getCountTestMethod()));

    // Add test find one method
    ensureGovernorHasMethod(new MethodMetadataBuilder(getFindOneTestMethod()));

    ensureGovernorHasMethod(new MethodMetadataBuilder(getRandomIdMethod()));

    itdTypeDetails = builder.build();
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

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, COUNT_TEST_METHOD_NAME,
            JavaType.VOID_PRIMITIVE, bodyBuilder);

    // Add @Test
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(TEST));

    return methodBuilder.build();
  }

  /**
   * Builds a method to test count method.
   * 
   * @return {@link MethodMetadata}
   */
  private MethodMetadata getFindOneTestMethod() {
    MethodMetadata method = getGovernorMethod(this.findOneTestMethodName);
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
    bodyBuilder.appendFormalLine("// Exercise");

    // Pet pet = repository.findOne(id);
    bodyBuilder.appendFormalLine("%s %s = %s().findOne(id);", getNameOfJavaType(this.entity),
        entityVar, getAccessorMethod(this.repositoryField).getMethodName());

    // Verify
    bodyBuilder.appendFormalLine("// Verify");

    // assertThat(pet).as("Check that findOne illegally returned null for id %s", id).isNotNull();
    bodyBuilder.appendFormalLine(
        "%1$s(%2$s).as(\"Check that findOne illegally returned null for id %s\", id).isNotNull();",
        getNameOfJavaType(ASSERT_THAT), this.entityVar);

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
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, this.findOneTestMethodName,
            JavaType.VOID_PRIMITIVE, bodyBuilder);

    // Add @Test
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(TEST));

    return methodBuilder.build();
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
