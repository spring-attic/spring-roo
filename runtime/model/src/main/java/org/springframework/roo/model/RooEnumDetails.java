package org.springframework.roo.model;

/**
 * Constants for Roo-specific {@link EnumDetails}s. Use them in preference to
 * creating new instances of these types.
 *
 * @author Sergio Clares
 * @author Jose Manuel Vivó
 * @since 2.0
 */
public final class RooEnumDetails {

  // ControllerType enum details
  public static final EnumDetails CONTROLLER_TYPE_ITEM = new EnumDetails(
      RooJavaType.ROO_ENUM_CONTROLLER_TYPE, new JavaSymbolName("ITEM"));
  public static final EnumDetails CONTROLLER_TYPE_COLLECTION = new EnumDetails(
      RooJavaType.ROO_ENUM_CONTROLLER_TYPE, new JavaSymbolName("COLLECTION"));
  public static final EnumDetails CONTROLLER_TYPE_SEARCH = new EnumDetails(
      RooJavaType.ROO_ENUM_CONTROLLER_TYPE, new JavaSymbolName("SEARCH"));
  public static final EnumDetails CONTROLLER_TYPE_DETAIL = new EnumDetails(
      RooJavaType.ROO_ENUM_CONTROLLER_TYPE, new JavaSymbolName("DETAIL"));

  // RelationType enum details
  public static final EnumDetails RELATION_TYPE_AGGREGATION = new EnumDetails(
      RooJavaType.ROO_ENUM_RELATION_TYPE, new JavaSymbolName("AGGREGATION"));
  public static final EnumDetails RELATION_TYPE_COMPOSITION = new EnumDetails(
      RooJavaType.ROO_ENUM_RELATION_TYPE, new JavaSymbolName("COMPOSITION"));

  /**
   * Constructor is private to prevent instantiation
   */
  private RooEnumDetails() {}
}
