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
import org.springframework.roo.classpath.scanner.MemberDetails;
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
  private final MemberDetails memberDetails;

  /**
   * Constructor
   * 
   * @param identifier
   * @param aspectName
   * @param governorPhysicalTypeMetadata
   * @param annotationValues
   */
  public ToStringMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final ToStringAnnotationValues annotationValues, MemberDetails memberDetails) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.isTrue(isValid(identifier),
        "Metadata identification string '%s' does not appear to be a valid", identifier);
    Validate.notNull(annotationValues, "Annotation values required");

    this.annotationValues = annotationValues;
    this.memberDetails = memberDetails;

    // Generate the toString() method
    builder.addMethod(getToStringMethod());

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
    final String toStringMethod = annotationValues.getToStringMethod();
    if (StringUtils.isBlank(toStringMethod)) {
      return null;
    }

    // Compute the relevant toString method name
    final JavaSymbolName methodName = new JavaSymbolName(toStringMethod);

    // See if the type itself declared the method
    if (governorHasMethod(methodName)) {
      return null;
    }

    // Get excludeFields attribute value
    final String[] excludeFields = annotationValues.getExcludeFields();

    //    builder.getImportRegistrationResolver().addImports(TO_STRING_BUILDER, TO_STRING_STYLE);
    List<FieldMetadata> affectedFields = new ArrayList<FieldMetadata>();
    for (FieldMetadata field : memberDetails.getFields()) {

      // Exclude non-common java. fields
      if (!field.getFieldType().getFullyQualifiedTypeName().startsWith("java.math")
          && !field.getFieldType().getFullyQualifiedTypeName().startsWith("java.lang")
          && !field.getFieldType().getFullyQualifiedTypeName().startsWith("java.util")) {
        continue;
      }

      // Exclude List/Set fields, even if they don't have relations
      if (field.getFieldType().equals(JdkJavaType.LIST)
          || field.getFieldType().equals(JdkJavaType.SET)
          || field.getFieldType().equals(JdkJavaType.MAP)) {
        continue;
      }

      // Exclude fields with relations from toString generation
      if (field.getAnnotation(JpaJavaType.MANY_TO_MANY) != null
          || field.getAnnotation(JpaJavaType.MANY_TO_ONE) != null
          || field.getAnnotation(JpaJavaType.ONE_TO_MANY) != null
          || field.getAnnotation(JpaJavaType.ONE_TO_ONE) != null) {
        continue;
      }

      // Check if field must be excluded manually by "excludeFields" attribute
      boolean exclude = false;
      if (excludeFields != null && excludeFields.length > 0) {

        for (int i = 0; i < excludeFields.length; i++) {
          if (excludeFields[i].equals(field.getFieldName().getSymbolName())) {
            exclude = true;
          }
        }
      }

      // Exclude field if necessary
      if (!exclude) {
        affectedFields.add(field);
      }
    }

    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Build toString method body
    // return "Entity {" + "fieldName1='" + fieldName1 + '\'' + ", fieldName2='" + fieldName2 + '\''+ "}" + super.toString();
    bodyBuilder.appendFormalLine(String.format("return \"%s {\" + ", governorTypeDetails.getType()
        .getSimpleTypeName()));
    for (int i = 0; i < affectedFields.size(); i++) {
      bodyBuilder.appendIndent();
      StringBuffer fieldString = new StringBuffer();
      fieldString.append("\"");
      if (i != 0) {
        fieldString.append(", ");
      }
      fieldString.append(affectedFields.get(i).getFieldName()).append("='\"").append(" + ")
          .append(affectedFields.get(i).getFieldName()).append(" + '\\''").append(" + ");
      if (i == affectedFields.size() - 1) {
        fieldString.append("\"}\" + ").append("super.toString();");
      }

      // Append next field line
      bodyBuilder.appendFormalLine(fieldString.toString());
    }

    return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, STRING, bodyBuilder);
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
