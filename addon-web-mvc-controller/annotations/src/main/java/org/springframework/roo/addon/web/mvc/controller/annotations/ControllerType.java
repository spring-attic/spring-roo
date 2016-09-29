package org.springframework.roo.addon.web.mvc.controller.annotations;

/**
 * Controller types.
 *
 * @author Manuel Iborra
 * @since 2.0
 */
public enum ControllerType {
  COLLECTION, DETAIL, ITEM, SEARCH;

  public static ControllerType getControllerType(final String typeName) {
    try {
      return ControllerType.valueOf(typeName);
    } catch (final IllegalArgumentException e) {
      return null;
    }
  }
}
