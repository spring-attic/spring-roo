package org.springframework.roo.addon.finder.addon.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.roo.classpath.details.FieldMetadata;

/**
 * Represents the predicate part of a query.
 * Predicate is used to define expressions or conditions on entity properties and concatenate them with And/Or. 
 * It is composed by {@link OrPart}s consisting of simple {@link Part} instances in turn. 
 * Optionally, a static ordering can be applied by appending an {@literal OrderBy} clause that references properties and by providing a sorting direction.
 * These conditions are properties combined with operators.  
 * 
 * @author Paula Navarro
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

  private static final Pattern COMPLETE_QUERY_TEMPLATE =
      Pattern
          .compile("^(\\p{Lu}.*?(\\p{Lu}.*?)?)((And|Or)(\\p{Lu}.*?(\\p{Lu}.*?)))*(AllIgnoreCase|AllIgnoringCase)?(OrderBy((\\p{Lu}.*?)(Asc|Desc))+)?\\z");


  /**
   * Builds a predicate from a source by creating search expressions and the order clause.
   * 
   * @param source
   * @param fields
   */
  public Predicate(String source, List<FieldMetadata> fields) {

    this.fields = fields;

    // Extracts AllIgnoreCase option and splits predicate between search expressions and order clause
    String[] parts = PartTree.split(detectAndSetAllIgnoreCase(source), ORDER_BY, -1);

    if (parts.length > 2) {
      throw new RuntimeException("ERROR: OrderBy must not be used more than once in a method name");
    }

    // Builds search expressions
    buildTree(parts[0]);

    // Builds order clause
    this.orderBySource = parts.length == 2 ? new OrderBySource(parts[1], fields) : null;

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

      // If the options will be added after an Or expression, OrderBy keyword is added. It makes possible transform an Or into an OrderBy expression
      if (nodes.size() > 1) {
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

        } else if (lastAnd.shouldIgnoreCase() != IgnoreCaseType.ALWAYS
            && Arrays.asList(Type.PREFIX_GROUP).contains(lastAnd.getOperator())) {

          // If the condition has not an operator but a group operator is defined, all operators belonging to the group are included
          List<String> types = lastAnd.getSupportedOperators();
          for (String type : types) {
            options.add(subject.concat(type));
          }
        }

        if (lastAnd.shouldIgnoreCase() != IgnoreCaseType.ALWAYS) {
          options.add(subject.concat("IgnoreCase"));
          options.add(subject.concat("IgnoringCase"));
        }

        // If the property condition is a reference to other entity, the related entity fields are shown
        if (!lastAnd.hasOperator() && lastAnd.shouldIgnoreCase() != IgnoreCaseType.ALWAYS) {
          List<FieldMetadata> fields =
              PartTree.getValidProperties(lastAnd.getProperty().getLeft().getFieldType());

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
   * Detects  AllIgnoreCase option and removes it from predicate.
   * 
   * @param predicate 
   * @return predicate predicate without AllIgnoreCase option.
   */
  private String detectAndSetAllIgnoreCase(String predicate) {

    Matcher matcher = ALL_IGNORE_CASE.matcher(predicate);

    if (matcher.find()) {
      alwaysIgnoreCase = true;

      // Save which option has been used (AllIgnoreCase or AllIgnoringCase)
      alwaysIgnoreCaseString = matcher.group(0);
      predicate =
          predicate.substring(0, matcher.start())
              + predicate.substring(matcher.end(), predicate.length());
    }

    return predicate;
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
      if (i > 0 && split[i - 1].length() == 0) {
        throw new RuntimeException("ERROR: Missing expression before Or");
      }
      nodes.add(new OrPart(split[i], fields));
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


}
