package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooThymeleaf;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooThymeleaf}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class ThymeleafMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = ThymeleafMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private boolean readOnly;
  private MethodMetadata listFormMethod;
  private MethodMetadata listMethod;
  private MethodMetadata showMethod;
  private MethodMetadata createMethod;
  private MethodMetadata updateMethod;
  private MethodMetadata deleteMethod;

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
   * @param listFormMethod MethodMetadata
   * @param listMethod MethodMetadata 
   * @param createMethod MethodMetadata 
   * @param updateMethod MethodMetadata 
   * @param deleteMethod MethodMetadata 
   * @param showMethod MethodMetadata 
   * @param readOnly boolean 
   */
  public ThymeleafMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final MethodMetadata listFormMethod,
      final MethodMetadata listMethod, final MethodMetadata showMethod, boolean readOnly) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.readOnly = readOnly;
    this.listMethod = listMethod;
    this.showMethod = showMethod;
    this.listFormMethod = listFormMethod;

    // Adds list and list form method
    ensureGovernorHasMethod(new MethodMetadataBuilder(listFormMethod));
    // TODO
    //ensureGovernorHasMethod(new MethodMetadataBuilder(listMethod));

    // Adds show method
    ensureGovernorHasMethod(new MethodMetadataBuilder(showMethod));

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * Method that returns list JSON method
   * 
   * @return
   */
  public MethodMetadata getListFormMethod() {
    return this.listFormMethod;
  }

  /**
   * Method that returns list JSON method
   * 
   * @return
   */
  public MethodMetadata getListMethod() {
    return this.listMethod;
  }


  /**
   * Method that returns create JSON method
   * 
   * @return
   */
  public MethodMetadata getCreateMethod() {
    return this.createMethod;
  }

  /**
   * Method that returns update JSON method
   * 
   * @return
   */
  public MethodMetadata getUpdateMethod() {
    return this.updateMethod;
  }

  /**
   * Method that returns delete JSON method
   * 
   * @return
   */
  public MethodMetadata getDeleteMethod() {
    return this.deleteMethod;
  }

  /**
   * Method that returns show JSON method
   * 
   * @return
   */
  public MethodMetadata getShowMethod() {
    return this.showMethod;
  }

  /**
   * Method that returns if related entity is
   * readOnly or not.
   * 
   * @return
   */
  public boolean isReadOnly() {
    return this.readOnly;
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
