package org.springframework.roo.addon.layers.repository.jpa.addon;

import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_JPA_CUSTOM;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.finder.addon.FinderMetadata;
import org.springframework.roo.addon.javabean.addon.JavaBeanMetadata;
import org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepositoryCustom;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.NestedAnnotationAttributeValue;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerTypeMatcher;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringletsJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Implementation of {@link RepositoryJpaCustomMetadataProvider}.
 *
 * @author Paula Navarro
 * @author Juan Carlos García
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class RepositoryJpaCustomMetadataProviderImpl extends
    AbstractMemberDiscoveringItdMetadataProvider implements RepositoryJpaCustomMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(RepositoryJpaCustomMetadataProviderImpl.class);

  private final Map<JavaType, String> domainTypeToRepositoryMidMap =
      new LinkedHashMap<JavaType, String>();
  private final Map<String, JavaType> repositoryMidToDomainTypeMap =
      new LinkedHashMap<String, JavaType>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_REPOSITORY_JPA_CUSTOM} as additional
   * JavaType that will trigger metadata registration.</li>
   * <li>Set ensure the governor type details represent a class.</li>
   * </ul>
   */
  @Override
  @SuppressWarnings("unchecked")
  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    super.setDependsOnGovernorBeingAClass(false);
    this.registryTracker =
        new MetadataDependencyRegistryTracker(context, this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_REPOSITORY_JPA_CUSTOM);

    this.keyDecoratorTracker =
        new CustomDataKeyDecoratorTracker(context, getClass(), new LayerTypeMatcher(
            ROO_REPOSITORY_JPA_CUSTOM, new JavaSymbolName(RooJpaRepositoryCustom.ENTITY_ATTRIBUTE)));
    this.keyDecoratorTracker.open();
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

    removeMetadataTrigger(ROO_REPOSITORY_JPA_CUSTOM);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return RepositoryJpaCustomMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = RepositoryJpaCustomMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = RepositoryJpaCustomMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Jpa_Repository_Custom";
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    // Determine the governor for this ITD, and whether any metadata is even
    // hoping to hear about changes to that JavaType and its ITDs
    final JavaType governor = itdTypeDetails.getName();
    final String localMid = domainTypeToRepositoryMidMap.get(governor);
    if (localMid != null) {
      return localMid;
    }

    final MemberHoldingTypeDetails memberHoldingTypeDetails =
        getTypeLocationService().getTypeDetails(governor);
    if (memberHoldingTypeDetails != null) {
      for (final JavaType type : memberHoldingTypeDetails.getLayerEntities()) {
        final String localMidType = domainTypeToRepositoryMidMap.get(type);
        if (localMidType != null) {
          return localMidType;
        }
      }
    }
    return null;
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
    final RepositoryJpaCustomAnnotationValues annotationValues =
        new RepositoryJpaCustomAnnotationValues(governorPhysicalTypeMetadata);

    // Getting repository custom
    JavaType entity = annotationValues.getEntity();
    Validate
        .notNull(
            entity,
            "ERROR: Repository custom interface should be contain an entity on @RooJpaRepositoryCustom annotation");

    // Register upstream dependency for FinderMetadata to update projection finders
    Set<ClassOrInterfaceTypeDetails> repositoryClasses =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_REPOSITORY_JPA);
    for (ClassOrInterfaceTypeDetails repositoryClass : repositoryClasses) {
      if (repositoryClass.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA).getAttribute("entity")
          .getValue().equals(entity)) {
        LogicalPath repositoryLogicalPath =
            PhysicalTypeIdentifier.getPath(repositoryClass.getDeclaredByMetadataId());
        String finderMetadataKey =
            FinderMetadata.createIdentifier(repositoryClass.getType(), repositoryLogicalPath);
        registerDependency(finderMetadataKey, metadataIdentificationString);
        break;
      }
    }

    // Getting findAll results type
    JavaType defaultReturnType = annotationValues.getDefaultReturnType();
    Validate
        .notNull(
            defaultReturnType,
            "ERROR: Repository custom interface should contain a defaultReturnType on @RooJpaRepositoryCustom annotation");

    // Add dependency between modules
    ClassOrInterfaceTypeDetails cid = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
    getTypeLocationService().addModuleDependency(cid.getName().getModule(), entity);
    getTypeLocationService().addModuleDependency(cid.getName().getModule(), defaultReturnType);

    // Getting referenced fields
    Map<FieldMetadata, JavaType> referencedFields = new HashMap<FieldMetadata, JavaType>();
    MemberDetails entityDetails = getMemberDetails(entity);
    List<FieldMetadata> entityFields = entityDetails.getFields();

    for (FieldMetadata field : entityFields) {
      ClassOrInterfaceTypeDetails fieldTypeDetails =
          getTypeLocationService().getTypeDetails(field.getFieldType());
      // Get only reference fields
      if (fieldTypeDetails != null
          && fieldTypeDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY) != null) {

        List<FieldMetadata> identifierFields =
            getPersistenceMemberLocator().getIdentifierFields(field.getFieldType());

        if (identifierFields.isEmpty()) {
          continue;
        }
        referencedFields.put(field, identifierFields.get(0).getFieldType());

        // Add dependency between modules
        getTypeLocationService().addModuleDependency(cid.getName().getModule(),
            field.getFieldType());
      }
    }

    // Register dependency between JavaBeanMetadata and this one
    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(getTypeLocationService().getTypeDetails(entity)
            .getDeclaredByMetadataId());
    final String javaBeanMetadataKey =
        JavaBeanMetadata.createIdentifier(
            getTypeLocationService().getTypeDetails(entity).getType(), logicalPath);
    registerDependency(javaBeanMetadataKey, metadataIdentificationString);

    // Get data for those finders whose return type is a projection
    Set<ClassOrInterfaceTypeDetails> repositoryJpaClasses =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_REPOSITORY_JPA);
    ClassOrInterfaceTypeDetails associatedRepositoryInterface = null;
    for (ClassOrInterfaceTypeDetails repositoryClass : repositoryJpaClasses) {
      if (repositoryClass.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA).getAttribute("entity")
          .getValue().equals(annotationValues.getEntity())) {
        associatedRepositoryInterface = repositoryClass;
        break;
      }
    }

    // Create a list of finder methods which must be in the repository custom, not Jpa interface
    // (those finders whose defaultReturnType are projections)
    List<CustomFinderMethod> findersToAdd = new ArrayList<CustomFinderMethod>();

    // Get @RooFinders from associated repository interface
    if (associatedRepositoryInterface != null
        && associatedRepositoryInterface.getAnnotation(RooJavaType.ROO_FINDERS) != null) {
      AnnotationAttributeValue<?> currentFinders =
          associatedRepositoryInterface.getAnnotation(RooJavaType.ROO_FINDERS).getAttribute(
              "finders");
      if (currentFinders != null) {

        List<?> values = (List<?>) currentFinders.getValue();
        Iterator<?> it = values.iterator();

        while (it.hasNext()) {

          // Get each finder value
          NestedAnnotationAttributeValue finderAnnotation =
              (NestedAnnotationAttributeValue) it.next();
          if (finderAnnotation.getValue() != null
              && finderAnnotation.getValue().getAttribute("finder") != null) {

            // Get finder return type
            JavaType finderReturnType =
                (JavaType) finderAnnotation.getValue().getAttribute("defaultReturnType").getValue();
            Validate.notNull(finderReturnType,
                "@RooFinder must have a 'defaultReturnType' parameter.");

            // Get finder form bean
            JavaType formBean =
                (JavaType) finderAnnotation.getValue().getAttribute("formBean").getValue();
            Validate.notNull(formBean, "@RooFinder must have a 'formBean' parameter.");
            if (formBean.equals(entity)) {

              // formBean hasn't been specified, so use repository defaultReturnType
              formBean = defaultReturnType;
            }

            // Check if default type is a Roo Projection or form bean is a DTO
            if ((getTypeLocationService().getTypeDetails(finderReturnType) != null && getTypeLocationService()
                .getTypeDetails(finderReturnType).getAnnotation(RooJavaType.ROO_ENTITY_PROJECTION) != null)
                || (getTypeLocationService().getTypeDetails(formBean) != null && getTypeLocationService()
                    .getTypeDetails(formBean).getAnnotation(RooJavaType.ROO_DTO) != null)) {

              // Get finder name
              String finderName = null;
              if (finderAnnotation.getValue().getAttribute("finder").getValue() instanceof String) {
                finderName = (String) finderAnnotation.getValue().getAttribute("finder").getValue();
              }
              Validate.notNull(finderName, "'finder' attribute in @RooFinder must be a String");

              // Get finder form bean
              JavaType finderFormBean =
                  (JavaType) finderAnnotation.getValue().getAttribute("formBean").getValue();
              Validate.notNull(finderFormBean, "@RooFinder must have a 'formBean' parameter.");

              // Add to finder methods list
              findersToAdd.add(new CustomFinderMethod(finderReturnType, new JavaSymbolName(
                  finderName), finderFormBean));
            }
          }
        }
      }
    }

    return new RepositoryJpaCustomMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, annotationValues, entity, defaultReturnType,
        referencedFields, findersToAdd);
  }

  protected void registerDependency(final String upstreamDependency,
      final String downStreamDependency) {

    if (getMetadataDependencyRegistry() != null
        && StringUtils.isNotBlank(upstreamDependency)
        && StringUtils.isNotBlank(downStreamDependency)
        && !upstreamDependency.equals(downStreamDependency)
        && !MetadataIdentificationUtils.getMetadataClass(downStreamDependency).equals(
            MetadataIdentificationUtils.getMetadataClass(upstreamDependency))) {
      getMetadataDependencyRegistry().registerDependency(upstreamDependency, downStreamDependency);
    }
  }

  public String getProvidesType() {
    return RepositoryJpaCustomMetadata.getMetadataIdentiferType();
  }
}
