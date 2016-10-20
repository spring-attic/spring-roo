package org.springframework.roo.addon.layers.service.addon;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.MetadataLocatorUtils;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultPhysicalTypeMetadata;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

import java.util.Collection;

/**
 * The {@link ServiceLocator} implementation.
 *
 * @author Jose Manuel Vivó
 * @since 2.0.0
 */
@Component
@Service
public class ServiceLocatorImpl implements ServiceLocator {

  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private MetadataDependencyRegistry dependencyRegistry;

  private MetadataLocatorUtils<JavaType> util;

  protected void activate(final ComponentContext cContext) {
    util = new MetadataLocatorUtils<JavaType>(new Evaluator(typeLocationService));
    dependencyRegistry.addNotificationListener(util);
  }

  public Collection<ClassOrInterfaceTypeDetails> getServices(final JavaType domainType) {
    return util.getValue(domainType, RooJavaType.ROO_SERVICE);
  }

  @Override
  public ClassOrInterfaceTypeDetails getService(final JavaType domainType) {
    Validate.notNull(domainType, "domainType is required");
    Collection<ClassOrInterfaceTypeDetails> repositories = getServices(domainType);
    if (repositories.isEmpty()) {
      return null;
    } else if (repositories.size() > 1) {
      throw new IllegalStateException("Found " + repositories.size() + " services for "
          + domainType.getFullyQualifiedTypeName());
    }
    return repositories.iterator().next();
  }

  @Override
  public ClassOrInterfaceTypeDetails getFirstService(final JavaType domainType) {
    Validate.notNull(domainType, "domainType is required");
    Collection<ClassOrInterfaceTypeDetails> repositories = getServices(domainType);
    if (repositories.isEmpty()) {
      return null;
    }
    return repositories.iterator().next();
  }

  public Collection<ClassOrInterfaceTypeDetails> getServiceImpls(final JavaType serviceType) {
    return util.getValue(serviceType, RooJavaType.ROO_SERVICE_IMPL);
  }

  @Override
  public ClassOrInterfaceTypeDetails getServiceImpl(final JavaType serviceType) {
    Validate.notNull(serviceType, "serviceType is required");
    Collection<ClassOrInterfaceTypeDetails> repositories = getServices(serviceType);
    if (repositories.isEmpty()) {
      return null;
    } else if (repositories.size() > 1) {
      throw new IllegalStateException("Found " + repositories.size() + " serviceImpl for "
          + serviceType.getFullyQualifiedTypeName());
    }
    return repositories.iterator().next();
  }

  @Override
  public ClassOrInterfaceTypeDetails getFirstServiceImpl(final JavaType serviceType) {
    Validate.notNull(serviceType, "serviceType is required");
    Collection<ClassOrInterfaceTypeDetails> repositories = getServices(serviceType);
    if (repositories.isEmpty()) {
      return null;
    }
    return repositories.iterator().next();
  }

  private class Evaluator extends MetadataLocatorUtils.LocatorEvaluatorByAnnotation {

    public Evaluator(TypeLocationService typeLocationService) {
      super(typeLocationService);
    }

    @Override
    public boolean evaluateForKey(JavaType key, ClassOrInterfaceTypeDetails valueToEvalueate,
        JavaType context) {
      if (RooJavaType.ROO_SERVICE.equals(context)) {
        final ServiceAnnotationValues annotationValues =
            new ServiceAnnotationValues(new DefaultPhysicalTypeMetadata(
                valueToEvalueate.getDeclaredByMetadataId(),
                typeLocationService.getPhysicalTypeCanonicalPath(valueToEvalueate
                    .getDeclaredByMetadataId()), valueToEvalueate));
        return annotationValues.getEntity() != null && annotationValues.getEntity().equals(key);
      }
      if (RooJavaType.ROO_SERVICE_IMPL.equals(context)) {
        final ServiceImplAnnotationValues annotationValues =
            new ServiceImplAnnotationValues(new DefaultPhysicalTypeMetadata(
                valueToEvalueate.getDeclaredByMetadataId(),
                typeLocationService.getPhysicalTypeCanonicalPath(valueToEvalueate
                    .getDeclaredByMetadataId()), valueToEvalueate));
        return annotationValues.getService() != null && annotationValues.getService().equals(key);
      }
      return false;
    }

    @Override
    public JavaType evalueteForEvict(String streamDependency) {
      if (ServiceMetadata.isValid(streamDependency)) {
        return ServiceMetadata.getJavaType(streamDependency);
      } else if (ServiceImplMetadata.isValid(streamDependency)) {
        return ServiceImplMetadata.getJavaType(streamDependency);
      }
      return null;
    }
  }
}
