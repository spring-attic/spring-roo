package org.springframework.roo.addon.web.mvc.controller.addon.responses.json;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
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
  private MethodMetadata createBatchMethod;
  private MethodMetadata updateBatchMethod;
  private MethodMetadata deleteBatchMethod;
  private List<MethodMetadata> finderMethods;
  private MethodMetadata populateHeadersMethod;

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
   * @param populateHeadersMethod MethodMetadata
   * @param methodMetadata3
   * @param methodMetadata2
   * @param methodMetadata
   * @param finderMethods List of MethodMetadata
   * @param readOnly boolean
  * @param type  Controller type
   */
  public JSONMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final MethodMetadata listMethod,
      final MethodMetadata createMethod, final MethodMetadata updateMethod,
      final MethodMetadata deleteMethod, final MethodMetadata showMethod,
      final MethodMetadata createBatchMethod, final MethodMetadata updateBatchMethod,
      final MethodMetadata deleteBatchMethod, final MethodMetadata populateHeadersMethod,
      final List<MethodMetadata> finderMethods, final boolean readOnly,
      final List<JavaType> typesToImport, final ControllerType type) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.readOnly = readOnly;
    this.listMethod = listMethod;
    this.createMethod = createMethod;
    this.updateMethod = updateMethod;
    this.deleteMethod = deleteMethod;
    this.showMethod = showMethod;
    this.createBatchMethod = createBatchMethod;
    this.updateBatchMethod = updateBatchMethod;
    this.deleteBatchMethod = deleteBatchMethod;
    this.finderMethods = finderMethods;
    this.populateHeadersMethod = populateHeadersMethod;

    if (type == ControllerType.COLLECTION) {
      ensureGovernorHasMethod(new MethodMetadataBuilder(listMethod));
    }

    if (!readOnly) {
      if (type == ControllerType.COLLECTION) {
        ensureGovernorHasMethod(new MethodMetadataBuilder(createMethod));
        ensureGovernorHasMethod(new MethodMetadataBuilder(createBatchMethod));
        ensureGovernorHasMethod(new MethodMetadataBuilder(updateBatchMethod));
        ensureGovernorHasMethod(new MethodMetadataBuilder(deleteBatchMethod));
        ensureGovernorHasMethod(new MethodMetadataBuilder(populateHeadersMethod));
      }
      if (type == ControllerType.ITEM) {
        ensureGovernorHasMethod(new MethodMetadataBuilder(updateMethod));
        ensureGovernorHasMethod(new MethodMetadataBuilder(deleteMethod));
      }

    }
    if (type == ControllerType.ITEM) {
      ensureGovernorHasMethod(new MethodMetadataBuilder(showMethod));
    }

    for (MethodMetadata finderMethod : finderMethods) {
      ensureGovernorHasMethod(new MethodMetadataBuilder(finderMethod));
    }

    // Add imports
    builder.getImportRegistrationResolver().addImports(typesToImport);

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * Method that returns list JSON method
   *
   * @return {@link MethodMetadata}
   */
  public MethodMetadata getListMethod() {
    return this.listMethod;
  }


  /**
   * Method that returns create JSON method
   *
   * @return {@link MethodMetadata}
   */
  public MethodMetadata getCreateMethod() {
    return this.createMethod;
  }

  /**
   * Method that returns update JSON method
   *
   * @return {@link MethodMetadata}
   */
  public MethodMetadata getUpdateMethod() {
    return this.updateMethod;
  }

  /**
   * Method that returns delete JSON method
   *
   * @return {@link MethodMetadata}
   */
  public MethodMetadata getDeleteMethod() {
    return this.deleteMethod;
  }

  /**
   * Method that returns delete batch JSON method
   *
   * @return {@link MethodMetadata}
   */
  public MethodMetadata getDeleteBatchMethod() {
    return this.deleteBatchMethod;
  }

  /**
   * Method that returns create batch JSON method
   *
   * @return {@link MethodMetadata}
   */
  public MethodMetadata getCreateBatchMethod() {
    return this.createBatchMethod;
  }

  /**
   * Method that returns update batch JSON method
   *
   * @return {@link MethodMetadata}
   */
  public MethodMetadata getUpdateBatchMethod() {
    return this.updateBatchMethod;
  }

  /**
   * Method that returns show JSON method
   *
   * @return {@link MethodMetadata}
   */
  public MethodMetadata getShowMethod() {
    return this.showMethod;
  }

  /**
   * Method that returns finder JSON methods
   *
   * @return {@link List<MethodMetadata>}
   */
  public List<MethodMetadata> getFinderMethods() {
    return this.finderMethods;
  }

  /**
   * Method that returns populateHeaders method
   *
   * @return {@link MethodMetadata}
   */
  public MethodMetadata getPopulateHeaders() {
    return this.populateHeadersMethod;
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
