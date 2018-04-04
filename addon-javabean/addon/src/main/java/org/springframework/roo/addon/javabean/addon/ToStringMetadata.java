package org.springframework.roo.addon.javabean.addon;

import static org.springframework.roo.model.JavaType.STRING;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.javabean.annotations.RooToString;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Metadata for {@link RooToString}.
 *
 * @author Ben Alex
 * @author Jose Manuel Vivó Arnal
 * @since 1.0
 */
public class ToStringMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = ToStringMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

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

  private final ToStringAnnotationValues annotationValues;
  private final List<FieldMetadata> fields;

  /**
   * Constructor
   *
   * @param identifier
   * @param aspectName
   * @param governorPhysicalTypeMetadata
   * @param annotationValues
   * @param fields
   * @param hasJavaBeanAnnotation
   */
  public ToStringMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final ToStringAnnotationValues annotationValues, List<FieldMetadata> fields,
      boolean hasJavaBeanAnnotation) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.isTrue(isValid(identifier),
        "Metadata identification string '%s' does not appear to be a valid", identifier);
    Validate.notNull(annotationValues, "Annotation values required");

    this.annotationValues = annotationValues;
    this.fields = fields;

    if (!hasJavaBeanAnnotation && !fields.isEmpty()) {
      // Generate the toString() method
      builder.addMethod(getToStringMethod());
    }

    // Create a representation of the desired output ITD
    itdTypeDetails = builder.build();
  }

  /**
   * Obtains the "toString" method for this type, if available.
   * <p>
   * If the user provided a non-default name for "toString", that method will
   * be returned.
   *
   * @return the "toString" method declared on this type or that will be
   *         introduced (or null if undeclared and not introduced)
   */
  private MethodMetadataBuilder getToStringMethod() {
    if (StringUtils.isBlank(annotationValues.getToStringMethod())) {
      return null;
    }

    // Compute the relevant toString method name
    final JavaSymbolName methodName = new JavaSymbolName(annotationValues.getToStringMethod());

    // See if the type itself declared the method
    if (governorHasMethod(methodName)) {
      return null;
    }

    return generateToStringMethod(getId(), governorTypeDetails.getType(), annotationValues, fields);
  }

  /**
   * Generates toString method
   *
   * @param metadataId
   * @param target
   * @param annotationValues
   * @param fields
   * @return
   */
  protected static MethodMetadataBuilder generateToStringMethod(String metadataId, JavaType target,
      ToStringAnnotationValues annotationValues, List<FieldMetadata> fields) {
    final JavaSymbolName methodName = new JavaSymbolName(annotationValues.getToStringMethod());
    // Get excludeFields attribute value
    final String[] excludeFields = annotationValues.getExcludeFields();

    // Get all fields from class
    List<FieldMetadata> affectedFields = new ArrayList<FieldMetadata>();
    for (FieldMetadata field : fields) {


      // Exclude field if necessary
      // Exclude non-common java. fields
      // Exclude static fields
      // Exclude List/Set fields, even if they don't have relations
      // Exclude fields with relations from toString generation
      // Check if field must be excluded manually by "excludeFields" attribute
      if (Modifier.isStatic(field.getModifier()) || !isCommonJavaField(field)
          || isCollectionField(field) || isRelationField(field) || isExcluded(excludeFields, field)) {
        continue;
      }
      affectedFields.add(field);
    }

    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Build toString method body
    // return "Entity {" + "fieldName1='" + fieldName1 + '\'' + ", fieldName2='" + fieldName2 + '\''+ "}" + super.toString();
    bodyBuilder.appendFormalLine(String.format("return \"%s {\" + ", target.getSimpleTypeName()));
    for (int i = 0; i < affectedFields.size(); i++) {
      bodyBuilder.appendIndent();
      StringBuilder fieldString = new StringBuilder();
      fieldString.append("\"");
      if (i != 0) {
        fieldString.append(", ");
      }
      FieldMetadata fieldMetadata = affectedFields.get(i);
      String fieldName = fieldMetadata.getFieldName().getSymbolName();

      String fieldValue = fieldName;
      if (isDateField(fieldMetadata)) {
        fieldValue =
            fieldName + " == null ? null : java.text.DateFormat.getDateTimeInstance().format("
                + fieldName + ")";
      } else if (isCalendarField(fieldMetadata)) {
        fieldValue =
            fieldName + " == null ? null : java.text.DateFormat.getDateTimeInstance().format("
                + fieldName + ".getTime())";
      }

      fieldString.append(fieldName).append("='\"").append(" + ").append(fieldValue)
          .append(" + '\\''").append(" + ");
      if (i == affectedFields.size() - 1) {
        fieldString.append("\"}\" + ").append("super.toString();");
      }

      // Append next field line
      bodyBuilder.appendFormalLine(fieldString.toString());
    }
    if (affectedFields.isEmpty()) {
      bodyBuilder.appendFormalLine("\"}\" + super.toString();");
    }

    return new MethodMetadataBuilder(metadataId, Modifier.PUBLIC, methodName, STRING, bodyBuilder);
  }

  private static boolean isDateField(FieldMetadata fieldMetadata) {
    return JdkJavaType.DATE.equals(fieldMetadata.getFieldType());
  }

  private static boolean isCalendarField(FieldMetadata fieldMetadata) {
    return JdkJavaType.CALENDAR.equals(fieldMetadata.getFieldType());
  }

  /**
   * Check if a field is a common java field (from packages java.math, java.lang, java.util)
   *
   * @param field
   * @return
   */
  protected static boolean isCommonJavaField(FieldMetadata field) {
    return field.getFieldType().getFullyQualifiedTypeName().startsWith("java.math")
        || field.getFieldType().getFullyQualifiedTypeName().startsWith("java.lang")
        || field.getFieldType().getFullyQualifiedTypeName().startsWith("java.util");
  }

  /**
   * Check if a field is excluded
   *
   * @param excludeFields
   * @param field
   * @return
   */
  protected static boolean isExcluded(final String[] excludeFields, FieldMetadata field) {
    if (excludeFields != null && excludeFields.length > 0) {
      final String symbolName = field.getFieldName().getSymbolName();
      for (String excluded : excludeFields) {
        if (symbolName.equals(excluded)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Check if field is a collection field
   *
   * @param field
   * @return
   */
  protected static boolean isCollectionField(FieldMetadata field) {
    return field.getFieldType().equals(JdkJavaType.LIST)
        || field.getFieldType().equals(JdkJavaType.SET)
        || field.getFieldType().equals(JdkJavaType.MAP);
  }

  /**
   * Check if field is a relation field
   *
   * @param field
   * @return
   */
  protected static boolean isRelationField(FieldMetadata field) {
    return field.getAnnotation(JpaJavaType.MANY_TO_MANY) != null
        || field.getAnnotation(JpaJavaType.MANY_TO_ONE) != null
        || field.getAnnotation(JpaJavaType.ONE_TO_MANY) != null
        || field.getAnnotation(JpaJavaType.ONE_TO_ONE) != null;
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
