package org.springframework.roo.addon.field.addon;

import static org.springframework.roo.model.RooJavaType.ROO_RELATION_MANAGEMENT;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link DtoMetadataProvider}.
 * <p/>
 *
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class RelationManagementMetadataProviderImpl extends
    AbstractMemberDiscoveringItdMetadataProvider implements RelationManagementMetadataProvider {

  protected MetadataDependencyRegistryTracker registryTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_DTO} as additional
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

    addMetadataTrigger(ROO_RELATION_MANAGEMENT);
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

    removeMetadataTrigger(ROO_RELATION_MANAGEMENT);
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return RelationManagementMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = RelationManagementMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = RelationManagementMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    return getLocalMid(itdTypeDetails);
  }

  @Override
  public String getItdUniquenessFilenameSuffix() {
    return "RelationManagement";
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {

    final ReleationManagementAnnotationValues annotationValues =
        new ReleationManagementAnnotationValues(governorPhysicalTypeMetadata);

    // Get all DTO fields
    ClassOrInterfaceTypeDetails cid = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
    List<FieldMetadata> fieldList =
        getMemberDetailsScanner().getMemberDetails(this.getClass().getName(), cid).getFields();
    Map<String, FieldMetadata> fields = new HashMap<String, FieldMetadata>(fieldList.size());

    // fill fields map
    for (FieldMetadata field : fieldList) {
      fields.put(field.getFieldName().getSymbolName(), field);
    }

    // Create metadata
    return new RelationManagementMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, annotationValues, fields);
  }

  public String getProvidesType() {
    return RelationManagementMetadata.getMetadataIdentiferType();
  }
}
