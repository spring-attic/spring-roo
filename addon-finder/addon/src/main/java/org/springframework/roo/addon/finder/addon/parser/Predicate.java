package org.springframework.roo.addon.finder.addon.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * This class is based on Predicate inner class located inside PartTree.java class from Spring Data commons project.
 * 
 * It has some little changes to be able to work properly on Spring Roo project
 * and make easy Spring Data query parser.
 * 
 * Get more information about original class on:
 * 
 * https://github.com/spring-projects/spring-data-commons/blob/master/src/main/java/org/springframework/data/repository/query/parser/PartTree.java
 * 
 * Represents the predicate part of a query.
 * Predicate is used to define expressions or conditions on entity properties and concatenate them with And/Or. 
 * It is composed by {@link OrPart}s consisting of simple {@link Part} instances in turn. 
 * Optionally, a static ordering can be applied by appending an {@literal OrderBy} clause that references properties and by providing a sorting direction.
 * These conditions are properties combined with operators.  
 * 
 * @author Paula Navarro
 * @author Juan Carlos García
 * @since 2.0
 */

public class Predicate {

  private static final Pattern ALL_IGNORE_CASE = Pattern.compile("AllIgnor(ing|e)Case");
  private static final String ORDER_BY = "OrderBy";

  private final List<OrPart> nodes = new ArrayList<OrPart>();
  private final OrderBySource orderBySource;
  private boolean alwaysIgnoreCase;
  private List<FieldMetadata> fields;
  private String alwaysIgnoreCaseString = "";
  private final PartTree currentPartTreeInstance;

  private static final Pattern COMPLETE_QUERY_TEMPLATE =
      Pattern
          .compile("^(\\p{Lu}.*?)((And|Or)(\\p{Lu}.*?))*(AllIgnoreCase|AllIgnoringCase)?(OrderBy((\\p{Lu}.*?)(Asc|Desc))+)?\\z");


  /**
   * Builds a predicate from a source by creating search expressions and the order clause.
   * 
   * @param partTree PartTree instance where current Predicate will be defined
   * @param source
   * @param fields
   */
  public Predicate(PartTree partTree, String source, List<FieldMetadata> fields) {

    Validate.notNull(partTree, "ERROR: PartTree instance is necessary to generate Subject");
    Validate.notNull(source, "ERROR: Predicate source must not be null.");

    this.currentPartTreeInstance = partTree;
    this.fields = fields;

    // Splits predicate between search expressions and order clause
    String[] parts = PartTree.split(source, ORDER_BY, -1);

    if (parts.length > 2) {
      throw new RuntimeException("ERROR: OrderBy must not be used more than once in a method name");
    }

    // Builds order clause
    this.orderBySource =
        parts.length == 2 ? new OrderBySource(currentPartTreeInstance, parts[1], fields) : null;

    // Extracts AllIgnoreCase option and builds search expressions
    buildTree(detectAndSetAllIgnoreCase(parts[0]));

  }

  /**
   * @return true if predicate has an order clause. Otherwise returns false.
   */
  public boolean hasOrderClause() {
    return orderBySource != null;
  }

  /**
   * Returns the different queries that can be defined from the current order clause.
   * The options are concatenated to the current query.
   * 
   * @param query that represents the subject and conditions 
   * @return
   */
  public List<String> getOrderOptions(String query) {
    return orderBySource.getOptions(query.concat(toString()));
  }


  /**
   * Returns the different queries that can be build based on the current predicate expressions.
   * The options are joined to the current query expression.
   * 
   * @param query that represents the subject information 
   * @return
   */
  public List<String> getOptions(String subject) {

    List<String> options = new ArrayList<String>();
    OrPart lastOr = null;
    Part lastAnd = null;

    // Extract the last condition
    if (!nodes.isEmpty()) {
      lastOr = nodes.get(nodes.size() - 1);
      lastAnd =
          lastOr.getChildren().isEmpty() ? null : lastOr.getChildren().get(
              lastOr.getChildren().size() - 1);
    }

    // Builds the current query by joining the subject expression with the predicate information
    subject = subject.concat(toString());

    // Check if last condition has a property
    if (lastAnd == null || !lastAnd.hasProperty()) {

      // If options are added after Or, OrderBy keyword is added. It makes possible transform an Or into an OrderBy expression
      if (subject.endsWith("Or")) {
        options.add(StringUtils.removeEnd(subject, "Or").concat(ORDER_BY));
      }

      // Show all fields with correct format
      for (FieldMetadata field : fields) {
        options.add(subject.concat(StringUtils.capitalize(field.getFieldName().toString())));
      }

    } else {

      // Once a property is defined, the query is complete and no more information is necessary but optional
      options.add(subject.concat(""));

      // Once a property is defined, OrderBy option can be added
      options.add(subject.concat(ORDER_BY));

      // AlwaysIgnoreCase option ends search criteria definition. 
      if (!alwaysIgnoreCase) {

        options.add(subject.concat("AllIgnoreCase"));
        options.add(subject.concat("AllIgnoringCase"));
        options.add(subject.concat("And"));
        options.add(subject.concat("Or"));


        if (!lastAnd.hasOperator()) {
          // If condition does not have an operator, optionally, it can be included
          List<String> types = lastAnd.getSupportedOperators();
          for (String type : types) {
            options.add(subject.concat(type));
          }

        } else if (lastAnd.shouldIgnoreCase() != IgnoreCaseType.ALWAYS) {

          // If the condition has an operator, but it also acts as prefix of other operators, they are included
          List<String> types = lastAnd.getSupportedOperatorsByPrefix(lastAnd.getOperator());
          for (String type : types) {
            if (StringUtils.isNotBlank(type)) {
              options.add(subject.concat(type));
            }
          }

        }

        // IgnoreCase option is available only for string properties
        if (lastAnd.shouldIgnoreCase() != IgnoreCaseType.ALWAYS
            && lastAnd.getProperty().getKey().getFieldType().equals(JavaType.STRING)) {
          options.add(subject.concat("IgnoreCase"));
          options.add(subject.concat("IgnoringCase"));
        }

        // If the property condition is a reference to other entity, the related entity fields are shown
        if (!lastAnd.hasOperator() && lastAnd.shouldIgnoreCase() != IgnoreCaseType.ALWAYS) {
          List<FieldMetadata> fields =
              currentPartTreeInstance.getValidProperties(lastAnd.getProperty().getLeft()
                  .getFieldType());

          if (fields != null) {
            for (FieldMetadata field : fields) {
              options.add(subject.concat(StringUtils.capitalize(field.getFieldName().toString())));
            }
          }
        }
      }
    }
    return options;
  }

  /**
   * Splits source by AllIgnoreCase option and returns information before this option.
   * 
   * @param source 
   * @return source previous to AllIgnoreCase option.
   */
  private String detectAndSetAllIgnoreCase(String source) {

    Matcher matcher = ALL_IGNORE_CASE.matcher(source);

    if (matcher.find()) {
      alwaysIgnoreCase = true;

      // Save which option has been used (AllIgnoreCase or AllIgnoringCase)
      alwaysIgnoreCaseString = matcher.group(0);
      source = source.substring(0, matcher.start());
    }

    return source;
  }

  /**
   * Splits source by Or{@literal Or} expressions and builds {@link OrPart}s which are simple conditions or expressions joined by And.
   * E.g. from the source "LastNameAndFirstNameOrAge" it will build the "LastNameAndFirstName" and "Age" OrderParts.
   * 
   * @param source the source to split up into {@literal Or} parts in turn.
   */
  private void buildTree(String source) {

    String[] split = PartTree.split(source, "Or", -1);
    for (int i = 0; i < split.length; i++) {

      // Validate previous expressions are correct
      if (i > 0 && StringUtils.isBlank(split[i - 1])) {
        throw new RuntimeException("ERROR: Missing expression before Or");
      }
      nodes.add(new OrPart(currentPartTreeInstance, split[i], fields));
    }

    // Validate expression before order clause is correct
    if (hasOrderClause()) {
      if (StringUtils.isBlank(split[split.length - 1])) {
        throw new RuntimeException("ERROR: Missing expression before OrderBy");
      }

      List<Part> parts = nodes.get(nodes.size() - 1).getChildren();
      if (!parts.get(parts.size() - 1).hasProperty()) {
        throw new RuntimeException("ERROR: Missing expression before OrderBy");
      }
    }
  }


  @Override
  public String toString() {
    return StringUtils.join(nodes, "Or").concat(alwaysIgnoreCaseString)
        .concat(hasOrderClause() ? orderBySource.toString() : "");
  }


  /**
   * Returns Order Clause information
   * @return
   */
  public OrderBySource getOrderBySource() {
    return orderBySource;
  }



  /**
   * Returns true if the predicate is well-defined, which means that its structure follows the SpringData rules and its condition properties belong to the entity domain.
   */
  public boolean IsValid() {

    for (OrPart orPart : nodes) {
      if (!orPart.isValid()) {
        return false;
      }
    }
    return IsValid(toString());
  }

  /**
   * Checks if source is a predicate well-defined,  which means that its structure follows the SpringData rules. However, it does not validates if the properties exist in the entity domain
   * @param source predicate
   * @return true if source matches the SpringData predicate definition. Otherwise returns false. 
   */
  public static boolean IsValid(String source) {
    if (PartTree.matches(source, COMPLETE_QUERY_TEMPLATE)) {
      return true;
    }
    return false;
  }

  /**
   * Builds the parameters that every condition needs to perform its operation
   * @return
   */
  public List<FinderParameter> getParameters() {
    List<FinderParameter> parameters = new ArrayList<FinderParameter>();

    // Tracks number of times a property name is used as parameter
    Map<FieldMetadata, Integer> parametersCount = new HashMap<FieldMetadata, Integer>();
    final Pattern lastIntPattern = Pattern.compile("[^0-9]+([0-9]+)$");

    for (OrPart orPart : nodes) {
      for (Part part : orPart.getChildren()) {

        if (part.getProperty() == null) {
          continue;
        }

        Integer count = parametersCount.get(part.getProperty().getLeft());

        for (FinderParameter parameter : part.getParameters()) {

          if (count != null) {

            // If a property has already been used as parameter name, we need to include a suffix to avoid duplicates
            String name = parameter.getName().toString();
            Matcher matcher = lastIntPattern.matcher(name);
            if (matcher.find()) {
              // Removes suffix if it already has one
              name = StringUtils.removeEnd(name, matcher.group(1));
            }
            count++;
            parameter.setName(new JavaSymbolName(name.concat(count.toString())));
          } else {
            count = 1;
          }

          // Update number of times a property has been used as parameter name
          parametersCount.put(part.getProperty().getLeft(), count);
          parameters.add(parameter);
        }
      }
    }
    return parameters;
  }


}
