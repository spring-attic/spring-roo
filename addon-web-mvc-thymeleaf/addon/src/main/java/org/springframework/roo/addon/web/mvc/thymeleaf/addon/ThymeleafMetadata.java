package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooThymeleaf;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.ImportRegistrationResolver;
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

  private ImportRegistrationResolver importResolver;

  private boolean readOnly;
  private MethodMetadata listFormMethod;
  private MethodMetadata listJSONMethod;
  private MethodMetadata listDatatablesJSONMethod;
  private MethodMetadata createFormMethod;
  private MethodMetadata createMethod;
  private MethodMetadata editFormMethod;
  private MethodMetadata updateMethod;
  private MethodMetadata deleteMethod;
  private MethodMetadata deleteJSONMethod;
  private MethodMetadata showMethod;
  private MethodMetadata populateFormMethod;
  private MethodMetadata populateFormatMethods;
  private List<MethodMetadata> detailsMethods;

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
   * @param listJSONMethod MethodMetadata 
   * @param listDatatablesJSONMethod MethodMetadata 
   * @param createFormMethod MethodMetadata
   * @param createMethod MethodMetadata 
   * @param editFormMethod MethodMetadata
   * @param updateMethod MethodMetadata 
   * @param deleteMethod MethodMetadata 
   * @param deleteJSONMethod MethodMetadata 
   * @param showMethod MethodMetadata 
   * @param detailsMetdhos List<MethodMetadata>
   * @param populateFormMethod MethodMetadata
   * @param populateFormatMethods MethodMetadata
   * @param readOnly boolean 
   * @param typesToImport List<JavaType>
   */
  public ThymeleafMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final MethodMetadata listFormMethod,
      final MethodMetadata listJSONMethod, final MethodMetadata listDatatablesJSONMethod,
      final MethodMetadata createFormMethod, final MethodMetadata createMethod,
      final MethodMetadata editFormMethod, final MethodMetadata updateMethod,
      final MethodMetadata deleteMethod, final MethodMetadata deleteJSONMethod,
      final MethodMetadata showMethod, final List<MethodMetadata> detailsMethods,
      final MethodMetadata populateFormMethod, final MethodMetadata populateFormatsMethod,
      final boolean readOnly, final List<JavaType> typesToImport) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.importResolver = builder.getImportRegistrationResolver();

    this.readOnly = readOnly;
    this.listFormMethod = listFormMethod;
    this.listJSONMethod = listJSONMethod;
    this.listDatatablesJSONMethod = listDatatablesJSONMethod;
    this.createFormMethod = createFormMethod;
    this.createMethod = createMethod;
    this.editFormMethod = editFormMethod;
    this.updateMethod = updateMethod;
    this.deleteMethod = deleteMethod;
    this.deleteJSONMethod = deleteJSONMethod;
    this.showMethod = showMethod;
    this.detailsMethods = detailsMethods;
    this.populateFormMethod = populateFormMethod;
    this.populateFormatMethods = populateFormatsMethod;

    // Adds list and list form method
    ensureGovernorHasMethod(new MethodMetadataBuilder(listFormMethod));
    ensureGovernorHasMethod(new MethodMetadataBuilder(listJSONMethod));
    ensureGovernorHasMethod(new MethodMetadataBuilder(listDatatablesJSONMethod));

    // Include CUD methods only if provided entity is not a readOnly entity
    if (!readOnly) {
      ensureGovernorHasMethod(new MethodMetadataBuilder(createFormMethod));
      ensureGovernorHasMethod(new MethodMetadataBuilder(createMethod));
      ensureGovernorHasMethod(new MethodMetadataBuilder(editFormMethod));
      ensureGovernorHasMethod(new MethodMetadataBuilder(updateMethod));
      ensureGovernorHasMethod(new MethodMetadataBuilder(deleteMethod));
      ensureGovernorHasMethod(new MethodMetadataBuilder(deleteJSONMethod));
    }

    // Adds show method
    ensureGovernorHasMethod(new MethodMetadataBuilder(showMethod));

    // Adds details methods
    for (MethodMetadata detailMethod : detailsMethods) {
      ensureGovernorHasMethod(new MethodMetadataBuilder(detailMethod));
    }

    // Always add populateForm method
    ensureGovernorHasMethod(new MethodMetadataBuilder(populateFormMethod));

    // Always add addDateTimeFormatPatterns method
    ensureGovernorHasMethod(new MethodMetadataBuilder(populateFormatsMethod));

    // Adding all necessary types to import
    importResolver.addImports(typesToImport);

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * Method that returns list Form method
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
  public MethodMetadata getListJSONMethod() {
    return this.listJSONMethod;
  }

  /**
   * Method that returns list Datatables JSON method
   * 
   * @return
   */
  public MethodMetadata getListDatatablesJSONMethod() {
    return this.listDatatablesJSONMethod;
  }

  /**
   * Method that returns create form Thymeleaf method
   * 
   * @return
   */
  public MethodMetadata getCreateFormMethod() {
    return this.createFormMethod;
  }

  /**
   * Method that returns create Thymeleaf method
   * 
   * @return
   */
  public MethodMetadata getCreateMethod() {
    return this.createMethod;
  }

  /**
   * Method that returns edit form Thymeleaf method
   * 
   * @return
   */
  public MethodMetadata getEditForm() {
    return this.editFormMethod;
  }

  /**
   * Method that returns update Thymeleaf method
   * 
   * @return
   */
  public MethodMetadata getUpdateMethod() {
    return this.updateMethod;
  }

  /**
   * Method that returns delete Thymeleaf method
   * 
   * @return
   */
  public MethodMetadata getDeleteMethod() {
    return this.deleteMethod;
  }

  /**
   * Method that returns delete JSON method
   * 
   * @return
   */
  public MethodMetadata getDeleteJSONMethod() {
    return this.deleteJSONMethod;
  }

  /**
   * Method that returns show Thymeleaf method
   * 
   * @return
   */
  public MethodMetadata getShowMethod() {
    return this.showMethod;
  }

  /**
   * Method that returns details Thymeleaf methods
   * 
   * @return
   */
  public List<MethodMetadata> getDetailsMethod() {
    return this.detailsMethods;
  }

  /**
   * Method that returns populateForm method
   * 
   * @return
   */
  public MethodMetadata getPopulateFormMethod() {
    return this.populateFormMethod;
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
