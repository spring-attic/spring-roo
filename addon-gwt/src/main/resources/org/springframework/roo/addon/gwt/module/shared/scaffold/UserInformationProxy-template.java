package __TOP_LEVEL_PACKAGE__.shared.scaffold;

import com.google.gwt.requestfactory.shared.ProxyFor;
import com.google.gwt.requestfactory.shared.ValueProxy;

import __TOP_LEVEL_PACKAGE__.server.scaffold.UserInformation;

/**
 * User info DTO.
 */
@ProxyFor(UserInformation.class)
public interface UserInformationProxy extends ValueProxy  {
  /**
   * Returns the user's email address.
   *
   * @return the user's email address as a String
   */
  String getEmail();

  /**
   * Returns the user's logout url.
   *
   * @return the user's logout url as a String
   */
  String getLogoutUrl();

  /**
   * Returns the user's name.
   *
   * @return the user's name as a String
   */
  String getName();
}
