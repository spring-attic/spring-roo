package org.springframework.roo.addon.test.addon.unit;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.test.annotations.RooIntegrationTest;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.comments.CommentStructure;
import org.springframework.roo.classpath.details.comments.CommentStructure.CommentLocation;
import org.springframework.roo.classpath.details.comments.JavadocComment;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooIntegrationTest}.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class UnitTestMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = UnitTestMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);
  private static final JavaType RULE = new JavaType("org.junit.Rule");
  private static final JavaType MOCKITO_RULE = new JavaType("org.mockito.junit.MockitoRule");
  private static final JavaType MOCKITO_JUNIT = new JavaType("org.mockito.junit.MockitoJUnit");
  private static final JavaType MOCK = new JavaType("org.mockito.Mock");
  private static final JavaType BEFORE = new JavaType("org.junit.Before");
  private static final JavaType AFTER = new JavaType("org.junit.After");
  private static final JavaType TEST = new JavaType("org.junit.Test");
  private static final JavaType IGNORE = new JavaType("org.junit.Ignore");


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

  private final JavaType targetType;
  private final ImportRegistrationResolver importResolver;
  private final List<AnnotationMetadataBuilder> mockAnnotation =
      new ArrayList<AnnotationMetadataBuilder>();
  private final AnnotationMetadata ignoreAnnotation;
  private ClassOrInterfaceTypeDetails targetTypeDetails;
  private List<String> methodNames;
  private MemberDetails memberDetails;

  public UnitTestMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final UnitTestAnnotationValues annotationValues, List<FieldMetadata> fieldDependencies,
      List<MethodMetadata> methods, ClassOrInterfaceTypeDetails targetTypeDetails,
      MemberDetails memberDetails) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.isTrue(isValid(identifier),
        "Metadata identification string '%s' does not appear to be a valid", identifier);
    Validate.notNull(annotationValues, "Annotation values required");

    this.targetType = annotationValues.getTargetClass();
    this.targetTypeDetails = targetTypeDetails;
    this.memberDetails = memberDetails;
    this.importResolver = builder.getImportRegistrationResolver();
    this.methodNames = new ArrayList<String>();

    // Build @Ignore
    AnnotationMetadataBuilder ignoreAnnotationBuilder = new AnnotationMetadataBuilder(IGNORE);
    ignoreAnnotationBuilder.addStringAttribute("value", "To be implemented by developer");
    ignoreAnnotation = ignoreAnnotationBuilder.build();

    // Initialize @Mock
    mockAnnotation.add(new AnnotationMetadataBuilder(MOCK));

    // Add @Rule field
    ensureGovernorHasField(getMockitoRuleField());

    // Build one mock field for each external dependency field
    for (FieldMetadata field : fieldDependencies) {
      ensureGovernorHasField(getDependencyField(field));
    }

    // Add target class field
    ensureGovernorHasField(getTargetClassField(this.targetType));

    // Add setup method
    ensureGovernorHasMethod(getSetupMethod());

    // Add clean method
    ensureGovernorHasMethod(getCleanMethod());

    // Build one test method for each targetClass method
    for (MethodMetadata method : methods) {

      // Add method names and avoid name repetition
      int counter = 2;
      String candidateName = method.getMethodName().getSymbolName();
      while (this.methodNames.contains(candidateName)) {
        candidateName = candidateName.concat(String.valueOf(counter));
        counter++;
      }
      this.methodNames.add(candidateName);

      // Add method
      ensureGovernorHasMethod(getTestMethod(method, candidateName));
    }

    itdTypeDetails = builder.build();
  }

  /**
   * Obtains a method annotated with @Test and @Ignore for testing one targeted 
   * type's method. Developer should implement logic.
   * 
   * @return {@link MethodMetadataBuilder}
   */
  private MethodMetadataBuilder getTestMethod(MethodMetadata method, String candidateName) {
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder.newLine();

    // Setup phase
    bodyBuilder.appendFormalLine("// Setup");
    bodyBuilder.appendFormalLine("// Implement additional setup if necessary");
    bodyBuilder.newLine();

    // Exercise phase
    bodyBuilder.appendFormalLine("// Exercise");
    List<JavaSymbolName> parameterNames = method.getParameterNames();
    StringBuffer parameters = new StringBuffer();
    for (int i = 0; i < parameterNames.size(); i++) {
      parameters.append(parameterNames.get(i).getSymbolName());
      if (i != parameterNames.size() - 1) {
        parameters.append(", ");
      }
    }
    if (method.getReturnType().equals(JavaType.VOID_PRIMITIVE)) {
      bodyBuilder.appendFormalLine(String.format("// this.%s.%s(%s);",
          StringUtils.uncapitalize(targetType.getSimpleTypeName()), method.getMethodName(),
          parameters));
    } else {
      bodyBuilder.appendFormalLine(String.format("// %s result = this.%s.%s(%s);", method
          .getReturnType().getNameIncludingTypeParameters(false, importResolver), StringUtils
          .uncapitalize(targetType.getSimpleTypeName()), method.getMethodName(), parameters));
    }
    bodyBuilder.newLine();

    // Verify phase
    bodyBuilder.appendFormalLine("// Verify");
    bodyBuilder.appendFormalLine("// Implement assertions");

    // Check if method alread exists
    JavaSymbolName methodName = new JavaSymbolName(String.format("%sTest", candidateName));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
            bodyBuilder);

    // Add @Test and @Ignore
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(TEST).build());
    methodBuilder.addAnnotation(ignoreAnnotation);

    return methodBuilder;
  }

  /**
   * Obtains a method annotated with @After for doing the test class teardown phase 
   * after finishing each test.
   * 
   * @return {@link MethodMetadataBuilder}
   */
  private MethodMetadataBuilder getCleanMethod() {
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("// Clean needed after executing each test method");
    bodyBuilder.appendFormalLine("// To be implemented by developer");
    bodyBuilder.newLine();

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName("clean"),
            JavaType.VOID_PRIMITIVE, bodyBuilder);

    // Add @After
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(AFTER).build());

    // Add comment
    CommentStructure comment = new CommentStructure();
    JavadocComment javaDocComment =
        new JavadocComment(
            "This method will be automatically executed after each test method for freeing resources allocated with @Before annotated method.");
    comment.addComment(javaDocComment, CommentLocation.BEGINNING);
    methodBuilder.setCommentStructure(comment);

    return methodBuilder;
  }

  /**
   * Obtains a method annotated with @Before for doing the test class setup before 
   * launching each test.
   * 
   * @return {@link MethodMetadataBuilder}
   */
  private MethodMetadataBuilder getSetupMethod() {

    // this.targetType = new TargetType();
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    if (!targetTypeDetails.isInterface() && !targetTypeDetails.isAbstract()
        && memberDetails.getConstructors().size() == 0) {
      bodyBuilder.appendFormalLine(String.format("this.%s = new %s();",
          StringUtils.uncapitalize(targetType.getSimpleTypeName()),
          targetType.getNameIncludingTypeParameters(false, importResolver)));
    }

    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("// Setup needed before executing each test method");
    bodyBuilder.appendFormalLine("// To be implemented by developer");
    bodyBuilder.newLine();

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName("setup"),
            JavaType.VOID_PRIMITIVE, bodyBuilder);

    // Add @Before
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(BEFORE).build());

    // Add comment
    CommentStructure comment = new CommentStructure();
    JavadocComment javaDocComment =
        new JavadocComment(
            "This method will be automatically executed before each test method for configuring needed resources.");
    comment.addComment(javaDocComment, CommentLocation.BEGINNING);
    methodBuilder.setCommentStructure(comment);

    return methodBuilder;
  }

  /**
   * Creates field with @Mock for using it in tests.
   * 
   * @return {@link FieldMetadataBuilder} for building field into ITD
   */
  private FieldMetadataBuilder getDependencyField(FieldMetadata field) {
    FieldMetadataBuilder fieldBuilder =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE, mockAnnotation, field.getFieldName(),
            field.getFieldType());
    return fieldBuilder;
  }

  /**
   * Creates target class field for initializing in tests.
   * 
   * @return {@link FieldMetadataBuilder} for building field into ITD.
   */
  private FieldMetadataBuilder getTargetClassField(JavaType targetType) {
    FieldMetadataBuilder fieldBuilder =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE, new JavaSymbolName(
            StringUtils.uncapitalize(targetType.getSimpleTypeName())), targetType, null);
    return fieldBuilder;
  }

  /**
   * Creates MockitoRule field for validating and initialize mocks.
   * 
   * @return {@link FieldMetadataBuilder} for building field into ITD.
   */
  private FieldMetadataBuilder getMockitoRuleField() {

    // Create field @Rule
    List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    AnnotationMetadataBuilder ruleAnnotation = new AnnotationMetadataBuilder(RULE);
    annotations.add(ruleAnnotation);

    // Create field
    FieldMetadataBuilder ruleField =
        new FieldMetadataBuilder(getId(), Modifier.PUBLIC, annotations, new JavaSymbolName(
            "mockito"), MOCKITO_RULE);
    ruleField.setFieldInitializer(String.format("%s.rule()",
        new JavaType(MOCKITO_JUNIT.getNameIncludingTypeParameters(false, importResolver))));

    return ruleField;
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
