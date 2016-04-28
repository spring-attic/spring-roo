package org.springframework.roo.model;

/**
 * Constants for Spring-specific {@link EnumDetails}s. Use them in preference to
 * creating new instances of these types.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public final class SpringEnumDetails {

  // RequestMethod enum details
  public static final EnumDetails REQUEST_METHOD_GET = new EnumDetails(
      SpringJavaType.REQUEST_METHOD, new JavaSymbolName("GET"));
  public static final EnumDetails REQUEST_METHOD_POST = new EnumDetails(
      SpringJavaType.REQUEST_METHOD, new JavaSymbolName("POST"));
  public static final EnumDetails REQUEST_METHOD_PUT = new EnumDetails(
      SpringJavaType.REQUEST_METHOD, new JavaSymbolName("PUT"));
  public static final EnumDetails REQUEST_METHOD_DELETE = new EnumDetails(
      SpringJavaType.REQUEST_METHOD, new JavaSymbolName("DELETE"));

  // MediaType enum details
  public static final EnumDetails MEDIA_TYPE_TEXT_HTML_VALUE = new EnumDetails(
      SpringJavaType.MEDIA_TYPE, new JavaSymbolName("TEXT_HTML_VALUE"));
  public static final EnumDetails MEDIA_TYPE_APPLICATION_JSON_VALUE = new EnumDetails(
      SpringJavaType.MEDIA_TYPE, new JavaSymbolName("APPLICATION_JSON_VALUE"));

  // HttpStatus enum details
  public static final EnumDetails HTTP_STATUS_CONFLICT = new EnumDetails(
      SpringJavaType.HTTP_STATUS, new JavaSymbolName("CONFLICT"));
  public static final EnumDetails HTTP_STATUS_CREATED = new EnumDetails(SpringJavaType.HTTP_STATUS,
      new JavaSymbolName("CREATED"));
  public static final EnumDetails HTTP_STATUS_OK = new EnumDetails(SpringJavaType.HTTP_STATUS,
      new JavaSymbolName("OK"));
  public static final EnumDetails HTTP_STATUS_NOT_FOUND = new EnumDetails(
      SpringJavaType.HTTP_STATUS, new JavaSymbolName("NOT_FOUND"));
  public static final EnumDetails HTTP_STATUS_FOUND = new EnumDetails(SpringJavaType.HTTP_STATUS,
      new JavaSymbolName("FOUND"));

  /**
   * Constructor is private to prevent instantiation
   */
  private SpringEnumDetails() {}
}
