package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import static org.springframework.roo.model.RooJavaType.ROO_THYMELEAF_MAIN_CONTROLLER;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMVCService;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringEnumDetails;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link ThymeleafMainControllerMetadataProvider}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class ThymeleafMainControllerMetadataProviderImpl extends
    AbstractMemberDiscoveringItdMetadataProvider implements ThymeleafMainControllerMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(ThymeleafMainControllerMetadataProviderImpl.class);

  private final Map<JavaType, String> domainTypeToServiceMidMap =
      new LinkedHashMap<JavaType, String>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  private String metadataIdentificationString;
  private ClassOrInterfaceTypeDetails controller;

  private ControllerMVCService controllerMVCService;

  private List<JavaType> typesToImport;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_THYMELEAF_MAIN_CONTROLLER} as additional 
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

    addMetadataTrigger(ROO_THYMELEAF_MAIN_CONTROLLER);
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

    removeMetadataTrigger(ROO_THYMELEAF_MAIN_CONTROLLER);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return ThymeleafMainControllerMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType =
        ThymeleafMainControllerMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = ThymeleafMainControllerMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Thymeleaf_MainController";
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

    this.controller = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
    this.metadataIdentificationString = metadataIdentificationString;

    this.typesToImport = new ArrayList<JavaType>();

    return new ThymeleafMainControllerMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, getIndexMethod(), typesToImport);
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
    return ThymeleafMainControllerMetadata.getMetadataIdentiferType();
  }

  /**
   * This method provides the "index" method that returns Thymeleaf view
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getIndexMethod() {

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, "/", null, "", "", "");
    if (existingMVCMethod != null) {
      return existingMVCMethod;
    }


    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("index");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET, "/", null, "", "", ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Always save locale
    bodyBuilder.appendFormalLine(String.format(
        "model.addAttribute(\"application_locale\", %s.getLocale().getLanguage());",
        addTypeToImport(SpringJavaType.LOCALE_CONTEXT_HOLDER).getSimpleTypeName()));

    // return "index";
    bodyBuilder.appendFormalLine("return \"index\";");

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method registers a new type on types to import list
   * and then returns it.
   * 
   * @param type
   * @return
   */
  private JavaType addTypeToImport(JavaType type) {
    typesToImport.add(type);
    return type;
  }

  public ControllerMVCService getControllerMVCService() {
    if (controllerMVCService == null) {
      // Get all Services implement ControllerMVCService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ControllerMVCService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          controllerMVCService = (ControllerMVCService) this.context.getService(ref);
          return controllerMVCService;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ControllerMVCService on JSONMetadataProviderImpl.");
        return null;
      }
    } else {
      return controllerMVCService;
    }
  }
}
