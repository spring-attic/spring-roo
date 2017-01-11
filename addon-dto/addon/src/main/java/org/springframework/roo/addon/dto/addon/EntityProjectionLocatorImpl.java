package org.springframework.roo.addon.dto.addon;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.MetadataLocatorUtils;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultPhysicalTypeMetadata;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

import java.util.Collection;

/**
 * = EntityProjectionLocatorImpl
 * 
 * The _EntityProjectionLocator_ implementation.
 *
 * @author Sergio Clares
 * @since 2.0.0
 */
@Component
@Service
public class EntityProjectionLocatorImpl implements EntityProjectionLocator {

  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private MetadataDependencyRegistry dependencyRegistry;
  @Reference
  private MetadataService metadataService;

  private MetadataLocatorUtils<JavaType> util;

  protected void activate(final ComponentContext cContext) {
    util = new MetadataLocatorUtils<JavaType>(new Evaluator(typeLocationService));
    dependencyRegistry.addNotificationListener(util);
  }

  @Override
  public Collection<ClassOrInterfaceTypeDetails> getEntityProjectionsForEntity(
      final JavaType domainType) {
    return util.getValue(domainType, RooJavaType.ROO_ENTITY_PROJECTION);
  }

  @Override
  public EntityProjectionMetadata getEntityProjectionMetadata(final JavaType entityProjection) {
    ClassOrInterfaceTypeDetails projectionDetails =
        typeLocationService.getTypeDetails(entityProjection);
    if (projectionDetails == null) {
      return null;
    }
    return metadataService.get(EntityProjectionMetadata.createIdentifier(projectionDetails));
  }

  private class Evaluator extends MetadataLocatorUtils.LocatorEvaluatorByAnnotation {

    public Evaluator(TypeLocationService typeLocationService) {
      super(typeLocationService);
    }

    @Override
    public boolean evaluateForKey(JavaType key, ClassOrInterfaceTypeDetails valueToEvalueate,
        JavaType context) {
      final EntityProjectionAnnotationValues annotationValues =
          new EntityProjectionAnnotationValues(new DefaultPhysicalTypeMetadata(
              valueToEvalueate.getDeclaredByMetadataId(),
              typeLocationService.getPhysicalTypeCanonicalPath(valueToEvalueate
                  .getDeclaredByMetadataId()), valueToEvalueate));
      return annotationValues.getEntity() != null && annotationValues.getEntity().equals(key);
    }

    @Override
    public JavaType evalueteForEvict(String streamDependency) {
      if (EntityProjectionMetadata.isValid(streamDependency)) {
        return EntityProjectionMetadata.getJavaType(streamDependency);
      }
      return null;
    }
  }

}
