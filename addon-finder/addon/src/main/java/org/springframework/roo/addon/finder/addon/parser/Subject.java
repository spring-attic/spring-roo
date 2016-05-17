package org.springframework.roo.addon.finder.addon.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.roo.classpath.details.FieldMetadata;


/**
 * This class is based on Subject inner class located inside PartTree.java class from Spring Data commons project.
 * 
 * It has some little changes to be able to work properly on Spring Roo project
 * and make easy Spring Data query parser.
 * 
 * Get more information about original class on:
 * 
 * https://github.com/spring-projects/spring-data-commons/blob/master/src/main/java/org/springframework/data/repository/query/parser/PartTree.java
 * 
 * Represents the subject part of the query. A query subject is enclosed between a database operation (query, read or find) and "By" token.
 *  E.g. {@code findDistinctUserByNameOrderByAge} would have the subject {@code DistinctUser}.
 *  Subject can have optional expressions like Distinct, limiting expressions such as First and Top, and a property which will be the result to return
 * 
 * @author Paula Navarro
 * @author Juan Carlos García
 * @since 2.0
 */
public class Subject {

  // Supported query operations to read data
  private static final String[] QUERY_TYPE = {"find", "query", "read"};

  public static final String QUERY_PATTERN = StringUtils.join(QUERY_TYPE, "|");
  public static final String COUNT_PATTERN = "count";
  private static final String DISTINCT = "Distinct";
  private static final String LIMITING_QUERY_PATTERN = "((First|Top)(\\d+)?)?";

  // Complete subject structures
  private static final Pattern COMPLETE_COUNT_BY_TEMPLATE = Pattern.compile("^" + COUNT_PATTERN
      + "(\\p{Lu}.*?)??By");
  private static final Pattern COMPLETE_QUERY_TEMPLATE = Pattern.compile("^(" + QUERY_PATTERN
      + ")(" + DISTINCT + ")?" + LIMITING_QUERY_PATTERN + "(\\p{Lu}.*?)??By");

  // Accepted subject structure to extract parameters
  private static final Pattern CORRECT_SUBJECT_TEMPLATE = Pattern.compile("^(" + QUERY_PATTERN
      + "|" + COUNT_PATTERN + ")?(" + DISTINCT + ")?(First|Top)?(\\d*)?(\\p{Lu}.*)?");

  // Template to check if subject is a count projection
  private static final Pattern COUNT_BY_TEMPLATE = Pattern.compile("^" + COUNT_PATTERN
      + "(\\p{Lu}.*)?");

  private boolean distinct;
  private Integer maxResults;
  private String operation = "";
  private String limit = "";
  private boolean isComplete = false;
  private boolean isCount = false;
  private Pair<FieldMetadata, String> property = null;
  private List<FieldMetadata> fields;

  private final PartTree currentPartTreeInstance;

  /**
   * Extracts the subject expressions from a source and builds a structure which represents it.
   * 
   * @param partTree PartTree instance where current Subject will be defined
   * @param source subject query
   * @param fields entity properties
   */
  public Subject(PartTree partTree, String source, List<FieldMetadata> fields) {

    Validate.notNull(partTree, "ERROR: PartTree instance is necessary to generate Subject");
    Validate.notNull(source, "ERROR: Subject source must not be null.");

    this.currentPartTreeInstance = partTree;
    this.fields = fields;
    this.isComplete = isValid(source);
    this.isCount = PartTree.matches(source, COUNT_BY_TEMPLATE);

    if (isCount) {
      buildCountSubject(source);
    } else {
      buildQuerySubject(source);
    }
  }

  /**
   * Extract count parameters from subject
   * 
   * @param subject
   */
  private void buildCountSubject(String subject) {
    Matcher grp = CORRECT_SUBJECT_TEMPLATE.matcher(subject);

    // Checks if query format and parameters are correct (not complete)
    if (!grp.find()) {
      return;
    }

    // Extract query type
    operation = grp.group(1);

    // Extract property
    property = extractValidField(grp.group(5), fields);

    distinct = false;
    limit = null;
    maxResults = null;
  }

  /**
   * Extract query parameters from subject
   * 
   * @param subject
   */
  private void buildQuerySubject(String subject) {
    Matcher grp = CORRECT_SUBJECT_TEMPLATE.matcher(subject);

    // Checks if query format and parameters are correct (not complete)
    if (!grp.find()) {
      return;
    }

    // Extract query type
    operation = grp.group(1);

    // Extract Distinct
    this.distinct = subject == null ? false : subject.contains(DISTINCT);

    // Extract if there is a limitation  expression
    limit = grp.group(3);

    if (limit != null) {
      maxResults = StringUtils.isNotBlank(grp.group(4)) ? Integer.valueOf(grp.group(4)) : null;
      if (maxResults != null && maxResults == 0) {
        throw new IllegalArgumentException("ERROR: Query max results cannot be 0");
      }
    }

    // Extract property
    property = extractValidField(grp.group(5), fields);

    // Check If property starts with reserved words
    if (property == null && maxResults == null) {

      if (distinct) {
        if (limit == null) {
          property = extractValidField(DISTINCT + grp.group(5), fields);

          if (property != null) {
            distinct = false;
          }

        } else if (maxResults == null) {
          property = extractValidField(DISTINCT + limit + grp.group(5), fields);

          if (property != null) {
            limit = null;
            distinct = false;
          }
        }
      } else if (limit != null) {
        property = extractValidField(limit + grp.group(5), fields);

        if (property != null) {
          limit = null;
        }
      }
    }

  }

  /**
   * Extracts the property defined in source. If any property is found returns null.
   * @param property
   * @return Pair of property metadata and property name
   */
  public Pair<FieldMetadata, String> extractValidField(String source, List<FieldMetadata> fields) {
    if (source == null) {
      return null;
    }

    source = StringUtils.substringBefore(source, "By");

    return currentPartTreeInstance.extractValidProperty(source, fields);
  }


  /**
   * Returns whether we indicate distinct lookup of entities.
   * 
   * @return {@literal true} if distinct
   */
  public boolean isDistinct() {
    return distinct;
  }

  /**
   * Returns whether a count projection shall be applied.
   * 
   * @return
   */
  public Boolean isCountProjection() {
    return isCount;
  }

  /**
   * Returns if subject is completed. Subject is complete if it has all
   * its expressions well-defined, starts with a query type (read, query
   * or find) and ends with "By" delimiter.
   * 
   * @return
   */
  public boolean isComplete() {
    return isComplete;
  }

  /**
   * Return the number of maximal results to return or {@literal null} if
   * not restricted.
   * 
   * @return
   */
  public Integer getMaxResults() {
    if (StringUtils.isBlank(limit)) {
      return null;
    }
    if (maxResults == null) {
      return 1;
    }
    return maxResults;
  }



  @Override
  public String toString() {
    return (operation != null ? operation.toLowerCase() : "").concat(isDistinct() ? DISTINCT : "")
        .concat(limit != null ? limit : "").concat(maxResults != null ? maxResults.toString() : "")
        .concat(property != null ? property.getRight() : "").concat(isComplete ? "By" : "");
  }

  /**
   * Returns the property metadata and name of this expression. 
   * If any property is defined, returns {@literal null}.
   * 
   * @return Pair of property metadata and property name
   */
  public Pair<FieldMetadata, String> getProperty() {
    return property;
  }


  /**
   * Returns true if subject expressions are well-defined (e.g. Distinct is defined before Top/First options) and the property belongs to the entity domain.
   * @return 
   */
  public boolean isValid() {
    return isValid(toString());
  }

  /**
   * Returns true if source is a well-defined subject. However, it does not validate if the property exist in the entity domain
   * @param source
   * @return  
   */
  public static boolean isValid(String source) {
    if (PartTree.matches(source, COUNT_BY_TEMPLATE)) {
      return PartTree.matches(source, COMPLETE_COUNT_BY_TEMPLATE);
    } else {
      return PartTree.matches(source, COMPLETE_QUERY_TEMPLATE);
    }
  }

  /**
   * Returns the different queries that can be build based on the current subject expressions.
   * The options are joined to the current query expression.
   * 
   * @return
   */
  public List<String> getOptions() {

    List<String> options = new ArrayList<String>();

    // Get current query to concatenate the new options
    String query = toString();

    // Checks if subject has an operation
    if (StringUtils.isBlank(operation)) {
      return Arrays.asList(ArrayUtils.add(QUERY_TYPE, COUNT_PATTERN));
    }

    // Once operation is included subject definition can end, so "By" option is always available
    options.add(query + "By");

    // Check if a property is included
    if (property == null) {

      // If subject does not have a property, all properties are available
      for (FieldMetadata field : fields) {
        options.add(query.concat(StringUtils.capitalize(field.getFieldName().toString())));
      }

      // Check if subject has a limiting expression. It can only be added before the property
      if (StringUtils.isBlank(limit) && !isCountProjection()) {
        options.add(query.concat("First"));
        options.add(query.concat("Top"));

        // Check if subject has Distinct expression. It can only be added before the property and limiting expression
        if (!isDistinct()) {
          options.add(query + DISTINCT);
        } else {
          // Add properties that start with reserved words
          for (FieldMetadata field : fields) {
            String name = StringUtils.capitalize(field.getFieldName().toString());
            if (name.startsWith(DISTINCT)) {
              options.add(query.concat(StringUtils.substringAfter(name, DISTINCT)));
            }
          }
        }

      } else if (maxResults == null && !isCountProjection()) {

        // Optionally, a limiting expression can have a number as parameter
        options.add(query + "[Number]");

        // Add properties that start with reserved words
        for (FieldMetadata field : fields) {
          String name = StringUtils.capitalize(field.getFieldName().toString());
          if (name.startsWith(limit)) {
            options.add(query.concat(StringUtils.substringAfter(name, limit)));
          } else if (isDistinct() && name.startsWith(DISTINCT.concat(limit))) {
            options.add(query.concat(StringUtils.substringAfter(name, DISTINCT.concat(limit))));
          }
        }
      }

    } else {
      // If the property is a reference to other entity, related entity properties are shown
      List<FieldMetadata> fields =
          currentPartTreeInstance.getValidProperties(property.getLeft().getFieldType());

      if (fields != null) {
        for (FieldMetadata relatedEntityfield : fields) {
          options.add(query.concat(StringUtils.capitalize(relatedEntityfield.getFieldName()
              .toString())));
        }
      }
    }

    return options;
  }

}
