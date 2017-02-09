package org.springframework.roo.addon.web.mvc.controller.addon.config;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.layers.service.addon.ServiceMetadata;
import org.springframework.roo.addon.web.mvc.controller.annotations.config.RooJsonMixin;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.model.SpringletsJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooJsonMixin}.
 *
 * @author Jose Manuel Vivó
 * @since 2.0
 */
public class EntityDeserializerMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final JavaType JSON_COMPONENT = new JavaType(
      "org.springframework.boot.jackson.JsonComponent");

  private static final String PROVIDES_TYPE_STRING = EntityDeserializerMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);
  private static final JavaType JSON_PARSER = new JavaType("com.fasterxml.jackson.core.JsonParser");
  private static final JavaType DESERIALIZATION_CONTEXT = new JavaType(
      "com.fasterxml.jackson.databind.DeserializationContext");
  private static final JavaType OBJECT_CODEC = new JavaType(
      "com.fasterxml.jackson.core.ObjectCodec");
  private static final JavaType JSON_NODE = new JavaType("com.fasterxml.jackson.databind.JsonNode");
  private static final JavaType IO_EXCEPTION = new JavaType("java.io.IOException");


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

  private final EntityDeserializerAnnotationValues annotationValues;
  private final JpaEntityMetadata entityMetadata;
  private ServiceMetadata serviceMetadata;
  private FieldMetadata serviceField;
  private FieldMetadata conversionServiceField;
  private ConstructorMetadata constructor;
  private MethodMetadata deserializeObjectMethod;

  /**
   * Constructor
   *
   * @param identifier the identifier for this item of metadata (required)
   * @param aspectName the Java type of the ITD (required)
   * @param governorPhysicalTypeMetadata the governor, which is expected to
   *            contain a {@link ClassOrInterfaceTypeDetails} (required)
   *
   */
  public EntityDeserializerMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      EntityDeserializerAnnotationValues annotationValues, JpaEntityMetadata entityMetadata,
      ServiceMetadata serviceMetadata) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.annotationValues = annotationValues;
    this.entityMetadata = entityMetadata;
    this.serviceMetadata = serviceMetadata;

    // Add annotation
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(JSON_COMPONENT));

    // extends JsonObjectDeserializer<Entity>
    /*
     * Moved to java as there were compilation problems when Mixin
     * uses @JsonDeserialize(using=EntityDeserializer.class) annotation
     * (requires extend of JsonDeseralizer)
     */
    //    ensureGovernorExtends(JavaType.wrapperOf(JSON_OBJECT_DESERIALIZER,
    //        entityMetadata.getDestination()));

    this.serviceField = getFieldFor(getId(), serviceMetadata.getDestination());
    ensureGovernorHasField(new FieldMetadataBuilder(serviceField));

    this.conversionServiceField = getFieldFor(getId(), SpringJavaType.CONVERSION_SERVICE);
    ensureGovernorHasField(new FieldMetadataBuilder(conversionServiceField));

    this.constructor = getConstructor(getId(), serviceField, conversionServiceField);
    ensureGovernorHasConstructor(new ConstructorMetadataBuilder(constructor));


    this.deserializeObjectMethod = getDeserializeMethod();
    ensureGovernorHasMethod(new MethodMetadataBuilder(this.deserializeObjectMethod));


    // Build the ITD
    itdTypeDetails = builder.build();
  }


  private MethodMetadata getDeserializeMethod() {
    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("deserializeObject");

    // Adding parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(JSON_PARSER));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(DESERIALIZATION_CONTEXT));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(OBJECT_CODEC));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(JSON_NODE));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Adding parameter names
    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("jsonParser"));
    parameterNames.add(new JavaSymbolName("context"));
    parameterNames.add(new JavaSymbolName("codec"));
    parameterNames.add(new JavaSymbolName("tree"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    final JavaType idType =
        getTypeToUseAsIdentifier(entityMetadata.getCurrentIndentifierField().getFieldType());

    // String idText = tree.asText();
    bodyBuilder.appendFormalLine("String idText = tree.asText();");

    // Long id = conversionService.convert(idText, Long.class);
    bodyBuilder.appendFormalLine("%s %s = %s.convert(idText, %s.class);",
        getNameOfJavaType(idType), entityMetadata.getCurrentIndentifierField().getFieldName(),
        conversionServiceField.getFieldName(), getNameOfJavaType(idType));

    final String entityItemName =
        StringUtils.uncapitalize(entityMetadata.getDestination().getSimpleTypeName());
    // Product product = productSercie.findOne(id);
    bodyBuilder.appendFormalLine("%s %s = %s.%s(%s);",
        getNameOfJavaType(entityMetadata.getDestination()), entityItemName,
        serviceField.getFieldName(), serviceMetadata.getCurrentFindOneMethod().getMethodName(),
        entityMetadata.getCurrentIndentifierField().getFieldName());

    // if (product == null) {
    bodyBuilder.appendFormalLine("if (%s == null) {", entityItemName);

    // throw new NotFoundException("Product not found");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("throw new %s(\"%s not found\");",
        getNameOfJavaType(SpringletsJavaType.SPRINGLETS_NOT_FOUND_EXCEPTION), entityMetadata
            .getDestination().getSimpleTypeName());
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");

    // return product;
    bodyBuilder.appendFormalLine("return %s;", entityItemName);


    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            entityMetadata.getDestination(), parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    methodBuilder.addThrowsType(IO_EXCEPTION);

    return methodBuilder.build();
  }

  private JavaType getTypeToUseAsIdentifier(JavaType type) {
    if (type.isPrimitive()) {
      if (JavaType.INT_PRIMITIVE.equals(type)) {
        return JavaType.INT_OBJECT;
      } else if (JavaType.LONG_PRIMITIVE.equals(type)) {
        return JavaType.LONG_OBJECT;
      } else if (JavaType.FLOAT_PRIMITIVE.equals(type)) {
        return JavaType.FLOAT_OBJECT;
      } else if (JavaType.DOUBLE_PRIMITIVE.equals(type)) {
        return JavaType.DOUBLE_OBJECT;
      }
    }
    return type;
  }

  /**
   * This method returns service field included on controller that it
   * represents the service spent as parameter
   *
   * @param service
   *            Searched service
   * @return The field that represents the service spent as parameter
   */
  public static FieldMetadata getFieldFor(String declaredByMetadataId, JavaType service) {

    // Generating service field name
    String fieldName =
        new JavaSymbolName(service.getSimpleTypeName()).getSymbolNameUnCapitalisedFirstLetter();

    return new FieldMetadataBuilder(declaredByMetadataId, Modifier.PRIVATE,
        new ArrayList<AnnotationMetadataBuilder>(), new JavaSymbolName(fieldName), service).build();
  }

  public static ConstructorMetadata getConstructor(String declaredByMetadataId,
      FieldMetadata serviceField, FieldMetadata conversionServiceField) {
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Generating constructor
    ConstructorMetadataBuilder constructor = new ConstructorMetadataBuilder(declaredByMetadataId);
    constructor.setModifier(Modifier.PUBLIC);
    constructor.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));

    // add Service to constructor
    String serviceFieldName = serviceField.getFieldName().getSymbolName();
    AnnotatedJavaType serviceParameter =
        new AnnotatedJavaType(serviceField.getFieldType(), new AnnotationMetadataBuilder(
            SpringJavaType.LAZY).build());
    constructor.addParameterName(serviceField.getFieldName());
    constructor.addParameterType(serviceParameter);

    // Generating body
    bodyBuilder.appendFormalLine("this.%1$s = %1$s;", serviceFieldName);

    // add Conversion service to constructor
    String conversionServiceFieldName = conversionServiceField.getFieldName().getSymbolName();
    constructor.addParameter(conversionServiceFieldName, conversionServiceField.getFieldType());

    // Generating body
    bodyBuilder.appendFormalLine("this.%1$s = %1$s;", conversionServiceFieldName);

    // Adding body
    constructor.setBodyBuilder(bodyBuilder);

    return constructor.build();

  }



  public JavaType getEntity() {
    return annotationValues.getEntity();
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
