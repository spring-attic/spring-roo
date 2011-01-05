package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gwt.requestfactory.shared.ServiceLocator;

/**
 * Gives a RequestFactory system access to the Google AppEngine UserService.
 */
public class UserServiceLocator implements ServiceLocator {
	public UserServiceWrapper getInstance(Class<?> clazz) {
		final UserService service = UserServiceFactory.getUserService();
		return new UserServiceWrapper() {

			public String createLoginURL(String destinationURL) {
				return service.createLoginURL(destinationURL);
			}

			public String createLogoutURL(String destinationURL) {
				return service.createLogoutURL(destinationURL);
			}

			public User getCurrentUser() {
				return service.getCurrentUser();
			}
		};
	}
}
