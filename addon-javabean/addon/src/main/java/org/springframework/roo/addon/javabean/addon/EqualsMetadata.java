package org.springframework.roo.addon.javabean.addon;

import static org.springframework.roo.model.JavaType.BOOLEAN_PRIMITIVE;
import static org.springframework.roo.model.JavaType.INT_PRIMITIVE;
import static org.springframework.roo.model.JavaType.OBJECT;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.javabean.annotations.RooEquals;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.comments.CommentStructure;
import org.springframework.roo.classpath.details.comments.CommentStructure.CommentLocation;
import org.springframework.roo.classpath.details.comments.JavadocComment;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Metadata for {@link RooEquals}.
 *
 * @author Alan Stewart
 * @since 1.2.0
 */
public class EqualsMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final JavaType EQUALS_BUILDER = new JavaType(
      "org.apache.commons.lang3.builder.EqualsBuilder");
  private static final JavaSymbolName EQUALS_METHOD_NAME = new JavaSymbolName("equals");
  private static final JavaType HASH_CODE_BUILDER = new JavaType(
      "org.apache.commons.lang3.builder.HashCodeBuilder");
  private static final JavaSymbolName HASH_CODE_METHOD_NAME = new JavaSymbolName("hashCode");
  private static final String OBJECT_NAME = "obj";
  private static final String PROVIDES_TYPE_STRING = EqualsMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
    return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
  }

  public static JavaType getJavaType(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  /**
   * Returns the class-level ID of this type of metadata
   *
   * @return a valid class-level MID
   */
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

  private final EqualsAnnotationValues annotationValues;
  private final List<FieldMetadata> locatedFields;
  private final FieldMetadata identifierField;
  private final boolean isJpaEntity;

  /**
   * Constructor
   *
   * @param identifier the ID of this piece of metadata (required)
   * @param aspectName the name of the ITD to generate (required)
   * @param governorPhysicalTypeMetadata the details of the governor
   *            (required)
   * @param annotationValues the values of the @RooEquals annotation
   *            (required)
   * @param equalityFields the fields to be compared by the
   *            `equals` method (can be `null` or empty)
   * @param identifierField the identifier field, in case the destination
   *            was an entity
   * @param hasJavaBeanAnnotation
   */
  public EqualsMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final EqualsAnnotationValues annotationValues, final List<FieldMetadata> equalityFields,
      final FieldMetadata identifierField, final boolean hasJavaBeanAnnotation) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.isTrue(isValid(identifier), "Metadata id '%s' is invalid", identifier);
    Validate.notNull(annotationValues, "Annotation values required");

    this.isJpaEntity = annotationValues.isJpaEntity();
    if (this.isJpaEntity && !hasJavaBeanAnnotation) {
      Validate.notNull(identifierField, "Couldn't find any identifier field for %s",
          this.destination.getSimpleTypeName());
    }

    this.annotationValues = annotationValues;
    this.locatedFields = equalityFields;
    this.identifierField = identifierField;


    if (!hasJavaBeanAnnotation && !CollectionUtils.isEmpty(equalityFields)) {
      ensureGovernorHasMethod(new MethodMetadataBuilder(getEqualsMethod()));
      ensureGovernorHasMethod(new MethodMetadataBuilder(getHashCodeMethod()));
    }

    // Create a representation of the desired output ITD
    itdTypeDetails = builder.build();
  }

  /**
   * Returns the default `equals` method body. Used for not-entity classes.
   *
   * @return {@link InvocableMemberBodyBuilder}
   */
  private static InvocableMemberBodyBuilder generateDefaultEqualsMethodBody(JavaType target,
      boolean appendSuper, List<FieldMetadata> fields,
      ImportRegistrationResolver importRegistrationResolver) {
    String typeName = target.getSimpleTypeName();
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder.appendFormalLine("if (!(" + OBJECT_NAME + " instanceof " + typeName + ")) {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("return false;");
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");
    bodyBuilder.appendFormalLine("if (this == " + OBJECT_NAME + ") {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("return true;");
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");
    bodyBuilder.appendFormalLine(typeName + " rhs = (" + typeName + ") " + OBJECT_NAME + ";");

    final StringBuilder builder =
        new StringBuilder(String.format("return new %s()",
            EQUALS_BUILDER.getNameIncludingTypeParameters(false, importRegistrationResolver)));
    if (appendSuper) {
      builder.append(".appendSuper(super.equals(" + OBJECT_NAME + "))");
    }
    for (final FieldMetadata field : fields) {
      builder.append(".append(" + field.getFieldName() + ", rhs." + field.getFieldName() + ")");
    }
    builder.append(".isEquals();");

    bodyBuilder.appendFormalLine(builder.toString());
    return bodyBuilder;
  }

  /**
   * Returns the default `hasCode` method return statement
   *
   * @return a {@link StringBuilder}
   */
  private static StringBuilder getDefaultHashCodeMethodReturnStatment(
      EqualsAnnotationValues annotationValues, List<FieldMetadata> locatedFields,
      ImportRegistrationResolver importRegistrationResolver) {
    final StringBuilder builder =
        new StringBuilder(String.format("return new %s()",
            HASH_CODE_BUILDER.getNameIncludingTypeParameters(false, importRegistrationResolver)));
    if (annotationValues.isAppendSuper()) {
      builder.append(".appendSuper(super.hashCode())");
    }
    for (final FieldMetadata field : locatedFields) {
      builder.append(".append(" + field.getFieldName() + ")");
    }
    builder.append(".toHashCode();");
    return builder;
  }

  /**
   * Returns the `equals` method to be generated
   *
   * @return `null` if no generation is required
   */
  private MethodMetadata getEqualsMethod() {
    MethodMetadata method = getGovernorMethod(EQUALS_METHOD_NAME, OBJECT);
    if (method != null) {
      return method;
    }

    return generateEqualsMethod(getId(), destination, annotationValues,
        identifierField == null ? null : getAccessorMethod(identifierField).getMethodName(),
        locatedFields, builder).build();
  }

  /**
   * Generate equals method
   *
   * @param metadataId
   * @param target
   * @param annotationValues
   * @param identifierAccessor
   * @param locatedFields
   * @param builder
   * @return
   */
  protected static MethodMetadataBuilder generateEqualsMethod(String metadataId, JavaType target,
      EqualsAnnotationValues annotationValues, JavaSymbolName identifierAccessor,
      List<FieldMetadata> locatedFields, ItdTypeDetailsBuilder builder) {
    final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName(OBJECT_NAME));

    // Create the method body depending on destination class properties
    InvocableMemberBodyBuilder bodyBuilder = null;
    if (annotationValues.isJpaEntity()) {
      bodyBuilder =
          getJpaEntityEqualsMethodBody(target, identifierAccessor,
              builder.getImportRegistrationResolver());
    } else {
      bodyBuilder =
          generateDefaultEqualsMethodBody(target, annotationValues.isAppendSuper(), locatedFields,
              builder.getImportRegistrationResolver());
    }

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(metadataId, Modifier.PUBLIC, EQUALS_METHOD_NAME,
            BOOLEAN_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(OBJECT), parameterNames,
            bodyBuilder);

    if (annotationValues.isJpaEntity()) {
      CommentStructure commentStructure = new CommentStructure();
      commentStructure
          .addComment(
              new JavadocComment(
                  "This `equals` implementation is specific for JPA entities and uses "
                      .concat(IOUtils.LINE_SEPARATOR)
                      .concat("the entity identifier for it, following the article in ")
                      .concat(IOUtils.LINE_SEPARATOR)
                      .concat(
                          "https://vladmihalcea.com/2016/06/06/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/")),
              CommentLocation.BEGINNING);
      methodBuilder.setCommentStructure(commentStructure);
    }

    return methodBuilder;
  }

  /**
   * Returns the `hashCode` method to be generated
   *
   * @return `null` if no generation is required
   */
  private MethodMetadata getHashCodeMethod() {
    MethodMetadata method = getGovernorMethod(HASH_CODE_METHOD_NAME);
    if (method != null) {
      return method;
    }

    return generateHashCodeMethod(getId(), annotationValues, locatedFields,
        builder.getImportRegistrationResolver()).build();
  }

  /**
   * Generate hashCode method
   *
   * @param metadataId
   * @param annotationValues
   * @param locatedFields
   * @param importRegistrationResolver
   * @return
   */
  protected static MethodMetadataBuilder generateHashCodeMethod(String metadataId,
      EqualsAnnotationValues annotationValues, List<FieldMetadata> locatedFields,
      ImportRegistrationResolver importRegistrationResolver) {
    // Create the method
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    StringBuilder builder = null;

    if (annotationValues.isJpaEntity()) {
      builder = getJpaEntityHashCodeMethodReturnStatment();
    } else {
      builder =
          getDefaultHashCodeMethodReturnStatment(annotationValues, locatedFields,
              importRegistrationResolver);
    }

    bodyBuilder.appendFormalLine(builder.toString());

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(metadataId, Modifier.PUBLIC, HASH_CODE_METHOD_NAME,
            INT_PRIMITIVE, bodyBuilder);

    if (annotationValues.isJpaEntity()) {
      CommentStructure commentStructure = new CommentStructure();
      commentStructure
          .addComment(
              new JavadocComment(
                  "This `hashCode` implementation is specific for JPA entities and uses a fixed `int` value to be able "
                      .concat(IOUtils.LINE_SEPARATOR)
                      .concat(
                          "to identify the entity in collections after a new id is assigned to the entity, following the article in ")
                      .concat(IOUtils.LINE_SEPARATOR)
                      .concat(
                          "https://vladmihalcea.com/2016/06/06/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/")),
              CommentLocation.BEGINNING);
      methodBuilder.setCommentStructure(commentStructure);
    }

    return methodBuilder;
  }

  /**
   * Returns the specific `equals` method body defined for JPA entity classes.
   *
   * @return {@link InvocableMemberBodyBuilder}
   */
  private static InvocableMemberBodyBuilder getJpaEntityEqualsMethodBody(JavaType target,
      JavaSymbolName identifierAccesor, ImportRegistrationResolver importRegistrationResolver) {
    String typeName = target.getSimpleTypeName();
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (this == obj) {
    //    return true;
    // }
    bodyBuilder.appendFormalLine("if (this == " + OBJECT_NAME + ") {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("return true;");
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // // instanceof is false if the instance is null
    // if (!(obj instanceof Pet)) {
    //    return false;
    // }
    bodyBuilder.appendFormalLine("// instanceof is false if the instance is null");
    bodyBuilder.appendFormalLine("if (!(" + OBJECT_NAME + " instanceof " + typeName + ")) {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("return false;");
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // return getId() != null && Objects.equals(getId(), ((Pet) obj).getId());
    bodyBuilder.appendFormalLine(
        "return %1$s() != null && %2$s.equals(%1$s(), ((%3$s) %4$s).%1$s());", identifierAccesor,
        JavaType.OBJECTS.getNameIncludingTypeParameters(false, importRegistrationResolver),
        typeName, OBJECT_NAME);

    return bodyBuilder;
  }

  /**
   * Returns the `hasCode` method return statement for Jpa entities
   *
   * @return a {@link StringBuilder}
   */
  private static StringBuilder getJpaEntityHashCodeMethodReturnStatment() {
    return new StringBuilder("return 31;");
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
