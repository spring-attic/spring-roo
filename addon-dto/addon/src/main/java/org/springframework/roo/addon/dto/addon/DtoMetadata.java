package org.springframework.roo.addon.dto.addon;

import java.lang.reflect.Modifier;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.*;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooDTO}.
 * <p>
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class DtoMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = DtoMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private final DtoAnnotationValues annotationValues;
  private final List<FieldMetadata> fields;
  private final JavaSymbolName serialField = new JavaSymbolName("serialVersionUID");
  private final JavaType type;

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

  /**
   * Constructor
   * 
   * @param identifier
   * @param aspectName
   * @param governorPhysicalTypeMetadata
   */
  public DtoMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final DtoAnnotationValues annotationValues, List<FieldMetadata> fields) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate
        .isTrue(
            isValid(identifier),
            "Metadata identification string '%s' does not appear to be a valid physical type identifier",
            identifier);

    this.annotationValues = annotationValues;
    this.fields = fields;
    this.type = governorPhysicalTypeMetadata.getType();

    if (annotationValues.getImmutable()) {
      ConstructorMetadataBuilder constructorMetadata = getConstructor();
      if (!constructorMetadata.getParameterTypes().isEmpty()) {
        ensureGovernorHasConstructor(constructorMetadata);
      }
    }

    // ROO-3868: New entity visualization support using a new format annotation
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(getEntityFormatAnnotation()));

    // Build ITD
    itdTypeDetails = builder.build();
  }

  /**
   * Builds constructor for initializing <code>final</code> fields.
   * 
   * @return ConstructorMetadataBuilder for adding constructor to ITD
   */
  private ConstructorMetadataBuilder getConstructor() {
    ConstructorMetadataBuilder constructorBuilder = new ConstructorMetadataBuilder(getId());
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    if (annotationValues.getImmutable() == true) {
      for (FieldMetadata field : fields) {

        // Add all fields excluding Serializable field
        if (!field.getFieldName().equals(serialField)) {
          String fieldName = field.getFieldName().getSymbolName();
          constructorBuilder.addParameter(fieldName, field.getFieldType());
          bodyBuilder.appendFormalLine(String.format("this.%s = %s;", fieldName, fieldName));
        }
      }
    }

    constructorBuilder.setModifier(Modifier.PUBLIC);
    constructorBuilder.setBodyBuilder(bodyBuilder);

    return constructorBuilder;
  }

  /**
   * Generates the Springlets `@EntityFormat` annotation to be applied to the dto
   *
   * @return AnnotationMetadata
   */
  private AnnotationMetadata getEntityFormatAnnotation() {
    String expressionAttribute = this.annotationValues.getFormatExpression();
    String messageAttribute = this.annotationValues.getFormatMessage();

    final AnnotationMetadataBuilder entityFormatBuilder =
        new AnnotationMetadataBuilder(SpringletsJavaType.SPRINGLETS_ENTITY_FORMAT);

    // Check for each attribute individually
    if (StringUtils.isNotBlank(expressionAttribute)) {
      entityFormatBuilder.addStringAttribute("value", expressionAttribute);

    }
    if (StringUtils.isNotBlank(messageAttribute)) {
      entityFormatBuilder.addStringAttribute("message", messageAttribute);
    }

    return entityFormatBuilder.build();
  }

}
