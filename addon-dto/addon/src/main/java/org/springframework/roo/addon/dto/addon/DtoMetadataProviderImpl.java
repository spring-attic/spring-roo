package org.springframework.roo.addon.dto.addon;

import static org.springframework.roo.model.RooJavaType.*;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.*;
import org.springframework.roo.classpath.details.*;
import org.springframework.roo.classpath.itd.*;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.*;
import org.springframework.roo.project.LogicalPath;

/**
 * Implementation of {@link DtoMetadataProvider}.
 * <p/>
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class DtoMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider implements
    DtoMetadataProvider {

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

    addMetadataTrigger(ROO_DTO);
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

    removeMetadataTrigger(ROO_DTO);
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return DtoMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = DtoMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = DtoMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    return getLocalMid(itdTypeDetails);
  }

  @Override
  public String getItdUniquenessFilenameSuffix() {
    return "DTO";
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {

    final DtoAnnotationValues annotationValues =
        new DtoAnnotationValues(governorPhysicalTypeMetadata);

    // Get all DTO fields
    ClassOrInterfaceTypeDetails cid = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
    List<FieldMetadata> fields =
        getMemberDetailsScanner().getMemberDetails(this.getClass().getName(), cid).getFields();

    // Add dependency between modules
    for (FieldMetadata field : fields) {
      getTypeLocationService().addModuleDependency(cid.getName().getModule(), field.getFieldType());
    }

    return new DtoMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata,
        annotationValues, fields);
  }

  public String getProvidesType() {
    return DtoMetadata.getMetadataIdentiferType();
  }
}
