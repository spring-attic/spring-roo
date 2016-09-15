package org.springframework.roo.addon.dto.addon;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.addon.dto.annotations.RooEntityProjection;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Metadata for {@link RooEntityProjection}.
 * <p>
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class EntityProjectionMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = EntityProjectionMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private final List<FieldMetadata> fields;
  private final JavaSymbolName serialField = new JavaSymbolName("serialVersionUID");

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
  public EntityProjectionMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final EntityProjectionAnnotationValues annotationValues, final List<FieldMetadata> fields) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate
        .isTrue(
            isValid(identifier),
            "Metadata identification string '%s' does not appear to be a valid physical type identifier",
            identifier);

    this.fields = fields;

    ConstructorMetadataBuilder constructorMetadata = getConstructor();
    if (!constructorMetadata.getParameterTypes().isEmpty()) {
      ensureGovernorHasConstructor(constructorMetadata);
    }

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

    for (FieldMetadata field : fields) {

      // Add all fields excluding Serializable field
      if (!field.getFieldName().equals(serialField)) {
        String fieldName = field.getFieldName().getSymbolName();
        constructorBuilder.addParameter(fieldName, field.getFieldType());
        bodyBuilder.appendFormalLine(String.format("this.%s = %s;", fieldName, fieldName));
      }
    }

    constructorBuilder.setModifier(Modifier.PUBLIC);
    constructorBuilder.setBodyBuilder(bodyBuilder);

    return constructorBuilder;
  }
}
