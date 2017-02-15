package org.springframework.roo.addon.jpa.addon.test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.jpa.annotations.test.RooJpaUnitTest;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
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
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooJpaUnitTest}.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class JpaUnitTestMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  // TODO: This class is under construction. It can contain several commented code

  private static final String PROVIDES_TYPE_STRING = JpaUnitTestMetadata.class.getName();
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

  private final JavaType entity;
  private final String entityVar;
  private final Map<JavaType, JavaType> entityAndItsFactoryMap;
  private final JavaType entityFactory;

  /**
   * Constructor
   * 
   * @param identifier
   * @param aspectName
   * @param governorPhysicalTypeMetadata
   * @param annotationValues
   * @param relationInfos
   * @param entityAndItsFactoryMap the map for each entity (key) and its entity factory (value). 
   */
  public JpaUnitTestMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final JpaUnitTestAnnotationValues annotationValues,
      final Collection<RelationInfo> relationInfos,
      final Map<JavaType, JavaType> entityAndItsFactoryMap) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.isTrue(isValid(identifier),
        "Metadata identification string '%s' does not appear to be a valid", identifier);
    Validate.notNull(annotationValues, "Annotation values required");

    this.entity = annotationValues.getTargetClass();
    this.entityVar = StringUtils.uncapitalize(this.entity.getSimpleTypeName());
    this.entityAndItsFactoryMap = entityAndItsFactoryMap;
    this.entityFactory = entityAndItsFactoryMap.get(this.entity);
    Validate.notNull(this.entityFactory,
        "Unable to locate the entity factory of %s in JpaUnitTestMetadata",
        this.entity.getSimpleTypeName());

    // Add entity factory fields
    for (JavaType entityFactory : entityAndItsFactoryMap.values()) {
      FieldMetadataBuilder entityFactoryField =
          new FieldMetadataBuilder(this.getId(), Modifier.PRIVATE, new JavaSymbolName(
              StringUtils.uncapitalize(entityFactory.getSimpleTypeName())), entityFactory,
              String.format("new %s()", getNameOfJavaType(entityFactory)));
      entityFactories.put(entityFactory, entityFactoryField.build());
      ensureGovernorHasField(entityFactoryField);
    }

    for (RelationInfo relationInfo : relationInfos) {

      // Create addRelation test method
      if (relationInfo.addMethod != null) {
        ensureGovernorHasMethod(new MethodMetadataBuilder(getAddToRelationMethod(relationInfo)));
      }

      // Create removeRelation test method 
      if (relationInfo.removeMethod != null) {
        ensureGovernorHasMethod(new MethodMetadataBuilder(getRemoveFromRelationMethod(relationInfo)));
      }
    }

    //    // Build @Ignore
    //    AnnotationMetadataBuilder ignoreAnnotationBuilder = new AnnotationMetadataBuilder(IGNORE);
    //    ignoreAnnotationBuilder.addStringAttribute("value", "To be implemented by developer");
    //    ignoreAnnotation = ignoreAnnotationBuilder.build();
    //
    //    // Initialize @Mock
    //    mockAnnotation.add(new AnnotationMetadataBuilder(MOCK));
    //
    //    // Add @Rule field
    //    ensureGovernorHasField(getMockitoRuleField());
    //
    //    // Build one mock field for each external dependency field
    //    for (FieldMetadata field : fieldDependencies) {
    //      ensureGovernorHasField(getDependencyField(field));
    //    }
    //
    //    // Add target class field
    //    ensureGovernorHasField(getTargetClassField(this.targetType));
    //
    //    // Add setup method
    //    ensureGovernorHasMethod(getSetupMethod());
    //
    //    // Add clean method
    //    ensureGovernorHasMethod(getCleanMethod());
    //
    //    // Build one test method for each targetClass method
    //    for (MethodMetadata method : methods) {
    //
    //      // Add method names and avoid name repetition
    //      int counter = 2;
    //      String candidateName = method.getMethodName().getSymbolName();
    //      while (this.methodNames.contains(candidateName)) {
    //        candidateName = candidateName.concat(String.valueOf(counter));
    //        counter++;
    //      }
    //      this.methodNames.add(candidateName);
    //
    //      // Add method
    //      ensureGovernorHasMethod(getTestMethod(method, candidateName));
    //    }

    itdTypeDetails = builder.build();
  }

  /**
   * Creates a method to test item additions to a relationship. 
   * 
   * @param addMethod the {@link MethodMetadata} to test.
   * @param relationFieldName a {@link String} with the field name 
   *            representing the relation on the parent entity.
   * @param childEntity the {@link JavaType} of child entity.
   * @param field the {@link FieldMetadata} of relation field.
   * @param relationInfo the {@link RelationInfo} of the relation field.
   * @return the {@link MethodMetadata}
   */
  private MethodMetadata getAddToRelationMethod(RelationInfo relationInfo) {

    // Initialize local variables
    MethodMetadata addMethod = relationInfo.addMethod;
    String relationFieldName = relationInfo.fieldName;
    JavaType childEntity = relationInfo.childType;
    FieldMetadata relationField = relationInfo.fieldMetadata;

    // Create test method name using target method name, child entity name and relation field 
    JavaSymbolName methodName =
        new JavaSymbolName(String.format("%sShouldAddThe%sToThe%sRelationship", addMethod
            .getMethodName().getSymbolName(), childEntity.getSimpleTypeName(), relationFieldName));

    MethodMetadata method = getGovernorMethod(methodName);
    if (method != null) {
      return method;
    }

    // Create body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Setup
    bodyBuilder.appendFormalLine("// Setup");

    // Entity entity = entityFactory.create(0);
    FieldMetadata entityFactory = entityFactories.get(this.entityFactory);
    bodyBuilder.appendFormalLine("%s %s = %s().create(0);", getNameOfJavaType(this.entity),
        this.entityVar, getAccessorMethod(entityFactory).getMethodName());

    // ChildEntity childEntity1 = childEntityFactory.create(0);
    FieldMetadata childEntityFactory =
        entityFactories.get(this.entityAndItsFactoryMap.get(childEntity));
    Validate.notNull(childEntityFactory,
        "Unable to locate the entity factory of %s in JpaUnitTestMetadata",
        childEntity.getSimpleTypeName());
    String childEntityVar1 =
        String.format("%s1", StringUtils.uncapitalize(childEntity.getSimpleTypeName()));
    bodyBuilder.appendFormalLine("%s %s = %s().create(0);", getNameOfJavaType(childEntity),
        childEntityVar1, getAccessorMethod(childEntityFactory).getMethodName());

    // ChildEntity childEntity2 = childEntityFactory.create(1);
    String childEntityVar2 =
        String.format("%s2", StringUtils.uncapitalize(childEntity.getSimpleTypeName()));
    bodyBuilder.appendFormalLine("%s %s = %s().create(1);", getNameOfJavaType(childEntity),
        childEntityVar2, getAccessorMethod(childEntityFactory).getMethodName());
    bodyBuilder.newLine();

    // Exercise
    bodyBuilder.appendFormalLine("// Exercise");

    // entity.ADD_METHOD(Arrays.asList(childEntity1, childEntity2));
    bodyBuilder.appendFormalLine("%s.%s(%s.asList(%s, %s));", entityVar, addMethod.getMethodName(),
        getNameOfJavaType(JavaType.ARRAYS), childEntityVar1, childEntityVar2);
    bodyBuilder.newLine();

    // Verify
    bodyBuilder.appendFormalLine("// Verify");

    // assertThat(entity.RELATION_FIELD_ACCESSOR()).as("Check 'ADD_METHOD_NAME' adds the FIELDNAME to the relationship")
    JavaSymbolName relationFieldAccessorName = BeanInfoUtils.getAccessorMethodName(relationField);
    bodyBuilder.appendFormalLine("%s(%s.%s()).as(\"Check '%s' adds the %s to the relationship\")",
        getNameOfJavaType(new JavaType("org.assertj.core.api.Assertions.assertThat"), true),
        this.entityVar, relationFieldAccessorName, addMethod.getMethodName(), relationFieldName);

    //  .contains(childEntity1).contains(childEntity2);
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(".contains(%s).contains(%s);", childEntityVar1, childEntityVar2);
    bodyBuilder.indentRemove();

    // assertThat(entity).as("Check 'ADD_METHOD_NAME' updates the CHILD_ENTITY relationship side")
    bodyBuilder.appendFormalLine("%s(%s).as(\"Check '%s' updates the %s relationship side\")",
        getNameOfJavaType(new JavaType("org.assertj.core.api.Assertions.assertThat"), true),
        this.entityVar, addMethod.getMethodName(), getNameOfJavaType(childEntity));

    //  .isEqualTo(childEntity1.PARENT_ENTITY_ACCESSOR()).isEqualTo(childEntity2.PARENT_ENTITY_ACCESSOR());
    JavaSymbolName parentEntityAccessor =
        BeanInfoUtils.getAccessorMethodName(new JavaSymbolName(relationInfo.mappedBy), this.entity);
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(".isEqualTo(%1$s.%3$s()).isEqualTo(%2$s.%3$s());",
        childEntityVar1, childEntityVar2, parentEntityAccessor);
    bodyBuilder.reset();

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.getId(), Modifier.PUBLIC, methodName,
            JavaType.VOID_PRIMITIVE, bodyBuilder);

    // Add @Test
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(TEST));

    // Add throws types
    methodBuilder.addThrowsType(JdkJavaType.EXCEPTION);

    return methodBuilder.build();
  }

  /**
   * Creates a method to test item removes from a relationship. 
   * 
   * @param removeMethod the {@link MethodMetadata} to test.
   * @param relationFieldName a {@link String} with the field name 
   *            representing the relation on the parent entity.
   * @param childEntity the {@link JavaType} of child entity.
   * @param field the {@link FieldMetadata} of relation field.
   * @return the {@link MethodMetadata}
   */
  private MethodMetadata getRemoveFromRelationMethod(RelationInfo relationInfo) {

    // Initialize local variables
    String relationFieldName = relationInfo.fieldName;
    JavaType childEntity = relationInfo.childType;
    FieldMetadata relationField = relationInfo.fieldMetadata;

    // Create test method name using target method name, child entity name and relation field 
    JavaSymbolName methodName =
        new JavaSymbolName(String.format("%sShouldRemoveThe%sFromThe%sRelationship",
            relationInfo.addMethod.getMethodName().getSymbolName(),
            childEntity.getSimpleTypeName(), relationFieldName));

    MethodMetadata method = getGovernorMethod(methodName);
    if (method != null) {
      return method;
    }

    // Create body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Setup
    bodyBuilder.appendFormalLine("// Setup");

    // Entity entity = entityFactory.create(0);
    FieldMetadata entityFactory = entityFactories.get(this.entityFactory);
    bodyBuilder.appendFormalLine("%s %s = %s().create(0);", getNameOfJavaType(this.entity),
        this.entityVar, getAccessorMethod(entityFactory).getMethodName());

    // ChildEntity childEntity1 = childEntityFactory.create(0);
    FieldMetadata childEntityFactory =
        entityFactories.get(this.entityAndItsFactoryMap.get(childEntity));
    Validate.notNull(childEntityFactory,
        "Unable to locate the entity factory of %s in JpaUnitTestMetadata",
        childEntity.getSimpleTypeName());
    String childEntityVar1 =
        String.format("%s1", StringUtils.uncapitalize(childEntity.getSimpleTypeName()));
    bodyBuilder.appendFormalLine("%s %s = %s().create(0);", getNameOfJavaType(childEntity),
        childEntityVar1, getAccessorMethod(childEntityFactory).getMethodName());

    // ChildEntity childEntity2 = childEntityFactory.create(1);
    String childEntityVar2 =
        String.format("%s2", StringUtils.uncapitalize(childEntity.getSimpleTypeName()));
    bodyBuilder.appendFormalLine("%s %s = %s().create(1);", getNameOfJavaType(childEntity),
        childEntityVar2, getAccessorMethod(childEntityFactory).getMethodName());

    // entity.ADD_METHOD(Arrays.asList(childEntity1, childEntity2));
    bodyBuilder.appendFormalLine("%s.%s(%s.asList(%s, %s));", entityVar,
        relationInfo.addMethod.getMethodName(), getNameOfJavaType(JavaType.ARRAYS),
        childEntityVar1, childEntityVar2);
    bodyBuilder.newLine();

    // Exercise
    bodyBuilder.appendFormalLine("// Exercise");

    // entity.REMOVE_METHOD(Collections.singleton(childEntity1));
    bodyBuilder.appendFormalLine("%s.%s(%s.singleton(%s));", this.entityVar,
        relationInfo.removeMethod.getMethodName(), getNameOfJavaType(JavaType.COLLECTIONS),
        childEntityVar1);
    bodyBuilder.newLine();

    // Verify
    bodyBuilder.appendFormalLine("// Verify");

    // assertThat(childEntity1.PARENT_ENTITY_ACCESSOR()).as("Check 'REMOVE_METHOD' updates the CHILD_ENTITY relationship side") 
    JavaSymbolName parentEntityAccessor =
        BeanInfoUtils.getAccessorMethodName(new JavaSymbolName(relationInfo.mappedBy), this.entity);
    bodyBuilder.appendFormalLine("%s(%s.%s()).as(\"Check '%s' updates the %s relationship side\")",
        getNameOfJavaType(new JavaType("org.assertj.core.api.Assertions.assertThat"), true),
        childEntityVar1, parentEntityAccessor, relationInfo.removeMethod.getMethodName(),
        getNameOfJavaType(childEntity));

    //  .isNull();
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(".isNull();");
    bodyBuilder.indentRemove();

    // assertThat(entity.RELATION_FIELD_ACCESSOR()).as("Check 'REMOVE_METHOD' removes a CHILD_ENTITY from the relationship")
    JavaSymbolName relationFieldAccessorName = BeanInfoUtils.getAccessorMethodName(relationField);
    bodyBuilder.appendFormalLine(
        "%s(%s.%s()).as(\"Check '%s' removes a %s from the relationship\")",
        getNameOfJavaType(new JavaType("org.assertj.core.api.Assertions.assertThat"), true),
        this.entityVar, relationFieldAccessorName, relationInfo.removeMethod.getMethodName(),
        getNameOfJavaType(childEntity));

    //  .doesNotContain(childEntity1).contains(childEntity2);
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(".doesNotContain(%s).contains(%s);", childEntityVar1,
        childEntityVar2);
    bodyBuilder.reset();

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.getId(), Modifier.PUBLIC, methodName,
            JavaType.VOID_PRIMITIVE, bodyBuilder);

    // Add @Test
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(TEST));

    // Add throws types
    methodBuilder.addThrowsType(JdkJavaType.EXCEPTION);

    return methodBuilder.build();
  }

  /**
   * Obtains a method annotated with @Test and @Ignore for testing one targeted 
   * type's method. Developer should implement logic.
   * 
   * @return {@link MethodMetadataBuilder}
   */
  private MethodMetadataBuilder getTestMethod(MethodMetadata method, String candidateName) {
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    //    bodyBuilder.newLine();
    //
    //    // Setup phase
    //    bodyBuilder.appendFormalLine("// Setup");
    //    bodyBuilder.appendFormalLine("// Implement additional setup if necessary");
    //    bodyBuilder.newLine();
    //
    //    // Exercise phase
    //    bodyBuilder.appendFormalLine("// Exercise");
    //    List<JavaSymbolName> parameterNames = method.getParameterNames();
    //    StringBuffer parameters = new StringBuffer();
    //    for (int i = 0; i < parameterNames.size(); i++) {
    //      parameters.append(parameterNames.get(i).getSymbolName());
    //      if (i != parameterNames.size() - 1) {
    //        parameters.append(", ");
    //      }
    //    }
    //    if (method.getReturnType().equals(JavaType.VOID_PRIMITIVE)) {
    //      bodyBuilder.appendFormalLine(String.format("// this.%s.%s(%s);",
    //          StringUtils.uncapitalize(targetType.getSimpleTypeName()), method.getMethodName(),
    //          parameters));
    //    } else {
    //      bodyBuilder.appendFormalLine(String.format("// %s result = this.%s.%s(%s);",
    //          method.getReturnType().getNameIncludingTypeParameters(false, importResolver),
    //          StringUtils.uncapitalize(targetType.getSimpleTypeName()), method.getMethodName(),
    //          parameters));
    //    }
    //    bodyBuilder.newLine();
    //
    //    // Verify phase
    //    bodyBuilder.appendFormalLine("// Verify");
    //    bodyBuilder.appendFormalLine("// Implement assertions");
    //
    //    // Check if method alread exists
    //    JavaSymbolName methodName = new JavaSymbolName(String.format("%sTest", candidateName));
    //
    //    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    //    MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC,
    //        methodName, JavaType.VOID_PRIMITIVE, bodyBuilder);
    //
    //    // Add @Test and @Ignore
    //    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(TEST).build());
    //    //    methodBuilder.addAnnotation(ignoreAnnotation);
    //
    //    return methodBuilder;
    return null;
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

    //    // this.targetType = new TargetType();
    //    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    //    if (!targetTypeDetails.isInterface() && !targetTypeDetails.isAbstract()
    //        && memberDetails.getConstructors().size() == 0) {
    //      bodyBuilder.appendFormalLine(String.format("this.%s = new %s();",
    //          StringUtils.uncapitalize(targetType.getSimpleTypeName()),
    //          targetType.getNameIncludingTypeParameters(false, importResolver)));
    //    }
    //
    //    bodyBuilder.newLine();
    //    bodyBuilder.appendFormalLine("// Setup needed before executing each test method");
    //    bodyBuilder.appendFormalLine("// To be implemented by developer");
    //    bodyBuilder.newLine();
    //
    //    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    //    MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC,
    //        new JavaSymbolName("setup"), JavaType.VOID_PRIMITIVE, bodyBuilder);
    //
    //    // Add @Before
    //    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(BEFORE).build());
    //
    //    // Add comment
    //    CommentStructure comment = new CommentStructure();
    //    JavadocComment javaDocComment = new JavadocComment(
    //        "This method will be automatically executed before each test method for configuring needed resources.");
    //    comment.addComment(javaDocComment, CommentLocation.BEGINNING);
    //    methodBuilder.setCommentStructure(comment);
    //
    //    return methodBuilder;
    return null;
  }

  /**
   * Creates field with @Mock for using it in tests.
   * 
   * @return {@link FieldMetadataBuilder} for building field into ITD
   */
  private FieldMetadataBuilder getDependencyField(FieldMetadata field) {
    //    FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE,
    //        mockAnnotation, field.getFieldName(), field.getFieldType());
    //    return fieldBuilder;
    return null;
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
    //    ruleField.setFieldInitializer(String.format("%s.rule()",
    //        new JavaType(MOCKITO_JUNIT.getNameIncludingTypeParameters(false, importResolver))));

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
