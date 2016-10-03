package org.springframework.roo.addon.web.mvc.controller.addon;

import static org.springframework.roo.model.RooJavaType.ROO_CONTROLLER;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.jvnet.inflector.Noun;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.javabean.addon.JavaBeanMetadata;
import org.springframework.roo.addon.layers.service.addon.ServiceMetadata;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of {@link ControllerMetadataProvider}.
 *
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class ControllerMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider
    implements ControllerMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(ControllerMetadataProviderImpl.class);

  private final Map<JavaType, String> domainTypeToServiceMidMap =
      new LinkedHashMap<JavaType, String>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_CONTROLLER} as additional JavaType
   * that will trigger metadata registration.</li>
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

    addMetadataTrigger(ROO_CONTROLLER);
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

    removeMetadataTrigger(ROO_CONTROLLER);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return ControllerMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = ControllerMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = ControllerMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Controller";
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    // Determine the governor for this ITD, and whether any metadata is even
    // hoping to hear about changes to that JavaType and its ITDs
    final JavaType governor = itdTypeDetails.getName();
    final String localMid = domainTypeToServiceMidMap.get(governor);
    if (localMid != null) {
      return localMid;
    }

    final MemberHoldingTypeDetails memberHoldingTypeDetails =
        getTypeLocationService().getTypeDetails(governor);
    if (memberHoldingTypeDetails != null) {
      for (final JavaType type : memberHoldingTypeDetails.getLayerEntities()) {
        final String localMidType = domainTypeToServiceMidMap.get(type);
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

    AnnotationMetadata controllerAnnotation =
        governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getAnnotation(
            RooJavaType.ROO_CONTROLLER);

    // Getting entity
    JavaType entity = (JavaType) controllerAnnotation.getAttribute("entity").getValue();

    // Getting type
    ControllerType type =
        ControllerType.getControllerType(((EnumDetails) controllerAnnotation.getAttribute("type")
            .getValue()).getField().getSymbolName());

    // Getting pathPrefix
    AnnotationAttributeValue<Object> pathPrefixAttr =
        controllerAnnotation.getAttribute("pathPrefix");
    String pathPrefix = "";
    if (pathPrefixAttr != null) {
      pathPrefix = StringUtils.lowerCase((String) pathPrefixAttr.getValue());
    }

    // Getting related service
    JavaType service = getService(entity);

    // Getting identifierType
    JavaType identifierType = getPersistenceMemberLocator().getIdentifierType(entity);

    // Generate path
    String path =
        "/".concat(StringUtils.lowerCase(Noun.pluralOf(entity.getSimpleTypeName(), Locale.ENGLISH)));
    if (StringUtils.isNotEmpty(pathPrefix)) {
      if (!pathPrefix.startsWith("/")) {
        pathPrefix = "/".concat(pathPrefix);
      }
      path = pathPrefix.concat(path);
    }

    // Check if is necessary to include service fields of Set and List
    // fields
    Set<ClassOrInterfaceTypeDetails> allDefinedServices =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_SERVICE);

    MemberDetails entityDetails = getMemberDetails(entity);
    List<FieldMetadata> entityFields = entityDetails.getFields();

    List<JavaType> detailsService = new ArrayList<JavaType>();

    for (FieldMetadata field : entityFields) {

      // If entity has some Set or List field, maybe is necessary to
      // generate details service
      if (field.getFieldType().getFullyQualifiedTypeName().equals(Set.class.getName())
          || field.getFieldType().getFullyQualifiedTypeName().equals(List.class.getName())) {

        // Getting inner type
        JavaType detailType = field.getFieldType().getBaseType();
        // Check if provided inner type is an entity annotated with
        // @RooJPAEntity
        if (detailType != null
            && getTypeLocationService().getTypeDetails(detailType) != null
            && getTypeLocationService().getTypeDetails(detailType).getAnnotation(
                RooJavaType.ROO_JPA_ENTITY) != null) {
          // Getting service that manages detail type
          for (ClassOrInterfaceTypeDetails detailService : allDefinedServices) {
            AnnotationAttributeValue<JavaType> entityServiceAttr =
                detailService.getAnnotation(RooJavaType.ROO_SERVICE).getAttribute("entity");
            if (entityServiceAttr != null && entityServiceAttr.getValue().equals(detailType)
                && !detailsService.contains(detailService.getType())) {
              detailsService.add(detailService.getType());
            }
          }
        }

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

    // Getting service metadata
    ClassOrInterfaceTypeDetails serviceDetails = getTypeLocationService().getTypeDetails(service);
    final LogicalPath serviceLogicalPath =
        PhysicalTypeIdentifier.getPath(serviceDetails.getDeclaredByMetadataId());
    final String serviceMetadataKey =
        ServiceMetadata.createIdentifier(serviceDetails.getType(), serviceLogicalPath);
    final ServiceMetadata serviceMetadata =
        (ServiceMetadata) getMetadataService().get(serviceMetadataKey);

    ControllerDetailInfo controllerDetailInfo = null;
    if (type == ControllerType.DETAIL) {
      // generate detail info object
      controllerDetailInfo = getControllerDetailInfo(governorPhysicalTypeMetadata, path, entity);
    }

    return new ControllerMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, entity, service, detailsService, path, type, identifierType,
        serviceMetadata, controllerDetailInfo);
  }

  private void registerDependency(final String upstreamDependency, final String downStreamDependency) {

    if (getMetadataDependencyRegistry() != null
        && StringUtils.isNotBlank(upstreamDependency)
        && StringUtils.isNotBlank(downStreamDependency)
        && !upstreamDependency.equals(downStreamDependency)
        && !MetadataIdentificationUtils.getMetadataClass(downStreamDependency).equals(
            MetadataIdentificationUtils.getMetadataClass(upstreamDependency))) {
      getMetadataDependencyRegistry().registerDependency(upstreamDependency, downStreamDependency);
    }
  }

  /**
   * Get necesary information about detail to create a detail controller
   *
   * @param governorPhysicalTypeMetadata the governor, which is expected to
     *            contain a {@link ClassOrInterfaceTypeDetails}
   * @param path Path parent controller
   * @param entity Parent entity
   *
   * @return Information about detail
   */
  private ControllerDetailInfo getControllerDetailInfo(
      PhysicalTypeMetadata governorPhysicalTypeMetadata, String path, JavaType entity) {

    ControllerDetailInfo controllerDetailInfo = new ControllerDetailInfo();

    // Getting the relationField from @RooDetail entity
    AnnotationAttributeValue<Object> relationFieldAttr =
        governorPhysicalTypeMetadata.getMemberHoldingTypeDetails()
            .getAnnotation(RooJavaType.ROO_DETAIL).getAttribute("relationField");

    if (relationFieldAttr == null) {
      LOGGER.log(Level.INFO,
          "ERROR: In %s controller, @RooDetail annotation must have relationField value",
          governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getName());
      return null;
    }

    String relationField = (String) relationFieldAttr.getValue();

    if (StringUtils.isEmpty(relationField)) {
      LOGGER.log(Level.INFO,
          "ERROR: In %s controller, @RooDetail annotation must have relationField value",
          governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getName());
      return null;
    }

    // get detail path
    String[] relationFieldParts = relationField.split("[.]");
    String pathDetail = path;
    if (relationFieldParts.length == 1) {
      pathDetail = pathDetail.concat("/{id}/").concat(relationFieldParts[0]);
    } else {
      pathDetail =
          pathDetail.concat("/").concat("{")
              .concat(StringUtils.lowerCase(entity.getSimpleTypeName())).concat("}/");
      int count = 0;
      while (count < relationFieldParts.length - 2) {
        pathDetail = pathDetail.concat(relationFieldParts[count]);
        pathDetail =
            pathDetail.concat("/").concat("{").concat(relationFieldParts[count]).concat("}/");
        count++;
      }
      pathDetail = pathDetail.concat(relationFieldParts[count]);
      pathDetail = pathDetail.concat("/{id}/").concat(relationFieldParts[count + 1]);

    }
    controllerDetailInfo.setPath(pathDetail);

    // get detail entity and detail patern entity
    JavaType detailEntity = getEntityFromRelationField(relationFieldParts, entity, 0);
    JavaType parentDetailEntity = entity;
    if (relationFieldParts.length > 1) {
      String[] parentRelationFieldParts =
          Arrays.copyOf(relationFieldParts, relationFieldParts.length - 1);
      parentDetailEntity = getEntityFromRelationField(parentRelationFieldParts, entity, 0);
    }

    // Getting & setting entities
    controllerDetailInfo.setEntity(detailEntity);
    controllerDetailInfo.setParentEntity(parentDetailEntity);

    // Getting & set services
    controllerDetailInfo.setService(getService(detailEntity));
    controllerDetailInfo.setParentService(getService(parentDetailEntity));

    // Getting & setting parent identifier type
    controllerDetailInfo.setParentIdentifierType(getPersistenceMemberLocator().getIdentifierType(
        entity));

    // Getting & setting parent field name
    controllerDetailInfo
        .setParentReferenceFieldName(relationFieldParts[relationFieldParts.length - 1]);


    return controllerDetailInfo;

  }

  /**
   * Get the entity that represents a relation field
   *
   * @param relationField Array that represents the relation field.
   * @param entity Current entity to search the corresponding field
   * @param level Current array level to search
   * @return
   */
  private JavaType getEntityFromRelationField(String[] relationField, JavaType entity, int level) {
    JavaType entityTypeToCalculate = null;
    MemberDetails memberDetails = getMemberDetails(entity);
    List<FieldMetadata> fields = memberDetails.getFields();
    for (FieldMetadata entityField : fields) {
      if (entityField.getFieldName().getSymbolName().equals(relationField[level])) {

        AnnotationMetadata oneToManyAnnotation = entityField.getAnnotation(JpaJavaType.ONE_TO_MANY);

        if (oneToManyAnnotation != null
            && (entityField.getFieldType().getFullyQualifiedTypeName()
                .equals(JavaType.LIST.getFullyQualifiedTypeName()) || entityField.getFieldType()
                .getFullyQualifiedTypeName().equals(JavaType.SET.getFullyQualifiedTypeName()))) {
          level++;
          if (relationField.length > level) {
            entityTypeToCalculate =
                getEntityFromRelationField(relationField, entityField.getFieldType()
                    .getParameters().get(0), level);
          } else {
            entityTypeToCalculate = entityField.getFieldType().getParameters().get(0);
          }
          break;
        }
      }
    }
    return entityTypeToCalculate;
  }

  /**
   * Get the service related with a entity
   *
   * @param entity Entity over wich search the service
   *
   * @return The service related with the entity
   */
  private JavaType getService(JavaType entity) {
    // Getting related service
    JavaType service = null;
    Set<ClassOrInterfaceTypeDetails> services =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_SERVICE);
    Iterator<ClassOrInterfaceTypeDetails> itServices = services.iterator();

    while (itServices.hasNext()) {
      ClassOrInterfaceTypeDetails existingService = itServices.next();
      AnnotationAttributeValue<Object> entityAttr =
          existingService.getAnnotation(RooJavaType.ROO_SERVICE).getAttribute("entity");
      if (entityAttr != null && entityAttr.getValue().equals(entity)) {
        service = existingService.getType();
      }
    }

    return service;
  }

  public String getProvidesType() {
    return ControllerMetadata.getMetadataIdentiferType();
  }
}
