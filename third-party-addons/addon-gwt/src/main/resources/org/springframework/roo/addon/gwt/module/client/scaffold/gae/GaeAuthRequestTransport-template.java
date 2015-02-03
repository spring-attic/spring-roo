package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import __TOP_LEVEL_PACKAGE__.client.scaffold.gae.GaeAuthenticationFailureEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.web.bindery.requestfactory.gwt.client.DefaultRequestTransport;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.google.gwt.user.client.Window;

/**
 * Extends DefaultRequestTransport to handle the authentication failures
 * reported by {@link com.google.gwt.sample.gaerequest.server.GaeAuthFilter}
 */
public class GaeAuthRequestTransport extends DefaultRequestTransport {
	private final EventBus eventBus;

	public GaeAuthRequestTransport(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	@Override
	protected RequestCallback createRequestCallback(
			final TransportReceiver receiver) {
		final RequestCallback superCallback = super.createRequestCallback(receiver);

		return new RequestCallback() {
			public void onResponseReceived(Request request, Response response) {
				/*
				 * The GaeAuthFailure filter responds with Response.SC_UNAUTHORIZED and
				 * adds a "login" url header if the user is not logged in. When we
				 * receive that combo, post an event so that the app can handle things
				 * as it sees fit.
				 */

				int statusCode = response.getStatusCode();
				if (Response.SC_UNAUTHORIZED == statusCode) {
					String loginUrl = response.getHeader("login");
					if (loginUrl != null) {
						/*
						 * Hand the receiver a non-fatal callback, so that
						 * com.google.web.bindery.requestfactory.shared.Receiver will not post a
						 * runtime exception.
						 */
						receiver.onTransportFailure(new ServerFailure("Unauthenticated user", null, null, false /* not fatal */));
						eventBus.fireEvent(new GaeAuthenticationFailureEvent(loginUrl));
						return;
					}
				}
				if (statusCode == 0) {
					/*
					 * A response with no status follows the SC_UNAUTHORIZED.
					 * Report it as non-fatal, so that
					 * com.google.web.bindery.requestfactory.shared.Receiver will not post a
					 * runtime exception
					 */
					receiver.onTransportFailure(new ServerFailure("Status zero response, probably after auth failure", null, null, false /* not fatal */));
					return;
				}
				superCallback.onResponseReceived(request, response);
			}

			public void onError(Request request, Throwable exception) {
				superCallback.onError(request, exception);
			}
		};
	}

	@Override
	protected RequestBuilder createRequestBuilder() {
		RequestBuilder builder = super.createRequestBuilder();
		// GaeAuthFilter uses this to construct login url
		builder.setHeader("requestUrl", Window.Location.getHref());
		return builder;
	}
}
