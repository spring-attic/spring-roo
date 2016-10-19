package org.springframework.roo.addon.layers.repository.jpa.addon;

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

import java.util.Collection;

/**
 * The {@link RepositoryJpaLocator} implementation.
 *
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @author Jose Manuel Vivó
 * @since 1.2.0
 */
@Component
@Service
public class RepositoryJpaLocatorImpl implements RepositoryJpaLocator {

  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private MetadataDependencyRegistry dependencyRegistry;

  private MetadataLocatorUtils<Object> util = null;

  protected void activate(final ComponentContext cContext) {
    if (util == null) {
      util = new MetadataLocatorUtils<Object>(typeLocationService, new Evaluator());
    }
    dependencyRegistry.addNotificationListener(util);
  }

  public Collection<ClassOrInterfaceTypeDetails> getRepositories(final JavaType domainType) {
    return util.getValue(domainType, null);
  }

  @Override
  public ClassOrInterfaceTypeDetails getRepository(JavaType domainType) {
    Validate.notNull(domainType, "domainType is required");
    Collection<ClassOrInterfaceTypeDetails> repositories = getRepositories(domainType);
    if (repositories.isEmpty()) {
      return null;
    } else if (repositories.size() > 1) {
      throw new IllegalStateException("Found " + repositories.size() + " repositories for "
          + domainType.getFullyQualifiedTypeName());
    }
    return repositories.iterator().next();
  }

  @Override
  public ClassOrInterfaceTypeDetails getFirstRepository(JavaType domainType) {
    Validate.notNull(domainType, "domainType is required");
    Collection<ClassOrInterfaceTypeDetails> repositories = getRepositories(domainType);
    if (repositories.isEmpty()) {
      return null;
    }
    return repositories.iterator().next();
  }

  private class Evaluator implements MetadataLocatorUtils.LocatorEvaluator<Object> {

    @Override
    public boolean evaluateForKey(JavaType key, ClassOrInterfaceTypeDetails valueToEvalueate,
        Object context) {
      final RepositoryJpaAnnotationValues annotationValues =
          new RepositoryJpaAnnotationValues(new DefaultPhysicalTypeMetadata(
              valueToEvalueate.getDeclaredByMetadataId(),
              typeLocationService.getPhysicalTypeCanonicalPath(valueToEvalueate
                  .getDeclaredByMetadataId()), valueToEvalueate));
      return annotationValues.getEntity() != null && annotationValues.getEntity().equals(key);
    }

    @Override
    public JavaType evalueteForEvict(String streamDependency) {
      if (RepositoryJpaMetadata.isValid(streamDependency)) {
        return RepositoryJpaMetadata.getJavaType(streamDependency);
      }
      return null;
    }
  }
}
