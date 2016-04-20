package org.springframework.roo.addon.finder.addon.parser;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * This class is based on Part.java class from Spring Data commons project.
 * 
 * It has some little changes to be able to work properly on Spring Roo project
 * and make easy Spring Data query parser.
 * 
 * Get more information about original class on:
 * 
 * https://github.com/spring-projects/spring-data-commons/blob/master/src/main/java/org/springframework/data/repository/query/parser/Part.java
 * 
 * Represents a single search expression (which are joined using And/Or operators).
 * This expression needs a property to define the condition. 
 * Optionally, an operator can be set after the property to perform an operation over it. 
 * Furthermore, {@literal IgnoreCase} option is available to be added to any property.
 * 
 * @author Paula Navarro
 * @author Juan Carlos García
 * @since 2.0
 */
public class Part {

  private static final Pattern IGNORE_CASE = Pattern.compile("Ignor(ing|e)Case");

  // Contains property metadata and name
  private final Pair<FieldMetadata, String> property;

  // Operator type
  private Type type = null;
  private String operatorGroup = "";
  private String operator = null;

  private IgnoreCaseType ignoreCase = IgnoreCaseType.NEVER;

  // Stores which ignore case option (IgnoreCase or IgnoringCase) has been used
  private String ignoreCaseString = "";

  private final PartTree currentPartTreeInstance;

  /**
   * Creates a new {@link Part} from a condition stored into source .
   * 
   * @param partTree PartTree instance where current Part will be defined
   * @param source the search criteria
   * @param fields entity properties
   */
  public Part(PartTree partTree, String source, List<FieldMetadata> fields) {

    Validate.notNull(partTree, "ERROR: PartTree instance is necessary to generate Part.");
    Validate.notNull(source, "ERROR: Source can not be null");
    Validate.notNull(fields, "ERROR: Entity properties can not be null");

    this.currentPartTreeInstance = partTree;

    // Extract and remove IgnoreCase option from source
    String partToUse = detectAndSetIgnoreCase(source);

    // Extract property
    this.property = currentPartTreeInstance.extractValidProperty(partToUse, fields);

    // Remove property from source to process the operator
    if (property != null) {
      partToUse = partToUse.substring(property.getRight().length());

      // Extract operator information
      Pair<Type, String> type = Type.extractOperator(partToUse, property.getLeft().getFieldType());
      this.type = type.getLeft();
      this.operator = type.getRight();
      this.operatorGroup = Type.extractOperatorGroup(operator);

      //Validates that ignore case option is only available for string property type 
      if (ignoreCase == IgnoreCaseType.ALWAYS
          && !property.getKey().getFieldType().equals(JavaType.STRING)) {
        throw new IllegalArgumentException(
            "ERROR: IgnoseCase option is only available for String properties");
      }
    }
  }



  /**
   * Detects if expression contains IgnoreCase option and removes it.
   * 
   * @param expression
   * @return expression without IgnoreCase option.
   */
  private String detectAndSetIgnoreCase(String expression) {

    Matcher matcher = IGNORE_CASE.matcher(expression);
    String result = expression;

    if (matcher.find()) {
      ignoreCase = IgnoreCaseType.ALWAYS;
      ignoreCaseString = matcher.group(0);
      result =
          expression.substring(0, matcher.start())
              + expression.substring(matcher.end(), expression.length());
    }

    return result;
  }


  /**
   * Returns how many method parameters are bound by this part.
   * 
   * @return
   */
  public int getNumberOfArguments() {
    if (type == null) {
      return 0;
    }
    return type.getNumberOfArguments();
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
   * @return the operator {@link Type}
   */
  public Type getType() {
    return type;
  }

  /**
   * Returns whether the search criteria referenced should be matched
   * ignoring case.
   * 
   * @return
   */
  public IgnoreCaseType shouldIgnoreCase() {
    return ignoreCase;
  }


  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return (property != null ? property.getRight() : "").concat(operator != null ? operator : "")
        .concat(ignoreCaseString);
  }

  /**
   * Returns operators supported by the search expression property
   * @return
   */
  public List<String> getSupportedOperators() {
    if (property == null) {
      return null;
    }

    List<String> typeKeywords = new ArrayList<String>();
    List<Type> types = Type.getOperators(property.getLeft().getFieldType());

    // Check if operator group is an operator
    boolean removePrefix = Type.ALL_KEYWORDS.contains(operatorGroup);

    // Get operators
    for (Type type : types) {
      for (String keyword : type.getKeywords()) {

        // Add operator if it does not belong to any group and operator group is not defined
        if (StringUtils.isBlank(operatorGroup)
            && !StringUtils.startsWithAny(keyword, Type.PREFIX_GROUP)) {
          typeKeywords.add(keyword);
        }

        // Add operator if it belongs to the operator group specified
        if (StringUtils.isNotBlank(operatorGroup) && keyword.startsWith(operatorGroup)) {

          //If operator group is an operator as well, we need to remove the operator group prefix from operators (to avoid it appears two times )
          if (removePrefix) {
            typeKeywords.add(StringUtils.substringAfter(keyword, operatorGroup));
          } else {
            typeKeywords.add(keyword);
          }
        }
      }
    }

    // If there is not an operator group, all operator groups are available
    if (StringUtils.isBlank(operatorGroup)) {
      typeKeywords.addAll(Arrays.asList(Type.PREFIX_GROUP));
    }

    return typeKeywords;

  }

  /**
   * Returns operators which name starts with a given prefix and are supported by the search expression property
   * 
   * @param prefix
   * @return
   */
  public List<String> getSupportedOperatorsByPrefix(String prefix) {
    if (property == null) {
      return null;
    }

    List<String> typeKeywords = new ArrayList<String>();
    List<Type> types = Type.getOperators(property.getLeft().getFieldType());

    // Check if operator group is an operator
    boolean removePrefix = Type.ALL_KEYWORDS.contains(prefix);

    // Get operators
    for (Type type : types) {
      for (String keyword : type.getKeywords()) {

        // Add operator if it does not belong to any group and operator group is not defined
        if (StringUtils.isBlank(prefix) && !StringUtils.startsWithAny(keyword, Type.PREFIX_GROUP)) {
          typeKeywords.add(keyword);
        }

        // Add operator if it belongs to the operator group specified
        if (StringUtils.isNotBlank(prefix) && keyword.startsWith(prefix)) {

          //If operator group is an operator as well, we need to remove the operator group prefix from operators (to avoid it appears two times )
          if (removePrefix) {
            typeKeywords.add(StringUtils.substringAfter(keyword, prefix));
          } else {
            typeKeywords.add(keyword);
          }
        }
      }
    }

    // If there is not an operator group, all operator groups are available
    if (StringUtils.isBlank(prefix)) {
      typeKeywords.addAll(Arrays.asList(Type.PREFIX_GROUP));
    }

    return typeKeywords;

  }



  /**
   * Returns true if the Part or search criteria has a property defined
   * @return
   */
  public boolean hasProperty() {
    return property != null;
  }

  /**
   * Returns true if the Part or search criteria has an operator
   * @return
   */
  public boolean hasOperator() {
    return type != null && StringUtils.isNotEmpty(operator);
  }


  /**
   * Returns the operator group. If it does not have a group returns an empty string.
   * @return
   */
  public String getOperatorGroup() {
    return operatorGroup;
  }


  /**
   * Returns operator keyword. If operator is not defined, returns an empty string.
   * @return
   */
  public String getOperator() {
    return operator;
  }


  /**
   * Builds a list of parameters based on the number of arguments that operator type needs and the property java type 
   * @return
   */
  public List<FinderParameter> getParameters() {

    List<FinderParameter> parameters = new ArrayList<FinderParameter>();
    String suffix = "";
    int arguments;

    if (!hasProperty()) {
      return parameters;
    }

    // Extract the number of operator parameters
    if (!hasOperator()) {

      // By default, if there is not an explicit operator, Is operation is performed
      arguments = Type.SIMPLE_PROPERTY.getNumberOfArguments();
    } else {
      arguments = type.getNumberOfArguments();
    }

    JavaType javaType = property.getLeft().getFieldType();
    String name = property.getLeft().getFieldName().toString();

    // In operator is a special case, since its parameter is a list of property java type objects
    if (type != null && (type == Type.IN || type == Type.NOT_IN)) {

      name = name.concat("List");
      JavaType listType =
          new JavaType("java.util.List", 0, DataType.TYPE, null, Arrays.asList(new JavaType(
              javaType.getFullyQualifiedTypeName(), javaType.getArray(), DataType.TYPE, javaType
                  .getArgName(), javaType.getParameters(), javaType.getModule())));

      parameters.add(new FinderParameter(listType, new JavaSymbolName(name)));

    } else {

      // Create a parameter for every argument that operator type needs
      for (int i = 0; i < arguments; i++) {

        // If operator type needs several parameters, we have to distinguish them by adding a counter
        if (type.getNumberOfArguments() > 1) {
          suffix = String.valueOf(i + 1);
        }
        parameters.add(new FinderParameter(property.getLeft().getFieldType(), new JavaSymbolName(
            name.concat(suffix))));

      }
    }

    return parameters;
  }

}
