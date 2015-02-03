package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import com.google.web.bindery.requestfactory.shared.ProxyForName;
import com.google.web.bindery.requestfactory.shared.ValueProxy;

/**
 * Client visible proxy of Google AppEngine User class.
 */
@ProxyForName("com.google.appengine.api.users.User")
public interface GaeUser extends ValueProxy {
	
	String getNickname();

	String getEmail();
}
