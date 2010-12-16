package __TOP_LEVEL_PACKAGE__.server.scaffold;

import org.springsource.roo.extrack.server.scaffold.UserInformation;

public class UserInformation {

  public static UserInformation getCurrentUserInformation() {
    return new UserInformation();
  }

  public String getEmail() {
    return "Dummy Email";
  }

  public String getLogoutUrl() {
    return "";
  }

  public String getName() {
    return "Dummy User";
  }
}
