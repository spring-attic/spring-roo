package org.springframework.roo.addon.web.mvc.exceptions.addon;

import static org.springframework.roo.model.RooJavaType.ROO_EXCEPTION_HANDLERS;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
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
import org.springframework.roo.classpath.details.annotations.NestedAnnotationAttributeValue;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
* Implementation of {@link ExceptionsMetadataProvider}
*
* @author Fran Cardoso
* @since 2.0
*/
@Component
@Service
public class ExceptionsMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider
    implements ExceptionsMetadataProvider {

  private final Map<JavaType, String> domainTypeToServiceMidMap =
      new LinkedHashMap<JavaType, String>();
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;
  protected MetadataDependencyRegistryTracker registryTracker = null;
  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_EXCEPTION_HANDLERS} as additional JavaType
   * that will trigger metadata registration.</li>
   * <li>Set ensure the governor type details represent a class.</li>
   * </ul>
   */
  @Override
  @SuppressWarnings("unchecked")
  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    serviceInstaceManager.activate(this.context);
    this.registryTracker =
        new MetadataDependencyRegistryTracker(context, this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_EXCEPTION_HANDLERS);
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

    removeMetadataTrigger(ROO_EXCEPTION_HANDLERS);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  public String getItdUniquenessFilenameSuffix() {
    return "ExceptionHandler";
  }

  @Override
  public String getProvidesType() {
    return ExceptionsMetadata.getMetadataIdentiferType();
  }

  @Override
  protected String getLocalMidToRequest(ItdTypeDetails itdTypeDetails) {
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
  protected String createLocalIdentifier(JavaType javaType, LogicalPath path) {
    return ExceptionsMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
    final JavaType javaType = ExceptionsMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = ExceptionsMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString,
      JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {

    AnnotationMetadata handlersAnnotation =
        governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getAnnotation(
            ROO_EXCEPTION_HANDLERS);

    // Prepare list to register @RooExceptionHandler annotations data
    List<ExceptionHandlerAnnotationValues> exceptionHandlerValues =
        new ArrayList<ExceptionHandlerAnnotationValues>();

    // Get nested annotations
    AnnotationAttributeValue<Object> handlers = handlersAnnotation.getAttribute("value");
    List<?> values = (List<?>) handlers.getValue();
    Iterator<?> valuesIt = values.iterator();

    // Iterate over nested annotations
    while (valuesIt.hasNext()) {
      NestedAnnotationAttributeValue handlerAnnotation =
          (NestedAnnotationAttributeValue) valuesIt.next();
      if (handlerAnnotation.getValue() != null) {
        // Get attribute values
        JavaType exception = getNestedAttributeValue(handlerAnnotation, "exception");
        ClassOrInterfaceTypeDetails exceptionDetails =
            getTypeLocationService().getTypeDetails(exception);
        Validate.notNull(exception,
            "'exception' attribute in @RooExceptionHandler must not be null");
        String errorView = getNestedAttributeValue(handlerAnnotation, "errorView");
        // Register attribute values
        exceptionHandlerValues
            .add(new ExceptionHandlerAnnotationValues(exceptionDetails, errorView));
      }
    }

    // Check if type is a controller
    AnnotationMetadata rooControllerAnnotation =
        governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getAnnotation(
            RooJavaType.ROO_CONTROLLER);
    boolean isController = rooControllerAnnotation != null;

    List<FieldMetadata> fieldsMetadata =
        getMemberDetailsScanner().getMemberDetails(this.getClass().getName(),
            governorPhysicalTypeMetadata.getMemberHoldingTypeDetails()).getFields();

    return new ExceptionsMetadata(metadataIdentificationString, exceptionHandlerValues, aspectName,
        governorPhysicalTypeMetadata, fieldsMetadata, isController);
  }

  /**
   * Gets value of a nested annotation attribute value
   *
   * @param newstedAnnotationAttr
   * @param attributeName
   * @return
   */
  private <T> T getNestedAttributeValue(NestedAnnotationAttributeValue nestedAnnotationAttr,
      String attributeName) {

    AnnotationMetadata annotationValue = nestedAnnotationAttr.getValue();
    if (annotationValue == null) {
      return null;
    }
    AnnotationAttributeValue<?> attribute = annotationValue.getAttribute(attributeName);
    if (attribute == null) {
      return null;
    }
    return (T) attribute.getValue();
  }

  private <T> T getAttributeValue(AnnotationAttributeValue attributeValue) {
    if (attributeValue == null) {
      return null;
    }
    return (T) attributeValue.getValue();
  }

}
