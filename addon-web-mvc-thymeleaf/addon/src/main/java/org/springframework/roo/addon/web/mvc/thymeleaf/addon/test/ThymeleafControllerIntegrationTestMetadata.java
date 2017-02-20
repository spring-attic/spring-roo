package org.springframework.roo.addon.web.mvc.thymeleaf.addon.test;

import java.lang.reflect.Modifier;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.web.mvc.thymeleaf.annotations.test.RooThymeleafControllerIntegrationTest;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.comments.CommentStructure;
import org.springframework.roo.classpath.details.comments.CommentStructure.CommentLocation;
import org.springframework.roo.classpath.details.comments.JavadocComment;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.model.SpringletsJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooThymeleafControllerIntegrationTest}.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class ThymeleafControllerIntegrationTestMetadata extends
    AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING =
      ThymeleafControllerIntegrationTestMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private static final JavaSymbolName MOCK_MVC_FIELD_NAME = new JavaSymbolName("mvc");

  private static final JavaType RUN_WITH = new JavaType("org.junit.runner.RunWith");
  private static final JavaType TEST = new JavaType("org.junit.Test");

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

  private final JavaType controller;
  private final List<JavaType> entityServices;
  private final JavaType entityFactory;
  private final JavaSymbolName entityFactoryFieldName;

  /**
   * Constructor
   * 
   * @param identifier
   * @param aspectName
   * @param governorPhysicalTypeMetadata
   * @param annotationValues
   * @param controller 
   * @param managedEntity
   * @param entityFactory 
   * @param relatedServices
   */
  public ThymeleafControllerIntegrationTestMetadata(final String identifier,
      final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final ThymeleafControllerIntegrationTestAnnotationValues annotationValues,
      final JavaType controller, final JavaType managedEntity, final JavaType entityFactory,
      final List<JavaType> relatedServices) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.isTrue(isValid(identifier),
        "Metadata identification string '%s' does not appear to be a valid", identifier);
    Validate.notNull(annotationValues, "Annotation values required");

    this.controller = controller;
    this.entityServices = relatedServices;
    this.entityFactory = entityFactory;
    this.entityFactoryFieldName = new JavaSymbolName("factory");

    // Add @RunWith(SpringRunner.class)
    ensureGovernorIsAnnotated(getRunWithAnnotation());

    // Add @SpringletsWebMvcTest
    ensureGovernorIsAnnotated(getSpringletsWebMvcAnnotation());

    // Add MockMvc field
    ensureGovernorHasField(getMockMvcField());

    // Add entity service fields
    for (JavaType service : this.entityServices) {
      ensureGovernorHasField(getEntityServiceField(service));
    }

    // Add entity factory field
    ensureGovernorHasField(getEntityFactoryField());

    // Add test method example
    ensureGovernorHasMethod(new MethodMetadataBuilder(getTestExampleMethod()));

    itdTypeDetails = builder.build();
  }

  /**
   * Builds and return a test example method.
   * 
   * @return {@link MethodMetadata}
   */
  private MethodMetadata getTestExampleMethod() {
    JavaSymbolName methodName = new JavaSymbolName("testMethodExample");

    // Check if method exists in governor
    MethodMetadata method = getGovernorMethod(methodName);
    if (method != null) {
      return method;
    }

    // Build method body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Setup
    bodyBuilder.appendFormalLine("// Setup");
    bodyBuilder.appendFormalLine("// Previous tasks");
    bodyBuilder.newLine();

    // Exercise
    bodyBuilder.appendFormalLine("// Exercise");
    bodyBuilder.appendFormalLine("// Execute method to test");
    bodyBuilder.newLine();

    // Verify
    bodyBuilder.appendFormalLine("// Verify");
    bodyBuilder.appendFormalLine("// Check results with assertions");

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.getId(), Modifier.PUBLIC, methodName,
            JavaType.VOID_PRIMITIVE, bodyBuilder);

    // Add @Test
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(TEST));

    CommentStructure commentStructure = new CommentStructure();
    commentStructure.addComment(new JavadocComment(
        "Test method example. To be implemented by developer."), CommentLocation.BEGINNING);
    methodBuilder.setCommentStructure(commentStructure);

    return methodBuilder.build();
  }

  /**
   * Builds and returns the entity factory field.
   * 
   * @return {@link FieldMetadataBuilder}
   */
  private FieldMetadataBuilder getEntityFactoryField() {
    FieldMetadataBuilder fieldBuilder =
        new FieldMetadataBuilder(this.getId(), Modifier.PRIVATE, this.entityFactoryFieldName,
            this.entityFactory, String.format("new %s()", getNameOfJavaType(this.entityFactory)));

    return fieldBuilder;
  }

  /**
   * Builds and returns the entity service field, annotated with @MockBean
   * 
   * @param the service {@link JavaType}
   * @return {@link FieldMetadataBuilder}
   */
  private FieldMetadataBuilder getEntityServiceField(JavaType service) {
    JavaSymbolName fieldName =
        new JavaSymbolName(String.format("%sService",
            StringUtils.uncapitalize(service.getSimpleTypeName())));
    FieldMetadataBuilder fieldBuilder =
        new FieldMetadataBuilder(this.getId(), Modifier.PRIVATE, fieldName, service, null);

    // Add @Autowired
    fieldBuilder.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.MOCK_BEAN));

    return fieldBuilder;
  }

  /**
   * Builds and returns a {@link MockMvc} field, annotated with @Autowired 
   * 
   * @return {@link FieldMetadataBuilder}
   */
  private FieldMetadataBuilder getMockMvcField() {
    FieldMetadataBuilder fieldBuilder =
        new FieldMetadataBuilder(this.getId(), Modifier.PRIVATE, MOCK_MVC_FIELD_NAME,
            SpringJavaType.MOCK_MVC, null);

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

  /**
   * Builds and returns @SpringletsWebMvcTest annotation
   * 
   * @return {@link AnnotationMetadataBuilder}
   */
  private AnnotationMetadataBuilder getSpringletsWebMvcAnnotation() {
    AnnotationMetadataBuilder annotationBuilder =
        new AnnotationMetadataBuilder(SpringletsJavaType.SPRINGLETS_WEB_MVC_TEST);
    annotationBuilder.addClassAttribute("controllers", this.controller);
    annotationBuilder.addBooleanAttribute("secure", false);
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
