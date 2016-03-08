package org.springframework.roo.addon.finder.addon.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.details.FieldMetadata;

/**
 * This class is based on OrderBySource.java class from Spring Data commons project. 
 * 
 * It has some little changes to be able to work properly on Spring Roo project
 * and make easy Spring Data query parser.
 * 
 * Get more information about original class on:
 * 
 * https://github.com/spring-projects/spring-data-commons/blob/master/src/main/java/org/springframework/data/repository/query/parser/OrderBySource.java
 * 
 * Represents an order clause, which is set after {@literal OrderBy} token. 
 * It expects the last part of the query to be given and supports order by several properties ending with its sorting {@link Direction}. 
 * 
 * @author Paula Navarro
 * @author Juan Carlos García
 * @since 2.0
 */
public class OrderBySource {

  private static final String BLOCK_SPLIT = "(?<=Asc|Desc)(?=\\p{Lu}|\\z)";
  private static final Pattern DIRECTION_SPLIT = Pattern.compile("(.*?)(Asc|Desc)?$");
  private static final String INVALID_ORDER_SYNTAX = "Invalid order syntax for part %s!";
  private static final Set<String> DIRECTION_KEYWORDS = new HashSet<String>(Arrays.asList("Asc",
      "Desc"));

  private final List<Order> orders;
  private List<FieldMetadata> fields;

  private final PartTree currentPartTreeInstance;


  /**
   * Creates a new {@link OrderBySource} for the given clause, checking the property referenced exists on the given
   * entity properties.
   * 
   * @param partTree PartTree instance where current OrderBySource will be defined
   * @param clause must not be {@literal null}.
   * @param fields entity properties must not be {@literal null}.
   */
  public OrderBySource(PartTree partTree, String clause, List<FieldMetadata> fields) {

    Validate.notNull(partTree, "ERROR: PartTree instance is necessary to generate OrderBy");
    Validate.notNull(clause, "ERROR: Clause can not be null");
    Validate.notNull(fields, "ERROR: Entity properties can not be null");

    this.currentPartTreeInstance = partTree;
    this.orders = new ArrayList<Order>();
    this.fields = fields;

    // Extract order properties
    for (String part : clause.split(BLOCK_SPLIT, -1)) {

      Matcher matcher = DIRECTION_SPLIT.matcher(part);

      if (!matcher.find()) {
        throw new IllegalArgumentException(String.format(INVALID_ORDER_SYNTAX, part));
      }

      String propertyString = matcher.group(1);
      String directionString = matcher.group(2);

      // No property, but only a direction keyword
      if (DIRECTION_KEYWORDS.contains(propertyString) && directionString == null) {
        throw new RuntimeException(String.format(INVALID_ORDER_SYNTAX, part));
      }

      // Invalid direction
      if (directionString != null && Direction.fromString(directionString) == null) {
        throw new RuntimeException("Invalid direction " + directionString);
      }

      Direction direction =
          StringUtils.isNotBlank(directionString) ? Direction.fromString(directionString) : null;
      this.orders.add(new Order(direction, currentPartTreeInstance.extractValidProperty(
          propertyString, fields)));
    }
  }



  /*
   * (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "OrderBy" + StringUtils.join(orders, "");
  }


  /**
   * Returns the different order clauses that can be build based on the current order clause defined.
   * Query is added as options prefix.
   * 
   * @param query prefix to be added to the options
   * @return
   */
  public List<String> getOptions(String query) {

    List<String> options = new ArrayList<String>();

    // Get the last order expression
    Order lastOrder = orders.get(orders.size() - 1);

    // Check if it has a property to order by
    if (!lastOrder.hasProperty()) {
      for (FieldMetadata field : fields) {
        options.add(query.concat(StringUtils.capitalize(field.getFieldName().toString())));
      }

      // Once an order expression is defined, order clause can end if no more properties are added
      if (orders.size() > 1) {
        options.add(query.concat(""));
      }

    } else {

      // Show order directions after the property
      options.add(query.concat(Direction.ASC.getKeyword()));
      options.add(query.concat(Direction.DESC.getKeyword()));

      // If property is a reference to other entity, related entity properties can be added
      List<FieldMetadata> fields =
          currentPartTreeInstance.getValidProperties(lastOrder.getProperty().getLeft()
              .getFieldType());

      if (fields != null) {
        for (FieldMetadata field : fields) {
          options.add(query.concat(StringUtils.capitalize(field.getFieldName().toString())));
        }
      }
    }

    return options;
  }


}
