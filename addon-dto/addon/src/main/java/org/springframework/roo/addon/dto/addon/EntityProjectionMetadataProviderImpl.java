package org.springframework.roo.addon.dto.addon;

import static org.springframework.roo.model.RooJavaType.ROO_ENTITY_PROJECTION;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link EntityProjectionMetadataProvider}.
 * <p/>
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class EntityProjectionMetadataProviderImpl extends
    AbstractMemberDiscoveringItdMetadataProvider implements EntityProjectionMetadataProvider {

  protected MetadataDependencyRegistryTracker registryTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_ENTITY_PROJECTION} as additional 
   * JavaType that will trigger metadata registration.</li>
   * <li>Set ensure the governor type details represent a class.</li>
   * </ul>
   */
  @Override
  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    super.setDependsOnGovernorBeingAClass(true);
    this.registryTracker =
        new MetadataDependencyRegistryTracker(context, this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_ENTITY_PROJECTION);
  }

  /**
   * This service is being deactivated so unregister upstream-downstream 
   * dependencies, triggers, matchers and listeners.
   * 
   * @param context
   */
  protected void deactivate(final ComponentContext context) {
    MetadataDependencyRegistry registry = this.registryTracker.getService();
    registry.removeNotificationListener(this);
    registry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(),
        getProvidesType());
    this.registryTracker.close();

    removeMetadataTrigger(ROO_ENTITY_PROJECTION);
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return EntityProjectionMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = EntityProjectionMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = EntityProjectionMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    return getLocalMid(itdTypeDetails);
  }

  @Override
  public String getItdUniquenessFilenameSuffix() {
    return "Projection";
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {

    final EntityProjectionAnnotationValues annotationValues =
        new EntityProjectionAnnotationValues(governorPhysicalTypeMetadata);

    // Get CID from governor
    ClassOrInterfaceTypeDetails cid = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();

    // Get all projection fields from annotation
    String[] fieldsString = annotationValues.getFields();
    List<FieldMetadata> fields = buildFieldMetadataFromAnnotation(fieldsString, cid);

    // Add dependency between modules
    for (FieldMetadata field : fields) {
      getTypeLocationService().addModuleDependency(cid.getName().getModule(), field.getFieldType());
    }

    return new EntityProjectionMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, annotationValues, fields);
  }

  /**
   * Builds FieldMetadata to provide {@link EntityProjectionMetadata} with the necessary 
   * resources to create constructor.
   * 
   * @param fields the String[] from 'fields' annotation parameter.
   * @param cid the governor ClassOrInterfaceTypeDetails, that is, the Projection physical type.
   * @return the List<FieldMetadata> with the fields to build the constructor.
   */
  private List<FieldMetadata> buildFieldMetadataFromAnnotation(String[] fields,
      ClassOrInterfaceTypeDetails cid) {
    List<FieldMetadata> allFields =
        getMemberDetailsScanner().getMemberDetails(this.getClass().getName(), cid).getFields();
    List<FieldMetadata> fieldsToAdd = new ArrayList<FieldMetadata>();

    // Iterate over all specified fields
    for (int i = 0; i < fields.length; i++) {
      String fieldName = "";
      boolean existsInGovernor = false;

      // Build field name following Java convention
      String[] splittedByDot = StringUtils.split(fields[i], ".");
      for (int t = 0; t < splittedByDot.length; t++) {
        if (t == 0) {
          fieldName = fieldName.concat(splittedByDot[t]);
        } else {
          fieldName = fieldName.concat(StringUtils.capitalize(splittedByDot[t]));
        }
      }

      // Check existence in governor
      for (FieldMetadata field : allFields) {
        if (field.getFieldName().getSymbolName().equals(fieldName)) {
          existsInGovernor = true;
          fieldsToAdd.add(field);
        }
      }

      if (!existsInGovernor) {
        throw new IllegalStateException(
            String
                .format(
                    "Field %s couldn't be located in %s. Please, be sure that it is well written in 'fields' param of @RooEntityProjection.",
                    fieldName, cid.getType().getFullyQualifiedTypeName()));
      }
    }
    return fieldsToAdd;
  }

  public String getProvidesType() {
    return EntityProjectionMetadata.getMetadataIdentiferType();
  }
}
