package org.springframework.roo.addon.finder.addon.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * Class to parse a {@link String} into a {@link Subject} and a {@link Predicate}.
 * Takes a entity details to extract the
 * properties of the domain class. The {@link PartTree} can then be used to
 * build queries based on its API instead of parsing the method name for each
 * query execution.
 * 
 * @author Paula Navarro
 * @since 2.0
 */
public class PartTree {

  private static final String KEYWORD_TEMPLATE = "(%s)(?=(\\p{Lu}|\\z))";
  private static final Pattern PREFIX_TEMPLATE = Pattern.compile("^(" + Subject.QUERY_PATTERN
      + ")((\\p{Lu}.*?))??By");

  /**
   * Subject is delimited by the query prefix (find, read or query) and {@literal By} delimiter, for
   * example "findDistinctUserByNameOrderByAge" would have the subject
   * "DistinctUser".
   */
  private final Subject subject;

  /**
   * Predicate contains conditions, and optionally order clause subject. E.g. "findDistinctUserByNameOrderByAge" would have
   * the predicate "NameOrderByAge".
   */
  private final Predicate predicate;

  /**
   * Query used to generate the Subject and Predicate
   */
  private final String originalQuery;

  private static TypeLocationService typeLocationService;

  /**
   * Creates a new {@link PartTree} by parsing the given {@link String}.
   * 
   * @param source
   *            the {@link String} to parse
   * @param memberDetails
   *            the member details of the entity class to extract the fields
   *            to expose them as options.
   * @param typeLocationService the service used to inspect the properties of the entities that are related with the entity. If it is null, only properties which belong to entity class are shown as options
   */
  public PartTree(String source, MemberDetails memberDetails,
      TypeLocationService typeLocationService) {

    Validate.notNull(source, "Source must not be null");
    Validate.notNull(memberDetails, "MemberDetails must not be null");

    // Extracts entity fields removing persistence fields and list type
    // fields
    List<FieldMetadata> fields = getValidProperties(memberDetails.getFields());

    this.originalQuery = source;
    this.typeLocationService = typeLocationService;

    Matcher matcher = PREFIX_TEMPLATE.matcher(source);

    if (!matcher.find()) {
      this.subject = new Subject(source, fields);
      this.predicate = new Predicate("", fields);
    } else {
      this.subject = new Subject(matcher.group(0), fields);
      this.predicate = new Predicate(source.substring(matcher.group().length()), fields);
    }

  }

  /**
   * Creates a new {@link PartTree} by parsing the given {@link String}.
   * 
   * @param source
   *            the {@link String} to parse
   * @param memberDetails
   *            the member details of the entity class to extract the fields
   *            to expose them as options.
   */
  public PartTree(String source, MemberDetails memberDetails) {
    this(source, memberDetails, null);
  }



  /**
   * Filters the entity properties that can be used to build Spring Data
   * expressions. Persistence properties are excluded as well as multivalued properties 
   * since Spring Data does not support operations with them
   * 
   * @param memberDetails
   * @return entity properties which type is supported  by SpringData
   */
  private static List<FieldMetadata> getValidProperties(List<FieldMetadata> fields) {

    List<FieldMetadata> validProperties = new ArrayList<FieldMetadata>();

    for (FieldMetadata fieldMetadata : fields) {

      // Check if its type is List/Map/etc
      if (fieldMetadata.getFieldType().isMultiValued())
        continue;

      // Check if it is annotated with @Id
      if (fieldMetadata.getAnnotation(new JavaType("javax.persistence.Id")) != null)
        continue;

      // Check if it is annotated with @Version
      if (fieldMetadata.getAnnotation(new JavaType("javax.persistence.Version")) != null)
        continue;

      validProperties.add(fieldMetadata);
    }

    return validProperties;
  }

  /**
   * Filters the entity properties of a javaType that can be used to build Spring Data
   * expressions. Persistence fields are excluded, and multivalued fields
   * are removed since Spring Data does not supports operations with them.
   * 
   * If typeLocationService is not defined or javaType does not belongs to a valid entity, returns {@literal null}
   * 
   * @param javaType
   * @return entity properties which type is supported  by SpringData
   */
  public static List<FieldMetadata> getValidProperties(JavaType javaType) {

    if (typeLocationService == null || javaType == null) {
      return null;
    }
    final ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(javaType);

    if (cid != null && cid.getAnnotation(RooJavaType.ROO_JPA_ENTITY) != null) {
      return getValidProperties((List<FieldMetadata>) cid.getDeclaredFields());
    }

    return null;
  }


  /**
   * Extract entity property name from raw property and returns the property metadata and the property name.
   * If raw property references a property of a related entity, returns a Pair with the related entity property metadata and 
   * a string composed by the reference property name and the related entity property name. 
   * E.g. if raw property contains "petName" and current entity has a relation with Pet, it will return Pair(NameMetadata, "petName"))
   * 
   * @param rawProperty the string that contains property name
   * @param fields entity properties
   * @return Pair that contains the property metadata and the property name.
   */
  public static Pair<FieldMetadata, String> extractValidProperty(String rawProperty,
      List<FieldMetadata> fields) {

    if (StringUtils.isBlank(rawProperty) || fields == null) {
      return null;
    }
    FieldMetadata tempField = null;

    rawProperty = StringUtils.uncapitalize(rawProperty);

    // ExtractProperty can contain other information after property name. For that reason, it is necessary find the property that matches more letters with the property contained into extractProperty
    for (FieldMetadata field : fields) {
      if (field.getFieldName().toString().compareTo(rawProperty) == 0) {
        tempField = field;
        break;
      }
      if (rawProperty.startsWith(field.getFieldName().toString())) {
        if (tempField == null
            || tempField.getFieldName().toString().length() < field.getFieldName().toString()
                .length())
          tempField = field;
      }
    }

    if (tempField == null) {
      return null;
    }

    // If extracted property is a reference to other entity, the fields of this related entity are inspected to check if extractProperty contains information about them 
    Pair<FieldMetadata, String> related = extractRelatedEntityValidProperty(rawProperty, tempField);
    if (related != null) {
      return Pair.of(
          related.getLeft() == null ? tempField : related.getLeft(),
          StringUtils.capitalize(tempField.getFieldName().toString()).concat(
              StringUtils.capitalize(related.getRight())));
    }

    return Pair.of(tempField, StringUtils.capitalize(tempField.getFieldName().toString()));
  }

  /**
   * Gets the property of a related entity, using raw property information.
   * 
   * @param rawProperty string that contains the definition of a property, which can be a property accessed by a relation.
   * @param referenceProperty property that represents a relation with other entity.
   * @return Pair that contains a property metadata and its name.
   */
  private static Pair<FieldMetadata, String> extractRelatedEntityValidProperty(
      String extractProperty, FieldMetadata referenceProperty) {

    if (StringUtils.isBlank(extractProperty) || referenceProperty == null) {
      return null;
    }

    // Extract the property of a related entity
    String property =
        StringUtils.substringAfter(extractProperty, referenceProperty.getFieldName().toString());
    if (StringUtils.isBlank(property)) {
      return null;
    }

    return extractValidProperty(property, getValidProperties(referenceProperty.getFieldType()));

  }


  /**
   * Returns the different queries that can be build based on the current defined query.
   * First it lists the subject expressions that can be build. Once it is completed, it returns the queries available to
   * define the predicate.
   * 
   * @return
   */
  public List<String> getOptions() {
    if (!subject.isComplete()) {
      return subject.getOptions();
    } else if (!predicate.hasOrderClause()) {
      return predicate.getOptions(subject.toString());
    } else {
      return predicate.getOrderOptions(subject.toString());
    }
  }

  /**
   * Returns whether we indicate distinct lookup of entities.
   * 
   * @return {@literal true} if distinct
   */
  public boolean isDistinct() {
    return subject.isDistinct();
  }


  @Override
  public String toString() {
    return subject.toString().concat(predicate.toString());
  }



  /**
   * Splits the given text at the given keyword. Expects camel-case style to
   * only match concrete keywords and not derivatives of it.
   * 
   * @param text
   *            the text to split
   * @param keyword
   *            the keyword to split around
   * @param limit the limit controls the number of times the pattern is applied and therefore affects the length of the resulting array
   * @return an array of split items
   */
  static String[] split(String text, String keyword, int limit) {

    Pattern pattern = Pattern.compile(String.format(KEYWORD_TEMPLATE, keyword));
    return pattern.split(text, limit);
  }

  /**
   * Returns true if PartTree query is well-defined and the query generated is the same that the one used to build its structure.
   * @return
   */
  public boolean isValid() {
    return subject.isValid() && predicate.IsValid() && toString().equals(originalQuery);

  }

  /**
   * Returns true if query is well-defined, which means that subject and predicate have a correct structure. 
   * However, it does not validate if entity properties exist.
   * @return
   */
  public static boolean isValid(String query) {
    Matcher matcher = PREFIX_TEMPLATE.matcher(query);

    if (!matcher.find()) {
      return Subject.isValid(query) && Predicate.IsValid("");
    } else {
      return Subject.isValid(matcher.group(0))
          && Predicate.IsValid(query.substring(matcher.group().length()));
    }


  }

  /**
   * Returns the number of maximal results to return or {@literal null} if
   * not restricted.
   * 
   * @return
   */
  public Integer getMaxResults() {
    return subject.getMaxResults();
  }

  /**
   * Returns true if the query matches with the given {@link Pattern}. Otherwise, returns false.
   * If the query is null, returns false.
   * 
   * @param query
   * @param pattern
   * @return
   */
  public final static boolean matches(String query, Pattern pattern) {
    return query == null ? false : pattern.matcher(query).find();
  }



}
