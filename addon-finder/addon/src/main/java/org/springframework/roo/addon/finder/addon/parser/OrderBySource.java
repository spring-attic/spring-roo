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
 * Represents an order clause, which is set after {@literal OrderBy} token. 
 * It expects the last part of the query to be given and supports order by several properties ending with its sorting {@link Direction}. 
 * 
 * @author Paula Navarro
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


  /**
   * Creates a new {@link OrderBySource} for the given clause, checking the property referenced exists on the given
   * entity properties.
   * 
   * @param clause must not be {@literal null}.
   * @param fields entity properties must not be {@literal null}.
   */
  public OrderBySource(String clause, List<FieldMetadata> fields) {

    Validate.notNull(clause, "Clause can not be null");
    Validate.notNull(fields, "Entity properties can not be null");

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
      this.orders.add(new Order(direction, PartTree.extractValidProperty(propertyString, fields)));
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
      options.add(query.concat(Direction.ASC.keyword));
      options.add(query.concat(Direction.DESC.keyword));

      // If property is a reference to other entity, related entity properties can be added
      List<FieldMetadata> fields =
          PartTree.getValidProperties(lastOrder.getProperty().getLeft().getFieldType());

      if (fields != null) {
        for (FieldMetadata field : fields) {
          options.add(query.concat(StringUtils.capitalize(field.getFieldName().toString())));
        }
      }
    }

    return options;
  }


}
