package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooThymeleafMainController;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooThymeleafMainController}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class ThymeleafMainControllerMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = ThymeleafMainControllerMetadata.class
      .getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private static final JavaType CONTROLLER_ANNOTATION = new JavaType(
      "org.springframework.stereotype.Controller");

  private ImportRegistrationResolver importResolver;
  private MethodMetadata indexMethod;

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
   * @param indexMethod MethodMetadata
   * @param typesToImport List<JavaType>
   */
  public ThymeleafMainControllerMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, MethodMetadata indexMethod,
      final List<JavaType> typesToImport) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.importResolver = builder.getImportRegistrationResolver();
    this.indexMethod = indexMethod;

    // Add @Controller annotation
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(CONTROLLER_ANNOTATION));

    // Add index method
    ensureGovernorHasMethod(new MethodMetadataBuilder(indexMethod));

    // Adding all necessary types to import
    importResolver.addImports(typesToImport);

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * This method returns the index method of Thymeleaf 
   * main controller
   * 
   * @return
   */
  public MethodMetadata getIndexMethod() {
    return this.indexMethod;
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
