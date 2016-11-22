package org.springframework.roo.addon.web.mvc.controller.addon;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.classpath.MetadataLocatorUtils;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The {@link ControllerLocator} implementation.
 *
 * @author Jose Manuel Vivó
 * @since 2.0.0
 */
@Component
@Service
public class ControllerLocatorImpl implements ControllerLocator {

  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private MetadataDependencyRegistry dependencyRegistry;

  private MetadataLocatorUtils<ControllerType> util;

  /**
   * Cache for {@link ControllerAnnotationValues} by controller javaType
   */
  private Map<JavaType, ControllerAnnotationValues> valuesCache =
      new HashMap<JavaType, ControllerAnnotationValues>(30);

  protected void activate(final ComponentContext cContext) {
    util = new MetadataLocatorUtils<ControllerType>(new Evaluator());
    dependencyRegistry.addNotificationListener(util);
  }

  public Collection<ClassOrInterfaceTypeDetails> getControllers(final JavaType domainType) {
    return util.getValue(domainType, null);
  }

  public Collection<ClassOrInterfaceTypeDetails> getControllers(final JavaType domainType,
      ControllerType type) {
    return util.getValue(domainType, type);
  }

  @Override
  public Collection<ClassOrInterfaceTypeDetails> getControllers(JavaType domainType,
      ControllerType type, JavaType viewType) {
    final Collection<ClassOrInterfaceTypeDetails> found = getControllers(domainType, type);
    if (found.isEmpty()) {
      return found;
    }
    Collection<ClassOrInterfaceTypeDetails> result = new HashSet<ClassOrInterfaceTypeDetails>();
    for (ClassOrInterfaceTypeDetails item : found) {
      AnnotationMetadata annotation = item.getAnnotation(viewType);
      if (annotation == null) {
        continue;
      }
      result.add(item);
    }
    return result;
  }


  private class Evaluator implements MetadataLocatorUtils.LocatorEvaluator<ControllerType> {

    @Override
    public boolean evaluateForKey(JavaType key, ClassOrInterfaceTypeDetails valueToEvalueate,
        ControllerType context) {
      final JavaType controller = valueToEvalueate.getType();
      ControllerAnnotationValues values = valuesCache.get(controller);
      if (values == null) {
        values = new ControllerAnnotationValues(valueToEvalueate);
        valuesCache.put(controller, values);
      }
      // Get annotation type enum value
      ControllerType controllerType = values.getType();
      JavaType entity = values.getEntity();
      if (entity.equals(key)) {
        if (context == null) {
          return true;
        } else {
          return controllerType.equals(context);
        }
      }
      return false;
    }

    @Override
    public JavaType evalueteForEvict(final String streamDependency) {
      if (ControllerMetadata.isValid(streamDependency)) {
        final JavaType controller = ControllerMetadata.getJavaType(streamDependency);
        if (controller != null) {
          valuesCache.remove(controller);
          return controller;
        }
      }
      return null;
    }

    @Override
    public Set<ClassOrInterfaceTypeDetails> getAllPosibilities(ControllerType context) {
      Set<ClassOrInterfaceTypeDetails> found =
          typeLocationService
              .findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_CONTROLLER);
      Set<ClassOrInterfaceTypeDetails> result = new HashSet<ClassOrInterfaceTypeDetails>();
      for (ClassOrInterfaceTypeDetails item : found) {
        ControllerAnnotationValues values = new ControllerAnnotationValues(item);
        if (values.getType() == context) {
          result.add(item);
        }
      }
      return result;
    }
  }
}
