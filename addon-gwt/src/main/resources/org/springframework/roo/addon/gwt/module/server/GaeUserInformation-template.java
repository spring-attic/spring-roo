package __TOP_LEVEL_PACKAGE__.server;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gwt.requestfactory.server.UserInformation;

/**
 * A user information class that uses the Google App Engine authentication framework.
 */
public class GaeUserInformation extends UserInformation {
	private static UserService userService = UserServiceFactory.getUserService();

	public static GaeUserInformation getCurrentUserInformation(String redirectUrl) {
		return new GaeUserInformation(redirectUrl);
	}

	public GaeUserInformation(String redirectUrl) {
		super(redirectUrl);
	}

	public String getEmail() {
		User user = userService.getCurrentUser();
		if (user == null) {
			return "";
		}
		return user.getEmail();
	}

	public Long getId() {
		User user = userService.getCurrentUser();
		if (user == null) {
			return 0L;
		}
		return new Long(user.hashCode());
	}

	public String getLoginUrl() {
		return userService.createLoginURL(redirectUrl);
	}

	public String getLogoutUrl() {
		return userService.createLogoutURL(redirectUrl);
	}

	public String getName() {
		User user = userService.getCurrentUser();
		if (user == null) {
			return "";
		}
		return user.getNickname();
	}

	public boolean isUserLoggedIn() {
		return userService.isUserLoggedIn();
	}

	/**
	 * Does nothing since in GAE authentication, the unique ID is provided by the user service and is based on a hash in the User object.
	 */
	public void setId(Long id) {
		// Do nothing
	}
}
