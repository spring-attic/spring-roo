package org.springframework.roo.addon.test.addon.integration;

import static org.springframework.roo.model.SpringJavaType.AUTOWIRED;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.test.annotations.RooIntegrationTest;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
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
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Metadata for {@link RooIntegrationTest}.
 *
 * @author Ben Alex
 * @author Manuel Iborra
 * @since 1.0
 */
public class IntegrationTestMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final JavaType ASSERT = new JavaType("org.junit.Assert");
  private static final JavaType BEFORE = new JavaType("org.junit.Before");
  private static final JavaType AFTER = new JavaType("org.junit.After");
  private static final String PROVIDES_TYPE_STRING = IntegrationTestMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);
  private static final JavaType RUN_WITH = new JavaType("org.junit.runner.RunWith");
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

  private JavaType dataOnDemandType;
  private List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
  private JavaType repository;

  /**
   * Constructor
   *
   * @param identifier
   * @param aspectName
   * @param governorPhysicalTypeMetadata
   * @param repository
   * @param dataOnDemandType
   * @param methods
   */
  public IntegrationTestMetadata(String identifier, JavaType aspectName,
      PhysicalTypeMetadata governorPhysicalTypeMetadata, JavaType repository,
      JavaType dataOnDemandType, List<MethodMetadata> methods) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.dataOnDemandType = dataOnDemandType;
    this.methods = methods;
    this.repository = repository;

    addRequiredIntegrationTestClassIntroductions();

    ensureGovernorHasMethod(getCleanMethod());

    ensureGovernorHasMethod(getSetupMethod());

    for (MethodMetadata method : methods) {
      ensureGovernorHasMethod(getMethod(method));
    }

    itdTypeDetails = builder.build();
  }

  /**
   * Obtains a method annotated with @After for doing the test class teardown
   * phase after finishing each test.
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
   * Obtains a method annotated with @Before for doing the test class setup
   * before launching each test.
   *
   * @param dataOnDemandType
   *
   * @return {@link MethodMetadataBuilder}
   */
  private MethodMetadataBuilder getSetupMethod() {

    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // dod = new [ENTITY]DataOnDemand();
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("dod = new %s(%s);",
        dataOnDemandType.getSimpleTypeName(),
        StringUtils.uncapitalize(repository.getSimpleTypeName())));

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
   * Adds the JUnit and Spring type level annotations if needed
   */
  private void addRequiredIntegrationTestClassIntroductions() {

    // Add an @RunWith(SpringJunit4ClassRunner) annotation to the type, if
    // the user did not define it on the governor directly
    if (MemberFindingUtils.getAnnotationOfType(governorTypeDetails.getAnnotations(), RUN_WITH) == null) {
      final AnnotationMetadataBuilder runWithBuilder = new AnnotationMetadataBuilder(RUN_WITH);
      runWithBuilder.addClassAttribute("value",
          "org.springframework.test.context.junit4.SpringJUnit4ClassRunner");
      builder.addAnnotation(runWithBuilder);
    }

    // Add an @ActiveProfiles, if the user did not define it on the governor
    // directly
    if (MemberFindingUtils.getAnnotationOfType(governorTypeDetails.getAnnotations(),
        SpringJavaType.ACTIVE_PROFILES) == null) {
      final AnnotationMetadataBuilder activeProfilesBuilder =
          new AnnotationMetadataBuilder(SpringJavaType.ACTIVE_PROFILES);
      activeProfilesBuilder.addStringAttribute("value", "dev");
      builder.addAnnotation(activeProfilesBuilder);
    }

    // Add an @WebAppConfiguration, if the user did not define it on the
    // governor
    // directly
    if (MemberFindingUtils.getAnnotationOfType(governorTypeDetails.getAnnotations(),
        SpringJavaType.WEB_APP_CONFIGURATION) == null) {
      final AnnotationMetadataBuilder webAppConfigurationBuilder =
          new AnnotationMetadataBuilder(SpringJavaType.WEB_APP_CONFIGURATION);
      builder.addAnnotation(webAppConfigurationBuilder);
    }

    // Add an @SpringBootTest, if the user did not define it on the governor
    // directly
    if (MemberFindingUtils.getAnnotationOfType(governorTypeDetails.getAnnotations(),
        SpringJavaType.SPRING_BOOT_TEST) == null) {
      final AnnotationMetadataBuilder springBootTestBuilder =
          new AnnotationMetadataBuilder(SpringJavaType.SPRING_BOOT_TEST);
      builder.addAnnotation(springBootTestBuilder);
    }

    // Add the data on demand field if the user did not define it on the
    // governor directly
    final FieldMetadata field = governorTypeDetails.getField(new JavaSymbolName("dod"));
    if (field != null) {
      Validate.isTrue(field.getFieldType().equals(dataOnDemandType),
          "Field 'dod' on '%s' must be of type '%s'", destination.getFullyQualifiedTypeName(),
          dataOnDemandType.getFullyQualifiedTypeName());
    } else {
      // Add the field via the ITD
      final FieldMetadataBuilder fieldBuilder =
          new FieldMetadataBuilder(getId(), 0, new ArrayList<AnnotationMetadataBuilder>(),
              new JavaSymbolName("dod"), dataOnDemandType);
      builder.addField(fieldBuilder);
    }

    // Add repository field if the user did not define it on the
    // governor directly
    String repositoryParamName = StringUtils.uncapitalize(repository.getSimpleTypeName());
    final FieldMetadata fieldRepo =
        governorTypeDetails.getField(new JavaSymbolName(repositoryParamName));
    if (fieldRepo != null) {
      Validate.isTrue(fieldRepo.getFieldType().equals(repository),
          "Field 'repository' on '%s' must be of type '%s'",
          destination.getFullyQualifiedTypeName(), repository.getFullyQualifiedTypeName());
      Validate.notNull(
          MemberFindingUtils.getAnnotationOfType(fieldRepo.getAnnotations(), AUTOWIRED),
          "Field 'repository' on '%s' must be annotated with @Autowired",
          destination.getFullyQualifiedTypeName());
    } else {

      // Add the field via the ITD
      final List<AnnotationMetadataBuilder> annotations =
          new ArrayList<AnnotationMetadataBuilder>();
      annotations.add(new AnnotationMetadataBuilder(AUTOWIRED));
      final FieldMetadataBuilder fieldBuilderRepo =
          new FieldMetadataBuilder(getId(), 0, annotations,
              new JavaSymbolName(repositoryParamName), repository);
      builder.addField(fieldBuilderRepo);
    }

    builder.getImportRegistrationResolver().addImport(ASSERT);
  }

  private MethodMetadataBuilder getMethod(MethodMetadata method) {
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("// Setup");
    bodyBuilder.appendFormalLine("// Implement additional setup if necessary");
    bodyBuilder.newLine();

    bodyBuilder.appendFormalLine("// Exercise");
    bodyBuilder.newLine();

    bodyBuilder.appendFormalLine("// Verify");
    bodyBuilder.appendFormalLine("// Implement assertions");
    bodyBuilder.newLine();

    String methodName = "test".concat(method.getMethodName().getSymbolNameCapitalisedFirstLetter());
    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName(methodName),
            JavaType.VOID_PRIMITIVE, bodyBuilder);

    // add annotations @Test and @Ignore("To be implemented by developer")
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(TEST).build());

    AnnotationMetadataBuilder ignoreAnnotationBuilder = new AnnotationMetadataBuilder(IGNORE);
    ignoreAnnotationBuilder.addStringAttribute("value", "To be implemented by developer");
    AnnotationMetadata ignoreAnnotation = ignoreAnnotationBuilder.build();
    methodBuilder.addAnnotation(ignoreAnnotation);

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

  public JavaType getDataOnDemandType() {
    return dataOnDemandType;
  }

  public List<MethodMetadata> getMethods() {
    return methods;
  }

  public JavaType getRepository() {
    return repository;
  }

}
