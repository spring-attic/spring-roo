package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window.Location;

/**
 * A minimal auth failure handler which takes the user a login page.
 */
public class ReloadOnAuthenticationFailure implements
		GaeAuthenticationFailureEvent.Handler {

	public HandlerRegistration register(EventBus eventBus) {
		return GaeAuthenticationFailureEvent.register(eventBus, this);
	}

	public void onAuthFailure(GaeAuthenticationFailureEvent requestEvent) {
		Location.replace(requestEvent.getLoginUrl());
	}
}
