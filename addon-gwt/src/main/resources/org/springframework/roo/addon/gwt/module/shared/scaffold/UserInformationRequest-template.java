package __TOP_LEVEL_PACKAGE__.shared.scaffold;

import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.RequestContext;
import com.google.gwt.requestfactory.shared.Service;

import __TOP_LEVEL_PACKAGE__.server.scaffold.UserInformation;


/**
 * Builds requests for user info.
 */
@Service(UserInformation.class)
public interface UserInformationRequest extends RequestContext {

  /**
   * Returns the current user information.
   *
   * @param redirectUrl the redirect UR as a String
   * @return an instance of {@link Request}&lt;{@link UserInformationProxy}&gt;
   */
  Request<UserInformationProxy> getCurrentUserInformation();
}
