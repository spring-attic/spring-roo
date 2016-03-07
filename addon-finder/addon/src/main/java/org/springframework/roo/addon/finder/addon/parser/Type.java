package org.springframework.roo.addon.finder.addon.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.roo.model.JavaType;

/**
 * The type of operation. Used to create query parts in various ways.
 * 
 * @author Paula Navarro
 * @since 2.0
 */
public enum Type {

  BETWEEN(2, "IsBetween", "Between"), IS_NOT_NULL(0, "IsNotNull", "NotNull"), IS_NULL(0, "IsNull",
      "Null"), LESS_THAN("IsLessThan", "LessThan"), LESS_THAN_EQUAL("IsLessThanEqual",
      "LessThanEqual"), GREATER_THAN("IsGreaterThan", "GreaterThan"), GREATER_THAN_EQUAL(
      "IsGreaterThanEqual", "GreaterThanEqual"), BEFORE("IsBefore", "Before"), AFTER("IsAfter",
      "After"), NOT_LIKE("IsNotLike", "NotLike"), LIKE("IsLike", "Like"), STARTING_WITH(
      "IsStartingWith", "StartingWith", "StartsWith"), ENDING_WITH("IsEndingWith", "EndingWith",
      "EndsWith"), NOT_CONTAINING("IsNotContaining", "NotContaining", "NotContains"), CONTAINING(
      "IsContaining", "Containing", "Contains"), NOT_IN("IsNotIn", "NotIn"), IN("IsIn", "In"), NEAR(
      "IsNear", "Near"), WITHIN("IsWithin", "Within"), REGEX("MatchesRegex", "Matches", "Regex"), EXISTS(
      0, "Exists"), TRUE(0, "IsTrue", "True"), FALSE(0, "IsFalse", "False"), NEGATING_SIMPLE_PROPERTY(
      "IsNot", "Not"), SIMPLE_PROPERTY("Is", "Equals");

  // Need to list them again explicitly as the order is important
  // (esp. for IS_NULL, IS_NOT_NULL)
  private static final List<Type> ALL = Arrays.asList(IS_NOT_NULL, IS_NULL, BETWEEN, LESS_THAN,
      LESS_THAN_EQUAL, GREATER_THAN, GREATER_THAN_EQUAL, BEFORE, AFTER, NOT_LIKE, LIKE,
      STARTING_WITH, ENDING_WITH, NOT_CONTAINING, CONTAINING, NOT_IN, IN, NEAR, WITHIN, REGEX,
      EXISTS, TRUE, FALSE, NEGATING_SIMPLE_PROPERTY, SIMPLE_PROPERTY);

  public static final Collection<String> ALL_KEYWORDS;

  // Some operators are grouped by their prefix to limit the number of operators
  public static final String[] PREFIX_GROUP = {"Is"};

  static {
    List<String> allKeywords = new ArrayList<String>();
    for (Type type : ALL) {
      allKeywords.addAll(type.keywords);
    }
    ALL_KEYWORDS = Collections.unmodifiableList(allKeywords);
  }


  private final List<String> keywords;
  private final int numberOfArguments;

  /**
   * Creates a new {@link Type} using the given keyword, number of
   * arguments to be bound and operator. Keyword and operator can be
   * {@literal null}.
   * 
   * @param numberOfArguments
   * @param keywords
   */
  private Type(int numberOfArguments, String... keywords) {

    this.numberOfArguments = numberOfArguments;
    this.keywords = Arrays.asList(keywords);
  }

  /**
   * Creates a new {@link Type} using the given keyword, number of arguments to be bound and operator. Keyword and
   * operator can be {@literal null}.
   * 
   * @param numberOfArguments
   * @param keywords
   */
  private Type(String... keywords) {
    this(1, keywords);
  }


  /**
   * Returns the operator group from an operator. 
   * If it does not belong to any group or does not exist, returns an empty string
   * 
   * @param operator 
   * @return group
   */
  public static String extractOperatorGroup(String operator) {
    if (operator == null) {
      return "";
    }
    for (String prefix : PREFIX_GROUP) {
      if (operator.startsWith(prefix))
        return prefix;
    }
    return "";
  }

  /**
   * Gets type operator types supported by Date or Calendar objects
   * @return
   */
  public static List<Type> getDateOperators() {
    return Arrays.asList(BEFORE, AFTER, BETWEEN);
  }

  /**
   * Gets type operator types supported by Numbers
   * @return
   */
  public static List<Type> getNumberOperators() {
    return Arrays.asList(LESS_THAN, LESS_THAN_EQUAL, GREATER_THAN, GREATER_THAN_EQUAL, BETWEEN);
  }

  /**
   * Gets type operator types supported by non-primitive java types
   * @return
   */
  public static List<Type> getObjectOperators() {
    return Arrays.asList(IS_NOT_NULL, IS_NULL);
  }

  /**
   * Gets type operator types supported by booleans
   * @return
   */
  public static List<Type> getBooleanOperators() {
    return Arrays.asList(TRUE, FALSE);
  }

  /**
   * Gets type operator types supported by Strings
   * @return
   */
  public static List<Type> getStringOperators() {
    return Arrays.asList(NOT_LIKE, LIKE, STARTING_WITH, ENDING_WITH, NOT_CONTAINING, CONTAINING,
        REGEX);
  }

  /**
   * Gets operator types supported by a javaType
   * 
   * @param javaType the {@link JavaType}
   * @return
   */
  public static List<Type> getOperators(JavaType javaType) {

    List<Type> types = new ArrayList<Type>();

    if (javaType == null) {
      return types;
    }

    // All java types can perform these operations
    types.add(Type.SIMPLE_PROPERTY);
    types.add(Type.NEGATING_SIMPLE_PROPERTY);
    types.add(Type.NOT_IN);
    types.add(Type.IN);

    // Only objects can use these operators
    if (!javaType.isPrimitive()) {
      types.addAll(Type.getObjectOperators());
    }

    if (javaType.isBoolean()) {
      // Boolean operators
      types.addAll(Type.getBooleanOperators());
    } else if (javaType.equals(JavaType.STRING)) {
      // String operators
      types.addAll(Type.getStringOperators());
    } else if (javaType.equals(new JavaType(Date.class))
        || javaType.equals(new JavaType(Calendar.class))) {
      // Date operators
      types.addAll(Type.getDateOperators());
    } else {
      // Number operators (int, double, long, etc)
      try {
        if (ClassUtils.getClass(javaType.getFullyQualifiedTypeName()).getSuperclass()
            .equals(Number.class)) {
          types.addAll(Type.getNumberOperators());

        }
      } catch (ClassNotFoundException e) {
        // Type not supported
      }
    }
    return types;

  }



  /**
   * Returns the {@link Type} and the operator, that javaType supports, from the given operator source
   * as a Pair(Type, keyword). Since operator source can contain information that is not referred to the operator, 
   * the operator will be the keyword that matches more letters with operator source. 
   * If any operator is found in operator source, returns Pair(SIMPLE_PROPERTY, null).
   * 
   * @param operatorSource
   * @return Pair of operator type and operator name
   */
  public static Pair<Type, String> extractOperator(String operatorSource, JavaType javaType) {
    Type lastType = SIMPLE_PROPERTY;
    String lastKeyword = null;

    if (javaType == null) {
      return Pair.of(lastType, lastKeyword);
    }

    for (Type type : getOperators(javaType)) {
      for (String keyword : type.keywords) {
        if (operatorSource.equals(keyword)) {
          return Pair.of(type, keyword);
        }
        if (operatorSource.startsWith(keyword)) {
          if (lastKeyword == null || lastKeyword.length() < keyword.length()) {
            lastKeyword = keyword;
            lastType = type;
          }
        }
      }
    }
    return Pair.of(lastType, lastKeyword);

  }

  /**
   * Returns all keywords supported by the current {@link Type}.
   * 
   * @return
   */
  public Collection<String> getKeywords() {
    return Collections.unmodifiableList(keywords);
  }



  /**
   * Returns the number of arguments of the property operator. By default
   * this exactly one argument.
   * 
   * @return
   */
  public int getNumberOfArguments() {
    return numberOfArguments;
  }


}
