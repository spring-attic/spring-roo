package org.springframework.roo.addon.web.mvc.controller.addon;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.web.mvc.controller.annotations.RooController;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooController}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class ControllerMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = ControllerMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private static final JavaType CONTROLLER_ANNOTATION = new JavaType(
      "org.springframework.stereotype.Controller");
  private static final JavaType REQUEST_MAPPING_ANNOTATION = new JavaType(
      "org.springframework.web.bind.annotation.RequestMapping");

  private JavaType service;
  private List<JavaType> detailsServices;

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
   * @param entity Javatype with entity managed by controller
   * @param service JavaType with service used by controller
   * @param detailsServices List with all services of every field that will be used
   *        as detail
   * @param path controllerPath
   */
  public ControllerMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final JavaType entity,
      final JavaType service, final List<JavaType> detailsServices, final String path) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.service = service;
    this.detailsServices = detailsServices;

    // Add @Controller annotation
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(CONTROLLER_ANNOTATION));

    // Add @RequestMapping annotation
    AnnotationMetadataBuilder requesMappingAnnotation =
        new AnnotationMetadataBuilder(REQUEST_MAPPING_ANNOTATION);
    requesMappingAnnotation.addStringAttribute("value", path);
    ensureGovernorIsAnnotated(requesMappingAnnotation);

    // Adding service reference
    FieldMetadata serviceField = getServiceField();
    ensureGovernorHasField(new FieldMetadataBuilder(serviceField));

    // Adding services of every fields that will be used as detail
    for (JavaType detailService : detailsServices) {
      FieldMetadata detailServiceField = getServiceDetailField(detailService);
      ensureGovernorHasField(new FieldMetadataBuilder(detailServiceField));
    }

    // Adding constructor
    ConstructorMetadata constructor = getConstructor();
    ensureGovernorHasConstructor(new ConstructorMetadataBuilder(constructor));

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * This method returns the controller constructor 
   * 
   * @return
   */
  public ConstructorMetadata getConstructor() {

    // Getting serviceFieldName
    String serviceFieldName = getServiceField().getFieldName().getSymbolName();

    // Generating constructor
    ConstructorMetadataBuilder constructor = new ConstructorMetadataBuilder(getId());
    constructor.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));
    constructor.addParameter(serviceFieldName, service);
    for (JavaType detailService : detailsServices) {
      constructor.addParameter(getServiceDetailField(detailService).getFieldName().getSymbolName(),
          detailService);
    }
    constructor.setModifier(Modifier.PUBLIC);

    // Adding body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder
        .appendFormalLine(String.format("this.%s = %s;", serviceFieldName, serviceFieldName));
    for (JavaType detailService : detailsServices) {
      bodyBuilder.appendFormalLine(String.format("this.%s = %s;",
          getServiceDetailField(detailService).getFieldName().getSymbolName(),
          getServiceDetailField(detailService).getFieldName().getSymbolName()));
    }
    constructor.setBodyBuilder(bodyBuilder);

    return constructor.build();
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
   * This method returns service field included on controller
   * 
   * @param service
   * @return
   */
  public FieldMetadata getServiceDetailField(JavaType detailService) {

    // Generating service field name
    String fieldName =
        new JavaSymbolName(detailService.getSimpleTypeName())
            .getSymbolNameUnCapitalisedFirstLetter();

    return new FieldMetadataBuilder(getId(), Modifier.PUBLIC,
        new ArrayList<AnnotationMetadataBuilder>(), new JavaSymbolName(fieldName), detailService)
        .build();
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
