package org.springframework.roo.classpath;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.model.JavaType;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility class which handles cache of a MetadataLocator
 *
 * @author Jose Manuel Vivó
 * @since 2.0.0
 */
public class MetadataLocatorUtils<CONTEXT> implements MetadataNotificationListener {

  /**
   * This interface is used by {@link MetadataLocatorUtils} to evaluate
   * which values match
   *
   * @author Jose Manuel Vivó
   *
   * @param <CONTEXT> context class used if more than one types
   */
  public interface LocatorEvaluator<CONTEXT> {

    /**
     * Should return true if value is suitable for key
     * for required context
     *
     * @param key
     * @param valueToEvalueate
     * @param context
     * @return
     */
    boolean evaluateForKey(JavaType key, ClassOrInterfaceTypeDetails valueToEvalueate,
        CONTEXT context);

    /**
     * Get key to evict based on metadata dependency id
     *
     * @param streamDependency
     * @return JavaType to evict or null if doesn't need to handle
     */
    JavaType evalueteForEvict(String streamDependency);

    /**
     * Returns all details which can match to context
     *
     * @param context
     * @return
     */
    Set<ClassOrInterfaceTypeDetails> getAllPosibilities(CONTEXT context);
  }

  private final LocatorEvaluator<CONTEXT> evaluator;

  private final Map<JavaType, Map<CONTEXT, Set<ClassOrInterfaceTypeDetails>>> cacheMap =
      new HashMap<JavaType, Map<CONTEXT, Set<ClassOrInterfaceTypeDetails>>>();

  /**
   * stores current cache values with cacheMap keys.
   * Useful to evict cache
   */
  private final Map<JavaType, JavaType> cacheMapInverse = new HashMap<JavaType, JavaType>();


  /**
   * Default constructor
   *
   * @param locator
   */
  public MetadataLocatorUtils(LocatorEvaluator<CONTEXT> locator) {
    this.evaluator = locator;
  }

  @Override
  public void notify(String upstreamDependency, String downstreamDependency) {
    checkEvictCache(evaluator.evalueteForEvict(downstreamDependency));
  }

  /**
   * Evict cache from cacheMap if type is found on cacheMapInverse
   *
   * @param type
   */
  public void checkEvictCache(final JavaType type) {
    if (type == null) {
      return;
    }
    final JavaType mainType = cacheMapInverse.remove(type);
    if (mainType != null) {
      cacheMap.remove(mainType);
    }
  }

  /**
   * Get value for type
   *
   * Use cache value if any.
   *
   * @param type
   * @param context
   * @return
   */
  public Collection<ClassOrInterfaceTypeDetails> getValue(final JavaType type, final CONTEXT context) {
    Map<CONTEXT, Set<ClassOrInterfaceTypeDetails>> currentMap;
    if (type == null) {
      return evaluator.getAllPosibilities(context);
    }
    if (!cacheMap.containsKey(type)) {
      currentMap = new HashMap<CONTEXT, Set<ClassOrInterfaceTypeDetails>>();
      cacheMap.put(type, currentMap);
    } else {
      currentMap = cacheMap.get(type);
    }
    if (!currentMap.containsKey(context)) {
      currentMap.put(context, new HashSet<ClassOrInterfaceTypeDetails>());
    }
    final Set<ClassOrInterfaceTypeDetails> existing = currentMap.get(context);
    final Set<ClassOrInterfaceTypeDetails> located = evaluator.getAllPosibilities(context);
    if (existing.containsAll(located)) {
      return existing;
    }

    final Map<String, ClassOrInterfaceTypeDetails> toReturn =
        new HashMap<String, ClassOrInterfaceTypeDetails>();
    for (final ClassOrInterfaceTypeDetails cid : located) {
      if (evaluator.evaluateForKey(type, cid, context)) {
        toReturn.put(cid.getDeclaredByMetadataId(), cid);
        cacheMapInverse.put(cid.getName(), type);
      }
    }
    existing.clear();
    existing.addAll(toReturn.values());
    return toReturn.values();
  }


  /**
   * = _LocatorEvaluatorByAnnotation_
   *
   * Abstract class which implements location by annotation
   *
   * @author Jose Manuel Vivó
   * @since 2.0.0
   */
  public static abstract class LocatorEvaluatorByAnnotation implements LocatorEvaluator<JavaType> {

    private final TypeLocationService typeLocationService;

    public LocatorEvaluatorByAnnotation(TypeLocationService typeLocationService) {
      this.typeLocationService = typeLocationService;
    }

    @Override
    public Set<ClassOrInterfaceTypeDetails> getAllPosibilities(JavaType context) {
      return typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(context);
    }

  }
}
