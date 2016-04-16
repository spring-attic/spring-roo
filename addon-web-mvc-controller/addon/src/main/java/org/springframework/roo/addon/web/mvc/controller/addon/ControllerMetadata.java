package org.springframework.roo.addon.web.mvc.controller.addon;

import java.lang.reflect.Modifier;
import java.util.ArrayList;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.web.mvc.controller.annotations.RooController;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
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

  private ImportRegistrationResolver importResolver;
  private JavaType service;

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
   * @param path controllerPath
   */
  public ControllerMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, JavaType entity, JavaType service,
      String path) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.service = service;
    this.importResolver = builder.getImportRegistrationResolver();

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

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * This method returns service field included on controller
   * 
   * @param service
   * @return
   */
  public FieldMetadata getServiceField() {

    // Generating service field name
    String fieldName =
        this.service.getSimpleTypeName().substring(0, 1).toLowerCase()
            .concat(this.service.getSimpleTypeName().substring(1));

    return new FieldMetadataBuilder(getId(), Modifier.PUBLIC,
        new ArrayList<AnnotationMetadataBuilder>(), new JavaSymbolName(fieldName), this.service)
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
