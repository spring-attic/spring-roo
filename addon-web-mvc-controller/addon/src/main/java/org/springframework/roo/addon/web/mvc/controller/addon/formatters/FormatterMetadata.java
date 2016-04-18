package org.springframework.roo.addon.web.mvc.controller.addon.formatters;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.web.mvc.controller.annotations.formatters.RooFormatter;
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
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooFormatter}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class FormatterMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = FormatterMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private ImportRegistrationResolver importResolver;
  private JavaType entity;
  private JavaType identifierType;
  private JavaType service;
  private FieldMetadata serviceField;
  private FieldMetadata conversionServiceField;
  private List<MethodMetadata> entityAccessors;

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
   * @param identifier the identifier for this item of metadata (required)
   * @param aspectName the Java type of the ITD (required)
   * @param governorPhysicalTypeMetadata the governor, which is expected to
   *            contain a {@link ClassOrInterfaceTypeDetails} (required)
   * @param entity Javatype with entity
   * @param identifierType JavaType with the identifier type of provided entity
   * @param entityAccessors List with all accessors of declared entity
   * @param service JavaType with service
   */
  public FormatterMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, JavaType entity,
      JavaType identifierType, List<MethodMetadata> entityAccessors, JavaType service) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.entity = entity;
    this.identifierType = identifierType;
    this.entityAccessors = entityAccessors;
    this.service = service;
    this.importResolver = builder.getImportRegistrationResolver();

    // Adding implements
    ensureGovernorImplements(new JavaType(SpringJavaType.FORMATTER.getFullyQualifiedTypeName(), 0,
        DataType.TYPE, null, Arrays.asList(entity)));

    // Adding service reference
    this.serviceField = getServiceField();
    ensureGovernorHasField(new FieldMetadataBuilder(serviceField));

    // Adding conversionService field
    this.conversionServiceField = getConversionServiceField();
    ensureGovernorHasField(new FieldMetadataBuilder(conversionServiceField));

    // Adding formatter constructor
    ensureGovernorHasConstructor(new ConstructorMetadataBuilder(getConstructor()));

    // Add parse() method
    ensureGovernorHasMethod(new MethodMetadataBuilder(getParseMethod()));

    // Add print() method
    ensureGovernorHasMethod(new MethodMetadataBuilder(getPrintMethod()));

    // Build the ITD
    itdTypeDetails = builder.build();
  }


  /**
   * This method returns the formatter constructor
   * @return
   */
  public ConstructorMetadata getConstructor() {

    FieldMetadata serviceField = getServiceField();
    FieldMetadata conversionServiceField = getConversionServiceField();

    ConstructorMetadataBuilder constructor = new ConstructorMetadataBuilder(getId());
    constructor.addParameter(serviceField.getFieldName().getSymbolName(),
        serviceField.getFieldType());
    constructor.addParameter(conversionServiceField.getFieldName().getSymbolName(),
        conversionServiceField.getFieldType());
    constructor.setModifier(Modifier.PUBLIC);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // this.serviceField = serviceField;
    bodyBuilder.appendFormalLine(String.format("this.%s = %s;", serviceField.getFieldName(),
        serviceField.getFieldName()));

    // this.conversionServiceField = conversionServiceField;
    bodyBuilder.appendFormalLine(String.format("this.%s = %s;",
        conversionServiceField.getFieldName(), conversionServiceField.getFieldName()));

    constructor.setBodyBuilder(bodyBuilder);

    return constructor.build();
  }

  /**
   * This method returns the parse method necessary on generated
   * formatters
   * 
   * @return
   */
  public MethodMetadata getParseMethod() {
    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(JavaType.STRING));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(new JavaType("java.util.Locale")));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("text"));
    parameterNames.add(new JavaSymbolName("locale"));

    JavaSymbolName methodName = new JavaSymbolName("parse");

    // Check if method exists
    if (governorHasMethod(methodName,
        AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes))) {
      return getGovernorMethod(methodName,
          AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    }

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (text == null || !StringUtils.hasText(text)) {
    bodyBuilder.appendFormalLine(String.format("if (text == null || !%s.hasText(text)) {",
        new JavaType("org.springframework.util.StringUtils").getNameIncludingTypeParameters(false,
            importResolver)));

    //  return null;
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("return null;");

    // }
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // Type id = conversionService.convert(text, Type.class);
    bodyBuilder.appendFormalLine(String.format("%s id = %s.convert(text, %s.class);",
        this.identifierType.getNameIncludingTypeParameters(false, importResolver),
        this.conversionServiceField.getFieldName(),
        this.identifierType.getNameIncludingTypeParameters(false, importResolver)));

    // return serviceField.findOne(id);
    bodyBuilder.appendFormalLine(String.format("return %s.findOne(id);",
        this.serviceField.getFieldName()));


    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, this.entity,
            parameterTypes, parameterNames, bodyBuilder);

    methodBuilder.addThrowsType(new JavaType("java.text.ParseException"));

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * This method returns the print method necessary on generated
   * formatters
   * 
   * @return
   */
  public MethodMetadata getPrintMethod() {
    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(this.entity));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(new JavaType("java.util.Locale")));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    JavaSymbolName entityFieldName =
        new JavaSymbolName(StringUtils.uncapitalize(this.entity.getSimpleTypeName()));
    parameterNames.add(entityFieldName);
    parameterNames.add(new JavaSymbolName("locale"));

    JavaSymbolName methodName = new JavaSymbolName("print");

    // Check if method exists
    if (governorHasMethod(methodName,
        AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes))) {
      return getGovernorMethod(methodName,
          AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    }

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return entityFieldName == null ? null : shipper.getCompanyName();
    String accessors = "";
    int count = 0;
    for (MethodMetadata accessor : entityAccessors) {
      accessors =
          accessors.concat(entityFieldName.getSymbolName()).concat(".")
              .concat(accessor.getMethodName().getSymbolName()).concat("()")
              .concat(").append(\" - \").append(");

      // We are going to include the first 5 accessors only
      if (count == 4) {
        break;
      }
      count++;
    }
    // Removing extra .concat()
    if (!StringUtils.isBlank(accessors)) {
      accessors =
          "new StringBuilder().append(".concat(accessors.substring(0, accessors.length() - 22))
              .concat(".toString()");
      bodyBuilder.appendFormalLine(String.format("return %s == null ? null : %s;", entityFieldName,
          accessors));
    } else {
      bodyBuilder.appendFormalLine(String.format("return %s.toString();", entityFieldName));
    }


    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING,
            parameterTypes, parameterNames, bodyBuilder);

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * This method returns service field included on controller
   * 
   * @return
   */
  public FieldMetadata getServiceField() {

    // Generating service field name
    String fieldName =
        new JavaSymbolName(this.service.getSimpleTypeName())
            .getSymbolNameUnCapitalisedFirstLetter();

    return new FieldMetadataBuilder(getId(), Modifier.PUBLIC,
        new ArrayList<AnnotationMetadataBuilder>(), new JavaSymbolName(fieldName), this.service)
        .build();
  }

  /**
   * This method returns conversionSevice field included on controller
   * 
   * @return
   */
  public FieldMetadata getConversionServiceField() {

    return new FieldMetadataBuilder(getId(), Modifier.PUBLIC,
        new ArrayList<AnnotationMetadataBuilder>(), new JavaSymbolName("conversionService"),
        SpringJavaType.CONVERSION_SERVICE).build();
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
