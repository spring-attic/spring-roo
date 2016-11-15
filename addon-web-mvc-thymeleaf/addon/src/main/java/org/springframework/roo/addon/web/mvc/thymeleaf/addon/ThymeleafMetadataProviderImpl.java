package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import static org.springframework.roo.model.RooJavaType.ROO_THYMELEAF;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.layers.service.addon.ServiceMetadata;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMetadata;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.addon.web.mvc.views.AbstractViewGeneratorMetadataProvider;
import org.springframework.roo.addon.web.mvc.views.MVCViewGenerationService;
import org.springframework.roo.addon.web.mvc.views.ViewContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Implementation of {@link ThymeleafMetadataProvider}.
 *
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class ThymeleafMetadataProviderImpl extends
    AbstractViewGeneratorMetadataProvider<ThymeleafMetadata> implements ThymeleafMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(ThymeleafMetadataProviderImpl.class);

  private final Map<JavaType, String> domainTypeToServiceMidMap =
      new LinkedHashMap<JavaType, String>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_THYMELEAF} as additional JavaType
   * that will trigger metadata registration.</li>
   * <li>Set ensure the governor type details represent a class.</li>
   * </ul>
   */
  @Override
  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    serviceInstaceManager.activate(this.context);
    super.setDependsOnGovernorBeingAClass(false);
    this.registryTracker =
        new MetadataDependencyRegistryTracker(context, this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_THYMELEAF);
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

    removeMetadataTrigger(ROO_THYMELEAF);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return ThymeleafMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = ThymeleafMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = ThymeleafMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Thymeleaf";
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
  protected MVCViewGenerationService getViewGenerationService() {
    return getServiceManager().getServiceInstance(this, MVCViewGenerationService.class);
  }

  @Override
  protected ThymeleafMetadata createMetadataInstance(final String metadataIdentificationString,
      final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final ControllerMetadata controllerMetadata, final ServiceMetadata serviceMetadata,
      final JpaEntityMetadata entityMetadata, final String entityPlural,
      final String entityIdentifierPlural,
      final List<Pair<RelationInfo, JpaEntityMetadata>> compositionRelationOneToOne,
      final JavaType itemController, final JavaType collectionController,
      final List<FieldMetadata> dateTimeFields, final List<FieldMetadata> enumFields,
      final Map<String, MethodMetadata> findersToAdd,
      final Map<JavaType, List<FieldMetadata>> formBeansDateTimeFields,
      final Map<JavaType, List<FieldMetadata>> formBeansEnumFields,
      final JavaType detailsItemController, final JavaType detailsCollectionController,
      final ViewContext ctx) {

    final ThymeleafMetadata metadata =
        new ThymeleafMetadata(metadataIdentificationString, aspectName,
            governorPhysicalTypeMetadata, controllerMetadata, serviceMetadata, entityMetadata,
            entityPlural, entityIdentifierPlural, compositionRelationOneToOne, itemController,
            collectionController, dateTimeFields, enumFields, findersToAdd,
            formBeansDateTimeFields, formBeansEnumFields, detailsItemController,
            detailsCollectionController);

    ctx.addExtraParameter("mvcControllerName", metadata.getMvcControllerName());
    if (controllerMetadata.getType() == ControllerType.COLLECTION) {
      ctx.addExtraParameter("mvcCollectionControllerName", metadata.getMvcControllerName());
    } else {
      ctx.addExtraParameter("mvcCollectionControllerName",
          ThymeleafMetadata.getMvcControllerName(collectionController));
    }
    if (controllerMetadata.getType() == ControllerType.ITEM) {
      ctx.addExtraParameter("mvcItemControllerName", metadata.getMvcControllerName());
    } else {
      ctx.addExtraParameter("mvcItemControllerName",
          ThymeleafMetadata.getMvcControllerName(itemController));
    }
    ctx.addExtraParameter("mvcMethodName_datatables",
        ThymeleafMetadata.getMvcMethodName(ThymeleafMetadata.LIST_DATATABLES_METHOD_NAME));
    ctx.addExtraParameter("mvcMethodName_createForm",
        ThymeleafMetadata.getMvcMethodName(ThymeleafMetadata.CREATE_FORM_METHOD_NAME));
    ctx.addExtraParameter("mvcMethodName_show",
        ThymeleafMetadata.getMvcMethodName(ThymeleafMetadata.SHOW_METHOD_NAME));
    ctx.addExtraParameter("mvcMethodName_editForm",
        ThymeleafMetadata.getMvcMethodName(ThymeleafMetadata.EDIT_FORM_METHOD_NAME));
    ctx.addExtraParameter("mvcMethodName_remove",
        ThymeleafMetadata.getMvcMethodName(ThymeleafMetadata.DELETE_METHOD_NAME));
    ctx.addExtraParameter("mvcMethodName_list",
        ThymeleafMetadata.getMvcMethodName(ThymeleafMetadata.LIST_METHOD_NAME));
    ctx.addExtraParameter("mvcMethodName_update",
        ThymeleafMetadata.getMvcMethodName(ThymeleafMetadata.UPDATE_METHOD_NAME));

    // TODO finder names

    return metadata;
  }

  public String getProvidesType() {
    return ThymeleafMetadata.getMetadataIdentiferType();
  }

}
