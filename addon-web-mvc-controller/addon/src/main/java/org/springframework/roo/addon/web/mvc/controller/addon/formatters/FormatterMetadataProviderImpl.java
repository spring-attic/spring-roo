package org.springframework.roo.addon.web.mvc.controller.addon.formatters;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.javabean.addon.JavaBeanMetadata;
import org.springframework.roo.addon.web.mvc.controller.addon.config.WebMvcConfigurationMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
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
 * Implementation of {@link FormatterMetadataProvider}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class FormatterMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider
    implements FormatterMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(FormatterMetadataProviderImpl.class);

  private final Map<JavaType, String> domainTypeToServiceMidMap =
      new LinkedHashMap<JavaType, String>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_FORMATTER} as additional 
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

    addMetadataTrigger(RooJavaType.ROO_FORMATTER);
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

    removeMetadataTrigger(RooJavaType.ROO_FORMATTER);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return FormatterMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = FormatterMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = FormatterMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Formatter";
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

    AnnotationMetadata formatterAnnotation =
        governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getAnnotation(
            RooJavaType.ROO_FORMATTER);

    // Getting entity
    JavaType entity = (JavaType) formatterAnnotation.getAttribute("entity").getValue();

    // Getting identifierType
    JavaType identifierType = getPersistenceMemberLocator().getIdentifierType(entity);

    // Getting entity accessors
    List<MethodMetadata> accessors = getEntityAccessors(metadataIdentificationString, entity);

    // Getting service
    JavaType service = (JavaType) formatterAnnotation.getAttribute("service").getValue();

    // Register dependency with WebMvcConfiguration
    Set<ClassOrInterfaceTypeDetails> webMvcConfigurations =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_WEB_MVC_CONFIGURATION);
    for (ClassOrInterfaceTypeDetails webMvcConfigurationDetails : webMvcConfigurations) {
      final LogicalPath logicalPath =
          PhysicalTypeIdentifier.getPath(webMvcConfigurationDetails.getDeclaredByMetadataId());
      final String webMvcConfigurationMetadataKey =
          WebMvcConfigurationMetadata.createIdentifier(webMvcConfigurationDetails.getType(),
              logicalPath);
      registerDependency(metadataIdentificationString, webMvcConfigurationMetadataKey);
    }

    // Register dependency with RooJavaBean
    Set<ClassOrInterfaceTypeDetails> rooJavabeans =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_JAVA_BEAN);
    for (ClassOrInterfaceTypeDetails rooJavaBean : rooJavabeans) {
      final LogicalPath logicalPath =
          PhysicalTypeIdentifier.getPath(rooJavaBean.getDeclaredByMetadataId());
      final String rooJavaBeanMetadataKey =
          JavaBeanMetadata.createIdentifier(rooJavaBean.getType(), logicalPath);
      registerDependency(rooJavaBeanMetadataKey, metadataIdentificationString);
    }

    return new FormatterMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, entity, identifierType, accessors, service);
  }

  /**
   * This method obtain accessors of a provided entity and all its superclasses
   * 
   * @param metadataIdentificationString
   * @param entity
   * @return
   */
  public List<MethodMetadata> getEntityAccessors(String metadataIdentificationString,
      JavaType entity) {
    List<MethodMetadata> accessors = new ArrayList<MethodMetadata>();

    // Getting entity details
    ClassOrInterfaceTypeDetails entityDetails = getTypeLocationService().getTypeDetails(entity);

    LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(entityDetails.getDeclaredByMetadataId());
    String javaBeanMetadataKey =
        JavaBeanMetadata.createIdentifier(entityDetails.getType(), logicalPath);
    JavaBeanMetadata javaBeanMetadata =
        (JavaBeanMetadata) getMetadataService().get(javaBeanMetadataKey);

    if (javaBeanMetadata.getAccesorMethods() != null) {
      // Get accessors
      List<MethodMetadata> javaBeanAccessors = javaBeanMetadata.getAccesorMethods();
      for (MethodMetadata method : javaBeanAccessors) {
        JavaType returnType = method.getReturnType();
        ClassOrInterfaceTypeDetails typeDetails =
            getTypeLocationService().getTypeDetails(returnType);
        // Ignore Set, List, Dates and Entity fields
        if (returnType.getFullyQualifiedTypeName().equals(Set.class.getName())
            || returnType.getFullyQualifiedTypeName().equals(List.class.getName())
            || returnType.getFullyQualifiedTypeName().equals(Date.class.getName())
            || returnType.getFullyQualifiedTypeName().equals(Calendar.class.getName())
            || typeDetails != null) {
          continue;
        }
        accessors.add(method);
      }
    }

    // Check if entity has superclass, to get accessors
    while (entityDetails.getSuperclass() != null) {

      entityDetails = entityDetails.getSuperclass();

      logicalPath = PhysicalTypeIdentifier.getPath(entityDetails.getDeclaredByMetadataId());
      javaBeanMetadataKey = JavaBeanMetadata.createIdentifier(entityDetails.getType(), logicalPath);
      javaBeanMetadata = (JavaBeanMetadata) getMetadataService().get(javaBeanMetadataKey);

      if (javaBeanMetadata != null && javaBeanMetadata.getAccesorMethods() != null) {
        // Get accessors
        List<MethodMetadata> javaBeanAccessors = javaBeanMetadata.getAccesorMethods();
        for (MethodMetadata method : javaBeanAccessors) {
          JavaType returnType = method.getReturnType();
          ClassOrInterfaceTypeDetails typeDetails =
              getTypeLocationService().getTypeDetails(returnType);
          // Ignore Set, List, Dates and Entity fields
          if (returnType.getFullyQualifiedTypeName().equals(Set.class.getName())
              || returnType.getFullyQualifiedTypeName().equals(List.class.getName())
              || returnType.getFullyQualifiedTypeName().equals(Date.class.getName())
              || returnType.getFullyQualifiedTypeName().equals(Calendar.class.getName())
              || typeDetails != null) {
            continue;
          }
          accessors.add(method);
        }
      }
    }

    return accessors;
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
    return FormatterMetadata.getMetadataIdentiferType();
  }
}
