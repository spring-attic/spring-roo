package org.springframework.roo.addon.web.mvc.controller.addon.config;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.web.mvc.controller.annotations.config.RooJsonMixin;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Metadata for {@link RooJsonMixin}.
 *
 * @author Jose Manuel Vivó
 * @since 2.0
 */
public class JSONMixinMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final JavaType JSON_DESERIALIZE = new JavaType(
      "com.fasterxml.jackson.databind.annotation.JsonDeserialize");
  private static final JavaType JSON_IDENTITY_INFO = new JavaType(
      "com.fasterxml.jackson.annotation.JsonIdentityInfo");
  private static final JavaType JSON_IGNORE = new JavaType(
      "com.fasterxml.jackson.annotation.JsonIgnore");
  private static final JavaType OBJECT_ID_GENERATORS_PROPERTY_GENERATOR = new JavaType(
      "com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator");

  private static final String PROVIDES_TYPE_STRING = JSONMixinMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);


  public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
    return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
  }

  public static String createIdentifier(ClassOrInterfaceTypeDetails details) {
    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(details.getDeclaredByMetadataId());
    return createIdentifier(details.getType(), logicalPath);
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

  private final JSONMixinAnnotationValues annotationValues;
  private final JpaEntityMetadata entityMetadata;
  private JavaSymbolName propertyIdGenerator;
  private final Map<FieldMetadata, JavaType> jsonDeserializerByField;

  /**
   * Constructor
   *
   * @param identifier the identifier for this item of metadata (required)
   * @param aspectName the Java type of the ITD (required)
   * @param governorPhysicalTypeMetadata the governor, which is expected to
   *            contain a {@link ClassOrInterfaceTypeDetails} (required)
   * @param annotationValues
   * @param entityMetadata
   * @param jsonDeserializerByEntity
   */
  public JSONMixinMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      JSONMixinAnnotationValues annotationValues, JpaEntityMetadata entityMetadata,
      Map<FieldMetadata, JavaType> jsonDeserializerByField) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.annotationValues = annotationValues;
    this.entityMetadata = entityMetadata;
    this.jsonDeserializerByField = jsonDeserializerByField;

    // Add @JsonIdentityInfo if has @OneToOne relation
    for (RelationInfo info : entityMetadata.getRelationInfos().values()) {
      if (info.cardinality == Cardinality.ONE_TO_ONE) {
        this.propertyIdGenerator = entityMetadata.getCurrentIndentifierField().getFieldName();
        AnnotationMetadataBuilder identifyInfo = new AnnotationMetadataBuilder(JSON_IDENTITY_INFO);
        identifyInfo.addClassAttribute("generator", OBJECT_ID_GENERATORS_PROPERTY_GENERATOR);
        identifyInfo.addStringAttribute("property", this.propertyIdGenerator.getSymbolName());
        ensureGovernorIsAnnotated(identifyInfo);
        break;
      }
    }

    // Add ignore properties
    for (RelationInfo info : entityMetadata.getRelationInfos().values()) {
      if (info.cardinality == Cardinality.ONE_TO_MANY
          || info.cardinality == Cardinality.MANY_TO_MANY) {
        ensureGovernorHasField(getIgnoreFieldFor(info.fieldMetadata));
      }
    }

    // Add deserializers
    for (Entry<FieldMetadata, JavaType> entry : jsonDeserializerByField.entrySet()) {
      ensureGovernorHasField(getDeseralizerFieldFor(entry.getKey(), entry.getValue()));
    }

    // Build the ITD
    itdTypeDetails = builder.build();
  }


  private FieldMetadataBuilder getDeseralizerFieldFor(FieldMetadata field, JavaType deserializer) {
    AnnotationMetadataBuilder annotation = new AnnotationMetadataBuilder(JSON_DESERIALIZE);
    annotation.addClassAttribute("using", deserializer);

    return new FieldMetadataBuilder(getId(), Modifier.PRIVATE, Arrays.asList(annotation),
        field.getFieldName(), field.getFieldType());
  }

  private FieldMetadataBuilder getIgnoreFieldFor(FieldMetadata field) {
    return new FieldMetadataBuilder(getId(), Modifier.PRIVATE,
        Arrays.asList(new AnnotationMetadataBuilder(JSON_IGNORE)), field.getFieldName(),
        field.getFieldType());
  }

  public JavaType getEntity() {
    return annotationValues.getEntity();
  }

  public JavaSymbolName getPropertyIdGenerator() {
    return propertyIdGenerator;
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
