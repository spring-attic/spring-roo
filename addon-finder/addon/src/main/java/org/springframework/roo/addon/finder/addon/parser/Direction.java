package org.springframework.roo.addon.finder.addon.parser;

import java.util.Locale;

/**
 * Order directions, which can be Ascending (from the smallest to the largest) or Descending (from the largest to the smallest).
 * 
 * @author Paula Navarro
 * @since 2.0
 */
public enum Direction {

  ASC("Asc"), DESC("Desc");

  private String keyword;

  private Direction(String keyword) {
    this.keyword = keyword;
  }

  public String getKeyword() {
    return keyword;
  }

  /**
   * Returns the {@link Direction} enum for the given {@link String} value.
   * 
   * @param value
   * @throws IllegalArgumentException in case the given value cannot be parsed into an enum value.
   * @return
   */
  public static Direction fromString(String value) {

    try {
      return Direction.valueOf(value.toUpperCase(Locale.US));
    } catch (Exception e) {
      throw new IllegalArgumentException(
          String.format(
              "ERROR: Invalid value '%s' for orders given! Has to be either 'desc' or 'asc' (case insensitive).",
              value), e);
    }
  }

  /**
   * Returns the {@link Direction} enum for the given {@link String} or null if it cannot be parsed into an enum
   * value.
   * 
   * @param value
   * @return
   */
  public static Direction fromStringOrNull(String value) {

    try {
      return fromString(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
