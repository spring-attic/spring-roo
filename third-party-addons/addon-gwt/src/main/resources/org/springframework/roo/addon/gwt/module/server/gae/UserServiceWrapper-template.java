package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import com.google.appengine.api.users.User;

/**
 * Service object that reduces the visible api of
 * {@link com.google.appengine.api.users.UserService}. Needed to work around a
 * limitation of RequestFactory, which cannot yet handle overloaded service
 * methods.
 */
public interface UserServiceWrapper {
	
	String createLoginURL(String destinationURL);

	String createLogoutURL(String destinationURL);

	User getCurrentUser();
}
