package org.springframework.roo.addon.web.mvc.controller.addon.responses.json;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.web.mvc.controller.annotations.responses.json.RooJSON;
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
 * Metadata for {@link RooJSON}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class JSONMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = JSONMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private boolean readOnly;
  private MethodMetadata listMethod;
  private MethodMetadata showMethod;
  private MethodMetadata createMethod;
  private MethodMetadata updateMethod;
  private MethodMetadata deleteMethod;
  private List<MethodMetadata> finderMethods;

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
   * @param listMethod MethodMetadata 
   * @param createMethod MethodMetadata 
   * @param updateMethod MethodMetadata 
   * @param deleteMethod MethodMetadata 
   * @param showMethod MethodMetadata 
   * @param finderMethods List of MethodMetadata
   * @param readOnly boolean 
   */
  public JSONMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final MethodMetadata listMethod,
      final MethodMetadata createMethod, final MethodMetadata updateMethod,
      final MethodMetadata deleteMethod, final MethodMetadata showMethod,
      List<MethodMetadata> finderMethods, boolean readOnly) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.readOnly = readOnly;
    this.listMethod = listMethod;
    this.createMethod = createMethod;
    this.updateMethod = updateMethod;
    this.deleteMethod = deleteMethod;
    this.showMethod = showMethod;
    this.finderMethods = finderMethods;

    ensureGovernorHasMethod(new MethodMetadataBuilder(listMethod));
    if (!readOnly) {
      ensureGovernorHasMethod(new MethodMetadataBuilder(createMethod));
      ensureGovernorHasMethod(new MethodMetadataBuilder(updateMethod));
      ensureGovernorHasMethod(new MethodMetadataBuilder(deleteMethod));
    }
    ensureGovernorHasMethod(new MethodMetadataBuilder(showMethod));

    for (MethodMetadata finderMethod : finderMethods) {
      ensureGovernorHasMethod(new MethodMetadataBuilder(finderMethod));
    }

    // Build the ITD
    itdTypeDetails = builder.build();
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
   * Method that returns finder JSON methods
   * 
   * @return
   */
  public List<MethodMetadata> getFinderMethods() {
    return this.finderMethods;
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
