package org.springframework.roo.addon.finder.addon.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.operations.jsr303.DateFieldPersistenceType;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaType;

/**
 * This class is based on PartTree.java class from Spring Data commons project.
 * 
 * It has some little changes to be able to work properly on Spring Roo project
 * and make easy Spring Data query parser.
 * 
 * Get more information about original class on:
 * 
 * https://github.com/spring-projects/spring-data-commons/blob/master/src/main/java/org/springframework/data/repository/query/parser/PartTree.java
 * 
 * Class to parse a {@link String} into a {@link Subject} and a {@link Predicate}.
 * Takes a entity details to extract the
 * properties of the domain class. The {@link PartTree} can then be used to
 * build queries based on its API instead of parsing the method name for each
 * query execution.
 * 
 * @author Paula Navarro
 * @author Juan Carlos García
 * @since 2.0
 */
public class PartTree {

  private static final String KEYWORD_TEMPLATE = "(%s)(?=(\\p{Lu}|\\z))";
  private static final Pattern PREFIX_TEMPLATE = Pattern.compile("^(" + Subject.QUERY_PATTERN + "|"
      + Subject.COUNT_PATTERN + ")((\\p{Lu}.*?))??By");

  /**
   * Subject is delimited by a prefix (find, read , query or count) and {@literal By} delimiter, for
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

  /**
   * Interface that provides operations to obtain useful information during finder autocomplete 
   */
  private final FinderAutocomplete finderAutocomplete;

  /**
   * Return type of generated finder
   */
  private JavaType returnType;

  /**
   * Return type provided in constructor when it is different from target entity. Can be null.
   */
  private JavaType providedReturnType;

  /**
   * Parameters of generated finder
   */
  List<FinderParameter> finderParameters;

  /**
   * Creates a new {@link PartTree} by parsing the given {@link String}.
   * 
   * @param source
   *            the {@link String} to parse
   * @param memberDetails
   *            the member details of the entity class to extract the fields
   *            to expose them as options.
   * @param finderAutocomplete interface that provides operations to obtain useful information during autocomplete 
   */
  public PartTree(String source, MemberDetails memberDetails,
      FinderAutocomplete finderAutocomplete, JavaType providedReturnType) {

    Validate.notNull(source, "Source must not be null");
    Validate.notNull(memberDetails, "MemberDetails must not be null");

    this.originalQuery = source;
    this.finderAutocomplete = finderAutocomplete;

    // Extracts entity fields removing persistence fields and list type
    // fields
    List<FieldMetadata> fields = getValidProperties(memberDetails.getFields());

    Matcher matcher = PREFIX_TEMPLATE.matcher(source);

    if (!matcher.find()) {
      this.subject = new Subject(this, source, fields);
      this.predicate = new Predicate(this, "", fields);
    } else {
      this.subject = new Subject(this, matcher.group(0), fields);
      this.predicate = new Predicate(this, source.substring(matcher.group().length()), fields);
    }

    this.providedReturnType = providedReturnType;

    this.returnType = extractReturnType(memberDetails);

    this.finderParameters = predicate.getParameters();

  }

  public PartTree(String source, MemberDetails memberDetails, FinderAutocomplete finderAutocomplete) {
    this(source, memberDetails, finderAutocomplete, null);
  }

  /**
   * Extracts the java type of the results to be returned by the PartTree query 
   * 
   * @param entityDetails the entity details to extract the object to return by default
   * @return
   */
  private JavaType extractReturnType(MemberDetails entityDetails) {

    Integer maxResults = subject.getMaxResults();
    Pair<FieldMetadata, String> property = subject.getProperty();
    JavaType type = null;

    // Count subject returns Long
    if (subject.isCountProjection()) {
      return JavaType.LONG_OBJECT;
    }

    if (property != null && property.getLeft() != null) {
      // Returns the property type if it is specified
      type = property.getLeft().getFieldType();

    } else if (providedReturnType != null) {
      type = providedReturnType;
    } else {

      // By default returns entity type
      List<MemberHoldingTypeDetails> details = entityDetails.getDetails();
      for (MemberHoldingTypeDetails detail : details) {
        if (finderAutocomplete != null
            && finderAutocomplete.getEntityDetails(detail.getType()).equals(entityDetails)) {
          type = detail.getType();
          break;
        } else {
          type = detail.getType();
        }
      }
    }


    // Check number of results to return. 
    if (maxResults != null && maxResults == 1) {
      // Unique result
      return type;
    }

    //If it is not an unique result, returns a list
    if (type.isPrimitive()) {
      // Lists cannot return primitive types, so primitive types are transformed into their wrapper class
      type =
          new JavaType(type.getFullyQualifiedTypeName(), type.getArray(), DataType.TYPE,
              type.getArgName(), type.getParameters(), type.getModule());
    }
    return new JavaType("java.util.List", 0, DataType.TYPE, null, Arrays.asList(type));

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
   * expressions. Persistence version property is excluded as well as multivalued properties 
   * since Spring Data does not support operations with them
   * 
   * @param memberDetails
   * @return entity properties which type is supported  by SpringData
   */
  private List<FieldMetadata> getValidProperties(List<FieldMetadata> fields) {

    List<FieldMetadata> validProperties = new ArrayList<FieldMetadata>();

    for (FieldMetadata fieldMetadata : fields) {

      // Check if its type is List/Map/etc
      if (fieldMetadata.getFieldType().isMultiValued())
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
   * expressions. Persistence version field is excluded, and multivalued fields
   * are removed since Spring Data does not supports operations with them.
   * 
   * @param javaType
   * @return entity properties which type is supported  by SpringData
   */
  public List<FieldMetadata> getValidProperties(JavaType javaType) {

    if (finderAutocomplete != null) {

      final MemberDetails entityDetails = finderAutocomplete.getEntityDetails(javaType);

      if (entityDetails != null) {
        return getValidProperties((List<FieldMetadata>) entityDetails.getFields());
      }

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
  public Pair<FieldMetadata, String> extractValidProperty(String rawProperty,
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
  private Pair<FieldMetadata, String> extractRelatedEntityValidProperty(String extractProperty,
      FieldMetadata referenceProperty) {

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

  /**
   * Returns whether a count projection shall be applied.
   * 
   * @return
   */
  public Boolean isCountProjection() {
    return subject.isCountProjection();
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

  /**
   * Method that obtains the return type of current finder 
   * 
   * @return JavaType with return type
   */
  public JavaType getReturnType() {
    return returnType;
  }

  /**
   * Method that obtains the necessary parameters of current finder
   * 
   * @return List that contains all necessary parameters
   */
  public List<FinderParameter> getParameters() {
    return finderParameters;
  }



}
