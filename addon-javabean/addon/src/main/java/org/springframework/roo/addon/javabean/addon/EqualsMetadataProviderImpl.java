package org.springframework.roo.addon.javabean.addon;

import static org.springframework.roo.model.RooJavaType.ROO_EQUALS;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
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
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Implementation of {@link EqualsMetadataProvider}.
 * 
 * @author Alan Stewart
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 1.2.0
 */
@Component
@Service
public class EqualsMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider
    implements EqualsMetadataProvider {

  protected MetadataDependencyRegistryTracker registryTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}</li>
   * <li>Registers {@link RooJavaType#ROO_EQUALS} as additional JavaType 
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
    addMetadataTrigger(ROO_EQUALS);
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
    removeMetadataTrigger(ROO_EQUALS);
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return EqualsMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = EqualsMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = EqualsMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Equals";
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    return getLocalMid(itdTypeDetails);
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
    final EqualsAnnotationValues annotationValues =
        new EqualsAnnotationValues(governorPhysicalTypeMetadata);
    if (!annotationValues.isAnnotationFound()) {
      return null;
    }

    final MemberDetails memberDetails = getMemberDetails(governorPhysicalTypeMetadata);
    if (memberDetails == null) {
      return null;
    }

    final JavaType javaType = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getName();
    final List<FieldMetadata> equalityFields =
        locateFields(javaType, annotationValues.getExcludeFields(), memberDetails,
            metadataIdentificationString);

    return new EqualsMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, annotationValues, equalityFields);
  }

  public String getProvidesType() {
    return EqualsMetadata.getMetadataIdentiferType();
  }

  private List<FieldMetadata> locateFields(final JavaType javaType, final String[] excludeFields,
      final MemberDetails memberDetails, final String metadataIdentificationString) {
    final SortedSet<FieldMetadata> locatedFields =
        new TreeSet<FieldMetadata>(new Comparator<FieldMetadata>() {
          public int compare(final FieldMetadata l, final FieldMetadata r) {
            return l.getFieldName().compareTo(r.getFieldName());
          }
        });

    final List<?> excludeFieldsList = CollectionUtils.arrayToList(excludeFields);
    final FieldMetadata versionField = getPersistenceMemberLocator().getVersionField(javaType);

    for (final FieldMetadata field : memberDetails.getFields()) {
      if (excludeFieldsList.contains(field.getFieldName().getSymbolName())) {
        continue;
      }
      if (Modifier.isStatic(field.getModifier()) || Modifier.isTransient(field.getModifier())
          || field.getFieldType().isCommonCollectionType() || field.getFieldType().isArray()) {
        continue;
      }
      if (versionField != null && field.getFieldName().equals(versionField.getFieldName())) {
        continue;
      }

      locatedFields.add(field);
      getMetadataDependencyRegistry().registerDependency(field.getDeclaredByMetadataId(),
          metadataIdentificationString);
    }

    return new ArrayList<FieldMetadata>(locatedFields);
  }
}
