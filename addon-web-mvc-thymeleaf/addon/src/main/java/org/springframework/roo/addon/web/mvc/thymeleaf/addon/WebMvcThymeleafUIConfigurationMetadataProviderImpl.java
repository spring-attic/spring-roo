package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link WebMvcThymeleafUIConfigurationMetadata}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class WebMvcThymeleafUIConfigurationMetadataProviderImpl extends
    AbstractMemberDiscoveringItdMetadataProvider implements
    WebMvcThymeleafUIConfigurationMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(WebMvcThymeleafUIConfigurationMetadataProviderImpl.class);

  private final Map<JavaType, String> domainTypeToServiceMidMap =
      new LinkedHashMap<JavaType, String>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_WEB_MVC_THYMELEAF_UI_CONFIGURATION} as additional 
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

    addMetadataTrigger(RooJavaType.ROO_WEB_MVC_THYMELEAF_UI_CONFIGURATION);
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

    removeMetadataTrigger(RooJavaType.ROO_WEB_MVC_THYMELEAF_UI_CONFIGURATION);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return WebMvcThymeleafUIConfigurationMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType =
        WebMvcThymeleafUIConfigurationMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path =
        WebMvcThymeleafUIConfigurationMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Thymeleaf_Configuration";
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

    // Looking for a valid DatatablesSortHandlerMethodArgumentResolver
    JavaType datatablesSortHandler = null;
    Set<ClassOrInterfaceTypeDetails> datatablesSortHandlerClasses =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_THYMELEAF_DATATABLES_SORT_HANDLER);
    if (datatablesSortHandlerClasses.isEmpty()) {
      throw new RuntimeException(
          "ERROR: DatatablesSortHandlerMethodArgumentResolver class doesn't exists or has been deleted.");
    }
    Iterator<ClassOrInterfaceTypeDetails> sortHandlerIterator =
        datatablesSortHandlerClasses.iterator();
    while (sortHandlerIterator.hasNext()) {
      datatablesSortHandler = sortHandlerIterator.next().getType();
      break;
    }

    // Looking for a valid DatatablesPageableHandlerMethodArgumentResolver
    JavaType datatablesPageableHandler = null;
    Set<ClassOrInterfaceTypeDetails> datatablesPageableHandlerClasses =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_THYMELEAF_DATATABLES_PAGEABLE_HANDLER);
    if (datatablesPageableHandlerClasses.isEmpty()) {
      throw new RuntimeException(
          "ERROR: DatatablesPageableHandlerMethodArgumentResolver class doesn't exists or has been deleted.");
    }
    Iterator<ClassOrInterfaceTypeDetails> pageableHandlerIterator =
        datatablesPageableHandlerClasses.iterator();
    while (pageableHandlerIterator.hasNext()) {
      datatablesPageableHandler = pageableHandlerIterator.next().getType();
      break;
    }

    return new WebMvcThymeleafUIConfigurationMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, datatablesPageableHandler, datatablesSortHandler);
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

  public String getProvidesType() {
    return WebMvcThymeleafUIConfigurationMetadata.getMetadataIdentiferType();
  }

}
