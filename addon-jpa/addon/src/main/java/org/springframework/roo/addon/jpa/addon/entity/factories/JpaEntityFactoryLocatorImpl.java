package org.springframework.roo.addon.jpa.addon.entity.factories;

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
 * JpaEntityFactoryLocatorImpl
 * 
 * The JpaEntityFactoryLocator implementation.
 *
 * @author Sergio Clares
 * @since 2.0.0
 */
@Component
@Service
public class JpaEntityFactoryLocatorImpl implements JpaEntityFactoryLocator {

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
  public JavaType getFirstJpaEntityFactoryForEntity(JavaType entity) {
    Collection<ClassOrInterfaceTypeDetails> jpaEntityFactoriesForEntity =
        getJpaEntityFactoriesForEntity(entity);
    if (jpaEntityFactoriesForEntity.iterator().hasNext()) {
      return jpaEntityFactoriesForEntity.iterator().next().getType();
    }
    return null;
  }

  @Override
  public Collection<ClassOrInterfaceTypeDetails> getJpaEntityFactoriesForEntity(
      final JavaType domainType) {
    return util.getValue(domainType, RooJavaType.ROO_JPA_ENTITY_FACTORY);
  }

  @Override
  public JpaEntityFactoryMetadata getJpaEntityFactoryMetadata(JavaType entityFactory) {
    ClassOrInterfaceTypeDetails factoryDetails = typeLocationService.getTypeDetails(entityFactory);
    if (factoryDetails == null) {
      return null;
    }
    return metadataService.get(JpaEntityFactoryMetadata.createIdentifier(factoryDetails));
  }

  private class Evaluator extends MetadataLocatorUtils.LocatorEvaluatorByAnnotation {

    public Evaluator(TypeLocationService typeLocationService) {
      super(typeLocationService);
    }

    @Override
    public boolean evaluateForKey(JavaType key, ClassOrInterfaceTypeDetails valueToEvalueate,
        JavaType context) {
      final JpaEntityFactoryAnnotationValues annotationValues =
          new JpaEntityFactoryAnnotationValues(new DefaultPhysicalTypeMetadata(
              valueToEvalueate.getDeclaredByMetadataId(),
              typeLocationService.getPhysicalTypeCanonicalPath(valueToEvalueate
                  .getDeclaredByMetadataId()), valueToEvalueate));
      return annotationValues.getEntity() != null && annotationValues.getEntity().equals(key);
    }

    @Override
    public JavaType evalueteForEvict(String streamDependency) {
      if (JpaEntityFactoryMetadata.isValid(streamDependency)) {
        return JpaEntityFactoryMetadata.getJavaType(streamDependency);
      }
      return null;
    }
  }

}
