package org.springframework.roo.addon.javabean.addon;

import static org.springframework.roo.model.RooJavaType.ROO_TO_STRING;

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
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides {@link ToStringMetadata}.
 * 
 * @author Ben Alex
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 1.0
 */
@Component
@Service
public class ToStringMetadataProvider extends AbstractMemberDiscoveringItdMetadataProvider {

  protected MetadataDependencyRegistryTracker registryTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}</li>
   * <li>Registers {@link RooJavaType#ROO_TO_STRING} as additional JavaType 
   * that will trigger metadata registration.</li>
   * </ul>
   */
  @Override
  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    this.registryTracker =
        new MetadataDependencyRegistryTracker(context, this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_TO_STRING);
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

    removeMetadataTrigger(ROO_TO_STRING);
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return ToStringMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = ToStringMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = ToStringMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "ToString";
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    return getLocalMid(itdTypeDetails);
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
    final ToStringAnnotationValues annotationValues =
        new ToStringAnnotationValues(governorPhysicalTypeMetadata);
    if (!annotationValues.isAnnotationFound()) {
      return null;
    }

    final MemberDetails memberDetails = getMemberDetails(governorPhysicalTypeMetadata);
    if (memberDetails == null || memberDetails.getFields().isEmpty()) {
      return null;
    }

    // Exclude fields which are in superclass
    ClassOrInterfaceTypeDetails superclass =
        governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getSuperclass();
    List<FieldMetadata> toStringFields = new ArrayList<FieldMetadata>();
    if (superclass != null && superclass != JavaType.OBJECT) {
      List<FieldMetadata> superclassFields =
          getMemberDetailsScanner().getMemberDetails(this.getClass().getName(), superclass)
              .getFields();
      for (FieldMetadata field : memberDetails.getFields()) {
        boolean alreadyInSuperclass = false;
        for (FieldMetadata superclassField : superclassFields) {
          if (superclassField.getDeclaredByMetadataId().equals(field.getDeclaredByMetadataId())
              && superclassField.getFieldName().equals(field.getFieldName())) {
            alreadyInSuperclass = true;
            break;
          }
        }
        if (!alreadyInSuperclass) {
          toStringFields.add(field);
        }
      }
    } else {
      toStringFields.addAll(memberDetails.getFields());
    }

    return new ToStringMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, annotationValues, toStringFields);
  }

  public String getProvidesType() {
    return ToStringMetadata.getMetadataIdentiferType();
  }
}
